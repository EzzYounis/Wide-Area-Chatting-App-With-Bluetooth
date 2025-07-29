package com.plcoding.bluetoothchat.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.plcoding.bluetoothchat.presentation.components.SimulationChatScreen
import com.plcoding.bluetoothchat.presentation.simulation.SimulationDemoActivity
import com.plcoding.bluetoothchat.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BluetoothChatTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val viewModel: BluetoothViewModel = hiltViewModel()
        val state by viewModel.state.collectAsState()
        val securityAlert by viewModel.securityAlert.collectAsState()

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            when {
                state.isConnecting -> {
                    ConnectingScreen()
                }
                state.isConnected -> {
                    SimulationChatScreen(
                        state = state,
                        viewModel = viewModel,
                        onDisconnect = viewModel::disconnectFromDevice,
                        onSendMessage = viewModel::sendMessage
                    )
                }
                else -> {
                    SimulationStartScreen(
                        onStartSimulation = {
                            // Option 1: Use current activity with viewModel
                            viewModel.waitForIncomingConnections()
                        },
                        onStartDemoActivity = {
                            // Option 2: Launch separate demo activity
                            startActivity(Intent(this@MainActivity, SimulationDemoActivity::class.java))
                        }
                    )
                }
            }

            // Security Alert Dialog
            securityAlert?.let { alert ->
                SecurityAlertDialog(
                    alert = alert,
                    onDismiss = { viewModel.clearSecurityAlert() },
                    onBlockDevice = {
                        viewModel.blockDevice(alert.deviceAddress)
                        viewModel.clearSecurityAlert()
                    }
                )
            }
        }
    }
}

@Composable
fun SimulationStartScreen(
    onStartSimulation: () -> Unit,
    onStartDemoActivity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colors.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Bluetooth Mesh IDS Simulator",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Test multi-hop networking and intrusion detection",
            color = Color.Gray,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Quick Start Button
        Button(
            onClick = onStartSimulation,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quick Start", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo Mode Button
        OutlinedButton(
            onClick = onStartDemoActivity,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Advanced Demo Mode", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            backgroundColor = Color(0xFFE3F2FD),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Features:",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureItem("• Virtual Bluetooth nodes")
                FeatureItem("• Multi-hop message routing")
                FeatureItem("• Real-time attack detection")
                FeatureItem("• ML + Rule-based IDS")
                FeatureItem("• Attack simulation tools")
            }
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = Color(0xFF424242),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun ConnectingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Initializing simulation...")
    }
}

@Composable
fun SecurityAlertDialog(
    alert: BluetoothViewModel.SecurityAlertUI,
    onDismiss: () -> Unit,
    onBlockDevice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🚨 Security Alert - ${alert.attackType}",
                color = MaterialTheme.colors.error
            )
        },
        text = {
            Column {
                Text("Device: ${alert.deviceName}")
                Text("Address: ${alert.deviceAddress}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Threat Level: ${alert.severity}")
                Text("Confidence: ${String.format("%.1f", alert.confidence * 100)}%")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Message: \"${alert.message.take(100)}...\"")
                Spacer(modifier = Modifier.height(8.dp))
                Text(alert.explanation)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recommended Actions:")
                alert.recommendedActions.forEach { action ->
                    Text("• $action")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onBlockDevice,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colors.error
                )
            ) {
                Text("BLOCK DEVICE")
            }
        }
    )}