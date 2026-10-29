package com.m0h31h31.bamburfidreader.cloud

import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.net.ssl.SSLContext

class BambuMqttRealtimeClient(
    private val session: BambuCloudSession,
    private val deviceIds: List<String>,
    private val onStatus: (BambuPrinterRealtimeStatus) -> Unit,
    private val onConnectionState: (BambuMqttConnectionState, String) -> Unit
) {
    private var client: MqttAsyncClient? = null

    fun start() {
        val cleanDeviceIds = deviceIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanDeviceIds.isEmpty() || session.tokens.accessToken.isBlank()) return
        onConnectionState(BambuMqttConnectionState.CONNECTING, "")
        val mqttClient = MqttAsyncClient(
            session.region.mqttUrl,
            "bambu-rfid-${session.account.uid}-${UUID.randomUUID()}",
            MemoryPersistence()
        )
        client = mqttClient
        mqttClient.setCallback(
            object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    onConnectionState(BambuMqttConnectionState.CONNECTED, "")
                    cleanDeviceIds.forEach { deviceId ->
                        subscribeToReport(mqttClient, deviceId)
                        requestFullStatus(mqttClient, deviceId)
                    }
                }

                override fun connectionLost(cause: Throwable?) {
                    onConnectionState(BambuMqttConnectionState.FAILED, cause?.message.orEmpty())
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val deviceId = topic?.substringAfter("device/")?.substringBefore("/report").orEmpty()
                    val payload = message?.payload?.toString(StandardCharsets.UTF_8).orEmpty()
                    BambuMqttStatusParser.parseReport(deviceId, payload)?.let(onStatus)
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            }
        )
        mqttClient.connect(
            MqttConnectOptions().apply {
                userName = "u_${session.account.uid}"
                password = session.tokens.accessToken.toCharArray()
                isCleanSession = true
                isAutomaticReconnect = true
                connectionTimeout = 10
                keepAliveInterval = 30
                socketFactory = SSLContext.getDefault().socketFactory
                sslHostnameVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
            },
            null,
            object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) = Unit

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    onConnectionState(BambuMqttConnectionState.FAILED, exception?.message.orEmpty())
                }
            }
        )
    }

    fun stop() {
        val mqttClient = client
        client = null
        runCatching {
            if (mqttClient?.isConnected == true) {
                mqttClient.disconnect()
            }
        }
        runCatching {
            mqttClient?.close()
        }
        onConnectionState(BambuMqttConnectionState.DISCONNECTED, "")
    }

    private fun subscribeToReport(client: MqttAsyncClient, deviceId: String) {
        runCatching {
            client.subscribe("device/$deviceId/report", 0)
        }
    }

    private fun requestFullStatus(client: MqttAsyncClient, deviceId: String) {
        val payload = JSONObject()
            .put(
                "pushing",
                JSONObject()
                    .put("sequence_id", System.currentTimeMillis().toString())
                    .put("command", "pushall")
                    .put("version", 1)
                    .put("push_target", 1)
            )
            .toString()
        runCatching {
            client.publish(
                "device/$deviceId/request",
                MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
                    qos = 0
                    isRetained = false
                }
            )
        }
    }

}
