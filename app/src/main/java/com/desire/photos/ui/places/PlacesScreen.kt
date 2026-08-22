package com.desire.photos.ui.places

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.MediaPermissions
import com.desire.photos.ui.home.PhotoPreviewPager
import com.desire.photos.ui.home.PreviewInfo
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoIconButton
import com.desire.photos.ui.theme.NeoSurface

@Composable
fun PlacesScreen(viewModel: PlacesViewModel = viewModel()) {
    val context = LocalContext.current
    val places by viewModel.places.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var openPlace by remember { mutableStateOf<Place?>(null) }
    var hasLocationPermission by remember { mutableStateOf(MediaPermissions.hasOptionalAccess(context)) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = MediaPermissions.hasOptionalAccess(context)
        viewModel.load(context)
    }

    LaunchedEffect(Unit) { viewModel.load(context) }

    val current = openPlace
    if (current != null) {
        PlaceDetailScreen(place = current, onBack = { openPlace = null })
        return
    }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        Column(Modifier.fillMaxSize()) {
            Text("Places", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = Neo.text, modifier = Modifier.padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 4.dp))
            Text("Backed-up photos, grouped by where they were taken",
                style = MaterialTheme.typography.bodySmall, color = Neo.muted,
                modifier = Modifier.padding(start = 22.dp, bottom = 12.dp))

            when {
                loading && places.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neo.primary)
                }
                places.isEmpty() -> EmptyPlaces(
                    showPermissionHint = !hasLocationPermission,
                    onGrantPermission = { permLauncher.launch(MediaPermissions.optional()) },
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(places, key = { it.key }) { place ->
                        PlaceCard(place) { openPlace = place }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: Place, onClick: () -> Unit) {
    val cover = place.items.firstOrNull { !it.isVideo } ?: place.items.first()
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(14.dp))
                    .background(Neo.dark.copy(alpha = 0.25f)),
            ) {
                if (!cover.isVideo) {
                    AsyncImage(
                        model = ServiceLocator.apiClient.contentUrl(cover),
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(28.dp))
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(place.name, fontWeight = FontWeight.SemiBold, color = Neo.text,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${place.items.size} item${if (place.items.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall, color = Neo.muted)
            }
        }
    }
}

@Composable
private fun PlaceDetailScreen(place: Place, onBack: () -> Unit) {
    var items by remember(place) { mutableStateOf(place.items) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoItems = remember(items) { items.filter { !it.isVideo } }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                NeoIconButton(Icons.Filled.ArrowBack, "Back", onBack, tint = Neo.text)
                Column(Modifier.align(Alignment.Center)) {
                    Text(place.name, fontWeight = FontWeight.Bold, color = Neo.text,
                        style = MaterialTheme.typography.titleMedium)
                    Text("${items.size} item${if (items.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall, color = Neo.muted)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(items, key = { it.id }) { dto ->
                    PlaceMediaCell(dto) { if (!dto.isVideo) previewIndex = photoItems.indexOf(dto) }
                }
            }
        }
    }

    val idx = previewIndex
    if (idx != null && idx < photoItems.size) {
        PhotoPreviewPager(
            count = photoItems.size,
            initialIndex = idx,
            onDismiss = { previewIndex = null },
            modelAt = { i -> ServiceLocator.apiClient.contentUrl(photoItems[i]) },
            infoAt = { i ->
                val dto = photoItems[i]
                PreviewInfo(
                    title = dto.fileName,
                    dateSec = dto.dateTakenSec,
                    sizeBytes = dto.sizeBytes,
                    width = dto.width,
                    height = dto.height,
                    isFavorite = dto.favorite,
                    serverId = dto.id,
                    aiCaption = dto.aiCaption,
                    documentText = dto.documentText,
                )
            },
            onDeleted = { i -> items = items.filterNot { it.id == photoItems[i].id } },
        )
    }
}

@Composable
private fun PlaceMediaCell(dto: MediaDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Neo.dark.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
    ) {
        if (!dto.isVideo) {
            AsyncImage(
                model = ServiceLocator.apiClient.contentUrl(dto),
                contentDescription = dto.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(30.dp))
        }
    }
}

@Composable
private fun EmptyPlaces(showPermissionHint: Boolean, onGrantPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoSurface(cornerRadius = 40.dp, contentPadding = PaddingValues(24.dp)) {
            Icon(Icons.Filled.Place, null, tint = Neo.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No places yet", fontWeight = FontWeight.Bold, color = Neo.text,
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (showPermissionHint) {
            Text(
                "Photos' location comes from your camera's EXIF data, which needs one more " +
                    "permission to read. Allow it, then back up a photo taken with location on.",
                style = MaterialTheme.typography.bodyMedium, color = Neo.muted,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            NeoButton("Allow photo locations", onGrantPermission)
        } else {
            Text(
                "Photos show up here once they're backed up and have location data. " +
                    "Not every photo has this — it depends on your camera app and settings.",
                style = MaterialTheme.typography.bodyMedium, color = Neo.muted,
            )
        }
    }
}
