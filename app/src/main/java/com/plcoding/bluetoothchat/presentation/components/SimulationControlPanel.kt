// SimulationControlPanel.kt - Enhanced UI for Simulation Control
package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bluetoothchat.data.chat.SimulationBluetoothController
import com.plcoding.bluetoothchat.domain.simulation.TopologyType
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel
import kotlinx.coroutines.launch

@Composable
fun SimulationControlPanel(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    var showPanel by remember { mutableStateOf(false) }
    var selectedTopology by remember { mutableStateOf(TopologyType.MESH) }
    var nodeCount by remember { mutableStateOf(9f) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        // Toggle Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPanel = !showPanel },
            backgroundColor = MaterialTheme.colors.primary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Simulation Controls",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (showPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = showPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Network Topology Selection
                    Text(
                        "Network Topology",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TopologyButton(
                            text = "Mesh",
                            icon = Icons.Default.DateRange,
                            isSelected = selectedTopology == TopologyType.MESH,
                            onClick = { selectedTopology = TopologyType.MESH }
                        )
                        TopologyButton(
                            text = "Star",
                            icon = Icons.Default.Star,
                            isSelected = selectedTopology == TopologyType.STAR,
                            onClick = { selectedTopology = TopologyType.STAR }
                        )
                        TopologyButton(
                            text = "Linear",
                            icon = Icons.Default.List,
                            isSelected = selectedTopology == TopologyType.LINEAR,
                            onClick = { selectedTopology = TopologyType.LINEAR }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Node Count Slider
                    Text(
                        "Number of Nodes: ${nodeCount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = nodeCount,
                        onValueChange = { nodeCount = it },
                        valueRange = 3f..20f,
                        steps = 16,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                (viewModel.bluetoothController as? SimulationBluetoothController)?.changeNetworkTopology(
                                    selectedTopology,
                                    nodeCount.toInt()
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Changes")
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Network Statistics
                    SimulationStatisticsCard(viewModel)

                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    // Attack Testing Panel
                    CompactAttackTestPanel(viewModel)
                }
            }
        }
    }
}

@Composable
fun TopologyButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = if (isSelected) MaterialTheme.colors.primary else Color.LightGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = if (isSelected) Color.White else Color.DarkGray,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text,
                color = if (isSelected) Color.White else Color.DarkGray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SimulationStatisticsCard(viewModel: BluetoothViewModel) {
    val statistics = remember { mutableStateOf<SimulationBluetoothController.SimulationStatistics?>(null) }

    LaunchedEffect(Unit) {
        statistics.value = (viewModel.bluetoothController as? SimulationBluetoothController)?.getSimulationStatistics()
    }

    statistics.value?.let { stats ->
        Column {
            Text(
                "Network Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            StatisticRow("Nodes", stats.nodeCount.toString())
            StatisticRow("Connections", stats.connectionCount.toString())
            StatisticRow("Messages", stats.totalMessages.toString())
            StatisticRow("Avg Hop Count", String.format("%.1f", stats.averageHopCount))
            StatisticRow("Delivery Rate", String.format("%.1f%%", stats.deliveryRate * 100))
        }
    }
}

@Composable
fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CompactAttackTestPanel(viewModel: BluetoothViewModel) {
    val coroutineScope = rememberCoroutineScope()

    Column {
        Text(
            "Attack Testing",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompactAttackButton(
                text = "Flood",
                color = Color(0xFF9C27B0),
                onClick = {
                    coroutineScope.launch {
                        viewModel.simulateAttack(BluetoothViewModel.AttackType.FLOODING)
                    }
                }
            )
            CompactAttackButton(
                text = "Inject",
                color = Color(0xFFF44336),
                onClick = {
                    coroutineScope.launch {
                        viewModel.simulateAttack(BluetoothViewModel.AttackType.INJECTION)
                    }
                }
            )
            CompactAttackButton(
                text = "Spoof",
                color = Color(0xFFFF9800),
                onClick = {
                    coroutineScope.launch {
                        viewModel.simulateAttack(BluetoothViewModel.AttackType.SPOOFING)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    viewModel.runPerformanceTest()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Assessment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Run Performance Test")
        }
    }
}

@Composable
fun CompactAttackButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize(1f)
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        contentPadding = PaddingValues(4.dp)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

// Network Visualization Component
@Composable
fun NetworkVisualizationPanel(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // This would contain the actual network visualization
            // Using Canvas to draw nodes and connections
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
                Text(
                    "Network Visualization",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// Attack History Panel
@Composable
fun AttackHistoryPanel(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    val attackNotifications by viewModel.attackNotifications.collectAsState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Attack History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    backgroundColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text(attackNotifications.size.toString())
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (attackNotifications.isEmpty()) {
                Text(
                    "No attacks detected yet",
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                attackNotifications.take(5).forEach { notification ->
                    AttackNotificationItem(notification)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AttackNotificationItem(
    notification: BluetoothViewModel.AttackNotificationUI
) {
    Card(
        backgroundColor = when (notification.severity) {
            BluetoothViewModel.AttackSeverity.CRITICAL -> Color(0xFFFFEBEE)
            BluetoothViewModel.AttackSeverity.HIGH -> Color(0xFFFFF3E0)
            BluetoothViewModel.AttackSeverity.MEDIUM -> Color(0xFFE3F2FD)
            BluetoothViewModel.AttackSeverity.LOW -> Color(0xFFE8F5E9)
        },
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attack Type Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.attackType) {
                            "FLOODING" -> Color(0xFF9C27B0)
                            "INJECTION" -> Color(0xFFF44336)
                            "SPOOFING" -> Color(0xFFFF9800)
                            "EXPLOIT" -> Color(0xFFE91E63)
                            else -> Color.Gray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (notification.attackType) {
                        "FLOODING" -> Icons.Default.Warning
                        "INJECTION" -> Icons.Default.Code
                        "SPOOFING" -> Icons.Default.Person
                        "EXPLOIT" -> Icons.Default.Lock
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Attack Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    notification.attackType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    notification.message,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                Text(
                    "From: ${notification.deviceName}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Time
            Text(
                formatTime(System.currentTimeMillis() - notification.timestamp),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatTime(millisAgo: Long): String {
    return when {
        millisAgo < 60000 -> "${millisAgo / 1000}s"
        millisAgo < 3600000 -> "${millisAgo / 60000}m"
        else -> "${millisAgo / 3600000}h"
    }
}