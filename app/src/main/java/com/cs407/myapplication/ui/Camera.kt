package com.cs407.myapplication.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onTakePhoto: () -> Unit = {},
    onOpenGallery: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    var expandedTop by remember { mutableStateOf(false) }
    var expandedBottom by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    var dropdownExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera") },
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
                                leadingIcon = {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onCalendarClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                onClick = {
                                    dropdownExpanded = false
                                    onProfileClick()
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onProfileClick() }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },

        bottomBar = {
            BottomAppBar(
                modifier = Modifier.height(70.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onTakePhoto) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Take Picture",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Open Gallery button
                    IconButton(onClick = onOpenGallery) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Open Gallery",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        },

        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        if (ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            startCamera(ctx, lifecycleOwner, previewView)
                        } else {
                            ActivityCompat.requestPermissions(
                                ctx as ComponentActivity,
                                arrayOf(Manifest.permission.CAMERA),
                                1001
                            )
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}

private fun startCamera(
    context: android.content.Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
    }, ContextCompat.getMainExecutor(context))
}