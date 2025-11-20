package com.cs407.myapplication.ui

import android.net.Uri
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
        composable("camera") {
            CameraScreen(
                onTakePhoto = { imageUri ->
                    val encoded = Uri.encode(imageUri)
                    navController.navigate("result?imageUri=$encoded")
                },
                onOpenGallery = { imageUri ->
                    val encoded = Uri.encode(imageUri)
                    navController.navigate("result?imageUri=$encoded")
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
            val uriEncoded = backStackEntry.arguments?.getString("imageUri")
            ResultScreen(
                imageUri = uriEncoded,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
