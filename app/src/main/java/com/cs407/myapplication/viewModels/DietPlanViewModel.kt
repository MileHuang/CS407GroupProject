package com.cs407.myapplication.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.myapplication.data.FirebaseProfileRepository
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

// ---------------- Repository 接口 ----------------

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

    // Firebase user profile 仓库
    private val profileRepository = FirebaseProfileRepository()

    // 应用上下文 & 本地缓存路径
    private var appContext: Context? = null
    private var localPlanPathInternal: String? = null

    val localPlanPath: String?
        get() = localPlanPathInternal

    // 日期范围
    var startDate: LocalDate? = null
        private set
    var endDate: LocalDate? = null
        private set

    // UserProfile：StateFlow 方便 UI 监听
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfileFlow: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // 兼容原来的属性访问方式
    var userProfile: UserProfile?
        get() = _userProfile.value
        private set(value) {
            _userProfile.value = value
        }

    // 计划缓存 & 当前选中日期的 meals
    var cachedPlan: DietPlanResponseDto? = null
        private set

    var selectedDayMeals: List<MealPlanDto>? = null
        private set

    // ---------------- 初始化 ----------------

    fun initIfNeeded(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            localPlanPathInternal = File(
                appContext!!.filesDir,
                "diet_plan_cached.json"
            ).absolutePath
        }
    }

    // ---------------- UserProfile 同步相关 ----------------

    /** 本地更新（例如 Profile 页面填完后立刻更新 UI） */
    fun updateUserProfile(profile: UserProfile) {
        userProfile = profile
    }

    /** 主动从 Firebase 刷新用户档案 */
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

    /** 保存档案到 Firebase，并同步到本地 ViewModel */
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

    // ---------------- 日期范围 ----------------

    fun updateDateRange(start: LocalDate, end: LocalDate) {
        startDate = start
        endDate = end
    }

    // ---------------- Diet Plan 相关 ----------------

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

    /**
     * 生成一段时间的饮食计划：
     * - 如果内存里已经有 userProfile：直接用它；
     * - 如果还没有：先从 Firebase 读取 userProfile，读取成功后再请求饮食计划；
     * - 如果 Firebase 里也没有档案：直接 onError，完全不再用硬编码默认值。
     */
    fun requestAndCachePlanForRange(
        start: LocalDate,
        end: LocalDate,
        onError: (Throwable) -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val cachedProfile = userProfile
        if (cachedProfile != null) {
            // 已有 profile，直接用
            launchDietPlanRequest(
                profile = cachedProfile,
                start = start,
                end = end,
                onError = onError,
                onSuccess = onSuccess
            )
        } else {
            // 还没有，就从 Firebase 读取一次
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

    /** 实际去服务器请求饮食计划的部分，抽成单独函数方便复用 */
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

    // ---------------- 本地缓存 ----------------

    private fun requireLocalPath(): String {
        return localPlanPathInternal
            ?: throw IllegalStateException(
                "DietPlanViewModel not initialized, call initIfNeeded(context) first."
            )
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


