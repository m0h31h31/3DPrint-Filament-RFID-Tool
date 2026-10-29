package com.m0h31h31.bamburfidreader.utils

import java.util.UUID
import java.security.MessageDigest

/** Keeps existing IDs verbatim; only unavailable/placeholder IDs need a UUID. */
internal object StableDeviceId {
    // Serialize reading and saving as one operation, including concurrent upload callers.
    @Synchronized
    fun getOrCreate(
        readStored: () -> String?,
        readAndroidId: () -> String?,
        save: (String) -> Unit,
        readDrmId: () -> ByteArray? = { null }
    ): String {
        readStored()?.takeIf(::isUsable)?.let { return it }
        val androidId = try {
            readAndroidId()
        } catch (_: Exception) {
            null
        }
        val id = androidId?.takeIf(::isUsable)
            ?: drmUuid(readDrmId)
            ?: UUID.randomUUID().toString()
        save(id)
        return id
    }

    private fun drmUuid(readDrmId: () -> ByteArray?): String? {
        val bytes = try {
            readDrmId()
        } catch (_: Exception) {
            null
        } ?: return null
        // Reject obvious vendor placeholders before hashing, or they become shared IDs.
        if (bytes.size < 16 || bytes.all { it == 0.toByte() } ||
            bytes.all { it == 0xff.toByte() } ||
            bytes.all { it == '0'.code.toByte() || it == '-'.code.toByte() }) return null
        // Keep this namespace/algorithm unchanged: it is the reinstall identity contract.
        // Never store or upload the raw DRM ID. Scope the derived UUID to this app.
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("com.m0h31h31.bamburfidreader:widevine-device-id:v1:".toByteArray(Charsets.UTF_8))
        return UUID.nameUUIDFromBytes(digest.digest(bytes)).toString()
    }

    private fun isUsable(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.equals("null", ignoreCase = true) ||
            trimmed.equals("unknown", ignoreCase = true)) return false
        return trimmed.any { it != '0' && it != '-' && !it.isWhitespace() }
    }
}
