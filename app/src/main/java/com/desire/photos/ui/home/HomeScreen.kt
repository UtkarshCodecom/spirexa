package com.desire.photos.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.desire.photos.config.AppConfig
import com.desire.photos.data.model.UploadStatus
import com.desire.photos.ui.MediaPermissions
import com.desire.photos.ui.albums.AddToAlbumDialog
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoChip
import com.desire.photos.ui.theme.NeoIconButton
import com.desire.photos.ui.theme.NeoSurface
import com.desire.photos.ui.theme.neuRaised
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(MediaPermissions.hasAccess(context)) }
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    var showShareLinkDialog by remember { mutableStateOf(false) }
    var showAddToFolderDialog by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasPermission = MediaPermissions.hasAccess(context)
        if (hasPermission) viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.refresh() else permLauncher.launch(MediaPermissions.required() + MediaPermissions.optional())
    }

    Box(Modifier.fillMaxSize().background(Neo.bg)) {
        if (!hasPermission) {
            PermissionPrompt { permLauncher.launch(MediaPermissions.required() + MediaPermissions.optional()) }
            return@Box
        }

        val items by viewModel.items.collectAsStateWithLifecycle()
        val selection by viewModel.selection.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val progress by viewModel.progress.collectAsStateWithLifecycle()
        val uploaded by viewModel.uploadedCount.collectAsStateWithLifecycle()
        val total by viewModel.totalCount.collectAsStateWithLifecycle()
        // Videos open in an external player, not the swipeable viewer — this is
        // the flat, navigable list previewIndex points into.
        val previewablePhotos = remember(items) { items.filter { !it.item.isVideo } }

        Column(modifier = Modifier.fillMaxSize()) {
            Header(uploaded = uploaded, total = total, onBackupNow = viewModel::backupNow)

            if (!AppConfig.isApiConfigured) {
                WarningBanner("The Photos server isn't configured yet — set API_BASE_URL to enable backup.")
            }

            if (progress.running) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp)) {
                    LinearProgressIndicator(
                        progress = { if (progress.total > 0) progress.done.toFloat() / progress.total else 0f },
                        color = Neo.primary,
                        trackColor = Neo.dark.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                    )
                    Text(
                        progress.currentName?.let { "Backing up $it…" } ?: (progress.message ?: "Working…"),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Neo.muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            FilterRow(current = filter, onSelect = { viewModel.filter.value = it })

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos here", color = Neo.muted)
                }
            } else {
                val groups = remember(items) { groupByMonth(items) }
                val memories = remember(items) { buildMemories(items) }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (filter == HomeFilter.ALL) {
                        if (memories.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                MemoriesSection(memories) { ui ->
                                    when {
                                        ui.item.isVideo && ui.isLocal -> openVideo(context, ui.item.uri)
                                        ui.item.isVideo -> Unit
                                        else -> previewIndex = previewablePhotos.indexOf(ui)
                                    }
                                }
                            }
                        }
                    }
                    groups.forEach { (label, photos) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            DateHeader(label, photos.size)
                        }
                        items(photos, key = { it.item.id }) { ui ->
                            MediaCell(
                                ui = ui,
                                selected = ui.item.id in selection,
                                onClick = {
                                    when {
                                        selection.isNotEmpty() -> viewModel.toggleSelect(ui.item.id)
                                        ui.item.isVideo && ui.isLocal -> openVideo(context, ui.item.uri)
                                        ui.item.isVideo -> Unit // cloud-only video: no local file to preview
                                        else -> previewIndex = previewablePhotos.indexOf(ui)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelect(ui.item.id) },
                            )
                        }
                    }
                }
            }
        }

        // Floating selection action bar.
        if (selection.isNotEmpty()) {
            val allExcluded = items.filter { it.item.id in selection }.all { it.record?.excluded == true }
            SelectionBar(
                count = selection.size,
                allExcluded = allExcluded,
                onShare = {
                    val uris = items.filter { it.item.id in selection && it.isLocal }.map { it.item.uri }
                    shareItems(context, uris)
                },
                onShareLink = { showShareLinkDialog = true },
                onToggleExclude = { if (allExcluded) viewModel.includeSelected() else viewModel.excludeSelected() },
                onAddToFolder = { showAddToFolderDialog = true },
                onDelete = viewModel::deleteSelectedFromCloud,
                onClear = viewModel::clearSelection,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            )
        }

        if (showAddToFolderDialog) {
            val ids = items.filter { it.item.id in selection }.mapNotNull { it.record?.serverId }
            AddToAlbumDialog(
                mediaIds = ids,
                onDismiss = { showAddToFolderDialog = false },
                onAdded = { showAddToFolderDialog = false; viewModel.clearSelection() },
            )
        }

        val idx = previewIndex
        if (idx != null && idx < previewablePhotos.size) {
            PhotoPreviewPager(
                count = previewablePhotos.size,
                initialIndex = idx,
                onDismiss = { previewIndex = null },
                modelAt = { i -> previewablePhotos[i].let { if (it.isLocal) it.item.uri else it.cloudModel } },
                infoAt = { i ->
                    val ui = previewablePhotos[i]
                    PreviewInfo(
                        title = ui.item.displayName,
                        dateSec = ui.item.dateAddedSec,
                        sizeBytes = ui.item.sizeBytes,
                        isFavorite = ui.favorite,
                        serverId = ui.record?.serverId,
                        localUri = if (ui.isLocal) ui.item.uri else null,
                    )
                },
                onDeleted = { i -> previewablePhotos[i].record?.serverId?.let { viewModel.removeFromRemote(it) } },
            )
        }
    }

    if (showShareLinkDialog) {
        ShareLinkDialog(
            viewModel = viewModel,
            onDismiss = { showShareLinkDialog = false },
            onCreated = { url ->
                showShareLinkDialog = false
                shareLinkText(context, url)
            },
        )
    }
}

