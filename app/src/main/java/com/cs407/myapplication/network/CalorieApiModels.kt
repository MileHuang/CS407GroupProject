package com.cs407.myapplication.network

// 只放网络返回的 DTO，和 server.py 对应

data class NutritionDetailDto(
    val mass_g: Double,
    val calories_kcal: Double,
    val protein_g: Double,
    val fat_g: Double,
    val carb_g: Double
)

data class DetectionResultDto(
    val label: String,
    val score: Double,
    val source_nutrition: String,
    val pipe1: NutritionDetailDto?,
    val pipe2: NutritionDetailDto?,
    val fused: NutritionDetailDto?
)

data class AnalyzeResponseDto(
    val id: String,
    val mode: String,
    val detections: List<DetectionResultDto>
)

data class DietPlanUserProfileRequest(
    val height_cm: Double,
    val weight_kg: Double,
    val sex: String,
    val age: Int?,
    val avg_daily_steps: Int?,
    val occupation: String,
    val personality: String?,
    val sleep_quality: String,
    val stress_level: String,
    val vegetarian: Boolean,
    val vegan: Boolean,
    val halal: Boolean,
    val kosher: Boolean,
    val allergies: List<String>,
    val dislikes: List<String>,
    val favorites: List<String>,
    val usual_meals_per_day: Int
)

data class DietPlanGenerateRequest(
    val user_profile: DietPlanUserProfileRequest,
    val start_date: String,
    val end_date: String,
    val goal: String
)