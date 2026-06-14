package ua.danichapps.radiantdays.ui.common

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.util.Locale

@Composable
fun rememberVoiceInputLauncher(
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (text.isNotBlank()) onResult(text)
    }
    return remember(launcher, onResult, onUnavailable) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
            } else {
                onUnavailable()
            }
        }
    }
}

fun appendVoiceTextToFieldValue(current: TextFieldValue, spoken: String): TextFieldValue {
    if (spoken.isBlank()) return current

    val insertAt = current.selection.start.coerceIn(0, current.text.length)
    val prefix = when {
        current.text.isEmpty() || insertAt == 0 -> ""
        current.text[insertAt - 1].isWhitespace() -> ""
        else -> " "
    }

    val newText = buildString {
        append(current.text.substring(0, insertAt))
        append(prefix)
        append(spoken)
        append(current.text.substring(insertAt))
    }
    val cursor = insertAt + prefix.length + spoken.length
    return TextFieldValue(newText, TextRange(cursor))
}
