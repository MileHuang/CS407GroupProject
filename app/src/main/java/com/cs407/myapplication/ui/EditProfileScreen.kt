package com.cs407.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.myapplication.viewModels.UserProfile
import com.cs407.myapplication.viewModels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileState by viewModel.profile.collectAsState()

    // 启动时加载 Firebase profile
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // ------- Editable fields -------
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }

    var occupation by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var sleepQuality by remember { mutableStateOf("medium") }
    var stressLevel by remember { mutableStateOf("medium") }

    var vegetarian by remember { mutableStateOf(false) }
    var vegan by remember { mutableStateOf(false) }
    var halal by remember { mutableStateOf(false) }
    var kosher by remember { mutableStateOf(false) }

    var allergies by remember { mutableStateOf("") }
    var dislikes by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf("") }

    var mealsPerDay by remember { mutableStateOf("3") }
    var goal by remember { mutableStateOf("") }
    var goalDesc by remember { mutableStateOf("") }

    // Sync firebase data → UI fields
    LaunchedEffect(profileState) {
        profileState?.let { p ->
            height = p.heightCm.takeIf { it > 0 }?.toString() ?: ""
            weight = p.weightKg.takeIf { it > 0 }?.toString() ?: ""
            sex = p.sex
            age = p.age?.toString() ?: ""
            steps = p.avgDailySteps?.toString() ?: ""

            occupation = p.occupation
            personality = p.personality.orEmpty()
            sleepQuality = p.sleepQuality
            stressLevel = p.stressLevel
            vegetarian = p.vegetarian
            vegan = p.vegan
            halal = p.halal
            kosher = p.kosher

            allergies = p.allergies.joinToString(", ")
            dislikes = p.dislikes.joinToString(", ")
            favorites = p.favorites.joinToString(", ")

            mealsPerDay = p.usualMealsPerDay.toString()
            goal = p.goal
            goalDesc = p.goalDescription.orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // ---------- BASIC INFO ----------
            OutlinedTextField(height, { height = it }, label = { Text("Height (cm)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(sex, { sex = it }, label = { Text("Sex") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(age, { age = it }, label = { Text("Age") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(steps, { steps = it }, label = { Text("Daily Steps") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            // ---------- LIFESTYLE ----------
            OutlinedTextField(occupation, { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(personality, { personality = it }, label = { Text("Personality") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(sleepQuality, { sleepQuality = it }, label = { Text("Sleep Quality (low/medium/high)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(stressLevel, { stressLevel = it }, label = { Text("Stress Level (low/medium/high)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            // ---------- DIETARY ----------
            Text("Dietary Restrictions")
            Row { Checkbox(vegetarian, { vegetarian = it }); Text("Vegetarian") }
            Row { Checkbox(vegan, { vegan = it }); Text("Vegan") }
            Row { Checkbox(halal, { halal = it }); Text("Halal") }
            Row { Checkbox(kosher, { kosher = it }); Text("Kosher") }
            Spacer(Modifier.height(16.dp))

            // ---------- FOOD PREFERENCES ----------
            OutlinedTextField(allergies, { allergies = it }, label = { Text("Allergies") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(dislikes, { dislikes = it }, label = { Text("Dislikes") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(favorites, { favorites = it }, label = { Text("Favorites") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            // ---------- MEALS & GOAL ----------
            OutlinedTextField(mealsPerDay, { mealsPerDay = it }, label = { Text("Meals Per Day") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(goal, { goal = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(goalDesc, { goalDesc = it }, label = { Text("Goal Description") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            // ---------- SAVE BUTTON ----------
            Button(
                onClick = {
                    val p = UserProfile(
                        heightCm = height.toDoubleOrNull() ?: 0.0,
                        weightKg = weight.toDoubleOrNull() ?: 0.0,
                        sex = sex,
                        age = age.toIntOrNull(),
                        avgDailySteps = steps.toIntOrNull(),
                        occupation = occupation,
                        personality = personality.ifBlank { null },
                        sleepQuality = sleepQuality,
                        stressLevel = stressLevel,
                        vegetarian = vegetarian,
                        vegan = vegan,
                        halal = halal,
                        kosher = kosher,
                        allergies = allergies.toListFromInput(),
                        dislikes = dislikes.toListFromInput(),
                        favorites = favorites.toListFromInput(),
                        usualMealsPerDay = mealsPerDay.toIntOrNull() ?: 3,
                        goal = goal,
                        goalDescription = goalDesc.ifBlank { null }
                    )

                    viewModel.saveProfile(
                        profile = p,
                        onSuccess = {
                            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        onError = { e ->
                            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}


// Utility: convert comma-separated input to list
private fun String.toListFromInput(): List<String> =
    this.split(",", "，", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
