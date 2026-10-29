package com.m0h31h31.bamburfidreader.ui.screens

import com.m0h31h31.bamburfidreader.ui.components.AppAlertDialog
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRegion
import com.m0h31h31.bamburfidreader.ui.components.AppIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilament
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalog
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentCatalogInfo
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentMutationResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudFilamentUpdate
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinter
import com.m0h31h31.bamburfidreader.cloud.BambuCloudPrinterResult
import com.m0h31h31.bamburfidreader.cloud.BambuAmsTray
import com.m0h31h31.bamburfidreader.cloud.BambuAmsUnit
import com.m0h31h31.bamburfidreader.cloud.BambuMqttConnectionState
import com.m0h31h31.bamburfidreader.cloud.BambuMqttRealtimeClient
import com.m0h31h31.bamburfidreader.cloud.BambuPrinterImageMatcher
import com.m0h31h31.bamburfidreader.cloud.BambuPrinterRealtimeStatus
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepository
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRepositoryResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudSession
import com.m0h31h31.bamburfidreader.cloud.BambuCloudLoginFailureReason
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRemainingSyncer
import com.m0h31h31.bamburfidreader.cloud.SensitiveValueMasker
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.AppLinearProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CLOUD_FILAMENT_NOTE_MAX_LENGTH = 150

