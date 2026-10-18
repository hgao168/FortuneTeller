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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.movenova.fortuneteller.R
import ai.movenova.fortuneteller.data.AppSettings
import ai.movenova.fortuneteller.data.Images
import ai.movenova.fortuneteller.data.MatchType
import ai.movenova.fortuneteller.data.PalmStore
import ai.movenova.fortuneteller.data.ReadingScope
import ai.movenova.fortuneteller.data.SavedReading
import ai.movenova.fortuneteller.ui.AccentAmber
import ai.movenova.fortuneteller.ui.AccentSky
import ai.movenova.fortuneteller.ui.FutureBackground
import ai.movenova.fortuneteller.ui.LanguageMenu
import ai.movenova.fortuneteller.ui.str
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    store: PalmStore,
    settings: AppSettings,
    onOpenReading: (SavedReading) -> Unit
) {
    val readings by store.readings.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        FutureBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    str(R.string.history_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                LanguageMenu(settings)
            }

            Spacer(Modifier.height(14.dp))
            if (readings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("🫳", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            str(R.string.history_empty),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(readings, key = { it.id }) { reading ->
                        HistoryRow(
                            reading = reading,
                            onOpen = { onOpenReading(reading) },
                            onDelete = { store.delete(reading.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    reading: SavedReading,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumb = Images.imageBitmapFromBase64(reading.imageBase64)
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
            } else {
                Text("🖐️", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (reading.isMatch) {
                    str(MatchType.fromApi(reading.matchType).labelRes) + " · " + str(R.string.nav_match_result)
                } else {
                    str(ReadingScope.fromApi(reading.scope).labelRes)
                },
                color = AccentAmber,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                reading.titleOrSummary,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                str(
                    R.string.history_saved_on,
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(reading.createdAt))
                ),
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (reading.scoreOrNull != null) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(AccentSky.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${reading.scoreOrNull}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(onClick = onOpen) {
            Text("›", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.headlineSmall)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = str(R.string.action_delete),
                tint = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}
