// VirtualBluetoothNode.kt
package com.plcoding.bluetoothchat.domain.simulation

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*
import kotlin.random.Random

/**
 * Represents a virtual Bluetooth device in the simulation
 */
class VirtualBluetoothNode(
    val nodeId: String,
    val nodeName: String,
    var position: Position,
    private val simulationEngine: SimulationEngine
) {
    // Node state
    var isEnabled = true
    var batteryLevel = 100
    var txPower = -59 // Transmission power in dBm

    // Connections and routing
    private val connections = ConcurrentHashMap<String, VirtualConnection>()
    private val routingTable = ConcurrentHashMap<String, RouteEntry>()
    private val messageBuffer = mutableListOf<BufferedMessage>()

    // Flows for state updates
    private val _nodeState = MutableStateFlow(NodeState())
    val nodeState: StateFlow<NodeState> = _nodeState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SimulatedMessage>()
    val incomingMessages: SharedFlow<SimulatedMessage> = _incomingMessages.asSharedFlow()

    private val nodeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    data class Position(val x: Double, val y: Double) {
        fun distanceTo(other: Position): Double {
            return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
        }
    }

    data class NodeState(
        val isEnabled: Boolean = true,
        val connectedNodes: List<String> = emptyList(),
        val discoveredNodes: List<DiscoveredNode> = emptyList(),
        val routingTableSize: Int = 0,
        val messageQueueSize: Int = 0
    )

    data class DiscoveredNode(
        val nodeId: String,
        val nodeName: String,
        val rssi: Int,
        val distance: Double
    )

    data class RouteEntry(
        val destination: String,
        val nextHop: String,
        val hopCount: Int,
        val sequenceNumber: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class BufferedMessage(
        val message: SimulatedMessage,
        val remainingRetries: Int = 3,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        startPeriodicDiscovery()
        startMessageProcessor()
        startRouteMaintenace()
    }

    /**
     * Discover nearby nodes within range
     */
    suspend fun discoverNearbyNodes(): List<DiscoveredNode> {
        if (!isEnabled) return emptyList()

        val nearbyNodes = simulationEngine.getNodesInRange(this)
        val discovered = nearbyNodes.map { node ->
            val distance = position.distanceTo(node.position)
            val rssi = calculateRSSI(distance)
            DiscoveredNode(
                nodeId = node.nodeId,
                nodeName = node.nodeName,
                rssi = rssi,
                distance = distance
            )
        }.filter { it.rssi > MINIMUM_RSSI }

        updateNodeState()
        return discovered
    }

    /**
     * Connect to another virtual node
     */
    suspend fun connectToNode(targetNodeId: String): Boolean {
        if (!isEnabled || connections.containsKey(targetNodeId)) return false

        val targetNode = simulationEngine.getNode(targetNodeId) ?: return false
        val distance = position.distanceTo(targetNode.position)

        if (distance > BLUETOOTH_RANGE) return false

        val connection = VirtualConnection(
            localNodeId = nodeId,
            remoteNodeId = targetNodeId,
            rssi = calculateRSSI(distance),
            established = System.currentTimeMillis()
        )

        connections[targetNodeId] = connection
        targetNode.acceptConnection(nodeId, connection)

        updateNodeState()
        return true
    }

    /**
     * Accept incoming connection
     */
    fun acceptConnection(fromNodeId: String, connection: VirtualConnection) {
        connections[fromNodeId] = connection.reverse()
        updateNodeState()
    }

    /**
     * Send a message (handles both direct and multi-hop)
     */
    suspend fun sendMessage(
        destinationId: String,
        content: String,
        messageType: MessageType = MessageType.DATA
    ): Boolean {
        val message = SimulatedMessage(
            id = generateMessageId(),
            source = nodeId,
            destination = destinationId,
            content = content,
            type = messageType,
            hopCount = 0,
            maxHops = MAX_HOP_COUNT
        )

        // Check if direct connection exists
        if (connections.containsKey(destinationId)) {
            return forwardMessageDirect(message, destinationId)
        }

        // Try multi-hop routing
        val route = routingTable[destinationId]
        if (route != null) {
            return forwardMessageDirect(message.copy(hopCount = 1), route.nextHop)
        }

        // Initiate route discovery
        initiateRouteDiscovery(destinationId)

        // Buffer the message
        messageBuffer.add(BufferedMessage(message))
        return true
    }

    /**
     * Receive and process incoming message
     */
    suspend fun receiveMessage(message: SimulatedMessage, fromNodeId: String) {
        if (!isEnabled) return

        // Update routing information
        updateRoutingInfo(message.source, fromNodeId, message.hopCount)

        when (message.type) {
            MessageType.DATA -> handleDataMessage(message)
            MessageType.RREQ -> handleRouteRequest(message)
            MessageType.RREP -> handleRouteReply(message)
            MessageType.RERR -> handleRouteError(message)
        }
    }

    private suspend fun handleDataMessage(message: SimulatedMessage) {
        if (message.destination == nodeId) {
            // Message reached destination
            _incomingMessages.emit(message)
        } else if (message.hopCount < message.maxHops) {
            // Forward the message
            forwardMessage(message.copy(hopCount = message.hopCount + 1))
        }
    }

    private suspend fun forwardMessage(message: SimulatedMessage) {
        val route = routingTable[message.destination] ?: return
        forwardMessageDirect(message, route.nextHop)
    }

    private suspend fun forwardMessageDirect(message: SimulatedMessage, nextHopId: String): Boolean {
        val connection = connections[nextHopId] ?: return false
        val nextNode = simulationEngine.getNode(nextHopId) ?: return false

        // Simulate transmission delay
        delay(calculateTransmissionDelay(message))

        // Simulate packet loss based on RSSI
        if (shouldDropPacket(connection.rssi)) {
            return false
        }

        nextNode.receiveMessage(message, nodeId)
        return true
    }

    /**
     * AODV Route Discovery - Route Request
     */
    private suspend fun initiateRouteDiscovery(destinationId: String) {
        val rreq = SimulatedMessage(
            id = generateMessageId(),
            source = nodeId,
            destination = destinationId,
            content = "RREQ",
            type = MessageType.RREQ,
            hopCount = 0,
            maxHops = MAX_HOP_COUNT,
            sequenceNumber = getNextSequenceNumber()
        )

        // Broadcast to all connected nodes
        broadcastMessage(rreq)
    }

    private suspend fun handleRouteRequest(rreq: SimulatedMessage) {
        // Check if we have a route to destination
        if (rreq.destination == nodeId) {
            // Send Route Reply
            sendRouteReply(rreq)
        } else {
            val existingRoute = routingTable[rreq.destination]
            if (existingRoute != null && existingRoute.sequenceNumber >= rreq.sequenceNumber) {
                // Send intermediate Route Reply
                sendRouteReply(rreq)
            } else {
                // Forward RREQ
                broadcastMessage(rreq.copy(hopCount = rreq.hopCount + 1))
            }
        }
    }

    private suspend fun sendRouteReply(rreq: SimulatedMessage) {
        val rrep = SimulatedMessage(
            id = generateMessageId(),
            source = nodeId,
            destination = rreq.source,
            content = "RREP:${rreq.destination}",
            type = MessageType.RREP,
            hopCount = 0,
            maxHops = MAX_HOP_COUNT,
            sequenceNumber = getNextSequenceNumber()
        )

        // Send back through reverse path
        val reverseRoute = routingTable[rreq.source]
        if (reverseRoute != null) {
            forwardMessageDirect(rrep, reverseRoute.nextHop)
        }
    }

    private suspend fun handleRouteReply(rrep: SimulatedMessage) {
        // Extract destination from content
        val destination = rrep.content.substringAfter("RREP:")

        // Update routing table
        updateRoutingInfo(destination, rrep.source, rrep.hopCount)

        // Process buffered messages
        processBufferedMessages(destination)

        // Forward RREP if not final destination
        if (rrep.destination != nodeId) {
            forwardMessage(rrep.copy(hopCount = rrep.hopCount + 1))
        }
    }

    private suspend fun handleRouteError(rerr: SimulatedMessage) {
        // Remove failed routes
        val failedDestination = rerr.content.substringAfter("RERR:")
        routingTable.remove(failedDestination)

        // Forward RERR
        if (rerr.hopCount < rerr.maxHops) {
            broadcastMessage(rerr.copy(hopCount = rerr.hopCount + 1))
        }
    }

    private suspend fun broadcastMessage(message: SimulatedMessage) {
        connections.keys.forEach { neighborId ->
            forwardMessageDirect(message, neighborId)
        }
    }

    private fun updateRoutingInfo(destination: String, nextHop: String, hopCount: Int) {
        if (destination == nodeId) return

        val existingRoute = routingTable[destination]
        if (existingRoute == null || existingRoute.hopCount > hopCount + 1) {
            routingTable[destination] = RouteEntry(
                destination = destination,
                nextHop = nextHop,
                hopCount = hopCount + 1,
                sequenceNumber = getNextSequenceNumber()
            )
        }
    }

    private suspend fun processBufferedMessages(destination: String) {
        val messages = messageBuffer.filter { it.message.destination == destination }
        messages.forEach { buffered ->
            sendMessage(destination, buffered.message.content, buffered.message.type)
        }
        messageBuffer.removeAll(messages)
    }

    /**
     * Periodic discovery of nearby nodes
     */
    private fun startPeriodicDiscovery() {
        nodeScope.launch {
            while (isActive) {
                delay(DISCOVERY_INTERVAL)
                if (isEnabled) {
                    val discovered = discoverNearbyNodes()
                    // Auto-connect to nearby nodes (simplified)
                    discovered.forEach { node ->
                        if (!connections.containsKey(node.nodeId) && node.rssi > -80) {
                            connectToNode(node.nodeId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Process buffered messages periodically
     */
    private fun startMessageProcessor() {
        nodeScope.launch {
            while (isActive) {
                delay(MESSAGE_PROCESS_INTERVAL)

                // Clean old messages
                val now = System.currentTimeMillis()
                messageBuffer.removeAll { now - it.timestamp > MESSAGE_TIMEOUT }

                // Retry buffered messages
                val toRetry = messageBuffer.filter { it.remainingRetries > 0 }
                toRetry.forEach { buffered ->
                    val route = routingTable[buffered.message.destination]
                    if (route != null) {
                        sendMessage(
                            buffered.message.destination,
                            buffered.message.content,
                            buffered.message.type
                        )
                        messageBuffer.remove(buffered)
                    } else {
                        // Decrement retry count
                        val index = messageBuffer.indexOf(buffered)
                        if (index >= 0) {
                            messageBuffer[index] = buffered.copy(
                                remainingRetries = buffered.remainingRetries - 1
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Maintain routing table
     */
    private fun startRouteMaintenace() {
        nodeScope.launch {
            while (isActive) {
                delay(ROUTE_MAINTENANCE_INTERVAL)

                // Remove stale routes
                val now = System.currentTimeMillis()
                val staleRoutes = routingTable.filter { (_, route) ->
                    now - route.timestamp > ROUTE_TIMEOUT
                }

                staleRoutes.forEach { (destination, _) ->
                    routingTable.remove(destination)
                    // Send route error
                    broadcastMessage(
                        SimulatedMessage(
                            id = generateMessageId(),
                            source = nodeId,
                            destination = "BROADCAST",
                            content = "RERR:$destination",
                            type = MessageType.RERR,
                            hopCount = 0,
                            maxHops = 3
                        )
                    )
                }
            }
        }
    }

    private fun calculateRSSI(distance: Double): Int {
        // Free space path loss model
        val pathLoss = 20 * log10(distance) + 20 * log10(2400.0) - 147.55
        return (txPower - pathLoss).toInt().coerceIn(-100, 0)
    }

    private fun calculateTransmissionDelay(message: SimulatedMessage): Long {
        val baseDelay = 50L // Base delay in ms
        val sizeDelay = (message.content.length / 100) * 10L // Size-based delay
        val randomDelay = Random.nextLong(0, 50) // Random jitter
        return baseDelay + sizeDelay + randomDelay
    }

    private fun shouldDropPacket(rssi: Int): Boolean {
        // Packet loss probability based on RSSI
        val lossRate = when {
            rssi > -60 -> 0.01  // 1% loss
            rssi > -70 -> 0.05  // 5% loss
            rssi > -80 -> 0.10  // 10% loss
            rssi > -90 -> 0.25  // 25% loss
            else -> 0.50        // 50% loss
        }
        return Random.nextDouble() < lossRate
    }

    private fun updateNodeState() {
        _nodeState.value = NodeState(
            isEnabled = isEnabled,
            connectedNodes = connections.keys.toList(),
            discoveredNodes = emptyList(), // Updated by discovery
            routingTableSize = routingTable.size,
            messageQueueSize = messageBuffer.size
        )
    }

    private var sequenceCounter = 0
    private fun getNextSequenceNumber(): Int = sequenceCounter++

    private fun generateMessageId(): String = "${nodeId}_${System.currentTimeMillis()}_${Random.nextInt()}"

    fun shutdown() {
        nodeScope.cancel()
        connections.clear()
        routingTable.clear()
        messageBuffer.clear()
    }

    companion object {
        const val BLUETOOTH_RANGE = 50.0 // meters
        const val MINIMUM_RSSI = -90
        const val MAX_HOP_COUNT = 10
        const val DISCOVERY_INTERVAL = 5000L // 5 seconds
        const val MESSAGE_PROCESS_INTERVAL = 1000L // 1 second
        const val ROUTE_MAINTENANCE_INTERVAL = 10000L // 10 seconds
        const val MESSAGE_TIMEOUT = 30000L // 30 seconds
        const val ROUTE_TIMEOUT = 60000L // 60 seconds
    }
}

data class VirtualConnection(
    val localNodeId: String,
    val remoteNodeId: String,
    val rssi: Int,
    val established: Long
) {
    fun reverse() = VirtualConnection(
        localNodeId = remoteNodeId,
        remoteNodeId = localNodeId,
        rssi = rssi,
        established = established
    )
}

data class SimulatedMessage(
    val id: String,
    val source: String,
    val destination: String,
    val content: String,
    val type: MessageType,
    val hopCount: Int,
    val maxHops: Int,
    val sequenceNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    DATA,    // Regular data message
    RREQ,    // Route Request
    RREP,    // Route Reply
    RERR     // Route Error
}