package com.cs407.myapplication.ui

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    // Pass both imageUri and selected model type to the Result page
    onTakePhoto: (String, String) -> Unit = { _, _ -> },
    onOpenGallery: (String, String) -> Unit = { _, _ -> },
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    // Available model pipeline modes
    val modelOptions = listOf("pipe1", "pipe2")

    // Current selected model (default = pipe1)
    var selectedModel by remember { mutableStateOf("pipe1") }
    var modelMenuExpanded by remember { mutableStateOf(false) }

    // Menu dropdown for Calendar / Profile
    var dropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner

    // ImageCapture instance for taking photos
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Gallery picker callback — forwards (imageUri + modelType)
    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {
                onOpenGallery(it.toString(), selectedModel)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera") },

                // Left navigation: menu with Calendar + Profile
                navigationIcon = {
                    Box {
                        IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Calendar") },
                                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                                onClick = {
                                    dropdownExpanded = false
                                    onCalendarClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                onClick = {
                                    dropdownExpanded = false
                                    onProfileClick()
                                }
                            )
                        }
                    }
                },

                // Right-side actions: Model switcher + Profile icon
                actions = {
                    // ▼ Model selection dropdown (pipe1 / pipe2)
                    Box {
                        TextButton(onClick = { modelMenuExpanded = true }) {
                            Text(
                                text = if (selectedModel == "pipe1")
                                    "Calorie Model"
                                else
                                    "Food-101 Model"
                            )
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Calorie Model (pipe1)") },
                                onClick = {
                                    selectedModel = "pipe1"
                                    modelMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Food-101 Model (pipe2)") },
                                onClick = {
                                    selectedModel = "pipe2"
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                    // ▲ End model selection UI

                    IconButton(onClick = { onProfileClick() }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB3E5FC),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },

        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(70.dp),
                containerColor = Color(0xFFB3E5FC),
                tonalElevation = 8.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Take a photo → Navigate to Result with selectedModel
                    IconButton(
                        onClick = {
                            takePhoto(context, imageCapture) { uri ->
                                onTakePhoto(uri, selectedModel)
                            }
                        }
                    ) {
                        Icon(Icons.Default.PhotoCamera, "Take Photo", Modifier.size(32.dp))
                    }

                    // Open gallery → Navigate with selectedModel
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "Open Gallery", Modifier.size(32.dp))
                    }
                }
            }
        },

        content = { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                // Camera preview using AndroidView + CameraX PreviewView
                AndroidView(
                    factory = { ctx ->
                        val preview = PreviewView(ctx)

                        // Check camera permission
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            startCamera(ctx, lifecycleOwner, preview, imageCapture)
                        } else {
                            ActivityCompat.requestPermissions(
                                ctx as ComponentActivity,
                                arrayOf(Manifest.permission.CAMERA),
                                1001
                            )
                        }
                        preview
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}

/** Bind CameraX (Preview + ImageCapture) to the lifecycle */
private fun startCamera(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

/** Take a photo using CameraX ImageCapture API */
private fun takePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    onImageSaved: (String) -> Unit
) {
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        .format(System.currentTimeMillis())

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Compose")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri?.toString() ?: ""
                onImageSaved(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
