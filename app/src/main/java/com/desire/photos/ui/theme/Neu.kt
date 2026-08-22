package com.desire.photos.ui.theme

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One full neumorphic color set. Swapping [Neo.current] re-themes the whole app. */
data class NeoPalette(
    val id: String,
    val displayName: String,
    val bg: Color,
    val surface: Color,
    val light: Color,
    val dark: Color,
    val primary: Color,
    val onPrimary: Color,
    val text: Color,
    val muted: Color,
    val danger: Color,
    val success: Color,
    val isDark: Boolean = false,
)

/** The curated theme choices shown in Settings. */
object NeoPalettes {
    val Classic = NeoPalette(
        id = "classic", displayName = "Classic",
        bg = Color(0xFFE9EDF3), surface = Color(0xFFE9EDF3),
        light = Color(0xFFFFFFFF), dark = Color(0xFFBBC6D6),
        primary = Color(0xFF5B7CE2), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF37465C), muted = Color(0xFF8895A8),
        danger = Color(0xFFE1665A), success = Color(0xFF4CAF7D),
    )
    val Midnight = NeoPalette(
        id = "midnight", displayName = "Midnight",
        bg = Color(0xFF20232B), surface = Color(0xFF20232B),
        light = Color(0xFF2B2F3A), dark = Color(0xFF121419),
        primary = Color(0xFF7C9CFF), onPrimary = Color(0xFF10131A),
        text = Color(0xFFEAECF2), muted = Color(0xFF8C93A6),
        danger = Color(0xFFFF6F6F), success = Color(0xFF5FD498),
        isDark = true,
    )
    val Sunset = NeoPalette(
        id = "sunset", displayName = "Sunset",
        bg = Color(0xFFF5E9E2), surface = Color(0xFFF5E9E2),
        light = Color(0xFFFFFDFB), dark = Color(0xFFDEC3B2),
        primary = Color(0xFFE8834E), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF4A362C), muted = Color(0xFF9C8478),
        danger = Color(0xFFD84C4C), success = Color(0xFF4E9A6E),
    )
    val Forest = NeoPalette(
        id = "forest", displayName = "Forest",
        bg = Color(0xFFE7EFE7), surface = Color(0xFFE7EFE7),
        light = Color(0xFFFBFFFB), dark = Color(0xFFBFD3BF),
        primary = Color(0xFF3F8F5F), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF2E3D33), muted = Color(0xFF7C8F80),
        danger = Color(0xFFD1594F), success = Color(0xFF3F8F5F),
    )
    val Rose = NeoPalette(
        id = "rose", displayName = "Rose",
        bg = Color(0xFFF7E8EF), surface = Color(0xFFF7E8EF),
        light = Color(0xFFFFFBFD), dark = Color(0xFFE2C0D0),
        primary = Color(0xFFC85B8E), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF4A2E3B), muted = Color(0xFF9C7C8B),
        danger = Color(0xFFD1594F), success = Color(0xFF4E9A6E),
    )
    val Ocean = NeoPalette(
        id = "ocean", displayName = "Ocean",
        bg = Color(0xFFE3F0F3), surface = Color(0xFFE3F0F3),
        light = Color(0xFFFAFFFF), dark = Color(0xFFB9D4DA),
        primary = Color(0xFF1D8FA6), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF243B40), muted = Color(0xFF7C9599),
        danger = Color(0xFFD1594F), success = Color(0xFF3F8F5F),
    )
    val Lavender = NeoPalette(
        id = "lavender", displayName = "Lavender",
        bg = Color(0xFFEDE9F7), surface = Color(0xFFEDE9F7),
        light = Color(0xFFFCFAFF), dark = Color(0xFFCFC4E8),
        primary = Color(0xFF7A5FC7), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF39304F), muted = Color(0xFF8E84A8),
        danger = Color(0xFFD1594F), success = Color(0xFF4E9A6E),
    )
    val Amber = NeoPalette(
        id = "amber", displayName = "Amber",
        bg = Color(0xFFF7F0E1), surface = Color(0xFFF7F0E1),
        light = Color(0xFFFFFCF5), dark = Color(0xFFE3D3AC),
        primary = Color(0xFFC98A20), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF4A3E25), muted = Color(0xFF9C8C68),
        danger = Color(0xFFD1594F), success = Color(0xFF4E9A6E),
    )
    val Slate = NeoPalette(
        id = "slate", displayName = "Slate",
        bg = Color(0xFF1B1F24), surface = Color(0xFF1B1F24),
        light = Color(0xFF262B32), dark = Color(0xFF0F1216),
        primary = Color(0xFF6FD6C7), onPrimary = Color(0xFF0C1A17),
        text = Color(0xFFE8ECEF), muted = Color(0xFF8A94A0),
        danger = Color(0xFFFF7A6F), success = Color(0xFF6FD6A5),
        isDark = true,
    )
    val Mint = NeoPalette(
        id = "mint", displayName = "Mint",
        bg = Color(0xFFE4F3EC), surface = Color(0xFFE4F3EC),
        light = Color(0xFFFAFFFC), dark = Color(0xFFBEDDCC),
        primary = Color(0xFF2FA37A), onPrimary = Color(0xFFFFFFFF),
        text = Color(0xFF243D33), muted = Color(0xFF7C9C8C),
        danger = Color(0xFFD1594F), success = Color(0xFF2FA37A),
    )

    val all = listOf(Classic, Midnight, Sunset, Forest, Rose, Ocean, Lavender, Amber, Slate, Mint)
    fun byId(id: String?): NeoPalette = all.firstOrNull { it.id == id } ?: Classic
}

