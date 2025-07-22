// SimulationEngine.kt - Complete Implementation
package com.plcoding.bluetoothchat.domain.simulation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*
import kotlin.random.Random

/**
 * Main simulation engine that manages virtual Bluetooth nodes
 */
class SimulationEngine {
    private val nodes = ConcurrentHashMap<String, VirtualBluetoothNode>()
    private val simulationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Simulation state
    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    // Network events
    private val _networkEvents = MutableSharedFlow<NetworkEvent>()
    val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()

    // Message tracking
    private var totalMessageCount = 0L
    private val messageMetrics = mutableListOf<MessageMetric>()

    data class SimulationState(
        val isRunning: Boolean = false,
        val nodeCount: Int = 0,
        val connectionCount: Int = 0,
        val messageCount: Long = 0,
        val topology: NetworkTopology = NetworkTopology()
    )

    data class NetworkTopology(
        val nodes: List<NodeInfo> = emptyList(),
        val connections: List<ConnectionInfo> = emptyList()
    )

    data class NodeInfo(
        val id: String,
        val name: String,
        val position: VirtualBluetoothNode.Position,
        val isGateway: Boolean = false,
        val batteryLevel: Int = 100,
        val messageCount: Int = 0
    )

    data class ConnectionInfo(
        val node1: String,
        val node2: String,
        val rssi: Int,
        val quality: ConnectionQuality
    )

    data class MessageMetric(
        val messageId: String,
        val hopCount: Int,
        val deliveryTime: Long,
        val success: Boolean
    )

    enum class ConnectionQuality {
        EXCELLENT, GOOD, FAIR, POOR
    }

    sealed class NetworkEvent {
        data class NodeAdded(val nodeId: String) : NetworkEvent()
        data class NodeRemoved(val nodeId: String) : NetworkEvent()
        data class ConnectionEstablished(val node1: String, val node2: String) : NetworkEvent()
        data class ConnectionLost(val node1: String, val node2: String) : NetworkEvent()
        data class MessageSent(val from: String, val to: String, val hopCount: Int) : NetworkEvent()
        data class MessageDelivered(val messageId: String, val totalHops: Int) : NetworkEvent()
        data class RouteDiscoveryStarted(val source: String, val destination: String) : NetworkEvent()
        data class RouteDiscovered(val source: String, val destination: String, val hopCount: Int) : NetworkEvent()
    }

    init {
        startNetworkMonitor()
    }

    /**
     * Initialize simulation with predefined topology
     */
    fun initializeSimulation(config: SimulationConfig) {
        clearSimulation()

        when (config.topology) {
            TopologyType.MESH -> createMeshTopology(config.nodeCount)
            TopologyType.STAR -> createStarTopology(config.nodeCount)
            TopologyType.LINEAR -> createLinearTopology(config.nodeCount)
            TopologyType.RANDOM -> createRandomTopology(config.nodeCount)
            TopologyType.CUSTOM -> createCustomTopology(config.customNodes)
        }

        // Auto-establish connections based on proximity
        simulationScope.launch {
            delay(500) // Let nodes initialize
            establishProximityConnections()
        }

        updateSimulationState()
    }

