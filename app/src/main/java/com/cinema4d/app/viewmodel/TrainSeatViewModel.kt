package com.cinema4d.app.viewmodel

import androidx.lifecycle.ViewModel
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.data.model.Train
import com.cinema4d.app.data.model.TrainClass
import com.cinema4d.app.data.repository.TrainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TrainSeatUiState(
    val train: Train? = null,
    val travelClass: TrainClass? = null,
    val seatMap: SeatMapDocument? = null,
    val occupied: Set<String> = emptySet(),
    val selected: List<String> = emptyList(),
    val previewSeatId: String? = null,
    val mode: SeatViewMode = SeatViewMode.MAP,
    val maxSeats: Int = 6,
) {
    val selectedSeats: List<SeatInfo>
        get() = seatMap?.seats?.filter { it.id in selected } ?: emptyList()

    val previewSeat: SeatInfo?
        get() = seatMap?.seats?.firstOrNull { it.id == previewSeatId }
            ?: seatMap?.seats?.firstOrNull { it.id == selected.firstOrNull() }
}

class TrainSeatViewModel(
    private val repository: TrainRepository,
    private val trainId: String,
    private val classId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(TrainSeatUiState())
    val state: StateFlow<TrainSeatUiState> = _state.asStateFlow()

    init {
        val train = repository.trainById(trainId)
        val travelClass = train?.let { repository.classById(it, classId) }
        val seatMap = repository.seatMap()
        _state.value = TrainSeatUiState(
            train = train,
            travelClass = travelClass,
            seatMap = seatMap,
            occupied = repository.occupiedSeats(trainId, classId),
        )
    }

    fun onSeatClick(seat: SeatInfo) {
        val s = _state.value
        if (seat.id in s.occupied) return
        val selected = s.selected.toMutableList()
        if (seat.id in selected) selected.remove(seat.id)
        else if (selected.size < s.maxSeats) selected.add(seat.id)
        // selecting a berth opens its view, the way the cinema flow does
        _state.update { it.copy(selected = selected, previewSeatId = seat.id, mode = SeatViewMode.SEAT_VIEW) }
    }

    fun setMode(mode: SeatViewMode) = _state.update { it.copy(mode = mode) }

    fun totalPrice(): Int {
        val s = _state.value
        val train = s.train ?: return 0
        return s.selectedSeats.sumOf { repository.priceFor(train, classId, it) }
    }
}
