package app.drawmark.android.lib.textcanvas

import android.util.Log
import androidx.compose.ui.geometry.Offset
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

private const val TAG = "TextFieldSerializer"

/**
 * Serialized representation of a text field for persistent storage.
 */
data class SerializedTextField(
    val text: String,
    val positionX: Float,
    val positionY: Float
)

/**
 * Utility class for serializing and deserializing text fields.
 */
class TextFieldSerializer {

    private val gson: Gson = GsonBuilder().create()

    /**
     * Serializes a list of text field states to a JSON string.
     */
    fun serializeTextFields(textFields: List<CanvasTextFieldState>): String {
        Log.d(TAG, "serializeTextFields called with ${textFields.size} text fields")
        val serializedTextFields = textFields.map { state ->
            Log.d(TAG, "  Serializing: text='${state.text}', position=${state.position}")
            SerializedTextField(
                text = state.text,
                positionX = state.position.x,
                positionY = state.position.y
            )
        }
        val json = gson.toJson(serializedTextFields)
        Log.d(TAG, "serializeTextFields result: $json")
        return json
    }

    /**
     * Deserializes a JSON string to a list of text field states.
     */
    fun deserializeTextFields(json: String): List<CanvasTextFieldState> {
        Log.d(TAG, "deserializeTextFields called with: $json")
        if (json.isBlank()) {
            Log.d(TAG, "deserializeTextFields: json is blank, returning empty list")
            return emptyList()
        }

        return try {
            val type = object : TypeToken<List<SerializedTextField>>() {}.type
            val serializedTextFields: List<SerializedTextField> = gson.fromJson(json, type)
            Log.d(TAG, "deserializeTextFields: parsed ${serializedTextFields.size} text fields")
            serializedTextFields.map { serialized ->
                Log.d(TAG, "  Deserializing: text='${serialized.text}', pos=(${serialized.positionX}, ${serialized.positionY})")
                CanvasTextFieldState.withText(
                    text = serialized.text,
                    position = Offset(serialized.positionX, serialized.positionY)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "deserializeTextFields: Exception during parsing", e)
            emptyList()
        }
    }
}
