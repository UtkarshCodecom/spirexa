package com.desire.photos.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.data.remote.PublicShareDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.home.PhotoPreviewPager
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoIconButton

/**
 * Public, read-only viewer for a shared album — no sign-in required. Reached
 * via a `https://.../share/{id}` deep link (see AndroidManifest.xml) or,
 * before that link is tapped, straight from MainActivity when it detects one
 * in the launching intent.
 */
@Composable
fun SharedScreen(shareId: String, onClose: () -> Unit) {
    var state by remember(shareId) { mutableStateOf<ShareLoadState>(ShareLoadState.Loading) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    // Videos aren't shown in the swipeable viewer — this is the flat, navigable list previewIndex points into.
    val photoItems = remember(state) {
        (state as? ShareLoadState.Loaded)?.data?.media?.filter { !it.isVideo } ?: emptyList()
    }

    LaunchedEffect(shareId) {
        state = ShareLoadState.Loading
        ServiceLocator.apiClient.getPublicShare(shareId)
            .onSuccess { state = ShareLoadState.Loaded(it) }
            .onFailure { state = ShareLoadState.Error(it.message ?: "This link isn't available") }
    }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        when (val s = state) {
            is ShareLoadState.Loading -> {
                CircularProgressIndicator(color = Neo.primary, modifier = Modifier.align(Alignment.Center))
            }
            is ShareLoadState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.CloudOff, null, tint = Neo.muted, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Link unavailable", fontWeight = FontWeight.Bold, color = Neo.text)
                    Spacer(Modifier.height(4.dp))
                    Text(s.message, color = Neo.muted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(20.dp))
                    NeoButton("Close", onClose)
                }
            }
            is ShareLoadState.Loaded -> {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.data.title, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = Neo.text)
                            Text("${s.data.media.size} shared with you",
                                style = MaterialTheme.typography.bodySmall, color = Neo.muted)
                        }
                        NeoIconButton(Icons.Filled.Close, "Close", onClose, tint = Neo.muted)
                    }

                    if (s.data.media.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nothing to show here", color = Neo.muted)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 104.dp),
                            contentPadding = PaddingValues(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(s.data.media, key = { it.id }) { dto ->
                                SharedMediaCell(dto) { if (!dto.isVideo) previewIndex = photoItems.indexOf(dto) }
                            }
                        }
                    }
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
            // infoAt defaults to null: bare viewer, no favorite/delete/add-to-folder —
            // a visitor viewing someone else's shared link shouldn't get those controls.
        )
    }
}

@Composable
private fun SharedMediaCell(dto: MediaDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Neo.dark.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ServiceLocator.apiClient.contentUrl(dto),
            contentDescription = dto.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (dto.isVideo) {
            Icon(
                Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(30.dp),
            )
        }
    }
}

private sealed interface ShareLoadState {
    data object Loading : ShareLoadState
    data class Loaded(val data: PublicShareDto) : ShareLoadState
    data class Error(val message: String) : ShareLoadState
}
