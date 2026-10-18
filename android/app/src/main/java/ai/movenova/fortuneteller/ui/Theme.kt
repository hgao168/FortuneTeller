package ai.movenova.fortuneteller.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Palette lifted from iOS ProDesignSystem.swift so both apps read as one product.
val FortuneBgTop = Color(0.08f, 0.07f, 0.16f)
val FortuneBgMid = Color(0.11f, 0.08f, 0.24f)
val FortuneBgBottom = Color(0.05f, 0.13f, 0.20f)
val AccentAmber = Color(1.00f, 0.78f, 0.44f)
val AccentPink = Color(1.00f, 0.57f, 0.72f)
val AccentSky = Color(0.54f, 0.86f, 0.95f)
val BlobAmber = Color(1.00f, 0.67f, 0.38f)
val BlobCyan = Color(0.31f, 0.81f, 0.96f)
val BlobPink = Color(0.99f, 0.52f, 0.74f)

/** Context carrying the in-app language override; every user-visible string goes through [str]. */
val LocalAppContext = staticCompositionLocalOf<Context> {
    error("LocalAppContext not provided — wrap the tree in AppRoot")
}

@Composable
fun str(@StringRes id: Int): String = LocalAppContext.current.getString(id)

@Composable
fun str(@StringRes id: Int, vararg args: Any): String = LocalAppContext.current.getString(id, *args)

@Composable
fun FortuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AccentAmber,
            onPrimary = Color(0xFF1B1230),
            secondary = AccentPink,
            tertiary = AccentSky,
            background = FortuneBgTop,
            onBackground = Color.White,
            surface = Color(0xFF1B1733),
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun FutureBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(FortuneBgTop, FortuneBgMid, FortuneBgBottom),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-150).dp, y = (-280).dp)
                .background(
                    Brush.radialGradient(listOf(BlobAmber.copy(alpha = 0.35f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .offset(x = 170.dp, y = 190.dp)
                .background(
                    Brush.radialGradient(listOf(BlobCyan.copy(alpha = 0.30f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(240.dp)
                .offset(x = (-120).dp, y = 330.dp)
                .background(
                    Brush.radialGradient(listOf(BlobPink.copy(alpha = 0.28f), Color.Transparent)),
                    CircleShape
                )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        AccentPink.copy(alpha = 0.28f),
                        AccentSky.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun PrimaryButton(
    title: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.35f)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    if (enabled && !loading) {
                        Brush.linearGradient(listOf(AccentAmber, AccentPink, AccentSky))
                    } else {
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.45f), Color.Gray.copy(alpha = 0.25f)))
                    },
                    RoundedCornerShape(20.dp)
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SecondaryButton(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.14f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 15.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

/** Section heading used on result screens. */
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = AccentSky,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}
