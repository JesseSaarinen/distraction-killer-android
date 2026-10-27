package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.ui.components.CheckCircle
import io.github.jessesaarinen.distractionkiller.ui.components.PrimaryButton

/**
 * "Mitä olit oikeasti tekemässä?", like tasks/question.js in the web demo.
 * The answer (an option or the user's own words) goes to the event log.
 */
@Composable
fun QuestionTask(onDone: (answer: String) -> Unit) {
    val options = stringArrayResource(R.array.question_options)
    var selected by remember { mutableStateOf<String?>(null) }
    var ownWords by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val answer = ownWords.trim().ifEmpty { selected }
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.question_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (option in options) {
                OptionRow(
                    text = option,
                    // Own words win over a chosen option, like `answer` above.
                    selected = option == selected && ownWords.isBlank(),
                    enabled = !submitted,
                    onClick = { selected = option },
                )
            }
        }
        OutlinedTextField(
            value = ownWords,
            onValueChange = { if (it.length <= Config.ANSWER_MAX_LENGTH) ownWords = it },
            placeholder = { Text(stringResource(R.string.question_own_words)) },
            singleLine = true,
            enabled = !submitted,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = stringResource(if (submitted) R.string.question_thanks else R.string.question_submit),
            onClick = {
                if (answer != null) {
                    submitted = true
                    // Closes the keyboard so the next buttons are visible.
                    focusManager.clearFocus()
                    onDone(answer)
                }
            },
            enabled = answer != null && !submitted,
            icon = if (submitted) R.drawable.ic_check else null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

/** One answer option: a card with a round check on the right. */
@Composable
private fun OptionRow(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            CheckCircle(checked = selected)
        }
    }
}