    /**
     * Create a mesh topology where nodes are arranged in a grid
     */
    private fun createMeshTopology(nodeCount: Int) {
        val gridSize = ceil(sqrt(nodeCount.toDouble())).toInt()
        val spacing = 30.0 // meters between nodes

        var nodeIndex = 0
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (nodeIndex >= nodeCount) break

                val position = VirtualBluetoothNode.Position(
                    x = col * spacing,
                    y = row * spacing
                )

                createNode(
                    id = "mesh_${nodeIndex}",
                    name = "Mesh Node ${nodeIndex + 1}",
                    position = position
                )

                nodeIndex++
            }
        }
    }

    /**
     * Create a star topology with one central node
     */
    private fun createStarTopology(nodeCount: Int) {
        if (nodeCount < 2) return

        // Central node
        createNode(
            id = "star_center",
            name = "Central Hub",
            position = VirtualBluetoothNode.Position(100.0, 100.0)
        )

        // Peripheral nodes
        val angleStep = 360.0 / (nodeCount - 1)
        val radius = 40.0

        for (i in 0 until nodeCount - 1) {
            val angle = Math.toRadians(i * angleStep)
            val position = VirtualBluetoothNode.Position(
                x = 100.0 + radius * cos(angle),
                y = 100.0 + radius * sin(angle)
            )

            createNode(
                id = "star_${i}",
                name = "Peripheral ${i + 1}",
                position = position
            )
        }
    }

    /**
     * Create a linear topology (chain)
     */
    private fun createLinearTopology(nodeCount: Int) {
        val spacing = 35.0 // meters between nodes

        for (i in 0 until nodeCount) {
            val position = VirtualBluetoothNode.Position(
                x = 50.0 + i * spacing,
                y = 100.0
            )

            createNode(
                id = "chain_${i}",
                name = "Chain Node ${i + 1}",
                position = position
            )
        }
    }

    /**
     * Create a random topology
     */
    private fun createRandomTopology(nodeCount: Int) {
        val areaSize = 150.0 // 150x150 meter area

        for (i in 0 until nodeCount) {
            val position = VirtualBluetoothNode.Position(
                x = Random.nextDouble(20.0, areaSize),
                y = Random.nextDouble(20.0, areaSize)
            )

            createNode(
                id = "random_${i}",
                name = "Node ${i + 1}",
                position = position
            )
        }
    }

    /**
     * Create custom topology from configuration
     */
    private fun createCustomTopology(customNodes: List<NodeConfig>) {
        customNodes.forEach { config ->
            createNode(
                id = config.id,
                name = config.name,
                position = VirtualBluetoothNode.Position(config.x, config.y)
            )
        }
    }

    /**
     * Create a new virtual node
     */
    fun createNode(
        id: String,
        name: String,
        position: VirtualBluetoothNode.Position
    ): VirtualBluetoothNode {
        val node = VirtualBluetoothNode(id, name, position, this)
        nodes[id] = node

        // Monitor node messages
        simulationScope.launch {
            node.incomingMessages.collect { message ->
                handleMessageDelivery(message)
            }
        }

        simulationScope.launch {
            _networkEvents.emit(NetworkEvent.NodeAdded(id))
        }

        updateSimulationState()
        return node
    }

    /**
     * Remove a node from the simulation
     */
    fun removeNode(nodeId: String) {
        nodes.remove(nodeId)?.shutdown()

        simulationScope.launch {
            _networkEvents.emit(NetworkEvent.NodeRemoved(nodeId))
        }

        updateSimulationState()
    }

    /**
     * Get a specific node
     */
    fun getNode(nodeId: String): VirtualBluetoothNode? = nodes[nodeId]

    /**
     * Get all nodes
     */
    fun getAllNodes(): List<VirtualBluetoothNode> = nodes.values.toList()

    /**
     * Get nodes within range of a specific node
     */
    fun getNodesInRange(node: VirtualBluetoothNode): List<VirtualBluetoothNode> {
        return nodes.values.filter { other ->
            other.nodeId != node.nodeId &&
                    other.isEnabled &&
                    node.position.distanceTo(other.position) <= VirtualBluetoothNode.BLUETOOTH_RANGE
        }
    }

    /**
     * Establish connections between nodes based on proximity
     */
    private suspend fun establishProximityConnections() {
        nodes.values.forEach { node ->
            val nearbyNodes = getNodesInRange(node)
            nearbyNodes.forEach { nearbyNode ->
                if (!node.nodeState.value.connectedNodes.contains(nearbyNode.nodeId)) {
                    node.connectToNode(nearbyNode.nodeId)

                    _networkEvents.emit(
                        NetworkEvent.ConnectionEstablished(node.nodeId, nearbyNode.nodeId)
                    )
                }
            }
        }
    }

    /**
     * Monitor network statistics
     */
    private fun startNetworkMonitor() {
        simulationScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                updateSimulationState()
            }
        }
    }

    /**
     * Update simulation state
     */
    private fun updateSimulationState() {
        val nodeInfos = nodes.values.map { node ->
            NodeInfo(
                id = node.nodeId,
                name = node.nodeName,
                position = node.position,
                isGateway = false,
                batteryLevel = node.batteryLevel,
                messageCount = 0 // Could track per-node message count
            )
        }

        val connections = mutableListOf<ConnectionInfo>()
        val processedPairs = mutableSetOf<String>()

        nodes.values.forEach { node ->
            node.nodeState.value.connectedNodes.forEach { connectedId ->
                val pairKey = listOf(node.nodeId, connectedId).sorted().joinToString("-")
                if (!processedPairs.contains(pairKey)) {
                    processedPairs.add(pairKey)

                    val otherNode = nodes[connectedId]
                    if (otherNode != null) {
                        val distance = node.position.distanceTo(otherNode.position)
                        val rssi = calculateRSSI(distance)

                        connections.add(
                            ConnectionInfo(
                                node1 = node.nodeId,
                                node2 = connectedId,
                                rssi = rssi,
                                quality = getConnectionQuality(rssi)
                            )
                        )
                    }
                }
            }
        }

        _simulationState.value = SimulationState(
            isRunning = true,
            nodeCount = nodes.size,
            connectionCount = connections.size,
            messageCount = totalMessageCount,
            topology = NetworkTopology(
                nodes = nodeInfos,
                connections = connections
            )
        )
    }

    /**
     * Handle message delivery tracking
     */
    private suspend fun handleMessageDelivery(message: SimulatedMessage) {
        totalMessageCount++

        messageMetrics.add(
            MessageMetric(
                messageId = message.id,
                hopCount = message.hopCount,
                deliveryTime = System.currentTimeMillis() - message.timestamp,
                success = true
            )
        )

        _networkEvents.emit(
            NetworkEvent.MessageDelivered(
                messageId = message.id,
                totalHops = message.hopCount
            )
        )

        updateSimulationState()
    }

    /**
     * Calculate RSSI based on distance
     */
    private fun calculateRSSI(distance: Double): Int {
        val txPower = -59
        val pathLoss = 20 * log10(distance) + 20 * log10(2400.0) - 147.55
        return (txPower - pathLoss).toInt().coerceIn(-100, 0)
    }

    /**
     * Get connection quality based on RSSI
     */
    private fun getConnectionQuality(rssi: Int): ConnectionQuality {
        return when {
            rssi > -60 -> ConnectionQuality.EXCELLENT
            rssi > -70 -> ConnectionQuality.GOOD
            rssi > -80 -> ConnectionQuality.FAIR
            else -> ConnectionQuality.POOR
        }
    }

    /**
     * Get average hop count for delivered messages
     */
    fun getAverageHopCount(): Double {
        if (messageMetrics.isEmpty()) return 0.0
        return messageMetrics.map { it.hopCount }.average()
    }

    /**
     * Get message delivery success rate
     */
    fun getDeliverySuccessRate(): Double {
        if (messageMetrics.isEmpty()) return 0.0
        val successCount = messageMetrics.count { it.success }
        return successCount.toDouble() / messageMetrics.size
    }

    /**
     * Clear simulation
     */
    fun clearSimulation() {
        nodes.values.forEach { it.shutdown() }
        nodes.clear()
        messageMetrics.clear()
        totalMessageCount = 0
        updateSimulationState()
    }

    /**
     * Shutdown simulation engine
     */
    fun shutdown() {
        clearSimulation()
        simulationScope.cancel()
    }
}