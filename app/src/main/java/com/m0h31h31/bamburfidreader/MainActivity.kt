package com.m0h31h31.bamburfidreader

import com.m0h31h31.bamburfidreader.ui.components.AppAlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.m0h31h31.bamburfidreader.ui.navigation.AppNavigation
import com.m0h31h31.bamburfidreader.ui.screens.NdefWriteRequest
import com.m0h31h31.bamburfidreader.ui.screens.NdefWriteType
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.ColorPalette
import com.m0h31h31.bamburfidreader.ui.theme.DEFAULT_APP_UI_STYLE
import com.m0h31h31.bamburfidreader.ui.theme.ThemeMode
import com.m0h31h31.bamburfidreader.ui.theme.BambuRfidReaderTheme
import com.m0h31h31.bamburfidreader.util.normalizeColorValue
import androidx.core.content.FileProvider
import com.m0h31h31.bamburfidreader.data.FILAMENT_DB_NAME
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import com.m0h31h31.bamburfidreader.data.SHARE_TAGS_TABLE
import com.m0h31h31.bamburfidreader.data.SNAPMAKER_SHARE_TAGS_TABLE
import com.m0h31h31.bamburfidreader.data.TRAY_UID_TABLE
import com.m0h31h31.bamburfidreader.data.syncCrealityMaterialDatabase
import com.m0h31h31.bamburfidreader.data.syncFilamentDatabase
import com.m0h31h31.bamburfidreader.logging.LogCollector
import com.m0h31h31.bamburfidreader.logging.logDebug
import com.m0h31h31.bamburfidreader.model.CModifyRecoveryInfo
import com.m0h31h31.bamburfidreader.model.CrealityMaterial
import com.m0h31h31.bamburfidreader.model.CrealityTagData
import com.m0h31h31.bamburfidreader.model.CrealityWritePending
import com.m0h31h31.bamburfidreader.model.DEFAULT_REMAINING_PERCENT
import com.m0h31h31.bamburfidreader.model.DisplayData
import com.m0h31h31.bamburfidreader.model.FilamentColorEntry
import com.m0h31h31.bamburfidreader.model.InventoryItem
import com.m0h31h31.bamburfidreader.model.NfcUiState
import com.m0h31h31.bamburfidreader.model.ParsedBlockData
import com.m0h31h31.bamburfidreader.model.ParsedField
import com.m0h31h31.bamburfidreader.model.ReaderBrand
import com.m0h31h31.bamburfidreader.model.ShareTagDbMeta
import com.m0h31h31.bamburfidreader.model.ShareTagItem
import com.m0h31h31.bamburfidreader.model.SnapmakerShareTagItem
import com.m0h31h31.bamburfidreader.model.SnapmakerTagData
import com.m0h31h31.bamburfidreader.nfc.BambuMifareOperator
import com.m0h31h31.bamburfidreader.nfc.BambuNfcOperation
import com.m0h31h31.bamburfidreader.nfc.BambuNfcResult
import com.m0h31h31.bamburfidreader.nfc.FormatTagBrand
import com.m0h31h31.bamburfidreader.nfc.FormatTagBrandDetector
import com.m0h31h31.bamburfidreader.nfc.MifareClassicSession
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityConfig
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityMode
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityPreferences
import com.m0h31h31.bamburfidreader.nfc.NfcCompatibilityTestResult
import com.m0h31h31.bamburfidreader.nfc.shouldHandlePassiveNfcRead
import com.m0h31h31.bamburfidreader.utils.AnalyticsReporter
import com.m0h31h31.bamburfidreader.utils.UpdateInfo
import com.m0h31h31.bamburfidreader.utils.ConfigManager
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.exception.ZipException as Zip4jException
import java.io.IOException
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val SHARE_BUNDLE_ZIP_NAME = "rfid_data.zip"
private const val SHARE_EXTRACT_MARKER_FILE = ".bundle_extracted"
private const val SHARE_IMPORT_ZIP_MIME = "application/zip"
private const val WRITE_KEY_LENGTH_BYTES = 6
private const val WRITE_SECTOR_COUNT = 16
private const val RW_AUTH_RETRY_COUNT = 2
private const val RW_BLOCK_RETRY_COUNT = 1
private const val WRITE_RESUME_MAX_ATTEMPTS = 3
private const val RW_RECONNECT_DELAY_MS = 35L
private const val UI_PREFS_NAME = "ui_prefs"
private const val KEY_VOICE_ENABLED = "voice_enabled"
private const val KEY_UI_STYLE = "ui_style"
private const val KEY_BOOST_REMIND_LAST_MS = "boost_remind_last_ms"
private const val BOOST_REMIND_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000   // 一周
private const val BOOST_DESIGN_URI = "bambulab://bbl/design/model/detail?design_id=2020787&appSharePlatform=copy"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_INVENTORY_ENABLED = "inventory_enabled"
private const val KEY_HIDE_COPIED_TAGS = "hide_copied_tags"
private const val KEY_DUAL_TAG_MODE = "dual_tag_mode"
private const val KEY_TAG_VIEW_MODE = "tag_view_mode"
private const val KEY_COLOR_PALETTE = "color_palette"
private const val KEY_USER_AGREEMENT_VERSION = "user_agreement_version"
private const val CURRENT_USER_AGREEMENT_VERSION = 1
private const val KEY_BAMBU_TAG_ENABLED = "bambu_tag_enabled"
private const val KEY_CREALITY_ENABLED = "creality_enabled"
private const val KEY_SNAPMAKER_TAG_ENABLED = "snapmaker_tag_enabled"
private const val KEY_CLOUD_CONNECT_ENABLED = "cloud_connect_enabled"
private const val KEY_COST_ENABLED = "cost_enabled"
private const val KEY_UI_SCALE = "ui_scale"
private const val KEY_AUTO_SHARE_TAG = "auto_share_tag"
private const val KEY_AUTO_DETECT_BRAND = "auto_detect_brand"
private const val KEY_GLOBAL_NFC_LISTENER = "global_nfc_listener"
private const val KEY_NOTICE_GUIDE_SHOWN = "notice_guide_shown"
private const val KEY_LAST_WRITTEN_SOURCE_UID = "last_written_source_uid"
private const val KEY_LAST_DICT_SYNC_VERSION_CODE = "last_dict_sync_version_code"
private const val KEY_UI_STYLE_FORCED_VERSION_CODE = "ui_style_forced_version_code"
private const val FORCE_MINIMAL_UI_STYLE_VERSION_CODE = 318

private enum class PendingNfcCompatibilityTest {
    READ_ONLY,
    WRITE_VERIFY
}

// Creality AES keys
private val CREALITY_KEY_DERIVE = byteArrayOf(
    113, 51, 98, 117, 94, 116, 49, 110,
    113, 102, 90, 40, 112, 102, 36, 49
)
private val CREALITY_KEY_DATA = byteArrayOf(
    72, 64, 67, 70, 107, 82, 110, 122,
    64, 75, 65, 116, 66, 74, 112, 50
)
private val CREALITY_LENGTH_TO_WEIGHT = mapOf(
    "0330" to "1 KG", "0247" to "750 G", "0198" to "600 G",
    "0165" to "500 G", "0082" to "250 G"
)
private val CREALITY_WEIGHT_TO_LENGTH: Map<String, String> =
    CREALITY_LENGTH_TO_WEIGHT.entries.associate { (k, v) -> v to k }

// ── 快造 (Snapmaker) RFID 密钥派生盐值 ────────────────────────────────────────
private val SNAPMAKER_SALT_A = "Snapmaker_qwertyuiop[,.;]".toByteArray(Charsets.US_ASCII)
private val SNAPMAKER_SALT_B = "Snapmaker_qwertyuiop[,.;]_1q2w3e".toByteArray(Charsets.US_ASCII)

private val SNAPMAKER_MAIN_TYPE_MAP = mapOf(
    0 to "Reserved", 1 to "PLA", 2 to "PETG", 3 to "ABS", 4 to "TPU", 5 to "PVA"
)
private val SNAPMAKER_SUB_TYPE_MAP = mapOf(
    0 to "Reserved", 1 to "Basic", 2 to "Matte", 3 to "SnapSpeed",
    4 to "Silk", 5 to "Support", 6 to "HF", 7 to "95A", 8 to "95A HF"
)

// 快造 RSA 公钥 (PKCS#1 PEM, key version 0‑9)
private val SNAPMAKER_RSA_KEYS = arrayOf(
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8oEF7YuKO863TbUxnrvY
H1JFrvCnMapm8Ho952KlfNWbf6IEDMlX6QJpBuvUkrkjWpLJJQurIWL3KFeLUhCh
POrYdiGrdsUlp4YO037iLSlgmzo1dUdgbawAcGox1PvR/Naw5ADibubO2rN49WQR
+BkxxigvoWHSFetaoMCswQ5B/niq3byhzktgmWOcv71F4yFwcxivF8R+s0gSBL4i
/1zNeSUZkbvP4/T0B08i3D+e6fl9xpCnINZ3P9OWcx+p3SB2o4TdmAeKV4hkT9n7
o+/OWr92fx6qbiNKJr04oMhrRsFK6w7hitp2n8RGS64w9lhtplnBgxtbgxAYyUnp
qwIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8nbtQNABbc5PkyzI0A5m
VH/E8y23Wld0iykvTOoBYJOrPwJDmXsnSyyX84Nv6voSr8FYv3Fb2SqSdOgQLFqp
BXvntXew8rPpq5Ll8gSzLRxE1VmEOVtZWCTJ4Wxwwi79rrFmpa/nAtUeYZIGiiud
w2MzCHXW5G3c1FWhQ0C8vUUMfBQXmGnoHGsul6R8xld6CDCWY8ia/FvfR+KCtMRn
VYyYguYsq4rODWJHiFCOef4FZconUR3RTh0ojvq78CsHk94goxidWzZoKcVnvWhh
bOixTjU37W4JDECEOui3ObMMvJkzxkZo1irlAH7jTiPqhP94U/JbRDpBlHOOn67b
GQIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxZQPYewwMFaPlcEHq+SH
QS1C1NhVmAaY56qxLyHJ4aNc2iWdCx4/9ZKY4CL6xkeCD88Zndv/xzImplRdoAzo
whD47Vm4iuq8+NqHUI8na6ISd+MZ/O6/eo/ggaEZBX8lR+Yf0qfWtntsI9flUOoJ
mq1IXvNXqOxflUmPyffT40QSkAN4Rr3scB3ozlxuJZehWM/lUmZ1H5PQDwAqsM0T
Rj6ChzVmUbSvwEvbDTwpXkpMA0C5//OW0T//IKDEBYxEl928vYbraLRDRIetgdaD
o+77+ztfOv4AyP/ipikprHwIWi7yga5KUXq/XpNPy6cPISZD+/LBUJBxLELspREP
rQIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvK8cJyeFeTkFgkSLCCAg
EgR9KAvIHmvK8CRdtn+W6PiIbN04MFIg8jiYW/3fq+AcBFFMo+HtR2gym8JNVx2I
RDI4WdfbR/0gaIHjOQ41OwlXmqqSkDsFmjxVI6bDRZYpHkOfkC+9Vi1Aii4l/Yq9
O7s+2j4zP9GoUWWJPb3mW07Vu+EnHB/XIuaoDJVQAS+ov3xTotCeKdcdgySnNP5g
kOvWUvWtwNQldCRcQ0eo3j5RO+4J4IRK2J8q7BrdV/gbJUE/BBPIOuURPLzNJJO3
wgx4PEwlb5uYEUL35ARL7NzL8ZOxebzs5H4tXuWrBhALw6O33Tfg3TmTmwR2JUpv
7QIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvafhk7Bdb3F+5B9w7YXv
chrNzl09QkZc27NLxL0ViRitGQhX9KC/xVg+XkBGI8XfioAwYkJ3jYgwmci5gJOL
ofPyNXcFtvtzq2NZNuDZY26krrXLORhS1o8ue92RB2gM92Rc2heWVrsvLycNl2Qz
OUjUEGmWpSMo98xIsgkTZJ4aYxWVN86yqknOcHVpTmcr5SBRB90K9hTRtsaMD97O
FYVc7AA/TGwqFJOnXXzWczWtg7kUY2vqCHwsvKs3G/EIFKOIe1n37V94OcxHTySC
co9Kc6Y0bGFIwIruinH1WkFVt6TAzo+0ZdZy5Sq493AG9y1RZ5nYj5qUmc1PMmrD
gwIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxWdxd7qeouSFbZ2Sldv3
apDrgAupOYiDRkO85C+qkZaezOzqW0EsOV0x7nG/smw++TRfHyGIK4gXCdg1JfNR
WYjqckRdnLYMzGdDk24VV5Bbrsgska0v0Oy1ucz3CYu+F22ais00OqK0MY0B96MI
/B/0pRSTAIyxvC6LjhHy8DYyPdqNF9EMikKfAfcn7ytsH1PoSSGVtrZqyNe5OLrW
yAw+FQsTg/VFJcYxPTQJ1ymwQmDCdKgApe3PVajyYswoIA7R0S8ujau0aAFEO3dU
GDEwjOnaHfwFlg3OKMFJTxc2sl/WEB8xtWuKl0Guf0VnzWJ6noxqf/DiaN1fuHG0
AwIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqF+YJNHLHC6c25oTDgNg
liahUxWBPSkgght1/gJu5vBRDKWEn6i/RuKAFdTOsH+Hlvr5qWms7bBUHx78UMF+
FF1Nq9tb4jhFuqq4HWsBBjNnU6O0JhFTjKJU2nudmphXlpdLQfcKSIYMQe795GHL
izh8WsNTcTHNNBkjhi7y4c4RUqnJso0L6vrf0B3EB/9DDUJitrwfw+1/OrKOEVEP
624sEa802cHfb+BG9zKBXjFwzYCYF9gWey9yeA3UA7EYmPpqA1lqNv8m0r7YjZ4n
uGBDjs+AXaGtdqrW3IUtkUF2vWwNSRncbcXi3mNfzslrtPhsDVAFki4vDSw7yNht
2wIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuKWRCTTgxPltfflWHdhu
2ITxWC/LTEl7OtatNWFhMFQZF2J5SN/45bjH6xIPTcDglTSl2/UMC1D/ugiq+j0z
dGSdE7xn3ZSzLTMCwgRkvXmd8aQgafBYbB7E6oAgus+6lRXZPwnMfZAe0yaJNHyt
1Wd8ZUlRY7BHSPPtmG1liVEzxoTb6urB6mK49r24+oC7xa65q5NSdlZWSTeaK4Xt
DVVDiwe+uubNTm59KnVAKgBMNd3qN942pH6fo/dBz++BzJVEG/qJewHUTGZAeIl+
CgqhSEbmEIgolsDgaKY99ZWa2FWJdo+ohYhmjc92TyB9kWw6yIwez+tlRUkssLGt
SwIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt7XOTs6P2xB8v8/xWVdR
wVefphRDXSuv74RObtr0pwLTc7BytkcDw8r60BNPv9hGDpW2S1szxqS8x4EaOHP7
81qNpIUULlUdXxty1RvpSdfRb044kpwl7A/s4OEakkyJZF1ed+Qte1FqOFDDIZ+l
g+Co8FjOwWixoSyIlR22mEP7r6Y98GL5tnSohkVoGAgEipswWb6549mssjZmES+J
hB0axY6Dl/LlDYxN6jjUZwSIo7bw0GXGm9ScW2qTVaT1m2A9etpD6OIG+iQVLQqP
whVBs5q0o/EM4nBN88RBsF2OmfkcZPJ2NdX6o3qx+pCZ9NDgkHjGDZdnGEnM5Lu2
dwIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent(),
    """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz/d5C5FpqlcF7NbUEvBN
fiDJWH0BF63PEwHPiX+cS6l+q4NqqYI167u1pGkZGJV1njgGYFTM08x2KO7/bk6o
CWcGuKWNM8Tp1+tv3XioNGVCnIpHmdUx5F9qcXlPtDx74wQk/+JZLQ/sLnLvHcV3
YTaz55fpyzVUHkgXusdVynSyAt3ywWWQRcjp3sspGa/udC0j6LCvrzqLACv3gMGA
Id0b6REzjSn03UzkwBIwSb8DszieeNhaCOK4M/TxPFNyrhQRYcAvhiZJu+tylqJs
VP+gaWFvElFeFkxcHvYXHdJPlJLjYeT51hm/pdll26yYLhpeBa0inHwSqv4D3jFZ
PQIDAQAB
-----END RSA PUBLIC KEY-----""".trimIndent()
)

private val WRITE_HKDF_SALT = byteArrayOf(
    0x9a.toByte(), 0x75.toByte(), 0x9c.toByte(), 0xf2.toByte(),
    0xc4.toByte(), 0xf7.toByte(), 0xca.toByte(), 0xff.toByte(),
    0x22.toByte(), 0x2c.toByte(), 0xb9.toByte(), 0x76.toByte(),
    0x9b.toByte(), 0x41.toByte(), 0xbc.toByte(), 0x96.toByte()
)
private val WRITE_INFO_A = "RFID-A\u0000".toByteArray(Charsets.US_ASCII)
private val WRITE_INFO_B = "RFID-B\u0000".toByteArray(Charsets.US_ASCII)

@Composable
private fun NoticeGuideDialog(
    onDismiss: () -> Unit,
    onGoToNotice: () -> Unit
) {
    com.m0h31h31.bamburfidreader.ui.components.AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(R.string.notice_guide_title)
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(R.string.notice_guide_message)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onGoToNotice) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(R.string.notice_guide_go)
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(R.string.notice_guide_skip)
                )
            }
        }
    )
}

@Composable
private fun BoostReminderDialog(
    onDismiss: () -> Unit,
    onBoost: () -> Unit
) {
    com.m0h31h31.bamburfidreader.ui.components.AppDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.boost_reminder_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.boost_reminder_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.notice_guide_skip))
                    }
                    Button(onClick = onBoost) {
                        Text(stringResource(R.string.boost_action_go))
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAgreementDialog(
    onDecline: () -> Unit,
    onAccept: () -> Unit
) {
    com.m0h31h31.bamburfidreader.ui.components.AppDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.user_agreement_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.user_agreement_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectionContainer {
                    Text(
                        text = stringResource(R.string.user_agreement_content),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDecline) {
                        Text(text = stringResource(R.string.user_agreement_decline))
                    }
                    Button(onClick = onAccept) {
                        Text(text = stringResource(R.string.user_agreement_accept))
                    }
                }
            }
        }
    }
}

private data class WriteResumePoint(
    val sector: Int,
    val blockOffset: Int
)

private enum class WritePrecheckAction {
    START_FROM_BEGINNING,
    RESUME_FROM_POINT,
    ALREADY_MATCHED,
    BLOCKED_CONFLICT,
    BLOCKED_UNREADABLE
}

private data class WritePrecheckResult(
    val action: WritePrecheckAction,
    val resumePoint: WriteResumePoint = WriteResumePoint(0, 0),
    val message: String = ""
)

private data class SelfTagPackageExport(
    val sourceDir: File,
    val files: List<File>,
    val zipName: String
)

class MainActivity : ComponentActivity() {
    private enum class FeedbackTone {
        SUCCESS,
        FAILURE
    }

