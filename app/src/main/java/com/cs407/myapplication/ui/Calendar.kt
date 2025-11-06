package com.cs407.myapplication.ui

import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    var selectedDate by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB3E5FC),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (selectedDate.isEmpty()) "Select a date:" else "Selected: $selectedDate",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            AndroidView(
                factory = { CalendarView(context) },
                update = { view ->
                    view.setOnDateChangeListener { _, year, month, day ->
                        selectedDate = "${month + 1}/$day/$year"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { /* TODO: future feature - save selection */ }) {
                Text("Confirm Date")
            }
        }
    }
}
