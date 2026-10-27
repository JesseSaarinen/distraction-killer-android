package io.github.jessesaarinen.distractionkiller.interrupt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SunMark
import io.github.jessesaarinen.distractionkiller.ui.durationText
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/**
 * Level 1: a calm card at the top of the video app, like showLevel1() in the
 * web demo. It slides in from the top edge.
 */
@Composable
fun Level1Banner(
    scrollSeconds: Int,
    onStop: () -> Unit,
    onContinue: () -> Unit,
) {
    val title = stringResource(R.string.level1_title, durationText(scrollSeconds))
    DistractionKillerTheme {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = slideInVertically(tween(durationMillis = 450)) { -it } + fadeIn(tween(durationMillis = 300)),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    // A screen reader announces the banner when it appears.
                    .semantics { paneTitle = title },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SunMark(modifier = Modifier.size(52.dp), rise = 0.3f)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.level1_label).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.level1_text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton(
                            text = stringResource(R.string.level1_stop),
                            onClick = onStop,
                            modifier = Modifier.weight(1.3f),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.level1_continue),
                            onClick = onContinue,
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Level1BannerPreview() {
    Level1Banner(scrollSeconds = 60, onStop = {}, onContinue = {})
}
