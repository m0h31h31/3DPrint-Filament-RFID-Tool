package com.m0h31h31.bamburfidreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.SnapmakerShareTagItem
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.AppSearchBar
import com.m0h31h31.bamburfidreader.ui.components.NeuButton
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle

private val snapItemShape = RoundedCornerShape(24.dp)

private val SNAPMAKER_MAIN_TYPE_NAMES = mapOf(
    0 to "Reserved", 1 to "PLA", 2 to "PETG", 3 to "ABS", 4 to "TPU", 5 to "PVA"
)

private val SNAPMAKER_SUB_TYPE_NAMES = mapOf(
    0 to "Reserved", 1 to "Basic", 2 to "Matte", 3 to "SnapSpeed",
    4 to "Silk", 5 to "Support", 6 to "HF", 7 to "95A", 8 to "95A HF"
)

private fun snapMainTypeName(code: Int): String =
    SNAPMAKER_MAIN_TYPE_NAMES[code] ?: "Unknown($code)"

private fun snapSubTypeName(code: Int): String =
    SNAPMAKER_SUB_TYPE_NAMES[code] ?: "Unknown($code)"

private fun snapFullTypeName(mainType: Int, subType: Int): String {
    val main = snapMainTypeName(mainType)
    val sub = snapSubTypeName(subType)
    return if (sub == "Reserved" || sub.startsWith("Unknown")) main else "$main $sub"
}

private data class SnapCategoryGroup(
    val materialType: String,
    val items: List<SnapmakerShareTagItem>
)

