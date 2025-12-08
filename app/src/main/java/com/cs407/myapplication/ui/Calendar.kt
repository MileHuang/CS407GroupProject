package com.cs407.myapplication.ui

import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.myapplication.network.MealPlanDto
import com.cs407.myapplication.viewModels.DietPlanViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val SERVER_BASE = "http://10.0.2.2:8000"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    viewModel: DietPlanViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedText by remember { mutableStateOf("") }

    var meals by remember { mutableStateOf<List<MealPlanDto>?>(null) }

    // ⭐ 日历是否展开
    var calendarExpanded by remember { mutableStateOf(true) }

    // ⭐ Generate 模式变量
    var generating by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) { viewModel.initIfNeeded(ctx) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {

            // -------- 折叠时的一行显示 --------
            if (!calendarExpanded) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { calendarExpanded = true }
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Selected: $selectedText")
                        Text("Change ▾", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // -------- 日历本体 --------
            AnimatedVisibility(visible = calendarExpanded) {
                AndroidView(
                    factory = { CalendarView(ctx) },
                    update = { calendar ->
                        calendar.setOnDateChangeListener { _, y, m, d ->
                            val date = LocalDate.of(y, m + 1, d)
                            selectedDate = date
                            selectedText = date.toString()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(350.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // =======================
            // ⭐ 按钮逻辑部分
            // =======================
            if (calendarExpanded) {

                // ⭐ 不是 generate 模式时显示“View + Generate”
                if (startDate == null) {

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val date = selectedDate ?: return@Button Toast
                                .makeText(ctx, "Please select a date", Toast.LENGTH_SHORT).show()

                            meals = viewModel.loadMealsForDate(date)
                            calendarExpanded = false   // 收起日历
                        }
                    ) { Text("View Diet Plan for Selected Day") }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val date = selectedDate ?: return@Button Toast
                                .makeText(ctx, "Please select a date", Toast.LENGTH_SHORT).show()

                            startDate = date
                            Toast.makeText(ctx, "Start date selected: $date\nSelect end date", Toast.LENGTH_LONG).show()
                        }
                    ) { Text("Generate Diet Plan") }

                } else {
                    // ⭐ startDate != null → 第二步确认 End Date
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val end = selectedDate ?: return@Button Toast
                                .makeText(ctx, "Please select end date", Toast.LENGTH_SHORT)
                                .show()

                            val s = startDate!!
                            val (startFinal, endFinal) =
                                if (s <= end) s to end else end to s

                            generating = true

                            scope.launch {
                                viewModel.requestAndCachePlanForRange(
                                    startFinal, endFinal,
                                    onSuccess = {
                                        generating = false
                                        startDate = null
                                        Toast.makeText(ctx, "Plan Generated!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = {
                                        generating = false
                                        startDate = null
                                        Toast.makeText(ctx, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    ) { Text("Confirm End Date & Generate") }
                }

                Spacer(Modifier.height(16.dp))
            }

            if (generating) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(12.dp))
            }

            // =====================
            // ⭐ Meals 显示
            // =====================
            meals?.let { list ->
                Text("Meals for $selectedText", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(list) { MealCard(it) }
                }
            }
        }
    }
}


@Composable
fun MealCard(meal: MealPlanDto) {
    val bg = when (meal.type.lowercase()) {
        "breakfast" -> Color(0xFFFFF3CD)
        "lunch" -> Color(0xFFE1F5FE)
        "dinner" -> Color(0xFFE8F5E9)
        else -> Color(0xFFF5F5F5)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                meal.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(12.dp))

            meal.image_url?.takeIf { it.isNotBlank() }?.let { img ->
                AsyncImage(
                    model = if (img.startsWith("http")) img else SERVER_BASE + img,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }

            val items = listOfNotNull(
                meal.item_1, meal.item_2, meal.item_3, meal.item_4, meal.item_5
            ).filter { it.isNotBlank() }

            items.forEach {
                Text("• $it", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