@Composable
fun CloudConnectScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember(context) { BambuCloudController.get(context) }
    val repository = controller.repository
    val coroutineScope = rememberCoroutineScope()

    // 持久状态（跨页面保留，由 controller 持有）
    var session by controller.sessionState
    var statusMessage by controller.statusMessageState
    var printers by controller.printersState
    var printersLoading by controller.printersLoadingState
    var printerStatusMessage by controller.printerStatusMessageState
    var realtimeStatuses by controller.realtimeStatusesState
    var mqttStatusMessage by controller.mqttStatusMessageState
    var filaments by controller.filamentsState
    var filamentCatalogInfo by controller.filamentCatalogInfoState
    var filamentsLoading by controller.filamentsLoadingState
    var filamentsSyncing by controller.filamentsSyncingState
    var filamentStatusMessage by controller.filamentStatusMessageState

    // 仅 UI 内的临时状态（弹窗/输入框），离开页面可重置
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginRegion by remember { mutableStateOf(repository.loadSession()?.region ?: BambuCloudRegion.CHINA) }
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var verificationRequired by remember { mutableStateOf(false) }
    var loginInProgress by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showGeetest by remember { mutableStateOf(false) }
    var captchaId by remember { mutableStateOf("") }
    var selectedFilament by remember { mutableStateOf<BambuCloudFilament?>(null) }
    var filamentDetailBusy by remember { mutableStateOf(false) }
    var filamentDetailMessage by remember { mutableStateOf("") }
    // 是否已带验证结果回提过一次：用于防止"解题→服务器又要验证"的死循环
    var captchaAttempted by remember { mutableStateOf(false) }

    fun syncFilamentRemainingToLocal() {
        controller.syncFilamentRemainingToLocal(context) { list ->
            BambuCloudRemainingSyncer.syncToLocalInventory(context, list).updatedCount
        }
    }

    LaunchedEffect(filaments) {
        controller.recomputeCatalog(context)
    }

    val deviceIdsKey = remember(printers) {
        printers.map { it.deviceId }.filter { it.isNotBlank() }.joinToString("|")
    }
    // 维护 MQTT 实时连接；离开页面不再 stop，连接保持
    LaunchedEffect(session?.region, session?.account?.uid, session?.tokens?.accessToken, deviceIdsKey) {
        controller.syncRealtime(context)
    }

    LaunchedEffect(Unit) {
        controller.ensureLoaded(context)
    }

    fun handleLoginResult(result: BambuCloudRepositoryResult) {
        loginInProgress = false
        when (result) {
            is BambuCloudRepositoryResult.Success -> {
                session = result.session
                showLoginDialog = false
                showGeetest = false
                captchaAttempted = false
                password = ""
                verificationCode = ""
                verificationRequired = false
                statusMessage = context.getString(R.string.cloud_status_login_success)
                coroutineScope.launch {
                    controller.refreshCloudLists(context)
                }
            }
            BambuCloudRepositoryResult.VerificationCodeRequired -> {
                // 人机验证已通过、服务器已下发短信/邮箱验证码：重新打开登录框显示验证码输入。
                captchaAttempted = false
                showGeetest = false
                verificationRequired = true
                showLoginDialog = true
                statusMessage = context.getString(
                    if (loginRegion == BambuCloudRegion.GLOBAL) R.string.cloud_status_verify_email_required
                    else R.string.cloud_status_verify_required
                )
            }
            is BambuCloudRepositoryResult.CaptchaRequired -> {
                showLoginDialog = false
                captchaId = result.captchaId
                when {
                    captchaAttempted -> {
                        // 已带验证结果回提过一次仍要求验证 → 停止避免死循环。
                        captchaAttempted = false
                        showGeetest = false
                        statusMessage = context.getString(R.string.cloud_status_captcha_loop)
                    }
                    captchaId.isNotBlank() -> {
                        // 触发人机验证：在 App 内弹出原生极验 v4 控件完成验证
                        statusMessage = context.getString(R.string.cloud_status_captcha_required)
                        showGeetest = true
                    }
                    else -> {
                        // 需要人机验证但缺 captchaId，无法弹控件
                        statusMessage = context.getString(R.string.cloud_status_captcha_required)
                    }
                }
            }
            is BambuCloudRepositoryResult.Failure -> {
                val loginFailureMessage = when (result.loginFailureReason) {
                    BambuCloudLoginFailureReason.ACCOUNT_OR_PASSWORD_INCORRECT -> {
                        context.getString(R.string.cloud_status_account_password_incorrect)
                    }
                    BambuCloudLoginFailureReason.VERIFICATION_CODE_INCORRECT -> {
                        context.getString(R.string.cloud_status_verification_code_incorrect)
                    }
                    null -> result.message
                }
                statusMessage = context.getString(R.string.cloud_status_login_failed, loginFailureMessage)
            }
        }
    }

    fun startPasswordLogin() {
        if (account.isBlank() || password.isBlank()) {
            statusMessage = context.getString(R.string.cloud_status_missing_credentials)
            return
        }
        captchaAttempted = false
        loginInProgress = true
        coroutineScope.launch {
            handleLoginResult(repository.loginWithPassword(account, password, loginRegion))
        }
    }

    fun startCodeLogin() {
        if (account.isBlank() || verificationCode.isBlank()) {
            statusMessage = context.getString(R.string.cloud_status_missing_code)
            return
        }
        captchaAttempted = false
        loginInProgress = true
        coroutineScope.launch {
            handleLoginResult(repository.loginWithCode(account, password, verificationCode, loginRegion))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .neuBackground()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeuPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val activeSession = session
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cloud_connect_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeSession != null) {
                        IconButton(
                            onClick = { controller.forceRefreshAll(context) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = com.m0h31h31.bamburfidreader.ui.components.AppIcons.Sync,
                                contentDescription = stringResource(R.string.cloud_refresh_all),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    CloudStatusPill(
                        text = statusMessage,
                        connected = activeSession != null
                    )
                    if (activeSession != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = stringResource(R.string.cloud_logout),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (activeSession == null) {
                    NeuButton(
                        text = stringResource(R.string.cloud_login),
                        onClick = {
                            verificationRequired = false
                            showLoginDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CloudAccountHeader(activeSession)
                }
            }
        }
        if (session != null) {
            CloudPrinterCarousel(
                printers = printers,
                loading = printersLoading,
                statusMessage = printerStatusMessage,
                mqttStatusMessage = mqttStatusMessage,
                realtimeStatuses = realtimeStatuses
            )
            CloudFilamentLibraryCard(
                filaments = filaments,
                catalogInfo = filamentCatalogInfo,
                loading = filamentsLoading,
                syncing = filamentsSyncing,
                statusMessage = filamentStatusMessage,
                onSyncRemaining = ::syncFilamentRemainingToLocal,
                onFilamentClick = { filament ->
                    selectedFilament = filament
                    filamentDetailMessage = ""
                }
            )
        }
    }

    if (showLoginDialog) {
        AppAlertDialog(
            onDismissRequest = {
                if (!loginInProgress) showLoginDialog = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.cloud_login_title))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(if (loginRegion == BambuCloudRegion.CHINA)
                                R.string.cloud_login_region_cn_badge else R.string.cloud_region_global),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.cloud_region_label), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BambuCloudRegion.entries.forEach { region ->
                            FilterChip(
                                selected = loginRegion == region,
                                enabled = !loginInProgress && !showGeetest,
                                onClick = {
                                    if (loginRegion != region) {
                                        loginRegion = region
                                        verificationRequired = false
                                        verificationCode = ""
                                        captchaId = ""
                                        captchaAttempted = false
                                        statusMessage = ""
                                    }
                                },
                                label = { Text(stringResource(if (region == BambuCloudRegion.CHINA)
                                    R.string.cloud_region_china else R.string.cloud_region_global)) }
                            )
                        }
                    }
                    if (statusMessage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    OutlinedTextField(
                        value = account,
                        onValueChange = { account = it },
                        enabled = !loginInProgress,
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_account)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = !loginInProgress,
                        singleLine = true,
                        label = { Text(stringResource(R.string.cloud_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (verificationRequired) {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            enabled = !loginInProgress,
                            singleLine = true,
                            label = { Text(stringResource(R.string.cloud_verification_code)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (loginInProgress) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppCircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.cloud_status_logging_in),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.cloud_login_security_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.cloud_login_password_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (verificationRequired) startCodeLogin() else startPasswordLogin()
                    },
                    enabled = !loginInProgress
                ) {
                    Text(
                        if (verificationRequired) {
                            stringResource(R.string.cloud_login_code)
                        } else {
                            stringResource(R.string.cloud_login_password)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLoginDialog = false },
                    enabled = !loginInProgress
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showLogoutConfirm) {
        AppAlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.cloud_logout_confirm_title)) },
            text = { Text(stringResource(R.string.cloud_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        controller.logout(context)
                        showLogoutConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showGeetest && captchaId.isNotBlank()) {
        BambuGeetestCaptchaDialog(
            captchaId = captchaId,
            region = loginRegion,
            onResult = { captcha ->
                // 极验解题成功 → 携带验证结果在 App 内重新登录（仅回提一次，防循环）
                showGeetest = false
                captchaAttempted = true
                loginInProgress = true
                statusMessage = context.getString(R.string.cloud_status_captcha_verifying)
                coroutineScope.launch {
                    handleLoginResult(repository.loginWithPasswordAndCaptcha(account, password, captcha, loginRegion))
                }
            },
            onError = { msg ->
                showGeetest = false
                statusMessage = context.getString(R.string.cloud_status_captcha_failed, msg)
            },
            onDismiss = { showGeetest = false }
        )
    }

    selectedFilament?.let { filament ->
        CloudFilamentDetailDialog(
            filament = filament,
            catalogInfo = filamentCatalogInfo[filament.id],
            busy = filamentDetailBusy,
            message = filamentDetailMessage,
            onDismiss = {
                if (!filamentDetailBusy) {
                    selectedFilament = null
                    filamentDetailMessage = ""
                }
            },
            onSave = { update ->
                if (filamentDetailBusy) return@CloudFilamentDetailDialog
                filamentDetailBusy = true
                filamentDetailMessage = ""
                coroutineScope.launch {
                    when (val result = repository.updateFilament(update)) {
                        BambuCloudFilamentMutationResult.Success -> {
                            filamentStatusMessage = context.getString(R.string.cloud_filament_update_success)
                            controller.refreshFilaments(context)
                            selectedFilament = null
                        }
                        is BambuCloudFilamentMutationResult.Failure -> {
                            filamentDetailMessage = context.getString(
                                R.string.cloud_filament_update_failed,
                                result.message
                            )
                        }
                    }
                    filamentDetailBusy = false
                }
            },
            onDelete = {
                if (filamentDetailBusy) return@CloudFilamentDetailDialog
                filamentDetailBusy = true
                filamentDetailMessage = ""
                coroutineScope.launch {
                    when (val result = repository.deleteFilament(filament)) {
                        BambuCloudFilamentMutationResult.Success -> {
                            filamentStatusMessage = context.getString(R.string.cloud_filament_delete_success)
                            controller.refreshFilaments(context)
                            selectedFilament = null
                        }
                        is BambuCloudFilamentMutationResult.Failure -> {
                            filamentDetailMessage = context.getString(
                                R.string.cloud_filament_delete_failed,
                                result.message
                            )
                        }
                    }
                    filamentDetailBusy = false
                }
            }
        )
    }
}

@Composable
private fun CloudPrinterCarousel(
    printers: List<BambuCloudPrinter>,
    loading: Boolean,
    statusMessage: String,
    mqttStatusMessage: String,
    realtimeStatuses: Map<String, BambuPrinterRealtimeStatus>
) {
    val context = LocalContext.current
    var printerAssetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        printerAssetNames = withContext(Dispatchers.IO) {
            context.assets.list(BambuPrinterImageMatcher.ASSET_DIR)?.toList().orEmpty()
        }
    }
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_printers_title_count, printers.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (statusMessage.isNotBlank() && printers.isNotEmpty()) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (mqttStatusMessage.isNotBlank()) {
                        Text(
                            text = mqttStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (loading) {
                    AppCircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            if (printers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (statusMessage.isBlank()) {
                            stringResource(R.string.cloud_printers_empty)
                        } else {
                            statusMessage
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { printers.size })
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // 卡片尽量占满宽度，仅保留少量边距露出相邻卡片，提示可左右滑动
                    val cardWidth = if (printers.size > 1) {
                        maxWidth - 36.dp
                    } else {
                        maxWidth - 8.dp
                    }
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = (maxWidth - cardWidth) / 2),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        CloudPrinterCard(
                            printer = printers[page],
                            realtimeStatus = realtimeStatuses[printers[page].deviceId],
                            assetNames = printerAssetNames,
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudFilamentLibraryCard(
    filaments: List<BambuCloudFilament>,
    catalogInfo: Map<Long, BambuCloudFilamentCatalogInfo>,
    loading: Boolean,
    syncing: Boolean,
    statusMessage: String,
    onSyncRemaining: () -> Unit,
    onFilamentClick: (BambuCloudFilament) -> Unit
) {
    NeuPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cloud_filaments_title_count, filaments.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.cloud_filaments_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onSyncRemaining,
                    enabled = filaments.isNotEmpty() && !loading && !syncing,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.cloud_filaments_sync_remaining),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (loading || syncing) {
                    AppCircularProgressIndicator(modifier = Modifier.size(22.dp))
                }
            }
            if (statusMessage.isNotBlank() && filaments.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (filaments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (statusMessage.isBlank()) {
                            stringResource(R.string.cloud_filaments_empty)
                        } else {
                            statusMessage
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    filaments.forEachIndexed { index, filament ->
                        CloudFilamentRow(
                            filament = filament,
                            catalogInfo = catalogInfo[filament.id],
                            onClick = { onFilamentClick(filament) }
                        )
                        if (index < filaments.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudFilamentRow(
    filament: BambuCloudFilament,
    catalogInfo: BambuCloudFilamentCatalogInfo?,
    onClick: () -> Unit
) {
    val unknown = stringResource(R.string.label_unknown)
    val title = buildList {
        if (filament.vendor.isNotBlank()) add(filament.vendor)
        add(filament.name.ifBlank { filament.type.ifBlank { unknown } })
    }.joinToString(" ")
    val meta = buildList {
        val colorName = catalogInfo?.colorName.orEmpty()
        val filamentNumber = catalogInfo?.filamentNumber.orEmpty()
        if (colorName.isNotBlank()) add(colorName)
        if (filamentNumber.isNotBlank()) add(filamentNumber)
    }.joinToString("  |  ")
    val total = filament.totalNetWeightGrams.coerceAtLeast(0)
    val net = filament.netWeightGrams.coerceAtLeast(0)
    val progress = if (total > 0) {
        (net.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CloudFilamentColorPreview(filament)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (filament.inPrinter) {
                val location = buildList {
                    if (filament.deviceName.isNotBlank()) add(filament.deviceName)
                    filament.amsId?.let { add(stringResource(R.string.cloud_filament_ams, it)) }
                    if (filament.slotId.isNotBlank()) {
                        add(stringResource(R.string.cloud_filament_slot, filament.slotId))
                    }
                }.joinToString("  |  ")
                if (location.isNotBlank()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Column(
            modifier = Modifier.width(92.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = if (total > 0) {
                    stringResource(R.string.cloud_filament_weight, net, total)
                } else {
                    stringResource(R.string.cloud_filament_weight_unknown)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AppLinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun CloudFilamentDetailDialog(
    filament: BambuCloudFilament,
    catalogInfo: BambuCloudFilamentCatalogInfo?,
    busy: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onSave: (BambuCloudFilamentUpdate) -> Unit,
    onDelete: () -> Unit
) {
    val unknown = stringResource(R.string.label_unknown)
    val invalidWeightMessage = stringResource(R.string.cloud_filament_invalid_net_weight)
    var displayName by remember(filament.id) { mutableStateOf(filament.displayName.orEmpty()) }
    var netWeightText by remember(filament.id) { mutableStateOf(filament.netWeightGrams.coerceAtLeast(0).toString()) }
    var note by remember(filament.id) { mutableStateOf(limitCloudFilamentNote(filament.note)) }
    var localMessage by remember(filament.id) { mutableStateOf("") }
    val effectiveMessage = localMessage.ifBlank { message }
    val title = displayName.ifBlank {
        filament.name.ifBlank {
            filament.type.ifBlank { unknown }
        }
    }
    val updateBuildResult = buildCloudFilamentUpdate(
        filament = filament,
        displayName = displayName,
        netWeightText = netWeightText,
        note = note
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_filament_detail_title, title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (effectiveMessage.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = effectiveMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        localMessage = ""
                    },
                    enabled = !busy,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text(stringResource(R.string.cloud_filament_field_display_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = netWeightText,
                    onValueChange = {
                        netWeightText = it.filter(Char::isDigit)
                        localMessage = ""
                    },
                    enabled = !busy,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text(stringResource(R.string.cloud_filament_field_net_weight)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.unit_grams)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = limitCloudFilamentNote(it)
                        localMessage = ""
                    },
                    enabled = !busy,
                    minLines = 1,
                    maxLines = 2,
                    textStyle = MaterialTheme.typography.bodySmall,
                    label = { Text(stringResource(R.string.cloud_filament_field_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (busy) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppCircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.cloud_filament_saving),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_id), filament.id.toString())
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_create_type), formatCreateType(filament.createType, unknown))
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_vendor), filament.vendor.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_filament_type), filament.type.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_filament_name), filament.name.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_filament_id), filament.filamentId.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_rfid), filament.rfid.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_color), filament.color.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_color_type), filament.colorType.toString())
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_colors),
                        filament.colors.joinToString(", ").ifBlank { unknown }
                    )
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_total_net_weight),
                        stringResource(R.string.cloud_filament_grams_value, filament.totalNetWeightGrams.coerceAtLeast(0))
                    )
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_created_at), formatCloudTimestamp(filament.createdAtSeconds, unknown))
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_updated_at), formatCloudTimestamp(filament.updatedAtSeconds, unknown))
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_status), formatCloudStatus(filament.status))
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_support),
                        if (filament.isSupport) stringResource(R.string.cloud_yes) else stringResource(R.string.cloud_no)
                    )
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_tray_id_name), filament.trayIdName.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_category), filament.category.ifBlank { unknown })
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_catalog_color),
                        catalogInfo?.colorName.orEmpty().ifBlank { unknown }
                    )
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_catalog_number),
                        catalogInfo?.filamentNumber.orEmpty().ifBlank { unknown }
                    )
                    CloudFilamentDetailLine(
                        stringResource(R.string.cloud_filament_field_in_printer),
                        if (filament.inPrinter) stringResource(R.string.cloud_yes) else stringResource(R.string.cloud_no)
                    )
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_device_id), filament.deviceId.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_ams_sn), filament.amsSerial.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_slot_id), filament.slotId.ifBlank { unknown })
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_ams_id), filament.amsId?.toString() ?: unknown)
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_ams_type), filament.amsType?.toString() ?: unknown)
                    CloudFilamentDetailLine(stringResource(R.string.cloud_filament_field_device_name), filament.deviceName.ifBlank { unknown })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && updateBuildResult !is CloudFilamentUpdateBuildResult.NoChanges,
                onClick = {
                    when (val result = buildCloudFilamentUpdate(filament, displayName, netWeightText, note)) {
                        CloudFilamentUpdateBuildResult.InvalidNetWeight -> {
                            localMessage = invalidWeightMessage
                        }
                        CloudFilamentUpdateBuildResult.NoChanges -> Unit
                        is CloudFilamentUpdateBuildResult.Update -> onSave(result.update)
                    }
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    enabled = !busy,
                    onClick = onDelete
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    enabled = !busy,
                    onClick = onDismiss
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
private fun CloudFilamentDetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

internal fun limitCloudFilamentNote(value: String): String {
    return value.take(CLOUD_FILAMENT_NOTE_MAX_LENGTH)
}

internal sealed class CloudFilamentUpdateBuildResult {
    data class Update(val update: BambuCloudFilamentUpdate) : CloudFilamentUpdateBuildResult()
    data object NoChanges : CloudFilamentUpdateBuildResult()
    data object InvalidNetWeight : CloudFilamentUpdateBuildResult()
}

internal fun buildCloudFilamentUpdate(
    filament: BambuCloudFilament,
    displayName: String,
    netWeightText: String,
    note: String
): CloudFilamentUpdateBuildResult {
    val totalNetWeight = filament.totalNetWeightGrams.coerceAtLeast(0)
    val trimmedNetWeight = netWeightText.trim()
    val netWeight = if (trimmedNetWeight.isBlank()) {
        null
    } else {
        trimmedNetWeight.toIntOrNull() ?: return CloudFilamentUpdateBuildResult.InvalidNetWeight
    }
    if (netWeight != null && (netWeight < 0 || netWeight > totalNetWeight)) {
        return CloudFilamentUpdateBuildResult.InvalidNetWeight
    }

    val normalizedDisplayName = displayName.trim()
    val originalDisplayName = filament.displayName.orEmpty().trim()
    val normalizedNote = limitCloudFilamentNote(note.trim())
    val originalNote = limitCloudFilamentNote(filament.note.trim())
    val normalizedNetWeight = netWeight?.takeIf { it != filament.netWeightGrams }
    val changedDisplayName = normalizedDisplayName.takeIf { it != originalDisplayName }
    val changedNote = normalizedNote.takeIf { it != originalNote }

    if (normalizedNetWeight == null && changedDisplayName == null && changedNote == null) {
        return CloudFilamentUpdateBuildResult.NoChanges
    }

    return CloudFilamentUpdateBuildResult.Update(
        BambuCloudFilamentUpdate(
            id = filament.id,
            netWeightGrams = normalizedNetWeight,
            displayName = changedDisplayName,
            note = changedNote
        )
    )
}

@Composable
private fun CloudFilamentColorPreview(filament: BambuCloudFilament) {
    val colors = filament.colors.ifEmpty {
        listOf(filament.color).filter { it.isNotBlank() }
    }.take(4)
    Row(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(6.dp)
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (colors.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
            colors.forEach { colorHex ->
                Box(
                    modifier = Modifier
                        .width((24 / colors.size.coerceAtLeast(1)).dp)
                        .height(24.dp)
                        .background(parseRgbaColor(colorHex) ?: MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun CloudPrinterCard(
    printer: BambuCloudPrinter,
    realtimeStatus: BambuPrinterRealtimeStatus?,
    assetNames: List<String>,
    modifier: Modifier = Modifier
) {
    val unknown = stringResource(R.string.label_unknown)
    val productName = printer.productName.ifBlank { unknown }
    val progress = realtimeStatus?.progress ?: printer.progress?.coerceIn(0, 100)
    val taskName = realtimeStatus?.taskName?.ifBlank { realtimeStatus.taskId }?.ifBlank { null }
        ?: printer.taskName.ifBlank { printer.taskId.ifBlank { unknown } }
    val taskStatus = realtimeStatus?.gcodeState?.ifBlank { null }
        ?: printer.taskStatus.ifBlank { unknown }
    val hasTask = taskName != unknown ||
        printer.taskId.isNotBlank() ||
        taskStatus != unknown ||
        progress != null
    var showAmsDialog by remember(printer.deviceId) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            CloudPrinterProductImage(
                printer = printer,
                assetNames = assetNames,
                productName = productName,
                modifier = Modifier.size(82.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CloudOnlinePill(online = printer.online)
                }
                if (printer.deviceName.isNotBlank()) {
                    Text(
                        text = printer.deviceName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val specLine = listOf(
                    printer.modelName,
                    printer.structure,
                    printer.nozzleDiameter?.let { stringResource(R.string.cloud_printer_nozzle_diameter, formatNozzle(it)) }.orEmpty()
                ).filter { it.isNotBlank() }.joinToString("  |  ")
                if (specLine.isNotBlank()) {
                    Text(
                        text = specLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        CloudPrinterTaskBlock(
            hasTask = hasTask,
            taskName = taskName,
            taskStatus = taskStatus,
            progress = progress,
            realtimeStatus = realtimeStatus
        )
        CloudAmsBlock(
            status = realtimeStatus,
            onClick = { showAmsDialog = true }
        )
    }
    if (showAmsDialog && realtimeStatus != null) {
        CloudAmsDialog(
            status = realtimeStatus,
            onDismiss = { showAmsDialog = false }
        )
    }
}

@Composable
private fun CloudPrinterProductImage(
    printer: BambuCloudPrinter,
    assetNames: List<String>,
    productName: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val assetName = remember(printer, assetNames) {
        BambuPrinterImageMatcher.matchAssetName(printer, assetNames)
    }
    val bitmap = rememberPrinterAssetBitmap(assetName)
    val shape = RoundedCornerShape(8.dp)
    val imageModifier = Modifier
        .then(modifier)
        .aspectRatio(1f)
        .clip(shape)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = shape
        )
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.cloud_printer_product_image),
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
    } else {
        Box(
            modifier = imageModifier.padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CloudOnlinePill(online: Boolean) {
    val containerColor = if (online) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    }
    val contentColor = if (online) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = stringResource(
                if (online) {
                    R.string.cloud_printer_online
                } else {
                    R.string.cloud_printer_offline
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun CloudPrinterDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudPrinterTaskBlock(
    hasTask: Boolean,
    taskName: String,
    taskStatus: String,
    progress: Int?,
    realtimeStatus: BambuPrinterRealtimeStatus?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (!hasTask) {
            Text(
                text = stringResource(R.string.cloud_printer_task_idle),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Text(
            text = taskName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.basicMarquee()
        )
        Text(
            text = taskStatus,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (progress != null) {
            AppLinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cloud_printer_progress, progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                val remaining = realtimeStatus?.remainingMinutes
                if (remaining != null && remaining > 0) {
                    Text(
                        text = formatRemainingTime(remaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (realtimeStatus != null) {
            CloudRealtimeMetricLine(realtimeStatus)
        }
    }
}

@Composable
private fun CloudRealtimeMetricLine(status: BambuPrinterRealtimeStatus) {
    val metrics = buildList {
        if (status.nozzleTemperature != null || status.nozzleTargetTemperature != null) {
            add(
                stringResource(
                    R.string.cloud_printer_nozzle_temp,
                    formatTemperature(status.nozzleTemperature),
                    formatTemperature(status.nozzleTargetTemperature)
                )
            )
        }
        if (status.bedTemperature != null || status.bedTargetTemperature != null) {
            add(
                stringResource(
                    R.string.cloud_printer_bed_temp,
                    formatTemperature(status.bedTemperature),
                    formatTemperature(status.bedTargetTemperature)
                )
            )
        }
        status.chamberTemperature?.let {
            add(stringResource(R.string.cloud_printer_chamber_temp, formatTemperature(it)))
        }
        if (status.wifiSignal.isNotBlank()) {
            add(stringResource(R.string.cloud_printer_wifi_signal, status.wifiSignal))
        }
    }
    if (metrics.isEmpty()) return
    Text(
        text = metrics.joinToString("  |  "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun CloudAmsBlock(
    status: BambuPrinterRealtimeStatus?,
    onClick: () -> Unit
) {
    val units = status?.amsUnits.orEmpty()
    if (units.isEmpty()) return
    val trays = units.flatMap { unit ->
        unit.trays.map { tray -> unit to tray }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cloud_ams_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.cloud_ams_expand),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trays.forEach { (_, tray) ->
                CloudAmsColorSwatch(
                    colorHex = tray.color,
                    active = status?.currentTray == tray.id,
                    empty = !tray.hasFilament
                )
            }
        }
    }
}

@Composable
private fun CloudAmsDialog(
    status: BambuPrinterRealtimeStatus,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cloud_ams_details_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                status.amsUnits.forEach { unit ->
                    CloudAmsUnitDetails(unit)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    )
}

@Composable
private fun CloudAmsUnitDetails(unit: BambuAmsUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val meta = buildList {
            if (unit.temperature.isNotBlank()) {
                add(stringResource(R.string.cloud_ams_temp, unit.temperature))
            }
            if (unit.humidity.isNotBlank()) {
                add(stringResource(R.string.cloud_ams_humidity, unit.humidity))
            }
        }.joinToString("  |  ")
        Text(
            text = if (meta.isBlank()) {
                stringResource(R.string.cloud_ams_unit, unit.id)
            } else {
                "${stringResource(R.string.cloud_ams_unit, unit.id)}  $meta"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        unit.trays.forEach { tray ->
            CloudAmsTrayRow(tray)
        }
    }
}

@Composable
private fun CloudAmsTrayRow(tray: BambuAmsTray) {
    val hasFilament = tray.hasFilament
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CloudAmsColorSwatch(colorHex = tray.color, active = false, empty = !hasFilament)
        Column(modifier = Modifier.weight(1f)) {
            val title = buildList {
                add(stringResource(R.string.cloud_ams_slot, tray.id))
                if (hasFilament) {
                    if (tray.filamentType.isNotBlank()) add(tray.filamentType)
                    if (tray.name.isNotBlank()) add(tray.name)
                } else {
                    add(stringResource(R.string.cloud_ams_slot_empty))
                }
            }.joinToString("  ")
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasFilament) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = if (hasFilament) buildList {
                tray.remain?.let { add(stringResource(R.string.cloud_ams_remain, it)) }
                if (tray.nozzleTempMin.isNotBlank() || tray.nozzleTempMax.isNotBlank()) {
                    add(
                        stringResource(
                            R.string.cloud_ams_nozzle_range,
                            tray.nozzleTempMin.ifBlank { "-" },
                            tray.nozzleTempMax.ifBlank { "-" }
                        )
                    )
                }
                if (tray.trayInfoIndex.isNotBlank()) add(tray.trayInfoIndex)
                if (tray.tagUid.isNotBlank() && tray.tagUid != "0000000000000000") {
                    add(tray.tagUid)
                }
            }.joinToString("  |  ") else stringResource(R.string.cloud_ams_slot_empty_hint)
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CloudAmsColorSwatch(
    colorHex: String,
    active: Boolean,
    empty: Boolean = false
) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 16.dp)
            .clip(shape)
            .background(
                if (empty) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                } else {
                    parseRgbaColor(colorHex) ?: MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                },
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (empty) {
            Text(
                text = "?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CloudStatusPill(
    text: String,
    connected: Boolean
) {
    val containerColor = if (connected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }
    val contentColor = if (connected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CloudAccountHeader(session: BambuCloudSession) {
    val account = session.account
    val displayName = account.name.ifBlank { stringResource(R.string.label_unknown) }
    val handleText = account.handle.ifBlank { stringResource(R.string.label_unknown) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CloudAvatar(
            name = displayName,
            avatarUrl = account.avatarUrl,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@$handleText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CloudAvatar(
    name: String,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberRemoteBitmap(avatarUrl)
    val shape = CircleShape
    val avatarModifier = modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), shape)
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    } else {
        Box(
            modifier = avatarModifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString().orEmpty().ifBlank { "B" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun rememberRemoteBitmap(imageUrl: String): ImageBitmap? {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageUrl) {
        bitmap = if (imageUrl.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(imageUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    try {
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    } finally {
                        connection.disconnect()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

@Composable
private fun rememberPrinterAssetBitmap(assetName: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(assetName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(assetName) {
        bitmap = if (assetName.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets
                        .open("${BambuPrinterImageMatcher.ASSET_DIR}/$assetName")
                        .use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                }.getOrNull()
            }
        }
    }
    return bitmap
}

internal fun mergeRealtimeStatus(
    previous: BambuPrinterRealtimeStatus?,
    next: BambuPrinterRealtimeStatus
): BambuPrinterRealtimeStatus {
    if (previous == null) return next
    return next.copy(
        gcodeState = next.gcodeState.ifBlank { previous.gcodeState },
        taskName = next.taskName.ifBlank { previous.taskName },
        taskId = next.taskId.ifBlank { previous.taskId },
        progress = next.progress ?: previous.progress,
        remainingMinutes = next.remainingMinutes ?: previous.remainingMinutes,
        nozzleTemperature = next.nozzleTemperature ?: previous.nozzleTemperature,
        nozzleTargetTemperature = next.nozzleTargetTemperature ?: previous.nozzleTargetTemperature,
        bedTemperature = next.bedTemperature ?: previous.bedTemperature,
        bedTargetTemperature = next.bedTargetTemperature ?: previous.bedTargetTemperature,
        chamberTemperature = next.chamberTemperature ?: previous.chamberTemperature,
        wifiSignal = next.wifiSignal.ifBlank { previous.wifiSignal },
        currentTray = next.currentTray.ifBlank { previous.currentTray },
        amsUnits = next.amsUnits.ifEmpty { previous.amsUnits }
    )
}

@Composable
private fun formatRemainingTime(minutes: Int): String {
    return if (minutes >= 60) {
        stringResource(R.string.cloud_printer_remaining_hm, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.cloud_printer_remaining_time, minutes)
    }
}

private fun formatNozzle(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
}

private fun formatCreateType(value: String, unknown: String): String {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "ams" -> "AMS"
        "" -> unknown
        else -> value
    }
}

@Composable
private fun formatCloudStatus(value: Int): String {
    return when (value) {
        0 -> stringResource(R.string.cloud_filament_status_normal)
        else -> value.toString()
    }
}

private fun formatCloudTimestamp(seconds: Long, unknown: String): String {
    if (seconds <= 0L) return unknown
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(seconds * 1000L))
    }.getOrDefault(unknown)
}

private fun formatTemperature(value: Double?): String {
    return value?.let {
        if (it % 1.0 == 0.0) {
            it.toInt().toString()
        } else {
            String.format("%.1f", it)
        }
    } ?: "-"
}

private fun parseRgbaColor(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length < 6) return null
    return runCatching {
        val red = clean.substring(0, 2).toInt(16)
        val green = clean.substring(2, 4).toInt(16)
        val blue = clean.substring(4, 6).toInt(16)
        val alpha = clean.takeIf { it.length >= 8 }?.substring(6, 8)?.toInt(16) ?: 255
        Color(red = red, green = green, blue = blue, alpha = alpha)
    }.getOrNull()
}
