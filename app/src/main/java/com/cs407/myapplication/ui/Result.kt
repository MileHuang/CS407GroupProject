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
    val uiState by viewModel.uiState.collectAsState()

    var selectedModel by remember { mutableStateOf("Food Calorie Model") }
    var expanded by remember { mutableStateOf(false) }

    // Load bitmap & analyze once
    LaunchedEffect(imageUri) {
        imageUri?.let { encoded ->
            val decoded = Uri.decode(encoded)
            val uri = Uri.parse(decoded)
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                // Tony’s ViewModel expects a Bitmap only
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

            // 📌 Thumbnail Image
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(Uri.parse(imageUri)),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .align(Alignment.Start)
                )
            } else {
                Text("No image selected")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 📌 Model Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Model Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Food Calorie Model", "Nutrition Model", "Fitness Model").forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                selectedModel = model
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 📌 Analyze Button (does nothing special yet)
            Button(onClick = { /* 可以添加基于 model 的行为 */ }) {
                Text("Analyze with $selectedModel")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 📌 LOADING
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            // 📌 ERROR
            uiState.errorMessage?.let { msg ->
                Text("Error: $msg", color = MaterialTheme.colorScheme.error)
            }

            // 📌 Analysis Result
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
