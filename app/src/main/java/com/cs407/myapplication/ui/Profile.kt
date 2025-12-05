package com.cs407.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.myapplication.viewModels.DietPlanViewModel
import com.cs407.myapplication.viewModels.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    dietPlanViewModel: DietPlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 从 ViewModel 的 StateFlow 里拿当前 profile
    val profileState by dietPlanViewModel.userProfileFlow.collectAsState()

    // 页面第一次进入时，尝试从 Firebase 拉取 profile
    LaunchedEffect(Unit) {
        dietPlanViewModel.refreshProfileFromCloud { e ->
            Toast.makeText(
                context,
                "Failed to load profile: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 各个输入框的本地状态
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    var sexText by rememberSaveable { mutableStateOf("") }
    var ageText by rememberSaveable { mutableStateOf("") }
    var stepsText by rememberSaveable { mutableStateOf("") }
    var occupationText by rememberSaveable { mutableStateOf("") }
    var personalityText by rememberSaveable { mutableStateOf("") }
    var sleepQualityText by rememberSaveable { mutableStateOf("medium") }
    var stressLevelText by rememberSaveable { mutableStateOf("medium") }

    var vegetarian by rememberSaveable { mutableStateOf(false) }
    var vegan by rememberSaveable { mutableStateOf(false) }
    var halal by rememberSaveable { mutableStateOf(false) }
    var kosher by rememberSaveable { mutableStateOf(false) }

    var allergiesText by rememberSaveable { mutableStateOf("") }
    var dislikesText by rememberSaveable { mutableStateOf("") }
    var favoritesText by rememberSaveable { mutableStateOf("") }

    var mealsPerDayText by rememberSaveable { mutableStateOf("3") }
    var goalText by rememberSaveable { mutableStateOf("fat_loss") }
    var goalDescriptionText by rememberSaveable { mutableStateOf("") }

    // 当 profileState 从 Firebase 载入后，同步到文本框
    LaunchedEffect(profileState) {
        profileState?.let { p ->
            heightText = if (p.heightCm == 0.0) "" else p.heightCm.toString()
            weightText = if (p.weightKg == 0.0) "" else p.weightKg.toString()
            sexText = p.sex
            ageText = p.age?.toString() ?: ""
            stepsText = p.avgDailySteps?.toString() ?: ""
            occupationText = p.occupation
            personalityText = p.personality.orEmpty()
            sleepQualityText = p.sleepQuality
            stressLevelText = p.stressLevel
            vegetarian = p.vegetarian
            vegan = p.vegan
            halal = p.halal
            kosher = p.kosher
            allergiesText = p.allergies.joinToString(", ")
            dislikesText = p.dislikes.joinToString(", ")
            favoritesText = p.favorites.joinToString(", ")
            mealsPerDayText = p.usualMealsPerDay.toString()
            goalText = p.goal
            goalDescriptionText = p.goalDescription.orEmpty()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 顶部：Back 按钮 + 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = onBack) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "User Profile",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 基本信息
        OutlinedTextField(
            value = heightText,
            onValueChange = { heightText = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = sexText,
            onValueChange = { sexText = it },
            label = { Text("Sex (e.g. male / female)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = ageText,
            onValueChange = { ageText = it },
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = stepsText,
            onValueChange = { stepsText = it },
            label = { Text("Average daily steps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = occupationText,
            onValueChange = { occupationText = it },
            label = { Text("Occupation") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personalityText,
            onValueChange = { personalityText = it },
            label = { Text("Personality (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 生活质量相关
        OutlinedTextField(
            value = sleepQualityText,
            onValueChange = { sleepQualityText = it },
            label = { Text("Sleep quality (low / medium / high)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = stressLevelText,
            onValueChange = { stressLevelText = it },
            label = { Text("Stress level (low / medium / high)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 饮食限制
        Text(text = "Dietary restrictions")
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = vegetarian, onCheckedChange = { vegetarian = it })
            Text(text = "Vegetarian")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = vegan, onCheckedChange = { vegan = it })
            Text(text = "Vegan")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = halal, onCheckedChange = { halal = it })
            Text(text = "Halal")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = kosher, onCheckedChange = { kosher = it })
            Text(text = "Kosher")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 过敏 / 不喜欢 / 喜欢
        OutlinedTextField(
            value = allergiesText,
            onValueChange = { allergiesText = it },
            label = { Text("Allergies (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dislikesText,
            onValueChange = { dislikesText = it },
            label = { Text("Dislikes (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = favoritesText,
            onValueChange = { favoritesText = it },
            label = { Text("Favorites (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 用餐 & 目标
        OutlinedTextField(
            value = mealsPerDayText,
            onValueChange = { mealsPerDayText = it },
            label = { Text("Usual meals per day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = goalText,
            onValueChange = { goalText = it },
            label = { Text("Goal (e.g. fat_loss / muscle_gain)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = goalDescriptionText,
            onValueChange = { goalDescriptionText = it },
            label = { Text("Goal description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 保存 Profile 按钮
        Button(
            onClick = {
                val profile = UserProfile(
                    heightCm = heightText.toDoubleOrNull() ?: 0.0,
                    weightKg = weightText.toDoubleOrNull() ?: 0.0,
                    sex = sexText.trim(),
                    age = ageText.toIntOrNull(),
                    avgDailySteps = stepsText.toIntOrNull(),
                    occupation = occupationText.trim(),
                    personality = personalityText.ifBlank { null },
                    sleepQuality = sleepQualityText.ifBlank { "medium" },
                    stressLevel = stressLevelText.ifBlank { "medium" },
                    vegetarian = vegetarian,
                    vegan = vegan,
                    halal = halal,
                    kosher = kosher,
                    allergies = allergiesText.toListFromInput(),
                    dislikes = dislikesText.toListFromInput(),
                    favorites = favoritesText.toListFromInput(),
                    usualMealsPerDay = mealsPerDayText.toIntOrNull() ?: 3,
                    goal = goalText.ifBlank { "fat_loss" },
                    goalDescription = goalDescriptionText.ifBlank { null }
                )

                scope.launch {
                    dietPlanViewModel.saveProfileToCloud(
                        profile = profile,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Profile saved",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { e ->
                            Toast.makeText(
                                context,
                                "Save failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Save Profile")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 登出按钮：signOut + 回到登录页
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log Out")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 把 "a, b, c" 这样的输入转换为 List<String>
private fun String.toListFromInput(): List<String> =
    this.split(',', '，', ';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