/* ---------- pieces ---------- */

@Composable
private fun Header(uploaded: Int, total: Int, onBackupNow: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Neo.text)
            Text("$uploaded of $total backed up", style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Neo.muted)
        }
        NeoButton(text = "Back up", icon = Icons.Filled.CloudUpload, onClick = onBackupNow)
    }
}

@Composable
private fun WarningBanner(text: String) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Neo.danger)
    }
}

@Composable
private fun FilterRow(current: HomeFilter, onSelect: (HomeFilter) -> Unit) {
    // Horizontally scrollable so a long label (e.g. "Excluded" on a narrow
    // screen) never wraps/overflows the row — it just scrolls instead.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HomeFilter.entries.forEach { f ->
            NeoChip(label = f.label(), selected = current == f, onClick = { onSelect(f) })
        }
    }
}

private fun HomeFilter.label() = when (this) {
    HomeFilter.ALL -> "All"
    HomeFilter.UPLOADED -> "Backed up"
    HomeFilter.EXCLUDED -> "Excluded"
}

@Composable
private fun DateHeader(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = Neo.text,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        Spacer(Modifier.size(8.dp))
        Text("$count", color = Neo.muted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    allExcluded: Boolean,
    onShare: () -> Unit,
    onShareLink: () -> Unit,
    onToggleExclude: () -> Unit,
    onAddToFolder: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NeoSurface(
        modifier = modifier,
        cornerRadius = 30.dp,
        elevation = 8.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$count", fontWeight = FontWeight.Bold, color = Neo.primary,
                modifier = Modifier.padding(start = 6.dp, end = 2.dp))
            NeoIconButton(Icons.Filled.Share, "Share files", onShare, size = 44.dp, tint = Neo.text)
            NeoIconButton(Icons.Filled.Link, "Share as link", onShareLink, size = 44.dp, tint = Neo.primary)
            NeoIconButton(
                if (allExcluded) Icons.Filled.Restore else Icons.Filled.Block,
                if (allExcluded) "Include in backup" else "Don't back up",
                onToggleExclude, size = 44.dp, tint = Neo.text,
            )
            NeoIconButton(Icons.Filled.CreateNewFolder, "Add to folder", onAddToFolder, size = 44.dp, tint = Neo.text)
            NeoIconButton(Icons.Filled.CloudOff, "Remove from backup", onDelete, size = 44.dp, tint = Neo.danger)
            NeoIconButton(Icons.Filled.Close, "Clear selection", onClear, size = 44.dp, tint = Neo.muted)
        }
    }
}

