package com.cinema4d.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.data.model.Show
import com.cinema4d.app.data.repository.CinemaRepository
import com.cinema4d.app.data.repository.SeatMapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SeatViewMode { MAP, SEAT_VIEW }

data class SeatSelectionUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val show: Show? = null,
    val movie: Movie? = null,
    val seatMap: SeatMapDocument? = null,
    val occupied: Set<String> = emptySet(),
    val selected: List<String> = emptyList(),
    val previewSeatId: String? = null,
    val mode: SeatViewMode = SeatViewMode.MAP,
    val maxSeats: Int = 8,
) {
    val selectedSeats: List<SeatInfo>
        get() = seatMap?.seats?.filter { it.id in selected } ?: emptyList()

    val previewSeat: SeatInfo?
        get() = seatMap?.seats?.firstOrNull { it.id == previewSeatId }
            ?: seatMap?.seats?.firstOrNull { it.id == selected.firstOrNull() }
            ?: seatMap?.seats?.firstOrNull { it.tier == "prime" && it.row == "F" }
}

class SeatSelectionViewModel(
    private val cinemaRepository: CinemaRepository,
    private val seatMapRepository: SeatMapRepository,
    private val showId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SeatSelectionUiState())
    val state: StateFlow<SeatSelectionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val show = cinemaRepository.showById(showId)
                val movie = show?.let { cinemaRepository.movieById(it.movieId) }
                val seatMap = seatMapRepository.loadSeatMap()
                if (show == null || movie == null) {
                    _state.update { it.copy(isLoading = false, error = "Show not found") }
                    return@launch
                }
                val occupied = cinemaRepository.occupiedSeats(show)
                _state.update {
                    it.copy(
                        isLoading = false,
                        show = show,
                        movie = movie,
                        seatMap = seatMap,
                        occupied = occupied,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load seats") }
            }
        }
    }

    fun onSeatClick(seat: SeatInfo) {
        val s = _state.value
        val selected = s.selected.toMutableList()
        if (seat.id !in s.occupied) {
            if (seat.id in selected) {
                selected.remove(seat.id)
            } else if (selected.size < s.maxSeats) {
                selected.add(seat.id)
            }
        }
        _state.update {
            it.copy(
                selected = selected,
                previewSeatId = seat.id,
                mode = SeatViewMode.SEAT_VIEW,
            )
        }
    }

    fun setMode(mode: SeatViewMode) = _state.update { it.copy(mode = mode) }

    fun setPreviewSeat(seatId: String) = _state.update { it.copy(previewSeatId = seatId) }

    fun priceForSeat(seat: SeatInfo): Int {
        val show = _state.value.show ?: return 0
        return cinemaRepository.priceFor(show, seat.tier)
    }

    fun totalPrice(): Int {
        val show = _state.value.show ?: return 0
        return _state.value.selectedSeats.sumOf { cinemaRepository.priceFor(show, it.tier) }
    }

    fun convenienceFee(): Int = if (_state.value.selected.isEmpty()) 0 else 30
}
