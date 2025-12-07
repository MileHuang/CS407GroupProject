package com.cs407.myapplication.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.myapplication.network.DietPlanApiClient
import com.cs407.myapplication.network.DietPlanDayDto
import com.cs407.myapplication.network.DietPlanResponseDto
import com.cs407.myapplication.network.MealPlanDto
import com.cs407.myapplication.network.NetworkDietPlanRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate

data class UserProfile(
    val heightCm: Double,
    val weightKg: Double,
    val sex: String,
    val age: Int?,
    val avgDailySteps: Int?,
    val occupation: String,
    val personality: String?,
    val sleepQuality: String,
    val stressLevel: String,
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
    val halal: Boolean = false,
    val kosher: Boolean = false,
    val allergies: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val usualMealsPerDay: Int = 3,
    val goal: String = "fat_loss",
    val goalDescription: String? = null
)

interface DietPlanRepository {
    suspend fun requestDietPlanForRange(
        userProfile: UserProfile,
        startDate: LocalDate,
        endDate: LocalDate
    ): DietPlanResponseDto
}

class DietPlanViewModel : ViewModel() {

    private val repository: DietPlanRepository =
        NetworkDietPlanRepository(DietPlanApiClient.service)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private var appContext: Context? = null
    private var localPlanPathInternal: String? = null

    val localPlanPath: String?
        get() = localPlanPathInternal

    var startDate: LocalDate? = null
        private set
    var endDate: LocalDate? = null
        private set
    var userProfile: UserProfile? = null
        private set

    var cachedPlan: DietPlanResponseDto? = null
        private set

    // ⭐ 当前选中日期对应的“整天图片 URL”
    var selectedDayImageUrl: String? = null
        private set

    var selectedDayMeals: List<MealPlanDto>? = null
        private set

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

    fun updateDateRange(start: LocalDate, end: LocalDate) {
        startDate = start
        endDate = end
    }

    private fun combinePlan(
        oldPlan: DietPlanResponseDto?,
        newPlan: DietPlanResponseDto
    ): DietPlanResponseDto {
        if (oldPlan == null) {
            return newPlan
        }

        val dayMap = LinkedHashMap<String, DietPlanDayDto>()
        for (d in oldPlan.days) {
            dayMap[d.date] = d
        }
        for (d in newPlan.days) {
            // 新计划覆盖旧计划
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

    fun requestAndCachePlanForRange(
        start: LocalDate,
        end: LocalDate,
        onError: (Throwable) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val profile = userProfile ?: UserProfile(
            heightCm = 170.0,
            weightKg = 65.0,
            sex = "male",
            age = 22,
            avgDailySteps = 8000,
            occupation = "student",
            personality = null,
            sleepQuality = "medium",
            stressLevel = "medium",
            vegetarian = false,
            vegan = false,
            halal = false,
            kosher = false,
            allergies = emptyList(),
            dislikes = emptyList(),
            favorites = listOf("chicken breast", "rice", "noodles"),
            usualMealsPerDay = 3
        )

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
            selectedDayImageUrl = null
            return null
        }

        val dateStr = targetDate.toString()
        val day = plan.days.firstOrNull { it.date == dateStr }

        if (day == null) {
            selectedDayMeals = null
            selectedDayImageUrl = null
            return null
        }

        selectedDayMeals = day.meals
        // ⭐ 把该天的图片 URL 存起来给 UI 用
        selectedDayImageUrl = day.day_image_url    // 注意下面 DTO 里要有 dayImageUrl
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
}
