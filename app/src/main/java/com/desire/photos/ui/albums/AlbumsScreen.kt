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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.desire.photos.data.model.MediaItem
import com.desire.photos.data.remote.AlbumDto
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.home.PhotoPreviewPager
import com.desire.photos.ui.home.PreviewInfo
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoIconButton
import com.desire.photos.ui.theme.NeoSurface

@Composable
fun AlbumsScreen(viewModel: AlbumsViewModel = viewModel()) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var openAlbum by remember { mutableStateOf<AlbumDto?>(null) }
    var localMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var openSmartCategory by remember { mutableStateOf<Pair<SmartCategory, List<MediaItem>>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
        localMedia = ServiceLocator.mediaRepository.queryMedia()
    }

    val openSmart = openSmartCategory
    if (openSmart != null) {
        SmartCategoryDetailScreen(
            category = openSmart.first,
            items = openSmart.second,
            onBack = { openSmartCategory = null },
        )
        return
    }

    val current = openAlbum
    if (current != null) {
        AlbumDetailScreen(
            album = current,
            onBack = { openAlbum = null; viewModel.load() },
            onDeleted = { openAlbum = null; viewModel.deleteAlbum(current.id) },
        )
        return
    }

    val smartCategories = remember(localMedia) {
        SMART_CATEGORIES.mapNotNull { cat ->
            val catItems = localMedia.filter { cat.matches(it.bucket ?: "") }
            if (catItems.isEmpty()) null else cat to catItems
        }
    }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        if (loading && albums.isEmpty() && smartCategories.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Neo.primary)
            }
        } else {
            // One grid for everything (smart + manual albums) so both kinds of
            // cards are laid out by the exact same GridCells.Adaptive columns —
            // guarantees they're sized and styled identically, not just similarly.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Albums", style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = Neo.text)
                            Text("${albums.size} folder${if (albums.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall, color = Neo.muted)
                        }
                        NeoButton("New folder", { showCreateDialog = true }, icon = Icons.Filled.CreateNewFolder)
                    }
                }

                if (smartCategories.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Smart albums", fontWeight = FontWeight.SemiBold, color = Neo.text,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(smartCategories, key = { "smart_${it.first.id}" }) { (category, catItems) ->
                        SmartCategoryCard(category, catItems) { openSmartCategory = category to catItems }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "My albums", fontWeight = FontWeight.SemiBold, color = Neo.text,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                        )
                    }
                }

                if (albums.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyAlbums { showCreateDialog = true }
                    }
                } else {
                    items(albums, key = { it.id }) { album ->
                        AlbumCard(album) { openAlbum = album }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                viewModel.createAlbum(title) { showCreateDialog = false }
            },
        )
    }
}

@Composable
private fun AlbumCard(album: AlbumDto, onClick: () -> Unit) {
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
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Folder, null, tint = Neo.primary.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
            }
            Column(Modifier.padding(12.dp)) {
                Text(album.title, fontWeight = FontWeight.SemiBold, color = Neo.text,
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text("${album.mediaCount} item${if (album.mediaCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall, color = Neo.muted)
            }
        }
    }
}

@Composable
private fun AlbumDetailScreen(album: AlbumDto, onBack: () -> Unit, onDeleted: () -> Unit) {
    val api = ServiceLocator.apiClient
    var items by remember { mutableStateOf<List<MediaDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoItems = remember(items) { items.filter { !it.isVideo } }

    BackHandler(onBack = onBack)

    LaunchedEffect(album.id) {
        loading = true
        val ids = api.getAlbumMediaIds(album.id).getOrDefault(emptyList()).toSet()
        val all = api.listAllMedia().getOrDefault(emptyList())
        items = all.filter { it.id in ids && !it.deleted }
        loading = false
    }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                NeoIconButton(Icons.Filled.ArrowBack, "Back", onBack, tint = Neo.text)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(album.title, fontWeight = FontWeight.Bold, color = Neo.text,
                        style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text("${items.size} item${if (items.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall, color = Neo.muted)
                }
                NeoIconButton(Icons.Filled.Delete, "Delete folder", onDeleted, tint = Neo.danger)
            }

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Neo.primary)
                }
                items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing in this folder yet", color = Neo.muted)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.id }) { dto ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Neo.dark.copy(alpha = 0.25f))
                                .clickable {
                                    if (!dto.isVideo) previewIndex = photoItems.indexOf(dto)
                                },
                        ) {
                            if (!dto.isVideo) {
                                AsyncImage(model = api.contentUrl(dto), contentDescription = dto.fileName,
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
    }

    val idx = previewIndex
    if (idx != null && idx < photoItems.size) {
        PhotoPreviewPager(
            count = photoItems.size,
            initialIndex = idx,
            onDismiss = { previewIndex = null },
            modelAt = { i -> api.contentUrl(photoItems[i]) },
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
private fun CreateAlbumDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        NeoSurface(cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
            Column {
                Text("New folder", fontWeight = FontWeight.Bold, color = Neo.text,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Neo.primary,
                        unfocusedBorderColor = Neo.dark,
                        focusedLabelColor = Neo.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeoButton("Cancel", onDismiss, contentColor = Neo.muted, modifier = Modifier.weight(1f))
                    NeoButton("Create", { onCreate(title) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyAlbums(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NeoSurface(cornerRadius = 40.dp, contentPadding = PaddingValues(24.dp)) {
            Icon(Icons.Filled.Folder, null, tint = Neo.primary, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No folders yet", fontWeight = FontWeight.Bold, color = Neo.text,
            style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Make a folder to group photos together — from here, or from any photo.",
            style = MaterialTheme.typography.bodyMedium, color = Neo.muted)
        Spacer(Modifier.height(20.dp))
        NeoButton("New folder", onCreate, icon = Icons.Filled.CreateNewFolder)
    }
}
