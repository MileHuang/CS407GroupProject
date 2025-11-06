package com.cs407.myapplication.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelsConfig(
    val bigModelName: String = "YOLO",
    val smallModelName: String = "small"
)

data class PipelineRegion(
    val id: Int,
    val bbox: List<Float>
)

data class Detection(
    val label: String,
    val massKg: Float
)

data class PipelineResult(
    val regions: List<PipelineRegion>,
    val detections: List<Detection>
)

data class NutritionRow(
    val item: String,
    val caloriesPer100Kg: Float,
    val proteinPer100Kg: Float,
    val fatPer100Kg: Float
)

data class NutritionTotals(
    val calories: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f
)

sealed class UiState {
    data object Idle : UiState()
    data object Running : UiState()
    data class Ready(val result: PipelineResult, val totals: NutritionTotals) : UiState()
}

class CalorieAnalysisViewModel : ViewModel() {

    private val _imagePath = MutableStateFlow("")
    val imagePath: StateFlow<String> = _imagePath

    private val _models = MutableStateFlow(ModelsConfig())
    val models: StateFlow<ModelsConfig> = _models

    private val _returnPath = MutableStateFlow("")
    val returnPath: StateFlow<String> = _returnPath

    private val _nutrition = MutableStateFlow(NutritionTotals())
    val nutrition: StateFlow<NutritionTotals> = _nutrition

    private val _ui = MutableStateFlow<UiState>(UiState.Idle)
    val ui: StateFlow<UiState> = _ui

    private val nutritionTable = MutableStateFlow<Map<String, NutritionRow>>(emptyMap())

    fun updateImagePath(path: String) {
        _imagePath.value = path
    }

    fun updateModelNames(bigModel: String, smallModel: String) {
        _models.value = ModelsConfig(bigModelName = bigModel, smallModelName = smallModel)
    }

    fun updateReturnPath(path: String) {
        _returnPath.value = path
    }

    fun setTablesFromCsvStrings(csvFiles: List<String>) {
        val merged = mutableMapOf<String, NutritionRow>()
        for (csv in csvFiles) {
            val rows = parseNutritionCsv(csv)
            for (r in rows) {
                merged[r.item.lowercase()] = r
            }
        }
        nutritionTable.value = merged
    }

    fun runPipeline() {
        val img = _imagePath.value
        val cfg = _models.value
        if (img.isEmpty()) {
            return
        }
        _ui.value = UiState.Running
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val regions = bigModelChunk(img, cfg.bigModelName)
                val dets = mutableListOf<Detection>()
                for (rg in regions) {
                    val more = smallModelClassify(rg, cfg.smallModelName)
                    dets.addAll(more)
                }
                PipelineResult(regions = regions, detections = dets)
            }
            val totals = computeTotals(result.detections, nutritionTable.value)
            _nutrition.value = totals
            _ui.value = UiState.Ready(result, totals)
        }
    }

    private fun bigModelChunk(imagePath: String, modelName: String): List<PipelineRegion> {
        val out = mutableListOf<PipelineRegion>()
        out.add(PipelineRegion(0, listOf(0f, 0f, 1f, 1f)))
        return out
    }

    private fun smallModelClassify(region: PipelineRegion, modelName: String): List<Detection> {
        return emptyList()
    }

    private fun computeTotals(detections: List<Detection>, table: Map<String, NutritionRow>): NutritionTotals {
        var cal = 0f
        var pro = 0f
        var fat = 0f
        for (d in detections) {
            val key = d.label.lowercase()
            val row = table[key]
            if (row != null) {
                val factor = d.massKg / 100f
                cal += row.caloriesPer100Kg * factor
                pro += row.proteinPer100Kg * factor
                fat += row.fatPer100Kg * factor
            }
        }
        return NutritionTotals(calories = cal, protein = pro, fat = fat)
    }

    private fun parseNutritionCsv(csv: String): List<NutritionRow> {
        val out = mutableListOf<NutritionRow>()
        val lines = csv.split("\n")
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) {
                continue
            }
            val parts = line.split(",")
            if (parts.size < 2) {
                continue
            }
            val item = parts[0].trim()
            if (item.isEmpty()) {
                continue
            }
            val nums = mutableListOf<Float>()
            for (p in parts.drop(1)) {
                val v = p.trim()
                val f = v.toFloatOrNull()
                if (f != null) {
                    nums.add(f)
                }
            }
            if (nums.isEmpty()) {
                continue
            }
            val c = nums.getOrNull(0) ?: 0f
            val pr = nums.getOrNull(1) ?: 0f
            val ft = nums.getOrNull(2) ?: 0f
            out.add(NutritionRow(item = item, caloriesPer100Kg = c, proteinPer100Kg = pr, fatPer100Kg = ft))
        }
        return out
    }

    fun totalCalories(): Float {
        return _nutrition.value.calories
    }

    fun totalProtein(): Float {
        return _nutrition.value.protein
    }

    fun totalFat(): Float {
        return _nutrition.value.fat
    }
}
