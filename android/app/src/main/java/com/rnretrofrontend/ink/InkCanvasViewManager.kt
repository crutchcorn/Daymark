// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Ink Canvas View Manager
// InkCanvasViewManager.kt
//
// React Native ViewManager for the InkCanvasView component.

package com.rnretrofrontend.ink

import android.graphics.Color
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class InkCanvasViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<InkCanvasView>() {

    companion object {
        const val REACT_CLASS = "InkCanvasView"
        const val COMMAND_CLEAR = 1
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): InkCanvasView {
        return InkCanvasView(context)
    }

    @ReactProp(name = "brushColor")
    fun setBrushColor(view: InkCanvasView, color: String?) {
        color?.let {
            try {
                view.setBrushColor(Color.parseColor(it))
            } catch (e: IllegalArgumentException) {
                // Invalid color format, use default black
                view.setBrushColor(Color.BLACK)
            }
        }
    }

    @ReactProp(name = "brushSize", defaultFloat = 5f)
    fun setBrushSize(view: InkCanvasView, size: Float) {
        view.setBrushSize(size)
    }

    @ReactProp(name = "brushFamily")
    fun setBrushFamily(view: InkCanvasView, family: String?) {
        family?.let {
            view.setBrushFamily(it)
        }
    }

    override fun getCommandsMap(): Map<String, Int> {
        return mapOf("clear" to COMMAND_CLEAR)
    }

    override fun receiveCommand(view: InkCanvasView, commandId: String?, args: ReadableArray?) {
        when (commandId) {
            "clear" -> view.clearCanvas()
        }
    }

    @Suppress("DEPRECATION")
    override fun receiveCommand(view: InkCanvasView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            COMMAND_CLEAR -> view.clearCanvas()
        }
    }
}
