package com.cinema4d.app.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinema4d.app.data.model.Train
import com.cinema4d.app.data.model.TrainClass
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.GlassChip
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import com.cinema4d.app.ui.components.GradientTitle
import com.cinema4d.app.ui.theme.AccentBright
import com.cinema4d.app.ui.theme.Gold500
import com.cinema4d.app.ui.theme.SuccessGreen
import com.cinema4d.app.utils.FormatUtils

@Composable
fun TrainsScreen(
    onSelectClass: (trainId: String, classId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo = AppContainer.trainRepository
    var dateIndex by remember { mutableStateOf(0) }
    val trains = remember { repo.trains }

    AmbientBackdrop(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column {
                    GradientTitle("Trains", MaterialTheme.typography.headlineMedium)
                    Text(
                        "Pick a seat you can actually see",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                    )
                }
            }
            item {
                GlassSurface(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("SBC", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
                            Text("KSR Bengaluru", style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", style = MaterialTheme.typography.titleMedium, color = AccentBright)
                            Text("Direct", style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("MAS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
                            Text("Chennai Central", style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(repo.dates) { date ->
                        GlassChip(
                            text = date,
                            selected = repo.dates[dateIndex] == date,
                            onClick = { dateIndex = repo.dates.indexOf(date) },
                        )
                    }
                }
            }
            items(trains, key = { it.id }) { train ->
                TrainCard(train = train, onSelectClass = onSelectClass)
            }
        }
    }
}

@Composable
private fun TrainCard(
    train: Train,
    onSelectClass: (String, String) -> Unit,
) {
    GlassSurface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${train.number} · ${train.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                    )
                    Text(
                        train.runsOn,
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.Star, null, tint = Gold500, modifier = Modifier.size(12.dp))
                    Text(
                        " ${train.rating}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextPrimary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(train.departure, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
                    Text(train.fromCode, style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                }
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(train.durationLabel, style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                    Box(
                        Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(0.7f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Text("0 stops", style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(train.arrival, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GlassTextPrimary)
                        if (train.arrivalDay.isNotEmpty()) {
                            Text(
                                train.arrivalDay,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentBright,
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                    Text(train.toCode, style = MaterialTheme.typography.labelSmall, color = GlassTextSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                train.classes.forEach { cls ->
                    ClassPill(cls = cls, onClick = { onSelectClass(train.id, cls.id) })
                }
            }
        }
    }
}

@Composable
private fun ClassPill(cls: TrainClass, onClick: () -> Unit) {
    val available = cls.status.startsWith("AVAILABLE")
    GlassSurface(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "${cls.code} · ${FormatUtils.inr(cls.price)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = GlassTextPrimary,
            )
            Text(
                cls.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (available) SuccessGreen else AccentBright,
            )
        }
    }
}
