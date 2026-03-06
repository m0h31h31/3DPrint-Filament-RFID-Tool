package com.m0h31h31.bamburfidreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import kotlinx.coroutines.delay

private enum class StatusTone {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

private fun resolveStatusTone(message: String): StatusTone {
    val text = message.lowercase()
    return when {
        listOf("失败", "错误", "异常", "取消", "不可用").any { it in text } -> StatusTone.ERROR
        listOf("成功", "完成", "已保存", "已打包", "已停止", "已导入").any { it in text } -> StatusTone.SUCCESS
        listOf("提醒", "警告", "请", "等待", "准备", "覆盖").any { it in text } -> StatusTone.WARNING
        else -> StatusTone.INFO
    }
}

@Composable
private fun statusToneColor(tone: StatusTone): Color {
    return when (tone) {
        StatusTone.SUCCESS -> Color(0xFF2E8B57)
        StatusTone.ERROR -> MaterialTheme.colorScheme.error
        StatusTone.WARNING -> Color(0xFFB7791F)
        StatusTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Preview
@Composable
fun MiscScreen(
    onBackupDatabase: () -> String = { "" },
    onImportDatabase: () -> String = { "" },
    onClearFuid: () -> String = { "" },
    onCancelClearFuid: () -> String = { "" },
    onResetDatabase: () -> String = { "" },
    miscStatusMessage: String = "",
    onExportTagPackage: () -> String = { "" },
    onSelectImportTagPackage: () -> String = { "" },
    appConfigMessage: String = "",
    readAllSectors: Boolean = false,
    onReadAllSectorsChange: (Boolean) -> Unit = {},
    saveKeysToFile: Boolean = false,
    onSaveKeysToFileChange: (Boolean) -> Unit = {},
    formatTagDebugEnabled: Boolean = false,
    onFormatTagDebugEnabledChange: (Boolean) -> Unit = {},
    forceOverwriteImport: Boolean = false,
    onForceOverwriteImportChange: (Boolean) -> Unit = {},
    formatInProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val boostLink =
        "bambulab://bbl/design/model/detail?design_id=2020787&instance_id=2253290&appSharePlatform=copy"
    var message by remember { mutableStateOf("") }
    var visibleStatusMessage by remember { mutableStateOf("") }
    var lastMiscStatusMessage by remember { mutableStateOf(miscStatusMessage) }
    var lastPageMessage by remember { mutableStateOf(message) }
    var showReadAllSectorsDialog by remember { mutableStateOf(false) }
    var showImportDatabaseConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(miscStatusMessage, message) {
        val trimmedMiscStatus = miscStatusMessage.trim()
        val trimmedPageMessage = message.trim()
        val nextMessage = when {
            trimmedPageMessage != lastPageMessage && trimmedPageMessage.isNotBlank() -> trimmedPageMessage
            trimmedMiscStatus != lastMiscStatusMessage && trimmedMiscStatus.isNotBlank() -> trimmedMiscStatus
            trimmedPageMessage.isNotBlank() -> trimmedPageMessage
            else -> trimmedMiscStatus
        }
        lastMiscStatusMessage = trimmedMiscStatus
        lastPageMessage = trimmedPageMessage
        if (nextMessage.isBlank()) {
            visibleStatusMessage = ""
            return@LaunchedEffect
        }
        visibleStatusMessage = nextMessage
        delay(10000)
        if (visibleStatusMessage == nextMessage) {
            visibleStatusMessage = ""
        }
    }

    fun handleReadAllSectorsChange(checked: Boolean) {
        if (checked) {
            showReadAllSectorsDialog = true
        } else {
            onReadAllSectorsChange(false)
        }
    }

    fun confirmReadAllSectors() {
        showReadAllSectorsDialog = false
        onReadAllSectorsChange(true)
    }

    fun confirmImportDatabase() {
        showImportDatabaseConfirmDialog = false
        message = onImportDatabase()
    }

    Surface(
        modifier = modifier.fillMaxSize().neuBackground(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val visibleStatusColor = statusToneColor(resolveStatusTone(visibleStatusMessage))
            if (appConfigMessage.isNotBlank() || visibleStatusMessage.isNotBlank()) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (appConfigMessage.isNotBlank()) {
                            Text(
                                text = appConfigMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (visibleStatusMessage.isNotBlank()) {
                            Text(
                                text = visibleStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = visibleStatusColor
                            )
                        }
                    }
                }
            }

            NeuPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(text = "读取全部扇区数据并保存文件")
                        Switch(
                            checked = readAllSectors,
                            onCheckedChange = ::handleReadAllSectorsChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(text = "保存秘钥到文件")
                        Switch(
                            checked = saveKeysToFile,
                            onCheckedChange = onSaveKeysToFileChange
                        )
                    }
                }
            }

            if (showReadAllSectorsDialog) {
                AlertDialog(
                    onDismissRequest = { showReadAllSectorsDialog = false },
                    title = { Text(text = "读取全部数据提醒") },
                    text = {
                        Text(
                            text = "读取全部数据会影响读取速度，数据会保存在包名(Android/data/com.m0h31h31.bamburfidreader/)下的 rfid_file/self_xxxxx 文件夹下。确定要开启吗？"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = ::confirmReadAllSectors) {
                            Text(text = "确定")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReadAllSectorsDialog = false }) {
                            Text(text = "取消")
                        }
                    }
                )
            }

            if (showImportDatabaseConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDatabaseConfirmDialog = false },
                    title = { Text(text = "确认导入数据库") },
                    text = {
                        Text(text = "导入数据库会覆盖当前本地耗材数据库内容。确定继续吗？")
                    },
                    confirmButton = {
                        TextButton(onClick = ::confirmImportDatabase) {
                            Text(text = "确定导入")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDatabaseConfirmDialog = false }) {
                            Text(text = "取消")
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeuButton(
                    text = stringResource(R.string.action_backup_db),
                    onClick = { message = onBackupDatabase() },
                    modifier = Modifier.weight(1f)
                )
                NeuButton(
                    text = stringResource(R.string.action_import_db),
                    onClick = { showImportDatabaseConfirmDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            NeuButton(
                text = if (formatInProgress) "取消格式化" else "格式化标签",
                onClick = {
                    message = if (formatInProgress) {
                        onCancelClearFuid()
                    } else {
                        onClearFuid()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            NeuPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = "显示格式化标签调试信息")
                    Switch(
                        checked = formatTagDebugEnabled,
                        onCheckedChange = onFormatTagDebugEnabledChange
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeuButton(
                    text = "打包标签数据",
                    onClick = { message = onExportTagPackage() },
                    modifier = Modifier.weight(1f)
                )
                NeuButton(
                    text = "导入标签包",
                    onClick = { message = onSelectImportTagPackage() },
                    modifier = Modifier.weight(1f)
                )
            }

            NeuPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = "强制覆盖导入标签")
                    Switch(
                        checked = forceOverwriteImport,
                        onCheckedChange = onForceOverwriteImportChange
                    )
                }
            }

            TextButton(onClick = { uriHandler.openUri(boostLink) }) {
                Text(text = stringResource(R.string.action_boost_open_bambu))
            }
        }
    }
}
