package com.desire.photos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.albums.AlbumsScreen
import com.desire.photos.ui.auth.AuthViewModel
import com.desire.photos.ui.auth.LoginScreen
import com.desire.photos.ui.home.HomeScreen
import com.desire.photos.ui.places.PlacesScreen
import com.desire.photos.ui.search.SearchScreen
import com.desire.photos.ui.settings.SettingsScreen
import com.desire.photos.ui.share.SharedScreen
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoPalettes
import com.desire.photos.ui.theme.PhotosTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    /** Compose can't observe a plain var, so the current share id lives in a MutableState. */
    private val sharedIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedIdState.value = shareIdFromIntent(intent)

        setContent {
            // Keep Neo.current in sync with the persisted choice — live, so picking a
            // new theme in Settings re-themes every screen immediately.
            LaunchedEffect(Unit) {
                ServiceLocator.settingsRepository.settings
                    .map { it.themeId }
                    .collect { Neo.current = NeoPalettes.byId(it) }
            }

            PhotosTheme {
                val sharedId by sharedIdState
                if (sharedId != null) {
                    // Public, no-login view — takes over the whole screen regardless
                    // of whether the visitor is signed in to their own account.
                    SharedScreen(shareId = sharedId!!, onClose = { sharedIdState.value = null })
                } else {
                    val authViewModel: AuthViewModel = viewModel()
                    val user by authViewModel.user.collectAsStateWithLifecycle()

                    if (user == null) {
                        LoginScreen(authViewModel)
                    } else {
                        MainScaffold()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shareIdFromIntent(intent)?.let { sharedIdState.value = it }
    }

    /** Extracts {id} from a .../share/{id} deep link, or null if this intent isn't one. */
    private fun shareIdFromIntent(intent: Intent?): String? {
        val data: Uri = intent?.data ?: return null
        val segments = data.pathSegments
        val shareIndex = segments.indexOf("share")
        if (shareIndex == -1 || shareIndex + 1 >= segments.size) return null
        return segments[shareIndex + 1]
    }
}

@Composable
private fun MainScaffold() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = tab != 0) { tab = 0 }
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Neo.primary,
        selectedTextColor = Neo.primary,
        indicatorColor = Neo.bg,
        unselectedIconColor = Neo.muted,
        unselectedTextColor = Neo.muted,
    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Neo.bg,
        bottomBar = {
            NavigationBar(containerColor = Neo.surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    label = { Text("Photos") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Albums") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("Places") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Search") },
                    colors = itemColors,
                )
                NavigationBarItem(
                    selected = tab == 4,
                    onClick = { tab = 4 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    colors = itemColors,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> HomeScreen()
                1 -> AlbumsScreen()
                2 -> PlacesScreen()
                3 -> SearchScreen()
                else -> SettingsScreen()
            }
        }
    }
}
