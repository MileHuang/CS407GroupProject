// DietPlanApiService.kt
package com.cs407.myapplication.viewModels

import com.cs407.myapplication.viewModels.ActivityEstimationRequestDto
import com.cs407.myapplication.viewModels.ActivityEstimationResponseDto
import com.cs407.myapplication.viewModels.DietPlanRequestDto
import com.cs407.myapplication.viewModels.DietPlanResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface DietPlanApiService {

    @POST("diet-plan/activity-profile")
    suspend fun estimateActivityProfile(
        @Body req: ActivityEstimationRequestDto
    ): ActivityEstimationResponseDto

    @POST("diet-plan/generate")
    suspend fun generateDietPlan(
        @Body req: DietPlanRequestDto
    ): DietPlanResponseDto
}
