package com.desire.photos.ui.search

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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.home.PhotoPreviewPager
import com.desire.photos.ui.home.PreviewInfo
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoChip
import com.desire.photos.ui.theme.NeoSurface

/**
 * Natural-language search over backed-up photos (matches the AI caption/tags
 * generated at upload time) plus a "Documents" filter for photos the AI
 * flagged as IDs/receipts/certificates — both driven by the same
 * server-side analysis, so this screen is just filtering, no extra AI calls.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel = viewModel()) {
    val api = ServiceLocator.apiClient
    val query by viewModel.query.collectAsStateWithLifecycle()
    val documentsOnly by viewModel.documentsOnly.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val photoResults = remember(results) { results.filter { !it.isVideo } }

    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        Column(Modifier.fillMaxSize()) {
            Text("Search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = Neo.text, modifier = Modifier.padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                placeholder = { Text("Search your photos…") },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = Neo.muted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Neo.primary,
                    unfocusedBorderColor = Neo.dark,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 16.dp)) {
                NeoChip("All", !documentsOnly) { viewModel.documentsOnly.value = false }
                NeoChip("Documents", documentsOnly) { viewModel.documentsOnly.value = true }
            }
            Spacer(Modifier.height(10.dp))

            when {
                loading && results.isEmpty() && (documentsOnly || query.isNotBlank()) ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Neo.primary)
                    }
                results.isEmpty() -> EmptySearch(documentsOnly, query.isNotBlank())
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(results, key = { it.id }) { dto ->
                        SearchResultCell(dto) { if (!dto.isVideo) previewIndex = photoResults.indexOf(dto) }
                    }
                }
            }
        }
    }

    val idx = previewIndex
    if (idx != null && idx < photoResults.size) {
        PhotoPreviewPager(
            count = photoResults.size,
            initialIndex = idx,
            onDismiss = { previewIndex = null },
            modelAt = { i -> api.contentUrl(photoResults[i]) },
            infoAt = { i ->
                val dto = photoResults[i]
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
            onDeleted = { i -> viewModel.remove(photoResults[i].id) },
        )
    }
}

@Composable
private fun SearchResultCell(dto: MediaDto, onClick: () -> Unit) {
    val api = ServiceLocator.apiClient
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Neo.dark.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
    ) {
        if (!dto.isVideo) {
            AsyncImage(
                model = api.contentUrl(dto),
                contentDescription = dto.aiCaption ?: dto.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(30.dp))
        }
        if (dto.isDocument) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp)).padding(4.dp),
            ) {
                Icon(Icons.Filled.Description, "Document", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun EmptySearch(documentsOnly: Boolean, hasQuery: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoSurface(cornerRadius = 40.dp, contentPadding = PaddingValues(24.dp)) {
            Icon(
                if (documentsOnly) Icons.Filled.Description else Icons.Filled.Search,
                null, tint = Neo.primary, modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            when {
                documentsOnly -> "No documents found"
                hasQuery -> "No matches"
                else -> "Search your photos"
            },
            fontWeight = FontWeight.Bold, color = Neo.text, style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                documentsOnly -> "Photos of IDs, receipts, and certificates show up here once they're backed up and analyzed."
                hasQuery -> "Try different words — search matches what's in the photo, not just the filename."
                else -> "Type what you remember — \"beach\", \"birthday cake\", a place, a color. Matches what's actually in the photo."
            },
            style = MaterialTheme.typography.bodyMedium, color = Neo.muted,
        )
    }
}
