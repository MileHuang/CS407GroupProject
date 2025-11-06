package com.cs407.myapplication.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.detector.Detection as TfDetection
import kotlin.math.max
import kotlin.math.min

// --- 数据结构（与你现有的保持一致/可并拷） ---
data class PipelineRegion(
    val id: Int,
    val boxNorm: List<Float> // [x0,y0,x1,y1] in [0,1]
)

data class Detection(
    val label: String,
    val score: Float,
    val grams: Float,
    val calories: Float,
    val protein: Float,
    val fat: Float
)

class CalorieAnalysisViewModel : ViewModel() {

    // ================= 配置与状态 =================
    var imagePath: String = ""
        private set
    var bigModelName: String = "YOLOv8"
        private set
    var smallModelName: String = "food_cls"
        private set
    var returnPath: String = ""
        private set

    // label -> (kcal, protein, fat) per 100g
    // 你应该在外部加载 CSV 后调用 setNutritionTable 传进来
    private var classToNutrition: Map<String, Triple<Float, Float, Float>> = emptyMap()

    fun setNutritionTable(table: Map<String, Triple<Float, Float, Float>>) {
        classToNutrition = table
    }

    fun updateImagePath(p: String) { imagePath = p }
    fun updateBigModelName(n: String) { bigModelName = n }
    fun updateSmallModelName(n: String) { smallModelName = n }
    fun updateReturnPath(p: String) { returnPath = p }

    // ================== 大模型（YOLO） ==================
    @Volatile private var yolo: ObjectDetector? = null

    private fun getYolo(ctx: Context): ObjectDetector {
        val existed = yolo
        if (existed != null) return existed
        val base = BaseOptions.builder()
            .setNumThreads(4)
            .build()
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(base)
            .setScoreThreshold(0.25f)   // 低阈值，多找一些框给小模型
            .setMaxResults(50)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            ctx,
            "models/yolo_food.tflite",
            options
        )
        yolo = detector
        return detector
    }

    // 读取位图
    private fun decodeBitmap(path: String): Bitmap {
        return BitmapFactory.decodeFile(path)
            ?: throw IllegalArgumentException("Cannot decode image file: $path")
    }

    // 归一化 box -> [x0,y0,x1,y1] ∈ [0,1]
    private fun toNormBox(bmp: Bitmap, box: android.graphics.RectF): List<Float> {
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        val x0 = (box.left / w).coerceIn(0f, 1f)
        val y0 = (box.top / h).coerceIn(0f, 1f)
        val x1 = (box.right / w).coerceIn(0f, 1f)
        val y1 = (box.bottom / h).coerceIn(0f, 1f)
        return listOf(x0, y0, x1, y1)
    }

    // --- 核心：用 YOLO 找到需要细分的小块区域 ---
    @WorkerThread
    fun bigModelChunk(ctx: Context, imagePath: String = this.imagePath): List<PipelineRegion> {
        val bmp = decodeBitmap(imagePath)
        val detector = getYolo(ctx)
        val tensorImage = TensorImage.fromBitmap(bmp)
        val results = detector.detect(tensorImage)

        val regions = mutableListOf<PipelineRegion>()
        var id = 0
        for (det in results) {
            val box = det.boundingBox
            regions.add(PipelineRegion(id++, toNormBox(bmp, box)))
        }

        // 若 YOLO 没检测到，兜底给整图
        if (regions.isEmpty()) {
            regions.add(PipelineRegion(0, listOf(0f, 0f, 1f, 1f)))
        }
        return regions
    }

    // ================== 小模型（分类/多标签） ==================
    @Volatile private var classifier: ImageClassifier? = null
    private fun getClassifier(ctx: Context): ImageClassifier {
        val existed = classifier
        if (existed != null) return existed
        val base = BaseOptions.builder()
            .setNumThreads(4)
            .build()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(base)
            .setMaxResults(5)
            .build()
        val cls = ImageClassifier.createFromFileAndOptions(
            ctx,
            "models/food_cls.tflite",
            options
        )
        classifier = cls
        return cls
    }

    // 裁剪区域位图
    private fun cropByNormBox(src: Bitmap, boxNorm: List<Float>): Bitmap {
        val (x0, y0, x1, y1) = boxNorm
        val w = src.width
        val h = src.height
        val l = (x0 * w).toInt().coerceIn(0, w - 1)
        val t = (y0 * h).toInt().coerceIn(0, h - 1)
        val r = (x1 * w).toInt().coerceIn(l + 1, w)
        val b = (y1 * h).toInt().coerceIn(t + 1, h)
        val cw = max(1, r - l)
        val ch = max(1, b - t)
        return Bitmap.createBitmap(src, l, t, cw, ch)
    }

    // 简单质量估计：按框面积×整图参考质量（可换成你的体积/像素密度估计）
    private fun estimateGramsByArea(boxNorm: List<Float>, fallbackWholeImageGrams: Float = 300f): Float {
        val area = ((boxNorm[2] - boxNorm[0]) * (boxNorm[3] - boxNorm[1])).coerceIn(0f, 1f)
        // 给一个更合理的下限，防止太小
        return (fallbackWholeImageGrams * area).coerceAtLeast(80f)
    }

    // --- 核心：对每个 Region 做小模型分类，并根据营养表计算 ---
    @WorkerThread
    fun smallModelClassify(
        ctx: Context,
        region: PipelineRegion,
        // 如果你的小模型能直接估克重，把这参数替换掉来源即可
        gramsEstimator: (PipelineRegion) -> Float = { estimateGramsByArea(it.boxNorm) }
    ): List<Detection> {
        val bmp = decodeBitmap(imagePath)
        val crop = cropByNormBox(bmp, region.boxNorm)
        val tensor = TensorImage.fromBitmap(crop)
        val cls = getClassifier(ctx)
        val results = cls.classify(tensor)

        if (results.isEmpty()) return emptyList()

        val estGrams = gramsEstimator(region)
        val factor = estGrams / 100f

        val dets = mutableListOf<Detection>()
        // 取 topK（已在 options 限制），将标签映射到营养表（不在表中则跳过）
        for (cat in results.first().categories) {
            val label = cat.label.uppercase()
            val score = cat.score
            val nut = classToNutrition[label] ?: continue
            val (per100Kcal, per100Protein, per100Fat) = nut
            dets.add(
                Detection(
                    label = label,
                    score = score,
                    grams = estGrams,
                    calories = per100Kcal * factor,
                    protein = per100Protein * factor,
                    fat = per100Fat * factor
                )
            )
        }
        return dets
    }

    // ============== 组合：整张图跑完两阶段并汇总 ==============
    /**
     * 运行完整管线：
     * 1) YOLO 得到菜品级 Region
     * 2) 小模型对每个 Region 分类
     * 3) 将同名类别聚合（求和克重与营养）
     */
    @WorkerThread
    fun runPipeline(ctx: Context): List<Detection> {
        val regions = bigModelChunk(ctx, imagePath)
        val all = regions.flatMap { smallModelClassify(ctx, it) }

        // 聚合同类名
        val agg = all.groupBy { it.label }.map { (label, items) ->
            val grams = items.sumOf { it.grams.toDouble() }.toFloat()
            val calories = items.sumOf { it.calories.toDouble() }.toFloat()
            val protein = items.sumOf { it.protein.toDouble() }.toFloat()
            val fat = items.sumOf { it.fat.toDouble() }.toFloat()
            Detection(
                label = label,
                score = items.maxOf { it.score }, // 取最高置信度做展示
                grams = grams,
                calories = calories,
                protein = protein,
                fat = fat
            )
        }
        return agg
    }
}
