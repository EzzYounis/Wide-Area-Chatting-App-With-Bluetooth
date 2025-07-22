// Simplified BluetoothDataTransferService.kt for Simulation Mode
package com.plcoding.bluetoothchat.data.chat

import android.content.Context
import android.util.Log
import com.plcoding.bluetoothchat.domain.chat.*
import com.plcoding.bluetoothchat.presentation.IDS.IDSModel
import com.plcoding.bluetoothchat.presentation.SecurityAlert
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Simplified version for simulation - no real Bluetooth sockets
 * Keeps IDS functionality for analyzing messages
 */
class BluetoothDataTransferService(
    private val context: Context,
    private val messageLogDao: MessageLogDao? = null,
    private val onSecurityAlert: (SecurityAlert) -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    // IDS Components
    private val idsModel: IDSModel = IDSModel(context)

    init {
        Log.d("BluetoothDataTransfer", "=== IDS SYSTEM INITIALIZED (Simulation Mode) ===")
        Log.d("BluetoothDataTransfer", "Model: ${idsModel.modelName}")
    }

    /**
     * Analyze message for simulation
     */
    suspend fun analyzeMessage(
        message: String,
        fromDevice: String,
        toDevice: String,
        direction: String = "INCOMING"
    ): BluetoothMessage {
        val timestamp = System.currentTimeMillis()

        // IDS Analysis
        val analysisStartTime = System.currentTimeMillis()
        val detectionResult = withContext(Dispatchers.Default) {
            idsModel.analyzeMessage(
                message = message,
                fromDevice = fromDevice,
                toDevice = toDevice,
                direction = direction
            )
        }
        val analysisTime = System.currentTimeMillis() - analysisStartTime

        // Log IDS results
        Log.d("BluetoothDataTransfer", "┏━━━ IDS ANALYSIS RESULTS ━━━━━━━━━━━━━━━━")
        Log.d("BluetoothDataTransfer", "┃ Status: ${if (detectionResult.isAttack) "ATTACK DETECTED" else "SAFE"}")
        Log.d("BluetoothDataTransfer", "┃ Attack Type: ${detectionResult.attackType}")
        Log.d("BluetoothDataTransfer", "┃ Confidence: ${String.format("%.1f", detectionResult.confidence * 100)}%")
        Log.d("BluetoothDataTransfer", "┃ Analysis Time: ${analysisTime}ms")
        Log.d("BluetoothDataTransfer", "┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (detectionResult.isAttack && detectionResult.shouldNotify) {
            // Send security alert to UI
            onSecurityAlert(
                SecurityAlert(
                    attackType = detectionResult.attackType,
                    deviceName = fromDevice,
                    deviceAddress = fromDevice,
                    message = message,
                    detectionMethod = "Enhanced IDS v9.0",
                    explanation = detectionResult.explanation
                )
            )
        }

        // Log message
        logMessage(fromDevice, toDevice, message, direction)

        // Create message with attack info
        return BluetoothMessage(
            message = message,
            senderName = fromDevice,
            isFromLocalUser = direction == "OUTGOING",
            isAttack = detectionResult.isAttack,
            attackType = if (detectionResult.isAttack) detectionResult.attackType else "",
            attackConfidence = if (detectionResult.isAttack) detectionResult.confidence else 0.0
        )
    }

    /**
     * Simulate message flow for testing
     */
    fun simulateMessageFlow(
        messages: List<String>,
        fromDevice: String = "TestDevice",
        toDevice: String = "LocalDevice"
    ): Flow<BluetoothMessage> = flow {
        messages.forEach { message ->
            delay(1000) // Simulate network delay
            val analyzedMessage = analyzeMessage(message, fromDevice, toDevice)
            emit(analyzedMessage)
        }
    }

    private suspend fun logMessage(
        fromDevice: String,
        toDevice: String,
        message: String,
        direction: String
    ) {
        try {
            messageLogDao?.insertMessage(
                MessageLog(
                    fromDevice = fromDevice,
                    toDevice = toDevice,
                    message = if (message.length > 500) message.take(500) + "..." else message,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e("BluetoothDataTransfer", "Logging failed", e)
        }
    }

    fun getStatistics(): String = idsModel.getStatistics()

    fun shutdown() {
        scope.cancel()
        idsModel.cleanup()
    }
}