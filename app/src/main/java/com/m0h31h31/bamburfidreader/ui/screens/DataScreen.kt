package com.m0h31h31.bamburfidreader.ui.screens

import com.m0h31h31.bamburfidreader.ui.components.AppAlertDialog
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m0h31h31.bamburfidreader.R
import com.m0h31h31.bamburfidreader.data.FilamentDbHelper
import com.m0h31h31.bamburfidreader.model.InventoryItem
import com.m0h31h31.bamburfidreader.ui.components.ColorSwatch
import com.m0h31h31.bamburfidreader.ui.components.AppSwitch
import com.m0h31h31.bamburfidreader.ui.components.AppCircularProgressIndicator
import com.m0h31h31.bamburfidreader.ui.components.NeuPanel
import com.m0h31h31.bamburfidreader.ui.components.ModernCard
import com.m0h31h31.bamburfidreader.ui.components.ModernPillButton
import com.m0h31h31.bamburfidreader.ui.components.ModernSectionHeader
import com.m0h31h31.bamburfidreader.ui.components.ModernWorkbenchTokens
import com.m0h31h31.bamburfidreader.ui.components.neuBackground
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import com.m0h31h31.bamburfidreader.util.parseColorValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class StackedColorGroup(
    val stackKey: String,
    val displayItem: InventoryItem,
    val items: List<InventoryItem>
) {
    val count: Int get() = items.size
    val remainingGrams: Int? get() = totalRemainingGrams(items.map { it.remainingGrams })
}

internal fun totalRemainingGrams(values: List<Int?>): Int? {
    if (values.isEmpty() || values.any { it == null }) return null
    return values.sumOf { it ?: 0 }
}

private data class DataRenderGroup(
    val materialType: String,
    val totalCount: Int,
    val sortedItems: List<InventoryItem>,
    val stackedGroups: List<StackedColorGroup>
)

private fun buildColorStackKey(item: InventoryItem): String {
    val normalizedValues = item.colorValues
        .map { it.trim().uppercase() }
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(",")
    return listOf(
        item.colorType.trim().lowercase(),
        item.colorName.trim().lowercase(),
        if (normalizedValues.isNotBlank()) normalizedValues else item.colorCode.trim().lowercase()
    ).joinToString("|")
}

private fun colorSortValue(item: InventoryItem): Long {
    val raw = (item.colorValues.firstOrNull() ?: item.colorCode)
        .trim()
        .removePrefix("#")
        .uppercase()
    return raw.toLongOrNull(16) ?: Long.MAX_VALUE
}

private fun calculateBrightness(color: Color): Float {
    return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
}

private fun getTextColorForBackground(colorValues: List<String>, colorCode: String): Color {
    val firstColor = if (colorValues.isNotEmpty()) {
        parseColorValue(colorValues[0].trim())
    } else {
        parseColorValue(colorCode.trim())
    }
    if (firstColor != null) {
        if (firstColor.alpha <= 0.5f) return Color.Black
        return if (calculateBrightness(firstColor) < 0.5f) Color.White else Color.Black
    }
    return Color.Black
}