/**
 * White-neumorphism theme, swappable at runtime. Every screen reads colors as
 * `Neo.primary`, `Neo.bg`, etc. — those now delegate to [current], so changing
 * it (from Settings) re-themes the whole app without touching any screen.
 */
object Neo {
    var current: NeoPalette by mutableStateOf(NeoPalettes.Classic)

    val bg: Color get() = current.bg
    val surface: Color get() = current.surface
    val light: Color get() = current.light
    val dark: Color get() = current.dark
    val primary: Color get() = current.primary
    val onPrimary: Color get() = current.onPrimary
    val text: Color get() = current.text
    val muted: Color get() = current.muted
    val danger: Color get() = current.danger
    val success: Color get() = current.success
}

/**
 * Draws a soft "raised" neumorphic surface: a light shadow up-left and a dark
 * shadow down-right. The shape is drawn inset by [elevation] so the blur stays
 * inside the node's bounds (add the same spacing around content).
 */
fun Modifier.neuRaised(
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 7.dp,
    background: Color = Neo.surface,
): Modifier = this.drawBehind {
    val s = elevation.toPx()
    val corner = cornerRadius.toPx()
    drawIntoCanvas { canvas ->
        val paint = NativePaint().apply {
            isAntiAlias = true
            color = background.toArgb()
        }
        paint.setShadowLayer(s, s * 0.55f, s * 0.55f, Neo.dark.toArgb())
        canvas.nativeCanvas.drawRoundRect(s, s, size.width - s, size.height - s, corner, corner, paint)
        paint.setShadowLayer(s, -s * 0.55f, -s * 0.55f, Neo.light.toArgb())
        canvas.nativeCanvas.drawRoundRect(s, s, size.width - s, size.height - s, corner, corner, paint)
        paint.clearShadowLayer()
    }
}

/** A raised neumorphic panel. Content padding already accounts for the shadow gap. */
@Composable
fun NeoSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 7.dp,
    background: Color = Neo.surface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    var base = modifier.neuRaised(cornerRadius, elevation, background)
    if (onClick != null) {
        base = base.clip(shape).clickable(onClick = onClick)
    }
    Box(
        modifier = base.padding(elevation).padding(contentPadding),
        content = content,
    )
}

/** Neumorphic pill button with an optional leading icon. */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = Neo.primary,
) {
    NeoSurface(
        modifier = modifier,
        cornerRadius = 18.dp,
        elevation = 6.dp,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text, color = contentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Round neumorphic icon button. */
@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Neo.text,
    size: Dp = 46.dp,
) {
    NeoSurface(
        modifier = modifier.size(size),
        cornerRadius = size / 2,
        elevation = 6.dp,
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.align(Alignment.Center).size(size * 0.42f),
        )
    }
}

/** Selectable neumorphic chip (raised = selected, subtle = not). */
@Composable
fun NeoChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .then(if (selected) Modifier.neuRaised(14.dp, 5.dp) else Modifier)
            .clip(shape)
            .clickable(interaction, LocalIndication.current, onClick = onClick)
            .padding(if (selected) 5.dp else 0.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (selected) Neo.primary else Neo.muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
