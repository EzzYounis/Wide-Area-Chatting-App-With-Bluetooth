// SimulationViewModel.kt - Updated for improved UI
package com.plcoding.bluetoothchat.presentation.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.bluetoothchat.domain.simulation.*
import com.plcoding.bluetoothchat.presentation.IDS.IDSModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val idsModel: IDSModel
) : ViewModel() {

    private val simulationEngine = SimulationEngine()

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

    private val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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

    fun createMeshNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.MESH,
                nodeCount = nodeCount
            )
        )
        _selectedNode.value = null
        _sourceNode.value = null
        _destinationNode.value = null
        _messageLog.value = emptyList()
    }

    fun createLinearNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.LINEAR,
                nodeCount = nodeCount
            )
        )
        _selectedNode.value = null
        _sourceNode.value = null
        _destinationNode.value = null
        _messageLog.value = emptyList()
    }

    fun createStarNetwork(nodeCount: Int) {
        simulationEngine.initializeSimulation(
            SimulationConfig(
                topology = TopologyType.STAR,
                nodeCount = nodeCount
            )
        )
        _selectedNode.value = null
        _sourceNode.value = null
        _destinationNode.value = null
        _messageLog.value = emptyList()
    }

    fun clearNetwork() {
        simulationEngine.clearSimulation()
        _selectedNode.value = null
        _sourceNode.value = null
        _destinationNode.value = null
        _messageLog.value = emptyList()
    }

    fun selectNode(node: VirtualNodeState) {
        _selectedNode.value = node

        // Subscribe to selected node's messages
        val virtualNode = simulationEngine.getNode(node.nodeId)
        virtualNode?.let { vNode ->
            viewModelScope.launch {
                vNode.incomingMessages.collect { message ->
                    handleIncomingMessage(vNode, message)
                }
            }
        }
    }

    fun selectSourceNode(node: VirtualNodeState) {
        _sourceNode.value = node

        // Subscribe to source node's messages
        val virtualNode = simulationEngine.getNode(node.nodeId)
        virtualNode?.let { vNode ->
            viewModelScope.launch {
                vNode.incomingMessages.collect { message ->
                    handleIncomingMessage(vNode, message)
                }
            }
        }
    }

    fun selectDestinationNode(node: VirtualNodeState) {
        _destinationNode.value = node
    }

    suspend fun sendMessage(from: String, to: String, content: String) {
        val sourceNode = simulationEngine.getNode(from) ?: return
        val targetNode = simulationEngine.getNode(to) ?: return

        // Clear previous message log for better clarity
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

    private suspend fun handleIncomingMessage(node: VirtualBluetoothNode, message: SimulatedMessage) {
        // Only process messages for the destination node in chat mode
        if (_destinationNode.value != null && node.nodeId != _destinationNode.value?.nodeId) {
            // This is a forwarding node, don't show as received
            return
        }

        // Run IDS analysis
        val idsResult = idsModel.analyzeMessage(
            message = message.content,
            fromDevice = message.source,
            toDevice = node.nodeId,
            direction = "INCOMING"
        )

        if (idsResult.isAttack) {
            addMessageEvent(
                MessageEvent(
                    type = MessageEventType.ATTACK_DETECTED,
                    description = "⚠️ ${idsResult.attackType} ATTACK",
                    details = "From: ${message.source}\nConfidence: ${String.format("%.1f", idsResult.confidence * 100)}%\nMessage: ${message.content}",
                    timestamp = dateFormatter.format(Date())
                )
            )
        } else {
            // Log received message
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
                val fromNode = simulationEngine.getNode(event.from)
                val toNode = simulationEngine.getNode(event.to)

                // Only log forwarding events, not direct sends
                if (event.hopCount > 0) {
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
            }
            is SimulationEngine.NetworkEvent.RouteDiscoveryStarted -> {
                // Optional: Show route discovery in progress
            }
            is SimulationEngine.NetworkEvent.RouteDiscovered -> {
                // Optional: Show route found
            }
            else -> {
                // Handle other events if needed
            }
        }
    }

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

    fun clearMessageLog() {
        _messageLog.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        simulationEngine.shutdown()
    }
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
    val timestamp: String
)

enum class MessageEventType {
    SENT, RECEIVED, FORWARDED, DELIVERED, DROPPED, ATTACK_DETECTED
}