package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BlueprintPrimaryDark,
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = BlueprintSecondaryDark,
    onSecondary = Color(0xFF003733),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = BlueprintTertiaryDark,
    onTertiary = Color(0xFF4A2800),
    background = StructuralBackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = StructuralSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = StructuralSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = BlueprintPrimary,
    onPrimary = BlueprintOnPrimary,
    primaryContainer = BlueprintPrimaryContainer,
    onPrimaryContainer = BlueprintOnPrimaryContainer,
    secondary = BlueprintSecondary,
    onSecondary = BlueprintOnSecondary,
    secondaryContainer = BlueprintSecondaryContainer,
    onSecondaryContainer = BlueprintOnSecondaryContainer,
    tertiary = BlueprintTertiary,
    onTertiary = BlueprintOnTertiary,
    tertiaryContainer = BlueprintTertiaryContainer,
    onTertiaryContainer = BlueprintOnTertiaryContainer,
    background = StructuralBackground,
    onBackground = StructuralOnBackground,
    surface = StructuralSurface,
    onSurface = StructuralOnSurface,
    surfaceVariant = StructuralSurfaceVariant,
    onSurfaceVariant = StructuralOnSurfaceVariant,
    outline = StructuralOutline
)

@Composable
fun RdmSolverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent engineering palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
