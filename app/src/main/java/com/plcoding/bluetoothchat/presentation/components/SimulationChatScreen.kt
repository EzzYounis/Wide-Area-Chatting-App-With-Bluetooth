// SimulationChatScreen.kt - Enhanced Chat Screen for Simulation
package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bluetoothchat.presentation.BluetoothUiState
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimulationChatScreen(
    state: BluetoothUiState,
    viewModel: BluetoothViewModel,
    onDisconnect: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val message = rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showControlPanel by remember { mutableStateOf(false) }
    var showNetworkView by remember { mutableStateOf(false) }

    // Collect states
    val idsStatistics by viewModel.idsStatistics.collectAsState()
    val detectionExplanation by viewModel.detectionExplanation.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Enhanced Header
        SimulationChatHeader(
            idsStatistics = idsStatistics,
            onDisconnect = onDisconnect,
            onToggleControls = { showControlPanel = !showControlPanel },
            onToggleNetwork = { showNetworkView = !showNetworkView }
        )

        // Control Panel (Collapsible)
        Column {
            androidx.compose.animation.AnimatedVisibility(
                visible = showControlPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    SimulationControlPanel(viewModel)
                    HorizontalDivider()
                }
            }

            // Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(state.messages.reversed()) { message ->
                        SimulationChatMessage(message)
                    }
                }

                // Network Visualization Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showNetworkView,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    NetworkVisualizationOverlay(
                        viewModel = viewModel,
                        onClose = { showNetworkView = false }
                    )
                }

                // Detection Explanation Banner
                detectionExplanation?.let { explanation ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        explanation,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearSecurityAlert() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF1565C0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Enhanced Message Input
        SimulationMessageInput(
            message = message.value,
            onMessageChange = { message.value = it },
            onSendClick = {
                if (message.value.isNotBlank()) {
                    onSendMessage(message.value)
                    message.value = ""
                    keyboardController?.hide()
                }
            }
        )
    }
}

@Composable
fun SimulationChatHeader(
    idsStatistics: BluetoothViewModel.IDSStatistics,
    onDisconnect: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleNetwork: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.primary
    ) {
        Column {
            // Main Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Simulation Chat",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "IDS: ${idsStatistics.modelStatus}",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                // Action Buttons
                Row {
                    IconButton(onClick = onToggleNetwork) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Network View",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onToggleControls) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Controls",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // IDS Status Bar
            if (idsStatistics.totalMessages > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IDSStatusItem(
                            label = "Messages",
                            value = idsStatistics.totalMessages.toString()
                        )
                        IDSStatusItem(
                            label = "Attacks",
                            value = idsStatistics.attacksDetected.values.sum().toString()
                        )
                        IDSStatusItem(
                            label = "Rate",
                            value = "${String.format(Locale.ROOT, "%.1f", idsStatistics.messageRate)}/min"
                        )
                        IDSStatusItem(
                            label = "Detection",
                            value = "${String.format(Locale.ROOT, "%.1f", idsStatistics.detectionRate)}%"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IDSStatusItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun SimulationChatMessage(
    message: com.plcoding.bluetoothchat.domain.chat.BluetoothMessage
) {
    val isAttack = message.isAttack && !message.isFromLocalUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromLocalUser) Alignment.End else Alignment.Start
    ) {
        // Attack warning badge
        if (isAttack) {
            Card(
                colors = CardDefaults.cardColors(containerColor = when (message.attackType) {
                    "FLOODING" -> Color(0xFF9C27B0)
                    "INJECTION" -> Color(0xFFF44336)
                    "SPOOFING" -> Color(0xFFFF9800)
                    "EXPLOIT" -> Color(0xFFE91E63)
                    else -> Color.Red
                }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${message.attackType} BLOCKED (${String.format(Locale.ROOT, "%.0f", message.attackConfidence * 100)}%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Message bubble
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = if (message.isFromLocalUser) 16.dp else 4.dp,
                topEnd = if (message.isFromLocalUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = when {
                isAttack -> Color(0xFFFFEBEE)
                message.isFromLocalUser -> MaterialTheme.colorScheme.primary
                else -> Color.White
            }),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = if (isAttack) androidx.compose.foundation.BorderStroke(2.dp, Color.Red) else null
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isAttack -> Color.Red
                        message.isFromLocalUser -> Color.White.copy(alpha = 0.8f)
                        else -> Color.Gray
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    message.message,
                    color = when {
                        isAttack -> Color.Red.copy(alpha = 0.8f)
                        message.isFromLocalUser -> Color.White
                        else -> Color.Black
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationMessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    var showAttackMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Column {
            // Quick Attack Buttons
            AnimatedVisibility(visible = showAttackMenu) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QuickAttackButton("Flood", Color(0xFF9C27B0)) {
                        coroutineScope.launch {
                            // val simulator = EnhancedAttackSimulator(viewModel.bluetoothController)
                            // simulator.executeFloodingAttack(EnhancedAttackSimulator.FloodIntensity.MEDIUM)
                        }
                    }
                    QuickAttackButton("SQL", Color(0xFFF44336)) {
                        coroutineScope.launch {
                            // val simulator = EnhancedAttackSimulator(viewModel.bluetoothController)
                            // simulator.executeInjectionAttack(EnhancedAttackSimulator.InjectionType.SQL)
                        }
                    }
                    QuickAttackButton("XSS", Color(0xFF3F51B5)) {
                        coroutineScope.launch {
                            // val simulator = EnhancedAttackSimulator(viewModel.bluetoothController)
                            // simulator.executeInjectionAttack(EnhancedAttackSimulator.InjectionType.SCRIPT)
                        }
                    }
                    QuickAttackButton("Spoof", Color(0xFFFF9800)) {
                        coroutineScope.launch {
                            // val simulator = EnhancedAttackSimulator(viewModel.bluetoothController)
                            // simulator.executeSpoofingAttack()
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attack Menu Toggle
                IconButton(
                    onClick = { showAttackMenu = !showAttackMenu },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (showAttackMenu) Color.Red else Color(0xFFE0E0E0))
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Attack Menu",
                        tint = if (showAttackMenu) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Message Input
                TextField(
                    value = message,
                    onValueChange = onMessageChange,
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button
                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (message.isNotBlank()) MaterialTheme.colorScheme.primary
                            else Color(0xFFE0E0E0)
                        ),
                    enabled = message.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (message.isNotBlank()) Color.White else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAttackButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun NetworkVisualizationOverlay(
    viewModel: BluetoothViewModel,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Network Topology",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network visualization
            NetworkVisualizationPanel(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}