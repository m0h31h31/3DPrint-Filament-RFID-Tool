package com.m0h31h31.bamburfidreader.utils

import android.content.Context
import com.m0h31h31.bamburfidreader.RawTagReadData
import com.m0h31h31.bamburfidreader.logDebug
import org.json.JSONArray
import org.json.JSONObject

object TagShareUploader {

    // ── 完整性判断 ───────────────────────────────────────────────────────────

    /**
     * 标签完整性判断：uid 非空，且至少读到部分 block 数据。
     */
    fun isComplete(rawData: RawTagReadData): Boolean {
        return rawData.uidHex.isNotBlank() &&
                rawData.rawBlocks.any { it != null && it.isNotEmpty() }
    }

    // ── 上传 ─────────────────────────────────────────────────────────────────

    /**
     * 上传标签原始数据（拓竹 / 快造通用）：发送 brand、uid、blocks、keys、device_id。
     */
    suspend fun uploadRawTag(context: Context, brand: String, rawData: RawTagReadData): Boolean {
        val endpoint = ConfigManager.getTagShareEndpoint(context)
        logDebug("TagShareUploader.uploadRawTag endpoint=${endpoint.value} isUsable=${endpoint.isUsable} brand=$brand uid=${rawData.uidHex}")
        if (!endpoint.isUsable) {
            logDebug("TagShareUploader: tagShareEndpoint 未配置，跳过上传")
            return false
        }
        val deviceId = AnalyticsReporter.getInstallId(context)
        return try {
            val ok = NetworkUtils.postJson(endpoint.value, buildRawPayload(brand, rawData, deviceId), AnalyticsReporter.apiKeyHeaders())
            logDebug(if (ok) "TagShareUploader: 上传成功 brand=$brand uid=${rawData.uidHex}"
                     else    "TagShareUploader: 上传失败 brand=$brand uid=${rawData.uidHex}")
            ok
        } catch (e: Exception) {
            logDebug("TagShareUploader: 上传异常: ${e.message}")
            false
        }
    }

    // ── Payload 构建 ─────────────────────────────────────────────────────────

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun buildRawPayload(brand: String, rawData: RawTagReadData, deviceId: String): JSONObject {
        val blocksArray = JSONArray().apply {
            rawData.rawBlocks.forEachIndexed { blockIndex, block ->
                val blockInSector = blockIndex % 4
                if (blockInSector == 3 && block != null) {
                    // 重建 trailer：用真实密钥替换硬件返回的 0x00 占位
                    val sector = blockIndex / 4
                    val trailer = block.copyOf()
                    rawData.sectorKeys.getOrNull(sector)?.first
                        ?.takeIf { it.size == 6 }?.copyInto(trailer, 0)
                    rawData.sectorKeys.getOrNull(sector)?.second
                        ?.takeIf { it.size == 6 }?.copyInto(trailer, 10)
                    put(trailer.toHex())
                } else {
                    put(block?.toHex() ?: "")
                }
            }
        }
        val keysArray = JSONArray().apply {
            rawData.sectorKeys.forEach { (keyA, keyB) ->
                put(JSONObject().apply {
                    put("a", keyA?.toHex() ?: "")
                    put("b", keyB?.toHex() ?: "")
                })
            }
        }
        return JSONObject().apply {
            put("brand", brand)
            put("uid", rawData.uidHex)
            put("blocks", blocksArray)
            put("keys", keysArray)
            put("device_id", deviceId)
        }
    }
}
