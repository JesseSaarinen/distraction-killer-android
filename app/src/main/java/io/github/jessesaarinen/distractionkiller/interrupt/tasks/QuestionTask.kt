package io.github.jessesaarinen.distractionkiller.interrupt.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.R

/**
 * "Mitä olit oikeasti tekemässä?", like tasks/question.js in the web demo.
 * The answer (an option or the user's own words) goes to the event log.
 */
@Composable
fun QuestionTask(onDone: (answer: String) -> Unit) {
    val options = stringArrayResource(R.array.question_options)
    // Saved, so turning the phone keeps the choice and the typed words.
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var ownWords by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val answer = ownWords.trim().ifEmpty { selected }
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.question_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        for (option in options) {
            val optionModifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            if (option == selected) {
                Button(onClick = {}, enabled = !submitted, modifier = optionModifier) { Text(option) }
            } else {
                OutlinedButton(onClick = { selected = option }, enabled = !submitted, modifier = optionModifier) {
                    Text(option)
                }
            }
        }
        OutlinedTextField(
            value = ownWords,
            onValueChange = { if (it.length <= Config.ANSWER_MAX_LENGTH) ownWords = it },
            placeholder = { Text(stringResource(R.string.question_own_words)) },
            singleLine = true,
            enabled = !submitted,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                if (answer != null) {
                    submitted = true
                    // Closes the keyboard so the next buttons are visible.
                    focusManager.clearFocus()
                    onDone(answer)
                }
            },
            enabled = answer != null && !submitted,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(if (submitted) R.string.question_thanks else R.string.question_submit))
        }
    }
}
