package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.ProgressBar
import io.github.jessesaarinen.distractionkiller.ui.components.ScreenScaffold
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard

// The permission steps (task N12). Task N22 moved them from the home screen
// behind Asetukset → Käyttöönotto, so they do not show while people try the
// demo on a phone that is set up already.
@Composable
fun PermissionsScreen(
    usageAccess: Boolean,
    overlayAllowed: Boolean,
    batteryUnrestricted: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onBack: () -> Unit,
) {
    val events by EventLog.events.collectAsState()
    // The whole chain works once the first interruption has happened.
    val firstInterruptionSeen = events.any { it.type == "level1" }

    ScreenScaffold(title = stringResource(R.string.home_setup_title), onBack = onBack) {
        SetupCard(
            usageAccess = usageAccess,
            overlayAllowed = overlayAllowed,
            batteryUnrestricted = batteryUnrestricted,
            firstInterruptionSeen = firstInterruptionSeen,
            onOpenUsageAccess = onOpenUsageAccess,
            onOpenOverlay = onOpenOverlay,
            onOpenAppInfo = onOpenAppInfo,
        )
    }
}

@Composable
private fun SetupCard(
    usageAccess: Boolean,
    overlayAllowed: Boolean,
    batteryUnrestricted: Boolean,
    firstInterruptionSeen: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val steps = listOf(usageAccess, overlayAllowed, batteryUnrestricted, firstInterruptionSeen)
    SectionCard {
        // FlowRow moves "0/4 valmis" under the title when the font is large.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.permissions_steps_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_setup_progress, steps.count { it }, steps.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        ProgressBar(fraction = steps.count { it } / steps.size.toFloat(), modifier = Modifier.padding(bottom = 8.dp))

        SetupStep(
            number = 1,
            done = usageAccess,
            title = stringResource(R.string.setup_usage_title),
            text = stringResource(R.string.setup_usage_text),
        ) {
            PrimaryButton(
                text = stringResource(R.string.home_open_usage_access),
                onClick = onOpenUsageAccess,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SetupStep(
            number = 2,
            done = overlayAllowed,
            title = stringResource(R.string.setup_overlay_title),
            text = stringResource(R.string.setup_overlay_text),
        ) {
            PrimaryButton(
                text = stringResource(R.string.home_open_overlay),
                onClick = onOpenOverlay,
                modifier = Modifier.fillMaxWidth(),
            )
            // Some phones block these switches for apps installed from an APK file.
            Hint(stringResource(R.string.setup_restricted_text), modifier = Modifier.padding(top = 4.dp))
            SecondaryButton(
                text = stringResource(R.string.home_open_app_info),
                onClick = onOpenAppInfo,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SetupStep(
            number = 3,
            done = batteryUnrestricted,
            title = stringResource(R.string.setup_battery_title),
            text = stringResource(R.string.setup_battery_text),
        ) {
            SecondaryButton(
                text = stringResource(R.string.home_open_app_info),
                onClick = onOpenAppInfo,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SetupStep(
            number = 4,
            done = firstInterruptionSeen,
            title = stringResource(R.string.setup_try_title),
            text = stringResource(R.string.setup_try_text),
        )
    }
}

/** One setup step: a number, or a check mark once done. A done step shows only its title. */
@Composable
private fun SetupStep(
    number: Int,
    done: Boolean,
    title: String,
    text: String,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text(text = number.toString(), style = MaterialTheme.typography.titleSmall)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (!done) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actions()
            }
        }
    }
}
