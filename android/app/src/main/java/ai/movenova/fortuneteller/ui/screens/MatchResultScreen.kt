package ai.movenova.fortuneteller.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.movenova.fortuneteller.R
import ai.movenova.fortuneteller.data.Images
import ai.movenova.fortuneteller.data.MatchType
import ai.movenova.fortuneteller.data.PalmMatchResponse
import ai.movenova.fortuneteller.data.PalmStore
import ai.movenova.fortuneteller.data.SavedReading
import ai.movenova.fortuneteller.ui.AccentAmber
import ai.movenova.fortuneteller.ui.AccentSky
import ai.movenova.fortuneteller.ui.FutureBackground
import ai.movenova.fortuneteller.ui.GlassCard
import ai.movenova.fortuneteller.ui.PrimaryButton
import ai.movenova.fortuneteller.ui.SecondaryButton
import ai.movenova.fortuneteller.ui.SectionLabel
import ai.movenova.fortuneteller.ui.str
import java.util.UUID

@Composable
fun MatchResultScreen(
    response: PalmMatchResponse?,
    imageABytes: ByteArray?,
    imageBBytes: ByteArray?,
    store: PalmStore,
    onDone: () -> Unit,
    alreadySaved: Boolean = false,
    savedImageABase64: String? = null,
    savedImageBBase64: String? = null
) {
    var saved by remember { mutableStateOf(alreadySaved) }
    val previewA = remember(savedImageABase64, imageABytes) {
        Images.imageBitmapFromBase64(savedImageABase64) ?: Images.imageBitmapFromBytes(imageABytes)
    }
    val previewB = remember(savedImageBBase64, imageBBytes) {
        Images.imageBitmapFromBase64(savedImageBBase64) ?: Images.imageBitmapFromBytes(imageBBytes)
    }

    Box(Modifier.fillMaxSize()) {
        FutureBackground()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                str(R.string.nav_match_result),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                str(MatchType.fromApi(response?.matchType ?: "").labelRes),
                color = AccentAmber,
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.height(18.dp))
            if (previewA != null || previewB != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(previewA, previewB).forEach { bitmap ->
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(AccentAmber, Color(0xFFFF8FB1), AccentSky))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(response?.score ?: 0).coerceIn(0, 100)}%",
                                color = Color.White,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                str(R.string.match_score_label),
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    response?.summary.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            listOf(
                R.string.match_strengths to (response?.strengths ?: emptyList()),
                R.string.match_tensions to (response?.tensions ?: emptyList()),
                R.string.match_advice to (response?.advice ?: emptyList())
            ).forEach { (labelRes, items) ->
                Spacer(Modifier.height(14.dp))
                SectionLabel(str(labelRes))
                Spacer(Modifier.height(6.dp))
                items.forEach { item ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text("•  ", color = AccentAmber)
                        Text(item, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium)
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
                        store.save(
                            SavedReading(
                                id = UUID.randomUUID().toString(),
                                createdAt = System.currentTimeMillis(),
                                kind = SavedReading.KIND_MATCH,
                                matchType = response.matchType,
                                match = response,
                                imageBase64 = Images.thumbnailBase64(imageABytes ?: ByteArray(0)),
                                imageBBase64 = Images.thumbnailBase64(imageBBytes ?: ByteArray(0))
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
