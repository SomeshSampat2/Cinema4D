package com.cinema4d.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinema4d.app.ui.theme.Accent
import com.cinema4d.app.ui.theme.AccentBright
import com.cinema4d.app.ui.theme.AccentDeep
import com.cinema4d.app.ui.theme.Ink900
import com.cinema4d.app.ui.theme.OnAccent

/** Text sitting on glass stays monochrome light-on-dark, per Liquid Glass guidance. */
val GlassTextPrimary = Color.White.copy(alpha = 0.95f)
val GlassTextSecondary = Color.White.copy(alpha = 0.62f)
private val GlassTint = Color(0xFF0B0C10)

/**
 * Dark canvas with faint warm blooms. Liquid Glass has no colour of its own, so
 * the backdrop stays near-neutral and the posters supply the colour.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink900)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A2410).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(140f, 60f),
                    radius = 820f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF20182B).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(980f, 1000f),
                    radius = 1050f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2A1208).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(120f, 2000f),
                    radius = 950f,
                )
            ),
        content = content,
    )
}

/**
 * Frosted dark panel: a dimming layer (so text stays legible), a hairline light
 * rim, and a soft specular sheen across the top-left. Neutral by design — the
 * content behind provides the hue.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(GlassTint.copy(alpha = 0.62f), GlassTint.copy(alpha = 0.38f))
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
                    ),
                ),
                shape,
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(640f, 520f),
                )
            ),
        content = content,
    )
}

/** Pill chip — neutral glass when idle, white "selected segment" when active. */
@Composable
fun GlassChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val shape = CircleShape
    val base = modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .clip(shape)
    Box(
        modifier = if (selected) {
            base.background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, Color.White.copy(alpha = 0.9f), shape)
        } else {
            base.background(GlassTint.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color(0xFF14151A) else GlassTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

/**
 * Primary call to action. Colour on Liquid Glass is reserved for emphasis, so
 * this is the only strongly tinted surface in the app: warm amber, dark label.
 */
@Composable
fun LiquidButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .then(
                if (enabled) {
                    Modifier.shadow(
                        16.dp,
                        shape,
                        ambientColor = Accent.copy(alpha = 0.35f),
                        spotColor = Accent.copy(alpha = 0.55f),
                    )
                } else Modifier
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.verticalGradient(listOf(AccentBright, Accent, AccentDeep))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF23252C), Color(0xFF191B21)))
                }
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.45f else 0.08f), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        endY = 80f,
                    )
                ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) OnAccent else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 13.dp),
        )
    }
}

/** Circular glass button for back arrows and secondary actions. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(GlassTint.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GlassTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Wordmark: warm white fading into amber. */
@Composable
fun GradientTitle(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = style.copy(
            brush = Brush.linearGradient(listOf(Color.White, AccentBright, Accent)),
        ),
        modifier = modifier,
    )
}

/** Thin separator that fades at both ends, for glass cards. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent)
                )
            )
    )
}

/** Small label + value row used inside glass cards. */
@Composable
fun GlassStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasize) GlassTextPrimary else GlassTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) AccentBright else GlassTextPrimary,
        )
    }
}

/** Frosted capsule search field with an amber caret. */
@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    GlassSurface(shape = CircleShape, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = GlassTextSecondary,
                modifier = Modifier.size(19.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(10.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = GlassTextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTextSecondary,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
