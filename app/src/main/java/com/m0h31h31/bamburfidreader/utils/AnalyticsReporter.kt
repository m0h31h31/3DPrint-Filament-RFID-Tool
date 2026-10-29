package com.m0h31h31.bamburfidreader.utils

import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import com.m0h31h31.bamburfidreader.BuildConfig
import org.json.JSONObject
import java.util.UUID

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val forceUpdate: Boolean
)

object AnalyticsReporter {
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_INSTALL_REPORTED = "install_reported"

    internal fun apiKeyHeaders(): Map<String, String> = buildMap {
        if (BuildConfig.EVENT_API_KEY.isNotBlank()) {
            put("X-API-Key", BuildConfig.EVENT_API_KEY)
        }
    }

    /**
     * 保留有效 ANDROID_ID；异常时优先使用应用范围的 Widevine 派生 UUID。
     * 已保存的编号优先，避免后续系统返回值变化导致身份切换。
     * Widevine 也不可用时才使用随机 UUID，此兜底无法保证清除数据或重装后不变。
     */
    fun getInstallId(context: Context): String {
        val prefs = context.getSharedPreferences("device_identity", Context.MODE_PRIVATE)
        return StableDeviceId.getOrCreate(
            readStored = { prefs.getString("install_id", null) },
            readAndroidId = {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            },
            save = { id ->
                if (!prefs.edit().putString("install_id", id).commit()) {
                    com.m0h31h31.bamburfidreader.logging.logDebug(
                        "Device identity persistence failed; current process retains the ID"
                    )
                }
            },
            readDrmId = {
                try {
                    MediaDrm(UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")).use { drm ->
                        drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
                    }
                } catch (_: Exception) {
                    com.m0h31h31.bamburfidreader.logging.logDebug(
                        "Device identity: Widevine unavailable; using persisted random UUID"
                    )
                    null
                }
            }
        )
    }

    /**
     * 上报异常标签到服务器。
     * @param cardUid 卡片硬件 NFC UID（8字符，必须）
     * @return true 表示上报成功（含重复上报），false 表示请求失败。
     */
    suspend fun reportAnomaly(context: Context, cardUid: String): Boolean {
        val cfg = ConfigManager.getAnomalyReportEndpoint(context)
        val endpoint = cfg.value
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.reportAnomaly endpoint=$endpoint (from config: ${cfg.isUsable})")
        if (endpoint.isBlank()) {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.reportAnomaly: endpoint blank, skipped")
            return false
        }
        val installId = getInstallId(context)
        val payload = org.json.JSONObject().apply {
            put("uid", cardUid.uppercase().trim())
            put("device_id", installId)
            put("timestamp_ms", System.currentTimeMillis())
        }
        val ok = NetworkUtils.postJson(endpoint, payload, apiKeyHeaders())
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.reportAnomaly result=$ok uid=$cardUid")
        return ok
    }

