package com.m0h31h31.bamburfidreader.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.cloud.BambuCloudCaptchaResult
import com.m0h31h31.bamburfidreader.cloud.BambuCloudRegion
import com.m0h31h31.bamburfidreader.logging.logDebug
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

private const val GEETEST_HOST_ASSET = "geetest_v4.html"
// 极验 captcha_id 通常与注册域名绑定，使用所选站点的基址匹配 Origin/Referer。

/**
 * 原生极验 v4（GeeTest v4）人机验证控件。
 *
 * 用拓竹返回的 captchaId 初始化极验,用户在 App 内完成滑块/点选,
 * 解题成功后通过 [onResult] 把验证结果四件套回传给调用方,由调用方携带这些
 * 字段重新请求登录接口完成认证（即"在 App 内完成认证",而非加载整页网页）。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BambuGeetestCaptchaDialog(
    captchaId: String,
    onResult: (BambuCloudCaptchaResult) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
    region: BambuCloudRegion = BambuCloudRegion.CHINA
) {
    // 保证只回传一次
    val delivered = remember { AtomicBoolean(false) }
    val resultCallback = onResult
    val errorCallback = onError
    val closeCallback = onDismiss

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(540.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cloud_captcha_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }
                Text(
                    text = stringResource(R.string.cloud_captcha_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
                AndroidView(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            val webView = this
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onResult(json: String) {
                                        webView.post {
                                            if (delivered.getAndSet(true)) return@post
                                            val result = parseCaptchaResult(json, captchaId)
                                            if (result == null) {
                                                logDebug("Geetest result parse failed: $json")
                                                delivered.set(false)
                                                errorCallback("parse_failed")
                                            } else {
                                                logDebug(
                                                    "Geetest solved: lot_number=" +
                                                        result.lotNumber +
                                                        ", gen_time=" + result.genTime
                                                )
                                                resultCallback(result)
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onError(msg: String) {
                                        webView.post {
                                            logDebug("Geetest error: $msg")
                                            errorCallback(msg)
                                        }
                                    }

                                    @JavascriptInterface
                                    fun onClose(msg: String) {
                                        webView.post { closeCallback() }
                                    }
                                },
                                "BrrCaptcha"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(
                                        "window.startCaptcha(${JSONObject.quote(captchaId)})",
                                        null
                                    )
                                }
                            }
                            val html = runCatching {
                                ctx.assets.open(GEETEST_HOST_ASSET).bufferedReader().use { it.readText() }
                            }.getOrElse {
                                logDebug("Geetest host asset load failed: ${it.message}")
                                ""
                            }
                            loadDataWithBaseURL(region.captchaBaseUrl, html, "text/html", "utf-8", null)
                        }
                    }
                )
            }
        }
    }
}

/** 解析极验 getValidate() 结果；缺少关键字段时返回 null。 */
private fun parseCaptchaResult(rawJson: String, fallbackCaptchaId: String): BambuCloudCaptchaResult? {
    return try {
        val json = JSONObject(rawJson)
        val passToken = json.optString("pass_token").trim()
        val lotNumber = json.optString("lot_number").trim()
        val captchaOutput = json.optString("captcha_output").trim()
        if (passToken.isBlank() || lotNumber.isBlank() || captchaOutput.isBlank()) return null
        BambuCloudCaptchaResult(
            captchaId = json.optString("captcha_id").trim().ifBlank { fallbackCaptchaId },
            lotNumber = lotNumber,
            captchaOutput = captchaOutput,
            passToken = passToken,
            genTime = json.optString("gen_time").trim()
        )
    } catch (_: Exception) {
        null
    }
}
