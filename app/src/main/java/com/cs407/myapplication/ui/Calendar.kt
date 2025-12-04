package com.cs407.myapplication.ui

import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.myapplication.viewModels.DietPlanViewModel
import com.cs407.myapplication.viewModels.MealPlanDto
import kotlinx.coroutines.launch
import java.time.LocalDate

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

    // 当前选中日期的餐食列表（用你说的组合结构）
    var mealsForSelectedDay by remember { mutableStateOf<List<MealPlanDto>?>(null) }

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
                        // 切日期时先清空旧的 meals
                        mealsForSelectedDay = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Button 1：重新生成饮食计划（设置开始日期）
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

            // Button 2：查看当日 / 确认结束日期并生成
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
                        // 查看当日饮食计划，并把结果存到 mealsForSelectedDay
                        val meals = viewModel.loadMealsForDate(date)
                        mealsForSelectedDay = meals
                        if (meals == null) {
                            Toast.makeText(
                                context,
                                "No diet plan for $date",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Found ${meals.size} meals for $date",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // 确认结束日期 + 调用生成接口
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

            // ---------- 用组合打印出食物列表 ----------
            mealsForSelectedDay?.let { meals ->
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Meals for $selectedDateText",
                    style = MaterialTheme.typography.titleMedium
                )

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