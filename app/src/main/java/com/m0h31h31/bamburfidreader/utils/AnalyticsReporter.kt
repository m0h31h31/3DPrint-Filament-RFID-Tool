package com.m0h31h31.bamburfidreader.utils

import android.content.Context
import android.os.Build
import com.m0h31h31.bamburfidreader.BuildConfig
import org.json.JSONObject
import java.util.UUID

object AnalyticsReporter {
    private const val PREFS_NAME = "analytics_prefs"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_INSTALL_REPORTED = "install_reported"

    internal fun apiKeyHeaders(): Map<String, String> = buildMap {
        if (BuildConfig.EVENT_API_KEY.isNotBlank()) {
            put("X-API-Key", BuildConfig.EVENT_API_KEY)
        }
    }

    /**
     * 获取本设备的稳定 UUID。首次调用时生成并持久化，后续调用返回同一值。
     * 卸载重装后会重新生成。
     */
    fun getInstallId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL_ID, it).apply()
        }
    }

    /**
     * 上报异常标签到服务器。
     * @param trayUid 料盘 UID（必须）
     * @param cardUid 卡片硬件 NFC UID（可选）
     * @return true 表示上报成功（含重复上报），false 表示请求失败。
     */
    suspend fun reportAnomaly(context: Context, trayUid: String, cardUid: String = ""): Boolean {
        val endpoint = ConfigManager.getAnomalyReportEndpoint(context).value
        if (endpoint.isBlank()) return false
        val installId = getInstallId(context)
        val payload = org.json.JSONObject().apply {
            put("uid", trayUid.lowercase().trim())
            put("card_uid", cardUid.lowercase().trim())
            put("device_id", installId)
            put("timestamp_ms", System.currentTimeMillis())
        }
        return NetworkUtils.postJson(endpoint, payload, apiKeyHeaders())
    }

    /**
     * 从服务器拉取异常 UID 列表并返回。
     * @return 异常 UID 集合，失败时返回 null。
     */
    suspend fun fetchAnomalyUids(context: Context): Set<String>? {
        val endpoint = ConfigManager.getAnomalyUidsEndpoint(context).value
        if (endpoint.isBlank()) return null
        val json = NetworkUtils.getJson(endpoint, apiKeyHeaders()) ?: return null
        val arr = json.optJSONArray("uids") ?: return null
        val result = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val uid = arr.optString(i).lowercase().trim()
            if (uid.isNotBlank()) result.add(uid)
        }
        return result
    }

    suspend fun saveNickname(context: Context, nickname: String): Boolean {
        val endpoint = ConfigManager.getNicknameEndpoint(context).value
        if (endpoint.isBlank()) return false
        val installId = getInstallId(context)
        val payload = JSONObject().apply {
            put("install_id", installId)
            put("nickname", nickname.trim())
        }
        return NetworkUtils.postJson(endpoint, payload, apiKeyHeaders())
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
