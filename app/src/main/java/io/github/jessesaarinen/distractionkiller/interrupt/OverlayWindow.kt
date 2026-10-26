package io.github.jessesaarinen.distractionkiller.interrupt

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A window drawn with Compose on top of other apps. It uses the accessibility
 * service's own overlay type, so no "display over other apps" permission is
 * needed. Create it with the AccessibilityService as the context.
 */
class OverlayWindow(private val context: Context, private val fullScreen: Boolean) {
    private val windowManager = checkNotNull(context.getSystemService(WindowManager::class.java))
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    fun show(content: @Composable () -> Unit) {
        hide()
        val newOwner = OverlayOwner()
        val newView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(newOwner)
            setViewTreeSavedStateRegistryOwner(newOwner)
            setContent(content)
        }
        try {
            windowManager.addView(newView, layoutParams())
        } catch (e: RuntimeException) {
            // The service is not connected (anymore). Nothing to show.
            newOwner.destroy()
            return
        }
        view = newView
        owner = newOwner
    }

    fun hide() {
        view?.let { windowManager.removeView(it) }
        owner?.destroy()
        view = null
        owner = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        if (fullScreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        // The banner lets touches outside it reach the app below.
        // The full-screen overlay (N9) takes focus so the user can type.
        if (fullScreen) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
    }
}

/** Gives the overlay's ComposeView the lifecycle that an Activity would normally give it. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
