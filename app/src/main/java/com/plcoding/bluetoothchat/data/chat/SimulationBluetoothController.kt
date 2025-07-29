package com.plcoding.bluetoothchat.data.chat

import android.content.Context
import android.util.Log
import com.plcoding.bluetoothchat.domain.chat.*
import com.plcoding.bluetoothchat.domain.simulation.*
import com.plcoding.bluetoothchat.presentation.SecurityAlert
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject


class SimulationBluetoothController @Inject constructor(
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
        Log.d("SimulationController", "Initializing Simulation-Only Mode")

        // Initialize simulation with default topology
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.MESH,
                nodeCount = 9,
                simulationSpeed = SimulationSpeed.NORMAL,
                enablePacketLoss = true
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
        Log.d("SimulationController", "Starting simulated discovery")

        coroutineScope.launch {
            val allNodes = simulationEngine.getAllNodes()
            val devices = mutableListOf<BluetoothDeviceDomain>()

            // Simulate gradual discovery
            allNodes.forEach { node ->
                delay(300) // Simulate discovery delay
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
        Log.d("SimulationController", "Stopping simulated discovery")
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> = flow {
        Log.d("SimulationController", "Starting simulated server")

        // Create a local virtual node
        currentVirtualNode = simulationEngine.createNode(
            id = "server_${System.currentTimeMillis()}",
            name = "Simulation Server",
            position = VirtualBluetoothNode.Position(50.0, 50.0)
        )

        _isConnected.value = true
        emit(ConnectionResult.ConnectionEstablished)

        // Listen for incoming messages
        currentVirtualNode?.let { node ->
            node.incomingMessages.collect { message ->
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
        Log.d("SimulationController", "Connecting to: ${device.name}")

        _isConnected.value = false
        emit(ConnectionResult.ConnectionEstablished)

        delay(500) // Simulate connection time

        val targetNode = simulationEngine.getNode(device.address)
        if (targetNode == null) {
            emit(ConnectionResult.Error("Virtual node not found"))
            return@flow
        }

        // Create local node if needed
        if (currentVirtualNode == null) {
            currentVirtualNode = simulationEngine.createNode(
                id = "client_${System.currentTimeMillis()}",
                name = "My Simulated Device",
                position = VirtualBluetoothNode.Position(
                    x = targetNode.position.x + 30,
                    y = targetNode.position.y
                )
            )
        }

        // Connect nodes
        currentVirtualNode?.connectToNode(targetNode.nodeId)
        _connectedDeviceAddress = device.address
        _isConnected.value = true

        Log.d("SimulationController", "Connected to ${device.name}")
        emit(ConnectionResult.ConnectionEstablished)

        // Listen for messages
        targetNode.incomingMessages.collect { message ->
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
        Log.d("SimulationController", "Sending message: $message")

        val currentNode = currentVirtualNode ?: return null

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
            val targetId = _connectedDeviceAddress ?: connectedNodes.first()
            val success = currentNode.sendMessage(targetId, message)

            if (success) {
                return analyzedMessage ?: BluetoothMessage(
                    message = message,
                    senderName = currentNode.nodeName,
                    isFromLocalUser = true
                )
            }
        }

        return null
    }

    override fun closeConnection() {
        Log.d("SimulationController", "Closing connection")
        currentVirtualNode?.shutdown()
        currentVirtualNode = null
        _connectedDeviceAddress = null
        _isConnected.value = false
    }

    override fun release() {
        Log.d("SimulationController", "Releasing resources")
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

        // Simulate paired and available devices
        _pairedDevices.value = virtualDevices.take(3)
        _scannedDevices.value = virtualDevices.drop(3)
    }

    // Additional simulation features
    fun changeNetworkTopology(topology: TopologyType, nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = topology,
                nodeCount = nodeCount
            )
        )
        updateDeviceLists()
    }

    fun getSimulationStatistics(): SimulationStatistics {
        val state = simulationEngine.simulationState.value
        return SimulationStatistics(
            nodeCount = state.nodeCount,
            connectionCount = state.connectionCount,
            totalMessages = state.messageCount,
            averageHopCount = simulationEngine.getAverageHopCount(),
            deliveryRate = simulationEngine.getDeliverySuccessRate(),
            idsStatus = dataTransferService?.getStatistics() ?: "N/A"
        )
    }

    fun simulateNetworkConditions(
        packetLoss: Float = 0.1f,
        latencyMs: Long = 100L,
        jitterMs: Long = 50L
    ) {
        // This would be implemented in the simulation engine
        Log.d("SimulationController", "Simulating network conditions - Loss: $packetLoss, Latency: $latencyMs ms")
    }

    data class SimulationStatistics(
        val nodeCount: Int,
        val connectionCount: Int,
        val totalMessages: Long,
        val averageHopCount: Double,
        val deliveryRate: Double,
        val idsStatus: String
    )

    // Network topology access for visualization
    fun getNetworkTopology(): SimulationEngine.NetworkTopology {
        return simulationEngine.simulationState.value.topology
    }

    // Get current simulation state for monitoring
    fun getSimulationState(): StateFlow<SimulationEngine.SimulationState> {
        return simulationEngine.simulationState
    }
}