package com.m0h31h31.bamburfidreader.ui.screens

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilament
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalog
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalogInfo
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinter
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinterResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepository
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepositoryResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudSession
import com.m0h31h31.bamburfidreader.cloud.BambuMqttConnectionState
import com.m0h31h31.bamburfidreader.cloud.BambuMqttRealtimeClient
import com.m0h31h31.bamburfidreader.cloud.BambuPrinterRealtimeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 进程级单例，持有 Bambu 云连接的会话、打印机/耗材数据以及 MQTT 实时客户端。
 *
 * 状态独立于 Compose 组合，因此离开"连接"页时不会断开 MQTT、不会销毁卡片；
 * 只有在用户主动退出登录或在配置页关闭"拓竹云连接"功能时才会断开。
 */
class BambuCloudController private constructor(appContext: Context) {

    val repository = BambuCloudRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val sessionState = mutableStateOf<BambuCloudSession?>(null)
    val statusMessageState = mutableStateOf("")
    val printersState = mutableStateOf<List<BambuCloudPrinter>>(emptyList())
    val printersLoadingState = mutableStateOf(false)
    val printerStatusMessageState = mutableStateOf("")
    val realtimeStatusesState = mutableStateOf<Map<String, BambuPrinterRealtimeStatus>>(emptyMap())
    val mqttStatusMessageState = mutableStateOf("")
    val filamentsState = mutableStateOf<List<BambuCloudFilament>>(emptyList())
    val filamentCatalogInfoState = mutableStateOf<Map<Long, BambuCloudFilamentCatalogInfo>>(emptyMap())
    val filamentsLoadingState = mutableStateOf(false)
    val filamentsSyncingState = mutableStateOf(false)
    val filamentStatusMessageState = mutableStateOf("")

    private var mqttClient: BambuMqttRealtimeClient? = null
    private var mqttKey: String = ""
    private var loaded = false

