package app.drawmark.android.lib.ink

import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.textcanvas.CanvasTextFieldState

/**
 * Represents a drawable element on the canvas with z-ordering support.
 * 
 * This sealed class allows strokes and text fields to be rendered
 * in their creation order, ensuring proper visual layering where
 * elements drawn later appear on top of elements drawn earlier.
 */
sealed class CanvasElement {
    /**
     * The z-index of this element, used for ordering during rendering.
     * Higher values are drawn on top of lower values.
     */
    abstract val zIndex: Long

    /**
     * A stroke element on the canvas.
     */
    data class StrokeElement(
        val stroke: Stroke,
        override val zIndex: Long
    ) : CanvasElement()

    /**
     * A text field element on the canvas.
     */
    data class TextFieldElement(
        val textField: CanvasTextFieldState,
        override val zIndex: Long
    ) : CanvasElement()
}

/**
 * Counter for generating unique z-indices.
 * This should be managed at the editor/manager level.
 */
class ZIndexCounter(initialValue: Long = 0L) {
    private var current: Long = initialValue

    /**
     * Gets the next z-index value and increments the counter.
     */
    fun next(): Long = current++

    /**
     * Gets the current value without incrementing.
     */
    fun current(): Long = current

    /**
     * Resets the counter to a specific value.
     * Useful when loading saved state.
     */
    fun reset(value: Long = 0L) {
        current = value
    }
}
