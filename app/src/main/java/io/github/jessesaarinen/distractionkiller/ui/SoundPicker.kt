package io.github.jessesaarinen.distractionkiller.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.AlarmSound
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.interrupt.Alarm
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import io.github.jessesaarinen.distractionkiller.ui.components.SecondaryButton
import io.github.jessesaarinen.distractionkiller.ui.components.SegmentedChoice

/**
 * The reminder sound (task N22): chime, beep or a spoken reminder, and a
 * button that plays the alarm as it will ring. Used on the settings screen
 * and in the setup steps.
 */
@Composable
fun SoundChoice(
    selected: AlarmSound,
    onSelect: (AlarmSound) -> Unit,
    soundOn: Boolean,
    vibrationOn: Boolean,
) {
    val alarm = rememberAlarm()
    SegmentedChoice(
        options = AlarmSound.entries,
        selected = selected,
        label = { stringResource(soundLabel(it)) },
        onSelect = { sound ->
            onSelect(sound)
            // Play the new choice right away, so the user hears what it is.
            if (soundOn) alarm.start(sound = sound, vibration = false)
        },
    )
    Hint(stringResource(soundHint(selected)))
    SecondaryButton(
        text = stringResource(R.string.settings_test_alarm),
        onClick = { alarm.start(sound = if (soundOn) selected else null, vibration = vibrationOn) },
        icon = R.drawable.ic_bell,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    )
}

/** An alarm for the test button, released when the screen goes away. */
@Composable
fun rememberAlarm(): Alarm {
    val context = LocalContext.current
    val alarm = remember { Alarm(context) }
    DisposableEffect(alarm) {
        onDispose { alarm.release() }
    }
    return alarm
}

private fun soundLabel(sound: AlarmSound): Int = when (sound) {
    AlarmSound.CHIME -> R.string.sound_chime
    AlarmSound.BEEP -> R.string.sound_beep
    AlarmSound.SPEECH -> R.string.sound_speech
}

private fun soundHint(sound: AlarmSound): Int = when (sound) {
    AlarmSound.CHIME -> R.string.sound_chime_hint
    AlarmSound.BEEP -> R.string.sound_beep_hint
    AlarmSound.SPEECH -> R.string.sound_speech_hint
}
