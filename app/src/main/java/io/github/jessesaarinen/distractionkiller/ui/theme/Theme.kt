package io.github.jessesaarinen.distractionkiller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jessesaarinen.distractionkiller.R

// All app colours, fonts and corner shapes live here (see DESIGN.md).
// The calm world: warm paper, dark ink, petrol green, and a peach sun that
// rises over the horizon when you look up from the phone.

// Light theme
private val Paper = Color(0xFFF6F3EE) // background
private val Card = Color(0xFFFFFDFA) // cards on the paper
private val Sand = Color(0xFFEDE6DB) // quiet fills: tracks, unselected choices
private val Ink = Color(0xFF1F2A2E) // text
private val InkQuiet = Color(0xFF56636A) // secondary text, 5.6:1 on paper
private val Petrol = Color(0xFF2F6F6A) // buttons and other actions
private val PetrolMist = Color(0xFFD8E9E5)
private val PetrolInk = Color(0xFF133B37)
private val DeepPetrol = Color(0xFF24524E) // hero cards
private val Peach = Color(0xFFE8A87C) // the sun
private val PeachMist = Color(0xFFF9E3D3)
private val PeachInk = Color(0xFF5B3418)
private val Clay = Color(0xFFA8432F) // destructive actions
private val ClayMist = Color(0xFFF6DED6)
private val ClayInk = Color(0xFF5A1C10)
private val Line = Color(0xFFE6DFD4) // hairlines around cards
private val LineStrong = Color(0xFF9A9184) // outlined buttons

// Dark theme: the same roles at night
private val Night = Color(0xFF111A1C)
private val NightCard = Color(0xFF1A2528)
private val NightSand = Color(0xFF243134)
private val Moon = Color(0xFFECE7E0)
private val MoonQuiet = Color(0xFFA7B3B5)
private val PetrolLight = Color(0xFF86C5BC)
private val PetrolNight = Color(0xFF1D3D3A)
private val PetrolMoon = Color(0xFFCDEAE4)
private val PetrolDark = Color(0xFF0C2624)
private val PeachLight = Color(0xFFF0B48A)
private val PeachNight = Color(0xFF4A3020)
private val PeachMoon = Color(0xFFFCDCC6)
private val PeachDark = Color(0xFF2A1608)
private val ClayLight = Color(0xFFF0A08E)
private val ClayNight = Color(0xFF4F2219)
private val NightLine = Color(0xFF2A3639)
private val NightLineStrong = Color(0xFF5A686B)

// Roles used by the screens:
// primary = actions, secondary = the sun (decoration, not text),
// tertiary = hero cards (status card, stop rate), surface = cards,
// surfaceVariant = quiet fills, outlineVariant = card hairlines.
private val LightColors = lightColorScheme(
    primary = Petrol,
    onPrimary = Paper,
    primaryContainer = PetrolMist,
    onPrimaryContainer = PetrolInk,
    inversePrimary = PetrolLight,
    secondary = Peach,
    onSecondary = Ink,
    secondaryContainer = PeachMist,
    onSecondaryContainer = PeachInk,
    tertiary = DeepPetrol,
    onTertiary = Paper,
    tertiaryContainer = PetrolMist,
    onTertiaryContainer = PetrolInk,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkQuiet,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    error = Clay,
    onError = Paper,
    errorContainer = ClayMist,
    onErrorContainer = ClayInk,
    outline = LineStrong,
    outlineVariant = Line,
    surfaceContainerLowest = Card,
    surfaceContainerLow = Card,
    surfaceContainer = Card,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = Sand,
)

private val DarkColors = darkColorScheme(
    primary = PetrolLight,
    onPrimary = PetrolDark,
    primaryContainer = PetrolNight,
    onPrimaryContainer = PetrolMoon,
    inversePrimary = Petrol,
    secondary = PeachLight,
    onSecondary = PeachDark,
    secondaryContainer = PeachNight,
    onSecondaryContainer = PeachMoon,
    tertiary = PetrolNight,
    onTertiary = Moon,
    tertiaryContainer = PetrolNight,
    onTertiaryContainer = PetrolMoon,
    background = Night,
    onBackground = Moon,
    surface = NightCard,
    onSurface = Moon,
    surfaceVariant = NightSand,
    onSurfaceVariant = MoonQuiet,
    surfaceTint = Color.Transparent,
    inverseSurface = Moon,
    inverseOnSurface = Night,
    error = ClayLight,
    onError = Night,
    errorContainer = ClayNight,
    onErrorContainer = ClayLight,
    outline = NightLineStrong,
    outlineVariant = NightLine,
    surfaceContainerLowest = Night,
    surfaceContainerLow = NightCard,
    surfaceContainer = NightCard,
    surfaceContainerHigh = NightCard,
    surfaceContainerHighest = NightSand,
)

// Headlines and big numbers: Fraunces, a soft serif (SIL Open Font License,
// assets/licenses). Everything else: the phone's own font (Roboto).
private val Fraunces = FontFamily(Font(R.font.fraunces_medium, FontWeight.Medium))

private fun serif(size: Int, lineHeight: Int, letterSpacing: Double = 0.0) = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private fun sans(size: Int, lineHeight: Int, weight: FontWeight, letterSpacing: Double) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private val AppTypography = Typography(
    displayLarge = serif(64, 68, -1.0), // the stop rate on the stats screen
    displayMedium = serif(44, 50, -0.5), // the breathing countdown
    displaySmall = serif(36, 42, -0.25), // home headline, thank-you title
    headlineLarge = serif(32, 38, -0.25), // screen titles, level 2 task titles
    headlineMedium = serif(28, 34), // numbers in the stats tiles, the movement task
    headlineSmall = serif(24, 30), // card titles, the level 1 banner
    titleLarge = sans(20, 26, FontWeight.Medium, 0.0),
    titleMedium = sans(16, 22, FontWeight.Medium, 0.1),
    titleSmall = sans(14, 20, FontWeight.Medium, 0.1),
    bodyLarge = sans(16, 24, FontWeight.Normal, 0.15),
    bodyMedium = sans(14, 20, FontWeight.Normal, 0.15),
    bodySmall = sans(13, 18, FontWeight.Normal, 0.2),
    labelLarge = sans(15, 20, FontWeight.Medium, 0.1), // buttons
    labelMedium = sans(12, 16, FontWeight.SemiBold, 1.2), // small capital labels above sections
    labelSmall = sans(11, 16, FontWeight.Medium, 0.5),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), // choices, text fields
    large = RoundedCornerShape(24.dp), // cards
    extraLarge = RoundedCornerShape(32.dp), // hero cards, the level 1 banner
)

@Composable
fun DistractionKillerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
