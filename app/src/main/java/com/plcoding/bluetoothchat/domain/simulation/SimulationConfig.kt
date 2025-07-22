// SimulationConfig.kt
package com.plcoding.bluetoothchat.domain.simulation

/**
 * Configuration for simulation setup
 */
data class SimulationConfig(
    val topology: TopologyType,
    val nodeCount: Int = 0,
    val customNodes: List<NodeConfig> = emptyList(),
    val simulationSpeed: SimulationSpeed = SimulationSpeed.NORMAL,
    val enablePacketLoss: Boolean = true,
    val enableBatterySimulation: Boolean = false
)

/**
 * Types of network topologies
 */
enum class TopologyType {
    MESH,     // Grid arrangement
    STAR,     // Central hub with peripherals
    LINEAR,   // Chain of nodes
    RANDOM,   // Random placement
    CUSTOM    // User-defined positions
}

/**
 * Configuration for individual nodes
 */
data class NodeConfig(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val batteryLevel: Int = 100,
    val txPower: Int = -59
)

/**
 * Simulation speed settings
 */
enum class SimulationSpeed {
    SLOW,     // 2x slower
    NORMAL,   // Real-time
    FAST,     // 2x faster
    VERY_FAST // 5x faster
}

/**
 * Preset configurations for common test scenarios
 */
object SimulationPresets {

    fun smallMeshNetwork() = SimulationConfig(
        topology = TopologyType.MESH,
        nodeCount = 6,
        simulationSpeed = SimulationSpeed.NORMAL
    )

    fun largeMeshNetwork() = SimulationConfig(
        topology = TopologyType.MESH,
        nodeCount = 16,
        simulationSpeed = SimulationSpeed.FAST
    )

    fun linearChain() = SimulationConfig(
        topology = TopologyType.LINEAR,
        nodeCount = 5,
        simulationSpeed = SimulationSpeed.NORMAL
    )

    fun starNetwork() = SimulationConfig(
        topology = TopologyType.STAR,
        nodeCount = 7,
        simulationSpeed = SimulationSpeed.NORMAL
    )

    fun customBuilding() = SimulationConfig(
        topology = TopologyType.CUSTOM,
        customNodes = listOf(
            // Floor 1
            NodeConfig("room_101", "Room 101", 0.0, 0.0),
            NodeConfig("room_102", "Room 102", 40.0, 0.0),
            NodeConfig("room_103", "Room 103", 80.0, 0.0),
            NodeConfig("hallway_1", "Hallway 1", 40.0, 20.0),

            // Floor 2
            NodeConfig("room_201", "Room 201", 0.0, 50.0),
            NodeConfig("room_202", "Room 202", 40.0, 50.0),
            NodeConfig("room_203", "Room 203", 80.0, 50.0),
            NodeConfig("hallway_2", "Hallway 2", 40.0, 70.0),

            // Stairwell (connects floors)
            NodeConfig("stairwell", "Stairwell", 120.0, 35.0)
        )
    )

    fun testScenarioFlooding() = SimulationConfig(
        topology = TopologyType.MESH,
        nodeCount = 9,
        simulationSpeed = SimulationSpeed.VERY_FAST,
        enablePacketLoss = false // Disable packet loss for flooding tests
    )

    fun reliabilityTest() = SimulationConfig(
        topology = TopologyType.RANDOM,
        nodeCount = 12,
        simulationSpeed = SimulationSpeed.NORMAL,
        enablePacketLoss = true,
        enableBatterySimulation = true
    )
}

/**
 * Simulation parameters
 */
data class SimulationParameters(
    val baseTransmissionDelay: Long = 50, // ms
    val discoveryInterval: Long = 5000, // ms
    val routeTimeout: Long = 60000, // ms
    val messageTimeout: Long = 30000, // ms
    val maxHopCount: Int = 10,
    val bluetoothRange: Double = 50.0, // meters
    val minimumRSSI: Int = -90 // dBm
)

/**
 * Attack simulation configuration
 */
data class AttackSimulationConfig(
    val enableAttacks: Boolean = true,
    val attackProbability: Double = 0.1, // 10% chance
    val attackTypes: List<AttackType> = AttackType.values().toList()
)

enum class AttackType {
    SPOOFING,
    INJECTION,
    FLOODING,
    REPLAY,
    MAN_IN_THE_MIDDLE
}