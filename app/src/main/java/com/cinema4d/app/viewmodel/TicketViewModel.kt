package com.cinema4d.app.viewmodel

import androidx.lifecycle.ViewModel
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.Show
import com.cinema4d.app.data.model.Theatre
import com.cinema4d.app.data.repository.CinemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

data class TicketUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val show: Show? = null,
    val movie: Movie? = null,
    val theatre: Theatre? = null,
    val seats: List<SeatInfo> = emptyList(),
    val seatPrices: Map<String, Int> = emptyMap(),
    val ticketTotal: Int = 0,
    val convenienceFee: Int = 0,
    val grandTotal: Int = 0,
    val bookingRef: String = "",
)

class TicketViewModel(
    private val repository: CinemaRepository,
    private val showId: String,
    private val seatIds: List<String>,
) : ViewModel() {

    private val _state = MutableStateFlow(TicketUiState())
    val state: StateFlow<TicketUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val show = repository.showById(showId)
            val movie = show?.let { repository.movieById(it.movieId) }
            val theatre = show?.let { repository.theatreById(it.theatreId) }
            val seatMap = repository.seatMap()
            if (show == null || movie == null) {
                _state.update { it.copy(isLoading = false, error = "Booking not found") }
                return@launch
            }
            val wanted = seatIds.toSet()
            val seats = seatMap.seats.filter { it.id in wanted }
            val prices = seats.associate { it.id to repository.priceFor(show, it.tier) }
            val ticketTotal = prices.values.sum()
            val fee = if (seats.isEmpty()) 0 else 30
            val ref = "C4D-" + (show.occupiedSeed * 31 + seatIds.hashCode()).toUInt()
                .toString(36).uppercase().padStart(6, '0').take(6)
            _state.update {
                it.copy(
                    isLoading = false,
                    show = show,
                    movie = movie,
                    theatre = theatre,
                    seats = seats,
                    seatPrices = prices,
                    ticketTotal = ticketTotal,
                    convenienceFee = fee,
                    grandTotal = ticketTotal + fee,
                    bookingRef = ref,
                )
            }
        }
    }
}
