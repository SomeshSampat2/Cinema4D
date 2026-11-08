package com.cinema4d.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.ErrorView
import com.cinema4d.app.ui.components.LoadingView
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.LiquidButton
import com.cinema4d.app.ui.components.TicketQrCode
import com.cinema4d.app.ui.theme.Gold500
import com.cinema4d.app.ui.theme.SuccessGreen
import com.cinema4d.app.utils.FormatUtils
import com.cinema4d.app.viewmodel.TicketViewModel
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer

@Composable
fun TicketScreen(
    showId: String,
    seatIds: List<String>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: TicketViewModel = viewModel(
        key = "ticket_${showId}_${seatIds.joinToString("_")}",
        factory = viewModelFactory {
            initializer { TicketViewModel(AppContainer.cinemaRepository, showId, seatIds) }
        }
    )
    val state by vm.state.collectAsState()

    AmbientBackdrop(modifier = modifier) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            state.isLoading -> LoadingView()
            state.error != null -> ErrorView(state.error ?: "Booking failed")
            else -> {
                val show = state.show
                val movie = state.movie
                val theatre = state.theatre
                if (show == null || movie == null) {
                    ErrorView("Booking details unavailable")
                } else {
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
                    Spacer(Modifier.height(8.dp))
                    Text("Booking Confirmed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Booking ref ${state.bookingRef}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTextSecondary,
                    )
                    Spacer(Modifier.height(20.dp))

                    // e-ticket card
                    GlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(movie.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${show.dateLabel} · ${show.time}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GlassTextSecondary,
                                    )
                                    Text(
                                        "${theatre?.name ?: ""} · ${show.screenName} · ${AppContainer.cinemaRepository.formatById(show.formatId).label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassTextSecondary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            state.seats.forEach { seat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Seat ${seat.seatLabel}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        FormatUtils.inr(state.seatPrices[seat.id] ?: 0),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GlassTextSecondary,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent)
                                        )
                                    ),
                            )
                            SummaryRow("Tickets", FormatUtils.inr(state.ticketTotal))
                            SummaryRow("Convenience fee", FormatUtils.inr(state.convenienceFee))
                            SummaryRow("Total", FormatUtils.inr(state.grandTotal), emphasize = true)
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                            ) {
                                TicketQrCode(
                                    content = "CINEMA4D|${state.bookingRef}|${state.seats.joinToString(",") { it.id }}|${show.id}",
                                )
                            }
                            Text(
                                "Show this QR at the theatre entrance",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlassTextSecondary,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    LiquidButton(
                        text = "Done",
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasize) Gold500 else MaterialTheme.colorScheme.onSurface,
        )
    }
}
