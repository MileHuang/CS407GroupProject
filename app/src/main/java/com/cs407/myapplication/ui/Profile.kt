package com.cs407.myapplication.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.myapplication.viewModels.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val profileState by viewModel.profile.collectAsState()

    // Load data
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                FirebaseAuth.getInstance().currentUser?.email ?: "User",
                fontSize = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            // Profile info
            profileState?.let { p ->

                ProfileItem("Height", "${p.heightCm} cm")
                ProfileItem("Weight", "${p.weightKg} kg")
                ProfileItem("Sex", p.sex)
                ProfileItem("Age", p.age?.toString() ?: "—")
                ProfileItem("Daily Steps", p.avgDailySteps?.toString() ?: "—")

                ProfileItem("Occupation", p.occupation)
                ProfileItem("Personality", p.personality ?: "—")
                ProfileItem("Sleep Quality", p.sleepQuality)
                ProfileItem("Stress Level", p.stressLevel)

                ProfileItem("Vegetarian", p.vegetarian.toString())
                ProfileItem("Vegan", p.vegan.toString())
                ProfileItem("Halal", p.halal.toString())
                ProfileItem("Kosher", p.kosher.toString())

                ProfileItem("Allergies", p.allergies.joinToString(", "))
                ProfileItem("Dislikes", p.dislikes.joinToString(", "))
                ProfileItem("Favorites", p.favorites.joinToString(", "))

                ProfileItem("Meals per Day", p.usualMealsPerDay.toString())

                ProfileItem("Goal", p.goal)
                ProfileItem("Goal Description", p.goalDescription ?: "—")


            } ?: Text("No profile data", color = Color.Gray)

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Log Out")
            }
        }
    }
}


@Composable
fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

