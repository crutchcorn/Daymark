package app.drawmark.ink

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.ink.InkEditorSurface
import app.drawmark.android.lib.ink.StrokeSerializer
import kotlin.collections.plus


@SuppressLint("ViewConstructor")
class InkEditorView(context: Context) : FrameLayout(context), InProgressStrokesFinishedListener {

    private val inProgressStrokesView = InProgressStrokesView(context)
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val strokeSerializer = StrokeSerializer()

    // Callback for stroke changes
    var onStrokesChange: ((String) -> Unit)? = null

    // Brush configuration
    private var brushColor: Int = Color.Black.toArgb()
    private var brushSize: Float = 5f
    private var brushFamily = StockBrushes.pressurePen()

    init {
        inProgressStrokesView.addFinishedStrokesListener(this)

        val composeView = ComposeView(context).apply {
            setContent {
                InkEditorSurface(
                    inProgressStrokesView = inProgressStrokesView,
                    finishedStrokesState = finishedStrokesState.value,
                    canvasStrokeRenderer = canvasStrokeRenderer,
                    getBrush = { createBrush() }
                )
            }
        }

        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun createBrush(): Brush {
        return Brush.createWithColorIntArgb(
            family = brushFamily,
            colorIntArgb = brushColor,
            size = brushSize,
            epsilon = 0.1f
        )
    }

    /**
     * Sets the brush color using ARGB integer format.
     */
    fun setBrushColor(color: Int) {
        brushColor = color
    }

    /**
     * Sets the brush stroke size.
     */
    fun setBrushSize(size: Float) {
        brushSize = size
    }

    /**
     * Sets the brush family type.
     * Supported values: "pen", "marker", "highlighter"
     */
    fun setBrushFamily(family: String) {
        brushFamily = when (family.lowercase()) {
            "marker" -> StockBrushes.marker()
            "highlighter" -> StockBrushes.highlighter()
            else -> StockBrushes.pressurePen()
        }
    }

    /**
     * Clears all strokes from the canvas.
     */
    fun clearCanvas() {
        finishedStrokesState.value = emptySet()
        notifyStrokesChanged()
    }

    /**
     * Loads strokes from a serialized JSON string.
     */
    fun loadStrokes(json: String) {
        if (json.isEmpty()) return
        val strokes = strokeSerializer.deserializeStrokes(json)
        finishedStrokesState.value = strokes
    }

    /**
     * Gets the current strokes as a serialized JSON string.
     */
    fun getSerializedStrokes(): String {
        return strokeSerializer.serializeStrokes(finishedStrokesState.value)
    }

    /**
     * Notifies the listener that strokes have changed.
     */
    private fun notifyStrokesChanged() {
        val serialized = strokeSerializer.serializeStrokes(finishedStrokesState.value)
        onStrokesChange?.invoke(serialized)
    }

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokesState.value += strokes.values
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
        notifyStrokesChanged()
    }
}
