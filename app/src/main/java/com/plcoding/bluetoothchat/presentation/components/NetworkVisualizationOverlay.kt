// NetworkVisualizationOverlay.kt - Fullscreen Network Visualization View
package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel

@Composable
fun NetworkVisualizationOverlayInSimulation(
    viewModel: BluetoothViewModel,
    onClose: () -> Unit
){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE000000))
            .padding(16.dp)
    ) {
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close Network View",
                tint = Color.White
            )
        }

        // Title
        Text(
            text = "Network Visualization",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )

        // Full-screen visualization
        NetworkVisualizationPanel(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp)
        )
    }
}
