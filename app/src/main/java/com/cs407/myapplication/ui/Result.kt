package com.cs407.myapplication.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlin.math.PI
import android.os.Build
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.cs407.myapplication.viewModels.CalorieServerViewModel

//////////////////////////////////////////////////////////////
//                 RESULT SCREEN (SERVER VERSION)
//////////////////////////////////////////////////////////////

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: String?,
    onBack: () -> Unit,
    viewModel: CalorieServerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 当图片加载时触发服务器调用
    LaunchedEffect(imageUri) {
        imageUri?.let {
            val uri = Uri.parse(Uri.decode(it))
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) viewModel.analyze(bmp)
        }
    }

    var selectedModel by remember { mutableStateOf("Food Calorie Model") }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    // 从服务器取第一项
    val det = uiState.detections.firstOrNull()

    val food = det?.label ?: "Detecting..."
    val calories = det?.caloriesKcal ?: 0.0
    val mass = det?.massGrams ?: 0.0
    val protein = det?.proteinGrams ?: 0.0
    val fat = det?.fatGrams ?: 0.0
    val carbs = det?.carbGrams ?: 0.0
    val recommended = 2000.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { modelMenuExpanded = true }) {
                            Text(selectedModel, color = Color.Black)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            listOf("Food Calorie Model", "Nutrition Model", "Fitness Model")
                                .forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model) },
                                        onClick = {
                                            selectedModel = model
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB3E5FC)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            //------------------------------
            // IMAGE LEFT + FOOD LABEL RIGHT
            //------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = food.uppercase(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedModel,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            //------------------------------
            // SHOW SERVER ERROR / LOADING
            //------------------------------

            if (uiState.isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            uiState.errorMessage?.let {
                Text("Error: $it", color = Color.Red)
                return@Column
            }

            //------------------------------
            //   TABLE WITH SERVER DATA
            //------------------------------

            InfoTable(
                food = food,
                calories = calories,
                mass = mass,
                protein = protein,
                fat = fat,
                carbs = carbs,
                recommend = recommended
            )

            Spacer(modifier = Modifier.height(24.dp))

            //------------------------------
            //   DONUT CHART WITH SERVER DATA
            //------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .height(250.dp),     // 给两个图相同高度
                    contentAlignment = Alignment.Center
                ) {
                    MealPercentDonut(
                        mealCalories = calories,
                        dailyGoal = recommended
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .height(250.dp),     // 同样高度
                    contentAlignment = Alignment.Center
                ) {
                    MacroDonut(
                        protein = protein,
                        fat = fat,
                        carbs = carbs
                    )
                }
            }
        }
    }
}

//////////////////////////////////////////////////////////////
//                 BITMAP LOADER
//////////////////////////////////////////////////////////////

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

//////////////////////////////////////////////////////////
//               TABLE SECTION
//////////////////////////////////////////////////////////

@Composable
fun InfoTable(
    food: String,
    calories: Double,
    mass: Double,
    protein: Double,
    fat: Double,
    carbs: Double,
    recommend: Double
) {
    val rows = listOf(
        "Food" to food,
        "Calories" to "%.1f kcal".format(calories),
        "Mass" to "%.1f g".format(mass),
        "Protein" to "%.1f g".format(protein),
        "Fat" to "%.1f g".format(fat),
        "Carbs" to "%.1f g".format(carbs),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F7F7))
            .padding(16.dp)
    ) {
        Text(
            "Nutrition Facts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, fontSize = 15.sp)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0x22000000))
        }
    }
}




//////////////////////////////////////////////////////////
//               DONUT CHART SECTION
//////////////////////////////////////////////////////////

@Composable
fun MealPercentDonut(
    mealCalories: Double,
    dailyGoal: Double
) {
    val percent = (mealCalories / dailyGoal).coerceIn(0.0, 1.0)
    val sweepAngle = (percent * 360f).toFloat()

    Column(
        modifier = Modifier.height(250.dp),   // 🔥 强制相同高度
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween  // 🔥 上下平分对齐
    ) {

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                drawArc(
                    color = Color(0xFFE0E0E0),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 22f)
                )
                drawArc(
                    color = Color(0xFF4FC3F7),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 22f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(percent * 100).toInt()}%",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("of daily", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Text("Meal % Goal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MacroDonut(
    protein: Double,
    fat: Double,
    carbs: Double
) {
    val proteinKcal = protein * 4
    val fatKcal = fat * 9
    val carbKcal = carbs * 4
    val total = (proteinKcal + fatKcal + carbKcal).takeIf { it > 0 } ?: 1.0

    val segments = listOf(
        Triple("Protein", proteinKcal, Color(0xFF64B5F6)),
        Triple("Carbs",   carbKcal,    Color(0xFFFFF176)),
        Triple("Fat",     fatKcal,     Color(0xFFFF8A65))
    )

    Column(
        modifier = Modifier.height(250.dp),   // 🔥 一样高度
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween  // 🔥 自动上下均分，看起来等高
    ) {

        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f
                segments.forEach { (_, value, color) ->
                    val sweep = (value / total * 360f).toFloat()
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 22f)
                    )
                    startAngle += sweep
                }
            }

            Text("Macros", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(horizontalAlignment = Alignment.Start) {
            segments.forEach { (label, value, color) ->
                val percent = value / total * 100
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$label: ${percent.toInt()}%", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
