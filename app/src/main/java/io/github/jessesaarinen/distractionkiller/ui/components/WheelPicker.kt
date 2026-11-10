package io.github.jessesaarinen.distractionkiller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private val ItemHeight = 48.dp
private const val VISIBLE_ITEMS = 3

/**
 * A vertical carousel of numbers, like a clock app's time picker. The number
 * in the middle band is the chosen one. Scrolling snaps to a number, and
 * tapping a number scrolls it to the middle, which also works with TalkBack.
 */
@Composable
fun WheelPicker(
    values: List<Int>,
    current: Int,
    onSelect: (Int) -> Unit,
    label: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = values.indexOf(current).coerceAtLeast(0))
    val scope = rememberCoroutineScope()
    // The item whose middle is closest to the middle of the carousel.
    val centered by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index ?: state.firstVisibleItemIndex
        }
    }
    LaunchedEffect(centered, state.isScrollInProgress) {
        if (!state.isScrollInProgress) values.getOrNull(centered)?.let { if (it != current) onSelect(it) }
    }

    Box(modifier = modifier.height(ItemHeight * VISIBLE_ITEMS), contentAlignment = Alignment.Center) {
        // The band that marks the chosen number.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
            // Room above the first and below the last number, so both can reach the middle.
            contentPadding = PaddingValues(vertical = ItemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(values) { index, value ->
                val isCentered = index == centered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .semantics { selected = isCentered }
                        .clickable(role = Role.Button) { scope.launch { state.animateScrollToItem(index) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(value),
                        style = if (isCentered) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        color = if (isCentered) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}
