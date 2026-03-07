package com.m0h31h31.bamburfidreader.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class AppUiStyle {
    NEUMORPHIC,
    MIUIX
}

val LocalAppUiStyle = compositionLocalOf { AppUiStyle.NEUMORPHIC }
