package com.m0h31h31.bamburfidreader.cloud

import android.content.Context

class BambuCloudRepository(
    private val serviceForRegion: (BambuCloudRegion) -> BambuCloudService,
    private val sessionStore: BambuCloudSessionStore,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    constructor(
        service: BambuCloudService,
        sessionStore: BambuCloudSessionStore,
        nowMillis: () -> Long = { System.currentTimeMillis() }
    ) : this({ service }, sessionStore, nowMillis)

    constructor(context: Context) : this(
        serviceForRegion = { region -> BambuCloudApiClient(BambuCloudHttpTransport(), region.apiBaseUrl) },
        sessionStore = SharedPreferencesBambuCloudSessionStore(context)
    )

    fun loadSession(): BambuCloudSession? = sessionStore.loadSession()

    fun logout() {
        sessionStore.clearSession()
    }

    suspend fun refreshAccount(): BambuCloudRepositoryResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudRepositoryResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        val account = when (val accountResult = service.fetchAccount(currentSession.tokens.accessToken)) {
            is BambuCloudApiResult.Success -> accountResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(accountResult.captchaId, accountResult.scene, accountResult.message)
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(accountResult.message)
            }
        }
        val refreshed = currentSession.copy(account = account)
        sessionStore.saveSession(refreshed)
        return BambuCloudRepositoryResult.Success(refreshed)
    }

    suspend fun fetchPrinters(): BambuCloudPrinterResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudPrinterResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        return when (val result = service.fetchPrinters(currentSession.tokens.accessToken)) {
            is BambuCloudApiResult.Success -> BambuCloudPrinterResult.Success(result.value)
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudPrinterResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudPrinterResult.Failure(result.message)
            is BambuCloudApiResult.Failure -> BambuCloudPrinterResult.Failure(result.message)
        }
    }

    suspend fun fetchFilaments(
        offset: Int = 0,
        limit: Int = 20
    ): BambuCloudFilamentResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudFilamentResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        return when (val result = service.fetchFilaments(currentSession.tokens.accessToken, offset, limit)) {
            is BambuCloudApiResult.Success -> BambuCloudFilamentResult.Success(result.value)
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudFilamentResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudFilamentResult.Failure(result.message)
            is BambuCloudApiResult.Failure -> BambuCloudFilamentResult.Failure(result.message)
        }
    }

    suspend fun updateFilament(update: BambuCloudFilamentUpdate): BambuCloudFilamentMutationResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudFilamentMutationResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        return when (val result = service.updateFilament(currentSession.tokens.accessToken, update)) {
            is BambuCloudApiResult.Success -> BambuCloudFilamentMutationResult.Success
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudFilamentMutationResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudFilamentMutationResult.Failure(result.message)
            is BambuCloudApiResult.Failure -> BambuCloudFilamentMutationResult.Failure(result.message)
        }
    }

    suspend fun deleteFilament(filament: BambuCloudFilament): BambuCloudFilamentMutationResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudFilamentMutationResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        return when (
            val result = service.deleteFilaments(
                accessToken = currentSession.tokens.accessToken,
                ids = listOf(filament.id),
                rfids = listOf(filament.rfid)
            )
        ) {
            is BambuCloudApiResult.Success -> BambuCloudFilamentMutationResult.Success
            BambuCloudApiResult.VerificationCodeRequired -> {
                BambuCloudFilamentMutationResult.Failure("Verification code required")
            }
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudFilamentMutationResult.Failure(result.message)
            is BambuCloudApiResult.Failure -> BambuCloudFilamentMutationResult.Failure(result.message)
        }
    }

    suspend fun fetchTasks(
        offset: Int = 0,
        limit: Int = 50,
        status: Int = 0
    ): BambuCloudTaskResult {
        val currentSession = sessionStore.loadSession()
            ?: return BambuCloudTaskResult.Failure("Not logged in")
        val service = serviceForRegion(currentSession.region)
        return when (val result = service.fetchTasks(currentSession.tokens.accessToken, offset, limit, status)) {
            is BambuCloudApiResult.Success -> BambuCloudTaskResult.Success(result.value)
            BambuCloudApiResult.VerificationCodeRequired -> BambuCloudTaskResult.Failure("Verification code required")
            is BambuCloudApiResult.CaptchaRequired -> BambuCloudTaskResult.Failure(result.message)
            is BambuCloudApiResult.Failure -> BambuCloudTaskResult.Failure(result.message)
        }
    }

    suspend fun loginWithPassword(
        account: String,
        password: String,
        region: BambuCloudRegion = BambuCloudRegion.CHINA
    ): BambuCloudRepositoryResult {
        val service = serviceForRegion(region)
        return completeLogin(service.loginWithPassword(account, password), region, service)
    }

    suspend fun loginWithCode(
        account: String,
        password: String,
        code: String,
        region: BambuCloudRegion = BambuCloudRegion.CHINA
    ): BambuCloudRepositoryResult {
        val service = serviceForRegion(region)
        return completeLogin(service.loginWithCode(account, password, code), region, service)
    }

    /** 携带极验 v4 验证结果重新登录（在 App 内完成人机验证后调用）。 */
    suspend fun loginWithPasswordAndCaptcha(
        account: String,
        password: String,
        captcha: BambuCloudCaptchaResult,
        region: BambuCloudRegion = BambuCloudRegion.CHINA
    ): BambuCloudRepositoryResult {
        val service = serviceForRegion(region)
        return completeLogin(service.loginWithCaptcha(account, password, captcha), region, service)
    }

    private suspend fun completeLogin(
        tokenResult: BambuCloudApiResult<BambuCloudTokens>,
        region: BambuCloudRegion,
        service: BambuCloudService
    ): BambuCloudRepositoryResult {
        val tokens = when (tokenResult) {
            is BambuCloudApiResult.Success -> tokenResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(tokenResult.captchaId, tokenResult.scene, tokenResult.message)
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(
                    message = tokenResult.message,
                    loginFailureReason = tokenResult.loginFailureReason
                )
            }
        }
        if (tokens.accessToken.isBlank()) {
            return BambuCloudRepositoryResult.Failure("Missing access token")
        }
        val account = when (val accountResult = service.fetchAccount(tokens.accessToken)) {
            is BambuCloudApiResult.Success -> accountResult.value
            BambuCloudApiResult.VerificationCodeRequired -> {
                return BambuCloudRepositoryResult.VerificationCodeRequired
            }
            is BambuCloudApiResult.CaptchaRequired -> {
                return BambuCloudRepositoryResult.CaptchaRequired(accountResult.captchaId, accountResult.scene, accountResult.message)
            }
            is BambuCloudApiResult.Failure -> {
                return BambuCloudRepositoryResult.Failure(accountResult.message)
            }
        }
        val savedAt = nowMillis()
        val session = BambuCloudSession(
            tokens = tokens,
            account = account,
            savedAtMillis = savedAt,
            expiresAtMillis = savedAt + tokens.expiresInSeconds * 1000L,
            region = region
        )
        sessionStore.saveSession(session)
        return BambuCloudRepositoryResult.Success(session)
    }
}