private fun buildSnapCategoryGroups(items: List<SnapmakerShareTagItem>): List<SnapCategoryGroup> =
    items
        .groupBy { snapMainTypeName(it.mainType).ifBlank { "未知" } }
        .entries
        .sortedBy { it.key }
        .map { (type, groupItems) ->
            SnapCategoryGroup(
                materialType = type,
                items = groupItems.sortedWith(
                    compareBy<SnapmakerShareTagItem> { snapSubTypeName(it.subType).ifBlank { "\uFFFF" } }
                        .thenByDescending { it.mfDate.ifBlank { "" } }
                )
            )
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapmakerTagScreen(
    items: List<SnapmakerShareTagItem> = emptyList(),
    loading: Boolean = false,
    writeStatusMessage: String = "",
    writeInProgress: Boolean = false,
    tagViewMode: String = "list",
    onStartWrite: (SnapmakerShareTagItem) -> Unit = {},
    onDelete: (SnapmakerShareTagItem) -> String = { "" },
    onCancelWrite: () -> Unit = {},
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalAppUiStyle.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var hintMessage by remember { mutableStateOf("") }
    var pendingDeleteItem by remember { mutableStateOf<SnapmakerShareTagItem?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    var expandedCategoryKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    var noticesExpanded by rememberSaveable {
        val saved = context.getSharedPreferences("ui_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("snapmaker_notices_expanded", true)
        mutableStateOf(saved)
    }

    LaunchedEffect(loading) {
        if (!loading && isRefreshing) isRefreshing = false
    }

    val filteredItems = remember(items, query) {
        val q = query.trim().lowercase()
        val filtered = if (q.isBlank()) items
        else items.filter { item ->
            item.uid.lowercase().contains(q) ||
            item.vendor.lowercase().contains(q) ||
            item.manufacturer.lowercase().contains(q) ||
            snapMainTypeName(item.mainType).lowercase().contains(q) ||
            snapSubTypeName(item.subType).lowercase().contains(q) ||
            snapFullTypeName(item.mainType, item.subType).lowercase().contains(q) ||
            item.mfDate.contains(q)
        }
        filtered.sortedWith(
            compareBy<SnapmakerShareTagItem> { snapMainTypeName(it.mainType).ifBlank { "\uFFFF" } }
                .thenBy { snapSubTypeName(it.subType).ifBlank { "\uFFFF" } }
                .thenByDescending { it.mfDate.ifBlank { "" } }
        )
    }

    val categories = remember(filteredItems) { buildSnapCategoryGroups(filteredItems) }
    val selectedItem = items.firstOrNull { it.uid == selectedUid }

    Surface(modifier = modifier.fillMaxSize().neuBackground(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search bar
            AppSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索品牌/类型/日期",
                modifier = Modifier.fillMaxWidth()
            )

            if (hintMessage.isNotBlank()) {
                Text(text = hintMessage, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "共 ${items.size} 个标签，显示 ${filteredItems.size} 个",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            NeuPanel(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp)
            ) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        if (!isRefreshing && !loading) {
                            isRefreshing = true
                            onRefresh()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (tagViewMode == "category") {
                            SnapCategoryView(
                                categories = categories,
                                selectedUid = selectedUid,
                                expandedCategoryKeys = expandedCategoryKeys,
                                onExpandedCategoryKeysChange = { expandedCategoryKeys = it },
                                onSelect = { item -> selectedUid = item.uid }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredItems, key = { it.uid }) { item ->
                                    SnapTagListItem(
                                        item = item,
                                        selected = item.uid == selectedUid,
                                        onSelect = { selectedUid = item.uid },
                                        onDeleteRequest = { pendingDeleteItem = item },
                                        uiStyle = uiStyle
                                    )
                                }
                            }
                        }
                        if (loading) {
                            Row(
                                modifier = Modifier.align(Alignment.Center).padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppCircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp))
                                Text(text = "加载中...", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Notice panel
            NeuPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(9.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val v = !noticesExpanded
                            noticesExpanded = v
                            context.getSharedPreferences("ui_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("snapmaker_notices_expanded", v).apply()
                        },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "写入注意事项",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (noticesExpanded) "▲" else "▼",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    AnimatedVisibility(visible = noticesExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        Column(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("请使用空白快造标签卡写入", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
                            Text("写入后将无法恢复为空白卡", fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (selectedItem != null) {
                Text(
                    text = "已选择：${snapFullTypeName(selectedItem.mainType, selectedItem.subType)}  ${selectedItem.uid}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Write button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeuButton(
                    text = if (writeInProgress) "取消写入" else "开始写入",
                    onClick = {
                        if (writeInProgress) {
                            onCancelWrite()
                        } else {
                            val item = selectedItem
                            if (item != null) onStartWrite(item)
                            else hintMessage = "请先选择一个标签"
                        }
                    },
                    enabled = writeInProgress || selectedItem != null,
                    modifier = Modifier.weight(1f)
                )
            }

            if (writeStatusMessage.isNotBlank()) {
                val statusColor = when {
                    writeStatusMessage.contains("成功", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                    writeStatusMessage.contains("失败", ignoreCase = true) ||
                        writeStatusMessage.contains("error", ignoreCase = true) -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val inProgress = writeStatusMessage.contains("正在") || writeStatusMessage.contains("请将")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inProgress) AppCircularProgressIndicator(modifier = Modifier.size(15.dp))
                    Text(text = writeStatusMessage, fontSize = 13.sp, color = statusColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Delete dialog
        val deleteTarget = pendingDeleteItem
        if (deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { pendingDeleteItem = null },
                title = { Text("删除确认") },
                text = { Text("确定删除该标签记录？(${deleteTarget.uid})") },
                confirmButton = {
                    TextButton(onClick = {
                        val message = onDelete(deleteTarget)
                        hintMessage = message
                        if (selectedUid == deleteTarget.uid) selectedUid = null
                        pendingDeleteItem = null
                    }) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteItem = null }) { Text(stringResource(R.string.action_cancel)) }
                }
            )
        }
    }
}

// ── List item ────────────────────────────────────────────────────────────────

@Composable
private fun SnapTagListItem(
    item: SnapmakerShareTagItem,
    selected: Boolean,
    onSelect: () -> Unit,
    onDeleteRequest: () -> Unit,
    uiStyle: AppUiStyle
) {
    val selectedFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
    val titleColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDeleteRequest()
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val deleteColor = if (uiStyle == AppUiStyle.MIUIX) MaterialTheme.colorScheme.errorContainer else Color(0xFFE54D4D)
            val deleteTextColor = if (uiStyle == AppUiStyle.MIUIX) MaterialTheme.colorScheme.onErrorContainer else Color.White
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 3.dp)
                    .clip(snapItemShape).background(deleteColor).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.bodyMedium,
                    color = deleteTextColor, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        NeuPanel(
            modifier = Modifier.fillMaxWidth().clickable { onSelect() },
            shape = snapItemShape,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(snapItemShape)
                    .background(if (selected) selectedFillColor else Color.Transparent)
                    .border(if (selected) 1.dp else 0.dp, if (selected) selectedBorderColor else Color.Transparent, snapItemShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(999.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)))
                // Color swatch
                if (item.rgb1 != 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000 or item.rgb1.toLong()))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            snapFullTypeName(item.mainType, item.subType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = titleColor
                        )
                        if (item.mfDate.isNotBlank()) {
                            Text(item.mfDate, fontSize = 10.sp, color = subtitleColor)
                        }
                    }
                    Text(
                        text = "${item.uid}  ${item.vendor.ifBlank { "-" }}",
                        fontSize = 12.sp, color = subtitleColor
                    )
                }
            }
        }
    }
}

// ── Category view ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SnapCategoryView(
    categories: List<SnapCategoryGroup>,
    selectedUid: String?,
    expandedCategoryKeys: List<String>,
    onExpandedCategoryKeysChange: (List<String>) -> Unit,
    onSelect: (SnapmakerShareTagItem) -> Unit
) {
    var dialogGroup by remember { mutableStateOf<SnapCategoryGroup?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { category ->
            val key = category.materialType
            val isExpanded = key in expandedCategoryKeys

            stickyHeader(key = "header_$key") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .clickable {
                            onExpandedCategoryKeysChange(
                                if (isExpanded) expandedCategoryKeys - key
                                else expandedCategoryKeys + key
                            )
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.materialType,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${category.items.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isExpanded) {
                item(key = "items_$key") {
                    // Show items as UID chips grouped by mfDate
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Group by vendor for sub-grouping inside category
                        val byVendor = category.items.groupBy { it.vendor.ifBlank { "-" } }
                        byVendor.forEach { (vendor, vendorItems) ->
                            if (byVendor.size > 1) {
                                Text(
                                    text = vendor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                vendorItems.forEach { item ->
                                    val isSelected = item.uid == selectedUid
                                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                    val chipText = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                    Box(
                                        modifier = Modifier
                                            .background(chipBg, RoundedCornerShape(6.dp))
                                            .clickable { onSelect(item) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            // Color swatch + subtype row
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (item.rgb1 != 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF000000 or item.rgb1.toLong()))
                                                            .border(0.5.dp, chipText.copy(alpha = 0.3f), CircleShape)
                                                    )
                                                }
                                                val subName = snapSubTypeName(item.subType)
                                                if (subName != "Reserved" && !subName.startsWith("Unknown")) {
                                                    Text(
                                                        text = subName,
                                                        fontSize = 9.sp,
                                                        color = chipText.copy(alpha = 0.85f),
                                                        lineHeight = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            Text(
                                                text = item.uid,
                                                fontSize = 11.sp,
                                                color = chipText,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                lineHeight = 13.sp
                                            )
                                            if (item.mfDate.isNotBlank()) {
                                                Text(
                                                    text = item.mfDate,
                                                    fontSize = 9.sp,
                                                    color = chipText.copy(alpha = 0.75f),
                                                    lineHeight = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
