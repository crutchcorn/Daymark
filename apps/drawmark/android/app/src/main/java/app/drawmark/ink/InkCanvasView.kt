package app.drawmark.ink

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.ink.InkDisplaySurfaceWithText
import app.drawmark.android.lib.ink.StrokeSerializer
import app.drawmark.android.lib.textcanvas.InkCanvasTextFieldManager
import app.drawmark.android.lib.textcanvas.TextFieldSerializer

private const val TAG = "InkCanvasView"

@SuppressLint("ViewConstructor")
class InkCanvasView(context: Context) : FrameLayout(context) {
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val strokeSerializer = StrokeSerializer()
    private val textFieldSerializer = TextFieldSerializer()
    private val textFieldManager = InkCanvasTextFieldManager()
    private val composeView: ComposeView
    private var wasDetached = false

    init {
        composeView = ComposeView(context).apply {
            // Keep composition alive across detach/attach cycles (e.g., tab switches)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun setComposeContent() {
        composeView.setContent {
            InkDisplaySurfaceWithText(
                finishedStrokesState = finishedStrokesState.value,
                canvasStrokeRenderer = canvasStrokeRenderer,
                textFieldManager = textFieldManager,
                isTextMode = false  // Read-only display, text fields not editable
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Post to ensure ComposeView is fully attached before setting content
        post {
            if (composeView.isAttachedToWindow) {
                if (wasDetached) {
                    // Dispose old composition first for re-attachment
                    composeView.disposeComposition()
                }
                setComposeContent()
                // Force a full layout pass to trigger initial render
                composeView.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                )
                composeView.layout(0, 0, width, height)
                composeView.invalidate()
            }
        }
    }

    override fun onDetachedFromWindow() {
        wasDetached = true
        super.onDetachedFromWindow()
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
     * Loads text fields from a serialized JSON string.
     */
    fun loadTextFields(json: String) {
        Log.d(TAG, "loadTextFields called with: $json")
        if (json.isEmpty()) {
            Log.d(TAG, "loadTextFields: json is empty, returning")
            return
        }
        val textFields = textFieldSerializer.deserializeTextFields(json)
        Log.d(TAG, "loadTextFields: deserialized ${textFields.size} text fields")
        textFieldManager.clearTextFields()
        textFields.forEach { textField ->
            Log.d(TAG, "loadTextFields: adding text field with text='${textField.text}' at ${textField.position}")
            textFieldManager.addTextField(textField.position, textField.text)
        }
        Log.d(TAG, "loadTextFields: textFieldManager now has ${textFieldManager.textFields.size} text fields")
    }
}
