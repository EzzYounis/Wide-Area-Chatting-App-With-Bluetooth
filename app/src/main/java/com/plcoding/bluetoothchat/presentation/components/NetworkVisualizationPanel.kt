// NetworkVisualizationPanel.kt - Network Topology Visualization
package com.plcoding.bluetoothchat.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.plcoding.bluetoothchat.data.chat.SimulationBluetoothController
import com.plcoding.bluetoothchat.domain.simulation.*
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun BluetoothNetworkVisualizationPanel(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    var showVisualization by remember { mutableStateOf(false) }
    var selectedNode by remember { mutableStateOf<String?>(null) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()

    // Collect network topology from simulation
    val networkTopology by remember {
        derivedStateOf {
            (viewModel.bluetoothController as? SimulationBluetoothController)
                ?.getNetworkTopology() ?: SimulationEngine.NetworkTopology()
        }
    }

    // Animation states
    val pulseAnimation = rememberInfiniteTransition()
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(modifier = modifier) {
        // Toggle Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showVisualization = !showVisualization },
            backgroundColor = MaterialTheme.colors.secondary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Network Visualization",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (showVisualization) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        AnimatedVisibility(
            visible = showVisualization,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column {
                    // Visualization Controls
                    VisualizationControls(
                        zoomLevel = zoomLevel,
                        onZoomChange = { zoomLevel = it },
                        onResetView = {
                            zoomLevel = 1f
                            panOffset = Offset.Zero
                        },
                        selectedNode = selectedNode,
                        onNodeDeselect = { selectedNode = null }
                    )

                    Divider()

                    // Network Canvas
                    Box(modifier = Modifier.fillMaxSize()) {
                        NetworkCanvas(
                            topology = networkTopology,
                            zoomLevel = zoomLevel,
                            panOffset = panOffset,
                            onPanChange = { panOffset = it },
                            onZoomChange = { zoomLevel = it },
                            selectedNode = selectedNode,
                            onNodeSelect = { selectedNode = it },
                            pulseAlpha = pulseAlpha,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Node Info Overlay
                        selectedNode?.let { nodeId ->
                            val node = networkTopology.nodes.find { it.id == nodeId }
                            node?.let {
                                NodeInfoOverlay(
                                    nodeInfo = it,
                                    connections = networkTopology.connections.filter { conn ->
                                        conn.node1 == nodeId || conn.node2 == nodeId
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisualizationControls(
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    onResetView: () -> Unit,
    selectedNode: String?,
    onNodeDeselect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onZoomChange(maxOf(0.5f, zoomLevel - 0.2f)) }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
            Text(
                "${(zoomLevel * 100).toInt()}%",
                fontSize = 12.sp,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onZoomChange(minOf(3f, zoomLevel + 0.2f)) }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }
        }

        if (selectedNode != null) {
            OutlinedButton(
                onClick = onNodeDeselect,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Deselect", fontSize = 12.sp)
            }
        }

        OutlinedButton(
            onClick = onResetView,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset", fontSize = 12.sp)
        }
    }
}

@Composable
fun NetworkCanvas(
    topology: SimulationEngine.NetworkTopology,
    zoomLevel: Float,
    panOffset: Offset,
    onPanChange: (Offset) -> Unit,
    onZoomChange: (Float) -> Unit,
    selectedNode: String?,
    onNodeSelect: (String?) -> Unit,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onZoomChange((zoomLevel * zoom).coerceIn(0.5f, 3f))
                    onPanChange(panOffset + pan)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val canvasSize = size
                    val adjustedOffset = (offset - panOffset) / zoomLevel

                    // Check if tap hit a node
                    val hitNode = topology.nodes.find { node ->
                        val nodePos = nodeToCanvasPosition(
                            node.position,
                            canvasSize.width.toFloat(),
                            canvasSize.height.toFloat()
                        )
                        val distance = sqrt(
                            (adjustedOffset.x - nodePos.x).pow(2) +
                            (adjustedOffset.y - nodePos.y).pow(2)
                        )
                        distance <= 30f // Node radius
                    }

                    onNodeSelect(hitNode?.id)
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Apply transformations
        translate(panOffset.x, panOffset.y) {
            scale(zoomLevel) {
                // Draw connections first (behind nodes)
                topology.connections.forEach { connection ->
                    val node1 = topology.nodes.find { it.id == connection.node1 }
                    val node2 = topology.nodes.find { it.id == connection.node2 }

                    if (node1 != null && node2 != null) {
                        drawConnection(
                            node1 = node1,
                            node2 = node2,
                            connection = connection,
                            canvasWidth = canvasWidth,
                            canvasHeight = canvasHeight,
                            selectedNode = selectedNode,
                            pulseAlpha = pulseAlpha
                        )
                    }
                }

                // Draw nodes on top
                topology.nodes.forEach { node ->
                    drawNode(
                        node = node,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        isSelected = node.id == selectedNode,
                        pulseAlpha = pulseAlpha
                    )
                }
            }
        }
    }
}

fun DrawScope.drawConnection(
    node1: SimulationEngine.NodeInfo,
    node2: SimulationEngine.NodeInfo,
    connection: SimulationEngine.ConnectionInfo,
    canvasWidth: Float,
    canvasHeight: Float,
    selectedNode: String?,
    pulseAlpha: Float
) {
    val pos1 = nodeToCanvasPosition(node1.position, canvasWidth, canvasHeight)
    val pos2 = nodeToCanvasPosition(node2.position, canvasWidth, canvasHeight)

    val isHighlighted = selectedNode == node1.id || selectedNode == node2.id

    val connectionColor = when (connection.quality) {
        SimulationEngine.ConnectionQuality.EXCELLENT -> Color.Green
        SimulationEngine.ConnectionQuality.GOOD -> Color.Blue
        SimulationEngine.ConnectionQuality.FAIR -> Color.Yellow
        SimulationEngine.ConnectionQuality.POOR -> Color.Red
    }

    val strokeWidth = if (isHighlighted) 4f else 2f
    val alpha = if (isHighlighted) pulseAlpha else 0.7f

    drawLine(
        color = connectionColor,
        start = pos1,
        end = pos2,
        strokeWidth = strokeWidth,
        alpha = alpha
    )

    // Draw signal strength indicator
    if (isHighlighted) {
        val midPoint = Offset(
            (pos1.x + pos2.x) / 2,
            (pos1.y + pos2.y) / 2
        )

        drawCircle(
            color = connectionColor,
            radius = 4f,
            center = midPoint,
            alpha = pulseAlpha
        )
    }
}

fun DrawScope.drawNode(
    node: SimulationEngine.NodeInfo,
    canvasWidth: Float,
    canvasHeight: Float,
    isSelected: Boolean,
    pulseAlpha: Float
) {
    val position = nodeToCanvasPosition(node.position, canvasWidth, canvasHeight)

    // Node colors based on type and state
    val nodeColor = when {
        node.isGateway -> Color.Red
        node.batteryLevel < 20 -> Color.Yellow
        else -> Color.Blue
    }

    val nodeRadius = if (isSelected) 35f else 25f
    val strokeWidth = if (isSelected) 4f else 2f

    // Draw selection ring
    if (isSelected) {
        drawCircle(
            color = nodeColor,
            radius = nodeRadius + 10f,
            center = position,
            alpha = pulseAlpha * 0.3f,
            style = Stroke(width = 2f)
        )
    }

    // Draw main node circle
    drawCircle(
        color = nodeColor,
        radius = nodeRadius,
        center = position,
        alpha = 0.8f
    )

    // Draw node border
    drawCircle(
        color = Color.White,
        radius = nodeRadius,
        center = position,
        style = Stroke(width = strokeWidth)
    )

    // Draw battery indicator
    if (node.batteryLevel < 100) {
        val batteryHeight = 8f
        val batteryWidth = 20f
        val batteryLevel = (node.batteryLevel / 100f) * batteryWidth

        // Battery outline
        drawRect(
            color = Color.White,
            topLeft = Offset(position.x - batteryWidth/2, position.y + nodeRadius + 8f),
            size = Size(batteryWidth, batteryHeight),
            style = Stroke(width = 1f)
        )

        // Battery level
        val batteryColor = when {
            node.batteryLevel < 20 -> Color.Red
            node.batteryLevel < 50 -> Color.Yellow
            else -> Color.Green
        }

        drawRect(
            color = batteryColor,
            topLeft = Offset(position.x - batteryWidth/2, position.y + nodeRadius + 8f),
            size = Size(batteryLevel, batteryHeight)
        )
    }

    // Draw message count indicator
    if (node.messageCount > 0) {
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(position.x + nodeRadius - 8f, position.y - nodeRadius + 8f)
        )
        drawCircle(
            color = Color.Red,
            radius = 6f,
            center = Offset(position.x + nodeRadius - 8f, position.y - nodeRadius + 8f)
        )
    }
}

@Composable
fun NodeInfoOverlay(
    nodeInfo: SimulationEngine.NodeInfo,
    connections: List<SimulationEngine.ConnectionInfo>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(200.dp),
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                nodeInfo.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "ID: ${nodeInfo.id}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Battery:", fontSize = 12.sp)
                Text("${nodeInfo.batteryLevel}%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Messages:", fontSize = 12.sp)
                Text("${nodeInfo.messageCount}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Connections:", fontSize = 12.sp)
                Text("${connections.size}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            if (nodeInfo.isGateway) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "GATEWAY NODE",
                    fontSize = 10.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            if (connections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connections:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                connections.take(3).forEach { connection ->
                    val otherNode = if (connection.node1 == nodeInfo.id) connection.node2 else connection.node1
                    val qualityColor = when (connection.quality) {
                        SimulationEngine.ConnectionQuality.EXCELLENT -> Color.Green
                        SimulationEngine.ConnectionQuality.GOOD -> Color.Blue
                        SimulationEngine.ConnectionQuality.FAIR -> Color.Yellow
                        SimulationEngine.ConnectionQuality.POOR -> Color.Red
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(otherNode, fontSize = 10.sp)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(qualityColor, CircleShape)
                        )
                    }
                }
                if (connections.size > 3) {
                    Text("...and ${connections.size - 3} more", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

private fun nodeToCanvasPosition(
    nodePosition: VirtualBluetoothNode.Position,
    canvasWidth: Float,
    canvasHeight: Float
): Offset {
    // Convert node position (0-1 range) to canvas coordinates
    // Add padding to keep nodes away from edges
    val padding = 50f
    val usableWidth = canvasWidth - (padding * 2)
    val usableHeight = canvasHeight - (padding * 2)

    return Offset(
        x = padding + (nodePosition.x.toFloat() * usableWidth),
        y = padding + (nodePosition.y.toFloat() * usableHeight)
    )
}
