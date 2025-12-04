package com.cs407.myapplication.viewModels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.myapplication.network.CalorieApi
import com.cs407.myapplication.network.CalorieApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

// 这里用前面两个文件里的类型
// import com.cs407.myapplication.viewModels.* 已经在同包，无需显式 import

// ------------------------
// UI 用的数据结构
// ------------------------

data class ServerDetectionUi(
    val label: String,
    val score: Double,
    val sourceNutrition: String,
    val modeUsed: String, // "pipe1" / "pipe2"
    val massGrams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val fatGrams: Double,
    val carbGrams: Double
)

data class CalorieUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val mode: String = "pipe2",               // "pipe1" or "pipe2"
    val detections: List<ServerDetectionUi> = emptyList()
)

// ------------------------
// Bitmap → Multipart 工具
// ------------------------

private fun Bitmap.toJpegMultipart(
    fieldName: String = "image",
    fileName: String = "food.jpg"
): MultipartBody.Part {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 95, stream)
    val bytes = stream.toByteArray()
    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, fileName, requestBody)
}

// ------------------------
// ViewModel 本体
// ------------------------

class CalorieServerViewModel(
    private val api: CalorieApi = CalorieApiClient.api
) : ViewModel() {

    companion object {
        const val MODE_PIPE1 = "pipe1"
        const val MODE_PIPE2 = "pipe2"
    }

    private val _uiState = MutableStateFlow(CalorieUiState())
    val uiState = _uiState.asStateFlow()

    fun setMode(mode: String) {
        if (mode != MODE_PIPE1 && mode != MODE_PIPE2) {
            return
        }
        _uiState.update { it.copy(mode = mode) }
    }

    fun analyze(bitmap: Bitmap) {
        val currentMode = _uiState.value.mode

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    detections = emptyList()
                )
            }

            try {
                val imagePart = bitmap.toJpegMultipart()
                val modeBody = currentMode.toRequestBody("text/plain".toMediaType())

                val resp = api.analyzeImage(imagePart, modeBody)

                val uiDetections = resp.detections.map { det ->
                    val nut = det.fused ?: det.pipe2 ?: det.pipe1

                    val mass = nut?.mass_g ?: 0.0
                    val kcal = nut?.calories_kcal ?: 0.0
                    val protein = nut?.protein_g ?: 0.0
                    val fat = nut?.fat_g ?: 0.0
                    val carb = nut?.carb_g ?: 0.0

                    ServerDetectionUi(
                        label = det.label,
                        score = det.score,
                        sourceNutrition = det.source_nutrition,
                        modeUsed = resp.mode,
                        massGrams = mass,
                        caloriesKcal = kcal,
                        proteinGrams = protein,
                        fatGrams = fat,
                        carbGrams = carb
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        detections = uiDetections
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearDetections() {
        _uiState.update { it.copy(detections = emptyList()) }
    }
}
