package com.desire.photos.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.albums.AddToAlbumDialog
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoSurface
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Details shown in the info panel and used by the action bar; all optional. */
data class PreviewInfo(
    val title: String,
    val dateSec: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false,
    /** Non-null only for items actually backed up — gates favorite/delete/add-to-folder. */
    val serverId: String? = null,
    /** Non-null when the file is on this device — gates the Share action. */
    val localUri: Uri? = null,
    /** AI-generated search caption, once background analysis has finished. */
    val aiCaption: String? = null,
    /** Transcribed text, present only when this was detected as a document. */
    val documentText: String? = null,
)

/**
 * Full-screen, swipeable, pinch-to-zoom photo viewer — swipe left/right to
 * move between [count] items, like every other photos app. [modelAt]/[infoAt]
 * resolve lazily per page rather than needing the whole list built up front.
 * A page whose [infoAt] returns null renders as a plain viewer with no action
 * bar (used for the public share-link screen, where a visitor shouldn't see
 * favorite/delete/add-to-folder controls on someone else's photos).
 */
@Composable
fun PhotoPreviewPager(
    count: Int,
    initialIndex: Int,
    onDismiss: () -> Unit,
    modelAt: (Int) -> Any?,
    infoAt: (Int) -> PreviewInfo? = { null },
    onDeleted: ((Int) -> Unit)? = null,
) {
    if (count <= 0) {
        onDismiss()
        return
    }
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, count - 1)) { count }
    var zoomedIn by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { zoomedIn = false }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomedIn,
            modifier = Modifier.fillMaxSize().background(Color(0xFF101216)),
        ) { page ->
            PhotoPreviewPage(
                model = modelAt(page),
                info = infoAt(page),
                isActivePage = page == pagerState.currentPage,
                onZoomChanged = { zoomedIn = it },
                onDismiss = onDismiss,
                onDeleted = onDeleted?.let { cb -> ({ cb(page) }) },
            )
        }
    }
}

@Composable
private fun PhotoPreviewPage(
    model: Any?,
    info: PreviewInfo?,
    isActivePage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onDeleted: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInfoPanel by remember { mutableStateOf(false) }
    var showAddToFolder by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var isFavorite by remember(info) { mutableStateOf(info?.isFavorite ?: false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (info == null) onDismiss() },
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else 2.5f
                            offset = Offset.Zero
                            if (isActivePage) onZoomChanged(scale > 1f)
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + pan else Offset.Zero
                        if (isActivePage) onZoomChanged(scale > 1f)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )

        RoundIconButton(
            Icons.Filled.Close, "Close",
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            onClick = onDismiss,
        )

        if (info != null) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (info.serverId != null) {
                    RoundIconButton(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Favorite",
                        tint = if (isFavorite) Color(0xFFE1665A) else Color.White,
                        onClick = {
                            isFavorite = !isFavorite
                            scope.launch { ServiceLocator.apiClient.toggleFavorite(info.serverId) }
                        },
                    )
                }
                RoundIconButton(Icons.Filled.Info, "Info") { showInfoPanel = !showInfoPanel }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                if (info.localUri != null) {
                    BottomAction(Icons.Filled.Share, "Share") { shareLocalFile(context, info.localUri) }
                }
                if (info.serverId != null) {
                    BottomAction(Icons.Filled.CreateNewFolder, "Add to folder") { showAddToFolder = true }
                    BottomAction(Icons.Filled.Delete, "Delete", tint = Color(0xFFFF7A6F)) { showDeleteConfirm = true }
                }
            }

            if (showInfoPanel) {
                InfoPanel(info, isFavorite, Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    if (showAddToFolder && info?.serverId != null) {
        AddToAlbumDialog(
            mediaIds = listOf(info.serverId),
            onDismiss = { showAddToFolder = false },
            onAdded = { showAddToFolder = false },
        )
    }

    if (showDeleteConfirm && info?.serverId != null) {
        DeleteConfirmDialog(
            deleting = deleting,
            onCancel = { showDeleteConfirm = false },
            onConfirm = {
                scope.launch {
                    deleting = true
                    val result = ServiceLocator.apiClient.permanentDelete(info.serverId)
                    deleting = false
                    showDeleteConfirm = false
                    result.onSuccess { onDeleted?.invoke() }
                }
            },
        )
    }
}

@Composable
private fun DeleteConfirmDialog(deleting: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = { if (!deleting) onCancel() }) {
        NeoSurface(cornerRadius = 26.dp, contentPadding = PaddingValues(20.dp)) {
            Column {
                Text("Delete this photo?", fontWeight = FontWeight.Bold, color = Neo.text,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This removes it from your backup. It can't be undone. Your device copy, if any, is untouched.",
                    style = MaterialTheme.typography.bodyMedium, color = Neo.muted,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeoButton("Cancel", onCancel, contentColor = Neo.muted, modifier = Modifier.weight(1f))
                    NeoButton(
                        if (deleting) "Deleting…" else "Delete",
                        { if (!deleting) onConfirm() },
                        contentColor = Neo.danger,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(Color(0x55000000), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier.size(48.dp).background(Color(0x55000000), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun InfoPanel(info: PreviewInfo, isFavorite: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE6181B22), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(20.dp),
    ) {
        Text(info.title, color = Color.White, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge)
        if (info.aiCaption != null) {
            Spacer(Modifier.height(4.dp))
            Text(info.aiCaption, color = Color(0xFFAAB2C0), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        InfoRow("Date", formatDate(info.dateSec))
        if (info.sizeBytes > 0) InfoRow("Size", formatSize(info.sizeBytes))
        if (info.width > 0 && info.height > 0) InfoRow("Dimensions", "${info.width} × ${info.height}")
        InfoRow("Status", if (info.serverId != null) "Backed up" + (if (isFavorite) " · Favorite" else "") else "Not backed up")
        if (info.documentText != null) {
            Spacer(Modifier.height(10.dp))
            Text("Detected text", color = Color(0xFFAAB2C0), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                info.documentText, color = Color.White, style = MaterialTheme.typography.bodySmall,
                maxLines = 6, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFFAAB2C0), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 12.dp))
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatDate(sec: Long): String {
    if (sec <= 0) return "Unknown"
    val fmt = SimpleDateFormat("d MMMM yyyy · h:mm a", Locale.getDefault())
    return fmt.format(Date(sec * 1000))
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun shareLocalFile(context: android.content.Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share")) }
}
