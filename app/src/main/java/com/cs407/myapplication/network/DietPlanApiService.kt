// DietPlanApiService.kt
package com.cs407.myapplication.network

import com.cs407.myapplication.network.ActivityEstimationRequestDto
import com.cs407.myapplication.network.ActivityEstimationResponseDto
import com.cs407.myapplication.network.DietPlanRequestDto
import com.cs407.myapplication.network.DietPlanResponseDto
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
