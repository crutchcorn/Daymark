// SPDX-License-Identifier: MIT
//
// Drawmark - Ink Editor View
// InkEditorView.kt
//
// A Jetpack Compose-based drawing surface using the Android Ink API.
// This view supports editing (drawing) strokes.

package app.drawmark.android.prototype.ink

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun InkEditorSurface(
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    getBrush: () -> Brush,
    modifier: Modifier = Modifier
) {
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }

    Box(modifier = Modifier.fillMaxSize().then(modifier)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)

                inProgressStrokesView.eagerInit()

                val predictor = MotionEventPredictor.newInstance(rootView)
                val touchListener = View.OnTouchListener { view, event ->
                    predictor.record(event)
                    val predictedEvent = predictor.predict()

                    try {
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                view.requestUnbufferedDispatch(event)
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                currentPointerId.value = pointerId
                                currentStrokeId.value = inProgressStrokesView.startStroke(
                                    event = event,
                                    pointerId = pointerId,
                                    brush = getBrush()
                                )
                                true
                            }

                            MotionEvent.ACTION_MOVE -> {
                                val pointerId = currentPointerId.value ?: return@OnTouchListener false
                                val strokeId = currentStrokeId.value ?: return@OnTouchListener false

                                for (pointerIndex in 0 until event.pointerCount) {
                                    if (event.getPointerId(pointerIndex) != pointerId) continue
                                    inProgressStrokesView.addToStroke(
                                        event,
                                        pointerId,
                                        strokeId,
                                        predictedEvent
                                    )
                                }
                                true
                            }

                            MotionEvent.ACTION_UP -> {
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                if (pointerId == currentPointerId.value) {
                                    val strokeId = currentStrokeId.value
                                    if (strokeId != null) {
                                        inProgressStrokesView.finishStroke(
                                            event,
                                            pointerId,
                                            strokeId
                                        )
                                    }
                                    view.performClick()
                                }
                                true
                            }

                            MotionEvent.ACTION_CANCEL -> {
                                val pointerIndex = event.actionIndex
                                val pointerId = event.getPointerId(pointerIndex)
                                if (pointerId == currentPointerId.value) {
                                    val strokeId = currentStrokeId.value
                                    if (strokeId != null) {
                                        inProgressStrokesView.cancelStroke(strokeId, event)
                                    }
                                }
                                true
                            }

                            else -> false
                        }
                    } finally {
                        predictedEvent?.recycle()
                    }
                }

                rootView.setOnTouchListener(touchListener)
                rootView.addView(inProgressStrokesView)
                rootView
            }
        )

        // Canvas for rendering finished strokes
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
}

/**
 * Composable wrapper for InkEditorView that can be used in Jetpack Compose.
 */
@Composable
fun InkEditor(
    context: Context,
    modifier: Modifier = Modifier
) {
    val inProgressStrokesView = remember { InProgressStrokesView(context) }
    val finishedStrokesState = remember { mutableStateOf(emptySet<Stroke>()) }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

    // Default brush configuration
    val brushFamily = remember { StockBrushes.pressurePen() }
    val brushColor = remember { Color.Black.toArgb() }
    val brushSize = remember { 5f }

    val getBrush: () -> Brush = {
        Brush.createWithColorIntArgb(
            family = brushFamily,
            colorIntArgb = brushColor,
            size = brushSize,
            epsilon = 0.1f
        )
    }

    // Set up the finished strokes listener
    val finishedStrokesListener = remember {
        object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                finishedStrokesState.value = finishedStrokesState.value + strokes.values
                inProgressStrokesView.removeFinishedStrokes(strokes.keys)
            }
        }
    }

    // Add/remove listener based on composition lifecycle
    androidx.compose.runtime.DisposableEffect(inProgressStrokesView) {
        inProgressStrokesView.addFinishedStrokesListener(finishedStrokesListener)
        onDispose {
            inProgressStrokesView.removeFinishedStrokesListener(finishedStrokesListener)
        }
    }

    InkEditorSurface(
        inProgressStrokesView = inProgressStrokesView,
        finishedStrokesState = finishedStrokesState.value,
        canvasStrokeRenderer = canvasStrokeRenderer,
        getBrush = getBrush,
        modifier = modifier
    )
}
