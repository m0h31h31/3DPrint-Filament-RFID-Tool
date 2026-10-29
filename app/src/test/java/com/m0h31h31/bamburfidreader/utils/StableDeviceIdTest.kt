package com.m0h31h31.bamburfidreader.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.Executors

class StableDeviceIdTest {
    private val drmId = ByteArray(32) { (it + 1).toByte() }

    @Test fun reinstallRecreatesSameIdFromDrmWhenAndroidIdIsZero() {
        fun freshInstall() = StableDeviceId.getOrCreate(
            { null }, { "0000000000000000" }, {}, { drmId }
        )
        val first = freshInstall()
        // Fixed vector protects existing derived IDs against future algorithm changes.
        assertEquals("fff85ce1-f464-375f-be2f-d8ca5fba1aa3", first)
        assertEquals(first, freshInstall())
        UUID.fromString(first)
        val other = StableDeviceId.getOrCreate(
            { null }, { null }, {}, { ByteArray(32) { (it + 2).toByte() } }
        )
        assertNotEquals(first, other)
    }

    @Test fun storedRandomUuidRemainsUnchangedWithNewDrmSource() {
        val oldId = "9a6c2088-8f11-4dba-b281-877e85dc0723"
        assertEquals(oldId, StableDeviceId.getOrCreate(
            { oldId }, { error("Must not read system ID") },
            { error("Must not overwrite existing ID") }, { error("Must not read DRM") }
        ))
    }

    @Test fun validAndroidIdTakesPriorityOverDrm() {
        assertEquals("Ab1234567890cDef", StableDeviceId.getOrCreate(
            { null }, { "Ab1234567890cDef" }, {}, { error("Must not read DRM") }
        ))
    }

    @Test fun unavailableOrPlaceholderDrmFallsBackToRandomUuid() {
        val sources: List<() -> ByteArray?> = listOf(
            { null }, { byteArrayOf() }, { ByteArray(32) },
            { ByteArray(32) { 0xff.toByte() } },
            { "0000000000000000".toByteArray() },
            { throw UnsupportedOperationException("No DRM") }
        )
        sources.forEach { source ->
            val id = StableDeviceId.getOrCreate({ null }, { null }, {}, source)
            assertEquals(4, UUID.fromString(id).version())
        }
    }

    private class Device(var androidId: String?) {
        var stored: String? = null
        fun read(): String = StableDeviceId.getOrCreate({ stored }, { androidId }, { stored = it })
    }

    @Test fun preservesExistingAndroidIdExactly() {
        val device = Device("Ab1234567890cDef")
        assertEquals("Ab1234567890cDef", device.read())
        device.androidId = "0000000000000000"
        assertEquals("Ab1234567890cDef", device.read())
    }

    @Test fun invalidSystemIdsGetPersistentDistinctUuids() {
        val invalidIds = listOf(null, "", "  ", "0", "0000000000000000",
            "00000000-0000-0000-0000-000000000000", "null", "unknown")
        val ids = invalidIds.map { invalid ->
            val device = Device(invalid)
            val id = device.read()
            assertEquals(4, UUID.fromString(id).version())
            assertEquals(id, device.read())
            device.androidId = "1234567890abcdef"
            assertEquals(id, device.read())
            id
        }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun repairsInvalidStoredId() {
        val device = Device("1234567890abcdef")
        device.stored = "0000000000000000"
        assertEquals("1234567890abcdef", device.read())
        assertEquals("1234567890abcdef", device.stored)
    }

    @Test fun systemReadFailureGetsStableFallback() {
        var stored: String? = null
        fun read() = StableDeviceId.getOrCreate({ stored },
            { throw SecurityException("Unavailable") }, { stored = it })
        val id = read()
        assertEquals(4, UUID.fromString(id).version())
        assertEquals(id, read())
    }

    @Test fun simultaneousCallsShareOneId() {
        val device = Device(null)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val results = executor.invokeAll((1..40).map {
                java.util.concurrent.Callable { device.read() }
            }).map { it.get() }
            assertEquals(1, results.toSet().size)
            assertEquals(device.stored, results.first())
        } finally {
            executor.shutdownNow()
        }
    }
}
