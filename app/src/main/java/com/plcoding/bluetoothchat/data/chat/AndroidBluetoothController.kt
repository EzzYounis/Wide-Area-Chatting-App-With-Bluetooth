// AndroidBluetoothController.kt - Simplified for Simulation Mode
package com.plcoding.bluetoothchat.data.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.plcoding.bluetoothchat.domain.chat.*
import com.plcoding.bluetoothchat.domain.simulation.*
import com.plcoding.bluetoothchat.presentation.SecurityAlert
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Simplified Bluetooth controller for simulation mode
 * No real Bluetooth functionality - everything is simulated
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothController @Inject constructor(
    private val context: Context,
    private val messageLogDao: MessageLogDao?
) : BluetoothController {

    // Simulation components
    private val simulationEngine = SimulationEngine()
    private var currentVirtualNode: VirtualBluetoothNode? = null
    private var dataTransferService: BluetoothDataTransferService? = null

    // Security callback
    private var onSecurityAlert: (SecurityAlert) -> Unit = { _ -> }
    fun setSecurityAlertCallback(callback: (SecurityAlert) -> Unit) {
        this.onSecurityAlert = callback
        dataTransferService?.let {
            // Re-create service with new callback
            dataTransferService = BluetoothDataTransferService(
                context = context,
                messageLogDao = messageLogDao,
                onSecurityAlert = callback
            )
        }
    }

    // Connection state
    private var _connectedDeviceAddress: String? = null
    override val connectedDeviceAddress: String?
        get() = _connectedDeviceAddress

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        Log.d("BluetoothController", "Initializing in SIMULATION MODE")

        // Initialize simulation with default topology
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.MESH,
                nodeCount = 8
            )
        )

        // Create data transfer service for IDS
        dataTransferService = BluetoothDataTransferService(
            context = context,
            messageLogDao = messageLogDao,
            onSecurityAlert = onSecurityAlert
        )

        // Convert virtual nodes to Bluetooth devices for UI
        updateDeviceLists()
    }

    override fun startDiscovery() {
        Log.d("BluetoothController", "Starting simulated discovery")

        // Simulate discovery by gradually revealing nodes
        coroutineScope.launch {
            val allNodes = simulationEngine.getAllNodes()
            val devices = mutableListOf<BluetoothDeviceDomain>()

            allNodes.forEach { node ->
                delay(500) // Simulate discovery delay
                devices.add(
                    BluetoothDevice(
                        name = node.nodeName,
                        address = node.nodeId
                    )
                )
                _scannedDevices.value = devices.toList()
            }
        }
    }

    override fun stopDiscovery() {
        Log.d("BluetoothController", "Stopping simulated discovery")
        // No-op in simulation
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> = flow {
        Log.d("BluetoothController", "Starting simulated Bluetooth server")

        // Create a local virtual node
        currentVirtualNode = simulationEngine.createNode(
            id = "local_server_${System.currentTimeMillis()}",
            name = "My Device (Server)",
            position = VirtualBluetoothNode.Position(50.0, 50.0)
        )

        _isConnected.value = true
        emit(ConnectionResult.ConnectionEstablished)

        // Listen for incoming messages on the virtual node
        currentVirtualNode?.let { node ->
            node.incomingMessages.collect { message ->
                // Run through IDS
                val analyzedMessage = dataTransferService?.analyzeMessage(
                    message = message.content,
                    fromDevice = message.source,
                    toDevice = node.nodeId,
                    direction = "INCOMING"
                )

                analyzedMessage?.let {
                    emit(ConnectionResult.TransferSucceeded(it))
                }
            }
        }
    }

    override fun connectToDevice(device: BluetoothDevice): Flow<ConnectionResult> = flow {
        Log.d("BluetoothController", "Connecting to simulated device: ${device.name}")

        _isConnected.value = false
        emit(ConnectionResult.ConnectionEstablished)

        delay(1000) // Simulate connection time

        // Get the virtual node
        val targetNode = simulationEngine.getNode(device.address)
        if (targetNode == null) {
            emit(ConnectionResult.Error("Virtual node not found"))
            return@flow
        }

        // Create a local node if we don't have one
        if (currentVirtualNode == null) {
            currentVirtualNode = simulationEngine.createNode(
                id = "local_client_${System.currentTimeMillis()}",
                name = "My Device",
                position = VirtualBluetoothNode.Position(
                    x = targetNode.position.x + 30,
                    y = targetNode.position.y
                )
            )
        }

        // Connect the nodes
        currentVirtualNode?.connectToNode(targetNode.nodeId)
        _connectedDeviceAddress = device.address
        _isConnected.value = true

        Log.d("BluetoothController", "Connected to ${device.name}")
        emit(ConnectionResult.ConnectionEstablished)

        // Listen for messages from the target node
        targetNode.incomingMessages.collect { message ->
            // Run through IDS
            val analyzedMessage = dataTransferService?.analyzeMessage(
                message = message.content,
                fromDevice = message.source,
                toDevice = targetNode.nodeId,
                direction = "INCOMING"
            )

            analyzedMessage?.let {
                emit(ConnectionResult.TransferSucceeded(it))
            }
        }
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        Log.d("BluetoothController", "Sending simulated message: $message")

        val currentNode = currentVirtualNode ?: run {
            Log.w("BluetoothController", "No current virtual node")
            return null
        }

        // Analyze outgoing message
        val analyzedMessage = dataTransferService?.analyzeMessage(
            message = message,
            fromDevice = currentNode.nodeId,
            toDevice = _connectedDeviceAddress ?: "remote",
            direction = "OUTGOING"
        )

        // Send through virtual network
        val connectedNodes = currentNode.nodeState.value.connectedNodes
        if (connectedNodes.isNotEmpty()) {
            // Send to first connected node or specific device
            val targetId = _connectedDeviceAddress ?: connectedNodes.first()
            val success = currentNode.sendMessage(targetId, message)

            if (success) {
                Log.d("BluetoothController", "Message sent successfully through simulation")
                return analyzedMessage ?: BluetoothMessage(
                    message = message,
                    senderName = currentNode.nodeName,
                    isFromLocalUser = true
                )
            }
        }

        Log.w("BluetoothController", "Failed to send message - no connected nodes")
        return null
    }

    override fun closeConnection() {
        Log.d("BluetoothController", "Closing simulated connection")

        currentVirtualNode?.shutdown()
        currentVirtualNode = null
        _connectedDeviceAddress = null
        _isConnected.value = false
    }

    override fun release() {
        Log.d("BluetoothController", "Releasing simulation resources")

        closeConnection()
        simulationEngine.shutdown()
        dataTransferService?.shutdown()
        coroutineScope.cancel()
    }

    private fun updateDeviceLists() {
        val virtualDevices = simulationEngine.getAllNodes().map { node ->
            BluetoothDevice(
                name = node.nodeName,
                address = node.nodeId
            )
        }

        // Show some as "paired" and others as "available"
        _pairedDevices.value = virtualDevices.take(3)
        _scannedDevices.value = virtualDevices.drop(3)

        Log.d("BluetoothController", "Updated device lists - Paired: ${_pairedDevices.value.size}, Scanned: ${_scannedDevices.value.size}")
    }

    /**
     * Simulate multi-hop message routing
     * This is for testing purposes - shows how messages hop through the network
     */
    fun simulateMultiHopMessage(
        sourceNodeId: String,
        destinationNodeId: String,
        message: String
    ) {
        coroutineScope.launch {
            val sourceNode = simulationEngine.getNode(sourceNodeId)
            val destNode = simulationEngine.getNode(destinationNodeId)

            if (sourceNode != null && destNode != null) {
                Log.d("BluetoothController", "Simulating multi-hop from ${sourceNode.nodeName} to ${destNode.nodeName}")
                sourceNode.sendMessage(destinationNodeId, message)
            }
        }
    }

    /**
     * Get simulation statistics
     */
    fun getSimulationStats(): String {
        val state = simulationEngine.simulationState.value
        return """
            Simulation Statistics:
            - Nodes: ${state.nodeCount}
            - Connections: ${state.connectionCount}
            - Messages: ${state.messageCount}
            - IDS Status: ${dataTransferService?.getStatistics() ?: "N/A"}
        """.trimIndent()
    }

    companion object {
        const val SIMULATION_MODE = true
    }
}