private val swatchShape = RoundedCornerShape(14.dp)
private const val DATA_SCREEN_PREFS = "data_screen_prefs"
private const val KEY_USE_DETAILED_CLASSIFICATION = "use_detailed_classification"
private const val KEY_MERGE_SAME_COLOR_ITEMS = "merge_same_color_items"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DataScreen(dbHelper: FilamentDbHelper?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uiStyle = LocalAppUiStyle.current
    val unknownColorText = stringResource(R.string.data_unknown_color)
    val prefs = remember(context) {
        context.getSharedPreferences(DATA_SCREEN_PREFS, Context.MODE_PRIVATE)
    }
    val groupedItems = remember { mutableStateOf(mapOf<String, List<InventoryItem>>()) }
    val isLoading = remember { mutableStateOf(true) }
    val useDetailedClassification = remember {
        mutableStateOf(prefs.getBoolean(KEY_USE_DETAILED_CLASSIFICATION, false))
    }
    val mergeSameColorItems = remember {
        mutableStateOf(prefs.getBoolean(KEY_MERGE_SAME_COLOR_ITEMS, false))
    }
    val activeStackDialog = remember { mutableStateOf<StackedColorGroup?>(null) }
    val isModernWorkbench = uiStyle == AppUiStyle.MODERN_WORKBENCH ||
            uiStyle == AppUiStyle.MODERN_WORKBENCH_COMPOSE
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(useDetailedClassification.value) {
        prefs.edit().putBoolean(
            KEY_USE_DETAILED_CLASSIFICATION,
            useDetailedClassification.value
        ).apply()
    }

    LaunchedEffect(mergeSameColorItems.value) {
        prefs.edit().putBoolean(
            KEY_MERGE_SAME_COLOR_ITEMS,
            mergeSameColorItems.value
        ).apply()
    }

    LaunchedEffect(dbHelper, useDetailedClassification.value) {
        val db = dbHelper?.readableDatabase
        if (db != null) {
            val items = withContext(Dispatchers.IO) { dbHelper.getAllInventory(db) }
            val grouped = if (useDetailedClassification.value) {
                items.groupBy { item -> item.materialDetailedType.ifBlank { context.getString(R.string.data_unknown_group) } }
            } else {
                items.groupBy { item -> item.materialType.ifBlank { context.getString(R.string.data_unknown_group) } }
            }
            val sortedKeys = grouped.keys.sortedWith { a, b ->
                val countA = grouped[a]?.size ?: 0
                val countB = grouped[b]?.size ?: 0
                if (countA != countB) countB.compareTo(countA) else a.compareTo(b)
            }
            groupedItems.value = sortedKeys.associateWith { grouped[it].orEmpty() }
        } else {
            groupedItems.value = emptyMap()
        }
        isLoading.value = false
    }

    val visibleGroups = remember(groupedItems.value, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) {
            groupedItems.value
        } else {
            groupedItems.value.mapValues { (_, items) ->
                items.filter { item ->
                    listOf(
                        item.materialType,
                        item.materialDetailedType,
                        item.colorName,
                        item.colorCode,
                        item.filaColorCode
                    ).any { it.lowercase().contains(query) }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .neuBackground()
            .statusBarsPadding()
            .padding(horizontal = if (isModernWorkbench) 14.dp else 12.dp, vertical = 10.dp)
    ) {
        if (isModernWorkbench) {
            val totalItems = groupedItems.value.values.sumOf { it.size }
            val visibleItems = visibleGroups.values.sumOf { it.size }
            ModernDataHeader(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                totalItems = totalItems,
                visibleItems = visibleItems,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        val controls: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.data_grouping))
                AppSwitch(
                    checked = useDetailedClassification.value,
                    onCheckedChange = { useDetailedClassification.value = it }
                )
                Text(
                    text = if (useDetailedClassification.value) {
                        stringResource(R.string.data_grouping_detailed)
                    } else {
                        stringResource(R.string.data_grouping_simple)
                    }
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.data_merge))
                AppSwitch(
                    checked = mergeSameColorItems.value,
                    onCheckedChange = { mergeSameColorItems.value = it }
                )
                Text(
                    text = if (mergeSameColorItems.value) {
                        stringResource(R.string.data_switch_on)
                    } else {
                        stringResource(R.string.data_switch_off)
                    }
                )
            }
        }
        if (!isModernWorkbench) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = { controls() }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    stringResource(R.string.data_grouping_simple) to false,
                    stringResource(R.string.data_grouping_detailed) to true
                ).forEach { (label, detailed) ->
                    ModernPillButton(
                        text = label,
                        selected = useDetailedClassification.value == detailed,
                        onClick = { useDetailedClassification.value = detailed },
                        modifier = Modifier.weight(1f)
                    )
                }
                ModernPillButton(
                    text = stringResource(R.string.data_merge),
                    selected = mergeSameColorItems.value,
                    onClick = { mergeSameColorItems.value = !mergeSameColorItems.value },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.padding(top = 3.dp))

        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppCircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(text = stringResource(R.string.data_loading), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (visibleGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.data_empty))
            }
        } else {
            val renderGroups = remember(visibleGroups, mergeSameColorItems.value) {
                visibleGroups.map { (materialType, items) ->
                    val sortedItems = items.sortedByDescending { colorSortValue(it) }
                    val stackedGroups = if (mergeSameColorItems.value) {
                        sortedItems.groupBy { buildColorStackKey(it) }
                            .map { (stackKey, grouped) ->
                                StackedColorGroup(stackKey, grouped.first(), grouped)
                            }
                            .sortedByDescending { colorSortValue(it.displayItem) }
                    } else {
                        emptyList()
                    }
                    DataRenderGroup(materialType, items.size, sortedItems, stackedGroups)
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(renderGroups, key = { it.materialType }) { group ->
                    run {
                        val materialType = group.materialType
                        val sortedItems = group.sortedItems
                        val stackedGroups = group.stackedGroups

                        val sectionContent: @Composable () -> Unit = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isModernWorkbench) {
                                    ModernSectionHeader(
                                        title = stringResource(
                                            R.string.data_group_title_format,
                                            materialType,
                                            group.totalCount
                                        )
                                    )
                                } else {
                                    Text(
                                        text = stringResource(
                                            R.string.data_group_title_format,
                                            materialType,
                                            group.totalCount
                                        ),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val cellWidth = if (isModernWorkbench) 62.dp else 58.dp
                                    val minGap = if (isModernWorkbench) 10.dp else 6.dp
                                    val columns = ((maxWidth + minGap) / (cellWidth + minGap))
                                        .toInt()
                                        .coerceAtLeast(1)
                                    if (!mergeSameColorItems.value) {
                                        SwatchGrid(
                                            cellWidth = cellWidth,
                                            columns = columns,
                                            itemCount = sortedItems.size
                                        ) { index ->
                                            val item = sortedItems[index]
                                            if (isModernWorkbench) {
                                                ModernSwatchCell(
                                                    width = cellWidth,
                                                    colorValues = item.colorValues,
                                                    colorCode = item.colorCode,
                                                    colorType = item.colorType,
                                                    title = item.resolvedColorName(),
                                                    code = item.filaColorCode.ifBlank { item.colorCode },
                                                    remainingGramsText = item.remainingGrams?.let {
                                                        stringResource(R.string.data_remaining_grams_format, it)
                                                    } ?: "-"
                                                )
                                            } else {
                                                SwatchCell(
                                                    size = cellWidth,
                                                    colorValues = item.colorValues,
                                                    colorCode = item.colorCode,
                                                    colorType = item.colorType,
                                                    title = item.resolvedColorName(),
                                                    subtitle = String.format("%.1f%%", item.remainingPercent)
                                                )
                                            }
                                        }
                                    } else {
                                        SwatchGrid(
                                            cellWidth = cellWidth,
                                            columns = columns,
                                            itemCount = stackedGroups.size
                                        ) { index ->
                                            val stack = stackedGroups[index]
                                            if (isModernWorkbench) {
                                                ModernSwatchCell(
                                                    width = cellWidth,
                                                    colorValues = stack.displayItem.colorValues,
                                                    colorCode = stack.displayItem.colorCode,
                                                    colorType = stack.displayItem.colorType,
                                                    title = stack.displayItem.resolvedColorName(),
                                                    code = stack.displayItem.filaColorCode.ifBlank { stack.displayItem.colorCode },
                                                    remainingGramsText = stack.remainingGrams?.let {
                                                        stringResource(R.string.data_remaining_grams_format, it)
                                                    } ?: "-",
                                                    badgeText = if (stack.count > 1) "${stack.count}" else null,
                                                    modifier = Modifier.clickable {
                                                        if (stack.count > 1) {
                                                            activeStackDialog.value = stack
                                                        }
                                                    }
                                                )
                                            } else {
                                                SwatchCell(
                                                    size = cellWidth,
                                                    colorValues = stack.displayItem.colorValues,
                                                    colorCode = stack.displayItem.colorCode,
                                                    colorType = stack.displayItem.colorType,
                                                    title = stack.displayItem.resolvedColorName(),
                                                    subtitle = if (stack.count > 1) null else String.format("%.1f%%", stack.displayItem.remainingPercent),
                                                    badgeText = if (stack.count > 1) "${stack.count}" else null,
                                                    modifier = Modifier.clickable {
                                                        if (stack.count > 1) {
                                                            activeStackDialog.value = stack
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (isModernWorkbench) {
                            ModernCard(
                                modifier = Modifier.fillMaxWidth(),
                                radius = 14.dp
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    sectionContent()
                                }
                            }
                        } else {
                            NeuPanel(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                sectionContent()
                            }
                        }
                    }
                }
            }
        }
    }

    val dialogStack = activeStackDialog.value
    if (dialogStack != null) {
        AppAlertDialog(
            onDismissRequest = { activeStackDialog.value = null },
            title = {
                Text(
                    text = stringResource(
                        R.string.data_stack_title_format,
                        dialogStack.displayItem.resolvedColorName().ifBlank {
                            context.getString(R.string.data_unknown_color)
                        },
                        dialogStack.count
                    ),
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(dialogStack.items) { item ->
                        NeuPanel(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ColorSwatch(
                                    colorValues = item.colorValues,
                                    colorType = item.colorType,
                                    modifier = Modifier.size(30.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.resolvedColorName().ifBlank {
                                            unknownColorText
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.data_uid_short,
                                            item.trayUid.takeLast(8)
                                        ),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = String.format("%.1f%%", item.remainingPercent),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeStackDialog.value = null }) {
                    Text(stringResource(R.string.data_close))
                }
            }
        )
    }
}

@Composable
private fun ModernDataHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    totalItems: Int,
    visibleItems: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = ModernWorkbenchTokens.Card,
                border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⌕",
                        color = ModernWorkbenchTokens.Ink,
                        style = MaterialTheme.typography.titleLarge
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = ModernWorkbenchTokens.Ink
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (query.isBlank()) {
                                Text(
                                    text = stringResource(R.string.data_search_placeholder),
                                    color = ModernWorkbenchTokens.Muted,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
            Text(
                text = "☰",
                color = ModernWorkbenchTokens.Ink,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.data_total_count),
                color = ModernWorkbenchTokens.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "  $totalItems",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "  |  ",
                color = ModernWorkbenchTokens.Line,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.data_current_filter),
                color = ModernWorkbenchTokens.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "  $visibleItems",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SwatchGrid(
    cellWidth: Dp,
    columns: Int,
    itemCount: Int,
    itemContent: @Composable (Int) -> Unit
) {
    val rowSpacing = 6.dp
    Column(verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
        val rows = (0 until itemCount).chunked(columns)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { index ->
                    Box(modifier = Modifier.width(cellWidth), contentAlignment = Alignment.Center) {
                        itemContent(index)
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.width(cellWidth))
                }
            }
        }
    }
}

@Composable
private fun ModernSwatchCell(
    width: Dp,
    colorValues: List<String>,
    colorCode: String,
    colorType: String,
    title: String,
    code: String,
    remainingGramsText: String,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(width)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp),
            shape = RoundedCornerShape(10.dp),
            color = ModernWorkbenchTokens.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, ModernWorkbenchTokens.Line)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    ColorSwatch(
                        colorValues = colorValues,
                        colorType = colorType,
                        modifier = Modifier.fillMaxSize(),
                        shape = RectangleShape
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = title,
                        color = ModernWorkbenchTokens.Ink,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = code.ifBlank { "-" },
                        color = Color(0xFF888888),
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = remainingGramsText,
                        color = Color(0xFF888888),
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .background(Color(0xCC26313F), shape = RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun SwatchCell(
    size: Dp,
    colorValues: List<String>,
    colorCode: String,
    colorType: String,
    title: String,
    subtitle: String?,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        ColorSwatch(
            colorValues = colorValues,
            colorType = colorType,
            modifier = Modifier.fillMaxSize()
        )
        val textColor = getTextColorForBackground(colorValues, colorCode)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = if (subtitle != null) 1.dp else 0.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(18.dp)
                    .background(Color(0x99000000), shape = RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
