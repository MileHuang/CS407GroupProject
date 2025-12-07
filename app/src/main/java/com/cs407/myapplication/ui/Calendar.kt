package com.cs407.myapplication.ui

import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.myapplication.network.MealPlanDto
import com.cs407.myapplication.viewModels.DietPlanViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

// 和服务器保持一致：Android 模拟器访问 PC 本机要用 10.0.2.2
private const val SERVER_BASE = "http://10.0.2.2:8000"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: DietPlanViewModel = viewModel()
) {
    var selectedDateText by remember { mutableStateOf("") }
    var selectedLocalDate by remember { mutableStateOf<LocalDate?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var hasChosenStart by remember { mutableStateOf(false) }
    var startDateForPlan by remember { mutableStateOf<LocalDate?>(null) }

    // 当天的 meals
    var mealsForSelectedDay by remember { mutableStateOf<List<MealPlanDto>?>(null) }
    // 当天的大图（服务器生成的一张拼图）
    var dayImageUrl by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            viewModel.initIfNeeded(context)
        } catch (_: Throwable) {
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB3E5FC),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (selectedDateText.isEmpty()) {
                    "Select a date:"
                } else {
                    "Selected: $selectedDateText"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                factory = { CalendarView(context) },
                update = { view ->
                    view.setOnDateChangeListener { _, year, month, day ->
                        val date = LocalDate.of(year, month + 1, day)
                        selectedLocalDate = date
                        selectedDateText = date.toString()
                        // 切日期时清空旧数据
                        mealsForSelectedDay = null
                        dayImageUrl = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Button 1：设置开始日期（用于重新生成计划）
            Button(
                onClick = {
                    val date = selectedLocalDate
                    if (date == null) {
                        Toast.makeText(
                            context,
                            "Please select a date first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    startDateForPlan = date
                    hasChosenStart = true
                    Toast.makeText(
                        context,
                        "Start date set to $date",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text(
                    text = if (!hasChosenStart) {
                        "Regenerate Diet Plan"
                    } else {
                        "Confirm Start Date: ${startDateForPlan ?: ""}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2：查看某一天 / 选择结束日期并生成
            Button(
                onClick = {
                    val date = selectedLocalDate
                    if (date == null) {
                        Toast.makeText(
                            context,
                            "Please select a date first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (!hasChosenStart) {
                        val meals = viewModel.loadMealsForDate(date)
                        mealsForSelectedDay = meals

                        val firstUrl = meals?.firstOrNull()?.image_url
                        Toast.makeText(
                            context,
                            "Found ${meals?.size ?: 0} meals, firstUrl=$firstUrl",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // 生成计划：确定结束日期
                        val start = startDateForPlan
                        if (start == null) {
                            Toast.makeText(
                                context,
                                "Start date not set",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val (startFinal, endFinal) =
                            if (start <= date) start to date else date to start

                        isGenerating = true
                        mealsForSelectedDay = null
                        dayImageUrl = null

                        scope.launch {
                            viewModel.requestAndCachePlanForRange(
                                start = startFinal,
                                end = endFinal,
                                onError = { e ->
                                    isGenerating = false
                                    hasChosenStart = false
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onSuccess = {
                                    isGenerating = false
                                    hasChosenStart = false
                                    Toast.makeText(
                                        context,
                                        "Finish: plan from $startFinal to $endFinal",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                }
            ) {
                Text(
                    text = if (!hasChosenStart) {
                        "View Diet Plan for Selected Day"
                    } else {
                        "Confirm End Date & Generate"
                    }
                )
            }

            if (isGenerating) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator()
            }

            mealsForSelectedDay?.let { meals ->
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Meals for $selectedDateText",
                    style = MaterialTheme.typography.titleMedium
                )

                // 先显示当天总览图（如果有）
                dayImageUrl?.let { rel ->
                    val fullUrl =
                        if (rel.startsWith("http")) rel else SERVER_BASE + rel

                    Spacer(modifier = Modifier.height(8.dp))

                    AsyncImage(
                        model = fullUrl,
                        contentDescription = "Day overview image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meals) { meal ->
                        MealCard(meal = meal)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: MealPlanDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = meal.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 如果以后 meal 也有单独图片，这里顺便支持
            val url = meal.image_url
            if (!url.isNullOrBlank()) {
                val fullUrl =
                    if (url.startsWith("http")) url else SERVER_BASE + url

                AsyncImage(
                    model = fullUrl,
                    contentDescription = "Meal image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val items = listOfNotNull(
                meal.item_1?.takeIf { it.isNotBlank() },
                meal.item_2?.takeIf { it.isNotBlank() },
                meal.item_3?.takeIf { it.isNotBlank() },
                meal.item_4?.takeIf { it.isNotBlank() },
                meal.item_5?.takeIf { it.isNotBlank() }
            )

            items.forEachIndexed { index, text ->
                Text(
                    text = "• item_${index + 1}: $text",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
