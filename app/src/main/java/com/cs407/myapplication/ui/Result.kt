package com.cs407.myapplication.ui

import android.net.Uri
import android.os.Build
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.myapplication.viewModels.CalorieServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: String?,
    onBack: () -> Unit,
    viewModel: CalorieServerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 把 uri -> Bitmap，只在首次拿到 uri 时做一次
    LaunchedEffect(imageUri) {
        imageUri?.let { encoded ->
            val decoded = Uri.decode(encoded)
            val uri = Uri.parse(decoded)
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                viewModel.analyze(bitmap)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
            // 显示原图（可选）
            imageUri?.let { encoded ->
                val decoded = Uri.decode(encoded)
                val uri = Uri.parse(decoded)
                val bmp = remember(uri) { loadBitmapFromUri(context, uri) }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.errorMessage?.let { msg ->
                Text("Error: $msg", color = MaterialTheme.colorScheme.error)
            }

            uiState.detections.firstOrNull()?.let { det ->
                Text("Food: ${det.label}")
                Text("Calories: ${"%.1f".format(det.caloriesKcal)} kcal")
                Text("Mass: ${"%.1f".format(det.massGrams)} g")
                Text("Protein: ${"%.1f".format(det.proteinGrams)} g")
                Text("Fat: ${"%.1f".format(det.fatGrams)} g")
                Text("Carbs: ${"%.1f".format(det.carbGrams)} g")
            }
        }
    }
}

/**
 * 工具函数：从 Uri 加载 Bitmap
 */
fun loadBitmapFromUri(
    context: android.content.Context,
    uri: Uri
): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
