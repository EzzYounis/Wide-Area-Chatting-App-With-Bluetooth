package com.plcoding.bluetoothchat.presentation.components
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Send
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel
import com.plcoding.bluetoothchat.presentation.simulation.EnhancedAttackSimulation
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun EnhancedAttackTestPanel(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var isAttackRunning by remember { mutableStateOf(false) }
    var selectedIntensity by remember { mutableStateOf(EnhancedAttackSimulation.FloodIntensity.MEDIUM) }
    var selectedInjectionType by remember { mutableStateOf(EnhancedAttackSimulation.InjectionType.MIXED) }

    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Attack Testing",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Advanced Attack Testing",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Attack status indicator
                AnimatedVisibility(visible = isAttackRunning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rotation by rememberInfiniteTransition().animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                tween(1000, easing = LinearEasing)
                            )
                        )
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Running",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Attack in Progress",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attack Categories
            AttackCategorySection(
                title = "Flooding Attacks",
                icon = Icons.Default.Warning,
                color = Color(0xFF9C27B0),
                isExpanded = expandedSection == "flooding",
                onExpandToggle = {
                    expandedSection = if (expandedSection == "flooding") null else "flooding"
                },
                content = {
                    FloodingAttackOptions(
                        selectedIntensity = selectedIntensity,
                        onIntensitySelected = { selectedIntensity = it },
                        onExecute = { intensity ->
                            coroutineScope.launch {
                                isAttackRunning = true
                                val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                                simulator.executeFloodingAttack(intensity)
                                isAttackRunning = false
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AttackCategorySection(
                title = "Injection Attacks",
                icon = Icons.Default.Code,
                color = Color(0xFFF44336),
                isExpanded = expandedSection == "injection",
                onExpandToggle = {
                    expandedSection = if (expandedSection == "injection") null else "injection"
                },
                content = {
                    InjectionAttackOptions(
                        selectedType = selectedInjectionType,
                        onTypeSelected = { selectedInjectionType = it },
                        onExecute = { type ->
                            coroutineScope.launch {
                                isAttackRunning = true
                                val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                                simulator.executeInjectionAttack(type)
                                isAttackRunning = false
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AttackCategorySection(
                title = "Spoofing Attacks",
                icon = Icons.Default.Person,
                color = Color(0xFFFF9800),
                isExpanded = expandedSection == "spoofing",
                onExpandToggle = {
                    expandedSection = if (expandedSection == "spoofing") null else "spoofing"
                },
                content = {
                    SpoofingAttackOptions(
                        onExecute = {
                            coroutineScope.launch {
                                isAttackRunning = true
                                val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                                simulator.executeSpoofingAttack()
                                isAttackRunning = false
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            AttackCategorySection(
                title = "Exploit Attacks",
                icon = Icons.Default.Lock,
                color = Color(0xFFE91E63),
                isExpanded = expandedSection == "exploit",
                onExpandToggle = {
                    expandedSection = if (expandedSection == "exploit") null else "exploit"
                },
                content = {
                    ExploitAttackOptions(
                        onExecute = {
                            coroutineScope.launch {
                                isAttackRunning = true
                                val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                                simulator.executeExploitAttack()
                                isAttackRunning = false
                            }
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Coordinated Attack Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isAttackRunning = true
                        val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                        simulator.executeCoordinatedAttack()
                        isAttackRunning = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF000000)
                ),
                enabled = !isAttackRunning
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Execute Coordinated Multi-Stage Attack",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Test Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isAttackRunning = true
                        viewModel.testIDSSystem()
                        isAttackRunning = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAttackRunning
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run IDS Test Suite")
            }
        }
    }
}

@Composable
fun AttackCategorySection(
    title: String,
    icon: ImageVector,
    color: Color,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = color.copy(alpha = 0.1f),
        elevation = 0.dp,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = color
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FloodingAttackOptions(
    selectedIntensity: EnhancedAttackSimulation.FloodIntensity,
    onIntensitySelected: (EnhancedAttackSimulation.FloodIntensity) -> Unit,
    onExecute: (EnhancedAttackSimulation.FloodIntensity) -> Unit
) {
    Column {
        Text(
            "Select Flooding Intensity",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnhancedAttackSimulation.FloodIntensity.values().forEach { intensity ->
                FilterChip(
                    selected = selectedIntensity == intensity,
                    onClick = { onIntensitySelected(intensity) },
                    modifier = Modifier.weight(1f),
                    colors = ChipDefaults.filterChipColors(
                        selectedBackgroundColor = when (intensity) {
                            EnhancedAttackSimulation.FloodIntensity.LOW -> Color(0xFF4CAF50)
                            EnhancedAttackSimulation.FloodIntensity.MEDIUM -> Color(0xFFFF9800)
                            EnhancedAttackSimulation.FloodIntensity.HIGH -> Color(0xFFF44336)
                        }
                    )
                ) {
                    Text(intensity.name, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFF5F5F5),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Attack Details:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (selectedIntensity) {
                        EnhancedAttackSimulation.FloodIntensity.LOW -> "20-30 messages at 100-300ms intervals"
                        EnhancedAttackSimulation.FloodIntensity.MEDIUM -> "70-100 messages at 50-150ms intervals"
                        EnhancedAttackSimulation.FloodIntensity.HIGH -> "150+ messages at 10-50ms intervals"
                    },
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onExecute(selectedIntensity) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF9C27B0)
            )
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute Flooding Attack")
        }
    }
}

@Composable
fun InjectionAttackOptions(
    selectedType: EnhancedAttackSimulation.InjectionType,
    onTypeSelected: (EnhancedAttackSimulation.InjectionType) -> Unit,
    onExecute: (EnhancedAttackSimulation.InjectionType) -> Unit
) {
    Column {
        Text(
            "Select Injection Type",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(EnhancedAttackSimulation.InjectionType.values().toList()) { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedType == type) Color(0xFFFFEBEE) else Color.Transparent
                        )
                        .clickable { onTypeSelected(type) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFF44336)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            type.name,
                            fontSize = 14.sp,
                            fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            getInjectionTypeDescription(type),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onExecute(selectedType) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFF44336)
            )
        ) {
            Icon(Icons.Default.Code, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute Injection Attack")
        }
    }
}

@Composable
fun SpoofingAttackOptions(
    onExecute: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFFFF3E0),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Spoofing Attack Patterns:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                listOf(
                    "• Financial scams with fake URLs",
                    "• Account security alerts",
                    "• Admin/System impersonation",
                    "• Social engineering attempts"
                ).forEach { pattern ->
                    Text(
                        pattern,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onExecute,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFFF9800)
            )
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute Spoofing Attack")
        }
    }
}

@Composable
fun ExploitAttackOptions(
    onExecute: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFFFE1E1),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Exploit Patterns Include:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                listOf(
                    "• Buffer overflow attempts",
                    "• Format string attacks",
                    "• Bluetooth AT commands",
                    "• Binary payload injection"
                ).forEach { pattern ->
                    Text(
                        pattern,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onExecute,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFFE91E63)
            )
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Execute Exploit Attack")
        }
    }
}

private fun getInjectionTypeDescription(type: EnhancedAttackSimulation.InjectionType): String {
    return when (type) {
        EnhancedAttackSimulation.InjectionType.SQL -> "Database query manipulation"
        EnhancedAttackSimulation.InjectionType.COMMAND -> "System command execution"
        EnhancedAttackSimulation.InjectionType.SCRIPT -> "Client-side script injection"
        EnhancedAttackSimulation.InjectionType.JSON -> "NoSQL/JSON payload attacks"
        EnhancedAttackSimulation.InjectionType.LDAP -> "Directory service attacks"
        EnhancedAttackSimulation.InjectionType.XML -> "XML parser exploitation"
        EnhancedAttackSimulation.InjectionType.MIXED -> "Combined injection techniques"
    }
}

// Quick Attack Buttons for Chat Screen
@Composable
fun QuickAttackBar(
    viewModel: BluetoothViewModel,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Flooding button
        IconButton(
            onClick = {
                viewModel.simulateAttack(BluetoothViewModel.AttackType.FLOODING)
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Flooding",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(20.dp)
            )
        }

        // Injection button
        IconButton(
            onClick = {
                viewModel.simulateAttack(BluetoothViewModel.AttackType.INJECTION)
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF44336).copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = "Injection",
                tint = Color(0xFFF44336),
                modifier = Modifier.size(20.dp)
            )
        }

        // Spoofing button
        IconButton(
            onClick = {
                viewModel.simulateAttack(BluetoothViewModel.AttackType.SPOOFING)
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9800).copy(alpha = 0.1f))
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Spoofing",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // More options
        Box {
            IconButton(
                onClick = { showMenu = !showMenu }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More attacks"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(onClick = {
                    coroutineScope.launch {
                        val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                        simulator.executeExploitAttack()
                    }
                    showMenu = false
                }) {
                    Row {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFE91E63)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exploit Attack")
                    }
                }

                DropdownMenuItem(onClick = {
                    coroutineScope.launch {
                        val simulator = EnhancedAttackSimulation(viewModel, viewModel.bluetoothController)
                        simulator.executeCoordinatedAttack()
                    }
                    showMenu = false
                }) {
                    Row {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coordinated Attack")
                    }
                }

                Divider()

                DropdownMenuItem(onClick = {
                    viewModel.runPerformanceTest()
                    showMenu = false
                }) {
                    Row {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Performance Test")
                    }
                }
            }
        }
    }
}