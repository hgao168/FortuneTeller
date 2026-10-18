package ai.movenova.fortuneteller.ui

import android.content.Context
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.movenova.fortuneteller.R
import ai.movenova.fortuneteller.data.ApiError
import ai.movenova.fortuneteller.data.AppLanguage
import ai.movenova.fortuneteller.data.AppSettings
import ai.movenova.fortuneteller.displayName
import java.io.File

/** FileProvider-backed capture targets (cache dir, wiped by the system when space is tight). */
object CaptureFiles {

    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(dir, "palm_${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Maps a thrown error to a localized, user-facing string. */
fun errorText(context: Context, error: Throwable): String = when (error) {
    is ApiError.Network -> context.getString(R.string.error_network, error.host)
    is ApiError.Unauthorized -> context.getString(R.string.error_unauthorized)
    is ApiError.TooLarge -> context.getString(R.string.error_image_too_large)
    is ApiError.BadRequest -> error.detail
    else -> context.getString(R.string.error_generic)
}

/** Language switcher mirroring iOS AppSettings (system / english / chinese). */
@Composable
fun LanguageMenu(settings: AppSettings) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage by settings.language.collectAsStateWithLifecycle()

    IconButton(onClick = { expanded = true }) {
        Icon(
            Icons.Filled.Language,
            contentDescription = context.getString(R.string.language_title),
            tint = Color.White.copy(alpha = 0.85f)
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        AppLanguage.entries.forEach { language ->
            DropdownMenuItem(
                text = {
                    Text(
                        if (language == currentLanguage) {
                            "✓ " + language.displayName(context)
                        } else {
                            language.displayName(context)
                        }
                    )
                },
                onClick = {
                    settings.setLanguage(language)
                    expanded = false
                }
            )
        }
    }
}

