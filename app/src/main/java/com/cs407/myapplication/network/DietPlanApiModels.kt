package com.cs407.myapplication.network

import com.google.gson.annotations.SerializedName

// ---------- Step 1: 活动强度 ----------

data class DailyRoutineItemDto(
    val period: String,        // "morning", "afternoon", "evening"
    val description: String    // "walk to work 20 minutes" ...
)

data class ActivityEstimationRequestDto(
    val height_cm: Double,
    val weight_kg: Double,
    val sex: String,               // "male", "female", "other"
    val age: Int? = null,

    val avg_daily_steps: Int? = null,
    val step_log: List<Int>? = null,

    val occupation: String,
    val personality: String? = null,

    val self_reported_activity_level: String? = null,
    val daily_routine: List<DailyRoutineItemDto> = emptyList(),

    val sleep_quality: String,
    val stress_level: String
)

data class ActivityEstimationResponseDto(
    val activity_category: String,          // "sedentary", "light", ...
    val estimated_tdee_kcal: Double?,       // nullable: Optional[float]
    val reasoning: String,
    val suggested_training_focus: String?
)


// ---------- Step 2: 饮食计划 ----------

data class DietPreferenceDto(
    val vegetarian: Boolean = false,
    val vegan: Boolean = false,
    val halal: Boolean = false,
    val kosher: Boolean = false,
    val allergies: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    val usual_meals_per_day: Int = 3
)

// 你原来的大概长这样
data class MealPlanDto(
    val type: String,
    val item_1: String?,
    val item_2: String?,
    val item_3: String?,
    val item_4: String?,
    val item_5: String?,
    // 可以保留 meal 级别的 imageUrl，将来想一餐一图的时候可用，现在会是 null
    val image_url: String? = null
)

data class DietPlanDayDto(
    val date: String,
    val calories_kcal: Double,
    val protein_g: Double,
    val fat_g: Double,
    val carb_g: Double,
    val meals: List<MealPlanDto>,
    val day_image_url: String? = null
)

data class DietPlanResponseDto(
    val id: String,
    val start_date: String,
    val end_date: String,
    val goal: String,
    val activity_category: String,
    val tdee_kcal: Double?,
    val summary: String,
    val days: List<DietPlanDayDto>
)


data class DietPlanRequestDto(
    val activity_profile: ActivityEstimationResponseDto,
    val start_date: String,           // LocalDate.toString()
    val end_date: String,
    val goal: String,                 // e.g. "fat loss", "muscle gain"
    val goal_description: String? = null,
    val diet_preference: DietPreferenceDto
)