package io.github.jessesaarinen.distractionkiller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// All app colours live here, like the CSS variables in the web demo.
// Calm world: warm off-white, dark text, petrol green, peach accent.
private val Background = Color(0xFFF6F3EE)
private val Ink = Color(0xFF1F2A2E)
private val Petrol = Color(0xFF2F6F6A)
private val Peach = Color(0xFFE8A87C)
private val InkQuiet = Color(0xFF5B6669)

private val DarkBackground = Color(0xFF151C1E)
private val DarkInk = Color(0xFFE9E5DF)
private val PetrolLight = Color(0xFF7FB8B2)
private val DarkInkQuiet = Color(0xFFA9B2B4)

private val LightColors = lightColorScheme(
    primary = Petrol,
    onPrimary = Background,
    secondary = Peach,
    onSecondary = Ink,
    background = Background,
    onBackground = Ink,
    surface = Background,
    onSurface = Ink,
    onSurfaceVariant = InkQuiet,
)

private val DarkColors = darkColorScheme(
    primary = PetrolLight,
    onPrimary = DarkBackground,
    secondary = Peach,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkBackground,
    onSurface = DarkInk,
    onSurfaceVariant = DarkInkQuiet,
)

@Composable
fun DistractionKillerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
