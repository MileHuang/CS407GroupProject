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
        startDestination = "camera"
    ) {
        // Camera page
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
        composable("calendar") {
            CalendarScreen(onBack = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "result?imageUri={imageUri}",
            arguments = listOf(
                navArgument("imageUri") {
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
