package app.drawmark.android.lib.textcanvas

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Handles text layout and rendering for Canvas-based text fields.
 *
 * This class is modeled after AndroidX's TextDelegate but adapted for
 * direct Canvas rendering with support for positioning on an ink canvas.
 *
 * @param text The text to display
 * @param style The text style
 * @param density The density for layout calculations
 * @param fontFamilyResolver The font family resolver
 * @param softWrap Whether text should wrap at soft line breaks
 * @param overflow How to handle text overflow
 * @param maxLines Maximum number of lines
 */
@Stable
class CanvasTextDelegate(
    val text: AnnotatedString,
    val style: TextStyle,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val maxLines: Int = Int.MAX_VALUE
) {
    private var paragraphIntrinsics: MultiParagraphIntrinsics? = null
    private var intrinsicsLayoutDirection: LayoutDirection? = null

    private val nonNullIntrinsics: MultiParagraphIntrinsics
        get() = paragraphIntrinsics
            ?: throw IllegalStateException("layoutIntrinsics must be called first")

    /**
     * The minimum intrinsic width of the text.
     */
    val minIntrinsicWidth: Int
        get() = nonNullIntrinsics.minIntrinsicWidth.ceilToInt()

    /**
     * The maximum intrinsic width of the text.
     */
    val maxIntrinsicWidth: Int
        get() = nonNullIntrinsics.maxIntrinsicWidth.ceilToInt()

    /**
     * Compute intrinsics for the given layout direction.
     */
    fun layoutIntrinsics(layoutDirection: LayoutDirection) {
        val localIntrinsics = paragraphIntrinsics
        val intrinsics = if (
            localIntrinsics == null ||
            layoutDirection != intrinsicsLayoutDirection ||
            localIntrinsics.hasStaleResolvedFonts
        ) {
            intrinsicsLayoutDirection = layoutDirection
            MultiParagraphIntrinsics(
                annotatedString = text,
                style = resolveDefaults(style, layoutDirection),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList()
            )
        } else {
            localIntrinsics
        }

        paragraphIntrinsics = intrinsics
    }

    /**
     * Perform text layout with the given constraints.
     */
    fun layout(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        prevResult: TextLayoutResult? = null
    ): TextLayoutResult {
        layoutIntrinsics(layoutDirection)

        // Check if we can reuse the previous result
        if (prevResult != null && canReuseLayout(prevResult, constraints, layoutDirection)) {
            return prevResult
        }

        val multiParagraph = layoutText(constraints, layoutDirection)

        val rawSize = IntSize(
            multiParagraph.width.ceilToInt(),
            multiParagraph.height.ceilToInt()
        )
        val size = IntSize(
            rawSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
            rawSize.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        )

        return TextLayoutResult(
            layoutInput = TextLayoutInput(
                text = text,
                style = style,
                placeholders = emptyList(),
                maxLines = maxLines,
                softWrap = softWrap,
                overflow = overflow,
                density = density,
                layoutDirection = layoutDirection,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            ),
            multiParagraph = multiParagraph,
            size = size
        )
    }

    private fun canReuseLayout(
        prevResult: TextLayoutResult,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Boolean {
        val input = prevResult.layoutInput
        return input.text == text &&
                input.style == style &&
                input.maxLines == maxLines &&
                input.softWrap == softWrap &&
                input.overflow == overflow &&
                input.density == density &&
                input.layoutDirection == layoutDirection &&
                input.fontFamilyResolver === fontFamilyResolver &&
                input.constraints == constraints
    }

    private fun layoutText(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MultiParagraph {
        val minWidth = constraints.minWidth
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            Constraints.Infinity
        }

        // Handle ellipsis for non-wrapping text
        val overwriteMaxLines = !softWrap && overflow == TextOverflow.Ellipsis
        val finalMaxLines = if (overwriteMaxLines) 1 else maxLines

        val width = if (minWidth == maxWidth) {
            maxWidth
        } else {
            maxIntrinsicWidth.coerceIn(minWidth, maxWidth)
        }

        return MultiParagraph(
            intrinsics = nonNullIntrinsics,
            constraints = Constraints.fitPrioritizingWidth(
                minWidth = 0,
                maxWidth = width,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            ),
            maxLines = finalMaxLines,
            overflow = overflow
        )
    }

    companion object {
        /**
         * Default cursor width in pixels.
         */
        const val DEFAULT_CURSOR_WIDTH = 2f

        /**
         * Default selection highlight color alpha.
         */
        const val SELECTION_ALPHA = 0.4f

        /**
         * Draw a text field onto an Android Canvas.
         *
         * @param canvas The native Android canvas to draw on
         * @param state The text field state containing text and selection info
         * @param textLayoutResult The result of text layout
         * @param position The position to draw at (top-left corner)
         * @param cursorColor The color for the cursor
         * @param selectionColor The color for selection highlight
         * @param showCursor Whether to draw the cursor
         */
        fun draw(
            canvas: Canvas,
            state: CanvasTextFieldState,
            textLayoutResult: TextLayoutResult,
            position: Offset = Offset.Zero,
            cursorColor: Color = Color.Black,
            selectionColor: Color = Color.Blue.copy(alpha = SELECTION_ALPHA),
            showCursor: Boolean = true
        ) {
            canvas.save()
            canvas.translate(position.x, position.y)

            val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)

            // Draw selection highlight first (behind text)
            if (state.hasSelection) {
                drawSelection(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    selection = state.selection,
                    selectionColor = selectionColor
                )
            }

            // Draw the text
            TextPainter.paint(composeCanvas, textLayoutResult)

            // Draw cursor on top
            if (showCursor && state.hasFocus && !state.hasSelection && state.cursorVisible) {
                drawCursor(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    cursorOffset = state.value.selection.start,
                    cursorColor = cursorColor
                )
            }

            // Draw composition underline if active
            state.composition?.let { composition ->
                drawCompositionUnderline(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    composition = composition,
                    color = cursorColor
                )
            }

            canvas.restore()
        }

        /**
         * Draw using a Compose Canvas instead of native Android Canvas.
         */
        fun draw(
            canvas: androidx.compose.ui.graphics.Canvas,
            state: CanvasTextFieldState,
            textLayoutResult: TextLayoutResult,
            position: Offset = Offset.Zero,
            cursorColor: Color = Color.Black,
            selectionColor: Color = Color.Blue.copy(alpha = SELECTION_ALPHA),
            showCursor: Boolean = true
        ) {
            draw(
                canvas = canvas.nativeCanvas,
                state = state,
                textLayoutResult = textLayoutResult,
                position = position,
                cursorColor = cursorColor,
                selectionColor = selectionColor,
                showCursor = showCursor
            )
        }

        /**
         * Draw the selection highlight.
         */
        private fun drawSelection(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            selection: androidx.compose.ui.text.TextRange,
            selectionColor: Color
        ) {
            if (selection.collapsed) return

            val selectionPath = textLayoutResult.getPathForRange(
                selection.min,
                selection.max
            )

            val paint = Paint().apply {
                color = selectionColor
                style = PaintingStyle.Fill
            }

            canvas.drawPath(selectionPath, paint)
        }

        /**
         * Draw the cursor at the specified offset.
         */
        private fun drawCursor(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            cursorOffset: Int,
            cursorColor: Color,
            cursorWidth: Float = DEFAULT_CURSOR_WIDTH
        ) {
            val cursorRect = textLayoutResult.getCursorRect(cursorOffset)

            val paint = Paint().apply {
                color = cursorColor
                style = PaintingStyle.Fill
            }

            // Draw cursor as a thin rectangle
            canvas.drawRect(
                Rect(
                    left = cursorRect.left,
                    top = cursorRect.top,
                    right = cursorRect.left + cursorWidth,
                    bottom = cursorRect.bottom
                ),
                paint
            )
        }

        /**
         * Draw the composition underline for IME input.
         */
        private fun drawCompositionUnderline(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            composition: androidx.compose.ui.text.TextRange,
            color: Color
        ) {
            if (composition.collapsed) return

            // Get the bounding boxes for the composition range
            val startRect = textLayoutResult.getCursorRect(composition.start)
            val endRect = textLayoutResult.getCursorRect(composition.end)

            val paint = Paint().apply {
                this.color = color
                style = PaintingStyle.Stroke
                strokeWidth = 2f
            }

            // Draw underline
            // For single-line text, draw a simple line
            // For multi-line, we'd need to draw multiple segments
            val y = maxOf(startRect.bottom, endRect.bottom) - 1f
            canvas.drawLine(
                p1 = Offset(startRect.left, y),
                p2 = Offset(endRect.right, y),
                paint = paint
            )
        }

        /**
         * Create or update a CanvasTextDelegate if parameters changed.
         */
        fun updateDelegate(
            current: CanvasTextDelegate?,
            text: AnnotatedString,
            style: TextStyle,
            density: Density,
            fontFamilyResolver: FontFamily.Resolver,
            softWrap: Boolean = true,
            overflow: TextOverflow = TextOverflow.Clip,
            maxLines: Int = Int.MAX_VALUE
        ): CanvasTextDelegate {
            if (current != null &&
                current.text == text &&
                current.style == style &&
                current.density == density &&
                current.fontFamilyResolver === fontFamilyResolver &&
                current.softWrap == softWrap &&
                current.overflow == overflow &&
                current.maxLines == maxLines
            ) {
                return current
            }

            return CanvasTextDelegate(
                text = text,
                style = style,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = softWrap,
                overflow = overflow,
                maxLines = maxLines
            )
        }
    }
}

/**
 * Extension to ceil a float to int.
 */
private fun Float.ceilToInt(): Int = ceil(this).roundToInt()
