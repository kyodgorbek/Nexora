package com.yodgorbek.nexora.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class ServerDevice(
    val id: String,
    val name: String,
    val status: String,
    val firmware: String,
    val lastSeen: String
)

@Serializable
data class ServerTelemetry(
    val deviceId: String,
    val temperature: Double,
    val humidity: Double,
    val battery: Int,
    val signal: Int,
    val timestamp: String
)

@Serializable
data class ServerCommandPayload(
    val command: String,
    val parameter: String? = null
)

fun main() {
    println("🚀 Starting Nexora Optional Local Server on http://0.0.0.0:8080 ...")
    println("Android Emulator access: http://10.0.2.2:8080")
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    install(CallLogging)
    install(WebSockets)
    install(CORS) {
        anyHost()
    }

    val inMemoryDevices = mutableListOf(
        ServerDevice("SIM-NX-NORMAL-01", "Nexora Sense [Normal]", "CONNECTED", "1.2.4", Clock.System.now().toString()),
        ServerDevice("SIM-NX-LOWBATT-02", "Nexora Beacon [Low Battery]", "IDLE", "1.1.0", Clock.System.now().toString()),
        ServerDevice("SIM-NX-HIGHTEMP-05", "Nexora Industrial [Thermal]", "WARNING", "1.3.0", Clock.System.now().toString())
    )

    val inMemoryTelemetry = mutableMapOf<String, MutableList<ServerTelemetry>>()

    routing {
        get("/") {
            call.respondText("Nexora Local IoT Server is running.", ContentType.Text.Plain)
        }

        // GET /api/v1/devices
        get("/api/v1/devices") {
            call.respond(inMemoryDevices)
        }

        // GET /api/v1/devices/{deviceId}
        get("/api/v1/devices/{deviceId}") {
            val id = call.parameters["deviceId"]
            val device = inMemoryDevices.find { it.id == id }
            if (device != null) {
                call.respond(device)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Device not found"))
            }
        }

        // GET /api/v1/devices/{deviceId}/telemetry
        get("/api/v1/devices/{deviceId}/telemetry") {
            val id = call.parameters["deviceId"] ?: ""
            val list = inMemoryTelemetry[id] ?: emptyList()
            call.respond(list)
        }

        // POST /api/v1/telemetry
        post("/api/v1/telemetry") {
            val telemetry = call.receive<ServerTelemetry>()
            val list = inMemoryTelemetry.getOrPut(telemetry.deviceId) { mutableListOf() }
            list.add(telemetry)
            if (list.size > 500) list.removeAt(0)
            call.respond(HttpStatusCode.Created, mapOf("status" to "SAVED", "timestamp" to telemetry.timestamp))
        }

        // POST /api/v1/devices/{deviceId}/commands
        post("/api/v1/devices/{deviceId}/commands") {
            val id = call.parameters["deviceId"] ?: ""
            val payload = call.receive<ServerCommandPayload>()
            println("[Local Server] Received Command for $id: ${payload.command} (${payload.parameter})")
            call.respond(HttpStatusCode.OK, mapOf("status" to "ACKNOWLEDGED", "targetDeviceId" to id))
        }

        // WebSocket: /ws/devices/{deviceId}
        webSocket("/ws/devices/{deviceId}") {
            val deviceId = call.parameters["deviceId"] ?: "UNKNOWN"
            println("[Local WebSocket] Client connected for stream: $deviceId")

            while (isActive) {
                val sample = ServerTelemetry(
                    deviceId = deviceId,
                    temperature = 22.0 + Random.nextDouble(-0.5, 0.5),
                    humidity = 45.0 + Random.nextDouble(-1.0, 1.0),
                    battery = Random.nextInt(90, 98),
                    signal = Random.nextInt(-65, -55),
                    timestamp = Clock.System.now().toString()
                )
                val jsonStr = Json.encodeToString(sample)
                send(Frame.Text(jsonStr))
                delay(2000)
            }
        }
    }
}
