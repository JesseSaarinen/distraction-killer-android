package io.github.jessesaarinen.distractionkiller.ui

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.BuildConfig
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.Settings
import io.github.jessesaarinen.distractionkiller.data.SettingsStore
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.IconBubble
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.ProgressBar
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SectionCard
import io.github.jessesaarinen.distractionkiller.ui.components.SideBySide
import io.github.jessesaarinen.distractionkiller.ui.components.SunMark
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

// Home screen (task N12): a status card that says whether the scroll watcher
// is on, a setup guide until everything works, and the way to settings and stats.
@Composable
fun HomeScreen(
    serviceEnabled: Boolean,
    batteryUnrestricted: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenInspector: () -> Unit,
) {
    val settings by SettingsStore.settings.collectAsState()
    val events by EventLog.events.collectAsState()
    val interruptions = events.count { it.type == "level1" }
    // The whole chain works once the first interruption has happened.
    val firstInterruptionSeen = interruptions > 0
    val setupDone = serviceEnabled && batteryUnrestricted && firstInterruptionSeen

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SunMark(modifier = Modifier.size(40.dp), rise = 0.55f)
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_headline),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.home_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                !serviceEnabled -> OffCard()
                !settings.interruptionsOn -> PausedCard()
                else -> OnCard(settings)
            }

            if (!setupDone) {
                SetupCard(
                    serviceEnabled = serviceEnabled,
                    batteryUnrestricted = batteryUnrestricted,
                    firstInterruptionSeen = firstInterruptionSeen,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenAppInfo = onOpenAppInfo,
                )
            }

            SideBySide {
                NavTile(
                    icon = R.drawable.ic_settings,
                    title = stringResource(R.string.home_open_settings),
                    subtitle = stringResource(R.string.home_settings_sub, durationText(settings.level1Seconds)),
                    onClick = onOpenSettings,
                )
                NavTile(
                    icon = R.drawable.ic_stats,
                    title = stringResource(R.string.home_open_stats),
                    subtitle = if (interruptions > 0) {
                        stringResource(R.string.home_stats_sub, interruptions)
                    } else {
                        stringResource(R.string.home_stats_empty_sub)
                    },
                    onClick = onOpenStats,
                )
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Hint(stringResource(R.string.home_privacy), modifier = Modifier.weight(1f))
            }
            // FlowRow moves the version under the button when the font is large.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = onOpenInspector,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_inspector),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_open_inspector),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Hint(
                    text = stringResource(R.string.home_version, BuildConfig.VERSION_NAME),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

/** Status: the watcher is on and interrupts after the chosen time. */
@Composable
private fun OnCard(settings: Settings) {
    val onCard = MaterialTheme.colorScheme.onTertiary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiary,
        contentColor = onCard,
    ) {
        Box {
            SunMark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(84.dp),
                rise = 0.6f,
                horizon = onCard.copy(alpha = 0.35f),
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusLabel(text = stringResource(R.string.home_on_label), color = onCard.copy(alpha = 0.8f)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    )
                }
                Text(
                    text = stringResource(R.string.home_on_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(end = 72.dp),
                )
                Text(
                    text = stringResource(R.string.home_service_on, durationText(settings.level1Seconds)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = onCard.copy(alpha = 0.9f),
                )
                Text(
                    text = if (settings.watchedApps.isEmpty()) {
                        stringResource(R.string.home_no_apps)
                    } else {
                        settings.watchedApps.sorted().joinToString("  ·  ")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCard.copy(alpha = 0.75f),
                )
                if (settings.demoMode || settings.tester > 0) {
                    // FlowRow puts the second label on its own line when the font is large.
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (settings.demoMode) Pill(stringResource(R.string.home_demo_on))
                        // Task N19: which RT8 tester's test is running.
                        if (settings.tester > 0) Pill(stringResource(R.string.home_tester_on, settings.tester))
                    }
                }
            }
        }
    }
}

/** A small peach label on the status card, like "Demotila päällä". */
@Composable
private fun Pill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Status: the user paused the interruptions in the settings. */
@Composable
private fun PausedCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusLabel(text = stringResource(R.string.home_paused_label)) {
                Icon(painter = painterResource(R.drawable.ic_pause), contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text = stringResource(R.string.home_paused_title), style = MaterialTheme.typography.headlineSmall)
            Text(text = stringResource(R.string.home_paused), style = MaterialTheme.typography.bodyLarge)
            PrimaryButton(
                text = stringResource(R.string.home_resume),
                onClick = { SettingsStore.update { it.copy(interruptionsOn = true) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

/** Status: the accessibility service is not on yet. The setup guide follows. */
@Composable
private fun OffCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusLabel(text = stringResource(R.string.home_off_label), color = MaterialTheme.colorScheme.primary) {
                Icon(painter = painterResource(R.drawable.ic_power), contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(text = stringResource(R.string.home_off_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.home_service_off),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A small capital label with a symbol in front, like "● PÄÄLLÄ". */
@Composable
private fun StatusLabel(
    text: String,
    color: Color = Color.Unspecified,
    symbol: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        symbol()
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SetupCard(
    serviceEnabled: Boolean,
    batteryUnrestricted: Boolean,
    firstInterruptionSeen: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val steps = listOf(serviceEnabled, batteryUnrestricted, firstInterruptionSeen)
    SectionCard {
        // FlowRow moves "0/3 valmis" under the title when the font is large.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.home_setup_title),
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
            done = serviceEnabled,
            title = stringResource(R.string.setup_accessibility_title),
            text = stringResource(R.string.setup_accessibility_text),
        ) {
            PrimaryButton(
                text = stringResource(R.string.home_open_accessibility),
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
            )
            // Android 13+ blocks this switch for apps installed from an APK file.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Hint(stringResource(R.string.setup_restricted_text), modifier = Modifier.padding(top = 4.dp))
                SecondaryButton(
                    text = stringResource(R.string.home_open_app_info),
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SetupStep(
            number = 2,
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
            number = 3,
            done = firstInterruptionSeen,
            title = stringResource(R.string.setup_try_title),
            text = stringResource(R.string.setup_try_text, Config.DEMO_LEVEL1_SECONDS),
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

/** A big button to another screen, with an icon and a one-line summary. */
@Composable
private fun NavTile(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconBubble(icon = icon)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Hint(subtitle)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DistractionKillerTheme {
        HomeScreen(
            serviceEnabled = false,
            batteryUnrestricted = false,
            onOpenAccessibilitySettings = {},
            onOpenAppInfo = {},
            onOpenSettings = {},
            onOpenStats = {},
            onOpenInspector = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenOnPreview() {
    DistractionKillerTheme {
        HomeScreen(
            serviceEnabled = true,
            batteryUnrestricted = true,
            onOpenAccessibilitySettings = {},
            onOpenAppInfo = {},
            onOpenSettings = {},
            onOpenStats = {},
            onOpenInspector = {},
        )
    }
}
