package com.yodgorbek.nexora.ai

import com.yodgorbek.nexora.domain.model.DeviceCapabilities
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface AiService {
    suspend fun analyzeTelemetry(request: TelemetryAnalysisRequest): TelemetryAnalysisResult
    suspend fun analyzeDeviceHealth(request: DeviceHealthRequest): DeviceHealthResult
    suspend fun interpretDeviceCommand(
        request: NaturalLanguageCommandRequest,
        capabilities: DeviceCapabilities
    ): RawAiCommandResponse
}

class DisabledAiService : AiService {
    override suspend fun analyzeTelemetry(request: TelemetryAnalysisRequest): TelemetryAnalysisResult {
        return TelemetryAnalysisResult(
            summary = "AI analysis is not configured or currently disabled.",
            possibleCauses = listOf("Groq API key not provided in Settings"),
            evidence = emptyList(),
            confidence = 0.0,
            recommendations = listOf("Configure your Groq API key in Settings -> AI Configuration to enable LLM-powered diagnostics.")
        )
    }

    override suspend fun analyzeDeviceHealth(request: DeviceHealthRequest): DeviceHealthResult {
        return DeviceHealthResult(
            diagnosis = "AI health analysis disabled. Using local telemetry heuristics.",
            riskLevel = "UNKNOWN",
            optimizationTips = listOf("Local heuristics are active and monitoring real-time sensor limits.")
        )
    }

    override suspend fun interpretDeviceCommand(
        request: NaturalLanguageCommandRequest,
        capabilities: DeviceCapabilities
    ): RawAiCommandResponse {
        return RawAiCommandResponse(
            command = RawCommandType.UNKNOWN,
            explanation = "AI command interpreter is disabled. Please provide a Groq API key in settings or use direct device controls.",
            confidence = 0.0
        )
    }
}

class FakeAiService(
    var simulatedCommand: RawAiCommandResponse = RawAiCommandResponse(
        command = RawCommandType.SET_SAMPLING_INTERVAL,
        seconds = 30,
        explanation = "Extracted 30-second sampling interval from prompt.",
        confidence = 0.95
    )
) : AiService {
    override suspend fun analyzeTelemetry(request: TelemetryAnalysisRequest): TelemetryAnalysisResult {
        return TelemetryAnalysisResult(
            summary = "Telemetry data appears consistent with moderate workload.",
            possibleCauses = listOf("Regular sensor broadcast activity"),
            evidence = listOf("Latest temperature: ${request.recentTelemetry.lastOrNull()?.temperature}°C"),
            confidence = 0.92,
            recommendations = listOf("Maintain default sampling rate of 2 seconds.")
        )
    }

    override suspend fun analyzeDeviceHealth(request: DeviceHealthRequest): DeviceHealthResult {
        return DeviceHealthResult(
            diagnosis = "Device operates within nominal parameters.",
            riskLevel = "LOW",
            optimizationTips = listOf("Battery and RF link quality are healthy.")
        )
    }

    override suspend fun interpretDeviceCommand(
        request: NaturalLanguageCommandRequest,
        capabilities: DeviceCapabilities
    ): RawAiCommandResponse {
        return simulatedCommand
    }
}

@Serializable
private data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.2,
    @SerialName("response_format")
    val responseFormat: GroqResponseFormat? = null
)

@Serializable
private data class GroqResponseFormat(
    val type: String = "json_object"
)

@Serializable
private data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
private data class GroqChatResponse(
    val choices: List<GroqChoice>
)

@Serializable
private data class GroqChoice(
    val message: GroqMessage
)