    private var nfcAdapter: NfcAdapter? = null
    private var uiState by mutableStateOf(NfcUiState(status = "Waiting for RFID tag"))
    private var filamentDbHelper: FilamentDbHelper? = null
    private var voiceEnabled by mutableStateOf(false)
    private var uiStyle by mutableStateOf(DEFAULT_APP_UI_STYLE)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var colorPalette by mutableStateOf(ColorPalette.OCEAN)
    private var bambuTagEnabled by mutableStateOf(true) // 控制拓竹RFID页面显示
    private var crealityEnabled by mutableStateOf(false) // 控制创想三维RFID页面显示
    private var snapmakerTagEnabled by mutableStateOf(false) // 控制快造复制页面显示
    private var cloudConnectEnabled by mutableStateOf(true) // 控制拓竹云连接页面显示
    private var costEnabled by mutableStateOf(true) // 控制费用/报价页面显示
    private var uiScale by mutableStateOf(1f) // 全局界面缩放比例
    private var snapmakerShareTagItems by mutableStateOf<List<SnapmakerShareTagItem>>(emptyList())
    private var snapmakerShareLoading by mutableStateOf(false)
    private var snapmakerWriteStatusMessage by mutableStateOf("")
    private var pendingSnapmakerWriteItem by mutableStateOf<SnapmakerShareTagItem?>(null)
    private var crealityStatusMessage by mutableStateOf("")
    private var crealityLastTagData by mutableStateOf<CrealityTagData?>(null)
    private var pendingCrealityWrite by mutableStateOf<CrealityWritePending?>(null)
    private var currentActiveRoute by mutableStateOf("reader")
    private var readAllSectors by mutableStateOf(false) // 控制是否读取全部扇区，默认关闭
    private var saveKeysToFile by mutableStateOf(false) // 控制是否额外导出秘钥文件
    private var forceOverwriteImport by mutableStateOf(false) // 控制导入标签包时是否覆盖同UID文件
    private var formatTagDebugEnabled by mutableStateOf(false) // 控制格式化标签调试弹窗
    private var inventoryEnabled by mutableStateOf(false) // 控制库存和数据页面显示
    private var autoDetectBrand by mutableStateOf(false)  // 自动识别RFID品牌并跳转
    private var globalNfcListenerEnabled by mutableStateOf(true) // 控制非识别页普通贴卡是否触发读卡
    private var autoShareTag by mutableStateOf(true)     // 读取完整数据后自动上传到共享服务器
    private var hideCopiedTags by mutableStateOf(true)   // 隐藏已复制标签
    private var dualTagMode by mutableStateOf(false)      // 双标签模式：复制2次才隐藏
    private var tagViewMode by mutableStateOf("category")     // 复制页视图：list/category
    private var readerBrand by mutableStateOf(ReaderBrand.BAMBU)   // 识别页品牌选择
    private var readerSnapmakerTagData by mutableStateOf<SnapmakerTagData?>(null)
    private var readerCrealityTagData by mutableStateOf<CrealityTagData?>(null)
    private var readerCrealityMaterial by mutableStateOf<CrealityMaterial?>(null)
    private var readerBrandStatus by mutableStateOf("")
    private var tts: TextToSpeech? = null
    private var ttsReady by mutableStateOf(false)
    private var ttsLanguageReady by mutableStateOf(false)
    private var lastSpokenKey: String? = null
    private var shouldNavigateToReader by mutableStateOf(false)
    private var shouldNavigateToTag by mutableStateOf(false)
    private var shouldNavigateToMisc by mutableStateOf(false)
    private var shouldScrollToNotice by mutableStateOf(false)
    private var tagPreselectedFileName by mutableStateOf<String?>(null)
    // 原始读卡临时缓存：readTag 仅负责写入；解析函数从此读取。
    private var latestRawTagData: RawTagReadData? = null
    private var latestSnapmakerRawData: RawTagReadData? = null
    private var shareTagItems by mutableStateOf<List<ShareTagItem>>(emptyList())
    private var writeStatusMessage by mutableStateOf("")
    private var pendingWriteItem by mutableStateOf<ShareTagItem?>(null)
    private var pendingVerifyItem by mutableStateOf<ShareTagItem?>(null)
    private var pendingCModifyItem by mutableStateOf<ShareTagItem?>(null)
    private var cModifyRecoveryInfo by mutableStateOf<CModifyRecoveryInfo?>(null)
    private var pendingClearFuid by mutableStateOf(false)
    private var pendingCuidTest by mutableStateOf(false)
    private var pendingNfcCompatibilityTest by mutableStateOf<PendingNfcCompatibilityTest?>(null)
    private var pendingNdefWriteRequest by mutableStateOf<NdefWriteRequest?>(null)
    private var shareLoading by mutableStateOf(false)
    private var shareRefreshStatusMessage by mutableStateOf("")
    private var shareRefreshStatusClearJob: Job? = null
    private var miscStatusMessage by mutableStateOf("")
    private var nfcCompatibilityConfig by mutableStateOf(NfcCompatibilityConfig.default())
    private var nfcCompatibilityStatusMessage by mutableStateOf("")
    private var lastWrittenSourceUidHex = ""
    private var anomalyUids by mutableStateOf<Map<String, Int>>(emptyMap())
    private var selectedTagCopyCount by mutableStateOf<Int?>(null)
    private var pendingUpdateInfo by mutableStateOf<UpdateInfo?>(null)
    private var isDownloadingUpdate by mutableStateOf(false)
    private var updateDownloadProgress by mutableStateOf(0)
    private var writeToolStatusMessage by mutableStateOf("")
    private var selfTagCount by mutableStateOf(0)
    private var debugInfoDialog: android.app.AlertDialog? = null
    private val debugInfoBuffer = StringBuilder()
    private val debugInfoLock = Any()
    // 防止 readerCallback 并发触发导致 "Close other technology first!"。
    private val readingInProgress = AtomicBoolean(false)
    // 防止共享目录重复并发扫描。
    private val shareLoadingInProgress = AtomicBoolean(false)
    private val shareLoadStatusRequested = AtomicBoolean(false)
    private var toneGenerator: ToneGenerator? = null
    private val importTagPackageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                miscStatusMessage = uiString(R.string.misc_select_tag_package_canceled)
                return@registerForActivityResult
            }
            miscStatusMessage = uiString(R.string.misc_importing_tag_package)
            lifecycleScope.launch(Dispatchers.IO) {
                val message = importTagPackageFromZipUri(uri)
                withContext(Dispatchers.Main) {
                    miscStatusMessage = message
                    refreshShareTagItemsAsync()
                }
            }
        }
    private val importSnapmakerTagPackageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                miscStatusMessage = uiString(R.string.misc_select_tag_package_canceled)
                return@registerForActivityResult
            }
            miscStatusMessage = uiString(R.string.misc_importing_tag_package)
            lifecycleScope.launch(Dispatchers.IO) {
                val message = importSnapmakerTagPackageFromZipUri(uri)
                withContext(Dispatchers.Main) {
                    miscStatusMessage = message
                    refreshSnapmakerShareTagItemsAsync()
                }
            }
        }
    private val importDatabaseLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                miscStatusMessage = uiString(R.string.db_import_canceled)
                return@registerForActivityResult
            }
            miscStatusMessage = importDatabaseFromUri(uri)
        }
    private val exportTagPackageLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(SHARE_IMPORT_ZIP_MIME)) { uri: Uri? ->
            if (uri == null) {
                miscStatusMessage = uiString(R.string.misc_export_tag_canceled)
                return@registerForActivityResult
            }
            lifecycleScope.launch(Dispatchers.IO) {
                val result = exportSelfTagPackageToUri(uri)
                withContext(Dispatchers.Main) {
                    miscStatusMessage = result
                }
            }
        }

    private fun resetDebugInfoDialog(title: String = getString(R.string.debug_info_title)) {
        synchronized(debugInfoLock) {
            debugInfoBuffer.clear()
        }
        if (!formatTagDebugEnabled) {
            runOnUiThread {
                try {
                    debugInfoDialog?.dismiss()
                } catch (_: Exception) {
                }
                debugInfoDialog = null
            }
            return
        }
        runOnUiThread {
            try {
                debugInfoDialog?.dismiss()
            } catch (_: Exception) {
            }
            debugInfoDialog = android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("")
                .setPositiveButton(getString(R.string.action_close), null)
                .create().also { it.show() }
        }
    }

    private fun appendDebugInfoDialog(message: String) {
        if (!formatTagDebugEnabled) return
        val text = synchronized(debugInfoLock) {
            if (debugInfoBuffer.isNotEmpty()) {
                debugInfoBuffer.insert(0, '\n')
            }
            debugInfoBuffer.insert(0, message)
            debugInfoBuffer.toString()
        }
        runOnUiThread {
            val dialog = debugInfoDialog
            if (dialog == null || !dialog.isShowing) {
                debugInfoDialog = android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.debug_info_title))
                    .setMessage(text)
                    .setPositiveButton(getString(R.string.action_close), null)
                    .create().also { it.show() }
            } else {
                dialog.setMessage(text)
            }
        }
    }

    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        if (!readingInProgress.compareAndSet(false, true)) {
            logEvent("读卡请求被忽略：上一次读卡尚未完成")
            return@ReaderCallback
        }
        logEvent("收到NFC标签回调")
        try {
            runOnUiThread {
                if (pendingWriteItem != null) {
                    writeStatusMessage = uiString(R.string.copy_write_in_progress)
                } else if (pendingVerifyItem != null) {
                    writeStatusMessage = uiString(R.string.copy_verify_in_progress)
                } else if (pendingCModifyItem != null) {
                    writeStatusMessage = uiString(R.string.tag_c_modify_in_progress)
                } else if (pendingNdefWriteRequest != null) {
                    writeToolStatusMessage = uiString(R.string.copy_ndef_in_progress)
                } else if (pendingClearFuid) {
                    miscStatusMessage = uiString(R.string.misc_status_formatting)
                } else if (pendingCuidTest) {
                    miscStatusMessage = uiString(R.string.misc_status_testing)
                } else if (pendingNfcCompatibilityTest != null) {
                    val message = uiString(R.string.nfc_compat_test_running)
                    nfcCompatibilityStatusMessage = message
                    miscStatusMessage = message
                } else if (pendingSnapmakerWriteItem != null) {
                    snapmakerWriteStatusMessage = uiString(R.string.snapmaker_write_in_progress_status)
                } else if (pendingCrealityWrite != null) {
                    crealityStatusMessage = uiString(R.string.creality_write_in_progress)
                }
            }
            if (pendingWriteItem != null) {
                val targetItem = pendingWriteItem
                val writeResult = if (targetItem != null) {
                    writeTagFromDump(tag, targetItem) { status ->
                        runOnUiThread { writeStatusMessage = status }
                    }
                } else {
                    uiString(R.string.copy_write_task_empty)
                }
                if (writeResult.contains("成功") || writeResult.contains("success", ignoreCase = true)) {
                    targetItem?.sourceUid?.let { rememberLastWrittenSourceUid(it) }
                    // 写入成功后自动校验（无需重复贴卡）
                    runOnUiThread { writeStatusMessage = uiString(R.string.copy_write_done_verifying) }
                    val verifyResult = if (targetItem != null) {
                        verifyTagAgainstDump(tag, targetItem)
                    } else uiString(R.string.copy_verify_task_empty)
                    runOnUiThread {
                        if (verifyResult.contains("成功") || verifyResult.contains("success", ignoreCase = true)) {
                            playFeedbackTone(FeedbackTone.SUCCESS)
                            if (targetItem != null && targetItem.dbId > 0) {
                                filamentDbHelper?.writableDatabase?.let { db ->
                                    filamentDbHelper!!.incrementShareTagCopyCount(db, targetItem.dbId)
                                    filamentDbHelper!!.setShareTagVerified(db, targetItem.dbId, true)
                                }
                                shareTagItems = shareTagItems.map { si ->
                                    if (si.dbId == targetItem.dbId) si.copy(copyCount = si.copyCount + 1, verified = true) else si
                                }
                            }
                            pendingWriteItem = null
                            writeStatusMessage = uiString(R.string.copy_write_verify_success)
                            // 上报 UID 复制事件到后端，并刷新显示的复制人数
                            if (targetItem != null) {
                                val copiedUid = targetItem.sourceUid
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val count = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.reportUidCopy(applicationContext, copiedUid)
                                    withContext(Dispatchers.Main) { if (count != null) selectedTagCopyCount = count }
                                }
                            }
                        } else {
                            // 校验失败但写入成功：递增次数，保留 pendingVerifyItem 供手动重试
                            playFeedbackTone(FeedbackTone.FAILURE)
                            if (targetItem != null && targetItem.dbId > 0) {
                                filamentDbHelper?.writableDatabase?.let { db ->
                                    filamentDbHelper!!.incrementShareTagCopyCount(db, targetItem.dbId)
                                }
                                shareTagItems = shareTagItems.map { si ->
                                    if (si.dbId == targetItem.dbId) si.copy(copyCount = si.copyCount + 1) else si
                                }
                            }
                            pendingWriteItem = null
                            pendingVerifyItem = targetItem
                            writeStatusMessage = uiString(R.string.write_success_verify_failed_format, verifyResult)
                            // 写入成功（即使校验失败）也上报复制事件
                            if (targetItem != null) {
                                val copiedUid = targetItem.sourceUid
                                lifecycleScope.launch(Dispatchers.IO) {
                                    val count = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.reportUidCopy(applicationContext, copiedUid)
                                    withContext(Dispatchers.Main) { if (count != null) selectedTagCopyCount = count }
                                }
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        writeStatusMessage = writeResult
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                }
            } else if (pendingVerifyItem != null) {
                val targetItem = pendingVerifyItem
                val result = if (targetItem != null) {
                    verifyTagAgainstDump(tag, targetItem)
                } else {
                    uiString(R.string.copy_verify_task_empty)
                }
                runOnUiThread {
                    writeStatusMessage = result
                    if (result.contains("成功") || result.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                        // 校验成功：标记已校验
                        if (targetItem != null && targetItem.dbId > 0) {
                            filamentDbHelper?.writableDatabase?.let { db ->
                                filamentDbHelper!!.setShareTagVerified(db, targetItem.dbId, true)
                            }
                            shareTagItems = shareTagItems.map { si ->
                                if (si.dbId == targetItem.dbId) si.copy(verified = true) else si
                            }
                        }
                        pendingVerifyItem = null
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                }
            } else if (pendingCModifyItem != null) {
                val targetItem = pendingCModifyItem
                val result = if (targetItem != null) {
                    writeCModifyTag(tag, targetItem) { status ->
                        runOnUiThread { writeStatusMessage = status }
                    }
                } else {
                    uiString(R.string.cmodify_task_empty)
                }
                runOnUiThread {
                    writeStatusMessage = result
                    if (result.contains("成功")) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                        pendingCModifyItem = null
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                }
            } else if (pendingNdefWriteRequest != null) {
                val request = pendingNdefWriteRequest
                val result = if (request != null) {
                    writeNdefDataAndVerify(tag, request)
                } else {
                    uiString(R.string.copy_ndef_task_empty)
                }
                runOnUiThread {
                    writeToolStatusMessage = result
                    if (result.contains("成功") || result.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                    pendingNdefWriteRequest = null
                }
            } else if (pendingClearFuid) {
                val result = clearFuidAndResetTag(tag) { status ->
                    runOnUiThread {
                        miscStatusMessage = status
                    }
                }
                runOnUiThread {
                    miscStatusMessage = result
                    if (result.contains("成功") || result.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                        // 格式化成功：重置对应 share_tags 记录的复制次数和校验标记
                        val trayUid = uiState.trayUidHex
                        if (trayUid.isNotBlank()) {
                            filamentDbHelper?.writableDatabase?.let { db ->
                                filamentDbHelper!!.resetShareTagByTrayUid(db, trayUid)
                            }
                            shareTagItems = shareTagItems.map { si ->
                                if (si.trayUid == trayUid) si.copy(copyCount = 0, verified = false) else si
                            }
                        }
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                    pendingClearFuid = false
                }
            } else if (pendingCuidTest) {
                val result = testCuidCard(tag) { status ->
                    runOnUiThread { miscStatusMessage = status }
                }
                runOnUiThread {
                    miscStatusMessage = result
                    if (result == uiString(R.string.misc_cuid_available)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                    pendingCuidTest = false
                }
            } else if (pendingNfcCompatibilityTest != null) {
                val testKind = pendingNfcCompatibilityTest
                val result = runNfcCompatibilityTest(
                    tag = tag,
                    includeWrite = testKind == PendingNfcCompatibilityTest.WRITE_VERIFY
                ) { status ->
                    runOnUiThread {
                        nfcCompatibilityStatusMessage = status
                        miscStatusMessage = status
                    }
                }
                runOnUiThread {
                    nfcCompatibilityStatusMessage = result
                    miscStatusMessage = result
                    if (result.contains("成功") || result.contains("推荐") || result.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                    pendingNfcCompatibilityTest = null
                }
                return@ReaderCallback
            } else if (pendingSnapmakerWriteItem != null) {
                val targetItem = pendingSnapmakerWriteItem!!
                val writeResult = writeSnapmakerTagFromDump(tag, targetItem) { status ->
                    runOnUiThread { snapmakerWriteStatusMessage = status }
                }
                runOnUiThread {
                    if (writeResult.contains("成功") || writeResult.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                        if (targetItem.dbId > 0) {
                            filamentDbHelper?.writableDatabase?.let { db ->
                                filamentDbHelper!!.incrementSnapmakerShareTagCopyCount(db, targetItem.dbId)
                            }
                            snapmakerShareTagItems = snapmakerShareTagItems.map { si ->
                                if (si.dbId == targetItem.dbId) si.copy(copyCount = si.copyCount + 1) else si
                            }
                        }
                        pendingSnapmakerWriteItem = null
                        snapmakerWriteStatusMessage = uiString(R.string.snapmaker_write_success)
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                        snapmakerWriteStatusMessage = writeResult
                    }
                }
            } else if (pendingCrealityWrite != null) {
                val target = pendingCrealityWrite!!
                val result = writeCrealityTag(tag, target)
                runOnUiThread {
                    crealityStatusMessage = result
                    if (result.contains("成功") || result.contains("success", ignoreCase = true)) {
                        playFeedbackTone(FeedbackTone.SUCCESS)
                        pendingCrealityWrite = null
                    } else {
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                }
            } else if (!shouldHandlePassiveNfcRead(globalNfcListenerEnabled, currentActiveRoute)) {
                logEvent("普通NFC读卡被忽略：全局监听关闭，当前页面=$currentActiveRoute")
                return@ReaderCallback
            } else if (crealityEnabled && currentActiveRoute == "creality") {
                // On the Creality screen: only attempt Creality read, stay on current screen
                val crealityResult = readCrealityTag(tag)
                runOnUiThread {
                    if (crealityResult != null) {
                        crealityLastTagData = crealityResult
                        crealityStatusMessage = uiString(R.string.creality_read_success)
                        playFeedbackTone(FeedbackTone.SUCCESS)
                    } else {
                        crealityStatusMessage = uiString(R.string.creality_read_failed)
                        playFeedbackTone(FeedbackTone.FAILURE)
                    }
                }
            } else {
                // 自动品牌检测：派生秘钥认证sector0，切换品牌后读取
                if (autoDetectBrand) {
                    val detected = detectBrandBySector0(tag)
                    if (detected != null && detected != readerBrand) {
                        runOnUiThread {
                            readerBrand = detected
                            readerBrandStatus = if (detected == ReaderBrand.BAMBU) "" else uiString(R.string.status_waiting_tag)
                        }
                        Thread.sleep(100)
                    }
                }
                when (readerBrand) {
                    ReaderBrand.BAMBU -> {
                        val result = readTag(tag)
                        runOnUiThread {
                            uiState = result
                            shouldNavigateToReader = true
                            maybeSpeakResult(result)
                            // 识别页读卡提示音：读到数据（成功/部分）播成功音，否则播失败音。
                            val readOk = result.status == uiString(R.string.status_read_success) ||
                                result.status == uiString(R.string.status_read_partial)
                            playFeedbackTone(if (readOk) FeedbackTone.SUCCESS else FeedbackTone.FAILURE)
                        }
                        val rawSnapshot = latestRawTagData
                        if (autoShareTag && rawSnapshot != null &&
                            com.m0h31h31.bamburfidreader.utils.TagShareUploader.isComplete(rawSnapshot)
                        ) {
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.m0h31h31.bamburfidreader.utils.TagShareUploader.uploadRawTag(
                                    applicationContext, "bambu", rawSnapshot
                                )
                            }
                        }
                    }
                    ReaderBrand.CREALITY -> {
                        val result = readCrealityTag(tag)
                        val material = result?.let {
                            filamentDbHelper?.getCrealityMaterialById(
                                filamentDbHelper!!.readableDatabase, it.materialId
                            )
                        }
                        runOnUiThread {
                            if (result != null) {
                                readerCrealityTagData = result
                                readerCrealityMaterial = material
                                readerBrandStatus = uiString(R.string.status_read_success)
                                playFeedbackTone(FeedbackTone.SUCCESS)
                            } else {
                                readerBrandStatus = uiString(R.string.status_read_failed)
                                playFeedbackTone(FeedbackTone.FAILURE)
                            }
                        }
                    }
                    ReaderBrand.SNAPMAKER -> {
                        val result = readSnapmakerTag(tag)
                        runOnUiThread {
                            if (result != null) {
                                readerSnapmakerTagData = result
                                readerBrandStatus = uiString(R.string.status_read_success)
                                playFeedbackTone(FeedbackTone.SUCCESS)
                            } else {
                                readerBrandStatus = uiString(R.string.status_read_failed)
                                playFeedbackTone(FeedbackTone.FAILURE)
                            }
                        }
                        val snapRaw = latestSnapmakerRawData
                        if (autoShareTag && snapRaw != null &&
                            com.m0h31h31.bamburfidreader.utils.TagShareUploader.isComplete(snapRaw)
                        ) {
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.m0h31h31.bamburfidreader.utils.TagShareUploader.uploadRawTag(
                                    applicationContext, "snapmaker", snapRaw
                                )
                            }
                        }
                    }
                }
            }
        } finally {
            readingInProgress.set(false)
        }
    }

    /**
     * 检查并更新配置文件
     */
    private fun checkAndUpdateConfig() {
        lifecycleScope.launch(Dispatchers.IO) {
            com.m0h31h31.bamburfidreader.utils.ConfigManager.checkAndUpdateConfig(
                this@MainActivity
            ) { message, updateAction ->
                runOnUiThread {
                    val builder = android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle(getString(R.string.config_update_title))
                        .setMessage(message)

                    if (message == "版本更新请到原地址下载") {
                        builder.setPositiveButton(getString(R.string.action_got_it), null)
                    } else {
                        builder.setPositiveButton(getString(R.string.action_got_it)) { _, _ ->
                            // 配置落盘和数据库重建较慢，放到后台线程执行
                            lifecycleScope.launch(Dispatchers.IO) {
                                updateAction()
                                withContext(Dispatchers.Main) {
                                    android.app.AlertDialog.Builder(this@MainActivity)
                                        .setTitle(getString(R.string.config_update_result_title))
                                        .setMessage(getString(R.string.config_update_success))
                                        .setPositiveButton(getString(R.string.action_got_it), null)
                                        .show()
                                }
                            }
                        }
                    }
                    builder.show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val uiPrefs = getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
        if (BuildConfig.VERSION_CODE == FORCE_MINIMAL_UI_STYLE_VERSION_CODE &&
            uiPrefs.getInt(KEY_UI_STYLE_FORCED_VERSION_CODE, 0) != FORCE_MINIMAL_UI_STYLE_VERSION_CODE
        ) {
            uiPrefs.edit()
                .putString(KEY_UI_STYLE, AppUiStyle.MODERN_WORKBENCH.name)
                .putInt(KEY_UI_STYLE_FORCED_VERSION_CODE, FORCE_MINIMAL_UI_STYLE_VERSION_CODE)
                .apply()
        }
        voiceEnabled = uiPrefs.getBoolean(KEY_VOICE_ENABLED, false)
        bambuTagEnabled = uiPrefs.getBoolean(KEY_BAMBU_TAG_ENABLED, true)
        crealityEnabled = uiPrefs.getBoolean(KEY_CREALITY_ENABLED, false)
        snapmakerTagEnabled = uiPrefs.getBoolean(KEY_SNAPMAKER_TAG_ENABLED, false)
        cloudConnectEnabled = uiPrefs.getBoolean(KEY_CLOUD_CONNECT_ENABLED, true)
        costEnabled = uiPrefs.getBoolean(KEY_COST_ENABLED, true)
        uiScale = uiPrefs.getFloat(KEY_UI_SCALE, 1f)
        inventoryEnabled = uiPrefs.getBoolean(KEY_INVENTORY_ENABLED, true)
        autoDetectBrand = uiPrefs.getBoolean(KEY_AUTO_DETECT_BRAND, false)
        globalNfcListenerEnabled = uiPrefs.getBoolean(KEY_GLOBAL_NFC_LISTENER, true)
        autoShareTag = uiPrefs.getBoolean(KEY_AUTO_SHARE_TAG, true)
        hideCopiedTags = uiPrefs.getBoolean(KEY_HIDE_COPIED_TAGS, true)
        dualTagMode = uiPrefs.getBoolean(KEY_DUAL_TAG_MODE, false)
        tagViewMode = uiPrefs.getString(KEY_TAG_VIEW_MODE, "category") ?: "category"
        nfcCompatibilityConfig = NfcCompatibilityPreferences.load(this)
        lastWrittenSourceUidHex = uiPrefs.getString(KEY_LAST_WRITTEN_SOURCE_UID, "").orEmpty()
        uiStyle = runCatching {
            AppUiStyle.valueOf(uiPrefs.getString(KEY_UI_STYLE, DEFAULT_APP_UI_STYLE.name).orEmpty())
        }.getOrDefault(DEFAULT_APP_UI_STYLE)
        themeMode = runCatching {
            ThemeMode.valueOf(uiPrefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name).orEmpty())
        }.getOrDefault(ThemeMode.SYSTEM)
        colorPalette = runCatching {
            ColorPalette.valueOf(uiPrefs.getString(KEY_COLOR_PALETTE, ColorPalette.OCEAN.name).orEmpty())
        }.getOrDefault(ColorPalette.OCEAN)
        var showUserAgreement by mutableStateOf(
            uiPrefs.getInt(KEY_USER_AGREEMENT_VERSION, 0) < CURRENT_USER_AGREEMENT_VERSION
        )
        val lastBoostRemind = uiPrefs.getLong(KEY_BOOST_REMIND_LAST_MS, 0L)
        var showBoostReminder by mutableStateOf(
            !showUserAgreement &&
            System.currentTimeMillis() - lastBoostRemind >= BOOST_REMIND_INTERVAL_MS
        )
        val noticeGuideShown = uiPrefs.getBoolean(KEY_NOTICE_GUIDE_SHOWN, false)
        var showNoticeGuide by mutableStateOf(false)
        LogCollector.init(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        filamentDbHelper = FilamentDbHelper(this)
        // 数据库同步和文件统计放到后台线程，避免阻塞首帧
        lifecycleScope.launch(Dispatchers.IO) {
            filamentDbHelper?.let {
                syncCrealityMaterialDatabase(this@MainActivity, it)
            }
            // 应用升级后强制触发一次 Bambu 字典 re-sync（按 versionCode 去重，每个版本只跑一次），
            // 使存量字典/库存/分享标签的颜色编码用新归一化逻辑（中间 0 统一）重新匹配。
            val lastSyncedVersion = uiPrefs.getInt(KEY_LAST_DICT_SYNC_VERSION_CODE, 0)
            if (lastSyncedVersion != BuildConfig.VERSION_CODE) {
                filamentDbHelper?.let { syncFilamentDatabase(this@MainActivity, it) }
                uiPrefs.edit().putInt(KEY_LAST_DICT_SYNC_VERSION_CODE, BuildConfig.VERSION_CODE).apply()
            }
            refreshSelfTagCount()
        }
        syncAnomalyUidsAsync()
        ensureShareDirectory()
        preloadShareTagItemsInBackground()
        if (voiceEnabled) {
            initTts()
        }
        uiState = NfcUiState(status = initialStatus())
        logEvent("应用启动")
        logDeviceInfo()
        lifecycleScope.launch(Dispatchers.IO) {
            AnalyticsReporter.reportInstallAndLaunch(this@MainActivity)
        }
        
        // 检查并更新配置文件
        checkAndUpdateConfig()

        // 检查在线更新
        checkForUpdateAsync()

        setContent {
            BambuRfidReaderTheme(themeMode = themeMode, uiStyle = uiStyle, colorPalette = colorPalette, uiScale = uiScale) {
                AppNavigation(
                    state = uiState,
                    voiceEnabled = voiceEnabled,
                    uiStyle = uiStyle,
                    readAllSectors = readAllSectors,
                    saveKeysToFile = saveKeysToFile,
                    formatTagDebugEnabled = formatTagDebugEnabled,
                    forceOverwriteImport = forceOverwriteImport,
                    nfcCompatibilityMode = nfcCompatibilityConfig.mode,
                    onNfcCompatibilityModeChange = { mode -> setNfcCompatibilityMode(mode) },
                    nfcCompatibilityStatusMessage = nfcCompatibilityStatusMessage,
                    nfcCompatibilityTestInProgress = pendingNfcCompatibilityTest != null,
                    onStartNfcCompatibilityReadTest = { enqueueNfcCompatibilityTest(includeWrite = false) },
                    onStartNfcCompatibilityWriteTest = { enqueueNfcCompatibilityTest(includeWrite = true) },
                    onCancelNfcCompatibilityTest = { cancelNfcCompatibilityTest() },
                    ttsReady = ttsReady,
                    ttsLanguageReady = ttsLanguageReady,
                    onVoiceEnabledChange = {
                        voiceEnabled = it
                        uiPrefs.edit().putBoolean(KEY_VOICE_ENABLED, it).apply()
                        if (!it) {
                            tts?.stop()
                        } else if (!ttsReady) {
                            initTts()
                        }
                    },
                    onUiStyleChange = {
                        uiStyle = it
                        uiPrefs.edit().putString(KEY_UI_STYLE, it.name).apply()
                    },
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        uiPrefs.edit().putString(KEY_THEME_MODE, it.name).apply()
                    },
                    colorPalette = colorPalette,
                    onColorPaletteChange = {
                        colorPalette = it
                        uiPrefs.edit().putString(KEY_COLOR_PALETTE, it.name).apply()
                    },
                    onReadAllSectorsChange = {
                        readAllSectors = it
                    },
                    onSaveKeysToFileChange = {
                        saveKeysToFile = it
                    },
                    onFormatTagDebugEnabledChange = {
                        formatTagDebugEnabled = it
                        if (!it) {
                            try {
                                debugInfoDialog?.dismiss()
                            } catch (_: Exception) {
                            }
                            debugInfoDialog = null
                        }
                    },
                    onForceOverwriteImportChange = {
                        forceOverwriteImport = it
                    },
                    bambuTagEnabled = bambuTagEnabled,
                    onBambuTagEnabledChange = { enabled ->
                        bambuTagEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_BAMBU_TAG_ENABLED, enabled).apply()
                    },
                    crealityEnabled = crealityEnabled,
                    onCrealityEnabledChange = { enabled ->
                        crealityEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_CREALITY_ENABLED, enabled).apply()
                    },
                    snapmakerTagEnabled = snapmakerTagEnabled,
                    onSnapmakerTagEnabledChange = { enabled ->
                        snapmakerTagEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_SNAPMAKER_TAG_ENABLED, enabled).apply()
                    },
                    cloudConnectEnabled = cloudConnectEnabled,
                    onCloudConnectEnabledChange = { enabled ->
                        cloudConnectEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_CLOUD_CONNECT_ENABLED, enabled).apply()
                    },
                    costEnabled = costEnabled,
                    onCostEnabledChange = { enabled ->
                        costEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_COST_ENABLED, enabled).apply()
                    },
                    uiScale = uiScale,
                    onUiScaleChange = { scale ->
                        uiScale = scale
                        uiPrefs.edit().putFloat(KEY_UI_SCALE, scale).apply()
                    },
                    snapmakerShareTagItems = snapmakerShareTagItems,
                    snapmakerShareLoading = snapmakerShareLoading,
                    snapmakerWriteStatusMessage = snapmakerWriteStatusMessage,
                    snapmakerWriteInProgress = pendingSnapmakerWriteItem != null,
                    onSnapmakerTagScreenEnter = { refreshSnapmakerShareTagItemsAsync() },
                    onStartWriteSnapmakerTag = { item -> enqueueSnapmakerWriteTask(item) },
                    onDeleteSnapmakerTagItem = { item -> deleteSnapmakerShareTagItem(item) },
                    onCancelSnapmakerWrite = {
                        pendingSnapmakerWriteItem = null
                        snapmakerWriteStatusMessage = ""
                    },
                    onSelectImportSnapmakerTagPackage = {
                        openSnapmakerTagPackagePicker()
                        val msg = uiString(R.string.misc_select_tag_package_prompt)
                        miscStatusMessage = msg
                        msg
                    },
                    crealityTagData = crealityLastTagData,
                    crealityStatusMessage = crealityStatusMessage,
                    crealityWriteInProgress = pendingCrealityWrite != null,
                    onCrealityPrepareWrite = { materialId, colorHex, weight ->
                        pendingCrealityWrite = CrealityWritePending(materialId, colorHex, weight)
                        crealityStatusMessage = uiString(R.string.creality_write_ready)
                    },
                    onCrealityCancelWrite = {
                        pendingCrealityWrite = null
                        crealityStatusMessage = ""
                    },
                    onCrealityClearTagData = {
                        crealityLastTagData = null
                    },
                    onActiveRouteChange = { route -> currentActiveRoute = route },
                    readerBrand = readerBrand,
                    onReaderBrandChange = { brand ->
                        readerBrand = brand
                        readerBrandStatus = if (brand == ReaderBrand.BAMBU) "" else uiString(R.string.status_waiting_tag)
                        readerCrealityTagData = null
                        readerCrealityMaterial = null
                        readerSnapmakerTagData = null
                    },
                    readerCrealityTagData = readerCrealityTagData,
                    readerCrealityMaterial = readerCrealityMaterial,
                    readerSnapmakerTagData = readerSnapmakerTagData,
                    readerBrandStatus = readerBrandStatus,
                    inventoryEnabled = inventoryEnabled,
                    onInventoryEnabledChange = { enabled ->
                        inventoryEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_INVENTORY_ENABLED, enabled).apply()
                    },
                    autoDetectBrand = autoDetectBrand,
                    onAutoDetectBrandChange = { enabled ->
                        autoDetectBrand = enabled
                        uiPrefs.edit().putBoolean(KEY_AUTO_DETECT_BRAND, enabled).apply()
                    },
                    globalNfcListenerEnabled = globalNfcListenerEnabled,
                    onGlobalNfcListenerEnabledChange = { enabled ->
                        globalNfcListenerEnabled = enabled
                        uiPrefs.edit().putBoolean(KEY_GLOBAL_NFC_LISTENER, enabled).apply()
                    },
                    autoShareTag = autoShareTag,
                    onAutoShareTagChange = { enabled ->
                        autoShareTag = enabled
                        uiPrefs.edit().putBoolean(KEY_AUTO_SHARE_TAG, enabled).apply()
                    },
                    onCheckDownloadPermission = {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            checkTagDownloadPermission()
                        }
                    },
                    onDownloadTagPackage = { brand, onProgress, onImportStatus ->
                        val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            downloadAndImportTagPackage(brand, onProgress, onImportStatus)
                        }
                        refreshShareTagItemsAsync()
                        result
                    },
                    onLoadMySharedUids = {
                        com.m0h31h31.bamburfidreader.utils.TagShareUploader.fetchMySharesWithTime(this@MainActivity)
                    },
                    hideCopiedTags = hideCopiedTags,
                    onHideCopiedTagsChange = { enabled ->
                        hideCopiedTags = enabled
                        uiPrefs.edit().putBoolean(KEY_HIDE_COPIED_TAGS, enabled).apply()
                    },
                    dualTagMode = dualTagMode,
                    onDualTagModeChange = { enabled ->
                        dualTagMode = enabled
                        uiPrefs.edit().putBoolean(KEY_DUAL_TAG_MODE, enabled).apply()
                    },
                    tagViewMode = tagViewMode,
                    onTagViewModeChange = { mode ->
                        tagViewMode = mode
                        uiPrefs.edit().putString(KEY_TAG_VIEW_MODE, mode).apply()
                    },
                    onNotesChange = { trayUid, originalMaterial, notes ->
                        updateTrayNotes(trayUid, originalMaterial, notes)
                    },
                    onTrayOutbound = { trayUid ->
                        removeTrayFromInventory(trayUid)
                    },
                    showRecoveryAction = uiState.status == uiString(R.string.status_read_partial) &&
                        uiState.uidHex.isNotBlank(),
                    onAttemptRecovery = { attemptRecoveryFromPartialRead() },
                    onRemainingChange = { trayUid, percent, grams ->
                        updateTrayRemaining(trayUid, percent, grams)
                    },
                    dbHelper = filamentDbHelper,
                    onBackupDatabase = { backupDatabase() },
                    onImportDatabase = { importDatabase() },
                    onClearFuid = { enqueueClearFuidTask() },
                    onCancelClearFuid = {
                        pendingClearFuid = false
                        miscStatusMessage = uiString(R.string.misc_cancel_format_task)
                        miscStatusMessage
                    },
                    onClearSelfTags = { clearSelfTagFiles() },
                    onClearShareTags = { clearShareTagFiles() },
                    onEnqueueCuidTest = { enqueueCuidTestTask() },
                    onCancelCuidTest = {
                        pendingCuidTest = false
                        val msg = uiString(R.string.misc_cuid_cancel)
                        miscStatusMessage = msg
                        msg
                    },
                    cuidTestInProgress = pendingCuidTest,
                    onResetDatabase = { resetDatabase() },
                    selfTagCount = selfTagCount,
                    miscStatusMessage = miscStatusMessage,
                    onExportTagPackage = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            miscStatusMessage = uiString(R.string.misc_exporting_tag_package)
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = exportSelfTagPackageToDownloads()
                                withContext(Dispatchers.Main) {
                                    miscStatusMessage = result
                                }
                            }
                            uiString(R.string.misc_exporting_tag_package)
                        } else {
                            val result = exportSelfTagPackageToDownloads()
                            miscStatusMessage = result
                            result
                        }
                    },
                    onSelectImportTagPackage = {
                        openTagPackagePicker()
                        val message = uiString(R.string.misc_select_tag_package_prompt)
                        miscStatusMessage = message
                        message
                    },
                    navigateToReader = shouldNavigateToReader,
                    navigateToTag = shouldNavigateToTag,
                    navigateToMisc = shouldNavigateToMisc,
                    scrollToNotice = shouldScrollToNotice,
                    onScrollToNoticeDone = { shouldScrollToNotice = false },
                    shareTagItems = shareTagItems,
                    tagPreselectedFileName = tagPreselectedFileName,
                    shareLoading = shareLoading,
                    writeStatusMessage = writeStatusMessage,
                    writeToolStatusMessage = writeToolStatusMessage,
                    writeInProgress = pendingWriteItem != null || pendingVerifyItem != null,
                    cModifyInProgress = pendingCModifyItem != null,
                    formatInProgress = pendingClearFuid,
                    anomalyUids = anomalyUids,
                    onReportAnomaly = { cardUid ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.reportAnomaly(applicationContext, cardUid)
                        }
                    },
                    onTagScreenEnter = {
                        refreshShareTagItemsAsync()
                        syncAnomalyUidsAsync()
                    },
                    onTagSelected = { uid ->
                        selectedTagCopyCount = null
                        lifecycleScope.launch(Dispatchers.IO) {
                            val count = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.fetchUidCopyCount(applicationContext, uid)
                            withContext(Dispatchers.Main) { selectedTagCopyCount = count }
                        }
                    },
                    selectedTagCopyCount = selectedTagCopyCount,
                    onStartWriteTag = { item ->
                        enqueueWriteTask(item)
                    },
                    onDeleteTagItem = { item ->
                        deleteShareTagItem(item)
                    },
                    onCancelWriteTag = {
                        pendingWriteItem = null
                        pendingVerifyItem = null
                        pendingCModifyItem = null
                        writeStatusMessage = uiString(R.string.copy_write_stopped_leave_page)
                    },
                    onStartCModifyTag = { item ->
                        enqueueCModifyTask(item)
                    },
                    cModifyRecoveryInfo = cModifyRecoveryInfo,
                    onDismissCModifyRecovery = { cModifyRecoveryInfo = null },
                    onStartNdefWrite = { request ->
                        enqueueNdefWriteTask(request)
                    },
                    pendingUpdateInfo = pendingUpdateInfo,
                    isDownloadingUpdate = isDownloadingUpdate,
                    updateDownloadProgress = updateDownloadProgress,
                    onStartUpdate = { info -> startUpdate(info) },
                    onDismissUpdate = { pendingUpdateInfo = null }
                )
                // 重置导航标志
                if (shouldNavigateToReader) {
                    shouldNavigateToReader = false
                }
                if (shouldNavigateToTag) {
                    shouldNavigateToTag = false
                }
                if (shouldNavigateToMisc) {
                    shouldNavigateToMisc = false
                }
                if (showUserAgreement) {
                    UserAgreementDialog(
                        onDecline = {
                            finishAffinity()
                        },
                        onAccept = {
                            uiPrefs.edit()
                                .putInt(KEY_USER_AGREEMENT_VERSION, CURRENT_USER_AGREEMENT_VERSION)
                                .apply()
                            showUserAgreement = false
                            if (System.currentTimeMillis() - lastBoostRemind < BOOST_REMIND_INTERVAL_MS
                                && !noticeGuideShown) {
                                showNoticeGuide = true
                            }
                        }
                    )
                }
                if (showBoostReminder) {
                    BoostReminderDialog(
                        onDismiss = {
                            uiPrefs.edit()
                                .putLong(KEY_BOOST_REMIND_LAST_MS, System.currentTimeMillis())
                                .apply()
                            showBoostReminder = false
                            if (!noticeGuideShown) showNoticeGuide = true
                        },
                        onBoost = {
                            uiPrefs.edit()
                                .putLong(KEY_BOOST_REMIND_LAST_MS, System.currentTimeMillis())
                                .apply()
                            showBoostReminder = false
                            if (!noticeGuideShown) showNoticeGuide = true
                            runCatching {
                                startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(BOOST_DESIGN_URI)
                                    )
                                )
                            }
                        }
                    )
                }
                if (showNoticeGuide) {
                    NoticeGuideDialog(
                        onDismiss = {
                            uiPrefs.edit().putBoolean(KEY_NOTICE_GUIDE_SHOWN, true).apply()
                            showNoticeGuide = false
                        },
                        onGoToNotice = {
                            uiPrefs.edit().putBoolean(KEY_NOTICE_GUIDE_SHOWN, true).apply()
                            showNoticeGuide = false
                            shouldNavigateToMisc = true
                            shouldScrollToNotice = true
                        }
                    )
                }
            }
        }
    }

    private fun enqueueWriteTask(item: ShareTagItem) {
        if (pendingClearFuid || pendingCuidTest || pendingNdefWriteRequest != null) {
            writeStatusMessage = uiString(R.string.copy_finish_current_task_first)
            return
        }
        val trayUid = item.trayUid.trim()
        if (trayUid.isNotBlank() && isTrayUidExists(trayUid)) {
            android.app.AlertDialog.Builder(this@MainActivity)
                .setTitle(uiString(R.string.copy_duplicate_tray_title))
                .setMessage(uiString(R.string.copy_duplicate_tray_message, trayUid))
                .setPositiveButton(uiString(R.string.copy_continue)) { _, _ ->
                    pendingWriteItem = item
                    pendingVerifyItem = null
                    writeStatusMessage = uiString(R.string.copy_write_ready)
                }
                .setNegativeButton(uiString(R.string.action_cancel)) { _, _ ->
                    writeStatusMessage = uiString(R.string.copy_duplicate_canceled)
                }
                .show()
        } else {
            pendingWriteItem = item
            pendingVerifyItem = null
            writeStatusMessage = uiString(R.string.copy_write_ready)
        }
    }

    private fun enqueueCModifyTask(item: ShareTagItem) {
        if (pendingWriteItem != null || pendingVerifyItem != null || pendingClearFuid || pendingCuidTest || pendingNdefWriteRequest != null) {
            writeStatusMessage = uiString(R.string.copy_finish_current_task_first)
            return
        }
        pendingCModifyItem = item
        writeStatusMessage = uiString(R.string.tag_c_modify_ready)
    }

    private fun attemptRecoveryFromPartialRead() {
        val uid = uiState.uidHex.trim().uppercase(Locale.US)
        if (uid.isBlank()) {
            writeStatusMessage = uiString(R.string.copy_recovery_uid_missing)
            return
        }
        writeStatusMessage = uiString(R.string.copy_recovery_searching)
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = loadShareTagItems()
            val matched = loaded.firstOrNull { it.sourceUid.uppercase(Locale.US) == uid }
            withContext(Dispatchers.Main) {
                shareTagItems = loaded
                shouldNavigateToTag = true
                if (matched != null) {
                    tagPreselectedFileName = matched.fileName
                    enqueueWriteTask(matched)
                    writeStatusMessage = uiString(
                        R.string.copy_recovery_found,
                        matched.fileName.removeSuffix(".txt")
                    )
                } else {
                    tagPreselectedFileName = null
                    writeStatusMessage = uiString(R.string.copy_recovery_not_found, uid)
                }
            }
        }
    }

    private fun buildNfcReaderModeFlags(): Int {
        var flags = NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or
            NfcAdapter.FLAG_READER_NFC_A
        if (!nfcCompatibilityConfig.forceNfcAOnly) {
            flags = flags or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V
        }
        return flags
    }

    private fun buildNfcReaderModeExtras(): Bundle {
        return Bundle().apply {
            putInt(
                NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,
                nfcCompatibilityConfig.presenceCheckDelayMs
            )
        }
    }

    private fun refreshNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return
        runCatching { adapter.disableReaderMode(this) }
        adapter.enableReaderMode(
            this,
            readerCallback,
            buildNfcReaderModeFlags(),
            buildNfcReaderModeExtras()
        )
    }

    private fun setNfcCompatibilityMode(mode: NfcCompatibilityMode) {
        nfcCompatibilityConfig = NfcCompatibilityPreferences.saveMode(this, mode)
        val message = uiString(R.string.nfc_compat_mode_changed_format, nfcCompatibilityModeLabel(mode))
        nfcCompatibilityStatusMessage = message
        miscStatusMessage = message
        refreshNfcReaderMode()
    }

    private fun nfcCompatibilityModeLabel(mode: NfcCompatibilityMode): String {
        return when (mode) {
            NfcCompatibilityMode.FAST -> uiString(R.string.nfc_compat_mode_fast)
            NfcCompatibilityMode.BALANCED -> uiString(R.string.nfc_compat_mode_balanced)
            NfcCompatibilityMode.STABLE -> uiString(R.string.nfc_compat_mode_stable)
        }
    }

    private fun enqueueNfcCompatibilityTest(includeWrite: Boolean): String {
        if (hasPendingNfcWriteOrMaintenanceTask()) {
            return uiString(R.string.nfc_task_waiting)
        }
        pendingNfcCompatibilityTest = if (includeWrite) {
            PendingNfcCompatibilityTest.WRITE_VERIFY
        } else {
            PendingNfcCompatibilityTest.READ_ONLY
        }
        val message = if (includeWrite) {
            uiString(R.string.nfc_compat_write_ready)
        } else {
            uiString(R.string.nfc_compat_read_ready)
        }
        nfcCompatibilityStatusMessage = message
        miscStatusMessage = message
        return message
    }

    private fun cancelNfcCompatibilityTest(): String {
        pendingNfcCompatibilityTest = null
        val message = uiString(R.string.nfc_compat_cancelled)
        nfcCompatibilityStatusMessage = message
        miscStatusMessage = message
        return message
    }

    private fun hasPendingNfcWriteOrMaintenanceTask(): Boolean {
        return pendingWriteItem != null ||
            pendingVerifyItem != null ||
            pendingCModifyItem != null ||
            pendingClearFuid ||
            pendingCuidTest ||
            pendingNdefWriteRequest != null ||
            pendingSnapmakerWriteItem != null ||
            pendingCrealityWrite != null ||
            pendingNfcCompatibilityTest != null
    }

    private fun rememberLastWrittenSourceUid(uidHex: String) {
        val normalized = uidHex.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            .uppercase(Locale.US)
        if (normalized.isBlank()) return
        lastWrittenSourceUidHex = normalized
        getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_WRITTEN_SOURCE_UID, normalized)
            .apply()
    }

    override fun onResume() {
        super.onResume()
        logEvent("应用进入前台")
        if (voiceEnabled && !ttsReady) {
            initTts()
        }
        val adapter = nfcAdapter
        if (adapter == null) {
            uiState = uiState.copy(status = uiString(R.string.status_device_no_nfc))
            logEvent("设备不支持 NFC")
            return
        }
        if (!adapter.isEnabled) {
            uiState = uiState.copy(status = uiString(R.string.status_nfc_disabled))
            logEvent("NFC 未开启")
            return
        }
        val pm = packageManager
        val supportsA = pm.hasSystemFeature("android.hardware.nfc.a")
        val supportsB = pm.hasSystemFeature("android.hardware.nfc.b")
        val supportsF = pm.hasSystemFeature("android.hardware.nfc.f")
        val supportsV = pm.hasSystemFeature("android.hardware.nfc.v")
        if (!supportsA) {
            logEvent("设备未声明 NFC-A，可能影响 MIFARE Classic 读取")
        }
        val flags = buildNfcReaderModeFlags()
        val extras = buildNfcReaderModeExtras()
        adapter.enableReaderMode(
            this,
            readerCallback,
            flags,
            extras
        )
        uiState = uiState.copy(status = uiString(R.string.status_waiting_tag))
        logEvent("已启用 NFC 读卡模式")
        // 处理从系统NFC分发冷启动时携带的tag（onNewIntent不会在首次启动时触发）
        val launchTag = intent?.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
        if (launchTag != null) {
            logEvent("从启动Intent中检测到NFC标签，开始处理")
            intent.removeExtra(NfcAdapter.EXTRA_TAG)
            Thread { readerCallback.onTagDiscovered(launchTag) }.start()
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        logEvent("应用进入后台，已关闭 NFC 读卡模式")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG) ?: return
        logEvent("通过系统NFC分发收到标签")
        readerCallback.onTagDiscovered(tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        logEvent("应用退出，准备打包日志")
        filamentDbHelper?.close()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        ttsLanguageReady = false
        toneGenerator?.release()
        toneGenerator = null
        val result = LogCollector.packageLogs(this)
        logDebug(result)
        LogCollector.append(this, "I", result)
    }

    private fun initialStatus(): String {
        val adapter = nfcAdapter
        return when {
            adapter == null -> uiString(R.string.status_device_no_nfc)
            !adapter.isEnabled -> uiString(R.string.status_nfc_disabled)
            else -> uiString(R.string.status_waiting_tag)
        }
    }

    private fun updateTrayRemaining(trayUidHex: String, percent: Float, grams: Int? = null) {
        if (trayUidHex.isBlank()) {
            return
        }
        val updatedPercent = percent.coerceIn(0f, 100f)
        val dbHelper = filamentDbHelper
        val db = dbHelper?.writableDatabase
        if (db != null) {
            dbHelper.upsertTrayRemaining(db, trayUidHex, updatedPercent, grams)
        }
        if (uiState.trayUidHex == trayUidHex) {
            uiState = uiState.copy(
                remainingPercent = updatedPercent,
                remainingGrams = grams ?: uiState.remainingGrams
            )
        }
        logDebug("更新耗材余量: $trayUidHex -> $updatedPercent%")
        LogCollector.append(this, "I", "更新耗材余量: $trayUidHex -> $updatedPercent%")
    }

    private fun updateTrayNotes(trayUidHex: String, originalMaterial: String, notes: String) {
        if (trayUidHex.isBlank()) return
        val dbHelper = filamentDbHelper
        val db = dbHelper?.writableDatabase
        if (db != null) {
            dbHelper.upsertTrayNotes(db, trayUidHex, originalMaterial, notes)
        }
        if (uiState.trayUidHex == trayUidHex) {
            uiState = uiState.copy(originalMaterial = originalMaterial, notes = notes)
        }
    }

    private fun removeTrayFromInventory(trayUidHex: String) {
        if (trayUidHex.isBlank()) {
            uiState = uiState.copy(status = uiString(R.string.inventory_outbound_failed_uid_missing))
            return
        }
        val db = filamentDbHelper?.writableDatabase
        if (db == null) {
            uiState = uiState.copy(status = uiString(R.string.inventory_outbound_failed_db_unavailable))
            return
        }
        try {
            filamentDbHelper?.deleteTrayInventory(db, trayUidHex)
            uiState = NfcUiState(
                status = uiString(R.string.inventory_outbound_success)
            )
            logDebug("出库成功: $trayUidHex")
            LogCollector.append(this, "I", "出库成功: $trayUidHex")
        } catch (e: Exception) {
            uiState = uiState.copy(
                status = uiString(R.string.inventory_outbound_failed_detail, e.message.orEmpty())
            )
            logDebug("出库失败: ${e.message}")
            LogCollector.append(this, "E", "出库失败: ${e.message}")
        }
    }

    private fun logEvent(message: String) {
        logDebug(message)
    }

    private fun uiString(@StringRes id: Int, vararg args: Any): String {
        return if (args.isEmpty()) getString(id) else getString(id, *args)
    }

    private fun playFeedbackTone(type: FeedbackTone) {
        val generator = toneGenerator ?: runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        }.getOrNull()?.also { toneGenerator = it } ?: return

        when (type) {
            FeedbackTone.SUCCESS -> generator.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
            FeedbackTone.FAILURE -> generator.startTone(ToneGenerator.TONE_PROP_NACK, 220)
        }
    }

    private fun logDeviceInfo() {
        logDebug(
            "设备信息: 品牌=${Build.BRAND}, 型号=${Build.MODEL}, 制造商=${Build.MANUFACTURER}"
        )
        logDebug(
            "系统信息: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}), " +
                    "构建=${Build.DISPLAY}, 指纹=${Build.FINGERPRINT}"
        )
        logDebug("内核版本: ${System.getProperty("os.version").orEmpty()}")
        val pm = packageManager
        val hasNfc = pm.hasSystemFeature("android.hardware.nfc")
        val hasHce = pm.hasSystemFeature("android.hardware.nfc.hce")
        val hasNfcF = pm.hasSystemFeature("android.hardware.nfc.hcef")
        val hasNfcA = pm.hasSystemFeature("android.hardware.nfc.a")
        val hasNfcB = pm.hasSystemFeature("android.hardware.nfc.b")
        val hasNfcV = pm.hasSystemFeature("android.hardware.nfc.v")
        val hasNfcU = pm.hasSystemFeature("android.hardware.nfc.uicc")
        logDebug(
            "NFC硬件特性: NFC=$hasNfc, HCE=$hasHce, HCEF=$hasNfcF, A=$hasNfcA, B=$hasNfcB, V=$hasNfcV, UICC=$hasNfcU"
        )
        val adapter = nfcAdapter
        if (adapter == null) {
            logDebug("NFC适配器: 未找到")
        } else {
            logDebug("NFC适配器: 已找到, 状态=${if (adapter.isEnabled) "开启" else "关闭"}")
        }
    }

    /** 备份到系统下载目录;排除 share_tags / snapmaker_share_tags 两个表(仅清空其数据,保留表结构)。 */
    private fun backupDatabase(): String {
        val dbFile = getDatabasePath(FILAMENT_DB_NAME)
        if (!dbFile.exists()) {
            return uiString(R.string.db_file_not_found)
        }
        val tmp = File(cacheDir, "filaments_backup_tmp.db")
        return try {
            // 先 checkpoint,确保 WAL 数据已并入主库文件
            runCatching {
                filamentDbHelper?.writableDatabase?.rawQuery("PRAGMA wal_checkpoint(FULL)", null)?.use { it.moveToFirst() }
            }
            dbFile.copyTo(tmp, overwrite = true)
            // 清空两个分享表的数据(不备份),并压缩
            android.database.sqlite.SQLiteDatabase.openDatabase(
                tmp.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                db.delete(SHARE_TAGS_TABLE, null, null)
                db.delete(SNAPMAKER_SHARE_TAGS_TABLE, null, null)
                db.execSQL("VACUUM")
            }
            val fileName = "filaments_backup_${
                java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            }.db"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return uiString(R.string.db_backup_failed)
                contentResolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                    ?: return uiString(R.string.db_backup_failed)
            } else {
                @Suppress("DEPRECATION")
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloads.mkdirs()
                tmp.copyTo(File(downloads, fileName), overwrite = true)
            }
            uiString(R.string.db_backup_success_download_format, fileName)
        } catch (e: Exception) {
            logDebug("数据库备份失败: ${e.message}")
            uiString(R.string.db_backup_failed)
        } finally {
            tmp.delete()
        }
    }

    /** 让用户从文件中选择备份导入(不再使用默认位置)。 */
    private fun importDatabase(): String {
        importDatabaseLauncher.launch(
            arrayOf(
                "application/octet-stream",
                "application/x-sqlite3",
                "application/vnd.sqlite3",
                "*/*"
            )
        )
        return uiString(R.string.db_import_select_file)
    }

    private fun importDatabaseFromUri(uri: Uri): String {
        val dbFile = getDatabasePath(FILAMENT_DB_NAME)
        val tmp = File(cacheDir, "filaments_import_tmp.db")
        return try {
            contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                ?: return uiString(R.string.db_import_failed)
            if (!isSqliteFile(tmp)) {
                return uiString(R.string.db_import_invalid_file)
            }
            filamentDbHelper?.close()
            // 删除旧 WAL/SHM,避免脏数据并入导入库
            File(dbFile.parentFile, "$FILAMENT_DB_NAME-wal").delete()
            File(dbFile.parentFile, "$FILAMENT_DB_NAME-shm").delete()
            tmp.copyTo(dbFile, overwrite = true)
            filamentDbHelper = FilamentDbHelper(this)
            filamentDbHelper?.writableDatabase   // 触发 onUpgrade 迁移(若导入库版本较旧)
            uiString(R.string.db_import_success)
        } catch (e: Exception) {
            logDebug("数据库导入失败: ${e.message}")
            uiString(R.string.db_import_failed)
        } finally {
            tmp.delete()
        }
    }

    /** 校验是否为 SQLite 文件(文件头 "SQLite format 3 ")。 */
    private fun isSqliteFile(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(16)
                if (input.read(header) < 16) return false
                String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun resetDatabase(): String {
        val dbFile = getDatabasePath(FILAMENT_DB_NAME)
        return try {
            filamentDbHelper?.close()
            if (dbFile.exists()) {
                dbFile.delete()
            }
            filamentDbHelper = FilamentDbHelper(this)
            filamentDbHelper?.let { syncFilamentDatabase(this, it) }
            uiString(R.string.db_reset_success)
        } catch (e: Exception) {
            logDebug("数据库重置失败: ${e.message}")
            uiString(R.string.db_reset_failed)
        }
    }

    private fun openTagPackagePicker() {
        importTagPackageLauncher.launch(
            arrayOf(
                SHARE_IMPORT_ZIP_MIME,
                "application/x-zip-compressed",
                "application/octet-stream",
                "*/*"
            )
        )
    }

    private fun exportSelfTagPackageToDownloads(): String {
        val (export, errorMessage) = prepareSelfTagPackageExport()
        if (export == null) return errorMessage ?: uiString(R.string.tag_export_prepare_failed)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, export.zipName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, SHARE_IMPORT_ZIP_MIME)
                    put(
                        android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS
                    )
                }
                val resolver = contentResolver
                val uri =
                    resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return uiString(R.string.tag_export_create_file_failed)
                writeSelfTagPackageToUri(uri, export) ?: return uiString(R.string.tag_export_open_file_failed)
            } else {
                exportTagPackageLauncher.launch(export.zipName)
                return uiString(R.string.tag_export_select_location)
            }
            uiString(R.string.tag_export_success_download_format, export.zipName)
        } catch (e: Exception) {
            logDebug("标签数据打包失败: ${e.message}")
            uiString(R.string.tag_export_failed_format, e.message.orEmpty())
        }
    }

    private fun exportSelfTagPackageToUri(uri: Uri): String {
        val (export, errorMessage) = prepareSelfTagPackageExport()
        if (export == null) return errorMessage ?: uiString(R.string.tag_export_prepare_failed)
        return try {
            writeSelfTagPackageToUri(uri, export) ?: return uiString(R.string.tag_export_open_uri_failed)
            uiString(R.string.tag_export_success_format, export.zipName)
        } catch (e: Exception) {
            logDebug("标签数据打包失败: ${e.message}")
            uiString(R.string.tag_export_failed_format, e.message.orEmpty())
        }
    }

    private fun prepareSelfTagPackageExport(): Pair<SelfTagPackageExport?, String?> {
        val externalDir = getExternalFilesDir(null)
            ?: return null to uiString(R.string.tag_export_app_dir_unavailable)
        val sourceDir = File(externalDir, "rfid_files/self_${getDeviceIdSuffix()}")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return null to uiString(R.string.tag_export_no_dir_format, sourceDir.name)
        }
        val files = sourceDir.walkTopDown().filter { it.isFile }.toList()
        if (files.isEmpty()) {
            return null to uiString(R.string.tag_export_dir_empty)
        }
        val zipName =
            "tag_package_${sourceDir.name}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.zip"
        return SelfTagPackageExport(
            sourceDir = sourceDir,
            files = files,
            zipName = zipName
        ) to null
    }

    private fun writeSelfTagPackageToUri(uri: Uri, export: SelfTagPackageExport): Uri? {
        val resolver = contentResolver
        resolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                export.files.forEach { file ->
                    val relative = file.relativeTo(export.sourceDir).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry("${export.sourceDir.name}/$relative"))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: return null
        return uri
    }

    /** 计算标签包 ZIP 密码（与服务器算法一致：SHA-256(install_id:TAG_PACKAGE_KEY)[:16]）*/
    private fun computeTagZipPassword(): String {
        val installId = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.getInstallId(this)
        val raw = "$installId:${BuildConfig.TAG_PACKAGE_KEY}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    /**
     * 将 URI 内容复制到临时文件，用 zip4j 打开（自动处理加密/非加密），
     * 返回 (entryName, bytes) 列表。处理完成后删除临时文件。
     */
    private fun extractZipEntries(uri: Uri): List<Pair<String, ByteArray>> {
        val tmp = File(cacheDir, "import_${System.currentTimeMillis()}.zip")
        try {
            contentResolver.openInputStream(uri)?.use { inp ->
                tmp.outputStream().use { inp.copyTo(it) }
            }
            val zipFile = Zip4jFile(tmp)
            if (zipFile.isEncrypted) {
                zipFile.setPassword(computeTagZipPassword().toCharArray())
            }
            val entries = mutableListOf<Pair<String, ByteArray>>()
            for (header in zipFile.fileHeaders) {
                if (!header.isDirectory) {
                    val bytes = zipFile.getInputStream(header).use { it.readBytes() }
                    entries.add(Pair(header.fileName, bytes))
                }
            }
            return entries
        } catch (e: Zip4jException) {
            logDebug("ZIP 解压失败（密码错误或格式不支持）: ${e.message}")
            return emptyList()
        } finally {
            tmp.delete()
        }
    }

    private fun importTagPackageFromZipUri(uri: Uri): String {
        val dbHelper = filamentDbHelper ?: return uiString(R.string.db_unavailable)
        val db = dbHelper.writableDatabase
        return try {
            var extractedCount = 0
            var skippedCount = 0
            var invalidCount = 0
            var overwrittenCount = 0

            // 从 DB 加载已有的 file_uid，用于去重（纯 DB 操作）
            val existingRows = dbHelper.getAllShareTagRows(db)
            val existingFileUidSet = existingRows.map { it.fileUid.uppercase(Locale.US) }.toMutableSet()

            val zipEntries = extractZipEntries(uri)
            if (zipEntries.isEmpty() && !(cacheDir.listFiles()?.any { it.name.startsWith("import_") } ?: false)) {
                return uiString(R.string.tag_import_invalid_zip)
            }
            for ((entryName, bytes) in zipEntries) {
                if (!entryName.lowercase(Locale.US).endsWith(".txt")) continue
                val incomingUid = File(entryName).nameWithoutExtension.uppercase(Locale.US)
                val alreadyExists = incomingUid.isNotBlank() && existingFileUidSet.contains(incomingUid)

                if (alreadyExists && !forceOverwriteImport) { skippedCount++; continue }

                val content = String(bytes, Charsets.UTF_8)
                val rawBlocks = parseHexTagFileStrict(content)
                if (rawBlocks == null) { invalidCount++; continue }
                if (!isImportableBambuTag(rawBlocks)) { invalidCount++; continue }

                val preview = NfcTagProcessor.parseForPreview(rawBlocks, filamentDbHelper) { }
                val trayUid = preview.trayUidHex.trim()

                if (alreadyExists && forceOverwriteImport && incomingUid.isNotBlank()) {
                    dbHelper.deleteShareTagByFileUid(db, incomingUid)
                }

                val normalized = content.trim().lines()
                    .map { it.trim() }.filter { it.isNotBlank() }.take(64).joinToString("\n")
                val productionDate = extractProductionDate(rawBlocks)
                val rowId = dbHelper.insertShareTag(
                    db,
                    fileUid = incomingUid,
                    trayUid = trayUid.ifBlank { null },
                    materialId = preview.materialId.ifBlank { null },
                    materialType = preview.displayData.type.ifBlank { null },
                    materialDetailedType = preview.displayData.detailedType.ifBlank { null },
                    colorUid = preview.displayData.colorCode.ifBlank { null },
                    filaColorCode = preview.displayData.filaColorCode.ifBlank { null },
                    colorName = preview.displayData.colorName.ifBlank { null },
                    colorNameEn = preview.displayData.colorNameEn.ifBlank { null },
                    colorType = preview.displayData.colorType.ifBlank { null },
                    colorValues = preview.displayData.colorValues.joinToString(",").ifBlank { null },
                    rawData = normalized,
                    productionDate = productionDate
                )
                if (rowId >= 0) {
                    extractedCount++
                    if (alreadyExists && forceOverwriteImport) overwrittenCount++
                    if (incomingUid.isNotBlank()) existingFileUidSet.add(incomingUid)
                } else {
                    logDebug("insertShareTag conflict for fileUid=$incomingUid")
                    skippedCount++
                }
            }

            when {
                extractedCount == 0 && skippedCount == 0 && invalidCount == 0 ->
                    uiString(R.string.tag_import_no_txt_data)
                extractedCount == 0 ->
                    uiString(R.string.tag_import_zero_format, invalidCount, skippedCount)
                forceOverwriteImport ->
                    uiString(R.string.tag_import_success_overwrite_format, extractedCount, overwrittenCount, invalidCount, skippedCount)
                else ->
                    uiString(R.string.tag_import_success_format, extractedCount, invalidCount, skippedCount)
            }
        } catch (e: Exception) {
            logDebug("导入标签包失败: ${e.message}")
            uiString(R.string.tag_import_failed_format, e.message.orEmpty())
        }
    }
    
    // ── 在线下载共享标签包 ──────────────────────────────────────────────────────

    // ── 在线下载权限检查 ──────────────────────────────────────────────────────

    /** 检查当前设备是否有资格下载标签包。IO 线程调用。返回 null 表示允许，非 null 为拒绝原因。 */
    private fun checkTagDownloadPermission(): String? {
        val installId = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.getInstallId(this)
        val endpoint = com.m0h31h31.bamburfidreader.utils.ConfigManager.getTagCanDownloadEndpoint(this)
        return try {
            val url = java.net.URL("$endpoint?install_id=${java.net.URLEncoder.encode(installId, "UTF-8")}")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            val code = conn.responseCode
            val body = if (code in 200..299)
                conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
            else
                conn.errorStream?.use { it.readBytes() }?.toString(Charsets.UTF_8) ?: ""
            conn.disconnect()
            val json = org.json.JSONObject(body)
            if (json.optBoolean("allowed", false)) null
            else json.optString("reason", getString(R.string.download_tag_package_failed))
        } catch (e: Exception) {
            logDebug("checkTagDownloadPermission error: ${e.message}")
            getString(R.string.download_tag_package_failed)
        }
    }

    // ── 在线下载并导入标签包 ──────────────────────────────────────────────────

    /**
     * 下载指定品牌的标签包并导入（suspend）。
     * onProgress(0..100) 在 IO 线程回调，调用方用 withContext(Main) 更新 UI。
     */
    private suspend fun downloadAndImportTagPackage(
        brand: String,
        onProgress: (Int) -> Unit,
        onImportStatus: (String) -> Unit
    ): String {
        val t0 = System.currentTimeMillis()
        logDebug("DL_IMPORT[$brand] ── 开始")
        val installId = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.getInstallId(this)
        val endpoint = com.m0h31h31.bamburfidreader.utils.ConfigManager.getTagDownloadEndpoint(this)
        val payload = org.json.JSONObject().apply {
            put("install_id", installId)
            put("brand", brand)
        }
        val tmp = java.io.File(cacheDir, "dl_${brand}_${System.currentTimeMillis()}.zip")
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        return try {
            logDebug("DL_IMPORT[$brand] ── 开始下载 endpoint=$endpoint")
            val tDl = System.currentTimeMillis()
            val result = com.m0h31h31.bamburfidreader.utils.NetworkUtils.postJsonDownloadToFile(
                endpoint, payload, tmp, onProgress = onProgress
            ) ?: return getString(R.string.download_tag_package_failed)
            logDebug("DL_IMPORT[$brand] ── 下载完成 size=${tmp.length()}B 耗时=${System.currentTimeMillis()-tDl}ms")

            val (code, errBody) = result
            if (code !in 200..299) {
                val detail = try {
                    org.json.JSONObject(errBody!!.toString(Charsets.UTF_8)).optString("detail", "")
                } catch (_: Exception) { "" }
                return if (detail.isNotBlank()) detail
                       else getString(R.string.download_tag_package_failed)
            }
            logDebug("DL_IMPORT[$brand] ── 开始导入 file=${tmp.name}")
            val tImp = System.currentTimeMillis()
            val msg = importTagPackageFromZipFile(tmp, snapmaker = brand == "snapmaker") { cur, total ->
                mainHandler.post {
                    val pct = if (total > 0) minOf(cur * 100 / total, 99) else 0
                    onProgress(pct)
                    onImportStatus(uiString(R.string.tag_import_progress_format, cur, total))
                }
            }
            logDebug("DL_IMPORT[$brand] ── 导入完成 耗时=${System.currentTimeMillis()-tImp}ms 总耗时=${System.currentTimeMillis()-t0}ms 结果=$msg")
            msg
        } finally {
            tmp.delete()
        }
    }

    /**
     * 直接从 zip 文件导入标签包（不经过 ContentResolver/FileProvider），
     * 复用现有解析逻辑，适用于下载后的临时文件。
     *
     * 支持两种格式：
     *  - M0RF 格式（新）：整文件 AES-GCM，一次 PBKDF2 推导 → 毫秒级解密
     *  - zip4j WZ_AES（旧）：逐条目加密，慢但保留兼容
     */
    private fun importTagPackageFromZipFile(
        zipFile: java.io.File,
        snapmaker: Boolean,
        onProgress: ((cur: Int, total: Int) -> Unit)? = null
    ): String {
        val dbHelper = filamentDbHelper ?: return uiString(R.string.db_unavailable)
        val db = dbHelper.writableDatabase
        return try {
            val t0 = System.currentTimeMillis()
            val allBytes = zipFile.readBytes()
            val isM0RF = allBytes.size >= 4 &&
                allBytes[0] == 'M'.code.toByte() &&
                allBytes[1] == '0'.code.toByte() &&
                allBytes[2] == 'R'.code.toByte() &&
                allBytes[3] == 'F'.code.toByte()
            logDebug("DL_IMPORT 检测格式 isM0RF=$isM0RF size=${allBytes.size}B 耗时=${System.currentTimeMillis()-t0}ms")

            val tRead = System.currentTimeMillis()
            val list = mutableListOf<Pair<String, ByteArray>>()

            if (isM0RF) {
                // 新格式：M0RF(4) + salt(16) + nonce(12) + AES-GCM ciphertext
                val salt       = allBytes.copyOfRange(4, 20)
                val nonce      = allBytes.copyOfRange(20, 32)
                val ciphertext = allBytes.copyOfRange(32, allBytes.size)
                val password   = computeTagZipPassword()
                val keySpec    = PBEKeySpec(password.toCharArray(), salt, 100000, 256)
                val key        = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
                val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                val plainZip = cipher.doFinal(ciphertext)
                logDebug("DL_IMPORT AES-GCM 解密完成 plainSize=${plainZip.size}B 耗时=${System.currentTimeMillis()-tRead}ms")
                val tZip = System.currentTimeMillis()
                ZipInputStream(plainZip.inputStream()).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            list.add(Pair(entry.name, zis.readBytes()))
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                logDebug("DL_IMPORT M0RF 解压完成 条目=${list.size} 耗时=${System.currentTimeMillis()-tZip}ms")
            } else {
                // 旧格式：zip4j WZ_AES 逐条目加密
                val encrypted = net.lingala.zip4j.ZipFile(zipFile).isEncrypted
                val password  = if (encrypted) computeTagZipPassword().toCharArray() else null
                net.lingala.zip4j.io.inputstream.ZipInputStream(
                    allBytes.inputStream().buffered(65536), password
                ).use { zis ->
                    var header = zis.nextEntry
                    while (header != null) {
                        if (!header.isDirectory) {
                            list.add(Pair(header.fileName, zis.readAllBytes()))
                        }
                        header = zis.nextEntry
                    }
                }
                logDebug("DL_IMPORT zip4j ZipInputStream(encrypted=$encrypted) 读入完成 条目=${list.size} 耗时=${System.currentTimeMillis()-tRead}ms")
            }

            val entries: List<Pair<String, ByteArray>> = list
            val totalBytes = entries.sumOf { it.second.size }
            logDebug("DL_IMPORT 读入完成 条目=${entries.size} 总字节=${totalBytes}B 总耗时=${System.currentTimeMillis()-t0}ms")

            val tProc = System.currentTimeMillis()
            val result = if (snapmaker) processSnapmakerZipEntries(entries, db, dbHelper, onProgress)
                         else processBambuZipEntries(entries, db, dbHelper, onProgress)
            logDebug("DL_IMPORT process 耗时=${System.currentTimeMillis()-tProc}ms")
            result
        } catch (e: Exception) {
            logDebug("importTagPackageFromZipFile error: ${e.message}")
            uiString(R.string.tag_import_failed_general_format, e.message.orEmpty())
        }
    }

    // ── 快造标签包导入 ──────────────────────────────────────────────────────────

    private fun openSnapmakerTagPackagePicker() {
        importSnapmakerTagPackageLauncher.launch(
            arrayOf(
                SHARE_IMPORT_ZIP_MIME,
                "application/x-zip-compressed",
                "application/octet-stream",
                "*/*"
            )
        )
    }

    private fun importSnapmakerTagPackageFromZipUri(uri: Uri): String {
        val dbHelper = filamentDbHelper ?: return uiString(R.string.db_unavailable)
        val db = dbHelper.writableDatabase
        return try {
            var extractedCount = 0
            var skippedCount = 0
            var invalidCount = 0

            val existingUids = dbHelper.getAllSnapmakerShareTagUids(db)
            val existingUidSet = existingUids.map { it.uppercase(Locale.US) }.toMutableSet()

            val zipEntries = extractZipEntries(uri)
            for ((entryName, bytes) in zipEntries) {
                if (!entryName.lowercase(Locale.US).endsWith(".txt")) continue
                val incomingUid = File(entryName).nameWithoutExtension.uppercase(Locale.US)
                val alreadyExists = incomingUid.isNotBlank() && existingUidSet.contains(incomingUid)
                if (alreadyExists) { skippedCount++; continue }

                val content = String(bytes, Charsets.UTF_8)
                val rawBlocks = parseHexTagFileStrict(content)
                if (rawBlocks == null) { invalidCount++; continue }
                if (!isValidSnapmakerTag(rawBlocks)) { invalidCount++; continue }

                val fields = parseSnapmakerShareFields(rawBlocks)
                val normalized = content.trim().lines()
                    .map { it.trim() }.filter { it.isNotBlank() }.take(64).joinToString("\n")

                dbHelper.insertSnapmakerShareTag(
                    db,
                    uid = incomingUid,
                    vendor = fields.vendor,
                    manufacturer = fields.manufacturer,
                    mainType = fields.mainType,
                    diameter = fields.diameter,
                    weight = fields.weight,
                    rgb1 = fields.rgb1,
                    mfDate = fields.mfDate,
                    rawData = normalized
                )
                extractedCount++
                existingUidSet.add(incomingUid)
            }

            when {
                extractedCount == 0 && skippedCount == 0 && invalidCount == 0 ->
                    uiString(R.string.tag_import_no_txt_data)
                extractedCount == 0 ->
                    uiString(R.string.tag_import_zero_format, invalidCount, skippedCount)
                else ->
                    uiString(R.string.snapmaker_import_success_format, extractedCount, invalidCount, skippedCount)
            }
        } catch (e: Exception) {
            logDebug("导入快造标签包失败: ${e.message}")
            uiString(R.string.snapmaker_import_failed_format, e.message.orEmpty())
        }
    }

    // ── zip entries 通用处理（供 URI 导入和文件直接导入共用）───────────────────

    private fun processBambuZipEntries(
        entries: List<Pair<String, ByteArray>>,
        db: android.database.sqlite.SQLiteDatabase,
        dbHelper: FilamentDbHelper,
        onProgress: ((cur: Int, total: Int) -> Unit)? = null
    ): String {
        val helper = dbHelper
        var extractedCount = 0; var skippedCount = 0
        var invalidCount = 0;   var overwrittenCount = 0
        val txtEntries = entries.filter { it.first.lowercase(Locale.US).endsWith(".txt") }
        val total = txtEntries.size
        var processed = 0
        val reportEvery = maxOf(1, total / 200)
        val tExist = System.currentTimeMillis()
        val existingRows = helper.getAllShareTagRows(db)
        val existingFileUidSet = existingRows.map { it.fileUid.uppercase(Locale.US) }.toMutableSet()
        logDebug("DL_IMPORT [bambu] 查已有记录=${existingRows.size}条 耗时=${System.currentTimeMillis()-tExist}ms 待处理txt=$total")
        val filamentCache = HashMap<String, List<FilamentColorEntry>>()
        val tTx = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for ((entryName, bytes) in txtEntries) {
                val incomingUid = File(entryName).nameWithoutExtension.uppercase(Locale.US)
                val alreadyExists = incomingUid.isNotBlank() && existingFileUidSet.contains(incomingUid)
                if (alreadyExists && !forceOverwriteImport) {
                    skippedCount++; processed++
                    if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
                    continue
                }
                val content = String(bytes, Charsets.UTF_8)
                val rawBlocks = parseHexTagFileStrict(content)
                if (rawBlocks == null) {
                    invalidCount++; processed++
                    if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
                    continue
                }
                val preview = NfcTagProcessor.parseForPreview(rawBlocks, filamentDbHelper, filamentCache) { }
                val trayUid = preview.trayUidHex.trim()
                if (alreadyExists && forceOverwriteImport && incomingUid.isNotBlank()) {
                    helper.deleteShareTagByFileUid(db, incomingUid)
                }
                val normalized = content.trim().lines()
                    .map { it.trim() }.filter { it.isNotBlank() }.take(64).joinToString("\n")
                val productionDate = extractProductionDate(rawBlocks)
                val rowId = helper.insertShareTag(
                    db, fileUid = incomingUid, trayUid = trayUid.ifBlank { null },
                    materialId = preview.materialId.ifBlank { null },
                    materialType = preview.displayData.type.ifBlank { null },
                    materialDetailedType = preview.displayData.detailedType.ifBlank { null },
                    colorUid = preview.displayData.colorCode.ifBlank { null },
                    filaColorCode = preview.displayData.filaColorCode.ifBlank { null },
                    colorName = preview.displayData.colorName.ifBlank { null },
                    colorNameEn = preview.displayData.colorNameEn.ifBlank { null },
                    colorType = preview.displayData.colorType.ifBlank { null },
                    colorValues = preview.displayData.colorValues.joinToString(",").ifBlank { null },
                    rawData = normalized, productionDate = productionDate
                )
                if (rowId >= 0) {
                    extractedCount++
                    if (alreadyExists && forceOverwriteImport) overwrittenCount++
                    if (incomingUid.isNotBlank()) existingFileUidSet.add(incomingUid)
                } else {
                    logDebug("insertShareTag conflict for fileUid=$incomingUid")
                    skippedCount++
                }
                processed++
                if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        logDebug("DL_IMPORT [bambu] 事务提交完成 耗时=${System.currentTimeMillis()-tTx}ms extracted=$extractedCount skipped=$skippedCount invalid=$invalidCount filamentCacheSize=${filamentCache.size}")
        return when {
            extractedCount == 0 && skippedCount == 0 && invalidCount == 0 ->
                uiString(R.string.tag_import_no_data)
            extractedCount == 0 ->
                uiString(R.string.tag_import_zero_format, invalidCount, skippedCount)
            forceOverwriteImport ->
                uiString(R.string.bambu_import_success_overwrite_format, extractedCount, overwrittenCount, invalidCount, skippedCount)
            else ->
                uiString(R.string.bambu_import_success_format, extractedCount, invalidCount, skippedCount)
        }
    }

    private fun processSnapmakerZipEntries(
        entries: List<Pair<String, ByteArray>>,
        db: android.database.sqlite.SQLiteDatabase,
        dbHelper: FilamentDbHelper,
        onProgress: ((cur: Int, total: Int) -> Unit)? = null
    ): String {
        val helper = dbHelper
        var extractedCount = 0; var skippedCount = 0; var invalidCount = 0
        val txtEntries = entries.filter { it.first.lowercase(Locale.US).endsWith(".txt") }
        val total = txtEntries.size
        var processed = 0
        val reportEvery = maxOf(1, total / 200)
        val tExist = System.currentTimeMillis()
        val existingUidSet = helper.getAllSnapmakerShareTagUids(db)
            .map { it.uppercase(Locale.US) }.toMutableSet()
        logDebug("DL_IMPORT [snapmaker] 查已有记录=${existingUidSet.size}条 耗时=${System.currentTimeMillis()-tExist}ms 待处理txt=$total")
        val tTx = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for ((entryName, bytes) in txtEntries) {
                val incomingUid = File(entryName).nameWithoutExtension.uppercase(Locale.US)
                if (incomingUid.isNotBlank() && existingUidSet.contains(incomingUid)) {
                    skippedCount++; processed++
                    if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
                    continue
                }
                val content = String(bytes, Charsets.UTF_8)
                val rawBlocks = parseHexTagFileStrict(content)
                if (rawBlocks == null) {
                    invalidCount++; processed++
                    if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
                    continue
                }
                val fields = parseSnapmakerShareFields(rawBlocks)
                val normalized = content.trim().lines()
                    .map { it.trim() }.filter { it.isNotBlank() }.take(64).joinToString("\n")
                helper.insertSnapmakerShareTag(
                    db, uid = incomingUid, vendor = fields.vendor,
                    manufacturer = fields.manufacturer, mainType = fields.mainType,
                    diameter = fields.diameter, weight = fields.weight,
                    rgb1 = fields.rgb1, mfDate = fields.mfDate, rawData = normalized
                )
                extractedCount++
                existingUidSet.add(incomingUid)
                processed++
                if (processed % reportEvery == 0 || processed == total) onProgress?.invoke(processed, total)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        logDebug("DL_IMPORT [snapmaker] 事务提交完成 耗时=${System.currentTimeMillis()-tTx}ms extracted=$extractedCount skipped=$skippedCount invalid=$invalidCount")
        return when {
            extractedCount == 0 && skippedCount == 0 && invalidCount == 0 ->
                uiString(R.string.tag_import_no_data)
            extractedCount == 0 ->
                uiString(R.string.tag_import_zero_format, invalidCount, skippedCount)
            else ->
                uiString(R.string.snapmaker_import_dl_success_format, extractedCount, invalidCount, skippedCount)
        }
    }

    private data class SnapmakerShareFields(
        val vendor: String,
        val manufacturer: String,
        val mainType: Int,
        val subType: Int,
        val diameter: Int,
        val weight: Int,
        val rgb1: Int,
        val mfDate: String
    )

    private fun parseSnapmakerShareFields(rawBlocks: List<ByteArray?>): SnapmakerShareFields {
        fun le16(block: ByteArray?, offset: Int): Int {
            if (block == null || offset + 1 >= block.size) return 0
            return ((block[offset + 1].toInt() and 0xFF) shl 8) or (block[offset].toInt() and 0xFF)
        }
        fun readRgb(block: ByteArray?, offset: Int): Int {
            if (block == null || offset + 2 >= block.size) return 0
            return ((block[offset].toInt() and 0xFF) shl 16) or
                   ((block[offset + 1].toInt() and 0xFF) shl 8) or
                   (block[offset + 2].toInt() and 0xFF)
        }

        val block1 = rawBlocks.getOrNull(1)  // sector0 block1: vendor
        val block2 = rawBlocks.getOrNull(2)  // sector0 block2: manufacturer
        val block4 = rawBlocks.getOrNull(4)  // sector1 block0: mainType/subType/colors
        val block5 = rawBlocks.getOrNull(5)  // sector1 block1: rgb values
        val block8 = rawBlocks.getOrNull(8)  // sector2 block0: diameter/weight
        val block10 = rawBlocks.getOrNull(10) // sector2 block2: mfDate

        val vendor = block1?.let { String(it, 0, minOf(16, it.size), Charsets.US_ASCII).trimEnd('\u0000') }.orEmpty()
        val manufacturer = block2?.let { String(it, 0, minOf(16, it.size), Charsets.US_ASCII).trimEnd('\u0000') }.orEmpty()
        val mainType = le16(block4, 2)
        val subType = le16(block4, 4)
        val diameter = le16(block8, 0)
        val weight = le16(block8, 2)
        val rgb1 = readRgb(block5, 0)
        val mfDate = block10?.let { String(it, 0, minOf(8, it.size), Charsets.US_ASCII).trimEnd('\u0000') }.orEmpty()

        return SnapmakerShareFields(vendor, manufacturer, mainType, subType, diameter, weight, rgb1, mfDate)
    }

    private fun isValidSnapmakerTag(rawBlocks: List<ByteArray?>): Boolean {
        // 校验 1：所有 16 个扇区的 trailer 权限位必须为 87 87 87 69
        for (sector in 0 until 16) {
            val trailer = rawBlocks.getOrNull(sector * 4 + 3) ?: return false
            if (trailer.size < 16) return false
            if (trailer[6] != 0x87.toByte() ||
                trailer[7] != 0x87.toByte() ||
                trailer[8] != 0x87.toByte() ||
                trailer[9] != 0x69.toByte()
            ) return false
        }

        // 校验 2：使用 UID 派生快造密钥，逐扇区比对 KeyA 和 KeyB
        val block0 = rawBlocks.getOrNull(0) ?: return false
        if (block0.size < 4) return false
        val uid = block0.copyOfRange(0, 4)
        val (expectedKeysA, expectedKeysB) = try {
            deriveSnapmakerKeys(uid)
        } catch (_: Exception) {
            return false
        }
        for (sector in 0 until 16) {
            val trailer = rawBlocks[sector * 4 + 3]!!
            val actualKeyA = trailer.copyOfRange(0, 6)
            val actualKeyB = trailer.copyOfRange(10, 16)
            if (!actualKeyA.contentEquals(expectedKeysA.getOrNull(sector) ?: return false)) return false
            if (!actualKeyB.contentEquals(expectedKeysB.getOrNull(sector) ?: return false)) return false
        }
        return true
    }

    private fun refreshSnapmakerShareTagItemsAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dbHelper = filamentDbHelper ?: return@launch
            withContext(Dispatchers.Main) { snapmakerShareLoading = true }
            val rows = dbHelper.getAllSnapmakerShareTagRows(dbHelper.readableDatabase)
            val items = rows.mapNotNull { row ->
                val rawData = row.rawData ?: return@mapNotNull null
                val rawBlocks = parseHexTagFileStrict(rawData) ?: return@mapNotNull null
                val block4 = rawBlocks.getOrNull(4)
                val subType = if (block4 != null && block4.size >= 6)
                    ((block4[5].toInt() and 0xFF) shl 8) or (block4[4].toInt() and 0xFF)
                else 0
                SnapmakerShareTagItem(
                    uid = row.uid,
                    vendor = row.vendor.orEmpty(),
                    manufacturer = row.manufacturer.orEmpty(),
                    mainType = row.mainType,
                    subType = subType,
                    diameter = row.diameter,
                    weight = row.weight,
                    rgb1 = row.rgb1,
                    mfDate = row.mfDate.orEmpty(),
                    rawBlocks = rawBlocks,
                    dbId = row.id,
                    copyCount = row.copyCount
                )
            }
            withContext(Dispatchers.Main) {
                snapmakerShareTagItems = items
                snapmakerShareLoading = false
            }
        }
    }

    private fun deleteSnapmakerShareTagItem(item: SnapmakerShareTagItem): String {
        val dbHelper = filamentDbHelper ?: return uiString(R.string.db_unavailable)
        dbHelper.deleteSnapmakerShareTagByUid(dbHelper.writableDatabase, item.uid)
        snapmakerShareTagItems = snapmakerShareTagItems.filter { it.uid != item.uid }
        return uiString(R.string.snapmaker_item_deleted)
    }

    private fun enqueueSnapmakerWriteTask(item: SnapmakerShareTagItem) {
        pendingSnapmakerWriteItem = item
        snapmakerWriteStatusMessage = uiString(R.string.snapmaker_write_place_blank_card)
    }

    private fun writeSnapmakerTagFromDump(
        tag: Tag,
        item: SnapmakerShareTagItem,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String {
        val mifare = MifareClassic.get(tag) ?: return uiString(R.string.snapmaker_write_failed_no_mifare)
        val sourceBlocks = item.rawBlocks
        if (sourceBlocks.isEmpty()) return uiString(R.string.snapmaker_write_failed_empty)

        val uid = tag.id ?: return uiString(R.string.snapmaker_write_failed_no_uid)
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val targetSectorCount = minOf(16, mifare.sectorCount)
        val fullFfTrailer = ByteArray(16).apply {
            for (i in 0..5) this[i] = 0xFF.toByte()
            this[6] = 0xFF.toByte(); this[7] = 0x07.toByte()
            this[8] = 0x80.toByte(); this[9] = 0x69.toByte()
            for (i in 10..15) this[i] = 0xFF.toByte()
        }

        return try {
            mifare.connect()
            Thread.sleep(700)

            // Phase 0: 检测卡片状态——先尝试 FF 秘钥，再尝试快造派生秘钥
            val (snapKeysA, snapKeysB) = deriveSnapmakerKeys(uid)
            val sector0FfAuth = authenticateSectorWithRetry(mifare, 0, listOf(ffKey), listOf(ffKey))

            if (!sector0FfAuth) {
                // 非空白卡，检查是否为已写入的快造卡
                reconnectMifareClassic(mifare)
                val sector0DerivedAuth = authenticateSectorWithRetry(
                    mifare, 0, listOf(snapKeysA[0]), listOf(snapKeysB[0])
                )
                if (!sector0DerivedAuth) {
                    return uiString(R.string.snapmaker_write_failed_auth)
                }

                // Phase 1: 将所有扇区从派生秘钥重置为全 FF
                onStatusUpdate?.invoke(uiString(R.string.snapmaker_write_detected_resetting))
                for (sector in 0 until targetSectorCount) {
                    onStatusUpdate?.invoke(uiString(R.string.snapmaker_write_resetting_format, sector + 1, targetSectorCount))
                    val trailerBlock = mifare.sectorToBlock(sector) + 3
                    val curKeyA = snapKeysA[sector]
                    val curKeyB = snapKeysB[sector]

                    // 步骤1：仅用派生 KeyB 认证，将权限位改为 FF078069（保留派生秘钥）
                    reconnectMifareClassic(mifare)
                    if (!authenticateSectorWithRetry(mifare, sector, emptyList(), listOf(curKeyB))) {
                        return uiString(R.string.snapmaker_write_reset_failed_auth_format, sector)
                    }
                    val step1Trailer = ByteArray(16).apply {
                        System.arraycopy(curKeyA, 0, this, 0, 6)
                        this[6] = 0xFF.toByte(); this[7] = 0x07.toByte()
                        this[8] = 0x80.toByte(); this[9] = 0x69.toByte()
                        System.arraycopy(curKeyB, 0, this, 10, 6)
                    }
                    if (!writeBlockWithRetry(mifare, trailerBlock, step1Trailer)) {
                        return uiString(R.string.snapmaker_write_reset_failed_perms_format, sector)
                    }
                    Thread.sleep(15)

                    // 步骤2：权限位已是 FF078069，将 KeyA/B 改为 FF
                    if (!authenticateSectorWithRetry(mifare, sector, listOf(curKeyA, ffKey), listOf(curKeyB, ffKey))) {
                        return uiString(R.string.snapmaker_write_reset_failed_step2_format, sector)
                    }
                    if (!writeBlockWithRetry(mifare, trailerBlock, fullFfTrailer)) {
                        return uiString(R.string.snapmaker_write_reset_failed_ff_format, sector)
                    }
                    Thread.sleep(15)

                    // 验证 FF 秘钥可用
                    if (!authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))) {
                        return uiString(R.string.snapmaker_write_reset_verify_failed_format, sector)
                    }
                }
                onStatusUpdate?.invoke(uiString(R.string.snapmaker_write_reset_done))
                Thread.sleep(100)
            }

            // Phase 2: 使用 FF 秘钥写入源数据
            for (sector in 0 until targetSectorCount) {
                reconnectMifareClassic(mifare)
                val trailerData = sourceBlocks.getOrNull(sector * 4 + 3)
                val sourceKeyA = trailerData?.takeIf { it.size == 16 }?.copyOfRange(0, 6)
                val sourceKeyB = trailerData?.takeIf { it.size == 16 }?.copyOfRange(10, 16)
                val authenticated = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(ffKey, sourceKeyA),
                    keysB = listOf(ffKey, sourceKeyB)
                )
                if (!authenticated) return uiString(R.string.snapmaker_write_failed_sector_auth_format, sector)

                onStatusUpdate?.invoke(uiString(R.string.snapmaker_write_sector_format, sector + 1, targetSectorCount))
                val startBlock = mifare.sectorToBlock(sector)
                for (offset in 0 until 4) {
                    val blockIndex = startBlock + offset
                    val targetData = sourceBlocks.getOrNull(blockIndex)
                        ?: return uiString(R.string.snapmaker_write_failed_block_missing_format, blockIndex)
                    if (targetData.size != 16) return uiString(R.string.snapmaker_write_failed_block_size_format, blockIndex)
                    if (!writeBlockWithRetry(mifare, blockIndex, targetData)) {
                        return uiString(R.string.snapmaker_write_failed_block_error_format, blockIndex)
                    }
                    Thread.sleep(20)
                }
            }
            uiString(R.string.snapmaker_write_success)
        } catch (e: Exception) {
            uiString(R.string.snapmaker_write_failed_format, e.message.orEmpty())
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    /**
     * 保存全部扇区数据到文件
     */
    private fun saveAllSectorsData(uidHex: String, rawBlocks: List<ByteArray?>, sectorKeys: List<Pair<ByteArray?, ByteArray?>>) {
        try {
            val rfidFilesDir = resolveSelfRfidDirectory()
            if (rfidFilesDir == null) {
                val message = getString(R.string.error_save_sector_no_dir)
                logDebug(message)
                LogCollector.append(this, "E", message)
                return
            }

            val outputFile = File(rfidFilesDir, "${uidHex}.txt")
            val accessBitsHex = "87878769"

            // 仅输出原始16进制文本：
            // 1. 每行一个区块（共64行）
            // 2. 无任何结构化说明文字
            // 3. 每个扇区尾块（sector*4+3）写入 KeyA + 87878769 + KeyB
            outputFile.bufferedWriter().use { writer ->
                for (sector in 0 until 16) {
                    for (block in 0 until 4) {
                        val blockIndex = sector * 4 + block
                        val lineHex = if (block == 3 && sector < sectorKeys.size) {
                            val keyAHex = sectorKeys[sector].first?.toHex()
                            val keyBHex = sectorKeys[sector].second?.toHex()
                            if (!keyAHex.isNullOrBlank() && !keyBHex.isNullOrBlank()) {
                                keyAHex + accessBitsHex + keyBHex
                            } else {
                                rawBlocks.getOrNull(blockIndex)?.toHex().orEmpty()
                            }
                        } else {
                            rawBlocks.getOrNull(blockIndex)?.toHex().orEmpty()
                        }
                        writer.write(lineHex)
                        writer.newLine()
                    }
                }
            }

            logDebug("全部扇区数据已保存到: ${outputFile.absolutePath}")
            LogCollector.append(this, "I", "全部扇区数据已保存到: ${outputFile.absolutePath}")
            refreshSelfTagCount()
        } catch (e: Exception) {
            logDebug("保存扇区数据失败: ${e.message}\n${Log.getStackTraceString(e)}")
            LogCollector.append(this, "E", "保存扇区数据失败: ${e.message}")
        }
    }

    private fun enqueueNdefWriteTask(request: NdefWriteRequest): String {
        if (pendingWriteItem != null || pendingVerifyItem != null || pendingClearFuid || pendingCuidTest || pendingNdefWriteRequest != null) {
            return uiString(R.string.write_finish_current_task_first)
        }

        val validationError = validateNdefWriteRequest(request)
        if (validationError != null) {
            writeToolStatusMessage = validationError
            return validationError
        }

        pendingNdefWriteRequest = request
        writeToolStatusMessage = uiString(R.string.write_ndef_ready)
        return writeToolStatusMessage
    }

    private fun validateNdefWriteRequest(request: NdefWriteRequest): String? {
        return when (request.type) {
            NdefWriteType.TEXT -> {
                if (request.textContent.isBlank()) getString(R.string.ndef_validate_text_empty) else null
            }
            NdefWriteType.URL -> {
                if (request.url.isBlank()) getString(R.string.ndef_validate_url_empty) else null
            }
            NdefWriteType.PHONE -> {
                if (request.phone.isBlank()) getString(R.string.ndef_validate_phone_empty) else null
            }
            NdefWriteType.WIFI -> {
                when {
                    request.wifiSsid.isBlank() -> getString(R.string.ndef_validate_wifi_ssid_empty)
                    request.wifiSecurity.isBlank() -> getString(R.string.ndef_validate_wifi_security_empty)
                    else -> null
                }
            }
        }
    }

    private fun enqueueClearFuidTask(): String {
        if (pendingWriteItem != null || pendingVerifyItem != null || pendingCuidTest || pendingNdefWriteRequest != null) {
            return uiString(R.string.misc_finish_current_write_verify)
        }
        resetDebugInfoDialog(getString(R.string.format_tag_debug_title))
        appendDebugInfoDialog(getString(R.string.format_tag_wait_state))
        pendingClearFuid = true
        miscStatusMessage = uiString(R.string.misc_format_ready)
        return miscStatusMessage
    }

    private fun enqueueCuidTestTask(): String {
        if (pendingWriteItem != null || pendingVerifyItem != null || pendingClearFuid || pendingNdefWriteRequest != null) {
            return uiString(R.string.misc_finish_current_write_verify)
        }
        pendingCuidTest = true
        miscStatusMessage = uiString(R.string.misc_cuid_test_ready)
        return miscStatusMessage
    }

    private fun testCuidCard(
        tag: Tag,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String {
        val mifare = MifareClassic.get(tag)
            ?: return uiString(R.string.misc_cuid_test_failed, getString(R.string.cuid_detail_no_mifare))
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        return try {
            mifare.connect()
            onStatusUpdate?.invoke(uiString(R.string.misc_cuid_detecting))

            // Step 1: Authenticate sector 0 with FF key
            val authOk = authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = 0,
                keysA = listOf(ffKey),
                keysB = listOf(ffKey)
            )
            if (!authOk) {
                return uiString(R.string.misc_cuid_format_first)
            }

            // Step 2: Read block 0 (save original)
            val originalBlock0 = readBlockWithRetry(mifare, 0)
                ?: return uiString(R.string.misc_cuid_test_failed, getString(R.string.cuid_detail_read_block0_failed))

            // Step 3: Read sector 0 trailer (block 3, save original)
            val trailerBlock = mifare.sectorToBlock(0) + 3
            val originalTrailer = readBlockWithRetry(mifare, trailerBlock)
                ?: return uiString(R.string.misc_cuid_test_failed, getString(R.string.cuid_detail_read_perms_failed))

            // Step 4: Modify sector 0 access bits to 878787 — must use Key B to write trailer
            val newTrailer = ByteArray(16).apply {
                for (i in 0..5) this[i] = 0xFF.toByte()       // KeyA: FF*6
                this[6] = 0x87.toByte()
                this[7] = 0x87.toByte()
                this[8] = 0x87.toByte()
                this[9] = originalTrailer[9]                   // preserve user data byte
                for (i in 10..15) this[i] = 0xFF.toByte()     // KeyB: FF*6
            }
            val authKeyBStep4 = authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = 0,
                keysA = emptyList(),
                keysB = listOf(ffKey)
            )
            if (!authKeyBStep4 || !writeBlockWithRetry(mifare, trailerBlock, newTrailer)) {
                return uiString(R.string.misc_cuid_test_failed, getString(R.string.cuid_detail_modify_perms_failed))
            }

            // Step 5: Re-authenticate after trailer change
            val reAuthOk = authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = 0,
                keysA = listOf(ffKey),
                keysB = listOf(ffKey)
            )
            if (!reAuthOk) {
                return uiString(R.string.misc_cuid_test_failed, getString(R.string.cuid_detail_auth_after_modify_failed))
            }

            // Step 6: Try to write block 0 with test data using KeyA FF
            val testData = hexToBytes("11223344440804006263646566676869")
            val writeOk = writeBlockWithRetry(mifare, 0, testData)

            // Step 7: Restore block 0 if it was modified
            if (writeOk) {
                authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = 0,
                    keysA = listOf(ffKey),
                    keysB = listOf(ffKey)
                )
                writeBlockWithRetry(mifare, 0, originalBlock0)
            }

            // Step 8: Restore sector 0 trailer to default FF078069 — must use Key B to write trailer
            val defaultTrailer = ByteArray(16).apply {
                for (i in 0..5) this[i] = 0xFF.toByte()    // KeyA: FF*6
                val access = hexToBytes("FF078069")
                System.arraycopy(access, 0, this, 6, 4)    // Access: FF 07 80 69
                for (i in 10..15) this[i] = 0xFF.toByte()  // KeyB: FF*6
            }
            authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = 0,
                keysA = emptyList(),
                keysB = listOf(ffKey)
            )
            writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)

            if (writeOk) uiString(R.string.misc_cuid_not_available)
            else uiString(R.string.misc_cuid_available)
        } catch (e: Exception) {
            uiString(R.string.misc_cuid_test_failed, e.message.orEmpty())
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun resolveSelfRfidDirectory(): File? {
        val deviceIdSuffix = getDeviceIdSuffix()
        val relativePath = "rfid_files/self_$deviceIdSuffix"
        val externalDir = getExternalFilesDir(null)
        val candidates = buildList {
            if (externalDir != null) add(File(externalDir, relativePath))
            add(File(filesDir, relativePath))
        }
        candidates.forEach { dir ->
            logDebug("尝试创建 self 目录: ${dir.absolutePath}")
            if (ensureDirectoryWritable(dir)) {
                logDebug("self 目录可用: ${dir.absolutePath}")
                return dir
            }
            logDebug("self 目录不可用: ${dir.absolutePath}")
        }
        return null
    }

    private fun countSelfTagFiles(): Int {
        val dir = resolveSelfRfidDirectory() ?: return 0
        return dir.walkTopDown()
            .count { it.isFile && it.extension.equals("txt", ignoreCase = true) }
    }

    private fun refreshSelfTagCount() {
        selfTagCount = countSelfTagFiles()
    }

    private fun clearSelfTagFiles(): String {
        val dir = resolveSelfRfidDirectory() ?: return uiString(R.string.self_tags_not_found)
        return try {
            var deleted = 0
            dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    if (file.delete()) {
                        deleted++
                    }
                }
            refreshSelfTagCount()
            uiString(R.string.self_tags_cleared_format, deleted)
        } catch (e: Exception) {
            logDebug("清空自有标签失败: ${e.message}")
            uiString(R.string.self_tags_clear_failed_format, e.message.orEmpty())
        }
    }

    private fun clearShareTagFiles(): String {
        // 同时清空拓竹和快造 DB 表及本地文件
        var dbDeleted = 0
        var snapmakerDbDeleted = 0
        filamentDbHelper?.writableDatabase?.let { db ->
            dbDeleted = filamentDbHelper!!.clearShareTagsTable(db)
            snapmakerDbDeleted = filamentDbHelper!!.clearSnapmakerShareTagsTable(db)
            // Reset migration flag so bundled tags are re-seeded on next refresh
            filamentDbHelper!!.deleteMetaValue(db, "share_disk_migration_v1")
        }
        shareTagItems = emptyList()
        snapmakerShareTagItems = emptyList()
        val externalDir = getExternalFilesDir(null) ?: filesDir
        val shareDir = File(externalDir, "rfid_files/share")
        if (!shareDir.exists()) return uiString(R.string.share_db_cleared_format, dbDeleted + snapmakerDbDeleted)
        return try {
            shareDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file -> file.delete() }
            uiString(R.string.share_db_cleared_format, dbDeleted + snapmakerDbDeleted)
        } catch (e: Exception) {
            logDebug("清空标签数据库失败: ${e.message}")
            uiString(R.string.share_db_clear_failed_format, e.message.orEmpty())
        }
    }

    private fun ensureDirectoryWritable(dir: File): Boolean {
        return try {
            if (dir.exists()) {
                if (!dir.isDirectory) {
                    logDebug("路径存在但不是目录: ${dir.absolutePath}")
                    return false
                }
            } else {
                val created = dir.mkdirs()
                if (!created && !dir.exists()) {
                    logDebug("创建目录失败: ${dir.absolutePath}")
                    return false
                }
            }
            if (!dir.canWrite()) {
                logDebug("目录不可写: ${dir.absolutePath}")
                return false
            }
            true
        } catch (e: Exception) {
            logDebug("目录检查失败: ${dir.absolutePath}, err=${e.message}")
            false
        }
    }

    /**
     * 保存秘钥到文件：
     * - 路径：rfid_files/keys
     * - 命名：UID.txt
     * - 格式：每行一个秘钥（按扇区顺序：KeyA、KeyB）
     */
    private fun saveSectorKeysToFile(
        uidHex: String,
        sectorKeys: List<Pair<ByteArray?, ByteArray?>>
    ) {
        try {
            val externalDir = getExternalFilesDir(null)
            if (externalDir == null) {
                logDebug("无法访问存储目录，秘钥文件未保存")
                return
            }
            val keysDir = File(externalDir, "rfid_files/keys")
            if (!keysDir.exists()) {
                keysDir.mkdirs()
            }
            val outputFile = File(keysDir, "${uidHex}.txt")
            outputFile.bufferedWriter().use { writer ->
                for (sector in 0 until WRITE_SECTOR_COUNT) {
                    val keyAHex = sectorKeys.getOrNull(sector)?.first?.toHex().orEmpty()
                    val keyBHex = sectorKeys.getOrNull(sector)?.second?.toHex().orEmpty()
                    writer.write(keyAHex)
                    writer.newLine()
                    writer.write(keyBHex)
                    writer.newLine()
                }
            }
            logDebug("秘钥已保存到: ${outputFile.absolutePath}")
            LogCollector.append(this, "I", "秘钥已保存到: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            logDebug("保存秘钥失败: ${e.message}")
            LogCollector.append(this, "E", "保存秘钥失败: ${e.message}")
        }
    }

    /**
     * 获取用于文件夹后缀的设备唯一ID（优先 ANDROID_ID）。
     * 仅用于本地目录命名，做最小化清洗避免路径非法字符。
     */
    private fun getDeviceIdSuffix(): String {
        val rawId = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.getInstallId(this)

        return rawId
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_-]"), "_")
            .take(32)
            .ifBlank { "unknown" }
    }

    private fun ensureShareDirectory() {
        val externalDir = getExternalFilesDir(null) ?: return
        val shareDir = File(externalDir, "rfid_files/share")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }
    }

    /**
     * 首次安装后自动把 assets/rfid_data.zip 解压到 rfid_files/share。
     * 已有 txt 数据或已写入标记时不会重复解压，避免覆盖用户内容。
     */
    private fun ensureBundledShareDataExtracted() {
        val externalDir = getExternalFilesDir(null) ?: return
        val shareDir = File(externalDir, "rfid_files/share")
        if (!shareDir.exists()) {
            shareDir.mkdirs()
        }

        val markerFile = File(shareDir, SHARE_EXTRACT_MARKER_FILE)
        val hasTxtFiles = shareDir.walkTopDown().any { file ->
            file.isFile && file.extension.equals("txt", ignoreCase = true)
        }
        if (markerFile.exists() || hasTxtFiles) {
            return
        }

        try {
            assets.open(SHARE_BUNDLE_ZIP_NAME).use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        unzipEntryToDir(zip, entry, shareDir)
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            markerFile.writeText(
                "extracted_at=${System.currentTimeMillis()}",
                Charsets.UTF_8
            )
            logDebug("基础共享数据已解压到: ${shareDir.absolutePath}")
        } catch (e: Exception) {
            // 允许 assets 中不存在该 zip，不阻塞应用启动。
            logDebug("基础共享数据解压跳过/失败: ${e.message}")
        }
    }

    private fun unzipEntryToDir(zip: ZipInputStream, entry: ZipEntry, targetDir: File) {
        val outFile = File(targetDir, entry.name)
        val targetPath = targetDir.canonicalPath
        val outPath = outFile.canonicalPath
        if (!outPath.startsWith("$targetPath${File.separator}") && outPath != targetPath) {
            throw IOException("非法压缩路径: ${entry.name}")
        }
        if (entry.isDirectory) {
            outFile.mkdirs()
            return
        }
        outFile.parentFile?.mkdirs()
        outFile.outputStream().use { output ->
            zip.copyTo(output)
        }
    }

    private fun loadShareTagItems(): List<ShareTagItem> {
        val dbHelper = filamentDbHelper ?: return emptyList()
        val db = dbHelper.writableDatabase
        val rows = dbHelper.getAllShareTagRows(db)
        val result = ArrayList<ShareTagItem>(rows.size)
        for (row in rows) {
            val rawData = row.rawData ?: continue
            val colorValuesList = row.colorValues?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            // 若 DB 中尚无生产日期，尝试从 raw_data 提取并回填
            val productionDate = row.productionDate.orEmpty()
            result.add(
                ShareTagItem(
                    relativePath = row.fileUid.lowercase(Locale.US),
                    fileName = "${row.fileUid}.txt",
                    sourceUid = row.fileUid,
                    trayUid = row.trayUid.orEmpty(),
                    materialId = row.materialId.orEmpty(),
                    materialType = row.materialType.orEmpty(),
                    materialDetailedType = row.materialDetailedType.orEmpty(),
                    colorUid = row.colorUid.orEmpty(),
                    filaColorCode = row.filaColorCode.orEmpty(),
                    colorName = row.colorName.orEmpty(),
                    colorNameEn = row.colorNameEn.orEmpty(),
                    colorType = row.colorType.orEmpty(),
                    colorValues = colorValuesList,
                    rawData = rawData,
                    dbId = row.id,
                    copyCount = row.copyCount,
                    verified = row.verified,
                    productionDate = productionDate
                )
            )
        }
        return result
    }

    /**
     * 将磁盘上的 share 目录 txt 文件迁移到 DB（含 raw_data）。
     * 仅在首次升级/安装后运行一次，之后由 DB meta key 标记跳过。
     */
    private fun migrateDiskFilesToDb() {
        val dbHelper = filamentDbHelper ?: return
        val db = dbHelper.writableDatabase
        if (dbHelper.getMetaValue(db, "share_disk_migration_v1") != null) return

        val externalDir = getExternalFilesDir(null)
        val shareDir = externalDir?.let { File(it, "rfid_files/share") }
        if (shareDir == null || !shareDir.exists()) {
            dbHelper.setMetaValue(db, "share_disk_migration_v1", "done")
            return
        }

        val existingDbUids = dbHelper.getAllShareTagRows(db)
            .map { it.fileUid.uppercase(Locale.US) }
            .toSet()

        shareDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("txt", ignoreCase = true) }
            .forEach { file ->
                val fileUid = file.nameWithoutExtension.uppercase(Locale.US)
                try {
                    val content = file.readText(Charsets.UTF_8)
                    val rawBlocks = parseHexTagFileStrict(content) ?: return@forEach
                    val normalized = content.trim().lines()
                        .map { it.trim() }.filter { it.isNotBlank() }.take(64).joinToString("\n")
                    if (fileUid in existingDbUids) {
                        // Update raw_data for existing DB rows that lack it
                        dbHelper.updateShareTagRawData(db, fileUid, normalized)
                    } else {
                        val preview = NfcTagProcessor.parseForPreview(rawBlocks, filamentDbHelper) {}
                        val trayUid = preview.trayUidHex.trim()
                        dbHelper.insertShareTag(
                            db,
                            fileUid = fileUid,
                            trayUid = trayUid.ifBlank { null },
                            materialId = preview.materialId.ifBlank { null },
                            materialType = preview.displayData.type.ifBlank { null },
                            materialDetailedType = preview.displayData.detailedType.ifBlank { null },
                            colorUid = preview.displayData.colorCode.ifBlank { null },
                            filaColorCode = preview.displayData.filaColorCode.ifBlank { null },
                            colorName = preview.displayData.colorName.ifBlank { null },
                            colorNameEn = preview.displayData.colorNameEn.ifBlank { null },
                            colorType = preview.displayData.colorType.ifBlank { null },
                            colorValues = preview.displayData.colorValues.joinToString(",").ifBlank { null },
                            rawData = normalized
                        )
                    }
                } catch (e: Exception) {
                    logDebug("迁移标签文件失败 ${file.name}: ${e.message}")
                }
            }
        dbHelper.setMetaValue(db, "share_disk_migration_v1", "done")
    }

    private fun deleteShareTagItem(item: ShareTagItem): String {
        return try {
            filamentDbHelper?.writableDatabase?.let { db ->
                filamentDbHelper!!.deleteShareTagByFileUid(db, item.sourceUid.uppercase(Locale.US))
            }
            // 同时尝试删除磁盘文件（兼容旧版迁移数据）
            try {
                val externalDir = getExternalFilesDir(null)
                val shareDir = externalDir?.let { File(it, "rfid_files/share") }
                if (shareDir != null) {
                    shareDir.walkTopDown()
                        .filter { it.isFile && it.nameWithoutExtension.equals(item.sourceUid, ignoreCase = true) }
                        .forEach { it.delete() }
                }
            } catch (_: Exception) { }
            shareTagItems = shareTagItems.filterNot { it.relativePath == item.relativePath }
            uiString(R.string.share_tag_delete_success_format, item.sourceUid)
        } catch (e: Exception) {
            logDebug("删除共享标签失败: ${e.message}")
            uiString(R.string.share_tag_delete_failed_format, e.message.orEmpty())
        }
    }

    private fun preloadShareTagItemsInBackground(): Boolean {
        return loadShareTagItemsInBackground(showStatus = false)
    }

    private fun refreshShareTagItemsAsync(): Boolean {
        return loadShareTagItemsInBackground(showStatus = true)
    }

    private fun loadShareTagItemsInBackground(showStatus: Boolean): Boolean {
        if (showStatus) {
            shareLoadStatusRequested.set(true)
            shareLoading = true
            shareRefreshStatusClearJob?.cancel()
            shareRefreshStatusMessage = uiString(R.string.share_refreshing)
        }
        if (!shareLoadingInProgress.compareAndSet(false, true)) {
            return false
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                ensureBundledShareDataExtracted()
                migrateDiskFilesToDb()
                val loadedItems = loadShareTagItems()
                withContext(Dispatchers.Main) {
                    shareTagItems = loadedItems
                    val statusRequested = shareLoadStatusRequested.getAndSet(false)
                    val shouldShowStatus = showStatus || statusRequested
                    if (shouldShowStatus) {
                        shareRefreshStatusMessage = uiString(R.string.share_refreshed_format, loadedItems.size)
                        scheduleClearShareRefreshStatusMessage()
                    }
                    shareLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val statusRequested = shareLoadStatusRequested.getAndSet(false)
                    val shouldShowStatus = showStatus || statusRequested
                    if (shouldShowStatus) {
                        shareRefreshStatusMessage = uiString(R.string.share_refresh_failed_format, e.message.orEmpty())
                        scheduleClearShareRefreshStatusMessage()
                    }
                    shareLoading = false
                }
            } finally {
                shareLoadingInProgress.set(false)
                runOnUiThread {
                    shareLoading = false
                }
            }
        }
        return true
    }

    private fun syncAnomalyUidsAsync() {
        val dbHelper = filamentDbHelper ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 先加载本地缓存，网络同步成功后再覆盖
                val cached = dbHelper.getAnomalyUids(dbHelper.readableDatabase)
                if (cached.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        anomalyUids = cached
                    }
                }
                val uids = com.m0h31h31.bamburfidreader.utils.AnalyticsReporter.fetchAnomalyUids(applicationContext)
                if (uids != null) {
                    dbHelper.saveAnomalyUids(dbHelper.writableDatabase, uids)
                    withContext(Dispatchers.Main) {
                        anomalyUids = uids
                    }
                }
            } catch (_: Exception) {
                // 静默失败，不影响主流程
            }
        }
    }

    // ── 在线更新 ──────────────────────────────────────────────────────────────

    private fun checkForUpdateAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val info = AnalyticsReporter.checkForUpdate(applicationContext) ?: return@launch
                withContext(Dispatchers.Main) {
                    pendingUpdateInfo = info
                    startUpdate(info)   // 检测到新版本立即自动下载
                }
            } catch (_: Exception) {}
        }
    }

    fun startUpdate(info: UpdateInfo) {
        if (!packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(android.net.Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }
        startApkDownload(info.downloadUrl)
    }

    /** 内置下载器:HTTP 拉取 APK 到应用私有目录,带进度,完成后调用安装(不依赖系统 DownloadManager)。 */
    private fun startApkDownload(downloadUrl: String) {
        if (isDownloadingUpdate) return
        isDownloadingUpdate = true
        updateDownloadProgress = 0
        Toast.makeText(this, getString(R.string.update_downloading), Toast.LENGTH_SHORT).show()
        logDebug("Update download start url=$downloadUrl")
        lifecycleScope.launch(Dispatchers.IO) {
            val dest = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BambuRfidReader_update.apk")
            val ok = runCatching { downloadApkTo(downloadUrl, dest) }.getOrElse {
                logDebug("Update download failed: ${it.message}")
                false
            }
            withContext(Dispatchers.Main) {
                isDownloadingUpdate = false
                if (ok && dest.exists() && dest.length() > 0) {
                    installDownloadedApk(dest)
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.update_download_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** 下载到 dest,手动跟随重定向(兼容跨协议跳转);更新进度。返回是否成功。 */
    private suspend fun downloadApkTo(urlStr: String, dest: File): Boolean {
        if (dest.exists()) dest.delete()
        dest.parentFile?.mkdirs()
        var current = urlStr
        var redirects = 0
        while (redirects < 5) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = false
            }
            try {
                conn.connect()
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: return false
                    current = URL(URL(current), loc).toString()
                    redirects++
                    continue
                }
                if (code !in 200..299) return false
                val total = conn.contentLength.toLong()
                var downloaded = 0L
                var lastPct = -1
                conn.inputStream.use { input ->
                    dest.outputStream().use { out ->
                        val buf = ByteArray(8192)
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            out.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val pct = (downloaded * 100 / total).toInt()
                                if (pct != lastPct) {
                                    lastPct = pct
                                    withContext(Dispatchers.Main) { updateDownloadProgress = pct }
                                }
                            }
                        }
                    }
                }
                return true
            } finally {
                conn.disconnect()
            }
        }
        return false
    }

    private fun installDownloadedApk(apkFile: File) {
        if (!apkFile.exists()) { logDebug("APK not found: ${apkFile.absolutePath}"); return }
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.update_provider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            logDebug("Install intent failed: ${e.message}")
        }
    }

    private fun scheduleClearShareRefreshStatusMessage() {
        shareRefreshStatusClearJob?.cancel()
        shareRefreshStatusClearJob = lifecycleScope.launch(Dispatchers.Main) {
            delay(3000)
            shareRefreshStatusMessage = ""
        }
    }

    private fun isTrayUidExists(trayUid: String): Boolean {
        val db = filamentDbHelper?.readableDatabase ?: return false
        val cursor = db.query(
            TRAY_UID_TABLE,
            arrayOf("tray_uid"),
            "tray_uid = ?",
            arrayOf(trayUid),
            null,
            null,
            null,
            "1"
        )
        cursor.use {
            return it.moveToFirst()
        }
    }

    /**
     * 从 block12 提取耗材生产日期，返回 yy-mm-dd 格式，无法解析时返回 null。
     * block12 原始内容为 ASCII 字符串，格式 YYYY_MM_DD_HH_MM。
     */
    private fun extractProductionDate(rawBlocks: List<ByteArray?>): String? {
        val block = rawBlocks.getOrNull(12) ?: return null
        if (block.size < 3) return null
        val trimmed = block.dropLastWhile { it == 0.toByte() || it == 0x20.toByte() }.toByteArray()
        if (trimmed.isEmpty()) return null
        if (!trimmed.all { it in 0x20..0x7E }) return null
        val raw = String(trimmed, Charsets.US_ASCII).trim()
        val parts = raw.split('_')
        if (parts.size < 3) return null
        val year = parts[0]; val month = parts[1]; val day = parts[2]
        if (!listOf(year, month, day).all { s -> s.all(Char::isDigit) }) return null
        if (year.length < 2) return null
        return "${year.takeLast(2)}-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }

    /** 严格校验：必须恰好 64 行，每行恰好 32 个十六进制字符（空格会被跳过计数外）。 */
    /**
     * 校验标签合法性：
     * 1. 所有扇区尾块权限位 + 用户数据字节必须为 87878769。
     * 2. 使用 block0 前 4 字节作为 UID 派生密钥，校验每个扇区的 KeyA / KeyB。
     * 尾块布局：KeyA(6B) + AccessBits(3B) + UserByte(1B) + KeyB(6B)
     */
    private fun isValidBambuTag(rawBlocks: List<ByteArray?>): Boolean {
        // 校验 1：权限位和用户数据
        for (sector in 0 until 16) {
            val trailer = rawBlocks.getOrNull(sector * 4 + 3) ?: return false
            if (trailer.size < 16) return false
            if (trailer[6] != 0x87.toByte() ||
                trailer[7] != 0x87.toByte() ||
                trailer[8] != 0x87.toByte() ||
                trailer[9] != 0x69.toByte()
            ) return false
        }

        // 校验 2：使用 UID 派生密钥，逐扇区比对 KeyA 和 KeyB
        val block0 = rawBlocks.getOrNull(0) ?: return false
        if (block0.size < 4) return false
        val uid = block0.copyOfRange(0, 4)
        val expectedKeys = deriveBambuKeys(uid)
        for (sector in 0 until 16) {
            val trailer = rawBlocks[sector * 4 + 3]!!
            val (expectedKeyA, expectedKeyB) = expectedKeys.getOrNull(sector) ?: return false
            val actualKeyA = trailer.copyOfRange(0, 6)
            val actualKeyB = trailer.copyOfRange(10, 16)
            if (!actualKeyA.contentEquals(expectedKeyA)) return false
            if (!actualKeyB.contentEquals(expectedKeyB)) return false
        }
        return true
    }

    /**
     * 导入用宽松校验：只校验权限位 87878769，不校验密钥（因为第三方工具读取的真实标签
     * 会因 Mifare Classic 硬件屏蔽导致 KeyA 全零，无法通过密钥派生校验）。
     */
    private fun isImportableBambuTag(rawBlocks: List<ByteArray?>): Boolean {
        for (sector in 0 until 16) {
            val trailer = rawBlocks.getOrNull(sector * 4 + 3) ?: return false
            if (trailer.size < 16) return false
            if (trailer[6] != 0x87.toByte() ||
                trailer[7] != 0x87.toByte() ||
                trailer[8] != 0x87.toByte() ||
                trailer[9] != 0x69.toByte()
            ) return false
        }
        return true
    }

    private fun parseHexTagFileStrict(content: String): List<ByteArray?>? {
        val lines = content.trim().lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.size != 64) return null
        val blocks = MutableList<ByteArray?>(64) { null }
        for ((i, line) in lines.withIndex()) {
            val hex = line.replace(" ", "").uppercase(Locale.US)
            if (hex.length != 32 || !hex.all { c -> c in '0'..'9' || c in 'A'..'F' }) return null
            blocks[i] = hexToBytes(hex)
        }
        return blocks
    }

    private fun parseHexDumpFile(file: File): List<ByteArray?>? {
        val lines = file.readLines(Charsets.UTF_8)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return null
        }
        val blocks = MutableList<ByteArray?>(64) { null }
        lines.take(64).forEachIndexed { index, line ->
            val hex = line.replace(" ", "").uppercase(Locale.US)
            if (hex.length == 32 && hex.all { it in '0'..'9' || it in 'A'..'F' }) {
                blocks[index] = hexToBytes(hex)
            }
        }
        return blocks
    }

    private fun resolveShareTagBlocks(item: ShareTagItem): List<ByteArray?>? {
        if (item.rawBlocks.isNotEmpty()) return item.rawBlocks
        return parseHexTagFileStrict(item.rawData)
    }

    private fun writeTagFromDump(tag: Tag, item: ShareTagItem, onStatusUpdate: ((String) -> Unit)? = null): String {
        val sourceBlocks = resolveShareTagBlocks(item)
            ?: return uiString(R.string.write_failed_format, uiString(R.string.tag_import_invalid_zip))
        return when (val result = BambuMifareOperator.run(
            tag = tag,
            config = nfcCompatibilityConfig,
            operation = BambuNfcOperation.WriteDumpWithFf(sourceBlocks),
            context = this,
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) },
            onStatus = { status -> onStatusUpdate?.invoke(status) }
        )) {
            is BambuNfcResult.Message -> result.message
            is BambuNfcResult.Failure -> result.message
            is BambuNfcResult.RawRead -> uiString(R.string.write_failed_format, uiString(R.string.bambu_nfc_unexpected_read_result))
        }
    }

    /**
     * CUID改写三阶段流程（集成校验，无需重复贴卡）：
     * Phase 1 — 用穷举密钥将所有扇区 Trailer 重置为全FF（FF*6 | FF078069 | FF*6），
     *           每扇区最多重试5次，写后立即用FF密钥回读权限位校验。
     * Phase 2 — 全部 Trailer 重置完成后，使用FF密钥逐扇区写入源数据，每扇区最多重试5次。
     * Phase 3 — 重连，使用源数据密钥全量回读校验，Trailer 仅比较访问位。
     */
    private fun writeCModifyTag(
        tag: Tag,
        item: ShareTagItem,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String {
        val sourceBlocks = resolveShareTagBlocks(item)
            ?: return uiString(R.string.cmodify_failed_format, uiString(R.string.tag_import_invalid_zip), uiString(R.string.cmodify_retry_hint))
        return when (val result = BambuMifareOperator.run(
            tag = tag,
            config = nfcCompatibilityConfig,
            operation = BambuNfcOperation.FormatThenWriteDump(sourceBlocks),
            context = this,
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) },
            onStatus = { status -> onStatusUpdate?.invoke(status) }
        )) {
            is BambuNfcResult.Message -> result.message
            is BambuNfcResult.Failure -> result.message
            is BambuNfcResult.RawRead -> uiString(R.string.cmodify_failed_format, uiString(R.string.bambu_nfc_unexpected_read_result), uiString(R.string.cmodify_retry_hint))
        }
    }

    private fun clearFuidAndResetTag(
        tag: Tag,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String {
        val brand = detectFormatTagBrand(tag) ?: return uiString(R.string.format_failed_sector0_all_failed)
        val detectedMessage = uiString(R.string.format_detected_brand_format, formatTagBrandLabel(brand))
        logDebug(detectedMessage)
        LogCollector.append(applicationContext, "I", detectedMessage)
        onStatusUpdate?.invoke(detectedMessage)
        if (brand != FormatTagBrand.BAMBU) {
            return formatNonBambuTagToDefaultFf(tag, brand, onStatusUpdate)
        }
        return when (val result = BambuMifareOperator.run(
            tag = tag,
            config = nfcCompatibilityConfig,
            operation = BambuNfcOperation.FormatToDefaultFf,
            context = this,
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) },
            onStatus = { status -> onStatusUpdate?.invoke(status) }
        )) {
            is BambuNfcResult.Message -> result.message
            is BambuNfcResult.Failure -> result.message
            is BambuNfcResult.RawRead -> uiString(R.string.format_failed_format, uiString(R.string.bambu_nfc_unexpected_read_result))
        }
    }

    private data class FormatSectorKeys(
        val keysA: List<ByteArray?>,
        val keysB: List<ByteArray?>
    )

    private fun detectFormatTagBrand(tag: Tag): FormatTagBrand? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            MifareClassicSession.applyTimeout(mifare, nfcCompatibilityConfig.mifareTimeoutMs)
            if (nfcCompatibilityConfig.postConnectDelayMs > 0) {
                Thread.sleep(nfcCompatibilityConfig.postConnectDelayMs)
            }
            val uid = tag.id ?: return null
            val ffKey = ByteArray(6) { 0xFF.toByte() }
            val bambuKeys = runCatching { deriveBambuKeys(uid) }.getOrNull().orEmpty()
            val snapmakerKeys = runCatching { deriveSnapmakerKeys(uid) }.getOrElse { Pair(emptyList(), emptyList()) }
            val crealityKey = runCatching { deriveCrealityKeyA(uid) }.getOrNull()

            val bambuAuth = listOf(0, 1).any { sector ->
                val keyB = bambuKeys.getOrNull(sector)?.second ?: return@any false
                reconnectMifareClassic(mifare)
                authenticateSectorWithRetry(mifare, sector, emptyList(), listOf(keyB))
            }

            val snapmakerAuth = listOf(0, 1).any { sector ->
                val keyB = snapmakerKeys.second.getOrNull(sector) ?: return@any false
                reconnectMifareClassic(mifare)
                authenticateSectorWithRetry(mifare, sector, emptyList(), listOf(keyB))
            }

            val crealitySector = if (mifare.sectorCount > 1) 1 else 0
            val crealityAuth = crealityKey?.let { key ->
                reconnectMifareClassic(mifare)
                authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = crealitySector,
                    keysA = listOf(key),
                    keysB = listOf(key, ffKey)
                )
            } ?: false

            val ffAuth = run {
                reconnectMifareClassic(mifare)
                authenticateSectorWithRetry(mifare, 0, listOf(ffKey), listOf(ffKey))
            }

            val result = FormatTagBrandDetector.choose(
                bambuAuth = bambuAuth,
                snapmakerAuth = snapmakerAuth,
                crealityAuth = crealityAuth,
                ffAuth = ffAuth
            )
            logFormatEvent("Format brand detect UID=${uid.toHex()} bambu=$bambuAuth snapmaker=$snapmakerAuth creality=$crealityAuth ff=$ffAuth result=$result")
            result
        } catch (e: Exception) {
            LogCollector.append(applicationContext, "E", "Format brand detect failed: ${e.javaClass.simpleName}: ${e.message}")
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun formatTagBrandLabel(brand: FormatTagBrand): String {
        return when (brand) {
            FormatTagBrand.BAMBU -> uiString(R.string.format_brand_bambu)
            FormatTagBrand.SNAPMAKER -> uiString(R.string.format_brand_snapmaker)
            FormatTagBrand.CREALITY -> uiString(R.string.format_brand_creality)
            FormatTagBrand.DEFAULT_FF -> uiString(R.string.format_brand_ff)
        }
    }

    private fun formatSectorKeys(
        brand: FormatTagBrand,
        uid: ByteArray,
        sector: Int,
        ffKey: ByteArray,
        snapmakerKeys: Pair<List<ByteArray>, List<ByteArray>>,
        crealityKey: ByteArray?
    ): FormatSectorKeys {
        return when (brand) {
            FormatTagBrand.SNAPMAKER -> FormatSectorKeys(
                keysA = listOf(snapmakerKeys.first.getOrNull(sector), ffKey),
                keysB = listOf(snapmakerKeys.second.getOrNull(sector), ffKey)
            )
            FormatTagBrand.CREALITY -> FormatSectorKeys(
                keysA = listOf(crealityKey, ffKey),
                keysB = listOf(crealityKey, ffKey)
            )
            FormatTagBrand.DEFAULT_FF -> FormatSectorKeys(
                keysA = listOf(ffKey),
                keysB = listOf(ffKey)
            )
            FormatTagBrand.BAMBU -> {
                val bambuKeys = deriveBambuKeys(uid).getOrNull(sector)
                FormatSectorKeys(
                    keysA = listOf(bambuKeys?.first, ffKey),
                    keysB = listOf(bambuKeys?.second, ffKey)
                )
            }
        }
    }

    private fun formatNonBambuTagToDefaultFf(
        tag: Tag,
        brand: FormatTagBrand,
        onStatusUpdate: ((String) -> Unit)? = null
    ): String {
        val mifare = MifareClassic.get(tag) ?: return uiString(R.string.format_failed_no_mifare)
        val uid = tag.id ?: return uiString(R.string.format_failed_no_uid)
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val zeroBlock = ByteArray(16)
        val defaultTrailer = ByteArray(16).apply {
            for (i in 0..5) this[i] = 0xFF.toByte()
            this[6] = 0xFF.toByte()
            this[7] = 0x07.toByte()
            this[8] = 0x80.toByte()
            this[9] = 0x69.toByte()
            for (i in 10..15) this[i] = 0xFF.toByte()
        }
        val snapmakerKeys = runCatching { deriveSnapmakerKeys(uid) }.getOrElse { Pair(emptyList(), emptyList()) }
        val crealityKey = runCatching { deriveCrealityKeyA(uid) }.getOrNull()

        return try {
            mifare.connect()
            MifareClassicSession.applyTimeout(mifare, nfcCompatibilityConfig.mifareTimeoutMs)
            if (nfcCompatibilityConfig.postConnectDelayMs > 0) {
                Thread.sleep(nfcCompatibilityConfig.postConnectDelayMs)
            }
            val targetSectorCount = minOf(16, mifare.sectorCount)
            if (targetSectorCount <= 0) {
                return uiString(R.string.format_failed_sector_count_format, mifare.sectorCount)
            }
            val originalBlock0 = run {
                val keys = formatSectorKeys(brand, uid, 0, ffKey, snapmakerKeys, crealityKey)
                if (!authenticateSectorWithRetry(mifare, 0, keys.keysA, keys.keysB)) {
                    return uiString(R.string.format_failed_sector0_all_failed)
                }
                readBlockWithRetry(mifare, 0) ?: return uiString(R.string.format_failed_read_block0)
            }

            for (sector in 0 until targetSectorCount) {
                onStatusUpdate?.invoke(uiString(R.string.bambu_nfc_format_trailer_status_format, sector + 1, targetSectorCount))
                val trailerBlock = mifare.sectorToBlock(sector) + mifare.getBlockCountInSector(sector) - 1
                if (!resetNonBambuTrailerToDefaultFf(
                        mifare = mifare,
                        brand = brand,
                        uid = uid,
                        sector = sector,
                        trailerBlock = trailerBlock,
                        defaultTrailer = defaultTrailer,
                        ffKey = ffKey,
                        snapmakerKeys = snapmakerKeys,
                        crealityKey = crealityKey
                    )
                ) {
                    return formatTrailerResetFailedMessage(brand, sector)
                }
                reconnectMifareClassic(mifare)
                if (!authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))) {
                    return uiString(R.string.format_failed_ff_auth_format, sector)
                }
            }

            for (sector in 0 until targetSectorCount) {
                onStatusUpdate?.invoke(uiString(R.string.bambu_nfc_format_clearing_status_format, sector + 1, targetSectorCount))
                reconnectMifareClassic(mifare)
                if (!authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))) {
                    return uiString(R.string.format_failed_ff_auth_format, sector)
                }
                val startBlock = mifare.sectorToBlock(sector)
                val blockCount = mifare.getBlockCountInSector(sector)
                for (offset in 0 until blockCount) {
                    val blockIndex = startBlock + offset
                    val isTrailer = offset == blockCount - 1
                    if (blockIndex == 0 || isTrailer) continue
                    if (!writeBlockWithRetry(mifare, blockIndex, zeroBlock)) {
                        return uiString(R.string.format_failed_block_write_format, blockIndex)
                    }
                }
            }

            for (sector in 0 until targetSectorCount) {
                reconnectMifareClassic(mifare)
                if (!authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))) {
                    return uiString(R.string.format_failed_ff_auth_format, sector)
                }
                val startBlock = mifare.sectorToBlock(sector)
                val blockCount = mifare.getBlockCountInSector(sector)
                for (offset in 0 until blockCount) {
                    val blockIndex = startBlock + offset
                    val isTrailer = offset == blockCount - 1
                    if (isTrailer) continue
                    val block = readBlockWithRetry(mifare, blockIndex)
                        ?: return uiString(R.string.bambu_nfc_format_verify_read_failed_format, blockIndex)
                    if (blockIndex == 0 && !block.contentEquals(originalBlock0)) {
                        return uiString(R.string.bambu_nfc_format_verify_block0_changed)
                    }
                    if (blockIndex != 0 && !block.all { it == 0.toByte() }) {
                        return uiString(R.string.bambu_nfc_format_verify_block_not_zero_format, blockIndex)
                    }
                }
            }
            uiString(R.string.format_success)
        } catch (e: Exception) {
            uiString(R.string.format_failed_format, e.message.orEmpty())
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun resetNonBambuTrailerToDefaultFf(
        mifare: MifareClassic,
        brand: FormatTagBrand,
        uid: ByteArray,
        sector: Int,
        trailerBlock: Int,
        defaultTrailer: ByteArray,
        ffKey: ByteArray,
        snapmakerKeys: Pair<List<ByteArray>, List<ByteArray>>,
        crealityKey: ByteArray?
    ): Boolean {
        logFormatEvent("Format trailer reset brand=$brand sector=$sector trailerBlock=$trailerBlock")
        return when (brand) {
            FormatTagBrand.SNAPMAKER -> {
                val keyA = snapmakerKeys.first.getOrNull(sector) ?: return false
                val keyB = snapmakerKeys.second.getOrNull(sector) ?: return false
                reconnectMifareClassic(mifare)
                val step1Auth = authenticateSectorWithRetry(mifare, sector, emptyList(), listOf(keyB))
                logFormatEvent("Snapmaker sector=$sector step1 derived KeyB auth=$step1Auth")
                if (!step1Auth) {
                    reconnectMifareClassic(mifare)
                    val ffAuth = authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))
                    logFormatEvent("Snapmaker sector=$sector FF fallback auth=$ffAuth")
                    if (!ffAuth) return false
                    val ffWrite = writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)
                    logFormatEvent("Snapmaker sector=$sector FF fallback trailer write=$ffWrite")
                    return ffWrite
                }
                val step1Trailer = ByteArray(16).apply {
                    System.arraycopy(keyA, 0, this, 0, 6)
                    this[6] = 0xFF.toByte()
                    this[7] = 0x07.toByte()
                    this[8] = 0x80.toByte()
                    this[9] = 0x69.toByte()
                    System.arraycopy(keyB, 0, this, 10, 6)
                }
                if (!writeBlockWithRetry(mifare, trailerBlock, step1Trailer)) {
                    logFormatEvent("Snapmaker sector=$sector step1 trailer write failed")
                    return false
                }
                if (nfcCompatibilityConfig.writeInterBlockDelayMs > 0) {
                    Thread.sleep(nfcCompatibilityConfig.writeInterBlockDelayMs)
                }
                reconnectMifareClassic(mifare)
                val step2Auth = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(keyA, ffKey),
                    keysB = listOf(keyB, ffKey)
                )
                logFormatEvent("Snapmaker sector=$sector step2 derived/FF auth=$step2Auth")
                if (!step2Auth) return false
                val step2Write = writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)
                logFormatEvent("Snapmaker sector=$sector step2 default trailer write=$step2Write")
                step2Write
            }

            FormatTagBrand.CREALITY -> {
                val key = crealityKey ?: return false
                reconnectMifareClassic(mifare)
                val derivedAuth = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(key),
                    keysB = listOf(key, ffKey)
                )
                logFormatEvent("Creality sector=$sector derived/FF auth=$derivedAuth")
                if (derivedAuth && writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)) {
                    logFormatEvent("Creality sector=$sector default trailer write=true")
                    return true
                }
                reconnectMifareClassic(mifare)
                val ffAuth = authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))
                logFormatEvent("Creality sector=$sector FF fallback auth=$ffAuth")
                if (!ffAuth) return false
                val ffWrite = writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)
                logFormatEvent("Creality sector=$sector FF fallback trailer write=$ffWrite")
                ffWrite
            }

            FormatTagBrand.DEFAULT_FF -> {
                reconnectMifareClassic(mifare)
                val ffAuth = authenticateSectorWithRetry(mifare, sector, listOf(ffKey), listOf(ffKey))
                logFormatEvent("FF sector=$sector auth=$ffAuth")
                if (!ffAuth) return false
                val write = writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)
                logFormatEvent("FF sector=$sector default trailer write=$write")
                write
            }

            FormatTagBrand.BAMBU -> {
                val keys = formatSectorKeys(brand, uid, sector, ffKey, snapmakerKeys, crealityKey)
                reconnectMifareClassic(mifare)
                val auth = authenticateSectorWithRetry(mifare, sector, keys.keysA, keys.keysB)
                logFormatEvent("Bambu fallback sector=$sector auth=$auth")
                if (!auth) return false
                val write = writeBlockWithRetry(mifare, trailerBlock, defaultTrailer)
                logFormatEvent("Bambu fallback sector=$sector default trailer write=$write")
                write
            }
        }
    }

    private fun logFormatEvent(message: String) {
        logDebug(message)
        LogCollector.append(applicationContext, "D", message)
    }

    private fun formatTrailerResetFailedMessage(brand: FormatTagBrand, sector: Int): String {
        return when (brand) {
            FormatTagBrand.SNAPMAKER -> uiString(R.string.format_failed_snapmaker_trailer_reset_format, sector)
            FormatTagBrand.CREALITY -> uiString(R.string.format_failed_creality_trailer_reset_format, sector)
            FormatTagBrand.BAMBU -> uiString(R.string.format_failed_bambu_trailer_reset_format, sector)
            FormatTagBrand.DEFAULT_FF -> uiString(R.string.format_failed_ff_auth_format, sector)
        }
    }

    private fun writeNdefDataAndVerify(tag: Tag, request: NdefWriteRequest): String {
        val message = try {
            buildNdefMessage(request)
        } catch (e: Exception) {
            return uiString(R.string.ndef_write_failed_generate_format, e.message.orEmpty())
        }
        val expectedBytes = message.toByteArray()
        val mifareClassic = MifareClassic.get(tag)

        return try {
            if (mifareClassic != null) {
                writeNdefByMifareClassicMappingAndVerify(mifareClassic, expectedBytes)
            } else {
                uiString(R.string.ndef_write_failed_no_mapping)
            }
        } catch (e: Exception) {
            uiString(R.string.ndef_write_failed_format, e.message.orEmpty())
        } finally {
            try {
                mifareClassic?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun writeNdefByMifareClassicMappingAndVerify(
        mifare: MifareClassic,
        ndefPayload: ByteArray
    ): String {
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        val tlv = buildNdefTlv(ndefPayload)

        return try {
            if (!mifare.isConnected) {
                mifare.connect()
            }
            if (mifare.sectorCount <= 1) {
                return uiString(R.string.ndef_write_m1_failed_sectors)
            }

            val dataBlocks = ArrayList<Int>()
            for (sector in 1 until mifare.sectorCount) {
                val authOk = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(ffKey),
                    keysB = listOf(ffKey)
                )
                if (!authOk) {
                    return uiString(R.string.ndef_write_m1_failed_auth_format, sector)
                }
                val startBlock = mifare.sectorToBlock(sector)
                val blockCount = mifare.getBlockCountInSector(sector)
                val trailerBlock = startBlock + blockCount - 1
                for (offset in 0 until blockCount) {
                    val blockIndex = startBlock + offset
                    if (blockIndex == trailerBlock) continue
                    dataBlocks.add(blockIndex)
                }
            }

            val capacity = dataBlocks.size * 16
            if (tlv.size > capacity) {
                return uiString(R.string.ndef_write_m1_failed_capacity_format, tlv.size, capacity)
            }

            val usedBlocks = ArrayList<Int>()
            val skippedBlocks = ArrayList<Int>()
            var writeOffset = 0
            for (blockIndex in dataBlocks) {
                if (writeOffset >= tlv.size) break
                val blockData = ByteArray(16) { 0x00.toByte() }
                val copyLen = minOf(16, tlv.size - writeOffset)
                System.arraycopy(tlv, writeOffset, blockData, 0, copyLen)
                if (!writeBlockWithRetry(mifare, blockIndex, blockData)) {
                    skippedBlocks.add(blockIndex)
                    continue
                }
                usedBlocks.add(blockIndex)
                writeOffset += copyLen
            }
            if (writeOffset < tlv.size) {
                val remain = tlv.size - writeOffset
                return uiString(R.string.ndef_write_m1_failed_blocks_format, remain)
            }

            val readBack = ByteArray(tlv.size)
            var readOffset = 0
            for (blockIndex in usedBlocks) {
                if (readOffset >= tlv.size) break
                val block = readBlockWithRetry(mifare, blockIndex)
                    ?: return uiString(R.string.ndef_write_m1_failed_verify_read_format, blockIndex)
                val copyLen = minOf(16, tlv.size - readOffset)
                System.arraycopy(block, 0, readBack, readOffset, copyLen)
                readOffset += copyLen
            }
            if (!readBack.contentEquals(tlv)) {
                return uiString(R.string.ndef_write_m1_failed_verify_mismatch)
            }
            if (skippedBlocks.isEmpty()) {
                uiString(R.string.ndef_write_m1_success)
            } else {
                uiString(R.string.ndef_write_m1_success_skipped_format, skippedBlocks.joinToString(","))
            }
        } catch (e: Exception) {
            uiString(R.string.ndef_write_m1_failed_exception_format, e.message.orEmpty())
        }
    }

    private fun buildNdefTlv(payload: ByteArray): ByteArray {
        if (payload.size <= 0xFE) {
            return ByteArray(payload.size + 3).apply {
                this[0] = 0x03.toByte()
                this[1] = payload.size.toByte()
                System.arraycopy(payload, 0, this, 2, payload.size)
                this[lastIndex] = 0xFE.toByte()
            }
        }
        return ByteArray(payload.size + 5).apply {
            this[0] = 0x03.toByte()
            this[1] = 0xFF.toByte()
            this[2] = ((payload.size shr 8) and 0xFF).toByte()
            this[3] = (payload.size and 0xFF).toByte()
            System.arraycopy(payload, 0, this, 4, payload.size)
            this[lastIndex] = 0xFE.toByte()
        }
    }

    private fun buildNdefMessage(request: NdefWriteRequest): NdefMessage {
        val record = when (request.type) {
            NdefWriteType.TEXT -> {
                NdefRecord.createTextRecord("zh", request.textContent.trim())
            }
            NdefWriteType.URL -> {
                val raw = request.url.trim()
                val normalized = if (
                    raw.startsWith("http://", ignoreCase = true) ||
                    raw.startsWith("https://", ignoreCase = true)
                ) raw else "https://$raw"
                NdefRecord.createUri(Uri.parse(normalized))
            }
            NdefWriteType.PHONE -> {
                val raw = request.phone.trim()
                val normalized = if (raw.startsWith("tel:", ignoreCase = true)) raw else "tel:$raw"
                NdefRecord.createUri(Uri.parse(normalized))
            }
            NdefWriteType.WIFI -> {
                val security = request.wifiSecurity.trim().uppercase(Locale.US).ifBlank { "WPA" }
                val ssid = escapeWifiField(request.wifiSsid.trim())
                val password = escapeWifiField(request.wifiPassword.trim())
                val wifiText = buildString {
                    append("WIFI:")
                    append("T:").append(security).append(';')
                    append("S:").append(ssid).append(';')
                    if (password.isNotEmpty()) {
                        append("P:").append(password).append(';')
                    }
                    append(';')
                }
                NdefRecord.createTextRecord("en", wifiText)
            }
        }
        return NdefMessage(arrayOf(record))
    }

    private fun escapeWifiField(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }

    private fun verifyTagAgainstDump(tag: Tag, item: ShareTagItem): String {
        val sourceBlocks = resolveShareTagBlocks(item)
            ?: return uiString(R.string.verify_failed_format, uiString(R.string.tag_import_invalid_zip))
        return when (val result = BambuMifareOperator.run(
            tag = tag,
            config = nfcCompatibilityConfig,
            operation = BambuNfcOperation.VerifyDump(sourceBlocks),
            context = this,
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) }
        )) {
            is BambuNfcResult.Message -> result.message
            is BambuNfcResult.Failure -> result.message
            is BambuNfcResult.RawRead -> uiString(R.string.verify_failed_format, uiString(R.string.bambu_nfc_unexpected_read_result))
        }
    }

    /**
     * 断线后用"源标签密钥"探测写入进度，返回应继续写入的扇区/区块位置。
     * 规则：
     * 1) 优先用源 trailer 里的 KeyA/KeyB 认证；
     * 2) 对已可读扇区逐块比较目标内容（trailer 仅比较访问位 6..9）；
     * 3) 返回第一个不一致块作为续写起点。
     */
    private fun detectWriteResumePoint(
        tag: Tag,
        sourceBlocks: List<ByteArray?>
    ): WriteResumePoint? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            val targetSectorCount = minOf(WRITE_SECTOR_COUNT, mifare.sectorCount)
            for (sector in 0 until targetSectorCount) {
                val trailerIndex = sector * 4 + 3
                val trailerData = sourceBlocks.getOrNull(trailerIndex) ?: return WriteResumePoint(sector, 0)
                if (trailerData.size != 16) return WriteResumePoint(sector, 0)
                val sourceKeyA = trailerData.copyOfRange(0, 6)
                val sourceKeyB = trailerData.copyOfRange(10, 16)

                val authBySourceKey = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(sourceKeyA),
                    keysB = listOf(sourceKeyB)
                )
                if (!authBySourceKey) {
                    // 该扇区大概率尚未写到 trailer（或写入未完成），从此扇区起继续。
                    return WriteResumePoint(sector, 0)
                }

                val startBlock = mifare.sectorToBlock(sector)
                for (offset in 0 until 4) {
                    val blockIndex = startBlock + offset
                    val expected = sourceBlocks.getOrNull(blockIndex) ?: return WriteResumePoint(sector, offset)
                    if (expected.size != 16) return WriteResumePoint(sector, offset)
                    val actual = readBlockWithRetry(mifare, blockIndex) ?: return WriteResumePoint(sector, offset)

                    if (!isBlockEquivalentForResume(blockIndex, expected, actual)) {
                        return WriteResumePoint(sector, offset)
                    }
                }
            }
            WriteResumePoint(targetSectorCount, 0)
        } catch (_: Exception) {
            null
        } finally {
            try {
                mifare.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * 写入前预检查：
     * - 空白卡：从头写；
     * - 已部分写入且前缀内容一致：从断点续写；
     * - 已写入其他内容/不可识别：阻止写入。
     */
    private fun precheckBeforeWrite(
        mifare: MifareClassic,
        sourceBlocks: List<ByteArray?>
    ): WritePrecheckResult {
        val ffKey = ByteArray(6) { 0xFF.toByte() }
        var resumePoint: WriteResumePoint? = null
        var matchedAnyBlock = false

        val targetSectorCount = minOf(WRITE_SECTOR_COUNT, mifare.sectorCount)
        for (sector in 0 until targetSectorCount) {
            val trailerIndex = sector * 4 + 3
            val trailerData = sourceBlocks.getOrNull(trailerIndex)
                ?: return WritePrecheckResult(
                    action = WritePrecheckAction.BLOCKED_CONFLICT,
                    message = getString(R.string.write_precheck_missing_trailer, sector)
                )
            if (trailerData.size != 16) {
                return WritePrecheckResult(
                    action = WritePrecheckAction.BLOCKED_CONFLICT,
                    message = getString(R.string.write_precheck_invalid_trailer, sector)
                )
            }
            val sourceKeyA = trailerData.copyOfRange(0, 6)
            val sourceKeyB = trailerData.copyOfRange(10, 16)
            val authBySource = authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = sector,
                keysA = listOf(sourceKeyA),
                keysB = listOf(sourceKeyB)
            )
            val authByFF = if (!authBySource) {
                authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(ffKey),
                    keysB = listOf(ffKey)
                )
            } else {
                false
            }

            if (!authBySource && !authByFF) {
                return WritePrecheckResult(
                    action = WritePrecheckAction.BLOCKED_UNREADABLE,
                    message = getString(R.string.write_precheck_auth_failed, sector)
                )
            }

            val startBlock = mifare.sectorToBlock(sector)
            for (offset in 0 until 4) {
                val blockIndex = startBlock + offset
                val expected = sourceBlocks.getOrNull(blockIndex)
                    ?: return WritePrecheckResult(
                        action = WritePrecheckAction.BLOCKED_CONFLICT,
                        message = getString(R.string.write_precheck_missing_block, blockIndex)
                    )
                if (expected.size != 16) {
                    return WritePrecheckResult(
                        action = WritePrecheckAction.BLOCKED_CONFLICT,
                        message = getString(R.string.write_precheck_invalid_block, blockIndex)
                    )
                }
                val actual = readBlockWithRetry(mifare, blockIndex)
                    ?: return WritePrecheckResult(
                        action = WritePrecheckAction.BLOCKED_UNREADABLE,
                        message = getString(R.string.write_precheck_read_failed, blockIndex)
                    )

                val matched = isBlockEquivalentForResume(blockIndex, expected, actual)
                val blankLike = isBlankLikeBlock(blockIndex, actual)
                if (matched) {
                    matchedAnyBlock = true
                    continue
                }
                if (blankLike) {
                    if (resumePoint == null) {
                        resumePoint = WriteResumePoint(sector, offset)
                    }
                    // 第一个空白断点之后不再强制要求连续匹配，续写将覆盖后续。
                    break
                }
                if (authByFF) {
                    // 该扇区仍可用默认 FF 密钥认证，按可覆盖区处理，不再阻止写入。
                    if (resumePoint == null) {
                        resumePoint = WriteResumePoint(sector, offset)
                    }
                    break
                }
                return WritePrecheckResult(
                    action = WritePrecheckAction.BLOCKED_CONFLICT,
                    message = getString(R.string.write_precheck_conflict, blockIndex)
                )
            }
            if (resumePoint != null) {
                break
            }
        }

        return when {
            resumePoint != null && (resumePoint.sector > 0 || resumePoint.blockOffset > 0) ->
                WritePrecheckResult(
                    action = WritePrecheckAction.RESUME_FROM_POINT,
                    resumePoint = resumePoint
                )
            matchedAnyBlock && resumePoint == null ->
                WritePrecheckResult(action = WritePrecheckAction.ALREADY_MATCHED)
            else ->
                WritePrecheckResult(action = WritePrecheckAction.START_FROM_BEGINNING)
        }
    }

    private fun isBlockEquivalentForResume(
        blockIndex: Int,
        expected: ByteArray,
        actual: ByteArray
    ): Boolean {
        if (blockIndex % 4 != 3) {
            return expected.contentEquals(actual)
        }
        // trailer：很多设备无法读出密钥位，仅比较访问控制位 6..9。
        for (i in 6..9) {
            if (expected[i] != actual[i]) return false
        }
        return true
    }

    private fun isBlankLikeBlock(blockIndex: Int, block: ByteArray): Boolean {
        if (block.all { it == 0.toByte() } || block.all { it == 0xFF.toByte() }) {
            return true
        }
        if (blockIndex % 4 != 3) {
            return false
        }
        // 常见空白 trailer: FFFFFFFFFFFF + FF078069 + FFFFFFFFFFFF
        val keyAAllFF = (0..5).all { block[it] == 0xFF.toByte() }
        val acDefault = block[6] == 0xFF.toByte() &&
            block[7] == 0x07.toByte() &&
            block[8] == 0x80.toByte() &&
            block[9] == 0x69.toByte()
        val keyBAllFF = (10..15).all { block[it] == 0xFF.toByte() }
        return keyAAllFF && acDefault && keyBAllFF
    }

    // ── 创想三维 Creality RFID ──────────────────────────────────────────────────

    private fun deriveCrealityKeyA(uid: ByteArray): ByteArray {
        val input = ByteArray(16) { uid[it % 4] }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CREALITY_KEY_DERIVE, "AES"))
        return cipher.doFinal(input).copyOfRange(0, 6)
    }

    private fun encryptCrealityData48(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CREALITY_KEY_DATA, "AES"))
        return cipher.doFinal(data)
    }

    private fun decryptCrealityData48(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(CREALITY_KEY_DATA, "AES"))
        return cipher.doFinal(data)
    }

    private fun buildCrealityTagBytes(materialId: String, colorHex: String, weight: String, serial: String = "000001"): ByteArray {
        val lengthCode = CREALITY_WEIGHT_TO_LENGTH[weight] ?: "0330"
        val raw = "AB124" +
            "0276" +
            "A2" +
            "1" + materialId.uppercase(Locale.US).padStart(5, '0') +
            "0" + colorHex.uppercase(Locale.US).trimStart('#').padStart(6, '0') +
            lengthCode +
            serial.padStart(6, '0') +
            "00000000000000"
        return raw.padEnd(48, ' ').toByteArray(Charsets.UTF_8)
    }

    private fun parseCrealityTagString(raw: String, uidHex: String = ""): CrealityTagData? {
        if (raw.length < 48) return null
        val serial = raw.substring(28, 34).trim().trimStart('0').ifEmpty { "" }
        val vendorId = raw.substring(5, 9).trim()
        // 尾部14字节可能含生产日期（写入端填充全零时不显示）
        val tailRaw = raw.substring(34).trim().trimEnd('\u0000', ' ')
        val mfDate = if (tailRaw.all { it == '0' }) "" else tailRaw
        return CrealityTagData(
            materialId = raw.substring(12, 17).trim(),
            colorHex = raw.substring(18, 24).trim(),
            weight = CREALITY_LENGTH_TO_WEIGHT[raw.substring(24, 28)] ?: getString(R.string.label_unknown),
            serial = serial,
            vendorId = vendorId,
            batch = "",
            lengthCode = raw.substring(24, 28),
            rawPlaintext = raw,
            uidHex = uidHex,
            mfDate = mfDate
        )
    }

    private fun readCrealityTag(tag: Tag): CrealityTagData? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            if (!mifare.isConnected) mifare.connect()
            Thread.sleep(300)
            val uid = tag.id ?: return null
            val derivedKey = deriveCrealityKeyA(uid)
            val ffKey = ByteArray(6) { 0xFF.toByte() }
            val authenticated = authenticateSectorWithRetry(
                mifare = mifare, sectorIndex = 1,
                keysA = listOf(derivedKey, ffKey),
                keysB = listOf(derivedKey, ffKey)
            )
            if (!authenticated) return null
            val b4 = readBlockWithRetry(mifare, 4) ?: return null
            val b5 = readBlockWithRetry(mifare, 5) ?: return null
            val b6 = readBlockWithRetry(mifare, 6) ?: return null
            val decrypted = decryptCrealityData48(b4 + b5 + b6)
            val uidHex = uid.joinToString("") { "%02X".format(it) }
            parseCrealityTagString(String(decrypted, Charsets.UTF_8), uidHex)
        } catch (e: Exception) {
            logDebug("Creality read failed: ${e.message}")
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun writeCrealityTag(tag: Tag, pending: CrealityWritePending): String {
        val mifare = MifareClassic.get(tag) ?: return uiString(R.string.creality_write_failed_no_mifare)
        return try {
            if (!mifare.isConnected) mifare.connect()
            Thread.sleep(300)
            val uid = tag.id ?: return uiString(R.string.creality_write_failed_no_uid)
            val derivedKey = deriveCrealityKeyA(uid)
            val ffKey = ByteArray(6) { 0xFF.toByte() }
            val authenticated = authenticateSectorWithRetry(
                mifare = mifare, sectorIndex = 1,
                keysA = listOf(derivedKey, ffKey),
                keysB = listOf(derivedKey, ffKey)
            )
            if (!authenticated) return uiString(R.string.creality_write_failed_auth)
            val plaintext = buildCrealityTagBytes(pending.materialId, pending.colorHex, pending.weight)
            val encrypted = encryptCrealityData48(plaintext)
            val b4ok = writeBlockWithRetry(mifare, 4, encrypted.copyOfRange(0, 16))
            val b5ok = writeBlockWithRetry(mifare, 5, encrypted.copyOfRange(16, 32))
            val b6ok = writeBlockWithRetry(mifare, 6, encrypted.copyOfRange(32, 48))
            if (!b4ok || !b5ok || !b6ok) return uiString(R.string.creality_write_failed_blocks)
            // Update trailer: KeyA=derived, access bits=FF078069, KeyB=FF×6
            val trailer = derivedKey +
                byteArrayOf(0xFF.toByte(), 0x07.toByte(), 0x80.toByte(), 0x69.toByte()) +
                ffKey
            writeBlockWithRetry(mifare, 7, trailer)
            uiString(R.string.creality_write_success)
        } catch (e: Exception) {
            uiString(R.string.creality_write_failed_format, e.message.orEmpty())
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    // ── End Creality ─────────────────────────────────────────────────────────

    // ── 自动品牌检测 ──────────────────────────────────────────────────────────

    /**
     * 通过扇区0秘钥认证判断卡片品牌：
     * 1. 拓竹：UID派生KeyA/B（deriveWriteKeys）
     * 2. 创想：AES派生KeyA（deriveCrealityKeyA），KeyB=FF
     * 3. 快造：HKDF派生KeyA/B（deriveSnapmakerKeys）
     * 4. 空白/未知：FF秘钥
     * 返回检测到的 ReaderBrand，或 null 表示无法判断（FF / 未知）
     */
    private fun detectBrandBySector0(tag: Tag): ReaderBrand? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            Thread.sleep(200)
            val uid = tag.id ?: return null
            val ffKey = ByteArray(6) { 0xFF.toByte() }

            val bambuKeysA = try { deriveWriteKeys(uid, WRITE_INFO_A) } catch (_: Exception) { emptyList() }
            val bambuKeysB = try { deriveWriteKeys(uid, WRITE_INFO_B) } catch (_: Exception) { emptyList() }
            val crealityKey = try { deriveCrealityKeyA(uid) } catch (_: Exception) { null }
            val (snapKeysA, snapKeysB) = try { deriveSnapmakerKeys(uid) } catch (_: Exception) { Pair(emptyList(), emptyList()) }

            when {
                bambuKeysA.isNotEmpty() && bambuKeysB.isNotEmpty() &&
                authenticateSectorWithRetry(mifare, 0, listOf(bambuKeysA[0]), listOf(bambuKeysB[0])) ->
                    ReaderBrand.BAMBU

                crealityKey != null &&
                authenticateSectorWithRetry(mifare, 0, listOf(crealityKey), listOf(ffKey)) ->
                    ReaderBrand.CREALITY

                snapKeysA.isNotEmpty() && snapKeysB.isNotEmpty() &&
                authenticateSectorWithRetry(mifare, 0, listOf(snapKeysA[0]), listOf(snapKeysB[0])) ->
                    ReaderBrand.SNAPMAKER

                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    // ── 快造 (Snapmaker) RFID ─────────────────────────────────────────────────

    /** HKDF-SHA256 单块派生：Extract (HMAC(salt, ikm)) 然后 Expand 取 length 字节 */
    private fun snapmakerHkdfDerive(ikm: ByteArray, salt: ByteArray, context: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(context)
        mac.update(0x01.toByte())
        return mac.doFinal().copyOf(length)
    }

    /** 为 16 个扇区分别派生 KeyA / KeyB */
    private fun deriveSnapmakerKeys(uid: ByteArray): Pair<List<ByteArray>, List<ByteArray>> {
        val keysA = (0 until 16).map { i ->
            snapmakerHkdfDerive(uid, SNAPMAKER_SALT_A, "key_a_$i".toByteArray(Charsets.US_ASCII), 6)
        }
        val keysB = (0 until 16).map { i ->
            snapmakerHkdfDerive(uid, SNAPMAKER_SALT_B, "key_b_$i".toByteArray(Charsets.US_ASCII), 6)
        }
        return Pair(keysA, keysB)
    }

    /** 读取 Snapmaker 标签，返回解析结果；认证或解析失败返回 null */
    private fun readSnapmakerTag(tag: Tag): SnapmakerTagData? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            if (!mifare.isConnected) mifare.connect()
            Thread.sleep(300)
            val uid = tag.id ?: return null
            val (keysA, keysB) = deriveSnapmakerKeys(uid)

            // 组装 1024 字节缓冲区：16 扇区 × 4 块 × 16 字节（含 trailer 占位）
            val dataBuf = ByteArray(1024)
            var anySuccess = false
            for (sector in 0 until 16) {
                val authenticated = authenticateSectorWithRetry(
                    mifare = mifare,
                    sectorIndex = sector,
                    keysA = listOf(keysA[sector]),
                    keysB = listOf(keysB[sector])
                )
                if (!authenticated) continue
                val firstBlock = mifare.sectorToBlock(sector)
                for (blockInSector in 0 until 4) {
                    val block = readBlockWithRetry(mifare, firstBlock + blockInSector)
                    if (block != null) {
                        block.copyInto(dataBuf, sector * 64 + blockInSector * 16)
                        if (blockInSector < 3) anySuccess = true
                    }
                }
            }
            if (!anySuccess) return null

            // 捕获原始块数据和密钥，供自动共享上传使用（与拓竹格式一致）
            val uidHexSnap = uid.joinToString("") { "%02X".format(it) }
            val rawBlocksSnap: List<ByteArray?> = (0 until 64).map { blockIndex ->
                val sector = blockIndex / 4
                val blockInSector = blockIndex % 4
                if (blockInSector == 3) {
                    // 重建 trailer 块：KeyA(6) + 访问字节(4) + KeyB(6)
                    val trailerOffset = sector * 64 + 48
                    val accessBytes = dataBuf.copyOfRange(trailerOffset + 6, trailerOffset + 10)
                    keysA[sector] + accessBytes + keysB[sector]
                } else {
                    val offset = sector * 64 + blockInSector * 16
                    dataBuf.copyOfRange(offset, offset + 16)
                }
            }
            latestSnapmakerRawData = RawTagReadData(
                uidHex   = uidHexSnap,
                keyA0Hex = keysA[0].joinToString("") { "%02x".format(it) },
                keyB0Hex = keysB[0].joinToString("") { "%02x".format(it) },
                keyA1Hex = keysA[1].joinToString("") { "%02x".format(it) },
                keyB1Hex = keysB[1].joinToString("") { "%02x".format(it) },
                sectorKeys = (0 until 16).map { i ->
                    Pair(keysA[i] as ByteArray?, keysB[i] as ByteArray?)
                },
                rawBlocks = rawBlocksSnap,
                errors    = emptyList()
            )

            parseSnapmakerData(dataBuf, uid)
        } catch (e: Exception) {
            logDebug("Snapmaker read failed: ${e.message}")
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    /** 将 dataBuf 解析为 SnapmakerTagData（偏移量来自 M1 协议规范） */
    private fun parseSnapmakerData(dataBuf: ByteArray, uid: ByteArray): SnapmakerTagData? {
        if (dataBuf.size != 1024) return null

        fun le16(offset: Int) = ((dataBuf[offset + 1].toInt() and 0xFF) shl 8) or (dataBuf[offset].toInt() and 0xFF)
        fun readRgb(offset: Int) = ((dataBuf[offset].toInt() and 0xFF) shl 16) or
                ((dataBuf[offset + 1].toInt() and 0xFF) shl 8) or
                (dataBuf[offset + 2].toInt() and 0xFF)

        // RSA 密钥版本: sector2 block2 bytes 8-9 → offset 168
        val rsaKeyVer = le16(168)

        // RSA 签名验证（可选，不影响数据展示）
        val isOfficial = tryVerifySnapmakerSignature(dataBuf, rsaKeyVer)

        val vendor = String(dataBuf, 16, 16, Charsets.US_ASCII).trimEnd('\u0000')        // sector0 block1
        val manufacturer = String(dataBuf, 32, 16, Charsets.US_ASCII).trimEnd('\u0000') // sector0 block2

        val mainTypeCode = le16(66)   // sector1 block0 bytes2-3
        val subTypeCode  = le16(68)   // sector1 block0 bytes4-5
        val colorNums    = dataBuf[72].toInt() and 0xFF                                 // sector1 block0 byte8

        val rgb1 = readRgb(80); val rgb2 = readRgb(83); val rgb3 = readRgb(86)
        val rgb4 = readRgb(89); val rgb5 = readRgb(92)

        val diameter    = le16(128)  // sector2 block0 bytes0-1
        val weight      = le16(130)  // sector2 block0 bytes2-3
        val dryingTemp  = le16(144)  // sector2 block1 bytes0-1
        val dryingTime  = le16(146)  // sector2 block1 bytes2-3
        val hotendMax   = le16(148)  // sector2 block1 bytes4-5
        val hotendMin   = le16(150)  // sector2 block1 bytes6-7
        val bedTemp     = le16(154)  // sector2 block1 bytes10-11
        val mfDate = String(dataBuf, 160, 8, Charsets.US_ASCII).trimEnd('\u0000')       // sector2 block2 bytes0-7

        return SnapmakerTagData(
            vendor       = vendor.ifBlank { "-" },
            manufacturer = manufacturer.ifBlank { "-" },
            mainType     = SNAPMAKER_MAIN_TYPE_MAP[mainTypeCode] ?: "Unknown($mainTypeCode)",
            subType      = SNAPMAKER_SUB_TYPE_MAP[subTypeCode]  ?: "Unknown($subTypeCode)",
            colorCount   = colorNums,
            rgb1 = rgb1, rgb2 = rgb2, rgb3 = rgb3, rgb4 = rgb4, rgb5 = rgb5,
            diameter     = diameter,
            weight       = weight,
            dryingTemp   = dryingTemp,
            dryingTime   = dryingTime,
            hotendMaxTemp = hotendMax,
            hotendMinTemp = hotendMin,
            bedTemp      = bedTemp,
            mfDate       = mfDate.ifBlank { "-" },
            isOfficial   = isOfficial,
            uidHex       = uid.joinToString("") { "%02X".format(it) },
            rsaKeyVersion = rsaKeyVer
        )
    }

    /** 将 PKCS#1 PEM 转换为 Java PublicKey（动态构造 SubjectPublicKeyInfo 包装） */
    private fun loadSnapmakerRsaPublicKey(pem: String): java.security.PublicKey? {
        return try {
            val base64 = pem
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val pkcs1 = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)

            fun derLen(n: Int): ByteArray = when {
                n < 128 -> byteArrayOf(n.toByte())
                n < 256 -> byteArrayOf(0x81.toByte(), n.toByte())
                else    -> byteArrayOf(0x82.toByte(), (n shr 8).toByte(), (n and 0xFF).toByte())
            }
            val algId = byteArrayOf(
                0x30, 0x0d,
                0x06, 0x09, 0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
            )
            val bsContent = byteArrayOf(0x00) + pkcs1
            val bitStr    = byteArrayOf(0x03) + derLen(bsContent.size) + bsContent
            val seqBody   = algId + bitStr
            val spki      = byteArrayOf(0x30) + derLen(seqBody.size) + seqBody

            java.security.KeyFactory.getInstance("RSA")
                .generatePublic(java.security.spec.X509EncodedKeySpec(spki))
        } catch (e: Exception) {
            logDebug("Snapmaker: RSA key load failed: ${e.message}")
            null
        }
    }

    /** 验证 Snapmaker 标签的 RSA-PKCS1v15-SHA256 签名；验证失败不影响解析 */
    private fun tryVerifySnapmakerSignature(dataBuf: ByteArray, keyVersion: Int): Boolean {
        if (keyVersion < 0 || keyVersion >= SNAPMAKER_RSA_KEYS.size) return false
        val pubKey = loadSnapmakerRsaPublicKey(SNAPMAKER_RSA_KEYS[keyVersion]) ?: return false
        return try {
            // 从 sector10-15 各取前 48 字节（3个数据块）拼成签名，取前 256 字节
            val sigCollected = ByteArray(288)
            for (i in 0 until 6) {
                dataBuf.copyInto(sigCollected, i * 48, (10 + i) * 64, (10 + i) * 64 + 48)
            }
            val sig = java.security.Signature.getInstance("SHA256withRSA")
            sig.initVerify(pubKey)
            sig.update(dataBuf, 0, 640)
            sig.verify(sigCollected.copyOf(256))
        } catch (e: Exception) {
            logDebug("Snapmaker: signature check failed: ${e.message}")
            false
        }
    }

    // ── End Snapmaker ─────────────────────────────────────────────────────────

    private fun runNfcCompatibilityTest(
        tag: Tag,
        includeWrite: Boolean,
        onStatusUpdate: (String) -> Unit
    ): String {
        val uid = tag.id ?: return uiString(R.string.nfc_compat_fail_no_uid)
        if (MifareClassic.get(tag) == null) {
            return uiString(R.string.nfc_compat_fail_no_mifare)
        }

        val results = NfcCompatibilityMode.values().map { mode ->
            val config = NfcCompatibilityConfig.forMode(mode)
            onStatusUpdate(uiString(R.string.nfc_compat_testing_mode_format, nfcCompatibilityModeLabel(mode)))
            testNfcCompatibilityMode(tag, uid, config, includeWrite)
        }

        val successful = results
            .filter { if (includeWrite) it.readOk && it.writeOk else it.readOk }
            .maxByOrNull { it.score }

        if (successful != null) {
            runOnUiThread {
                nfcCompatibilityConfig = NfcCompatibilityPreferences.saveMode(this, successful.mode)
                refreshNfcReaderMode()
            }
        }

        val summary = results.joinToString(separator = uiString(R.string.nfc_compat_summary_separator)) { result ->
            val readText = if (result.readOk) {
                uiString(R.string.nfc_compat_read_ok)
            } else {
                uiString(R.string.nfc_compat_read_failed)
            }
            val writeText = if (includeWrite) {
                if (result.writeOk) {
                    uiString(R.string.nfc_compat_write_ok)
                } else {
                    uiString(R.string.nfc_compat_write_failed)
                }
            } else {
                ""
            }
            "${nfcCompatibilityModeLabel(result.mode)}:$readText$writeText ${result.durationMs}ms"
        }

        return if (successful != null) {
            uiString(R.string.nfc_compat_success_format, nfcCompatibilityModeLabel(successful.mode), summary)
        } else if (includeWrite && results.any { it.readOk }) {
            uiString(R.string.nfc_compat_write_no_reliable_mode_format, summary)
        } else {
            uiString(R.string.nfc_compat_no_available_mode_format, summary)
        }
    }

    private fun testNfcCompatibilityMode(
        tag: Tag,
        uid: ByteArray,
        config: NfcCompatibilityConfig,
        includeWrite: Boolean
    ): NfcCompatibilityTestResult {
        val startedAt = System.currentTimeMillis()
        val mifare = MifareClassic.get(tag) ?: return NfcCompatibilityTestResult(
            mode = config.mode,
            readOk = false,
            writeOk = false,
            durationMs = 0L,
            message = uiString(R.string.nfc_compat_result_no_mifare)
        )
        return try {
            mifare.connect()
            MifareClassicSession.applyTimeout(mifare, config.mifareTimeoutMs)
            if (config.postConnectDelayMs > 0) Thread.sleep(config.postConnectDelayMs)

            val ffKey = ByteArray(6) { 0xFF.toByte() }
            val bambuKeys = deriveBambuKeys(uid)
            logNfcCompatibilityDerivedKeys(uid, bambuKeys, config)
            if (config.postKeyDerivationDelayMs > 0) {
                Thread.sleep(config.postKeyDerivationDelayMs)
            }
            val sector = if (mifare.sectorCount > 1) 1 else 0
            val keys = bambuKeys.getOrNull(sector)
            val authenticated = MifareClassicSession.authenticateSectorWithRetry(
                mifare = mifare,
                sectorIndex = sector,
                keysA = listOf(keys?.first, ffKey),
                keysB = listOf(keys?.second, ffKey),
                retryCount = config.authRetryCount,
                reconnectDelayMs = config.reconnectDelayMs,
                keyOrder = MifareClassicSession.KeyOrder.INTERLEAVED_BY_INDEX,
                ensureConnectedBeforeAttempt = true,
                reconnectAfterFailedAttempt = config.reconnectAfterFailedAuth,
                mifareTimeoutMs = config.mifareTimeoutMs,
                postConnectDelayMs = config.postConnectDelayMs
            )
            if (!authenticated) {
                return NfcCompatibilityTestResult(
                    mode = config.mode,
                    readOk = false,
                    writeOk = false,
                    durationMs = System.currentTimeMillis() - startedAt,
                    message = uiString(R.string.nfc_compat_result_auth_failed)
                )
            }

            val dataBlock = mifare.sectorToBlock(sector) + if (sector == 0) 1 else 0
            val original = readCompatibilityBlock(mifare, dataBlock, config)
                ?: return NfcCompatibilityTestResult(
                    mode = config.mode,
                    readOk = false,
                    writeOk = false,
                    durationMs = System.currentTimeMillis() - startedAt,
                    message = uiString(R.string.nfc_compat_result_read_failed)
                )

            val writeOk = if (includeWrite) {
                writeCompatibilityBlock(mifare, dataBlock, original, config)
            } else {
                false
            }

            NfcCompatibilityTestResult(
                mode = config.mode,
                readOk = true,
                writeOk = writeOk,
                durationMs = System.currentTimeMillis() - startedAt
            )
        } catch (e: Exception) {
            NfcCompatibilityTestResult(
                mode = config.mode,
                readOk = false,
                writeOk = false,
                durationMs = System.currentTimeMillis() - startedAt,
                message = "${e.javaClass.simpleName}: ${e.message}"
            )
        } finally {
            try {
                mifare.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun logNfcCompatibilityDerivedKeys(
        uid: ByteArray,
        keys: List<Pair<ByteArray, ByteArray>>,
        config: NfcCompatibilityConfig
    ) {
        val uidHex = uid.toHex()
        val preview = keys.take(4).mapIndexed { sector, sectorKeys ->
            "S$sector A=${sectorKeys.first.toHex()} B=${sectorKeys.second.toHex()}"
        }.joinToString(separator = " | ")
        val summary = "NFC compatibility ${config.mode} derived keys UID=$uidHex uidBytes=${uid.size} sectors=${keys.size} postKeyDelay=${config.postKeyDerivationDelayMs}ms reconnectAfterFailedAuth=${config.reconnectAfterFailedAuth} $preview"
        logDebug(summary)
        LogCollector.append(this, "I", summary)
        keys.forEachIndexed { sector, sectorKeys ->
            LogCollector.append(
                this,
                "D",
                "NFC compatibility ${config.mode} S$sector KeyA=${sectorKeys.first.toHex()} KeyB=${sectorKeys.second.toHex()}"
            )
        }
    }

    private fun readCompatibilityBlock(
        mifare: MifareClassic,
        blockIndex: Int,
        config: NfcCompatibilityConfig
    ): ByteArray? {
        for (attempt in 0..config.blockRetryCount) {
            try {
                val raw = mifare.readBlock(blockIndex)
                return when {
                    raw.size == 16 -> raw
                    raw.size > 16 -> raw.copyOf(16)
                    else -> null
                }
            } catch (_: Exception) {
                if (attempt < config.blockRetryCount && config.readInterBlockDelayMs > 0) {
                    Thread.sleep(config.readInterBlockDelayMs)
                }
            }
        }
        return null
    }

    private fun writeCompatibilityBlock(
        mifare: MifareClassic,
        blockIndex: Int,
        data: ByteArray,
        config: NfcCompatibilityConfig
    ): Boolean {
        for (attempt in 0..config.blockRetryCount) {
            try {
                if (config.writeInterBlockDelayMs > 0) Thread.sleep(config.writeInterBlockDelayMs)
                mifare.writeBlock(blockIndex, data)
                if (config.writeVerificationDelayMs > 0) Thread.sleep(config.writeVerificationDelayMs)
                val readBack = readCompatibilityBlock(mifare, blockIndex, config)
                if (readBack != null && readBack.contentEquals(data)) {
                    return true
                }
            } catch (_: Exception) {
                if (attempt < config.blockRetryCount && config.writeInterBlockDelayMs > 0) {
                    Thread.sleep(config.writeInterBlockDelayMs)
                }
            }
        }
        return false
    }

    private fun authenticateSectorWithRetry(
        mifare: MifareClassic,
        sectorIndex: Int,
        keysA: List<ByteArray?>,
        keysB: List<ByteArray?>
    ): Boolean {
        return MifareClassicSession.authenticateSectorWithRetry(
            mifare = mifare,
            sectorIndex = sectorIndex,
            keysA = keysA,
            keysB = keysB,
            retryCount = nfcCompatibilityConfig.authRetryCount,
            reconnectDelayMs = nfcCompatibilityConfig.reconnectDelayMs,
            keyOrder = MifareClassicSession.KeyOrder.ALL_A_THEN_ALL_B,
            ensureConnectedBeforeAttempt = true,
            mifareTimeoutMs = nfcCompatibilityConfig.mifareTimeoutMs,
            postConnectDelayMs = nfcCompatibilityConfig.postConnectDelayMs
        )
    }

    private fun writeBlockWithRetry(
        mifare: MifareClassic,
        blockIndex: Int,
        data: ByteArray
    ): Boolean {
        for (attempt in 0..nfcCompatibilityConfig.blockRetryCount) {
            try {
                if (!ensureMifareClassicConnected(mifare)) {
                    continue
                }
                if (nfcCompatibilityConfig.interBlockDelayMs > 0) {
                    Thread.sleep(nfcCompatibilityConfig.interBlockDelayMs)
                }
                mifare.writeBlock(blockIndex, data)
                if (nfcCompatibilityConfig.writeVerificationDelayMs > 0) {
                    Thread.sleep(nfcCompatibilityConfig.writeVerificationDelayMs)
                }
                if (!nfcCompatibilityConfig.verifyEachWriteBlock || isMifareTrailerBlock(mifare, blockIndex)) {
                    return true
                }
                val readBack = try {
                    mifare.readBlock(blockIndex)
                } catch (_: Exception) {
                    null
                }?.let { raw ->
                    when {
                        raw.size == 16 -> raw
                        raw.size > 16 -> raw.copyOf(16)
                        else -> null
                    }
                }
                if (readBack != null && readBack.contentEquals(data)) {
                    return true
                }
            } catch (_: Exception) {
                if (!mifare.isConnected) {
                    reconnectMifareClassic(mifare)
                    return false
                }
            }
        }
        return false
    }

    private fun isMifareTrailerBlock(mifare: MifareClassic, blockIndex: Int): Boolean {
        return try {
            val sector = mifare.blockToSector(blockIndex)
            val firstBlock = mifare.sectorToBlock(sector)
            blockIndex == firstBlock + mifare.getBlockCountInSector(sector) - 1
        } catch (_: Exception) {
            false
        }
    }

    private fun readBlockWithRetry(
        mifare: MifareClassic,
        blockIndex: Int
    ): ByteArray? {
        for (attempt in 0..nfcCompatibilityConfig.blockRetryCount) {
            try {
                if (!ensureMifareClassicConnected(mifare)) {
                    continue
                }
                val raw = mifare.readBlock(blockIndex)
                return when {
                    raw.size == 16 -> raw
                    raw.size > 16 -> raw.copyOf(16)
                    else -> null
                }
            } catch (_: Exception) {
                reconnectMifareClassic(mifare)
            }
        }
        return null
    }

    private fun ensureMifareClassicConnected(mifare: MifareClassic): Boolean {
        return MifareClassicSession.ensureConnected(
            mifare = mifare,
            reconnectDelayMs = nfcCompatibilityConfig.reconnectDelayMs,
            mifareTimeoutMs = nfcCompatibilityConfig.mifareTimeoutMs,
            postConnectDelayMs = nfcCompatibilityConfig.postConnectDelayMs
        )
    }

    private fun reconnectMifareClassic(mifare: MifareClassic): Boolean {
        return MifareClassicSession.reconnect(
            mifare = mifare,
            reconnectDelayMs = nfcCompatibilityConfig.reconnectDelayMs,
            mifareTimeoutMs = nfcCompatibilityConfig.mifareTimeoutMs,
            postConnectDelayMs = nfcCompatibilityConfig.postConnectDelayMs
        )
    }

    private fun deriveWriteKeys(uid: ByteArray, info: ByteArray): List<ByteArray> {
        val prk = hkdfExtractForWrite(WRITE_HKDF_SALT, uid)
        val okm = hkdfExpandForWrite(prk, info, WRITE_KEY_LENGTH_BYTES * WRITE_SECTOR_COUNT)
        val keys = ArrayList<ByteArray>(WRITE_SECTOR_COUNT)
        for (i in 0 until WRITE_SECTOR_COUNT) {
            val start = i * WRITE_KEY_LENGTH_BYTES
            keys.add(okm.copyOfRange(start, start + WRITE_KEY_LENGTH_BYTES))
        }
        return keys
    }

    private fun hkdfExtractForWrite(salt: ByteArray, ikm: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    private fun hkdfExpandForWrite(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val hashLen = mac.macLength
        val blocks = ceil(length.toDouble() / hashLen.toDouble()).toInt()
        var t = ByteArray(0)
        val output = java.io.ByteArrayOutputStream()
        for (i in 1..blocks) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()
            output.write(t)
        }
        return output.toByteArray().copyOf(length)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val result = ByteArray(clean.length / 2)
        var i = 0
        while (i < clean.length) {
            result[i / 2] = clean.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        return result
    }

    private fun initTts() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        ttsLanguageReady = false
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val locales = listOf(
                    Locale.SIMPLIFIED_CHINESE,
                    Locale.CHINESE,
                    Locale.Builder().setLanguage("zh").setRegion("CN").build(),
                    Locale.getDefault()
                )
                for (locale in locales) {
                    val result = tts?.setLanguage(locale)
                    if (result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        ttsLanguageReady = true
                        logDebug("语音语言可用: $locale")
                        break
                    }
                }
                if (!ttsLanguageReady) {
                    logDebug("没有可用的语音语言")
                }
                logDebug("语音引擎初始化完成: $ttsReady，语言就绪: $ttsLanguageReady")
            } else {
                ttsReady = false
                ttsLanguageReady = false
                logDebug("语音引擎初始化失败")
            }
        }
    }

    private fun maybeSpeakResult(state: NfcUiState) {
        if (!voiceEnabled) {
            return
        }
        if (!ttsReady) {
            return
        }
        val type = state.displayType.trim()
        val colorName = state.displayColorName.trim()
        if (type.isBlank() && colorName.isBlank()) {
            return
        }
        val key = listOf(
            state.uidHex,
            type,
            colorName,
            state.displayColorCode,
            state.displayColorType,
            state.displayColors.joinToString(separator = ",")
        ).joinToString(separator = "|")
        if (key == lastSpokenKey) {
            return
        }
        lastSpokenKey = key
        val parts = ArrayList<String>()
        if (type.isNotBlank()) {
            val speechType = buildSpeechMaterialName(type)
            parts.add(getString(R.string.tts_material_type_format, speechType))
        }
        if (colorName.isNotBlank()) {
            parts.add(getString(R.string.tts_color_format, colorName))
        }
        val message = parts.joinToString(separator = getString(R.string.tts_separator))
        if (message.isNotBlank()) {
            logDebug("语音播报内容: $message")
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "scan_result")
        }
    }

    private fun buildSpeechMaterialName(raw: String): String {
        val words = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) {
            return ""
        }
        return words.joinToString("、") { word ->
            if (isAllUppercaseWord(word)) {
                val letters = word.filter { it.isLetterOrDigit() }
                if (letters.isBlank()) {
                    word
                } else {
                    letters.map { it.lowercaseChar().toString() }.joinToString("、")
                }
            } else {
                word
            }
        }
    }

    private fun isAllUppercaseWord(word: String): Boolean {
        var hasLetter = false
        for (ch in word) {
            if (ch in 'a'..'z') {
                return false
            }
            if (ch in 'A'..'Z') {
                hasLetter = true
            }
        }
        return hasLetter
    }

    private fun readTag(tag: Tag): NfcUiState {
        // 第一阶段：仅做读卡，返回原始块数据，不做业务解析。
        // 开启自动共享时强制读取全部扇区，确保上传数据完整。
        // 识别页读取使用高速无重试配置，最大化读卡速度；写入/校验仍用用户档位。
        val rawResult = NfcTagReader.readRaw(
            tag = tag,
            readAllSectors = readAllSectors || autoShareTag,
            context = this,
            compatibilityConfig = NfcCompatibilityConfig.fastRead(),
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) }
        )

        return when (rawResult) {
            is RawTagReadResult.Success -> {
                // 成功后先缓存到临时变量，解析流程只依赖该临时变量。
                latestRawTagData = rawResult.data
                if (readAllSectors) {
                    // 按配置导出全部扇区原始数据（调试/排障用途）。
                    saveAllSectorsData(
                        uidHex = rawResult.data.uidHex,
                        rawBlocks = rawResult.data.rawBlocks,
                        sectorKeys = rawResult.data.sectorKeys
                    )
                }
                if (saveKeysToFile) {
                    saveSectorKeysToFile(
                        uidHex = rawResult.data.uidHex,
                        sectorKeys = rawResult.data.sectorKeys
                    )
                }
                // 第二阶段：独立解析与入库。
                parseLatestRawTagData()
            }

            is RawTagReadResult.Failure -> {
                // 读卡失败直接映射为 UI 状态，不进入解析流程。
                when (rawResult.reason) {
                    RawTagReadFailureReason.UID_MISSING -> NfcUiState(
                        status = uiString(R.string.status_uid_missing)
                    )

                    RawTagReadFailureReason.MIFARE_UNSUPPORTED -> NfcUiState(
                        status = uiString(R.string.error_mifare_unsupported),
                        uidHex = rawResult.uidHex,
                        keyA0Hex = rawResult.keyA0Hex,
                        keyB0Hex = rawResult.keyB0Hex,
                        keyA1Hex = rawResult.keyA1Hex,
                        keyB1Hex = rawResult.keyB1Hex,
                        error = uiString(R.string.error_mifare_unsupported)
                    )

                    RawTagReadFailureReason.EXCEPTION -> NfcUiState(
                        status = uiString(R.string.status_read_failed),
                        uidHex = rawResult.uidHex,
                        keyA0Hex = rawResult.keyA0Hex,
                        keyB0Hex = rawResult.keyB0Hex,
                        keyA1Hex = rawResult.keyA1Hex,
                        keyB1Hex = rawResult.keyB1Hex,
                        error = rawResult.message.ifBlank { uiString(R.string.error_read_exception) }
                    )
                }
            }
        }
    }

    private fun parseLatestRawTagData(): NfcUiState {
        // 解析函数只从临时变量取数据，避免与读卡层耦合。
        val rawData = latestRawTagData ?: return NfcUiState(
            status = uiString(R.string.status_read_failed),
            error = uiString(R.string.error_read_exception)
        )

        // 执行解析 + 入库，返回结构化展示数据。
        val processed = NfcTagProcessor.parseAndPersist(
            rawData = rawData,
            dbHelper = filamentDbHelper,
            defaultRemainingPercent = DEFAULT_REMAINING_PERCENT.toFloat(),
            logger = ::logDebug,
            appendLog = { level, message -> LogCollector.append(applicationContext, level, message) }
        )

        // 依据原始读卡错误与有效块情况，统一生成最终状态文案。
        val status = when {
            rawData.errors.isEmpty() -> uiString(R.string.status_read_success)
            processed.blockHexes.any { it.isNotBlank() } -> uiString(R.string.status_read_partial)
            else -> uiString(R.string.status_read_failed)
        }
        if (rawData.errors.isNotEmpty()) {
            logDebug("读取错误: ${rawData.errors.joinToString(separator = "; ")}")
        }

        val extraFields = if (processed.trayUidHex.isNotBlank()) {
            val db = filamentDbHelper?.readableDatabase
            db?.let { filamentDbHelper?.getTrayExtraFields(it, processed.trayUidHex) } ?: Pair("", "")
        } else Pair("", "")

        return NfcUiState(
            status = status,
            uidHex = rawData.uidHex,
            keyA0Hex = rawData.keyA0Hex,
            keyB0Hex = rawData.keyB0Hex,
            keyA1Hex = rawData.keyA1Hex,
            keyB1Hex = rawData.keyB1Hex,
            blockHexes = processed.blockHexes,
            parsedFields = processed.parsedFields,
            displayType = processed.displayData.type,
            displayDetailedType = processed.displayData.detailedType,
            displayColorName = processed.displayData.colorName,
            displayColorCode = processed.displayData.colorCode,
            displayFilaColorCode = processed.displayData.filaColorCode,
            displayColorType = processed.displayData.colorType,
            displayColors = processed.displayData.colorValues,
            secondaryFields = processed.displayData.secondaryFields,
            trayUidHex = processed.trayUidHex,
            remainingPercent = processed.remainingPercent,
            remainingGrams = processed.remainingGrams,
            totalWeightGrams = processed.totalWeightGrams,
            originalMaterial = extraFields.first,
            notes = extraFields.second,
            error = rawData.errors.joinToString(separator = "; ")
        )
    }
}


private fun ByteArray.toHex(): String =
    joinToString(separator = "") { "%02X".format(Locale.US, it) }
