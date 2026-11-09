package com.cinema4d.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import com.cinema4d.app.ui.components.LiquidButton
import com.cinema4d.app.ui.components.TicketQrCode
import com.cinema4d.app.ui.theme.AccentBright
import com.cinema4d.app.ui.theme.SuccessGreen
import com.cinema4d.app.utils.FormatUtils
import kotlin.math.abs

@Composable
fun TrainTicketScreen(
    trainId: String,
    classId: String,
    seatIds: List<String>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repo = AppContainer.trainRepository
    val train = repo.trainById(trainId)
    val travelClass = train?.let { repo.classById(it, classId) }
    val seats = repo.seatMap().seats.filter { it.id in seatIds }

    val fare = seats.sumOf { repo.priceFor(train!!, classId, it) }
    val fee = 20
    val total = fare + fee
    val pnr = remember(trainId, seatIds) {
        val seed = (trainId + classId + seatIds.joinToString()).hashCode().toLong()
        val n = abs((seed * 7919) % 9_000_000) + 1_000_000
        val first = (n / 1_000_000).coerceIn(1, 9)
        "${first}${(n / 100_000) % 10}${(n / 10_000) % 10}-${n % 1_000_000}"
    }

    AmbientBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(46.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Booking Confirmed",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GlassTextPrimary,
            )
            Text(
                "PNR $pnr",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTextSecondary,
            )
            Spacer(Modifier.height(20.dp))

            GlassSurface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        train?.let { "${it.number} · ${it.name}" } ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlassTextPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                train?.departure ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary,
                            )
                            Text(
                                train?.fromName ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                            )
                        }
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                train?.durationLabel ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                            )
                            Box(
                                Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth(0.6f)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.18f))
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                train?.arrival ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary,
                            )
                            Text(
                                train?.toName ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent)
                                )
                            ),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Coach C1 · ${travelClass?.code ?: ""} · ${travelClass?.label ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GlassTextPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    seats.forEach { seat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Seat ${seat.seatLabel} · ${seat.tier.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextPrimary,
                            )
                            Text(
                                FormatUtils.inr(repo.priceFor(train!!, classId, seat)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Fare", style = MaterialTheme.typography.bodyMedium, color = GlassTextSecondary)
                        Text(FormatUtils.inr(fare), style = MaterialTheme.typography.bodyMedium, color = GlassTextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Convenience fee", style = MaterialTheme.typography.bodyMedium, color = GlassTextSecondary)
                        Text(FormatUtils.inr(fee), style = MaterialTheme.typography.bodyMedium, color = GlassTextPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextPrimary,
                        )
                        Text(
                            FormatUtils.inr(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentBright,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .padding(12.dp),
                    ) {
                        TicketQrCode(content = "CINEMA4D|TRAIN|$pnr|$trainId|${seatIds.joinToString(",")}")
                    }
                    Text(
                        "Show this QR at the platform gate",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            LiquidButton(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}
