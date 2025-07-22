// SimulationDemoActivity.kt - Complete Fixed Implementation
package com.plcoding.bluetoothchat.presentation.simulation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plcoding.bluetoothchat.ui.theme.BluetoothChatTheme
import com.plcoding.bluetoothchat.domain.simulation.VirtualBluetoothNode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.*

@AndroidEntryPoint
class SimulationDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothChatTheme {
                val navController = rememberNavController()
                val viewModel: SimulationViewModel = hiltViewModel()

                NavHost(
                    navController = navController,
                    startDestination = "node_selection"
                ) {
                    composable("node_selection") {
                        NodeSelectionScreen(
                            viewModel = viewModel,
                            onNavigateToChat = { navController.navigate("chat") },
                            onBackPressed = { finish() }
                        )
                    }
                    composable("chat") {
                        EnhancedChatScreen(
                            viewModel = viewModel,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NodeSelectionScreen(
    viewModel: SimulationViewModel,
    onNavigateToChat: () -> Unit,
    onBackPressed: () -> Unit
) {
    val simulationState by viewModel.simulationState.collectAsState()
    val sourceNode by viewModel.sourceNode.collectAsState()
    val destinationNode by viewModel.destinationNode.collectAsState()

    var currentStep by remember { mutableStateOf(0) } // 0: Topology, 1: Source, 2: Destination

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            0 -> "Select Network Topology"
                            1 -> "Select Source Node"
                            2 -> "Select Destination Node"
                            else -> "Setup"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onBackPressed()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colors.primary.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                }
            ) { step ->
                when (step) {
                    0 -> TopologySelectionStep(
                        onTopologySelected = { topology ->
                            when (topology) {
                                "mesh" -> viewModel.createMeshNetwork(9)
                                "chain" -> viewModel.createLinearNetwork(5)
                                "star" -> viewModel.createStarNetwork(7)
                            }
                            currentStep = 1
                        }
                    )
                    1 -> NodeSelectionStep(
                        nodes = simulationState.nodes,
                        selectedNode = sourceNode,
                        selectionType = "Source",
                        selectionColor = Color(0xFF4CAF50),
                        onNodeSelected = { node ->
                            viewModel.selectSourceNode(node)
                            currentStep = 2
                        }
                    )
                    2 -> NodeSelectionStep(
                        nodes = simulationState.nodes.filter { it.nodeId != sourceNode?.nodeId },
                        selectedNode = destinationNode,
                        selectionType = "Destination",
                        selectionColor = Color(0xFFF44336),
                        onNodeSelected = { node ->
                            viewModel.selectDestinationNode(node)
                            onNavigateToChat()
                        }
                    )
                }
            }

            // Progress Indicator
            if (currentStep > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= currentStep) MaterialTheme.colors.primary
                                    else Color.Gray.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopologySelectionStep(
    onTopologySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Choose a network topology to simulate multi-hop communication",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        )

        TopologyOption(
            title = "Mesh Network",
            description = "9 nodes arranged in a 3x3 grid pattern",
            icon = Icons.Default.Apps,
            color = Color(0xFF4CAF50),
            onClick = { onTopologySelected("mesh") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TopologyOption(
            title = "Chain Network",
            description = "5 nodes connected in a linear chain",
            icon = Icons.Default.Timeline,
            color = Color(0xFF2196F3),
            onClick = { onTopologySelected("chain") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TopologyOption(
            title = "Star Network",
            description = "Central hub with 6 peripheral nodes",
            icon = Icons.Default.StarBorder,
            color = Color(0xFFFF9800),
            onClick = { onTopologySelected("star") }
        )
    }
}

@Composable
fun TopologyOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.White
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(32.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = color
            )
        }
    }
}

@Composable
fun NodeSelectionStep(
    nodes: List<VirtualNodeState>,
    selectedNode: VirtualNodeState?,
    selectionType: String,
    selectionColor: Color,
    onNodeSelected: (VirtualNodeState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            backgroundColor = selectionColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = selectionColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Select the $selectionType node for your communication",
                    color = selectionColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Network Visualization
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            elevation = 4.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                SimpleNetworkVisualization(
                    nodes = nodes,
                    selectedNodeId = selectedNode?.nodeId,
                    selectionColor = selectionColor,
                    onNodeClick = onNodeSelected
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Node Grid
        Text(
            "Available Nodes",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(nodes) { node ->
                NodeCard(
                    node = node,
                    isSelected = selectedNode?.nodeId == node.nodeId,
                    selectionColor = selectionColor,
                    onClick = { onNodeSelected(node) }
                )
            }
        }
    }
}

@Composable
fun NodeCard(
    node: VirtualNodeState,
    isSelected: Boolean,
    selectionColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = if (isSelected) 8.dp else 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (isSelected) selectionColor.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) BorderStroke(2.dp, selectionColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) selectionColor else Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Router,
                    contentDescription = node.nodeName,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                node.nodeName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectionColor else Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                "${node.connectedNodes.size} connections",
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SimpleNetworkVisualization(
    nodes: List<VirtualNodeState>,
    selectedNodeId: String?,
    selectionColor: Color,
    onNodeClick: (VirtualNodeState) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 40f
            val maxX = nodes.maxOfOrNull { it.position.x } ?: 200.0
            val maxY = nodes.maxOfOrNull { it.position.y } ?: 200.0
            val scaleX = (width - 2 * padding) / maxX.toFloat()
            val scaleY = (height - 2 * padding) / maxY.toFloat()
            val scale = minOf(scaleX, scaleY)

            // Draw connections
            nodes.forEach { node ->
                node.connectedNodes.forEach { connectedId ->
                    val connectedNode = nodes.find { it.nodeId == connectedId }
                    if (connectedNode != null && node.nodeId < connectedId) {
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(
                                (node.position.x * scale + padding).toFloat(),
                                (node.position.y * scale + padding).toFloat()
                            ),
                            end = Offset(
                                (connectedNode.position.x * scale + padding).toFloat(),
                                (connectedNode.position.y * scale + padding).toFloat()
                            ),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }

            // Draw nodes
            nodes.forEach { node ->
                val center = Offset(
                    (node.position.x * scale + padding).toFloat(),
                    (node.position.y * scale + padding).toFloat()
                )

                val isSelected = node.nodeId == selectedNodeId

                // Selection ring
                if (isSelected) {
                    drawCircle(
                        color = selectionColor.copy(alpha = 0.3f),
                        radius = 24.dp.toPx(),
                        center = center
                    )
                }

                // Node
                drawCircle(
                    color = if (isSelected) selectionColor else Color(0xFF2196F3),
                    radius = 16.dp.toPx(),
                    center = center
                )

                // Inner circle
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = center
                )

                // Icon
                drawCircle(
                    color = if (isSelected) selectionColor else Color(0xFF2196F3),
                    radius = 6.dp.toPx(),
                    center = center
                )
            }
        }

        // Click handlers
        nodes.forEach { node ->
            val padding = 40f
            val maxX = nodes.maxOfOrNull { it.position.x } ?: 200.0
            val maxY = nodes.maxOfOrNull { it.position.y } ?: 200.0
            val scaleX = (width - 2 * padding) / maxX.toFloat()
            val scaleY = (height - 2 * padding) / maxY.toFloat()
            val scale = minOf(scaleX, scaleY)

            val nodeX = (node.position.x * scale + padding).toFloat()
            val nodeY = (node.position.y * scale + padding).toFloat()

            Box(
                modifier = Modifier
                    .offset(
                        x = (nodeX - 24).dp,
                        y = (nodeY - 24).dp
                    )
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onNodeClick(node) }
            )
        }
    }
}

@Composable
fun EnhancedChatScreen(
    viewModel: SimulationViewModel,
    onBackPressed: () -> Unit
) {
    val sourceNode by viewModel.sourceNode.collectAsState()
    val destinationNode by viewModel.destinationNode.collectAsState()
    val messageLog by viewModel.messageLog.collectAsState()
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sourceNode?.nodeName ?: "Source",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Text(
                                destinationNode?.nodeName ?: "Destination",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            "Multi-hop Communication",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = !showInfo }) {
                        Icon(
                            Icons.Default.Info,
                            "Connection Info",
                            tint = Color.White
                        )
                    }
                },
                backgroundColor = MaterialTheme.colors.primary,
                elevation = 0.dp
            )
        },
        bottomBar = {
            ChatInputBar(
                message = message,
                onMessageChange = { message = it },
                onSendClick = {
                    if (message.isNotBlank()) {
                        sourceNode?.let { source ->
                            destinationNode?.let { dest ->
                                scope.launch {
                                    viewModel.sendMessage(
                                        from = source.nodeId,
                                        to = dest.nodeId,
                                        content = message
                                    )
                                    message = ""
                                }
                            }
                        }
                    }
                },
                onAttackClick = {
                    sourceNode?.let { source ->
                        destinationNode?.let { dest ->
                            scope.launch {
                                viewModel.sendMessage(
                                    from = source.nodeId,
                                    to = dest.nodeId,
                                    content = "URGENT: Click http://malicious.site to verify account"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (messageLog.isEmpty()) {
                EmptyChatPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messageLog.reversed()) { event ->
                        ChatMessageItem(event)
                    }
                }
            }

            // Connection Info Overlay
            AnimatedVisibility(
                visible = showInfo,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                ConnectionInfoOverlay(
                    sourceNode = sourceNode,
                    destinationNode = destinationNode,
                    onDismiss = { showInfo = false }
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttackClick: () -> Unit
) {
    Surface(
        elevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attack button
            IconButton(
                onClick = onAttackClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9800))
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Test Attack",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Message field
            TextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Gray.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            IconButton(
                onClick = onSendClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (message.isNotBlank()) MaterialTheme.colors.primary
                        else Color.Gray.copy(alpha = 0.3f)
                    ),
                enabled = message.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(event: MessageEvent) {
    val alignment = when (event.type) {
        MessageEventType.SENT -> Alignment.CenterEnd
        MessageEventType.RECEIVED -> Alignment.CenterStart
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        when (event.type) {
            MessageEventType.SENT, MessageEventType.RECEIVED -> {
                MessageBubble(
                    message = event.details,
                    timestamp = event.timestamp,
                    isSent = event.type == MessageEventType.SENT
                )
            }
            MessageEventType.ATTACK_DETECTED -> {
                AttackWarningCard(event)
            }
            else -> {
                SystemMessageCard(event)
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: String,
    timestamp: String,
    isSent: Boolean
) {
    Card(
        modifier = Modifier.widthIn(max = 280.dp),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isSent) 16.dp else 4.dp,
            bottomEnd = if (isSent) 4.dp else 16.dp
        ),
        backgroundColor = if (isSent) MaterialTheme.colors.primary else Color.White,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                message,
                color = if (isSent) Color.White else Color.Black,
                fontSize = 15.sp
            )
            Text(
                timestamp,
                color = if (isSent) Color.White.copy(alpha = 0.7f) else Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AttackWarningCard(event: MessageEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        backgroundColor = Color(0xFFFF5252),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.description,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    event.details,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    event.timestamp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SystemMessageCard(event: MessageEvent) {
    Card(
        modifier = Modifier,
        backgroundColor = when (event.type) {
            MessageEventType.FORWARDED -> Color(0xFFFFF3E0)
            MessageEventType.DELIVERED -> Color(0xFFE8F5E9)
            MessageEventType.DROPPED -> Color(0xFFFFEBEE)
            else -> Color.Gray.copy(alpha = 0.1f)
        },
        shape = RoundedCornerShape(8.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (event.type) {
                    MessageEventType.FORWARDED -> Icons.Default.SwapHoriz
                    MessageEventType.DELIVERED -> Icons.Default.Check
                    MessageEventType.DROPPED -> Icons.Default.Cancel
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = when (event.type) {
                    MessageEventType.FORWARDED -> Color(0xFFFF6F00)
                    MessageEventType.DELIVERED -> Color(0xFF4CAF50)
                    MessageEventType.DROPPED -> Color(0xFFF44336)
                    else -> Color.Gray
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    event.description,
                    fontSize = 12.sp,
                    color = Color.Black.copy(alpha = 0.8f)
                )
                if (event.details.isNotEmpty()) {
                    Text(
                        event.details,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                event.timestamp,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}