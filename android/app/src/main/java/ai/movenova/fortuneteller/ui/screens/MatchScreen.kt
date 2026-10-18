package ai.movenova.fortuneteller.ui.screens

import android.Manifest
import android.app.DatePickerDialog
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import ai.movenova.fortuneteller.data.MatchType
import ai.movenova.fortuneteller.data.PalmMatchResponse
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
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    settings: AppSettings,
    onResult: (PalmMatchResponse, ByteArray, ByteArray) -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiClient() }
    val scope = rememberCoroutineScope()

    var matchType by remember { mutableStateOf(MatchType.ROMANTIC) }
    var imageA by remember { mutableStateOf<ByteArray?>(null) }
    var imageB by remember { mutableStateOf<ByteArray?>(null) }
    var birthA by remember { mutableStateOf("") }
    var birthB by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickerA = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) Images.readBytes(context, uri)?.let { imageA = it; errorMessage = null }
    }
    val pickerB = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) Images.readBytes(context, uri)?.let { imageB = it; errorMessage = null }
    }

    var pendingSlot by remember { mutableStateOf(0) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val file = pendingFile
        if (captured && file != null) {
            runCatching { file.readBytes() }.getOrNull()?.let { bytes ->
                if (pendingSlot == 0) imageA = bytes else imageB = bytes
                errorMessage = null
            }
        }
    }

    fun launchCapture(slot: Int) {
        val file = CaptureFiles.newCaptureFile(context)
        pendingSlot = slot
        pendingFile = file
        captureLauncher.launch(CaptureFiles.uriFor(context, file))
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCapture(pendingSlot)
        } else {
            errorMessage = context.getString(R.string.error_generic)
        }
    }

    fun showBirthPicker(current: String, onPicked: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                onPicked(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
                    Text(str(R.string.match_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(str(R.string.app_nav_subtitle), color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                }
                LanguageMenu(settings)
            }

            Spacer(Modifier.height(12.dp))
            Text(str(R.string.match_subtitle), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))
            Text(str(R.string.match_type_title), color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MatchType.entries.forEach { option ->
                    FilterChip(
                        selected = option == matchType,
                        onClick = { matchType = option },
                        label = { Text(str(option.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentAmber.copy(alpha = 0.28f),
                            selectedLabelColor = Color.White,
                            labelColor = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            val slots = listOf(
                R.string.match_person_a to pickerA,
                R.string.match_person_b to pickerB
            )
            slots.forEachIndexed { index, (labelRes, launcher) ->
                val slotImage = if (index == 0) imageA else imageB
                Spacer(Modifier.height(14.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(str(labelRes), color = AccentAmber, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    val preview = Images.imageBitmapFromBytes(slotImage)
                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.9f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖐️", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SecondaryButton(
                            title = str(R.string.action_capture),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    launchCapture(index)
                                } else {
                                    pendingSlot = index
                                    cameraPermission.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                        SecondaryButton(
                            title = str(R.string.action_gallery),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (index == 0) showBirthPicker(birthA) { birthA = it; errorMessage = null }
                            else showBirthPicker(birthB) { birthB = it; errorMessage = null }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            (if (index == 0) birthA else birthB).ifBlank {
                                str(if (index == 0) R.string.match_birth_a else R.string.match_birth_b)
                            },
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            errorMessage?.let {
                Text(it, color = Color(0xFFFF9AA2), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
            }

            PrimaryButton(
                title = if (loading) str(R.string.match_analyzing) else str(R.string.match_analyze),
                loading = loading,
                enabled = imageA != null && imageB != null && birthA.isNotBlank() && birthB.isNotBlank(),
                onClick = {
                    val a = imageA ?: return@PrimaryButton
                    val b = imageB ?: return@PrimaryButton
                    loading = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val jpegA = withContext(Dispatchers.Default) { Images.compressForUpload(a) }
                                ?: throw IllegalStateException("compress A failed")
                            val jpegB = withContext(Dispatchers.Default) { Images.compressForUpload(b) }
                                ?: throw IllegalStateException("compress B failed")
                            val response = api.matchPalm(
                                imageAJpeg = jpegA,
                                imageBJpeg = jpegB,
                                matchType = matchType,
                                personABirth = birthA,
                                personBBirth = birthB,
                                language = settings.apiLanguageCode()
                            )
                            onResult(response, jpegA, jpegB)
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
