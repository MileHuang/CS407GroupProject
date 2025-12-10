package com.cs407.myapplication.viewModels


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.myapplication.data.FirebaseProfileRepository
import com.cs407.myapplication.data.FirebaseDietPlanRepository
import com.cs407.myapplication.network.DietPlanApiClient
import com.cs407.myapplication.network.DietPlanDayDto
import com.cs407.myapplication.network.DietPlanResponseDto
import com.cs407.myapplication.network.MealPlanDto
import com.cs407.myapplication.network.NetworkDietPlanRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate

// ---------------- UserProfile ----------------

data class UserProfile(
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val sex: String = "",
    val age: Int? = null,
    val avgDailySteps: Int? = null,
    val occupation: String = "",
    val personality: String? = null,
    val sleepQuality: String = "medium",
    val stressLevel: String = "medium",
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
    val halal: Boolean = false,
    val kosher: Boolean = false,
    val allergies: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val usualMealsPerDay: Int = 3,
    val goal: String = "fat_loss",
    val goalDescription: String? = null,
)

// ---------------- DietPlanRepository ----------------

interface DietPlanRepository {
    suspend fun requestDietPlanForRange(
        userProfile: UserProfile,
        startDate: LocalDate,
        endDate: LocalDate
    ): DietPlanResponseDto
}

// ---------------- ViewModel ----------------

class DietPlanViewModel : ViewModel() {

    private val repository: DietPlanRepository =
        NetworkDietPlanRepository(DietPlanApiClient.service)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()


    private val profileRepository = FirebaseProfileRepository()
    private val dietPlanCloudRepository = FirebaseDietPlanRepository()

    private var appContext: Context? = null
    private var localPlanPathInternal: String? = null

    val localPlanPath: String?
        get() = localPlanPathInternal

    var startDate: LocalDate? = null
        private set
    var endDate: LocalDate? = null
        private set

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfileFlow: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    var userProfile: UserProfile?
        get() = _userProfile.value
        private set(value) {
            _userProfile.value = value
        }

    var cachedPlan: DietPlanResponseDto? = null
        private set

    var selectedDayMeals: List<MealPlanDto>? = null
        private set

    // ----------------Initialization----------------

    fun initIfNeeded(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            localPlanPathInternal = File(
                appContext!!.filesDir,
                "diet_plan_cached.json"
            ).absolutePath
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    fun refreshProfileFromCloud(
        onError: (Throwable) -> Unit = {}
    ) {
        profileRepository.loadProfile(
            onSuccess = { profile ->
                if (profile != null) {
                    userProfile = profile
                }
            },
            onError = onError
        )
    }

    fun saveProfileToCloud(
        profile: UserProfile,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        userProfile = profile
        profileRepository.saveProfile(
            profile = profile,
            onSuccess = onSuccess,
            onError = onError
        )
    }



    fun refreshPlanFromCloud(
        onError: (Throwable) -> Unit = {}
    ) {
        dietPlanCloudRepository.loadLatestPlan(
            onSuccess = { plan ->
                if (plan != null) {
                    cachedPlan = plan
                    startDate = LocalDate.parse(plan.start_date)
                    endDate = LocalDate.parse(plan.end_date)
                }
            },
            onError = onError
        )
    }



    fun updateDateRange(start: LocalDate, end: LocalDate) {
        startDate = start
        endDate = end
    }


    fun requestAndCachePlanForRange(
        start: LocalDate,
        end: LocalDate,
        onError: (Throwable) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {

        val currentProfile = userProfile
        if (currentProfile != null) {
            launchDietPlanRequest(
                profile = currentProfile,
                start = start,
                end = end,
                onError = onError,
                onSuccess = onSuccess
            )
        } else {

            profileRepository.loadProfile(
                onSuccess = { profile ->
                    if (profile == null) {
                        onError(
                            IllegalStateException(
                                "User profile not found in Firebase. Please complete your profile first."
                            )
                        )
                        return@loadProfile
                    }
                    userProfile = profile
                    launchDietPlanRequest(
                        profile = profile,
                        start = start,
                        end = end,
                        onError = onError,
                        onSuccess = onSuccess
                    )
                },
                onError = onError
            )
        }
    }

    private fun launchDietPlanRequest(
        profile: UserProfile,
        start: LocalDate,
        end: LocalDate,
        onError: (Throwable) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val newPlan = withContext(ioDispatcher) {
                    repository.requestDietPlanForRange(profile, start, end)
                }

                val mergedPlan = combinePlan(cachedPlan, newPlan)

                startDate = LocalDate.parse(mergedPlan.start_date)
                endDate = LocalDate.parse(mergedPlan.end_date)
                cachedPlan = mergedPlan

                withContext(ioDispatcher) {
                    savePlanToLocal(mergedPlan)
                }

                dietPlanCloudRepository.saveLatestPlan(
                    plan = mergedPlan,
                    onSuccess = { /* 可以记录 log，当前先忽略 */ },
                    onError = { /* 不打断 UI，只是云端保存失败 */ }
                )

                onSuccess()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }


    fun loadMealsForDate(targetDate: LocalDate): List<MealPlanDto>? {
        var plan = cachedPlan
        if (plan == null) {
            plan = loadPlanFromLocal()
            cachedPlan = plan
        }

        if (plan == null) {
            selectedDayMeals = null
            return null
        }

        val dateStr = targetDate.toString()
        val day = plan.days.firstOrNull { it.date == dateStr }

        if (day == null) {
            selectedDayMeals = null
            return null
        }

        selectedDayMeals = day.meals
        return day.meals
    }


    private fun requireLocalPath(): String {
        return localPlanPathInternal
            ?: throw IllegalStateException("DietPlanViewModel not initialized, call initIfNeeded(context) first.")
    }

    private fun savePlanToLocal(plan: DietPlanResponseDto) {
        try {
            val path = requireLocalPath()
            val file = File(path)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            val jsonString = gson.toJson(plan)
            file.writeText(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadPlanFromLocal(): DietPlanResponseDto? {
        return try {
            val path = localPlanPathInternal ?: return null
            val file = File(path)
            if (!file.exists()) {
                null
            } else {
                val jsonString = file.readText()
                gson.fromJson(jsonString, DietPlanResponseDto::class.java)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }


    private fun combinePlan(
        oldPlan: DietPlanResponseDto?,
        newPlan: DietPlanResponseDto
    ): DietPlanResponseDto {
        if (oldPlan == null) return newPlan

        val dayMap = LinkedHashMap<String, DietPlanDayDto>()
        for (d in oldPlan.days) {
            dayMap[d.date] = d
        }
        for (d in newPlan.days) {
            dayMap[d.date] = d
        }

        val mergedDays = dayMap.values.sortedBy { LocalDate.parse(it.date) }
        val mergedStart = mergedDays.firstOrNull()?.date ?: newPlan.start_date
        val mergedEnd = mergedDays.lastOrNull()?.date ?: newPlan.end_date

        return DietPlanResponseDto(
            id = newPlan.id,
            start_date = mergedStart,
            end_date = mergedEnd,
            goal = newPlan.goal,
            activity_category = newPlan.activity_category,
            tdee_kcal = newPlan.tdee_kcal,
            summary = newPlan.summary,
            days = mergedDays
        )
    }
}
