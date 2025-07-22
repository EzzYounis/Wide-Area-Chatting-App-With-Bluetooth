// Updated MainActivity.kt
package com.plcoding.bluetoothchat.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.plcoding.bluetoothchat.presentation.components.ChatScreen
import com.plcoding.bluetoothchat.presentation.components.DeviceScreen
import com.plcoding.bluetoothchat.presentation.simulation.SimulationDemoActivity
import com.plcoding.bluetoothchat.ui.theme.BluetoothChatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: BluetoothViewModel by viewModels()

    // Preference for simulation mode
    private var isSimulationMode by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BluetoothChatTheme {
                var showModeDialog by remember { mutableStateOf(true) }

                // Mode selection dialog
                if (showModeDialog) {
                    ModeSelectionDialog(
                        onSimulationMode = {
                            isSimulationMode = true
                            showModeDialog = false
                            // Launch simulation activity
                            startActivity(Intent(this, SimulationDemoActivity::class.java))
                            finish()
                        },
                        onBluetoothMode = {
                            isSimulationMode = false
                            showModeDialog = false
                            // Continue with Bluetooth setup
                            setupBluetooth()
                        }
                    )
                } else if (!isSimulationMode) {
                    // Regular Bluetooth mode
                    BluetoothModeScreen()
                }
            }
        }
    }

    private fun setupBluetooth() {
        val bluetoothManager by lazy {
            applicationContext.getSystemService(BluetoothManager::class.java)
        }
        val bluetoothAdapter by lazy {
            bluetoothManager?.adapter
        }

        val isBluetoothEnabled: Boolean = bluetoothAdapter?.isEnabled == true

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* No action needed */ }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val canEnableBluetooth = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else true

            if(canEnableBluetooth && !isBluetoothEnabled) {
                enableBluetoothLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }
    }

    @Composable
    fun BluetoothModeScreen() {
        val viewModel = hiltViewModel<BluetoothViewModel>()
        val state by viewModel.state.collectAsState()
        val context = LocalContext.current
        val securityAlert by viewModel.securityAlert.collectAsState()

        // Show error messages
        LaunchedEffect(key1 = state.errorMessage) {
            state.errorMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        // Show connection success
        LaunchedEffect(key1 = state.isConnected) {
            if(state.isConnected) {
                Toast.makeText(context, "You're connected!", Toast.LENGTH_LONG).show()
            }
        }

        Surface(color = MaterialTheme.colors.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isConnecting -> {
                        ConnectingScreen()
                    }
                    state.isConnected -> {
                        ChatScreen(
                            state = state,
                            onDisconnect = viewModel::disconnectFromDevice,
                            onSendMessage = viewModel::sendMessage,
                            viewModel = viewModel,
                        )
                    }
                    else -> {
                        DeviceScreen(
                            state = state,
                            onStartScan = viewModel::startScan,
                            onStopScan = viewModel::stopScan,
                            onDeviceClick = viewModel::connectToDevice,
                            onStartServer = viewModel::waitForIncomingConnections
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

                // Mode switch button
                if (!state.isConnected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                startActivity(Intent(this@MainActivity, SimulationDemoActivity::class.java))
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2196F3)
                            )
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Simulation",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulation Mode")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionDialog(
    onSimulationMode: () -> Unit,
    onBluetoothMode: () -> Unit
) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colors.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Select Mode",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Choose how you want to use the app",
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Simulation Mode Button
                OutlinedButton(
                    onClick = onSimulationMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color(0xFFE3F2FD)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = Color(0xFF2196F3)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Simulation Mode",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        Text(
                            "Test multi-hop networking without devices",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bluetooth Mode Button
                Button(
                    onClick = onBluetoothMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Bluetooth Mode",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Use with real Bluetooth devices",
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    backgroundColor = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF6F00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Simulation mode allows you to test multi-hop communication without physical devices",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        }
    }
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
        Text(text = "Connecting...")
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
    )
}