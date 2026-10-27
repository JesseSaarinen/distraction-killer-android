package io.github.jessesaarinen.distractionkiller.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R

// Building blocks shared by the app's screens, so every screen has the same
// spacing, cards and headings. Colours come from the theme (ui/theme/Theme.kt).

/** A screen with a back button, a big title and scrolling content. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The icon button's touch area is bigger than the arrow, so the
            // arrow lines up with the text below.
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .semantics { heading() },
            )
            content()
        }
    }
}

/** Small capital label above a card, like "AJOITUS". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 4.dp, top = 12.dp)
            .semantics { heading() },
    )
}

/** A white card with a hairline border. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/**
 * Cards side by side with the same width and height, like the Asetukset and
 * Tilastot tiles. With a very large font a long word would not fit in a
 * narrow card, so then the cards go on top of each other instead.
 */
@Composable
fun SideBySide(
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { cards, constraints ->
        if (cards.isEmpty()) return@Layout layout(0, 0) {}
        val gap = spacing.roundToPx()
        val width = constraints.maxWidth
        val cardWidth = (width - gap * (cards.size - 1)) / cards.size
        // minIntrinsicWidth is the width of the longest word (plus padding).
        val fits = cards.all { it.minIntrinsicWidth(Constraints.Infinity) <= cardWidth }
        if (fits) {
            val height = cards.maxOf { it.maxIntrinsicHeight(cardWidth) }
            val placed = cards.map { it.measure(Constraints.fixed(cardWidth, height)) }
            layout(width, height) {
                placed.forEachIndexed { i, card -> card.placeRelative(i * (cardWidth + gap), 0) }
            }
        } else {
            val placed = cards.map { it.measure(Constraints.fixedWidth(width)) }
            layout(width, placed.sumOf { it.height } + gap * (placed.size - 1)) {
                var y = 0
                placed.forEach { card ->
                    card.placeRelative(0, y)
                    y += card.height + gap
                }
            }
        }
    }
}

/** Helper text under a setting or a title. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}

/** An icon in a soft circle. Decorative: the text next to it says the same. */
@Composable
fun IconBubble(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** A thin rounded bar that is filled up to fraction (0..1). */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/**
 * The calm full screen's background: paper (or night) with a warm glow rising
 * from the bottom edge, like the light just before sunrise. Used by level 2
 * and the thank-you screen.
 */
@Composable
fun CalmBackground(content: @Composable () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val sun = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(background)
                val glowCenter = Offset(size.width / 2f, size.height)
                val glowRadius = size.maxDimension * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(sun.copy(alpha = 0.32f), sun.copy(alpha = 0f)),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}
