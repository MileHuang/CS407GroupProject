package com.cs407.myapplication.ui

import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedLocalDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateText by remember { mutableStateOf("") }

    var isExpanded by remember { mutableStateOf(false) }   // 只有 View 按钮控制
    var hasChosenStart by remember { mutableStateOf(false) }
    var startDateForPlan by remember { mutableStateOf<LocalDate?>(null) }

    var isGenerating by remember { mutableStateOf(false) }
    var mealsForSelectedDay by remember { mutableStateOf<List<MealPlanDto>?>(null) }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 顶部选中日期提示（不再有手动 toggle）
            Text(
                text = if (selectedDateText.isEmpty()) "Select a date"
                else "Selected: $selectedDateText",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 只有 isExpanded == true 时才显示日历
            AnimatedVisibility(visible = isExpanded) {
                AndroidView(
                    factory = { CalendarView(context) },
                    update = { view ->
                        view.setOnDateChangeListener { _, year, month, day ->
                            val date = LocalDate.of(year, month + 1, day)
                            selectedLocalDate = date
                            selectedDateText = date.toString()
                            mealsForSelectedDay = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Button 1：设置 start date（不控制日历显隐）
            Button(
                onClick = {
                    val date = selectedLocalDate
                    if (date == null) {
                        Toast.makeText(
                            context,
                            "Tap \"View Diet Plan\" to choose a date first",
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
                    text = if (!hasChosenStart) "Regenerate Diet Plan"
                    else "Start Date: ${startDateForPlan ?: ""}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Button 2：既负责“弹出日历”，又负责 view / generate
            Button(
                onClick = {
                    // 1️⃣ 若日历当前是收起的 → 只展开，不做别的
                    if (!isExpanded) {
                        isExpanded = true
                        return@Button
                    }

                    // 2️⃣ 日历已经展开，要求一定先选日期
                    val date = selectedLocalDate
                    if (date == null) {
                        Toast.makeText(
                            context,
                            "Please tap on a date in the calendar",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    if (!hasChosenStart) {
                        // 3️⃣ 仅查看当天计划：加载 + 收回日历
                        val meals = viewModel.loadMealsForDate(date)
                        mealsForSelectedDay = meals
                        if (meals == null) {
                            Toast.makeText(
                                context,
                                "No diet plan for $date",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // 查看完就收起日历
                        isExpanded = false
                    } else {
                        // 4️⃣ 已有 start，这次把 date 当作 end 来生成区间
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
                                    // ✅ 生成成功后收起日历
                                    isExpanded = false
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
                    text = if (!hasChosenStart)
                        "View Diet Plan for Selected Day"
                    else
                        "Confirm End Date & Generate"
                )
            }

            if (isGenerating) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))

            mealsForSelectedDay?.let { meals ->
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
                        MealCard(meal)
                    }
                }
            }
        }
    }
}




@Composable
private fun MealCard(meal: MealPlanDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
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
                Text("• ${text}")
            }
        }
    }
}
