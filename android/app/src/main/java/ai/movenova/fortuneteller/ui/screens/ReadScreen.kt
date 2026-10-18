package ai.movenova.fortuneteller.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.movenova.fortuneteller.R
import ai.movenova.fortuneteller.data.ApiClient
import ai.movenova.fortuneteller.data.AppSettings
import ai.movenova.fortuneteller.data.Images
import ai.movenova.fortuneteller.data.PalmReadingResponse
import ai.movenova.fortuneteller.data.ReadingScope
import ai.movenova.fortuneteller.ui.AccentAmber
import ai.movenova.fortuneteller.ui.CaptureFiles
import ai.movenova.fortuneteller.ui.FutureBackground
import ai.movenova.fortuneteller.ui.GlassCard
import ai.movenova.fortuneteller.ui.LanguageMenu
import ai.movenova.fortuneteller.ui.PrimaryButton
import ai.movenova.fortuneteller.ui.SecondaryButton
import ai.movenova.fortuneteller.ui.errorText
import ai.movenova.fortuneteller.ui.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReadScreen(
    settings: AppSettings,
    onResult: (PalmReadingResponse, ByteArray) -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiClient() }
    val scope = rememberCoroutineScope()

    var selectedScope by remember { mutableStateOf(ReadingScope.MONTH) }
    var rawImage by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = pendingFile
        if (captured && file != null) {
            runCatching { file.readBytes() }.getOrNull()?.let {
                rawImage = it
                errorMessage = null
            }
        }
    }

    fun launchCapture() {
        val file = CaptureFiles.newCaptureFile(context)
        pendingFile = file
        captureLauncher.launch(CaptureFiles.uriFor(context, file))
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCapture() else errorMessage = context.getString(R.string.error_generic)
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Images.readBytes(context, uri)?.let {
                rawImage = it
                errorMessage = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        FutureBackground()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(str(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(str(R.string.app_nav_subtitle), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                }
                LanguageMenu(settings)
            }

            Spacer(Modifier.height(18.dp))
            Text(str(R.string.app_hero_title), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(str(R.string.app_subtitle), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(18.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                val preview = Images.imageBitmapFromBytes(rawImage)
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = str(R.string.action_capture),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.15f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.15f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🖐️", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(str(R.string.app_empty_preview_title), color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                str(R.string.app_empty_preview_subtitle),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryButton(
                        title = str(R.string.action_capture),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCapture() else permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    SecondaryButton(
                        title = str(R.string.action_gallery),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(str(R.string.scope_title), color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ReadingScope.entries.forEach { scopeOption ->
                    FilterChip(
                        selected = scopeOption == selectedScope,
                        onClick = { selectedScope = scopeOption },
                        label = { Text(str(scopeOption.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentAmber.copy(alpha = 0.28f),
                            selectedLabelColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            errorMessage?.let {
                Text(it, color = Color(0xFFFF9AA2), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
            }

            PrimaryButton(
                title = if (loading) str(R.string.reading_analyzing) else str(R.string.action_analyze),
                loading = loading,
                enabled = rawImage != null,
                onClick = {
                    val bytes = rawImage ?: return@PrimaryButton
                    loading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val jpeg = withContext(Dispatchers.Default) { Images.compressForUpload(bytes) }
                                ?: throw IllegalStateException("compress failed")
                            val response = api.analyzePalm(jpeg, selectedScope, settings.apiLanguageCode())
                            onResult(response, jpeg)
                        } catch (t: Throwable) {
                            errorMessage = errorText(context, t)
                        } finally {
                            loading = false
                        }
                    }
                }
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
