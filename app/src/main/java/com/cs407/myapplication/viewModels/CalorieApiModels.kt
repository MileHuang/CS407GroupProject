package com.cs407.myapplication.viewModels

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
