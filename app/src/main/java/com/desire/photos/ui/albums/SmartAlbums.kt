package com.desire.photos.ui.albums

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.desire.photos.data.model.MediaItem
import com.desire.photos.ui.home.PhotoPreviewPager
import com.desire.photos.ui.home.PreviewInfo
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoIconButton
import com.desire.photos.ui.theme.NeoSurface

/** A predefined, auto-populated album based on which on-device folder a photo lives in. */
data class SmartCategory(val id: String, val label: String, val icon: ImageVector, val matches: (String) -> Boolean)

val SMART_CATEGORIES: List<SmartCategory> = listOf(
    SmartCategory("camera", "Camera", Icons.Filled.PhotoCamera) { it.equals("camera", true) || it.equals("dcim", true) },
    SmartCategory("screenshots", "Screenshots", Icons.Filled.Screenshot) { it.contains("screenshot", true) },
    SmartCategory("whatsapp", "WhatsApp", Icons.Filled.Chat) { it.contains("whatsapp", true) },
    SmartCategory("instagram", "Instagram", Icons.Filled.CameraAlt) { it.contains("instagram", true) },
    SmartCategory("telegram", "Telegram", Icons.Filled.Send) { it.contains("telegram", true) },
    SmartCategory("snapchat", "Snapchat", Icons.Filled.Bolt) { it.contains("snapchat", true) },
    SmartCategory("downloads", "Downloads", Icons.Filled.Screenshot) { it.contains("download", true) },
)

/**
 * Same card shell as a manually-created album (AlbumsScreen.kt's AlbumCard) —
 * NeoSurface, 20dp corners, 1.3f cover box, identical text layout below —
 * placed into the exact same GridCells.Adaptive(150dp) grid as those cards,
 * so a smart album looks and sizes exactly like one you created yourself.
 * The only visual difference is a real cover photo instead of a folder icon,
 * plus a small badge for which app/source it came from.
 */
@Composable
fun SmartCategoryCard(category: SmartCategory, items: List<MediaItem>, onClick: () -> Unit) {
    val cover = items.firstOrNull { !it.isVideo } ?: items.first()
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
                    AsyncImage(model = cover.uri, contentDescription = category.label,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(6.dp).size(24.dp)
                        .clip(CircleShape).background(Neo.primary),
                    contentAlignment = Alignment.Center,
                ) { Icon(category.icon, null, tint = Neo.onPrimary, modifier = Modifier.size(13.dp)) }
            }
            Column(Modifier.padding(12.dp)) {
                Text(category.label, fontWeight = FontWeight.SemiBold, color = Neo.text,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${items.size} item${if (items.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall, color = Neo.muted)
            }
        }
    }
}

@Composable
fun SmartCategoryDetailScreen(category: SmartCategory, items: List<MediaItem>, onBack: () -> Unit) {
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoItems = remember(items) { items.filter { !it.isVideo } }

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                NeoIconButton(Icons.Filled.ArrowBack, "Back", onBack, tint = Neo.text)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(category.label, fontWeight = FontWeight.Bold, color = Neo.text,
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
                items(items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Neo.dark.copy(alpha = 0.25f))
                            .clickable { if (!item.isVideo) previewIndex = photoItems.indexOf(item) },
                    ) {
                        if (!item.isVideo) {
                            AsyncImage(model = item.uri, contentDescription = item.displayName,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.align(Alignment.Center).size(30.dp))
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
            modelAt = { i -> photoItems[i].uri },
            infoAt = { i ->
                val item = photoItems[i]
                PreviewInfo(
                    title = item.displayName,
                    dateSec = item.dateAddedSec,
                    sizeBytes = item.sizeBytes,
                    localUri = item.uri,
                )
            },
        )
    }
}
