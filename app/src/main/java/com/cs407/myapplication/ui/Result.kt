package com.cs407.myapplication.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.cs407.myapplication.viewModels.CalorieServerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    imageUri: String?,
    onBack: () -> Unit,
    viewModel: CalorieServerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    // Collect observable UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // ▌Load bitmap & call analyze() only once per new URI
    LaunchedEffect(imageUri) {
        imageUri?.let { encoded ->
            val decoded = Uri.decode(encoded)
            val uri = Uri.parse(decoded)

            val bitmap = loadBitmapFromUri(context, uri)

            // Only start analysis if the bitmap loads correctly
            bitmap?.let {
                viewModel.analyze(it)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB3E5FC)
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

            // ------------------------------------------------------------
            // 1. Thumbnail Image Preview
            // ------------------------------------------------------------
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))   // Rounded corners
                        .align(Alignment.Start)
                )
            } else {
                Text("No image selected")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------------------------
            // 2. Loading State
            // ------------------------------------------------------------
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            // ------------------------------------------------------------
            // 3. Error Display
            // ------------------------------------------------------------
            uiState.errorMessage?.let { msg ->
                Text(
                    "Error: $msg",
                    color = MaterialTheme.colorScheme.error
                )
            }

            // ------------------------------------------------------------
            // 4. Nutrition Results
            // ------------------------------------------------------------
            uiState.detections.firstOrNull()?.let { det ->
                Spacer(modifier = Modifier.height(16.dp))
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
 * Utility: Load a Bitmap from a given URI.
 * Supports both legacy MediaStore and modern ImageDecoder (API 28+).
 */
fun loadBitmapFromUri(
    context: android.content.Context,
    uri: Uri
): android.graphics.Bitmap? {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
