package ua.danichapps.radiantdays.sync

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ua.danichapps.radiantdays.BuildConfig
import ua.danichapps.radiantdays.domain.repository.CalendarEventRepository
import kotlin.math.min

class WebSocketBridgeClient(
    private val repository: CalendarEventRepository,
    private val deviceIdProvider: DeviceIdProvider,
) {

    private val client = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var shouldRun = false
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    fun start() {
        synchronized(lock) {
            if (shouldRun) return
            shouldRun = true
            reconnectAttempt = 0
        }
        connect()
    }

    fun stop() {
        synchronized(lock) {
            shouldRun = false
            reconnectAttempt = 0
            reconnectJob?.cancel()
            reconnectJob = null
            webSocket?.close(1000, "foreground stopped")
            webSocket = null
        }
    }

    fun shutdown() {
        stop()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }

    private fun connect() {
        val wsUrl: String
        synchronized(lock) {
            if (!shouldRun || webSocket != null) return
            val deviceId = deviceIdProvider.getOrCreateDeviceId()
            wsUrl = "ws://${BuildConfig.WS_BRIDGE_HOST}:${BuildConfig.WS_BRIDGE_PORT}/ws/$deviceId"
            webSocket = client.newWebSocket(
                Request.Builder()
                    .url(wsUrl)
                    .build(),
                createListener(),
            )
        }
        Log.d("qqwe_tag  WSBridge", "connect url=$wsUrl")
    }

    private fun createListener(): WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            synchronized(lock) {
                reconnectAttempt = 0
            }
            Log.d("qqwe_tag  WSBridge", "connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val command = BridgeJsonCodec.parseCommand(text)
            Log.d("qqwe_tag WebSocketBridgeClient, onMessage, requestId:", command?.requestId ?: "unknown")

            if (command == null) {
                Log.d("qqwe_tag  WSBridge", "skip malformed payload")
                return
            }

            if (command.action != ACTION_GET_NOTES) {
                Log.d("qqwe_tag  WSBridge", "skip unknown action=${command.action}")
                return
            }

            Log.d("qqwe_tag  WSBridge", "action=get_notes requestId=${command.requestId}")
            scope.launch {
                val payload = try {
                    val events = repository.getAllEvents().first()
                    BridgeJsonCodec.buildSuccessResponse(
                        requestId = command.requestId,
                        events = events,
                    )
                } catch (error: Exception) {
                    BridgeJsonCodec.buildErrorResponse(
                        requestId = command.requestId,
                        message = error.message ?: "unknown error",
                    )
                }
                val sent = webSocket.send(payload)
                if (!sent) {
                    Log.d("qqwe_tag  WSBridge", "send failed requestId=${command.requestId}")
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
            Log.d("qqwe_tag  WSBridge", "closing code=$code reason=$reason")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("qqwe_tag  WSBridge", "closed code=$code reason=$reason")
            handleSocketEnded()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("qqwe_tag  WSBridge", "failure message=${t.message}")
            handleSocketEnded()
        }
    }

    private fun handleSocketEnded() {
        var needsReconnect = false
        synchronized(lock) {
            if (webSocket != null) {
                webSocket = null
            }
            if (shouldRun) {
                needsReconnect = true
            }
        }

        if (needsReconnect) scheduleReconnect()
    }

    private fun scheduleReconnect() {
        synchronized(lock) {
            if (!shouldRun) return
            if (reconnectJob?.isActive == true) return
            reconnectAttempt += 1
            val backoffMs = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * reconnectAttempt)

            reconnectJob = scope.launch {
                delay(backoffMs)
                synchronized(lock) {
                    reconnectJob = null
                }
                connect()
            }

            Log.d("qqwe_tag  WSBridge", "reconnect scheduled in ${backoffMs}ms")
        }
    }

    private companion object {
        private const val ACTION_GET_NOTES = "get_notes"
        private const val BASE_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 10_000L
    }
}
