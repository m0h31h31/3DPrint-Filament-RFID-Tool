package com.m0h31h31.bamburfidreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.m0h31h31.bamburfidreader.ui.theme.AppUiStyle
import com.m0h31h31.bamburfidreader.ui.theme.LocalAppUiStyle
import top.yukonga.miuix.kmp.basic.InputField as MiuixInputField
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SearchBar as MiuixSearchBar
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

private val NeuShape = RoundedCornerShape(24.dp)
private val NeuInnerShape = RoundedCornerShape(18.dp)

@Composable
fun Modifier.neuBackground(): Modifier {
    val uiStyle = LocalAppUiStyle.current
    val colors = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
        AppUiStyle.MIUIX -> listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        )
    }
    return background(brush = Brush.verticalGradient(colors = colors))
}

@Composable
fun Modifier.neuCard(
    shape: Shape = NeuShape,
    elevated: Boolean = true
): Modifier {
    val uiStyle = LocalAppUiStyle.current
    val base = MaterialTheme.colorScheme.surface
    val darkShadow = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
    }
    val lightShadow = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> Color.White.copy(alpha = 0.92f)
        AppUiStyle.MIUIX -> Color.White.copy(alpha = 0.45f)
    }
    val borderColor = when (uiStyle) {
        AppUiStyle.NEUMORPHIC -> Color.White.copy(alpha = 0.7f)
        AppUiStyle.MIUIX -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }
    val shadowed = if (elevated) {
        when (uiStyle) {
            AppUiStyle.NEUMORPHIC -> this
                .shadow(
                    elevation = 10.dp,
                    shape = shape,
                    ambientColor = darkShadow,
                    spotColor = darkShadow
                )
                .shadow(
                    elevation = 2.dp,
                    shape = shape,
                    ambientColor = lightShadow,
                    spotColor = lightShadow
                )
            AppUiStyle.MIUIX -> this.shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = darkShadow,
                spotColor = darkShadow
            )
        }
    } else {
        this
    }
    return shadowed
        .clip(shape)
        .background(base)
        .border(
            width = 1.dp,
            color = borderColor,
            shape = shape
        )
}

@Composable
fun NeuPanel(
    modifier: Modifier = Modifier,
    shape: Shape = NeuShape,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .neuCard(shape = shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val uiStyle = LocalAppUiStyle.current
    val buttonModifier = if (uiStyle == AppUiStyle.MIUIX) {
        modifier
    } else {
        modifier.neuCard(shape = NeuInnerShape, elevated = true)
    }
    val buttonColors = if (uiStyle == AppUiStyle.MIUIX) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = buttonModifier,
        shape = if (uiStyle == AppUiStyle.MIUIX) MaterialTheme.shapes.medium else NeuInnerShape,
        colors = buttonColors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (uiStyle == AppUiStyle.MIUIX) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}

@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            label = { Text(label) },
            modifier = modifier,
            shape = MaterialTheme.shapes.medium
        )
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            label = { Text(label) },
            modifier = modifier.neuCard(
                shape = NeuInnerShape,
                elevated = true
            ),
            shape = NeuInnerShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun AppSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        val collapsed = remember { { _: Boolean -> } }
        MiuixSearchBar(
            inputField = {
                MiuixInputField(
                    value,
                    onValueChange,
                    { query ->
                        onValueChange(query)
                        onSearch?.invoke()
                    },
                    false,
                    collapsed,
                    Modifier.fillMaxWidth(),
                    placeholder,
                    true,
                    TextStyle.Default
                )
            },
            onExpandedChange = collapsed,
            modifier = modifier
        ) {}
    } else {
        NeuTextField(
            value = value,
            onValueChange = onValueChange,
            label = placeholder,
            modifier = modifier
        )
    }
}

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange ?: {},
            modifier = modifier
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            colors = SwitchDefaults.colors()
        )
    }
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished
        )
    } else {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            colors = androidx.compose.material3.SliderDefaults.colors()
        )
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        if (progress == null) {
            MiuixInfiniteProgressIndicator(modifier = modifier)
        } else {
            MiuixCircularProgressIndicator(
                modifier = modifier,
                progress = progress.coerceIn(0f, 1f)
            )
        }
    } else {
        if (progress == null) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = modifier
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = modifier
            )
        }
    }
}

@Composable
fun AppLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    val uiStyle = LocalAppUiStyle.current
    if (uiStyle == AppUiStyle.MIUIX) {
        MiuixLinearProgressIndicator(
            modifier = modifier,
            progress = progress?.coerceIn(0f, 1f)
        )
    } else {
        if (progress == null) {
            androidx.compose.material3.LinearProgressIndicator(
                modifier = modifier
            )
        } else {
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = modifier
            )
        }
    }
}
