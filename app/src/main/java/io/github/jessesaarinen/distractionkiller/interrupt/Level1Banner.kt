package io.github.jessesaarinen.distractionkiller.interrupt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.ui.durationText
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/** Level 1: a calm banner at the top of the video app, like showLevel1() in the web demo. */
@Composable
fun Level1Banner(
    scrollSeconds: Int,
    onStop: () -> Unit,
    onContinue: () -> Unit,
) {
    val title = stringResource(R.string.level1_title, durationText(scrollSeconds))
    DistractionKillerTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                // A screen reader announces the banner when it appears.
                .semantics { paneTitle = title },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.level1_text),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.level1_stop))
                    }
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.level1_continue))
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
