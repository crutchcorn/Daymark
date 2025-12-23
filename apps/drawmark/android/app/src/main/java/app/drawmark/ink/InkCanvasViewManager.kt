// SPDX-License-Identifier: MIT
//
// Drawmark - Ink Canvas View Manager
// InkCanvasViewManager.kt
//
// React Native ViewManager for the InkCanvasView component (read-only display).
// For editing capabilities, use InkEditorViewManager instead.

package app.drawmark.ink

import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import app.drawmark.android.lib.ink.InkCanvasView
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext

class InkCanvasViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<InkCanvasView>() {

    companion object {
        const val REACT_CLASS = "InkCanvasView"
        const val COMMAND_LOAD_STROKES = 1
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): InkCanvasView {
        return InkCanvasView(context)
    }
    override fun getCommandsMap(): Map<String, Int> {
        return mapOf(
            "loadStrokes" to COMMAND_LOAD_STROKES
        )
    }

    override fun receiveCommand(view: InkCanvasView, commandId: String?, args: ReadableArray?) {
        when (commandId) {
            "loadStrokes" -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun receiveCommand(view: InkCanvasView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            COMMAND_LOAD_STROKES -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }
}