    /** 首次进入页面时加载一次会话与列表；再次进入不重复加载，保留已有卡片。 */
    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        scope.launch { reload(context) }
    }

    private suspend fun reload(context: Context) {
        val s = repository.loadSession()
        sessionState.value = s
        statusMessageState.value = if (s == null) {
            context.getString(R.string.cloud_status_not_logged_in)
        } else {
            context.getString(R.string.cloud_status_logged_in)
        }
        if (s != null) {
            when (val result = repository.refreshAccount()) {
                is BambuCloudRepositoryResult.Success -> sessionState.value = result.session
                else -> Unit
            }
            refreshCloudLists(context)
        }
    }

    suspend fun refreshPrinters(context: Context) {
        if (repository.loadSession() == null) {
            printersState.value = emptyList()
            printerStatusMessageState.value = ""
            realtimeStatusesState.value = emptyMap()
            mqttStatusMessageState.value = ""
            printersLoadingState.value = false
            return
        }
        printersLoadingState.value = true
        printerStatusMessageState.value = context.getString(R.string.cloud_printers_loading)
        when (val result = repository.fetchPrinters()) {
            is BambuCloudPrinterResult.Success -> {
                printersState.value = result.printers
                printerStatusMessageState.value = if (result.printers.isEmpty()) {
                    context.getString(R.string.cloud_printers_empty)
                } else {
                    ""
                }
            }
            is BambuCloudPrinterResult.Failure -> {
                printerStatusMessageState.value = context.getString(
                    R.string.cloud_printers_refresh_failed,
                    result.message
                )
            }
        }
        printersLoadingState.value = false
    }

    suspend fun refreshFilaments(context: Context) {
        if (repository.loadSession() == null) {
            filamentsState.value = emptyList()
            filamentCatalogInfoState.value = emptyMap()
            filamentStatusMessageState.value = ""
            filamentsLoadingState.value = false
            filamentsSyncingState.value = false
            return
        }
        filamentsLoadingState.value = true
        filamentStatusMessageState.value = context.getString(R.string.cloud_filaments_loading)
        when (val result = repository.fetchFilaments(offset = 0, limit = 20)) {
            is BambuCloudFilamentResult.Success -> {
                filamentsState.value = result.filaments
                filamentStatusMessageState.value = if (result.filaments.isEmpty()) {
                    context.getString(R.string.cloud_filaments_empty)
                } else {
                    ""
                }
            }
            is BambuCloudFilamentResult.Failure -> {
                filamentStatusMessageState.value = context.getString(
                    R.string.cloud_filaments_refresh_failed,
                    result.message
                )
            }
        }
        filamentsLoadingState.value = false
    }

    suspend fun refreshCloudLists(context: Context) {
        refreshPrinters(context)
        refreshFilaments(context)
    }

    /** 强制刷新所有云端接口：账号、打印机、耗材，并重连实时状态。 */
    fun forceRefreshAll(context: Context) {
        if (repository.loadSession() == null) return
        scope.launch {
            when (val result = repository.refreshAccount()) {
                is BambuCloudRepositoryResult.Success -> sessionState.value = result.session
                else -> Unit
            }
            refreshCloudLists(context)
            // 设备/会话可能变化，强制重建 MQTT 连接
            mqttClient?.stop()
            mqttClient = null
            mqttKey = ""
            realtimeStatusesState.value = emptyMap()
            syncRealtime(context)
        }
    }

    /** 根据当前会话与打印机列表维护 MQTT 连接；key 不变时不重连，离开页面也不会断开。 */
    fun syncRealtime(context: Context) {
        val session = sessionState.value
        val deviceIds = printersState.value.map { it.deviceId }.filter { it.isNotBlank() }
        val key = if (session == null) {
            ""
        } else {
            "${session.region.storedValue}:${session.account.uid}:${session.tokens.accessToken}:${deviceIds.joinToString("|")}"
        }
        if (key == mqttKey && (key.isBlank() || mqttClient != null)) return
        mqttClient?.stop()
        mqttClient = null
        mqttKey = key
        if (session == null || deviceIds.isEmpty()) {
            mqttStatusMessageState.value = ""
            return
        }
        val client = BambuMqttRealtimeClient(
            session = session,
            deviceIds = deviceIds,
            onStatus = { status ->
                scope.launch {
                    realtimeStatusesState.value = realtimeStatusesState.value + (
                        status.deviceId to mergeRealtimeStatus(
                            realtimeStatusesState.value[status.deviceId],
                            status
                        )
                    )
                }
            },
            onConnectionState = { state, error ->
                scope.launch {
                    mqttStatusMessageState.value = when (state) {
                        BambuMqttConnectionState.DISCONNECTED -> ""
                        BambuMqttConnectionState.CONNECTING ->
                            context.getString(R.string.cloud_mqtt_connecting)
                        BambuMqttConnectionState.CONNECTED ->
                            context.getString(R.string.cloud_mqtt_connected)
                        BambuMqttConnectionState.FAILED ->
                            context.getString(
                                R.string.cloud_mqtt_failed,
                                error.ifBlank { context.getString(R.string.label_unknown) }
                            )
                    }
                }
            }
        )
        client.start()
        mqttClient = client
    }

    fun syncFilamentRemainingToLocal(context: Context, syncer: suspend (List<BambuCloudFilament>) -> Int) {
        val filaments = filamentsState.value
        if (filaments.isEmpty() || filamentsSyncingState.value) return
        filamentsSyncingState.value = true
        filamentStatusMessageState.value = context.getString(R.string.cloud_filaments_syncing_remaining)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { syncer(filaments) }
            }
            result.onSuccess { updatedCount ->
                filamentStatusMessageState.value = if (updatedCount > 0) {
                    context.getString(R.string.cloud_filaments_sync_remaining_success, updatedCount)
                } else {
                    context.getString(R.string.cloud_filaments_sync_remaining_no_match)
                }
            }.onFailure { error ->
                filamentStatusMessageState.value = context.getString(
                    R.string.cloud_filaments_sync_remaining_failed,
                    error.message.orEmpty().ifBlank { context.getString(R.string.label_unknown) }
                )
            }
            filamentsSyncingState.value = false
        }
    }

    fun recomputeCatalog(context: Context) {
        val filaments = filamentsState.value
        scope.launch {
            filamentCatalogInfoState.value = if (filaments.isEmpty()) {
                emptyMap()
            } else {
                withContext(Dispatchers.IO) {
                    filaments.mapNotNull { filament ->
                        BambuCloudFilamentCatalog.lookup(context, filament)?.let { info ->
                            filament.id to info
                        }
                    }.toMap()
                }
            }
        }
    }

    fun applySession(session: BambuCloudSession) {
        sessionState.value = session
    }

    fun logout(context: Context) {
        repository.logout()
        mqttClient?.stop()
        mqttClient = null
        mqttKey = ""
        sessionState.value = null
        printersState.value = emptyList()
        filamentsState.value = emptyList()
        filamentCatalogInfoState.value = emptyMap()
        printerStatusMessageState.value = ""
        filamentStatusMessageState.value = ""
        filamentsSyncingState.value = false
        realtimeStatusesState.value = emptyMap()
        mqttStatusMessageState.value = ""
        statusMessageState.value = context.getString(R.string.cloud_status_logged_out)
    }

    /** 关闭"拓竹云连接"功能时调用：断开实时连接，但保留登录态，便于再次开启快速恢复。 */
    fun shutdownRealtime() {
        mqttClient?.stop()
        mqttClient = null
        mqttKey = ""
        realtimeStatusesState.value = emptyMap()
        mqttStatusMessageState.value = ""
    }

    companion object {
        @Volatile
        private var instance: BambuCloudController? = null

        fun get(context: Context): BambuCloudController =
            instance ?: synchronized(this) {
                instance ?: BambuCloudController(context.applicationContext).also { instance = it }
            }
    }
}
