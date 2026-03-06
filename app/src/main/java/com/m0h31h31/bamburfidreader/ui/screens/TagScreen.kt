package com.m0h31h31.bamburfidreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.ShareTagItem
import com.m0h31h31.bamburfidreader.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.m0h31h31.bamburfidreader.ui.components.ColorSwatch
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.NeuTextField
import com.m0h31h31.bamburfidreader.ui.components.neuBackground

private val tagItemShape = RoundedCornerShape(24.dp)

private fun uidDisplayName(fileName: String): String = fileName.removeSuffix(".txt")

private fun tokenizeSearchQuery(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val buffer = StringBuilder()

    fun charType(c: Char): Int = when {
        c.isWhitespace() -> 0
        c.isLetterOrDigit() && c.code < 128 -> 1
        c.isLetterOrDigit() -> 2
        else -> 0
    }

    var lastType = -1
    for (c in input.trim()) {
        val type = charType(c)
        if (type == 0) {
            if (buffer.isNotEmpty()) {
                tokens += buffer.toString()
                buffer.clear()
            }
            lastType = -1
            continue
        }
        if (lastType != -1 && type != lastType && buffer.isNotEmpty()) {
            tokens += buffer.toString()
            buffer.clear()
        }
        buffer.append(c)
        lastType = type
    }
    if (buffer.isNotEmpty()) {
        tokens += buffer.toString()
    }
    return tokens.map { it.lowercase() }.filter { it.isNotBlank() }
}

@Preview
@Composable
fun TagScreen(
    items: List<ShareTagItem> = emptyList(),
    loading: Boolean = false,
    preselectedFileName: String? = null,
    refreshStatusMessage: String = "",
    writeStatusMessage: String = "",
    writeInProgress: Boolean = false,
    onRefresh: () -> String = { "" },
    onStartWrite: (ShareTagItem) -> Unit = {},
    onDelete: (ShareTagItem) -> String = { "" },
    onCancelWrite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var hintMessage by remember { mutableStateOf("") }
    var pendingDeleteItem by remember { mutableStateOf<ShareTagItem?>(null) }

    LaunchedEffect(preselectedFileName, items) {
        val target = preselectedFileName
        if (!target.isNullOrBlank()) {
            val matched = items.firstOrNull { it.fileName == target }
            if (matched != null) {
                selectedFileName = matched.relativePath
            }
        }
    }

    val filteredItems = remember(items, query) {
        val tokens = tokenizeSearchQuery(query)
        if (tokens.isEmpty()) {
            items
        } else {
            items.filter { item ->
                val fields = listOf(
                    item.materialType.lowercase(),
                    item.colorName.lowercase(),
                    item.colorUid.lowercase(),
                    uidDisplayName(item.fileName).lowercase()
                )
                tokens.all { token -> fields.any { it.contains(token) } }
            }
        }
    }
    val selectedItem = items.firstOrNull { it.relativePath == selectedFileName }

    Surface(
        modifier = modifier.fillMaxSize().neuBackground(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "按耗材/颜色筛选(支持 pla红)",
                    modifier = Modifier.weight(1f)
                )
                NeuButton(
                    text = "刷新",
                    onClick = { hintMessage = onRefresh() },
                    modifier = Modifier.width(88.dp)
                )
            }

            if (hintMessage.isNotBlank()) {
                Text(
                    text = hintMessage,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (refreshStatusMessage.isNotBlank()) {
                Text(
                    text = refreshStatusMessage,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "总数: ${items.size}    当前筛选: ${filteredItems.size}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NeuPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredItems, key = { it.relativePath }) { item ->
                            val selected = item.relativePath == selectedFileName
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        pendingDeleteItem = item
                                        false
                                    } else {
                                        false
                                    }
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 3.dp)
                                            .clip(tagItemShape)
                                            .background(Color(0xFFE54D4D))
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "删除",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            ) {
                                NeuPanel(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFileName = item.relativePath },
                                    shape = tagItemShape,
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.materialType.ifBlank { "未知" },
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "UID: ${uidDisplayName(item.fileName)}    颜色ID: ${item.colorUid.ifBlank { "未知" }}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.colorName.ifBlank { "未知颜色" },
                                                fontSize = 12.sp
                                            )
                                            ColorSwatch(
                                                colorValues = item.colorValues,
                                                colorType = item.colorType,
                                                modifier = Modifier.width(42.dp).height(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (loading) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                            Text(text = "正在加载共享数据...", fontSize = 11.sp)
                        }
                    }
                }
            }

            NeuPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(9.dp)
            ) {
                Column(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "写入前请严格遵守：",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "1. 标签必须紧贴手机 NFC 区域，写入过程中不要移动",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "2. 写入完成按提示,移开标签,再贴上识别验证",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "3. 不要写入已有的标签,相同的标签会被识别为一卷料",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "4. 写入可能失败!作者不对任何后果负责!!!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (selectedItem != null) {
                Text(
                    text = "当前选择: ${uidDisplayName(selectedItem.fileName)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val deleteTarget = pendingDeleteItem
            if (deleteTarget != null) {
                AlertDialog(
                    onDismissRequest = { pendingDeleteItem = null },
                    title = { Text("确认删除") },
                    text = { Text("确定删除标签 ${uidDisplayName(deleteTarget.fileName)} 吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val message = onDelete(deleteTarget)
                                hintMessage = message
                                val deleted = message.startsWith("删除成功") || message.contains("已从列表移除")
                                if (deleted && selectedFileName == deleteTarget.relativePath) {
                                    selectedFileName = null
                                }
                                pendingDeleteItem = null
                            }
                        ) {
                            Text("删除")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeleteItem = null }) {
                            Text("取消")
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuButton(
                    text = "开始写入",
                    onClick = {
                        val item = selectedItem
                        if (item != null) {
                            onStartWrite(item)
                        } else {
                            hintMessage = "请先选择一条数据"
                        }
                    },
                    enabled = !writeInProgress && selectedItem != null,
                    modifier = Modifier.weight(1f)
                )
                NeuButton(
                    text = "取消写入",
                    onClick = onCancelWrite,
                    enabled = writeInProgress,
                    modifier = Modifier.weight(1f)
                )
            }

            if (writeStatusMessage.isNotBlank()) {
                val statusColor = when {
                    writeStatusMessage.contains("成功") -> MaterialTheme.colorScheme.primary
                    writeStatusMessage.contains("失败") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = writeStatusMessage,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
