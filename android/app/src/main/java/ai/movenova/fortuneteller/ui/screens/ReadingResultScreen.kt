package ai.movenova.fortuneteller.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.movenova.fortuneteller.R
import ai.movenova.fortuneteller.data.Images
import ai.movenova.fortuneteller.data.PalmReadingResponse
import ai.movenova.fortuneteller.data.PalmStore
import ai.movenova.fortuneteller.data.ReadingScope
import ai.movenova.fortuneteller.data.SavedReading
import ai.movenova.fortuneteller.ui.AccentAmber
import ai.movenova.fortuneteller.ui.FutureBackground
import ai.movenova.fortuneteller.ui.GlassCard
import ai.movenova.fortuneteller.ui.PrimaryButton
import ai.movenova.fortuneteller.ui.SecondaryButton
import ai.movenova.fortuneteller.ui.SectionLabel
import ai.movenova.fortuneteller.ui.str
import java.util.UUID

@Composable
fun ReadingResultScreen(
    response: PalmReadingResponse?,
    imageBytes: ByteArray?,
    store: PalmStore,
    onDone: () -> Unit,
    alreadySaved: Boolean = false
) {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(alreadySaved) }

    Box(Modifier.fillMaxSize()) {
        FutureBackground()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                str(R.string.nav_reading),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                str(ReadingScope.fromApi(response?.scope ?: "").labelRes),
                color = AccentAmber,
                style = MaterialTheme.typography.labelLarge
            )

            val preview = Images.imageBitmapFromBytes(imageBytes)
            if (preview != null) {
                Spacer(Modifier.height(14.dp))
                Image(
                    bitmap = preview,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                SectionLabel(str(R.string.result_cosmic_signal))
                Spacer(Modifier.height(6.dp))
                Text(
                    response?.summary.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            response?.sections?.forEach { section ->
                Spacer(Modifier.height(12.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(section.title, color = AccentAmber, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(section.text, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!response?.advice.isNullOrEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(str(R.string.reading_advice_label))
                Spacer(Modifier.height(8.dp))
                response?.advice?.forEach { tip ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", color = AccentAmber)
                        Text(tip, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                response?.disclaimer?.takeIf { it.isNotBlank() } ?: str(R.string.disclaimer_default),
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(20.dp))
            if (response != null && !saved) {
                PrimaryButton(
                    title = str(R.string.action_save),
                    onClick = {
                        val thumb = Images.thumbnailBase64(imageBytes ?: ByteArray(0))
                        store.save(
                            SavedReading(
                                id = UUID.randomUUID().toString(),
                                createdAt = System.currentTimeMillis(),
                                kind = SavedReading.KIND_READING,
                                scope = response.scope,
                                reading = response,
                                imageBase64 = thumb
                            )
                        )
                        saved = true
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
            SecondaryButton(
                title = if (saved) str(R.string.action_saved) else str(R.string.action_close),
                onClick = onDone
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
