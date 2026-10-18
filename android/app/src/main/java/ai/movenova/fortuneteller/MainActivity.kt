package ai.movenova.fortuneteller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ai.movenova.fortuneteller.data.AppLanguage
import ai.movenova.fortuneteller.data.AppSettings
import ai.movenova.fortuneteller.data.PalmMatchResponse
import ai.movenova.fortuneteller.data.PalmReadingResponse
import ai.movenova.fortuneteller.data.PalmStore
import ai.movenova.fortuneteller.data.SavedReading
import ai.movenova.fortuneteller.ui.AccentAmber
import ai.movenova.fortuneteller.ui.FortuneTheme
import ai.movenova.fortuneteller.ui.LocalAppContext
import ai.movenova.fortuneteller.ui.str
import ai.movenova.fortuneteller.ui.screens.HistoryScreen
import ai.movenova.fortuneteller.ui.screens.MatchResultScreen
import ai.movenova.fortuneteller.ui.screens.MatchScreen
import ai.movenova.fortuneteller.ui.screens.ReadScreen
import ai.movenova.fortuneteller.ui.screens.ReadingResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings(applicationContext)
        val store = PalmStore(applicationContext)
        setContent { AppRoot(settings, store) }
    }
}

/** Result payloads handed to the result screens (kept in memory, not serialized into routes). */
class AppNavPayload {
    var reading: PalmReadingResponse? = null
    var readingImage: ByteArray? = null
    var match: PalmMatchResponse? = null
    var matchImageA: ByteArray? = null
    var matchImageB: ByteArray? = null
    var saved: SavedReading? = null
}

private data class Tab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun AppRoot(settings: AppSettings, store: PalmStore) {
    val language by settings.language.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val localized = remember(language) { settings.localizedContext(baseContext) }

    CompositionLocalProvider(LocalAppContext provides localized) {
        FortuneTheme {
            val navController = rememberNavController()
            val payload = remember { AppNavPayload() }
            val tabs = listOf(
                Tab("read", R.string.nav_read, Icons.Filled.Spa),
                Tab("match", R.string.nav_match, Icons.Filled.Favorite),
                Tab("history", R.string.nav_history, Icons.Filled.History)
            )
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination

            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF17132E)) {
                        tabs.forEach { tab ->
                            val selected = currentRoute?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = str(tab.labelRes)) },
                                label = { Text(str(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentAmber,
                                    selectedTextColor = AccentAmber,
                                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "read",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("read") {
                        ReadScreen(
                            settings = settings,
                            onResult = { response, image ->
                                payload.reading = response
                                payload.readingImage = image
                                navController.navigate("readingResult")
                            }
                        )
                    }
                    composable("readingResult") {
                        ReadingResultScreen(
                            response = payload.reading,
                            imageBytes = payload.readingImage,
                            store = store,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    composable("match") {
                        MatchScreen(
                            settings = settings,
                            onResult = { response, a, b ->
                                payload.match = response
                                payload.matchImageA = a
                                payload.matchImageB = b
                                navController.navigate("matchResult")
                            }
                        )
                    }
                    composable("matchResult") {
                        MatchResultScreen(
                            response = payload.match,
                            imageABytes = payload.matchImageA,
                            imageBBytes = payload.matchImageB,
                            store = store,
                            onDone = { navController.popBackStack() }
                        )
                    }
                    composable("history") {
                        HistoryScreen(
                            store = store,
                            settings = settings,
                            onOpenReading = { saved ->
                                payload.saved = saved
                                navController.navigate("historyDetail")
                            }
                        )
                    }
                    composable("historyDetail") {
                        HistoryDetailRoute(payload.saved, store) { navController.popBackStack() }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailRoute(saved: SavedReading?, store: PalmStore, onDone: () -> Unit) {
    if (saved == null) {
        onDone()
        return
    }
    if (saved.isMatch) {
        MatchResultScreen(
            response = saved.match,
            imageABytes = null,
            imageBBytes = null,
            store = store,
            onDone = onDone,
            alreadySaved = true,
            savedImageABase64 = saved.imageBase64,
            savedImageBBase64 = saved.imageBBase64
        )
    } else {
        ReadingResultScreen(
            response = saved.reading,
            imageBytes = null,
            store = store,
            onDone = onDone,
            alreadySaved = true,
            savedImageBase64 = saved.imageBase64
        )
    }
}

/** Helper used by the language picker in the read screen. */
fun AppLanguage.displayName(localized: android.content.Context): String = when (this) {
    AppLanguage.SYSTEM -> localized.getString(R.string.language_system)
    AppLanguage.ENGLISH -> localized.getString(R.string.language_english)
    AppLanguage.CHINESE -> localized.getString(R.string.language_chinese)
}
