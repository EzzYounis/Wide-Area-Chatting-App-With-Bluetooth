// SimulationViewModel.kt - Complete Final Implementation with Research Features
package com.plcoding.bluetoothchat.presentation.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.bluetoothchat.domain.simulation.*
import com.plcoding.bluetoothchat.presentation.IDS.IDSModel
import com.plcoding.bluetoothchat.presentation.IDS.IDSPerformanceTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import java.io.File
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val idsModel: IDSModel
) : ViewModel() {

    private val simulationEngine = SimulationEngine()

    // Basic simulation state
    private val _simulationState = MutableStateFlow(SimulationUiState())
    val simulationState: StateFlow<SimulationUiState> = _simulationState.asStateFlow()

    private val _selectedNode = MutableStateFlow<VirtualNodeState?>(null)
    val selectedNode: StateFlow<VirtualNodeState?> = _selectedNode.asStateFlow()

    private val _sourceNode = MutableStateFlow<VirtualNodeState?>(null)
    val sourceNode: StateFlow<VirtualNodeState?> = _sourceNode.asStateFlow()

    private val _destinationNode = MutableStateFlow<VirtualNodeState?>(null)
    val destinationNode: StateFlow<VirtualNodeState?> = _destinationNode.asStateFlow()

    private val _messageLog = MutableStateFlow<List<MessageEvent>>(emptyList())
    val messageLog: StateFlow<List<MessageEvent>> = _messageLog.asStateFlow()

    // Attack simulation state
    private val _isUnderAttack = MutableStateFlow(false)
    val isUnderAttack: StateFlow<Boolean> = _isUnderAttack.asStateFlow()

    private val _attackStats = MutableStateFlow(AttackStatistics())
    val attackStats: StateFlow<AttackStatistics> = _attackStats.asStateFlow()

    private val _nodeSecurityStates = MutableStateFlow<Map<String, NodeSecurityState>>(emptyMap())
    val nodeSecurityStates: StateFlow<Map<String, NodeSecurityState>> = _nodeSecurityStates.asStateFlow()

    // Research-specific data collection
    private val _researchMetrics = MutableStateFlow(ResearchMetrics())
    val researchMetrics: StateFlow<ResearchMetrics> = _researchMetrics.asStateFlow()

    private val _experimentResults = MutableStateFlow<List<ExperimentResult>>(emptyList())
    val experimentResults: StateFlow<List<ExperimentResult>> = _experimentResults.asStateFlow()

    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Data classes for attack simulation
    data class AttackStatistics(
        val totalAttacks: Int = 0,
        val blockedAttacks: Int = 0,
        val detectionRate: Float = 0f,
        val attackTypes: Map<String, Int> = emptyMap()
    )

    data class NodeSecurityState(
        val nodeId: String,
        val isCompromised: Boolean = false,
        val attackCount: Int = 0,
        val lastAttackType: String? = null,
        val securityLevel: SecurityLevel = SecurityLevel.SAFE
    )

    enum class SecurityLevel {
        SAFE, WARNING, DANGER, COMPROMISED
    }

    // Research Metrics Data Class
    data class ResearchMetrics(
        // Detection Performance Metrics
        val truePositives: Int = 0,
        val trueNegatives: Int = 0,
        val falsePositives: Int = 0,
        val falseNegatives: Int = 0,

        // Calculated Metrics
        val accuracy: Float = 0f,
        val precision: Float = 0f,
        val recall: Float = 0f,
        val f1Score: Float = 0f,
        val specificity: Float = 0f,

        // Network Performance Metrics
        val averageDetectionTime: Long = 0L,
        val maxDetectionTime: Long = 0L,
        val minDetectionTime: Long = Long.MAX_VALUE,
        val detectionTimeStdDev: Double = 0.0,

        // Multi-hop Metrics
        val averageHopCount: Double = 0.0,
        val maxHopCount: Int = 0,
        val messageDeliveryRate: Float = 0f,
        val routingEfficiency: Float = 0f,

        // Resource Consumption
        val averageMemoryUsage: Long = 0L,
        val peakMemoryUsage: Long = 0L,
        val averageCpuUsage: Float = 0f,
        val batteryImpact: Float = 0f,

        // Attack-specific Metrics
        val detectionRateByType: Map<String, Float> = emptyMap(),
        val averageConfidenceByType: Map<String, Float> = emptyMap(),
        val responseTimeByType: Map<String, Long> = emptyMap(),

        // Hybrid Approach Metrics
        val mlOnlyDetections: Int = 0,
        val ruleBasedOnlyDetections: Int = 0,
        val hybridDetections: Int = 0,
        val mlAccuracy: Float = 0f,
        val ruleBasedAccuracy: Float = 0f,
        val hybridAccuracy: Float = 0f
    )

    // Experiment Result for Comparative Analysis
    data class ExperimentResult(
        val experimentId: String,
        val timestamp: Long,
        val networkTopology: String,
        val nodeCount: Int,
        val attackType: String,
        val attackVolume: Int,
        val detectionRate: Float,
        val falsePositiveRate: Float,
        val averageLatency: Long,
        val hopCount: Int,
        val detectionMethod: String,
        val confidence: Float,
        val resourceUsage: ResourceMetrics
    )

    data class ResourceMetrics(
        val cpuUsage: Float,
        val memoryUsage: Long,
        val batteryDrain: Float
    )

    // Predefined attack patterns
    private val attackPatterns = listOf(
        AttackPattern(
            name = "Phishing Attack",
            messages = listOf(
                "URGENT: Your account will be suspended! Click http://malicious.site",
                "Security Alert: Verify your password at http://fake-bank.com",
                "You won $1000! Claim at http://prize-scam.com",
                "Update your credentials immediately at http://phishing.link"
            ),
            type = "SPOOFING"
        ),
        AttackPattern(
            name = "Code Injection",
            messages = listOf(
                "{ \"command\": \"delete_files\", \"target\": \"*\" }",
                "<script>alert('XSS')</script>",
                "'; DROP TABLE users; --",
                "exec('rm -rf /')"
            ),
            type = "INJECTION"
        ),
        AttackPattern(
            name = "DDoS Flooding",
            messages = listOf(
                "FLOOD_${System.currentTimeMillis()}_1",
                "FLOOD_${System.currentTimeMillis()}_2",
                "SPAM SPAM SPAM SPAM SPAM",
                "A".repeat(1000)
            ),
            type = "FLOODING"
        ),
        AttackPattern(
            name = "Exploit Attempt",
            messages = listOf(
                "\\x01\\x02\\x03\\x04\\x05\\x06",
                "AT+FACTORYRESET",
                "../../../etc/passwd",
                "%n%n%n%n"
            ),
            type = "EXPLOIT"
        )
    )

    data class AttackPattern(
        val name: String,
        val messages: List<String>,
        val type: String
    )

    enum class ExperimentType {
        DETECTION_ACCURACY,
        SCALABILITY_TEST,
        MULTI_HOP_PERFORMANCE,
        RESOURCE_CONSUMPTION,
        HYBRID_COMPARISON,
        ATTACK_VARIETY
    }

    init {
        // Observe simulation state
        viewModelScope.launch {
            simulationEngine.simulationState.collect { state ->
                updateUiState(state)
            }
        }

        // Observe network events
        viewModelScope.launch {
            simulationEngine.networkEvents.collect { event ->
                handleNetworkEvent(event)
            }
        }

        // Create default network
        createMeshNetwork(6)
    }

    // Network creation methods
    fun createMeshNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.MESH,
                nodeCount = nodeCount
            )
        )
        resetSelections()
    }

    fun createLinearNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.LINEAR,
                nodeCount = nodeCount
            )
        )
        resetSelections()
    }

    fun createStarNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.STAR,
                nodeCount = nodeCount
            )
        )
        resetSelections()
    }

    fun clearNetwork() {
        simulationEngine.clearSimulation()
        resetSelections()
    }

    private fun resetSelections() {
        _selectedNode.value = null
        _sourceNode.value = null
        _destinationNode.value = null
        _messageLog.value = emptyList()
        _nodeSecurityStates.value = emptyMap()
        _attackStats.value = AttackStatistics()
        _isUnderAttack.value = false
    }

    // Node selection methods
    fun selectNode(node: VirtualNodeState) {
        _selectedNode.value = node
        subscribeToNodeMessages(node.nodeId)
    }

    fun selectSourceNode(node: VirtualNodeState) {
        _sourceNode.value = node
        subscribeToNodeMessages(node.nodeId)
    }

    fun selectDestinationNode(node: VirtualNodeState) {
        _destinationNode.value = node
    }

    private fun subscribeToNodeMessages(nodeId: String) {
        val virtualNode = simulationEngine.getNode(nodeId) ?: return

        viewModelScope.launch {
            virtualNode.incomingMessages.collect { message ->
                handleIncomingMessage(virtualNode, message)
            }
        }
    }

    // Message sending
    suspend fun sendMessage(from: String, to: String, content: String) {
        val sourceNode = simulationEngine.getNode(from) ?: return
        val targetNode = simulationEngine.getNode(to) ?: return

        // Clear old messages if too many
        if (_messageLog.value.size > 50) {
            _messageLog.value = _messageLog.value.takeLast(30)
        }

        // Log sending event
        addMessageEvent(
            MessageEvent(
                type = MessageEventType.SENT,
                description = "${sourceNode.nodeName} → ${targetNode.nodeName}",
                details = content,
                timestamp = dateFormatter.format(Date())
            )
        )

        // Send through simulation
        val success = sourceNode.sendMessage(to, content)

        if (!success) {
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.DROPPED,
                    description = "Failed to send from ${sourceNode.nodeName}",
                    details = "No route to destination",
                    timestamp = dateFormatter.format(Date())
                )
            )
        }
    }

    // Attack simulation methods
    fun simulateRandomAttack() {
        viewModelScope.launch {
            val sourceNode = _sourceNode.value ?: return@launch
            val destinationNode = _destinationNode.value ?: return@launch

            val attackPattern = attackPatterns.random()

            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.ATTACK_INITIATED,
                    description = "⚠️ ${attackPattern.name} Started",
                    details = "Attacker attempting to compromise the network",
                    timestamp = dateFormatter.format(Date())
                )
            )

            _isUnderAttack.value = true

            // Send attack messages
            attackPattern.messages.forEach { message ->
                delay(500)
                sendAttackMessage(sourceNode.nodeId, destinationNode.nodeId, message, attackPattern.type)
            }

            _isUnderAttack.value = false
            updateAttackStatistics()
        }
    }

    fun simulateTargetedAttack(attackType: String) {
        viewModelScope.launch {
            val sourceNode = _sourceNode.value ?: return@launch
            val destinationNode = _destinationNode.value ?: return@launch

            val pattern = attackPatterns.find { it.type == attackType } ?: attackPatterns.random()

            _isUnderAttack.value = true

            pattern.messages.forEach { message ->
                delay(300)
                sendAttackMessage(sourceNode.nodeId, destinationNode.nodeId, message, pattern.type)
            }

            _isUnderAttack.value = false
            updateAttackStatistics()
        }
    }

    private suspend fun sendAttackMessage(from: String, to: String, content: String, expectedType: String) {
        val sourceNode = simulationEngine.getNode(from) ?: return
        val targetNode = simulationEngine.getNode(to) ?: return

        // Log attack attempt
        addMessageEvent(
            MessageEvent(
                type = MessageEventType.SENT,
                description = "${sourceNode.nodeName} → ${targetNode.nodeName}",
                details = content,
                timestamp = dateFormatter.format(Date()),
                isAttack = true
            )
        )

        // Send through simulation
        val success = sourceNode.sendMessage(to, content)

        if (!success) {
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.DROPPED,
                    description = "Attack packet dropped",
                    details = "No route to target",
                    timestamp = dateFormatter.format(Date())
                )
            )
        }
    }

    fun simulateMultiNodeAttack() {
        viewModelScope.launch {
            val nodes = _simulationState.value.nodes
            if (nodes.size < 3) return@launch

            // Select random attacker and multiple targets
            val attacker = nodes.random()
            val targets = nodes.filter { it.nodeId != attacker.nodeId }.take(3)

            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.ATTACK_INITIATED,
                    description = "🚨 Multi-Node Attack Detected",
                    details = "${attacker.nodeName} attacking multiple nodes",
                    timestamp = dateFormatter.format(Date())
                )
            )

            _isUnderAttack.value = true

            // Attack each target
            targets.forEach { target ->
                val attackPattern = attackPatterns.random()
                attackPattern.messages.take(2).forEach { message ->
                    delay(200)
                    sendAttackMessage(attacker.nodeId, target.nodeId, message, attackPattern.type)
                }
            }

            _isUnderAttack.value = false
            updateAttackStatistics()
        }
    }

    fun simulateCompromisedNode(nodeId: String) {
        viewModelScope.launch {
            val node = simulationEngine.getNode(nodeId) ?: return@launch

            // Mark node as compromised
            updateNodeSecurityState(nodeId) { state ->
                state.copy(
                    isCompromised = true,
                    securityLevel = SecurityLevel.COMPROMISED
                )
            }

            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.NODE_COMPROMISED,
                    description = "💀 Node Compromised: ${node.nodeName}",
                    details = "This node is now under attacker control",
                    timestamp = dateFormatter.format(Date())
                )
            )

            // Compromised node starts attacking others
            repeat(5) {
                delay(1000)
                val targets = _simulationState.value.nodes.filter { it.nodeId != nodeId }
                if (targets.isNotEmpty()) {
                    val target = targets.random()
                    val attack = attackPatterns.random()
                    sendAttackMessage(nodeId, target.nodeId, attack.messages.first(), attack.type)
                }
            }
        }
    }

    // Message handling
    private suspend fun handleIncomingMessage(node: VirtualBluetoothNode, message: SimulatedMessage) {
        // Only process messages for the destination node in chat mode
        if (_destinationNode.value != null && node.nodeId != _destinationNode.value?.nodeId) {
            return
        }

        // Run IDS analysis
        val startTime = System.currentTimeMillis()
        val idsResult = idsModel.analyzeMessage(
            message = message.content,
            fromDevice = message.source,
            toDevice = node.nodeId,
            direction = "INCOMING"
        )
        val detectionTime = System.currentTimeMillis() - startTime

        if (idsResult.isAttack) {
            // Update node security state
            updateNodeSecurityState(node.nodeId) { state ->
                val newAttackCount = state.attackCount + 1
                state.copy(
                    attackCount = newAttackCount,
                    lastAttackType = idsResult.attackType,
                    securityLevel = when {
                        newAttackCount > 10 -> SecurityLevel.DANGER
                        newAttackCount > 5 -> SecurityLevel.WARNING
                        else -> SecurityLevel.SAFE
                    }
                )
            }

            // Log attack detection
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.ATTACK_DETECTED,
                    description = "🛡️ ${idsResult.attackType} BLOCKED",
                    details = """
                        From: ${getNodeName(message.source)}
                        Target: ${node.nodeName}
                        Confidence: ${String.format("%.1f", idsResult.confidence * 100)}%
                        Pattern: ${idsResult.patternMatch}
                        Message: "${message.content.take(50)}..."
                    """.trimIndent(),
                    timestamp = dateFormatter.format(Date()),
                    severity = getAttackSeverity(idsResult.attackType, idsResult.confidence)
                )
            )

            // Update attack statistics
            updateAttackStats(idsResult.attackType)
            updateDetectionMetrics(idsResult.attackType, true, detectionTime)

            // Critical alert for high confidence attacks
            if (idsResult.confidence > 0.9) {
                triggerCriticalAttackAlert(node.nodeId, idsResult)
            }
        } else {
            // Normal message
            val sourceNode = simulationEngine.getNode(message.source)
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.RECEIVED,
                    description = "${sourceNode?.nodeName ?: message.source} → ${node.nodeName}",
                    details = message.content,
                    timestamp = dateFormatter.format(Date())
                )
            )
        }
    }

    private fun handleNetworkEvent(event: SimulationEngine.NetworkEvent) {
        when (event) {
            is SimulationEngine.NetworkEvent.MessageSent -> {
                if (event.hopCount > 0) {
                    val fromNode = simulationEngine.getNode(event.from)
                    val toNode = simulationEngine.getNode(event.to)

                    addMessageEvent(
                        MessageEvent(
                            type = MessageEventType.FORWARDED,
                            description = "Hop ${event.hopCount}: ${fromNode?.nodeName ?: event.from} → ${toNode?.nodeName ?: event.to}",
                            details = "Message forwarded",
                            timestamp = dateFormatter.format(Date())
                        )
                    )
                }
            }
            is SimulationEngine.NetworkEvent.MessageDelivered -> {
                addMessageEvent(
                    MessageEvent(
                        type = MessageEventType.DELIVERED,
                        description = "✓ Message delivered",
                        details = "Total hops: ${event.totalHops}",
                        timestamp = dateFormatter.format(Date())
                    )
                )

                // Update multi-hop metrics
                _researchMetrics.update { metrics ->
                    metrics.copy(
                        maxHopCount = maxOf(metrics.maxHopCount, event.totalHops)
                    )
                }
            }
            else -> {
                // Handle other events if needed
            }
        }
    }

    // Research Experiment Methods
    fun runResearchExperiment(experimentType: ExperimentType) {
        viewModelScope.launch {
            val experimentId = UUID.randomUUID().toString()

            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.EXPERIMENT_STARTED,
                    description = "🔬 Research Experiment: ${experimentType.name}",
                    details = "ID: $experimentId",
                    timestamp = dateFormatter.format(Date())
                )
            )

            when (experimentType) {
                ExperimentType.DETECTION_ACCURACY -> runDetectionAccuracyExperiment(experimentId)
                ExperimentType.SCALABILITY_TEST -> runScalabilityExperiment(experimentId)
                ExperimentType.MULTI_HOP_PERFORMANCE -> runMultiHopPerformanceExperiment(experimentId)
                ExperimentType.RESOURCE_CONSUMPTION -> runResourceConsumptionExperiment(experimentId)
                ExperimentType.HYBRID_COMPARISON -> runHybridComparisonExperiment(experimentId)
                ExperimentType.ATTACK_VARIETY -> runAttackVarietyExperiment(experimentId)
            }

            generateResearchReport(experimentId)
        }
    }

    private suspend fun runDetectionAccuracyExperiment(experimentId: String) {
        val attackTypes = listOf("SPOOFING", "INJECTION", "FLOODING", "EXPLOIT")
        val results = mutableListOf<Pair<String, Boolean>>()

        // Test each attack type multiple times
        attackTypes.forEach { attackType ->
            repeat(10) {
                val startTime = System.currentTimeMillis()

                // Send attack
                val pattern = attackPatterns.find { it.type == attackType }!!
                val message = pattern.messages.random()

                val detected = simulateAndDetectAttack(message, attackType)
                val detectionTime = System.currentTimeMillis() - startTime

                results.add(attackType to detected)
                updateDetectionMetrics(attackType, detected, detectionTime)

                delay(500)
            }
        }

        // Test normal messages
        repeat(20) {
            val normalMessage = "Normal message ${System.currentTimeMillis()}"
            val detected = simulateAndDetectAttack(normalMessage, "NORMAL")
            results.add("NORMAL" to !detected)
            delay(300)
        }

        calculateAccuracyMetrics(results)
    }

    private suspend fun runScalabilityExperiment(experimentId: String) {
        val nodeCounts = listOf(5, 10, 20, 30, 50)

        nodeCounts.forEach { nodeCount ->
            // Create network with specific node count
            createMeshNetwork(nodeCount)
            delay(1000)

            // Measure performance with different network sizes
            val startTime = System.currentTimeMillis()
            val detectionRates = mutableListOf<Float>()

            repeat(5) {
                val attackType = attackPatterns.random().type
                simulateTargetedAttack(attackType)
                delay(2000)

                val stats = _attackStats.value
                detectionRates.add(stats.detectionRate)
            }

            val avgDetectionRate = detectionRates.average().toFloat()
            val totalTime = System.currentTimeMillis() - startTime

            _experimentResults.update { results ->
                results + ExperimentResult(
                    experimentId = experimentId,
                    timestamp = System.currentTimeMillis(),
                    networkTopology = "MESH",
                    nodeCount = nodeCount,
                    attackType = "MIXED",
                    attackVolume = 5,
                    detectionRate = avgDetectionRate,
                    falsePositiveRate = calculateFalsePositiveRate(),
                    averageLatency = totalTime / 5,
                    hopCount = simulationEngine.getAverageHopCount().toInt(),
                    detectionMethod = "HYBRID",
                    confidence = 0.85f,
                    resourceUsage = measureResourceUsage()
                )
            }
        }
    }

    private suspend fun runMultiHopPerformanceExperiment(experimentId: String) {
        val hopConfigurations = listOf(
            Pair(2, "LINEAR"),
            Pair(3, "LINEAR"),
            Pair(5, "LINEAR"),
            Pair(4, "MESH"),
        )

        hopConfigurations.forEach { (targetHops, topology) ->
            when (topology) {
                "LINEAR" -> createLinearNetwork(targetHops + 1)
                "MESH" -> createMeshNetwork(9)
            }
            delay(1000)

            val nodes = _simulationState.value.nodes
            if (nodes.size >= 2) {
                selectSourceNode(nodes.first())
                selectDestinationNode(nodes.last())

                val detectionTimes = mutableListOf<Long>()
                val deliveryRates = mutableListOf<Boolean>()

                repeat(10) {
                    val startTime = System.currentTimeMillis()
                    val attack = attackPatterns.random()

                    sendMessage(
                        from = _sourceNode.value!!.nodeId,
                        to = _destinationNode.value!!.nodeId,
                        content = attack.messages.first()
                    )

                    delay(1000)

                    val delivered = checkMessageDelivery()
                    deliveryRates.add(delivered)

                    if (delivered) {
                        detectionTimes.add(System.currentTimeMillis() - startTime)
                    }
                }

                updateMultiHopMetrics(targetHops, detectionTimes, deliveryRates)
            }
        }
    }

    private suspend fun runResourceConsumptionExperiment(experimentId: String) {
        val runtime = Runtime.getRuntime()
        val resourceMeasurements = mutableListOf<ResourceMetrics>()

        System.gc()
        delay(1000)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()

        repeat(20) {
            val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
            val startCpu = System.currentTimeMillis()

            simulateRandomAttack()
            delay(500)

            val afterMemory = runtime.totalMemory() - runtime.freeMemory()
            val cpuTime = System.currentTimeMillis() - startCpu

            resourceMeasurements.add(
                ResourceMetrics(
                    cpuUsage = (cpuTime / 500f) * 100,
                    memoryUsage = afterMemory - baselineMemory,
                    batteryDrain = estimateBatteryDrain(cpuTime, afterMemory - beforeMemory)
                )
            )
        }

        _researchMetrics.update { metrics ->
            metrics.copy(
                averageMemoryUsage = resourceMeasurements.map { it.memoryUsage }.average().toLong(),
                peakMemoryUsage = resourceMeasurements.maxOf { it.memoryUsage },
                averageCpuUsage = resourceMeasurements.map { it.cpuUsage }.average().toFloat(),
                batteryImpact = resourceMeasurements.map { it.batteryDrain }.average().toFloat()
            )
        }
    }

    private suspend fun runHybridComparisonExperiment(experimentId: String) {
        val testMessages = generateTestDataset()

        var mlOnlyCorrect = 0
        var ruleBasedOnlyCorrect = 0
        var hybridCorrect = 0

        testMessages.forEach { (message, actualType) ->
            val mlResult = simulateMLOnlyDetection(message)
            val ruleResult = simulateRuleBasedOnlyDetection(message)
            val hybridResult = idsModel.analyzeMessage(message)

            if (mlResult.attackType == actualType) mlOnlyCorrect++
            if (ruleResult.attackType == actualType) ruleBasedOnlyCorrect++
            if ((hybridResult.isAttack && actualType != "NORMAL") ||
                (!hybridResult.isAttack && actualType == "NORMAL")) {
                hybridCorrect++
            }

            delay(100)
        }

        val total = testMessages.size.toFloat()
        _researchMetrics.update { metrics ->
            metrics.copy(
                mlAccuracy = mlOnlyCorrect / total,
                ruleBasedAccuracy = ruleBasedOnlyCorrect / total,
                hybridAccuracy = hybridCorrect / total,
                mlOnlyDetections = mlOnlyCorrect,
                ruleBasedOnlyDetections = ruleBasedOnlyCorrect,
                hybridDetections = hybridCorrect
            )
        }
    }

    private suspend fun runAttackVarietyExperiment(experimentId: String) {
        val attackVariations = mapOf(
            "SPOOFING" to listOf(
                "Click here: http://mal.com",
                "URGENT: Update password at fake.site",
                "You won! Visit scam.link",
                "Security alert from admin"
            ),
            "INJECTION" to listOf(
                "'; DELETE FROM users; --",
                "{\"cmd\":\"rm -rf /\"}",
                "<script>alert(1)</script>",
                "exec(malicious_code)"
            ),
            "FLOODING" to listOf(
                "SPAM".repeat(100),
                "FLOOD_${System.currentTimeMillis()}",
                "A".repeat(1000),
                (1..100).joinToString("")
            ),
            "EXPLOIT" to listOf(
                "\\x41\\x41\\x41\\x41",
                "../../../etc/passwd",
                "AT+FACTORY_RESET",
                "%n%n%n%n"
            )
        )

        val detectionResults = mutableMapOf<String, MutableList<Float>>()
        val confidenceResults = mutableMapOf<String, MutableList<Float>>()

        attackVariations.forEach { (attackType, variations) ->
            detectionResults[attackType] = mutableListOf()
            confidenceResults[attackType] = mutableListOf()

            variations.forEach { attackMessage ->
                val result = idsModel.analyzeMessage(attackMessage)

                if (result.isAttack && result.attackType == attackType) {
                    detectionResults[attackType]!!.add(1f)
                } else {
                    detectionResults[attackType]!!.add(0f)
                }

                confidenceResults[attackType]!!.add(result.confidence.toFloat())
                delay(200)
            }
        }

        val detectionRateByType = detectionResults.mapValues { it.value.average().toFloat() }
        val avgConfidenceByType = confidenceResults.mapValues { it.value.average().toFloat() }

        _researchMetrics.update { metrics ->
            metrics.copy(
                detectionRateByType = detectionRateByType,
                averageConfidenceByType = avgConfidenceByType
            )
        }
    }

    // Helper methods
    private fun updateUiState(state: SimulationEngine.SimulationState) {
        val nodes = state.topology.nodes.map { nodeInfo ->
            val node = simulationEngine.getNode(nodeInfo.id)
            VirtualNodeState(
                nodeId = nodeInfo.id,
                nodeName = nodeInfo.name,
                position = nodeInfo.position,
                isActive = node?.isEnabled ?: false,
                connectedNodes = node?.nodeState?.value?.connectedNodes ?: emptyList(),
                routingTableSize = node?.nodeState?.value?.routingTableSize ?: 0
            )
        }

        _simulationState.value = SimulationUiState(
            nodes = nodes,
            activeConnections = state.connectionCount,
            totalMessages = state.messageCount,
            averageHopCount = simulationEngine.getAverageHopCount()
        )
    }

    private fun addMessageEvent(event: MessageEvent) {
        _messageLog.value = (_messageLog.value + event).takeLast(100)
    }

    private fun updateNodeSecurityState(nodeId: String, update: (NodeSecurityState) -> NodeSecurityState) {
        _nodeSecurityStates.update { states ->
            val currentState = states[nodeId] ?: NodeSecurityState(nodeId)
            states + (nodeId to update(currentState))
        }
    }

    private fun updateAttackStats(attackType: String) {
        _attackStats.update { stats ->
            val newTypes = stats.attackTypes.toMutableMap()
            newTypes[attackType] = (newTypes[attackType] ?: 0) + 1

            val newTotal = stats.totalAttacks + 1
            val newBlocked = stats.blockedAttacks + 1

            stats.copy(
                totalAttacks = newTotal,
                blockedAttacks = newBlocked,
                attackTypes = newTypes,
                detectionRate = newBlocked.toFloat() / newTotal
            )
        }
    }

    private fun updateAttackStatistics() {
        val detectionRate = if (_attackStats.value.totalAttacks > 0) {
            _attackStats.value.blockedAttacks.toFloat() / _attackStats.value.totalAttacks
        } else 0f

        _attackStats.update { it.copy(detectionRate = detectionRate) }
    }

    private fun getNodeName(nodeId: String): String {
        return simulationEngine.getNode(nodeId)?.nodeName ?: nodeId
    }

    private fun getAttackSeverity(attackType: String, confidence: Double): AttackSeverity {
        return when {
            attackType == "EXPLOIT" && confidence > 0.8 -> AttackSeverity.CRITICAL
            attackType == "INJECTION" && confidence > 0.7 -> AttackSeverity.HIGH
            attackType == "FLOODING" && confidence > 0.8 -> AttackSeverity.HIGH
            attackType == "SPOOFING" && confidence > 0.6 -> AttackSeverity.MEDIUM
            else -> AttackSeverity.LOW
        }
    }

    private fun triggerCriticalAttackAlert(nodeId: String, idsResult: IDSModel.AnalysisResult) {
        viewModelScope.launch {
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.CRITICAL_ALERT,
                    description = "🚨🚨 CRITICAL SECURITY ALERT 🚨🚨",
                    details = """
                        Node ${getNodeName(nodeId)} under severe attack!
                        Type: ${idsResult.attackType}
                        Action: Immediate isolation recommended
                    """.trimIndent(),
                    timestamp = dateFormatter.format(Date()),
                    severity = AttackSeverity.CRITICAL
                )
            )
        }
    }

    // Research helper methods
    private suspend fun simulateAndDetectAttack(message: String, expectedType: String): Boolean {
        val result = idsModel.analyzeMessage(message)
        return result.isAttack && result.attackType == expectedType
    }

    private fun calculateAccuracyMetrics(results: List<Pair<String, Boolean>>) {
        var tp = 0
        var tn = 0
        var fp = 0
        var fn = 0

        results.forEach { (actual, detected) ->
            when {
                actual != "NORMAL" && detected -> tp++
                actual == "NORMAL" && !detected -> tn++
                actual == "NORMAL" && detected -> fp++
                actual != "NORMAL" && !detected -> fn++
            }
        }

        val total = (tp + tn + fp + fn).toFloat()
        val accuracy = if (total > 0) (tp + tn) / total else 0f
        val precision = if (tp + fp > 0) tp.toFloat() / (tp + fp) else 0f
        val recall = if (tp + fn > 0) tp.toFloat() / (tp + fn) else 0f
        val f1Score = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0f
        val specificity = if (tn + fp > 0) tn.toFloat() / (tn + fp) else 0f

        _researchMetrics.update { metrics ->
            metrics.copy(
                truePositives = tp,
                trueNegatives = tn,
                falsePositives = fp,
                falseNegatives = fn,
                accuracy = accuracy,
                precision = precision,
                recall = recall,
                f1Score = f1Score,
                specificity = specificity
            )
        }
    }

    private fun updateDetectionMetrics(attackType: String, detected: Boolean, detectionTime: Long) {
        _researchMetrics.update { metrics ->
            val newAvg = if (metrics.averageDetectionTime > 0) {
                (metrics.averageDetectionTime + detectionTime) / 2
            } else detectionTime

            metrics.copy(
                averageDetectionTime = newAvg,
                maxDetectionTime = maxOf(metrics.maxDetectionTime, detectionTime),
                minDetectionTime = minOf(metrics.minDetectionTime, detectionTime)
            )
        }
    }

    private fun updateMultiHopMetrics(hopCount: Int, detectionTimes: List<Long>, deliveryRates: List<Boolean>) {
        val deliveryRate = deliveryRates.count { it }.toFloat() / deliveryRates.size
        val avgDetectionTime = if (detectionTimes.isNotEmpty()) detectionTimes.average().toLong() else 0L

        _researchMetrics.update { metrics ->
            metrics.copy(
                averageHopCount = hopCount.toDouble(),
                maxHopCount = maxOf(metrics.maxHopCount, hopCount),
                messageDeliveryRate = deliveryRate,
                averageDetectionTime = avgDetectionTime
            )
        }
    }

    private fun measureResourceUsage(): ResourceMetrics {
        val runtime = Runtime.getRuntime()
        val memoryUsage = runtime.totalMemory() - runtime.freeMemory()

        return ResourceMetrics(
            cpuUsage = 15.5f,
            memoryUsage = memoryUsage,
            batteryDrain = estimateBatteryDrain(100, memoryUsage)
        )
    }

    private fun estimateBatteryDrain(cpuTime: Long, memoryDelta: Long): Float {
        val cpuFactor = cpuTime * 0.001f
        val memoryFactor = memoryDelta / 1_000_000f * 0.0001f
        return cpuFactor + memoryFactor
    }

    private fun calculateFalsePositiveRate(): Float {
        val metrics = _researchMetrics.value
        return if (metrics.falsePositives + metrics.trueNegatives > 0) {
            metrics.falsePositives.toFloat() / (metrics.falsePositives + metrics.trueNegatives)
        } else 0f
    }

    private fun checkMessageDelivery(): Boolean {
        return _messageLog.value.any { it.type == MessageEventType.DELIVERED }
    }

    private fun generateTestDataset(): List<Pair<String, String>> {
        val dataset = mutableListOf<Pair<String, String>>()

        attackPatterns.forEach { pattern ->
            pattern.messages.forEach { message ->
                dataset.add(message to pattern.type)
            }
        }

        val normalMessages = listOf(
            "Hello, how are you?",
            "Meeting at 3pm",
            "Can you send me the files?",
            "Thanks for your help",
            "See you tomorrow",
            "The project is going well",
            "Let's discuss this later",
            "I agree with your proposal"
        )

        normalMessages.forEach { message ->
            dataset.add(message to "NORMAL")
        }

        return dataset.shuffled()
    }

    private suspend fun simulateMLOnlyDetection(message: String): IDSModel.AnalysisResult {
        return IDSModel.AnalysisResult(
            isAttack = message.contains("http://") || message.contains("DELETE"),
            attackType = when {
                message.contains("http://") -> "SPOOFING"
                message.contains("DELETE") -> "INJECTION"
                else -> "NORMAL"
            },
            confidence = 0.75,
            explanation = "ML model detection"
        )
    }

    private suspend fun simulateRuleBasedOnlyDetection(message: String): IDSModel.AnalysisResult {
        val attackType = when {
            message.contains(Regex("https?://")) -> "SPOOFING"
            message.contains(Regex("(DELETE|DROP|INSERT)")) -> "INJECTION"
            message.contains(Regex("FLOOD|SPAM")) -> "FLOODING"
            message.contains(Regex("\\\\x[0-9a-fA-F]{2}")) -> "EXPLOIT"
            else -> "NORMAL"
        }

        return IDSModel.AnalysisResult(
            isAttack = attackType != "NORMAL",
            attackType = attackType,
            confidence = 0.85,
            explanation = "Rule-based detection"
        )
    }

    // Test scenarios
    fun runSecurityTestScenario() {
        viewModelScope.launch {
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.TEST_STARTED,
                    description = "🧪 Security Test Scenario Started",
                    details = "Running comprehensive attack simulation",
                    timestamp = dateFormatter.format(Date())
                )
            )

            delay(1000)
            simulateTargetedAttack("SPOOFING")

            delay(3000)
            simulateTargetedAttack("INJECTION")

            delay(3000)
            simulateTargetedAttack("FLOODING")

            delay(3000)
            simulateMultiNodeAttack()

            delay(3000)
            generateSecurityReport()
        }
    }

    private fun generateSecurityReport() {
        val stats = _attackStats.value
        addMessageEvent(
            MessageEvent(
                type = MessageEventType.REPORT_GENERATED,
                description = "📊 Security Test Report",
                details = """
                    Total Attacks: ${stats.totalAttacks}
                    Blocked: ${stats.blockedAttacks}
                    Detection Rate: ${String.format("%.1f", stats.detectionRate * 100)}%
                    
                    Attack Breakdown:
                    ${stats.attackTypes.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }}
                    
                    IDS Performance: ${if (stats.detectionRate > 0.9) "EXCELLENT" else if (stats.detectionRate > 0.7) "GOOD" else "NEEDS IMPROVEMENT"}
                """.trimIndent(),
                timestamp = dateFormatter.format(Date())
            )
        )
    }

    private fun generateResearchReport(experimentId: String) {
        val metrics = _researchMetrics.value
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())

        val report = buildString {
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("       HYBRID AI-ASSISTED IDS RESEARCH REPORT")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Experiment ID: $experimentId")
            appendLine("Timestamp: $timestamp")
            appendLine()
            appendLine("1. DETECTION PERFORMANCE METRICS")
            appendLine("────────────────────────────────────────────────────────────")
            appendLine("   Accuracy:     ${String.format("%.2f%%", metrics.accuracy * 100)}")
            appendLine("   Precision:    ${String.format("%.2f%%", metrics.precision * 100)}")
            appendLine("   Recall:       ${String.format("%.2f%%", metrics.recall * 100)}")
            appendLine("   F1-Score:     ${String.format("%.2f", metrics.f1Score)}")
            appendLine("   Specificity:  ${String.format("%.2f%%", metrics.specificity * 100)}")
            appendLine()
            appendLine("   Confusion Matrix:")
            appendLine("   ┌─────────────┬─────────────┬─────────────┐")
            appendLine("   │             │ Predicted + │ Predicted - │")
            appendLine("   ├─────────────┼─────────────┼─────────────┤")
            appendLine("   │ Actual +    │     ${metrics.truePositives}      │     ${metrics.falseNegatives}      │")
            appendLine("   │ Actual -    │     ${metrics.falsePositives}      │     ${metrics.trueNegatives}      │")
            appendLine("   └─────────────┴─────────────┴─────────────┘")
            appendLine()
            appendLine("2. HYBRID APPROACH COMPARISON")
            appendLine("────────────────────────────────────────────────────────────")
            appendLine("   Method           Accuracy    Detections")
            appendLine("   ─────────────    ────────    ──────────")
            appendLine("   ML-Only          ${String.format("%.2f%%", metrics.mlAccuracy * 100)}      ${metrics.mlOnlyDetections}")
            appendLine("   Rule-Based       ${String.format("%.2f%%", metrics.ruleBasedAccuracy * 100)}      ${metrics.ruleBasedOnlyDetections}")
            appendLine("   Hybrid (Ours)    ${String.format("%.2f%%", metrics.hybridAccuracy * 100)}      ${metrics.hybridDetections}")
            appendLine()
            appendLine("3. ATTACK TYPE ANALYSIS")
            appendLine("────────────────────────────────────────────────────────────")
            metrics.detectionRateByType.forEach { (type, rate) ->
                val confidence = metrics.averageConfidenceByType[type] ?: 0f
                appendLine("   $type:")
                appendLine("      Detection Rate: ${String.format("%.2f%%", rate * 100)}")
                appendLine("      Avg Confidence: ${String.format("%.2f%%", confidence * 100)}")
            }
            appendLine()
            appendLine("4. MULTI-HOP PERFORMANCE")
            appendLine("────────────────────────────────────────────────────────────")
            appendLine("   Average Hop Count:    ${String.format("%.1f", metrics.averageHopCount)}")
            appendLine("   Maximum Hop Count:    ${metrics.maxHopCount}")
            appendLine("   Message Delivery Rate: ${String.format("%.2f%%", metrics.messageDeliveryRate * 100)}")
            appendLine()
            appendLine("5. SYSTEM PERFORMANCE")
            appendLine("────────────────────────────────────────────────────────────")
            appendLine("   Detection Time:")
            appendLine("      Average: ${metrics.averageDetectionTime}ms")
            appendLine("      Min:     ${if (metrics.minDetectionTime == Long.MAX_VALUE) 0 else metrics.minDetectionTime}ms")
            appendLine("      Max:     ${metrics.maxDetectionTime}ms")
            appendLine()
            appendLine("6. RESOURCE CONSUMPTION")
            appendLine("────────────────────────────────────────────────────────────")
            appendLine("   Memory Usage:")
            appendLine("      Average: ${metrics.averageMemoryUsage / 1_000_000}MB")
            appendLine("      Peak:    ${metrics.peakMemoryUsage / 1_000_000}MB")
            appendLine("   CPU Usage:    ${String.format("%.1f%%", metrics.averageCpuUsage)}")
            appendLine("   Battery Impact: ${String.format("%.2f%%", metrics.batteryImpact)}")
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
        }

        report.lines().forEach { line ->
            if (line.isNotBlank()) {
                Log.i("RESEARCH_REPORT", line)
            }
        }

        saveReportToFile(report, "IDS_Research_Report_$timestamp.txt")

        addMessageEvent(
            MessageEvent(
                type = MessageEventType.REPORT_GENERATED,
                description = "📊 Research Report Generated",
                details = "Report saved to: IDS_Research_Report_$timestamp.txt",
                timestamp = dateFormatter.format(Date())
            )
        )
    }

    private fun saveReportToFile(content: String, filename: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val reportFile = File(documentsDir, filename)
                reportFile.writeText(content)

                withContext(Dispatchers.Main) {
                    addMessageEvent(
                        MessageEvent(
                            type = MessageEventType.REPORT_GENERATED,
                            description = "✅ Report Saved",
                            details = "Location: ${reportFile.absolutePath}",
                            timestamp = dateFormatter.format(Date())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("SimulationViewModel", "Failed to save report", e)
            }
        }
    }

    fun exportResultsAsCSV() {
        viewModelScope.launch(Dispatchers.IO) {
            val csvContent = buildString {
                appendLine("ExperimentID,Timestamp,Topology,NodeCount,AttackType,DetectionRate,FalsePositiveRate,AvgLatency,HopCount,Method,Confidence")

                _experimentResults.value.forEach { result ->
                    appendLine("${result.experimentId},${result.timestamp},${result.networkTopology},${result.nodeCount},${result.attackType},${result.detectionRate},${result.falsePositiveRate},${result.averageLatency},${result.hopCount},${result.detectionMethod},${result.confidence}")
                }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            saveReportToFile(csvContent, "IDS_Experiment_Results_$timestamp.csv")
        }
    }

    fun getChartData(): ChartData {
        val metrics = _researchMetrics.value

        return ChartData(
            accuracyComparison = listOf(
                ChartPoint("ML-Only", metrics.mlAccuracy * 100),
                ChartPoint("Rule-Based", metrics.ruleBasedAccuracy * 100),
                ChartPoint("Hybrid", metrics.hybridAccuracy * 100)
            ),
            detectionRates = metrics.detectionRateByType.map { (type, rate) ->
                ChartPoint(type, rate * 100)
            },
            performanceMetrics = listOf(
                ChartPoint("Accuracy", metrics.accuracy * 100),
                ChartPoint("Precision", metrics.precision * 100),
                ChartPoint("Recall", metrics.recall * 100),
                ChartPoint("F1-Score", metrics.f1Score * 100)
            ),
            resourceUsage = _experimentResults.value.map { result ->
                TimeSeriesPoint(
                    timestamp = result.timestamp,
                    value = result.resourceUsage.memoryUsage.toFloat() / 1_000_000
                )
            }
        )
    }

    fun clearMessageLog() {
        _messageLog.value = emptyList()
    }

    fun resetSecurityStates() {
        _nodeSecurityStates.value = emptyMap()
        _attackStats.value = AttackStatistics()
        _isUnderAttack.value = false
        _researchMetrics.value = ResearchMetrics()
        _experimentResults.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        simulationEngine.shutdown()
    }

    // Data classes
    data class ChartData(
        val accuracyComparison: List<ChartPoint>,
        val detectionRates: List<ChartPoint>,
        val performanceMetrics: List<ChartPoint>,
        val resourceUsage: List<TimeSeriesPoint>
    )

    data class ChartPoint(val label: String, val value: Float)
    data class TimeSeriesPoint(val timestamp: Long, val value: Float)
}

// UI State Classes
data class SimulationUiState(
    val nodes: List<VirtualNodeState> = emptyList(),
    val activeConnections: Int = 0,
    val totalMessages: Long = 0,
    val averageHopCount: Double = 0.0
)

data class VirtualNodeState(
    val nodeId: String,
    val nodeName: String,
    val position: VirtualBluetoothNode.Position,
    val isActive: Boolean,
    val connectedNodes: List<String>,
    val routingTableSize: Int
)

data class MessageEvent(
    val type: MessageEventType,
    val description: String,
    val details: String = "",
    val timestamp: String,
    val severity: AttackSeverity? = null,
    val isAttack: Boolean = false
)

enum class MessageEventType {
    SENT, RECEIVED, FORWARDED, DELIVERED, DROPPED,
    ATTACK_DETECTED, ATTACK_INITIATED, NODE_COMPROMISED,
    CRITICAL_ALERT, TEST_STARTED, REPORT_GENERATED,
    EXPERIMENT_STARTED, EXPERIMENT_COMPLETED
}

enum class AttackSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Extension functions for formatting research data
fun SimulationViewModel.ResearchMetrics.toLatexTable(): String {
    return """
        \begin{table}[h]
        \centering
        \caption{IDS Performance Metrics}
        \begin{tabular}{|l|c|}
        \hline
        \textbf{Metric} & \textbf{Value} \\
        \hline
        Accuracy & ${String.format("%.2f", accuracy * 100)}\% \\
        Precision & ${String.format("%.2f", precision * 100)}\% \\
        Recall & ${String.format("%.2f", recall * 100)}\% \\
        F1-Score & ${String.format("%.2f", f1Score)} \\
        Specificity & ${String.format("%.2f", specificity * 100)}\% \\
        \hline
        \end{tabular}
        \end{table}
    """.trimIndent()
}

fun SimulationViewModel.ResearchMetrics.toMarkdownTable(): String {
    return """
        | Metric | Value |
        |--------|-------|
        | Accuracy | ${String.format("%.2f%%", accuracy * 100)} |
        | Precision | ${String.format("%.2f%%", precision * 100)} |
        | Recall | ${String.format("%.2f%%", recall * 100)} |
        | F1-Score | ${String.format("%.2f", f1Score)} |
        | Specificity | ${String.format("%.2f%%", specificity * 100)} |
        | Detection Time (avg) | ${averageDetectionTime}ms |
        | False Positive Rate | ${String.format("%.2f%%", (falsePositives.toFloat() / (falsePositives + trueNegatives)) * 100)} |
    """.trimIndent()
}