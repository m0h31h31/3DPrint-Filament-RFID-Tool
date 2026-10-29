package com.m0h31h31.bamburfidreader.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BambuCloudRegionTest {
    private class Store : BambuCloudSessionStore {
        var session: BambuCloudSession? = null
        override fun loadSession() = session
        override fun saveSession(session: BambuCloudSession) { this.session = session }
        override fun clearSession() { session = null }
    }

    @Test fun legacySessionsDefaultToChina() {
        assertEquals(BambuCloudRegion.CHINA, BambuCloudRegion.fromStoredValue(null))
        assertEquals(BambuCloudRegion.CHINA, BambuCloudRegion.fromStoredValue(""))
        assertEquals(BambuCloudRegion.GLOBAL, BambuCloudRegion.fromStoredValue("global"))
    }

    @Test fun internationalLoginAndRestoredRequestsUseInternationalServer() = runBlocking {
        val requests = mutableListOf<BambuCloudHttpRequest>()
        val transport = object : BambuCloudTransport {
            override suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
                requests += request
                return BambuCloudHttpResponse(200, if (request.url.endsWith("/login"))
                    """{"accessToken":"global-token","refreshToken":"refresh","expiresIn":3600}"""
                else """{"uid":42,"name":"User","devices":[],"list":[],"total":0}""")
            }
        }
        val store = Store()
        fun repository() = BambuCloudRepository(
            serviceForRegion = { BambuCloudApiClient(transport, it.apiBaseUrl) }, sessionStore = store
        )
        val repo = repository()
        val result = repo.loginWithPassword("test@example.com", "test", BambuCloudRegion.GLOBAL)
        assertTrue(result is BambuCloudRepositoryResult.Success)
        assertEquals(BambuCloudRegion.GLOBAL, store.session?.region)
        val restored = repository()
        restored.refreshAccount()
        restored.fetchPrinters()
        restored.fetchFilaments()
        restored.fetchTasks()
        assertEquals(6, requests.size)
        assertTrue(requests.all { it.url.startsWith("https://api.bambulab.com/") })
        assertTrue(requests.drop(1).all { it.headers["Authorization"] == "Bearer global-token" })
        assertEquals("ssl://us.mqtt.bambulab.com:8883", store.session?.region?.mqttUrl)
        assertEquals("https://bambulab.com/", store.session?.region?.captchaBaseUrl)
    }

    @Test fun verificationFlowsUseSelectedRegion() = runBlocking {
        val requests = mutableListOf<BambuCloudHttpRequest>()
        val transport = object : BambuCloudTransport {
            override suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
                requests += request
                return BambuCloudHttpResponse(400, "{}")
            }
        }
        val repo = BambuCloudRepository(
            serviceForRegion = { BambuCloudApiClient(transport, it.apiBaseUrl) }, sessionStore = Store()
        )
        repo.loginWithCode("user", "password", "123456", BambuCloudRegion.GLOBAL)
        repo.loginWithPasswordAndCaptcha("user", "password",
            BambuCloudCaptchaResult("id", "lot", "pass", "time", "output"), BambuCloudRegion.GLOBAL)
        repo.loginWithPassword("user", "password")
        assertEquals("https://api.bambulab.com/v1/user-service/user/login", requests[0].url)
        assertEquals(requests[0].url, requests[1].url)
        assertTrue(requests[1].headers.containsKey("x-bbl-captcha-result"))
        assertEquals("https://api.bambulab.cn/v1/user-service/user/login", requests[2].url)
    }

    @Test fun failedInternationalLoginDoesNotReplaceExistingDomesticSession() = runBlocking {
        val original = BambuCloudSession(BambuCloudTokens("cn-token", "refresh", 60),
            BambuCloudAccount(1, "User", "", "", ""), 0, 60000)
        val store = Store().apply { session = original }
        val requests = mutableListOf<BambuCloudHttpRequest>()
        val transport = object : BambuCloudTransport {
            override suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse {
                requests += request
                return BambuCloudHttpResponse(401, "{}")
            }
        }
        val repo = BambuCloudRepository(
            serviceForRegion = { BambuCloudApiClient(transport, it.apiBaseUrl) }, sessionStore = store
        )
        assertTrue(repo.loginWithPassword("user", "bad", BambuCloudRegion.GLOBAL)
            is BambuCloudRepositoryResult.Failure)
        assertEquals(original, store.session)
        repo.fetchPrinters()
        assertTrue(requests.last().url.startsWith("https://api.bambulab.cn/"))
        assertEquals("Bearer cn-token", requests.last().headers["Authorization"])
    }
}