class GroqAiService(
    private val apiKeyProvider: () -> String?,
    private val modelProvider: () -> String = { "llama-3.3-70b-versatile" },
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }
) : AiService {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = "https://api.groq.com/openai/v1/chat/completions"

    override suspend fun analyzeTelemetry(request: TelemetryAnalysisRequest): TelemetryAnalysisResult {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return DisabledAiService().analyzeTelemetry(request)
        }

        val prompt = """
            You are an expert IoT Systems and Embedded Device Diagnostic Engineer for the Nexora connected-device platform.
            Analyze the following telemetry history and anomaly report for device '${request.deviceName}' (ID: ${request.deviceId}, FW: ${request.firmwareVersion}):
            
            Recent Telemetry Samples:
            ${request.recentTelemetry.takeLast(10).joinToString("\n") { "T=${it.temperature}°C, H=${it.humidity}%, Batt=${it.batteryPercentage}%, RSSI=${it.signalStrength}dBm" }}
            
            Detected Anomalies:
            ${request.anomalies.joinToString("\n") { "${it.severity}: ${it.title} - ${it.description} (${it.evidence})" }}
            
            Health Overview:
            Overall Score: ${request.health.overallScore}/100, Battery: ${request.health.batteryHealth}%, Link: ${request.health.signalHealth}%
            
            Respond strictly in valid JSON with this exact schema:
            {
              "summary": "Concise summary of findings",
              "possibleCauses": ["cause 1", "cause 2"],
              "evidence": ["evidence 1", "evidence 2"],
              "confidence": 0.95,
              "recommendations": ["recommendation 1", "recommendation 2"]
            }
        """.trimIndent()

        return try {
            val response: GroqChatResponse = httpClient.post(baseUrl) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    GroqChatRequest(
                        model = modelProvider(),
                        messages = listOf(
                            GroqMessage("system", "You are an IoT diagnostics AI. Output strictly valid JSON."),
                            GroqMessage("user", prompt)
                        ),
                        responseFormat = GroqResponseFormat("json_object")
                    )
                )
            }.body()

            val content = response.choices.firstOrNull()?.message?.content ?: "{}"
            jsonParser.decodeFromString<TelemetryAnalysisResult>(content)
        } catch (e: Exception) {
            TelemetryAnalysisResult(
                summary = "AI analysis failed: ${e.message}",
                possibleCauses = listOf("Network error or invalid API key"),
                evidence = emptyList(),
                confidence = 0.0,
                recommendations = listOf("Check your internet connection and verify your Groq API key.")
            )
        }
    }

    override suspend fun analyzeDeviceHealth(request: DeviceHealthRequest): DeviceHealthResult {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return DisabledAiService().analyzeDeviceHealth(request)
        }

        val prompt = """
            Diagnose health status for IoT device:
            Score: ${request.health.overallScore}, Stability: ${request.health.stabilityScore}, Disconnects24h: ${request.health.disconnectCount24h}
            Active Anomalies: ${request.recentAnomalies.size}
            
            Respond strictly in valid JSON with this schema:
            {
              "diagnosis": "Short diagnosis text",
              "riskLevel": "LOW | MODERATE | HIGH | CRITICAL",
              "optimizationTips": ["tip 1", "tip 2"]
            }
        """.trimIndent()

        return try {
            val response: GroqChatResponse = httpClient.post(baseUrl) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    GroqChatRequest(
                        model = modelProvider(),
                        messages = listOf(
                            GroqMessage("system", "You are an IoT embedded systems diagnostics engineer. Output strictly valid JSON."),
                            GroqMessage("user", prompt)
                        ),
                        responseFormat = GroqResponseFormat("json_object")
                    )
                )
            }.body()

            val content = response.choices.firstOrNull()?.message?.content ?: "{}"
            jsonParser.decodeFromString<DeviceHealthResult>(content)
        } catch (e: Exception) {
            DeviceHealthResult(
                diagnosis = "Diagnosis fallback due to network failure: ${e.message}",
                riskLevel = "MODERATE",
                optimizationTips = listOf("Review local anomalies in the telemetry tab.")
            )
        }
    }

    override suspend fun interpretDeviceCommand(
        request: NaturalLanguageCommandRequest,
        capabilities: DeviceCapabilities
    ): RawAiCommandResponse {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return DisabledAiService().interpretDeviceCommand(request, capabilities)
        }

        val prompt = """
            You are a strict natural-language command translator for the Nexora IoT Bluetooth Low Energy device platform.
            Translate the user's plain text instruction into a structured command JSON.
            
            Supported Command Types:
            - SET_SAMPLING_INTERVAL (requires "seconds": integer between ${capabilities.minSamplingIntervalSeconds} and ${capabilities.maxSamplingIntervalSeconds})
            - SET_TELEMETRY_ENABLED (requires "enabled": boolean)
            - RESTART_DEVICE
            - CALIBRATE_SENSOR (requires "sensor_type": string, "offset": double)
            - UNKNOWN (use when the user request cannot be safely mapped)
            
            User Prompt: "${request.userPrompt}"
            Target Device: "${request.deviceName}"
            Current Sampling Interval: ${request.currentSamplingInterval}s
            Telemetry Enabled: ${request.isTelemetryEnabled}
            
            Respond strictly in valid JSON matching this schema:
            {
              "command": "SET_SAMPLING_INTERVAL" | "SET_TELEMETRY_ENABLED" | "RESTART_DEVICE" | "CALIBRATE_SENSOR" | "UNKNOWN",
              "seconds": 30,
              "enabled": true,
              "sensor_type": "temperature",
              "offset": -0.5,
              "explanation": "Clear explanation of what will be executed",
              "confidence": 0.98
            }
        """.trimIndent()

        return try {
            val response: GroqChatResponse = httpClient.post(baseUrl) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    GroqChatRequest(
                        model = modelProvider(),
                        messages = listOf(
                            GroqMessage("system", "You are a strict IoT command translator. You NEVER invent unsupported commands. Output valid JSON only."),
                            GroqMessage("user", prompt)
                        ),
                        responseFormat = GroqResponseFormat("json_object")
                    )
                )
            }.body()

            val content = response.choices.firstOrNull()?.message?.content ?: "{}"
            jsonParser.decodeFromString<RawAiCommandResponse>(content)
        } catch (e: Exception) {
            RawAiCommandResponse(
                command = RawCommandType.UNKNOWN,
                explanation = "Failed to communicate with Groq AI: ${e.message}",
                confidence = 0.0
            )
        }
    }
}
