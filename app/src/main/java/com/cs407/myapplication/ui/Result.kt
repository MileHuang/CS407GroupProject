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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlin.math.PI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(imageUri: String?, onBack: () -> Unit) {

    var selectedModel by remember { mutableStateOf("Food Calorie Model") }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    // ---------------------------
    // Mock demo values (replace with backend data)
    // ---------------------------
    val food = "hamburger"
    val calories = 441.6
    val mass = 220.8
    val protein = 16.6
    val fat = 14.7
    val carbs = 60.7
    val recommended = 2000.0  // Placeholder

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
                            listOf(
                                "Food Calorie Model",
                                "Nutrition Model",
                                "Fitness Model"
                            ).forEach { model ->
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image on the LEFT
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Food label on RIGHT — big and obvious
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = food.uppercase(),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = selectedModel,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========= TABLE ==========
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

            // ========= DONUT CHART ==========
            DonutChart(
                protein = protein,
                fat = fat,
                carbs = carbs,
                totalCalories = calories,
                recommendedCalories = recommended
            )
        }
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
    val recProtein = 120  // placeholder
    val recFat = 70
    val recCarb = 260
    val recCalories = 2000 // NEW 🔥

    val rows = listOf(
        Triple("Food", food, ""),
        Triple("Calories", "%.1f kcal".format(calories), "$recCalories kcal"),
        Triple("Mass", "%.1f g".format(mass), ""),
        Triple("Protein", "%.1f g".format(protein), "${recProtein} g"),
        Triple("Fat", "%.1f g".format(fat), "${recFat} g"),
        Triple("Carbs", "%.1f g".format(carbs), "${recCarb} g"),
        Triple("Remaining", "%.1f kcal".format(recommend - calories), "")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Metric", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Detected", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Recommended", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        rows.forEach { (label, value, rec) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(label, modifier = Modifier.weight(1f))
                Text(value, modifier = Modifier.weight(1f))
                Text(rec, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}



//////////////////////////////////////////////////////////
//               DONUT CHART SECTION
//////////////////////////////////////////////////////////

@Composable
fun DonutChart(
    protein: Double,
    fat: Double,
    carbs: Double,
    totalCalories: Double,
    recommendedCalories: Double
) {
    // Convert grams → kcal
    val proteinKcal = protein * 4
    val fatKcal = fat * 9
    val carbKcal = carbs * 4
    val remaining = (recommendedCalories - totalCalories).coerceAtLeast(0.0)

    val segments = listOf(
        Triple("Protein", proteinKcal, Color(0xFF64B5F6)),
        Triple("Fat", fatKcal, Color(0xFFFFB74D)),
        Triple("Carbs", carbKcal, Color(0xFF81C784)),
        Triple("Remaining", remaining, Color(0xFFE0E0E0))
    )

    val totalKcal = segments.sumOf { it.second }
    if (totalKcal <= 0) return

    val percentOfGoal = (totalCalories / recommendedCalories * 100).coerceIn(0.0, 999.0)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(260.dp)) {
            var startAngle = -90f

            val diameter = size.minDimension
            val strokeWidth = 40f

            segments.forEach { (_, value, color) ->
                if (value <= 0) return@forEach

                val sweep = 360f * (value / totalKcal).toFloat()

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = androidx.compose.ui.geometry.Offset(
                        (size.width - diameter) / 2,
                        (size.height - diameter) / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter)
                )

                startAngle += sweep
            }
        }

        // Center Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${"%.0f".format(percentOfGoal)}%", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("of daily goal")
        }

        // Labels
        Text("Protein", modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp), fontSize = 12.sp)
        Text("Fat", modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp), fontSize = 12.sp)
        Text("Carbs", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), fontSize = 12.sp)
        Text("Remaining", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), fontSize = 12.sp)
    }
}
