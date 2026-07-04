package eu.kanade.tachiyomi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen

/**
 * FORK: Phase 1 placeholder home screen.
 *
 * Replaces Aniyomi's default home (the tab navigator) with a minimal stub.
 * The real AniList-driven home is Phase 2. Tapping "Library" pushes [HomeScreen]
 * (the tab navigator with manga gated out); "Settings" pushes [SettingsScreen].
 *
 * This is intentionally minimal — not a polished screen. Phase 2 builds the real thing.
 */
object PlaceholderHomeScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Kuta",
                    style = MaterialTheme.typography.displayMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AniList browse coming soon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { navigator.push(HomeScreen) }) {
                    Text("Library")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { navigator.push(SettingsScreen()) }) {
                    Text("Settings")
                }
            }
        }
    }
}
