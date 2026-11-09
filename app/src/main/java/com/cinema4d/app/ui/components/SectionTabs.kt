package com.cinema4d.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Floating glass switcher between the app's booking sections. */
@Composable
fun SectionTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labels: List<String> = listOf("Movies", "Trains"),
) {
    GlassSurface(shape = CircleShape, modifier = modifier) {
        Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(
                            if (active) Modifier.background(Color.White.copy(alpha = 0.92f))
                            else Modifier
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) Color(0xFF14151A) else GlassTextPrimary,
                    )
                }
            }
        }
    }
}