/**
 * Prompts for a title, then creates a public no-login share link for whichever
 * selected items are already backed up (items still uploading are skipped —
 * they have no server id yet — and the dialog says so up front).
 */
@Composable
private fun ShareLinkDialog(
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val selectedIds by viewModel.selection.collectAsStateWithLifecycle()
    val selectedCount = selectedIds.size
    val shareableCount = remember(selectedIds) { viewModel.selectedServerIds().size }
    var title by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!loading) onDismiss() }) {
        NeoSurface(cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
            Column {
                Text("Share as link", fontWeight = FontWeight.Bold, color = Neo.text,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (shareableCount < selectedCount) {
                        "$shareableCount of $selectedCount are backed up and will be shared. " +
                            "The rest are still uploading."
                    } else {
                        "Anyone with the link can view these $shareableCount item" +
                            (if (shareableCount == 1) "" else "s") + " — no account needed."
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Neo.muted,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    enabled = !loading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Neo.primary,
                        unfocusedBorderColor = Neo.dark,
                        focusedLabelColor = Neo.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Neo.danger, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeoButton("Cancel", onDismiss, contentColor = Neo.muted, modifier = Modifier.weight(1f))
                    if (loading) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Neo.primary, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        NeoButton("Create & share", {
                            loading = true
                            error = null
                            viewModel.createShare(title) { result ->
                                loading = false
                                result.onSuccess { onCreated(it.shareUrl) }
                                    .onFailure { error = it.message ?: "Couldn't create the link" }
                            }
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCell(
    ui: MediaUi,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val model: Any? = if (ui.isLocal) ui.item.uri else ui.cloudModel
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Neo.dark.copy(alpha = 0.25f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (model != null) {
            AsyncImage(model = model, contentDescription = ui.item.displayName,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }

        if (ui.item.isVideo) {
            Icon(Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(30.dp))
        }

        if (!ui.isLocal) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                    .background(Color(0x66000000), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Cloud, "In cloud", tint = Color.White, modifier = Modifier.size(12.dp)) }
        }

        StatusBadge(ui.status, Modifier.align(Alignment.BottomEnd).padding(5.dp))

        if (selected) {
            Box(Modifier.fillMaxSize().background(Neo.primary.copy(alpha = 0.28f)))
            Icon(Icons.Filled.CheckCircle, "Selected", tint = Neo.primary,
                modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                    .background(Color.White, CircleShape).size(20.dp))
        }
    }
}

@Composable
private fun StatusBadge(status: UploadStatus, modifier: Modifier = Modifier) {
    val (icon, tint) = when (status) {
        UploadStatus.UPLOADED -> Icons.Filled.CloudDone to Neo.success
        UploadStatus.UPLOADING -> Icons.Filled.Sync to Color.White
        UploadStatus.FAILED -> Icons.Filled.ErrorOutline to Neo.danger
        UploadStatus.EXCLUDED -> Icons.Filled.Block to Color(0xFFE0E0E0)
        UploadStatus.SKIPPED -> Icons.Filled.CloudOff to Color(0xFFE0E0E0)
        UploadStatus.PENDING, UploadStatus.NOT_UPLOADED -> Icons.Filled.CloudQueue to Color.White
    }
    Box(
        modifier = modifier.size(22.dp).background(Color(0x66000000), CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, status.name, tint = tint, modifier = Modifier.size(14.dp)) }
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NeoSurface(cornerRadius = 40.dp, contentPadding = PaddingValues(24.dp)) {
            Icon(Icons.Filled.CloudUpload, null, tint = Neo.primary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Allow access to your photos", fontWeight = FontWeight.Bold, color = Neo.text,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("We need permission to read your photos and videos so they can be backed up.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Neo.muted)
        Spacer(Modifier.height(22.dp))
        NeoButton("Grant permission", onRequest)
    }
}

/* ---------- helpers ---------- */

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "Good night"
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        hour < 21 -> "Good evening"
        else -> "Good night"
    }
}

private data class MemorySection(val title: String, val items: List<MediaUi>)

/** "On this day" for past years, plus a few "N months ago" call-backs. */
private fun buildMemories(items: List<MediaUi>): List<MemorySection> {
    if (items.isEmpty()) return emptyList()
    val sections = mutableListOf<MemorySection>()
    val today = Calendar.getInstance()
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val currentYear = today.get(Calendar.YEAR)

    for (yearsAgo in 1..15) {
        val year = currentYear - yearsAgo
        val matches = items.filter { ui ->
            if (ui.item.dateAddedSec <= 0) return@filter false
            val c = Calendar.getInstance().apply { timeInMillis = ui.item.dateAddedSec * 1000 }
            c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == todayMonth && c.get(Calendar.DAY_OF_MONTH) == todayDay
        }
        if (matches.isNotEmpty()) {
            val label = if (yearsAgo == 1) "On this day, last year" else "On this day, $yearsAgo years ago"
            sections.add(MemorySection(label, matches))
        }
    }

    for (monthsAgo in intArrayOf(1, 2, 3, 6, 9)) {
        val target = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsAgo) }
        val targetYear = target.get(Calendar.YEAR)
        val targetMonth = target.get(Calendar.MONTH)
        val matches = items.filter { ui ->
            if (ui.item.dateAddedSec <= 0) return@filter false
            val c = Calendar.getInstance().apply { timeInMillis = ui.item.dateAddedSec * 1000 }
            c.get(Calendar.YEAR) == targetYear && c.get(Calendar.MONTH) == targetMonth
        }
        if (matches.isNotEmpty()) {
            val label = if (monthsAgo == 1) "A month ago" else "$monthsAgo months ago"
            sections.add(MemorySection(label, matches))
        }
    }

    return sections
}

/**
 * One compact horizontal strip — a single card per memory, cover photo with
 * the label overlaid — like Google Photos' Memories row. Deliberately a
 * single row (not one row per memory) so this stays a small sliver of the
 * home screen regardless of how many memories exist today.
 */
@Composable
private fun MemoriesSection(sections: List<MemorySection>, onOpenPhoto: (MediaUi) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(sections, key = { it.title }) { section ->
            MemoryCard(section) { onOpenPhoto(section.items.first()) }
        }
    }
}

@Composable
private fun MemoryCard(section: MemorySection, onClick: () -> Unit) {
    val cover = section.items.first()
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Neo.dark.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
    ) {
        if (cover.item.isVideo && !cover.isLocal) {
            Icon(Icons.Filled.PlayCircle, "Video", tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(26.dp))
        } else {
            AsyncImage(
                model = if (cover.isLocal) cover.item.uri else cover.cloudModel,
                contentDescription = section.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                section.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                maxLines = 2,
            )
        }
    }
}

/** Groups the gallery by month, e.g. "August 2026" — mirrors Google Photos' all-photos view. */
private fun groupByMonth(items: List<MediaUi>): List<Pair<String, List<MediaUi>>> {
    val keyFmt = SimpleDateFormat("yyyyMM", Locale.getDefault())
    val labelFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val map = LinkedHashMap<String, Pair<String, MutableList<MediaUi>>>()
    for (ui in items) {
        val secs = ui.item.dateAddedSec
        val date = Date(if (secs > 0) secs * 1000 else 0)
        val key = if (secs > 0) keyFmt.format(date) else "backed-up"
        val label = if (secs > 0) labelFmt.format(date) else "Backed up"
        map.getOrPut(key) { label to mutableListOf() }.second.add(ui)
    }
    return map.values.map { it.first to it.second }
}

private fun shareItems(context: android.content.Context, uris: List<Uri>) {
    if (uris.isEmpty()) return
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share ${uris.size} items")) }
}

/** Opens the system share sheet with a plain-text share link (e.g. WhatsApp, SMS, copy). */
private fun shareLinkText(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share link")) }
}

private fun openVideo(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}
