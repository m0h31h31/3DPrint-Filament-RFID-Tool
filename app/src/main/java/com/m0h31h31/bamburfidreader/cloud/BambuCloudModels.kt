package com.m0h31h31.bamburfidreader.cloud

data class BambuCloudTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
    val loginType: String = ""
)

data class BambuCloudAccount(
    val uid: Long,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val bio: String
)

data class BambuCloudPrinter(
    val deviceId: String,
    val deviceName: String,
    val modelName: String,
    val productName: String,
    val online: Boolean,
    val taskId: String,
    val taskName: String,
    val taskStatus: String,
    val progress: Int?,
    val thumbnailUrl: String,
    val structure: String = "",
    val nozzleDiameter: Double? = null,
    val accessCode: String = ""
)

data class BambuCloudFilament(
    val id: Long,
    val createType: String,
    val vendor: String,
    val type: String,
    val name: String,
    val filamentId: String,
    val rfid: String,
    val color: String,
    val colorType: Int = 0,
    val colors: List<String>,
    val netWeightGrams: Int,
    val totalNetWeightGrams: Int,
    val note: String = "",
    val createdAtSeconds: Long = 0L,
    val updatedAtSeconds: Long = 0L,
    val status: Int = 0,
    val isSupport: Boolean = false,
    val trayIdName: String,
    val category: String = "",
    val displayName: String? = null,
    val inPrinter: Boolean,
    val deviceId: String,
    val amsSerial: String,
    val slotId: String,
    val amsId: Int?,
    val amsType: Int? = null,
    val deviceName: String = ""
)

data class BambuCloudFilamentUpdate(
    val id: Long,
    val netWeightGrams: Int? = null,
    val displayName: String? = null,
    val note: String? = null
)

data class BambuCloudSession(
    val tokens: BambuCloudTokens,
    val account: BambuCloudAccount,
    val savedAtMillis: Long,
    val expiresAtMillis: Long,
    val region: BambuCloudRegion = BambuCloudRegion.CHINA
)

/** 打印历史任务中单条耗材用量（来自 amsDetailMapping）。 */
data class BambuCloudTaskMaterial(
    val filamentId: String,
    val filamentType: String,
    val color: String,
    val weightGrams: Double,
    val nozzleId: Int
)

/** 打印历史任务（/v1/user-service/my/tasks 的一条 hit）。 */
data class BambuCloudTask(
    val id: Long,
    val title: String,
    val coverUrl: String,
    val status: Int,
    val failedType: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val weightGrams: Double,
    val costTimeSeconds: Int,
    val deviceModel: String,
    val deviceName: String,
    val repetitions: Int,
    val plateIndex: Int,
    val materials: List<BambuCloudTaskMaterial>
)

/** 打印历史分页结果。 */
data class BambuCloudTaskPage(
    val total: Int,
    val tasks: List<BambuCloudTask>
)

/**
 * 极验 v4（GeeTest v4）解题结果。
 * captchaId 即拓竹登录接口返回的 captchaId（= geetest captcha_id），
 * 其余四项来自极验 getValidate()。携带这些字段重新请求登录即可通过人机验证。
 */
data class BambuCloudCaptchaResult(
    val captchaId: String,
    val lotNumber: String,
    val captchaOutput: String,
    val passToken: String,
    val genTime: String
)

enum class BambuCloudLoginFailureReason {
    ACCOUNT_OR_PASSWORD_INCORRECT,
    VERIFICATION_CODE_INCORRECT
}

sealed class BambuCloudApiResult<out T> {
    data class Success<T>(val value: T) : BambuCloudApiResult<T>()
    data object VerificationCodeRequired : BambuCloudApiResult<Nothing>()
    /** 登录触发了人机验证（captcha），需要在网页中完成验证。 */
    data class CaptchaRequired(
        val captchaId: String,
        val scene: String,
        val message: String
    ) : BambuCloudApiResult<Nothing>()
    data class Failure(
        val message: String,
        val statusCode: Int? = null,
        val loginFailureReason: BambuCloudLoginFailureReason? = null
    ) : BambuCloudApiResult<Nothing>()
}

data class BambuCloudHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)

data class BambuCloudHttpResponse(
    val statusCode: Int,
    val body: String
)

interface BambuCloudTransport {
    suspend fun execute(request: BambuCloudHttpRequest): BambuCloudHttpResponse
}

interface BambuCloudService {
    suspend fun loginWithPassword(
        account: String,
        password: String
    ): BambuCloudApiResult<BambuCloudTokens>

    suspend fun loginWithCode(
        account: String,
        password: String,
        code: String
    ): BambuCloudApiResult<BambuCloudTokens>

    /** 携带极验 v4 验证结果重新登录（通过人机验证）。 */
    suspend fun loginWithCaptcha(
        account: String,
        password: String,
        captcha: BambuCloudCaptchaResult
    ): BambuCloudApiResult<BambuCloudTokens>

    suspend fun fetchAccount(accessToken: String): BambuCloudApiResult<BambuCloudAccount>

    suspend fun fetchPrinters(accessToken: String): BambuCloudApiResult<List<BambuCloudPrinter>>

    suspend fun fetchFilaments(
        accessToken: String,
        offset: Int,
        limit: Int
    ): BambuCloudApiResult<List<BambuCloudFilament>>

    suspend fun updateFilament(
        accessToken: String,
        update: BambuCloudFilamentUpdate
    ): BambuCloudApiResult<Unit>

    suspend fun deleteFilaments(
        accessToken: String,
        ids: List<Long>,
        rfids: List<String>
    ): BambuCloudApiResult<Unit>

    suspend fun fetchTasks(
        accessToken: String,
        offset: Int,
        limit: Int,
        status: Int
    ): BambuCloudApiResult<BambuCloudTaskPage>
}

interface BambuCloudSessionStore {
    fun loadSession(): BambuCloudSession?
    fun saveSession(session: BambuCloudSession)
    fun clearSession()
}

sealed class BambuCloudRepositoryResult {
    data class Success(val session: BambuCloudSession) : BambuCloudRepositoryResult()
    data object VerificationCodeRequired : BambuCloudRepositoryResult()
    /** 需要人机验证：调用方应打开网页登录完成验证。 */
    data class CaptchaRequired(
        val captchaId: String,
        val scene: String,
        val message: String
    ) : BambuCloudRepositoryResult()
    data class Failure(
        val message: String,
        val loginFailureReason: BambuCloudLoginFailureReason? = null
    ) : BambuCloudRepositoryResult()
}

sealed class BambuCloudPrinterResult {
    data class Success(val printers: List<BambuCloudPrinter>) : BambuCloudPrinterResult()
    data class Failure(val message: String) : BambuCloudPrinterResult()
}

sealed class BambuCloudFilamentResult {
    data class Success(val filaments: List<BambuCloudFilament>) : BambuCloudFilamentResult()
    data class Failure(val message: String) : BambuCloudFilamentResult()
}

sealed class BambuCloudFilamentMutationResult {
    data object Success : BambuCloudFilamentMutationResult()
    data class Failure(val message: String) : BambuCloudFilamentMutationResult()
}

sealed class BambuCloudTaskResult {
    data class Success(val page: BambuCloudTaskPage) : BambuCloudTaskResult()
    data class Failure(val message: String) : BambuCloudTaskResult()
}
