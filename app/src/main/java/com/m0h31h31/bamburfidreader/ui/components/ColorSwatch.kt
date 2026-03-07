package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import com.m0h31h31.bamburfidreader.ui.components.neuCard
import com.m0h31h31.bamburfidreader.util.parseColorValue
import kotlin.math.roundToInt

private fun needsCheckerboard(colors: List<Color>): Boolean {
    return colors.any { it.alpha < 1f }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    val uiStyle = LocalAppUiStyle.current
    val light = if (uiStyle == AppUiStyle.MIUIX) {
        Color(0xFFF2F3F5)
    } else {
        Color(0xFFF5F5F5)
    }
    val dark = if (uiStyle == AppUiStyle.MIUIX) {
        Color(0xFFD9DEE6)
    } else {
        Color(0xFFE1E1E1)
    }
    Canvas(modifier = modifier) {
        val tileSize = 6.dp.toPx()
        if (tileSize <= 0f) return@Canvas
        val columns = (size.width / tileSize).roundToInt() + 1
        val rows = (size.height / tileSize).roundToInt() + 1
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                drawRect(
                    color = if ((x + y) % 2 == 0) light else dark,
                    topLeft = Offset(x * tileSize, y * tileSize),
                    size = androidx.compose.ui.geometry.Size(tileSize, tileSize)
                )
            }
        }
    }
}

@Composable
fun ColorSwatch(
    colorValues: List<String>,
    colorType: String,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalAppUiStyle.current
    val parsedColors = colorValues.mapNotNull { parseColorValue(it) }
    val colors = if (parsedColors.isNotEmpty()) {
        parsedColors
    } else {
        listOf(MaterialTheme.colorScheme.surface)
    }
    val resolvedType = when {
        colorType.isNotBlank() -> colorType
        colors.size > 1 -> "多拼色"
        else -> "单色"
    }
    val shape = RoundedCornerShape(14.dp)
    val showCheckerboard = needsCheckerboard(colors)
    val borderColor = if (uiStyle == AppUiStyle.MIUIX) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
    } else {
        Color.White.copy(alpha = 0.65f)
    }
    val containerModifier = if (uiStyle == AppUiStyle.MIUIX) {
        modifier.clip(shape)
    } else {
        modifier.neuCard(shape = shape).clip(shape)
    }

    when (resolvedType) {
        "渐变色" -> {
            Box(
                modifier = containerModifier
                    .border(1.dp, borderColor, shape)
            ) {
                if (showCheckerboard) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(colors))
                )
            }
        }

        "多拼色" -> {
            Box(
                modifier = containerModifier
                    .border(1.dp, borderColor, shape)
            ) {
                if (showCheckerboard) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }
                Row(modifier = Modifier.fillMaxSize()) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = containerModifier
                    .border(1.dp, borderColor, shape)
            ) {
                if (showCheckerboard) {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.firstOrNull() ?: Color.Transparent)
                )
            }
        }
    }
}
