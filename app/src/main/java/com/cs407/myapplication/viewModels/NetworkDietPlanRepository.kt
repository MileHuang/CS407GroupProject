package com.cs407.myapplication.viewModels

import com.cs407.myapplication.viewModels.DietPlanApiService
import com.cs407.myapplication.viewModels.ActivityEstimationRequestDto
import com.cs407.myapplication.viewModels.ActivityEstimationResponseDto
import com.cs407.myapplication.viewModels.DietPlanRequestDto
import com.cs407.myapplication.viewModels.DietPlanResponseDto
import com.cs407.myapplication.viewModels.DietPreferenceDto
import java.time.LocalDate

/**
 * 真正访问 server.py 的 Repository 实现
 */
class NetworkDietPlanRepository(
    private val api: DietPlanApiService
) : DietPlanRepository {

    override suspend fun requestDietPlanForRange(
        userProfile: UserProfile,
        startDate: LocalDate,
        endDate: LocalDate
    ): DietPlanResponseDto {

        // -------- Step 1: UserProfile -> ActivityEstimationRequestDto --------
        val activityReq = ActivityEstimationRequestDto(
            height_cm = userProfile.heightCm,
            weight_kg = userProfile.weightKg,
            sex = userProfile.sex,
            age = userProfile.age,
            avg_daily_steps = userProfile.avgDailySteps,

            step_log = null,

            occupation = userProfile.occupation,

            personality = null,
            self_reported_activity_level = null,

            daily_routine = emptyList(),

            sleep_quality = "unknown",
            stress_level = "unknown"
        )


        // /diet-plan/activity-profile
        val activityProfile: ActivityEstimationResponseDto =
            api.estimateActivityProfile(activityReq)

        // -------- Step 2: DietPreferenceDto + DietPlanRequestDto --------
        val dietPref = DietPreferenceDto(
            vegetarian = userProfile.vegetarian,
            vegan = userProfile.vegan,
            halal = userProfile.halal,
            kosher = userProfile.kosher,
            allergies = userProfile.allergies,
            dislikes = userProfile.dislikes,
            favorites = userProfile.favorites,
            usual_meals_per_day = userProfile.usualMealsPerDay
        )

        val request = DietPlanRequestDto(
            activity_profile = activityProfile,
            start_date = startDate.toString(),
            end_date = endDate.toString(),
            goal = userProfile.goal,
            goal_description = userProfile.goalDescription,
            diet_preference = dietPref
        )

        // /diet-plan/generate
        return api.generateDietPlan(request)
    }
}
