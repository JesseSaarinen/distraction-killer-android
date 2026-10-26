package io.github.jessesaarinen.distractionkiller.interrupt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.jessesaarinen.distractionkiller.TaskType
import io.github.jessesaarinen.distractionkiller.data.EventLog
import io.github.jessesaarinen.distractionkiller.data.LogEvent
import io.github.jessesaarinen.distractionkiller.detection.AppMarkers
import io.github.jessesaarinen.distractionkiller.detection.EventKind
import io.github.jessesaarinen.distractionkiller.detection.InspectorEntry
import io.github.jessesaarinen.distractionkiller.detection.InspectorLog
import io.github.jessesaarinen.distractionkiller.detection.ScrollTimer
import io.github.jessesaarinen.distractionkiller.ui.theme.DistractionKillerTheme

/**
 * The calm full screen: the level 2 task, and the thank-you screen after
 * stopping. It is an Activity rather than an overlay, so the video app goes
 * to the background (its video pauses) and the keyboard works for the
 * question task. The scroll watcher service starts it.
 */
class CalmActivity : ComponentActivity() {

    private var level2Open = false
    private var app = ""
    private var task = TaskType.BREATHING
    private var scrollSeconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = intent.getStringExtra(EXTRA_APP).orEmpty()
        task = TaskType.fromId(intent.getStringExtra(EXTRA_TASK))
        scrollSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
        val startWithDone = intent.getBooleanExtra(EXTRA_DONE, false)
        level2Open = savedInstanceState?.getBoolean(STATE_LEVEL2_OPEN) ?: !startWithDone
        if (savedInstanceState == null) {
            // In the inspector this shows that Android let the service open the screen.
            InspectorLog.note(if (startWithDone) "thank-you screen opened" else "level 2 screen opened")
        }

        setContent {
            DistractionKillerTheme {
                var showDone by rememberSaveable { mutableStateOf(startWithDone) }
                if (showDone) {
                    DoneScreen(scrollSeconds = scrollSeconds, onClose = ::goHome)
                } else {
                    Level2Screen(
                        task = task,
                        scrollSeconds = scrollSeconds,
                        onFinish = { outcome, answer ->
                            finishLevel2(outcome, answer)
                            if (outcome == "stopped") {
                                showDone = true
                            } else {
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // After turning the phone, the thank-you screen must not count as an open break.
        outState.putBoolean(STATE_LEVEL2_OPEN, level2Open)
    }

    override fun onStop() {
        super.onStop()
        // The user left the break some other way (Home, app switch, screen off).
        if (level2Open && !isChangingConfigurations) {
            finishLevel2("dismissed", null)
            finish()
        }
    }

    private fun finishLevel2(outcome: String, answer: String?) {
        if (!level2Open) return
        level2Open = false
        ScrollTimer.reset()
        EventLog.add(
            LogEvent(
                type = "outcome",
                app = AppMarkers.label(app),
                scrollSeconds = scrollSeconds,
                level = 2,
                task = task.id,
                outcome = outcome,
                answer = answer,
            )
        )
        val detail = "level2 ${task.id} $outcome" + if (answer != null) " answered" else ""
        InspectorLog.add(InspectorEntry(System.currentTimeMillis(), app, EventKind.OUTCOME, detail = detail))
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        private const val EXTRA_APP = "app"
        private const val EXTRA_TASK = "task"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_DONE = "done"
        private const val STATE_LEVEL2_OPEN = "level2Open"

        /** Level 2 with one task. */
        fun level2Intent(context: Context, app: String, task: TaskType, scrollSeconds: Int): Intent =
            baseIntent(context, app, scrollSeconds).putExtra(EXTRA_TASK, task.id)

        /** Straight to the thank-you screen (after "Lopeta tähän" on level 1). */
        fun doneIntent(context: Context, app: String, scrollSeconds: Int): Intent =
            baseIntent(context, app, scrollSeconds).putExtra(EXTRA_DONE, true)

        private fun baseIntent(context: Context, app: String, scrollSeconds: Int): Intent =
            Intent(context, CalmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_APP, app)
                .putExtra(EXTRA_SECONDS, scrollSeconds)
    }
}
