package app.drawmark.android.lib.textcanvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Represents a single atomic text change that can be undone/redone.
 *
 * @param index Start point of the change
 * @param preText Text that was replaced (empty for insertions)
 * @param postText Text that replaced preText (empty for deletions)
 * @param preSelection Selection before the change
 * @param postSelection Selection after the change
 * @param timeInMillis When this change was made
 * @param canMerge Whether this change can be merged with adjacent changes
 */
data class TextUndoOperation(
    val index: Int,
    val preText: String,
    val postText: String,
    val preSelection: TextRange,
    val postSelection: TextRange,
    val timeInMillis: Long = System.currentTimeMillis(),
    val canMerge: Boolean = true
) {
    /**
     * The type of edit operation.
     */
    val editType: TextEditType = when {
        preText.isEmpty() && postText.isNotEmpty() -> TextEditType.Insert
        preText.isNotEmpty() && postText.isEmpty() -> TextEditType.Delete
        else -> TextEditType.Replace
    }
    
    /**
     * For delete operations, the direction of deletion.
     */
    val deleteType: TextDeleteType
        get() {
            if (editType != TextEditType.Delete) return TextDeleteType.NotByUser
            if (!postSelection.collapsed) return TextDeleteType.NotByUser
            if (preSelection.collapsed) {
                return if (preSelection.start > postSelection.start) {
                    TextDeleteType.Start // Backspace
                } else {
                    TextDeleteType.End // Delete key
                }
            } else if (preSelection.start == postSelection.start && preSelection.start == index) {
                return TextDeleteType.Inner // Selection deleted
            }
            return TextDeleteType.NotByUser
        }
    
    /**
     * Whether this is a newline insertion (unmergeable).
     */
    val isNewLineInsert: Boolean
        get() = postText == "\n" || postText == "\r\n"
}

/**
 * Type of text edit operation.
 */
enum class TextEditType {
    Insert,
    Delete,
    Replace
}

/**
 * Direction of a delete operation.
 */
enum class TextDeleteType {
    Start,      // Backspace - delete before cursor
    End,        // Delete key - delete after cursor
    Inner,      // Selection deleted
    NotByUser   // Programmatic deletion
}

// Time window for merging operations (500ms)
private const val MERGE_INTERVAL_MILLIS = 500L
private const val UNDO_CAPACITY = 100

/**
 * Manages undo/redo history for text field changes with intelligent merging.
 *
 * This class maintains a stack-based history of text field states,
 * merging consecutive similar operations (like typing multiple characters)
 * into single undoable actions.
 */
@Stable
class UndoManager {
    // Undo stack
    private val undoStack = mutableListOf<TextUndoOperation>()
    
    // Redo stack
    private val redoStack = mutableListOf<TextUndoOperation>()
    
    // Staging area for potentially mergeable operations
    private var stagingOp by mutableStateOf<TextUndoOperation?>(null)
    
    // Flag to prevent recording during undo/redo
    private var isUndoRedoInProgress = false

    /**
     * Whether undo is available.
     */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty() || stagingOp != null

    /**
     * Whether redo is available.
     */
    val canRedo: Boolean
        get() = redoStack.isNotEmpty() && stagingOp == null

    /**
     * Record a text change for undo history.
     *
     * @param preValue The value before the change
     * @param postValue The value after the change
     * @param allowMerge Whether to allow merging with previous operations
     */
    fun recordChange(
        preValue: TextFieldValue,
        postValue: TextFieldValue,
        allowMerge: Boolean = true
    ) {
        if (isUndoRedoInProgress) return
        
        // Find the change
        val (index, preText, postText) = findChange(preValue.text, postValue.text)
        
        // No actual text change
        if (preText.isEmpty() && postText.isEmpty()) return
        
        val op = TextUndoOperation(
            index = index,
            preText = preText,
            postText = postText,
            preSelection = preValue.selection,
            postSelection = postValue.selection,
            canMerge = allowMerge
        )
        
        record(op)
    }

    /**
     * Record an operation, potentially merging with the staging operation.
     */
    private fun record(op: TextUndoOperation) {
        // Clear redo stack on new changes
        redoStack.clear()
        
        val currentStaging = stagingOp
        if (currentStaging == null) {
            stagingOp = op
            return
        }
        
        // Try to merge with staging
        val merged = tryMerge(currentStaging, op)
        if (merged != null) {
            stagingOp = merged
            return
        }
        
        // Can't merge - flush staging and set new op
        flush()
        stagingOp = op
    }
    
