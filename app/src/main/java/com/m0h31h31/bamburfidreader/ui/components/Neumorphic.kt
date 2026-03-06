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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val NeuShape = RoundedCornerShape(24.dp)
private val NeuInnerShape = RoundedCornerShape(18.dp)

@Composable
fun Modifier.neuBackground(): Modifier = background(
    brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    )
)

@Composable
fun Modifier.neuCard(
    shape: Shape = NeuShape,
    elevated: Boolean = true
): Modifier {
    val base = MaterialTheme.colorScheme.surface
    val darkShadow = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    val lightShadow = Color.White.copy(alpha = 0.92f)
    val shadowed = if (elevated) {
        this
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
    } else {
        this
    }
    return shadowed
        .clip(shape)
        .background(base)
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.7f),
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.neuCard(shape = NeuInnerShape),
        shape = NeuInnerShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        label = { Text(label) },
        modifier = modifier.neuCard(shape = NeuInnerShape, elevated = false),
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
