package com.cs407.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavPage() {
    val navController = rememberNavController()

    // 🔥 自动检测是否登录
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "camera" else "login"
    ) {
        // -------------------------
        // LOGIN
        // -------------------------
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("camera") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // -------------------------
        // CAMERA
        // -------------------------
        composable("camera") {
            CameraScreen(
                onTakePhoto = { uri, model ->
                    navController.navigate("result?imageUri=$uri&model=$model")
                },
                onOpenGallery = { uri, model ->
                    navController.navigate("result?imageUri=$uri&model=$model")
                },
                onCalendarClick = { navController.navigate("calendar") },
                onProfileClick = { navController.navigate("profile") }
            )
        }

        // -------------------------
        // CALENDAR
        // -------------------------
        composable("calendar") {
            CalendarScreen(onBack = { navController.popBackStack() })
        }

        // -------------------------
        // PROFILE
        // -------------------------
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },

                onLogout = {
                    // 🔥 真正登出 Firebase
                    FirebaseAuth.getInstance().signOut()

                    // 🔥 跳回登录页并清除返回栈
                    navController.navigate("login") {
                        popUpTo("camera") { inclusive = true }
                    }
                }
            )
        }

        // -------------------------
        // RESULT PAGE
        // -------------------------
        composable(
            route = "result?imageUri={imageUri}&model={model}",
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("model") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->

            val uri = backStackEntry.arguments?.getString("imageUri")

            ResultScreen(
                imageUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
