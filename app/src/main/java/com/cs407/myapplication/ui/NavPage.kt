package com.cs407.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun NavPage() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"      // ⬅ 登录界面作为入口
    ) {
        // -------------------------
        // LOGIN
        // -------------------------
        composable("login") {
            LoginScreen(
                onLoginSuccess = { userId ->
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
                onCalendarClick = {
                    navController.navigate("calendar")
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
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
                    navController.navigate("login") {
                        popUpTo("camera") { inclusive = true }  // 清空返回栈，防止回退到已登录状态
                    }
                }
            )
        }

        // -------------------------
        // RESULT (带参数 imageUri + model)
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
            val model = backStackEntry.arguments?.getString("model")

            ResultScreen(
                imageUri = uri,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
