package app.drawmark.android.lib.ink

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

// TODO: This composable was originally meant to handle displaying our contents for us, but now I'm
//   wondering if we should use this instead:
//   @see https://developer.android.com/reference/kotlin/androidx/ink/rendering/android/view/ViewStrokeRenderer
@Composable
fun InkDisplaySurface(
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer
) {
    // Canvas for rendering finished strokes (read-only display)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasTransform = Matrix()
        drawContext.canvas.nativeCanvas.concat(canvasTransform)
        val canvas = drawContext.canvas.nativeCanvas

        finishedStrokesState.forEach { stroke ->
            canvasStrokeRenderer.draw(
                stroke = stroke,
                canvas = canvas,
                strokeToScreenTransform = canvasTransform
            )
        }
    }
}