    /**
     * 从服务器拉取异常 UID 列表及上报计数并返回。
     * 服务端返回格式：{"uids": [{"uid": "...", "count": N}, ...]}
     * @return uid → 上报人数 的映射，失败时返回 null。
     */
    suspend fun fetchAnomalyUids(context: Context): Map<String, Int>? {
        val cfg = ConfigManager.getAnomalyUidsEndpoint(context)
        val endpoint = cfg.value
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchAnomalyUids endpoint=$endpoint (from config: ${cfg.isUsable})")
        if (endpoint.isBlank()) {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchAnomalyUids: endpoint blank, skipped")
            return null
        }
        val json = NetworkUtils.getJson(endpoint, apiKeyHeaders())
        if (json == null) {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchAnomalyUids: response null")
            return null
        }
        val arr = json.optJSONArray("uids")
        if (arr == null) {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchAnomalyUids: no 'uids' array in response: $json")
            return null
        }
        val result = mutableMapOf<String, Int>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i)
            if (item != null) {
                val uid = item.optString("uid").uppercase().trim()
                val count = item.optInt("count", 1).coerceAtLeast(1)
                if (uid.isNotBlank()) result[uid] = count
            }
        }
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchAnomalyUids: got ${result.size} uids")
        return result
    }

    /**
     * 上报 UID 复制事件到服务器。
     * @param uid 被复制的拓竹标签 UID（sourceUid）
     * @return 服务端返回的复制人数，失败时返回 null。
     */
    suspend fun reportUidCopy(context: Context, uid: String): Int? {
        val endpoint = ConfigManager.getUidCopyEndpoint(context).value
        if (endpoint.isBlank()) return null
        val installId = getInstallId(context)
        val payload = JSONObject().apply {
            put("uid", uid.uppercase().trim())
            put("device_id", installId)
            put("timestamp_ms", System.currentTimeMillis())
        }
        val json = NetworkUtils.postJsonGetResponse(endpoint, payload, apiKeyHeaders())
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.reportUidCopy uid=$uid result=$json")
        return json?.optInt("copy_count", -1)?.takeIf { it >= 0 }
    }

    /**
     * 查询指定 UID 已被复制的人数。
     * @param uid 要查询的标签 UID
     * @return 复制人数，失败时返回 null。
     */
    suspend fun fetchUidCopyCount(context: Context, uid: String): Int? {
        val baseEndpoint = ConfigManager.getUidCopyEndpoint(context).value
        if (baseEndpoint.isBlank()) return null
        val endpoint = "${baseEndpoint.trimEnd('/')}/${uid.uppercase().trim()}/count"
        val json = NetworkUtils.getJson(endpoint, apiKeyHeaders())
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.fetchUidCopyCount uid=$uid result=$json")
        return json?.optInt("copy_count", -1)?.takeIf { it >= 0 }
    }

    suspend fun saveNickname(context: Context, nickname: String): Boolean {
        val cfg = ConfigManager.getNicknameEndpoint(context)
        val endpoint = cfg.value
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.saveNickname endpoint=$endpoint (from config: ${cfg.isUsable})")
        if (endpoint.isBlank()) {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.saveNickname: endpoint blank, skipped")
            return false
        }
        val installId = getInstallId(context)
        val payload = JSONObject().apply {
            put("install_id", installId)
            put("nickname", nickname.trim())
        }
        val ok = NetworkUtils.postJson(endpoint, payload, apiKeyHeaders())
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.saveNickname result=$ok")
        return ok
    }

    /**
     * 查询服务端最新版本。若有新版本（versionCode 大于当前）则返回 UpdateInfo，否则返回 null。
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? {
        val cfg = ConfigManager.getUpdateEndpoint(context)
        val endpoint = cfg.value
        com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.checkForUpdate endpoint=$endpoint")
        if (endpoint.isBlank()) return null
        val json = NetworkUtils.getJson(endpoint, apiKeyHeaders()) ?: return null
        val remoteVersionCode = json.optInt("versionCode", 0)
        if (remoteVersionCode <= 0) return null
        val downloadUrl = json.optString("downloadUrl", "")
        if (downloadUrl.isBlank()) return null
        val currentVersionCode = try {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
        } catch (_: Exception) { return null }
        if (remoteVersionCode <= currentVersionCode) return null
        return UpdateInfo(
            versionCode = remoteVersionCode,
            versionName = json.optString("versionName", ""),
            downloadUrl = downloadUrl,
            changelog   = json.optString("changelog", ""),
            forceUpdate = json.optBoolean("forceUpdate", false)
        ).also {
            com.m0h31h31.bamburfidreader.logging.logDebug("AnalyticsReporter.checkForUpdate: new version ${it.versionName} (code=${it.versionCode})")
        }
    }

    suspend fun reportInstallAndLaunch(context: Context) {
        val endpoint = ConfigManager.getAppConfigUserCountEndpoint(context).value
        if (endpoint.isBlank()) return
        val headers = apiKeyHeaders()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val installId = getInstallId(context)

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val commonPayload = JSONObject().apply {
            put("install_id", installId)
            put("package_name", context.packageName)
            put("version_name", packageInfo.versionName.orEmpty())
            put("version_code", packageInfo.longVersionCode)
            put("platform", "android")
            put("sdk_int", Build.VERSION.SDK_INT)
            put("manufacturer", Build.MANUFACTURER.orEmpty())
            put("model", Build.MODEL.orEmpty())
            put("timestamp_ms", System.currentTimeMillis())
        }

        if (!prefs.getBoolean(KEY_INSTALL_REPORTED, false)) {
            val installPayload = JSONObject(commonPayload.toString()).apply {
                put("event", "install")
            }
            val installSent = NetworkUtils.postJson(endpoint, installPayload, headers)
            if (installSent) {
                prefs.edit().putBoolean(KEY_INSTALL_REPORTED, true).apply()
            }
        }

        val launchPayload = JSONObject(commonPayload.toString()).apply {
            put("event", "launch")
        }
        NetworkUtils.postJson(endpoint, launchPayload, headers)
    }
}
