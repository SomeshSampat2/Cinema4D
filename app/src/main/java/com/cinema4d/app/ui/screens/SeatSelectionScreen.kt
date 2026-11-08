package com.cinema4d.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.cinema4d.app.data.model.SeatMapDocument
import kotlinx.coroutines.launch
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.repository.SeatMapRepository
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.ErrorView
import com.cinema4d.app.ui.components.LoadingView
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.GlassChip
import com.cinema4d.app.ui.components.GlassIconButton
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.LiquidButton
import com.cinema4d.app.ui.components.SeatMapCanvas
import com.cinema4d.app.ui.theme.Gold500
import com.cinema4d.app.ui.theme.SeatSelected
import com.cinema4d.app.ui.theme.AccentBright
import com.cinema4d.app.ui.theme.TierClassic
import com.cinema4d.app.ui.theme.TierPrime
import com.cinema4d.app.ui.theme.TierRecliner
import com.cinema4d.app.utils.FormatUtils
import com.cinema4d.app.viewmodel.SeatSelectionUiState
import com.cinema4d.app.viewmodel.SeatSelectionViewModel
import com.cinema4d.app.viewmodel.SeatViewMode
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SeatSelectionScreen(
    showId: String,
    onBack: () -> Unit,
    onProceed: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SeatSelectionViewModel = viewModel(
        key = "seats_$showId",
        factory = viewModelFactory {
            initializer {
                SeatSelectionViewModel(
                    AppContainer.cinemaRepository,
                    AppContainer.seatMapRepository,
                    showId,
                )
            }
        }
    )
    val state by vm.state.collectAsState()

    AmbientBackdrop(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.movie?.title ?: "Select Seats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                state.show?.let { show ->
                    Text(
                        "${show.dateLabel} · ${show.time} · ${show.screenName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassTextSecondary,
                    )
                }
            }
        }

        when {
            state.isLoading -> LoadingView(label = "Loading seat map…")
            state.error != null -> ErrorView(state.error ?: "Error", onRetry = onBack)
            else -> {
                val seatMap = state.seatMap
                if (seatMap == null) {
                    ErrorView("Seat map unavailable")
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (state.mode) {
                            SeatViewMode.MAP -> SeatMapCanvas(
                                seatMap = seatMap,
                                occupied = state.occupied,
                                selected = state.selected.toSet(),
                                previewSeatId = state.previewSeatId,
                                interactive = true,
                                onSeatClick = vm::onSeatClick,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            )
                            SeatViewMode.SEAT_VIEW -> SeatViewerWithSheet(
                                seatMap = seatMap,
                                state = state,
                                vm = vm,
                                onProceed = onProceed,
                            )
                        }
                    }

                    if (state.mode == SeatViewMode.MAP) {
                        MapModeFooter(state = state, vm = vm, onProceed = onProceed)
                    }
                }
            }
        }
    }
    }
}


@Composable
private fun SeatViewerWithSheet(
    seatMap: SeatMapDocument,
    state: SeatSelectionUiState,
    vm: SeatSelectionViewModel,
    onProceed: (List<String>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val peekPx = with(density) { 104.dp.toPx() }
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    val maxOffset = (sheetHeight - peekPx).coerceAtLeast(0f)
    val offset = remember { Animatable(0f) }

    // start collapsed once the sheet has been measured
    LaunchedEffect(maxOffset) {
        if (maxOffset > 0f && offset.value == 0f) offset.snapTo(maxOffset)
    }

    val sheetExpanded by remember(maxOffset) {
        derivedStateOf { maxOffset > 0f && offset.value < maxOffset * 0.6f }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Seat3DView(
            seatMap = seatMap,
            previewSeat = state.previewSeat,
            screenArtAsset = state.movie?.screenArtAsset,
            trailerAsset = state.movie?.trailerAsset,
            showAudioToggle = !sheetExpanded,
            modifier = Modifier.fillMaxSize(),
        )

        GlassSurface(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset { IntOffset(0, offset.value.roundToInt()) }
                .onSizeChanged { sheetHeight = it.height.toFloat() }
                .pointerInput(maxOffset) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + drag).coerceIn(0f, maxOffset))
                            }
                        },
                        onDragEnd = {
                            val target = if (offset.value > maxOffset / 2f) maxOffset else 0f
                            scope.launch {
                                offset.animateTo(
                                    target,
                                    spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                )
                            }
                        },
                    )
                },
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // grab handle — tap toggles between peeked and expanded
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 44.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .clickable {
                            scope.launch {
                                offset.animateTo(
                                    if (offset.value > maxOffset / 2f) 0f else maxOffset,
                                    spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                                )
                            }
                        },
                )
                SelectionSummary(
                    state = state,
                    vm = vm,
                    onProceed = onProceed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
                SeatMapCanvas(
                    seatMap = seatMap,
                    occupied = state.occupied,
                    selected = state.selected.toSet(),
                    previewSeatId = state.previewSeatId,
                    interactive = true,
                    onSeatClick = { seat ->
                        vm.onSeatClick(seat)
                        // picking a seat drops the sheet back down so the hall fills the screen
                        scope.launch {
                            offset.animateTo(
                                maxOffset,
                                spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SelectionSummary(
    state: SeatSelectionUiState,
    vm: SeatSelectionViewModel,
    onProceed: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassChip(text = "Seat Map", onClick = { vm.setMode(SeatViewMode.MAP) })
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (state.selected.isEmpty()) "Tap seats on the map" else "${state.selected.size} seat(s) · ${FormatUtils.inr(vm.totalPrice())}",
                style = MaterialTheme.typography.labelMedium,
                color = GlassTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.selectedSeats.joinToString("  ") { it.seatLabel },
                style = MaterialTheme.typography.labelSmall,
                color = AccentBright,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LiquidButton(
            text = "Proceed",
            enabled = state.selected.isNotEmpty(),
            onClick = { onProceed(state.selected) },
        )
    }
}

@Composable
private fun MapModeFooter(
    state: SeatSelectionUiState,
    vm: SeatSelectionViewModel,
    onProceed: (List<String>) -> Unit,
) {
    GlassSurface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            // legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(TierPrime, "Prime")
                LegendDot(TierClassic, "Classic")
                LegendDot(TierRecliner, "Recliner")
                LegendDot(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), "Occupied")
                LegendDot(SeatSelected, "Selected")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.selected.isEmpty()) "Select up to ${state.maxSeats} seats"
                        else "${state.selected.size} seat(s) · ${FormatUtils.inr(vm.totalPrice())}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.selectedSeats.joinToString("  ") { it.seatLabel }.ifEmpty { "Tap a seat to preview its view in 3D" },
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.previewSeat != null) {
                    GlassChip(
                        text = "3D View",
                        onClick = { vm.setMode(SeatViewMode.SEAT_VIEW) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                LiquidButton(
                    text = "Proceed",
                    enabled = state.selected.isNotEmpty(),
                    onClick = { onProceed(state.selected) },
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
