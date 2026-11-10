package io.github.jessesaarinen.distractionkiller.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.jessesaarinen.distractionkiller.R
import io.github.jessesaarinen.distractionkiller.Config
import io.github.jessesaarinen.distractionkiller.data.WatchedApp
import io.github.jessesaarinen.distractionkiller.ui.components.Hint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lets the user pick an app on the phone to add to the watched apps (task N20).
 * The picked app gets Config.DEFAULT_ALLOWANCE_MIN as its daily time.
 * Lists names only, no app icons, so no brand logos show in our own UI.
 * exclude: package names that are watched already.
 */
@Composable
fun AppPickerDialog(exclude: Set<String>, onPick: (WatchedApp) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Reading every app's name can take a moment, so it is done off the main thread.
    val apps by produceState<List<WatchedApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { launchableApps(context, exclude) }
    }
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_picker_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.app_picker_search)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = apps
                val shown = list?.filter { it.label.contains(query.trim(), ignoreCase = true) }.orEmpty()
                when {
                    list == null -> Hint(stringResource(R.string.app_picker_loading))
                    shown.isEmpty() -> Hint(stringResource(R.string.app_picker_empty))
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable(role = Role.Button) { onPick(app) }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_picker_cancel))
            }
        },
    )
}

/**
 * The apps with a home screen icon, sorted by name, without this app and the
 * excluded ones. The <queries> entry in the manifest lets Android 11+ show them.
 */
private fun launchableApps(context: Context, exclude: Set<String>): List<WatchedApp> {
    val packageManager = context.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    // The newer overload with ResolveInfoFlags is Android 13+ only. This one works on every version.
    @Suppress("DEPRECATION")
    val found = packageManager.queryIntentActivities(launcher, 0)
    return found
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName && it.packageName !in exclude }
        .distinctBy { it.packageName }
        .map { info ->
            val label = info.loadLabel(packageManager).toString().trim()
            WatchedApp(info.packageName, label.ifEmpty { info.packageName }, Config.DEFAULT_ALLOWANCE_MIN)
        }
        .sortedBy { it.label.lowercase() }
}
