package com.desire.photos.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.desire.photos.data.remote.AlbumDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoSurface
import kotlinx.coroutines.launch

/**
 * Picks (or creates) a folder and adds [mediaIds] to it. Folders aren't
 * exclusive — a photo can be in several — so this is the one action behind
 * both "move to folder" and "copy to folder"; there's no separate concept of
 * removing a photo from the main library the way "move" usually implies.
 */
@Composable
fun AddToAlbumDialog(
    mediaIds: List<String>,
    onDismiss: () -> Unit,
    onAdded: (AlbumDto) -> Unit,
) {
    val api = ServiceLocator.apiClient
    val scope = rememberCoroutineScope()
    var albums by remember { mutableStateOf<List<AlbumDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var addingTo by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        api.listAlbums().onSuccess { albums = it }.onFailure { error = it.message }
        loading = false
    }

    suspend fun addTo(album: AlbumDto) {
        addingTo = album.id
        val result = api.addToAlbum(album.id, mediaIds)
        addingTo = null
        result.onSuccess { onAdded(album) }.onFailure { error = it.message ?: "Couldn't add to folder" }
    }

    Dialog(onDismissRequest = onDismiss) {
        NeoSurface(cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
            Column {
                Text(
                    if (mediaIds.size == 1) "Add to folder" else "Add ${mediaIds.size} to folder",
                    fontWeight = FontWeight.Bold, color = Neo.text,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))

                if (error != null) {
                    Text(error!!, color = Neo.danger, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Neo.primary, modifier = Modifier.size(24.dp))
                    }
                    albums.isEmpty() && !showCreate -> Text(
                        "No folders yet — create one below.",
                        style = MaterialTheme.typography.bodySmall, color = Neo.muted,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    else -> LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                        items(albums, key = { it.id }) { album ->
                            AlbumRow(album, isAdding = addingTo == album.id) {
                                scope.launch { addTo(album) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (showCreate) {
                    InlineCreateAlbumRow(
                        onCreate = { title ->
                            scope.launch {
                                loading = true
                                val created = api.createAlbum(title)
                                loading = false
                                showCreate = false
                                created.onSuccess { album -> addTo(album) }
                                    .onFailure { error = it.message }
                            }
                        },
                        onCancel = { showCreate = false },
                    )
                } else {
                    NeoButton("New folder", { showCreate = true }, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(10.dp))
                NeoButton("Close", onDismiss, contentColor = Neo.muted, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AlbumRow(album: AlbumDto, isAdding: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isAdding, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Neo.dark.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Folder, null, tint = Neo.primary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(album.title, color = Neo.text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("${album.mediaCount} item${if (album.mediaCount == 1) "" else "s"}",
                color = Neo.muted, style = MaterialTheme.typography.labelSmall)
        }
        if (isAdding) {
            CircularProgressIndicator(color = Neo.primary, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Filled.CheckCircle, "Add", tint = Neo.dark, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InlineCreateAlbumRow(onCreate: (String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    Column {
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
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeoButton("Cancel", onCancel, contentColor = Neo.muted, modifier = Modifier.weight(1f))
            NeoButton("Create & add", { onCreate(title) }, modifier = Modifier.weight(1f))
        }
    }
}