    /**
     * Try to merge two operations.
     */
    private fun tryMerge(first: TextUndoOperation, second: TextUndoOperation): TextUndoOperation? {
        if (!first.canMerge || !second.canMerge) return null
        
        // Don't merge if too much time has passed
        if (second.timeInMillis - first.timeInMillis >= MERGE_INTERVAL_MILLIS) return null
        
        // Don't merge newline insertions
        if (first.isNewLineInsert || second.isNewLineInsert) return null
        
        // Only merge same type of operations
        if (first.editType != second.editType) return null
        
        // Merge consecutive insertions
        if (first.editType == TextEditType.Insert && 
            first.index + first.postText.length == second.index) {
            return TextUndoOperation(
                index = first.index,
                preText = "",
                postText = first.postText + second.postText,
                preSelection = first.preSelection,
                postSelection = second.postSelection,
                timeInMillis = first.timeInMillis
            )
        }
        
        // Merge consecutive deletions (same direction)
        if (first.editType == TextEditType.Delete && 
            first.deleteType == second.deleteType) {
            when (first.deleteType) {
                TextDeleteType.Start -> {
                    // Backspace: second deletes before first
                    if (second.index + second.preText.length == first.index) {
                        return TextUndoOperation(
                            index = second.index,
                            preText = second.preText + first.preText,
                            postText = "",
                            preSelection = first.preSelection,
                            postSelection = second.postSelection,
                            timeInMillis = first.timeInMillis
                        )
                    }
                }
                TextDeleteType.End -> {
                    // Delete key: second deletes at same position after first
                    if (first.index == second.index) {
                        return TextUndoOperation(
                            index = first.index,
                            preText = first.preText + second.preText,
                            postText = "",
                            preSelection = first.preSelection,
                            postSelection = second.postSelection,
                            timeInMillis = first.timeInMillis
                        )
                    }
                }
                else -> { /* Don't merge other delete types */ }
            }
        }
        
        return null
    }
    
    /**
     * Flush the staging operation to the undo stack.
     */
    private fun flush() {
        stagingOp?.let { op ->
            undoStack.add(op)
            // Trim if over capacity
            while (undoStack.size > UNDO_CAPACITY) {
                undoStack.removeAt(0)
            }
        }
        stagingOp = null
    }
    
    /**
     * Find the change between two strings.
     * Returns (index, preText, postText).
     */
    private fun findChange(pre: String, post: String): Triple<Int, String, String> {
        // Find common prefix
        var prefixLen = 0
        while (prefixLen < pre.length && prefixLen < post.length && 
               pre[prefixLen] == post[prefixLen]) {
            prefixLen++
        }
        
        // Find common suffix (but don't overlap with prefix)
        var suffixLen = 0
        while (suffixLen < pre.length - prefixLen && 
               suffixLen < post.length - prefixLen &&
               pre[pre.length - 1 - suffixLen] == post[post.length - 1 - suffixLen]) {
            suffixLen++
        }
        
        val preText = pre.substring(prefixLen, pre.length - suffixLen)
        val postText = post.substring(prefixLen, post.length - suffixLen)
        
        return Triple(prefixLen, preText, postText)
    }

    /**
     * Undo the last change.
     *
     * @param currentValue The current text field value
     * @return The value to restore, or null if nothing to undo
     */
    fun undo(currentValue: TextFieldValue): TextFieldValue? {
        if (!canUndo) return null
        
        isUndoRedoInProgress = true
        try {
            // Flush any staging op first
            flush()
            
            if (undoStack.isEmpty()) return null
            
            val op = undoStack.removeAt(undoStack.lastIndex)
            
            // Add to redo stack
            redoStack.add(op)
            
            // Apply the undo (reverse the operation)
            return applyUndo(currentValue, op)
        } finally {
            isUndoRedoInProgress = false
        }
    }

    /**
     * Redo the last undone change.
     *
     * @param currentValue The current text field value
     * @return The value to restore, or null if nothing to redo
     */
    fun redo(currentValue: TextFieldValue): TextFieldValue? {
        if (!canRedo) return null
        
        isUndoRedoInProgress = true
        try {
            if (redoStack.isEmpty()) return null
            
            val op = redoStack.removeAt(redoStack.lastIndex)
            
            // Add back to undo stack
            undoStack.add(op)
            
            // Apply the redo (forward the operation)
            return applyRedo(currentValue, op)
        } finally {
            isUndoRedoInProgress = false
        }
    }
    
    /**
     * Apply an undo operation (reverse direction).
     */
    private fun applyUndo(current: TextFieldValue, op: TextUndoOperation): TextFieldValue {
        val text = current.text
        val newText = text.substring(0, op.index) + 
                      op.preText + 
                      text.substring(op.index + op.postText.length)
        return TextFieldValue(
            text = newText,
            selection = op.preSelection
        )
    }
    
    /**
     * Apply a redo operation (forward direction).
     */
    private fun applyRedo(current: TextFieldValue, op: TextUndoOperation): TextFieldValue {
        val text = current.text
        val newText = text.substring(0, op.index) + 
                      op.postText + 
                      text.substring(op.index + op.preText.length)
        return TextFieldValue(
            text = newText,
            selection = op.postSelection
        )
    }

    /**
     * Clear all undo/redo history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        stagingOp = null
    }
}
