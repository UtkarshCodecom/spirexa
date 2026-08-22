package com.desire.photos.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import com.desire.photos.data.settings.LargeMediaPolicy
import com.desire.photos.ui.BatteryOptimization
import com.desire.photos.ui.theme.Neo
import com.desire.photos.ui.theme.NeoButton
import com.desire.photos.ui.theme.NeoChip
import com.desire.photos.ui.theme.NeoPalette
import com.desire.photos.ui.theme.NeoPalettes
import com.desire.photos.ui.theme.NeoSurface

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neo.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 18.dp, bottom = 28.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Neo.text,
            modifier = Modifier.padding(start = 6.dp, bottom = 14.dp))

        // Account
        SectionCard {
            Text(viewModel.userEmail ?: "Signed in", style = MaterialTheme.typography.bodyLarge, color = Neo.text)
            Text("Files stored under: ${viewModel.userId?.take(12) ?: "—"}…",
                style = MaterialTheme.typography.bodySmall, color = Neo.muted)
            Spacer(Modifier.height(12.dp))
            NeoButton("Sign out", viewModel::signOut, contentColor = Neo.danger)
        }

        SectionTitle("Backup")
        SectionCard {
            SettingRow("Back up my photos", "Automatic backup on/off",
                settings.backupEnabled, onChange = viewModel::setEnabled)
        }

        SectionTitle("Theme")
        SectionCard {
            Text("Pick a look", style = MaterialTheme.typography.bodyLarge, color = Neo.text)
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                NeoPalettes.all.forEach { palette ->
                    ThemeSwatch(
                        palette = palette,
                        selected = settings.themeId == palette.id,
                        onClick = { viewModel.setThemeId(palette.id) },
                    )
                }
            }
        }

        SectionTitle("Reliable background backup")
        ReliabilityCard()

        SectionTitle("Network")
        SectionCard {
            SettingRow("Wi‑Fi", "Back up over Wi‑Fi",
                settings.backupOverWifi, settings.backupEnabled, viewModel::setWifi)
            Divider()
            SettingRow("Mobile data (5G/4G)", "Back up over mobile data",
                settings.backupOverMobile, settings.backupEnabled, viewModel::setMobile)
            Divider()
            SettingRow("Background", "Only when the phone is idle — you won't feel it",
                settings.backupInBackground, settings.backupEnabled, viewModel::setBackground)
        }

        SectionTitle("What to back up")
        SectionCard {
            SettingRow("Include videos", "Turn off to back up photos only",
                settings.backupVideos, settings.backupEnabled, viewModel::setBackupVideos)
            Divider()
            Text("Skip files larger than", style = MaterialTheme.typography.bodyLarge, color = Neo.text,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SizeOption(0, "No limit", settings.maxUploadMb, viewModel::setMaxUploadMb)
                SizeOption(25, "25 MB", settings.maxUploadMb, viewModel::setMaxUploadMb)
                SizeOption(50, "50 MB", settings.maxUploadMb, viewModel::setMaxUploadMb)
                SizeOption(100, "100 MB", settings.maxUploadMb, viewModel::setMaxUploadMb)
            }
        }

        SectionTitle("Large photos")
        SectionCard {
            SettingRow("Reduce quality of large photos", "Recompress big images to save space & data",
                settings.degradeLargeMedia, settings.backupEnabled, viewModel::setDegrade)
            Divider()
            Text("Upload large files (over 15 MB)", style = MaterialTheme.typography.bodyLarge, color = Neo.text,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LargeMediaPolicy.entries.forEach { policy ->
                    NeoChip(policy.label(), settings.largeMediaPolicy == policy) { viewModel.setLargePolicy(policy) }
                }
            }
            Text(settings.largeMediaPolicy.explanation(),
                style = MaterialTheme.typography.bodySmall, color = Neo.muted,
                modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private fun LargeMediaPolicy.label() = when (this) {
    LargeMediaPolicy.ALWAYS -> "Always"
    LargeMediaPolicy.SOMETIMES -> "Sometimes"
    LargeMediaPolicy.NEVER -> "Never"
}

private fun LargeMediaPolicy.explanation() = when (this) {
    LargeMediaPolicy.ALWAYS -> "Every large photo and video is uploaded."
    LargeMediaPolicy.SOMETIMES -> "Only some large files are uploaded each pass, to save data."
    LargeMediaPolicy.NEVER -> "Large files are skipped and never backed up."
}

@Composable
private fun SizeOption(value: Int, label: String, current: Int, onSelect: (Int) -> Unit) {
    NeoChip(label = label, selected = current == value, onClick = { onSelect(value) })
}

@Composable
private fun ThemeSwatch(palette: NeoPalette, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(palette.bg)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) palette.primary else palette.dark.copy(alpha = 0.5f),
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(palette.primary))
            if (selected) {
                Icon(Icons.Filled.CheckCircle, null, tint = palette.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(palette.displayName, style = MaterialTheme.typography.labelSmall, color = Neo.muted)
    }
}

@Composable
private fun ReliabilityCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var ignored by remember { mutableStateOf(BatteryOptimization.isIgnored(context)) }

    // Re-check when returning from the system dialog.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) ignored = BatteryOptimization.isIgnored(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for notification permission first (so the "Backing up…" notice can show), then battery.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { runCatching { context.startActivity(BatteryOptimization.requestIntent(context)) } }

    SectionCard {
        Text("Keep backing up when closed", style = MaterialTheme.typography.bodyLarge, color = Neo.text)
        Text(
            "Lets Photos finish uploading even after you swipe it away or the screen is off.",
            style = MaterialTheme.typography.bodySmall, color = Neo.muted,
        )
        Spacer(Modifier.height(12.dp))
        if (ignored) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Neo.success, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Battery optimization is off", color = Neo.text)
            }
        } else {
            NeoButton("Allow unrestricted battery", contentColor = Neo.primary, onClick = {
                val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsNotif) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching { context.startActivity(BatteryOptimization.requestIntent(context)) }
                }
            })
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "On some phones (Xiaomi, Oppo, Vivo, Samsung…) you may also need to turn on " +
                "\"Autostart\" for Photos in your system settings.",
            style = MaterialTheme.typography.bodySmall, color = Neo.muted,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = Neo.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 10.dp))
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    NeoSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    Spacer(
        Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp)
            .background(Neo.dark.copy(alpha = 0.25f)),
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Neo.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Neo.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Neo.onPrimary,
                checkedTrackColor = Neo.primary,
                uncheckedThumbColor = Neo.muted,
                uncheckedTrackColor = Neo.bg,
                uncheckedBorderColor = Neo.dark,
            ),
        )
    }
}
