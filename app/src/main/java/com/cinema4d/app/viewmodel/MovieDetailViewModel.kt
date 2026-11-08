package com.cinema4d.app.viewmodel

import androidx.lifecycle.ViewModel
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.data.model.TheatreShowGroup
import com.cinema4d.app.data.repository.CinemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val groups: List<TheatreShowGroup> = emptyList(),
    val error: String? = null,
)

class MovieDetailViewModel(
    private val repository: CinemaRepository,
    private val movieId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(MovieDetailUiState())
    val state: StateFlow<MovieDetailUiState> = _state.asStateFlow()

    init {
        val movie = repository.movieById(movieId)
        if (movie == null) {
            _state.update { it.copy(isLoading = false, error = "Movie not found") }
        } else {
            _state.update {
                it.copy(
                    isLoading = false,
                    movie = movie,
                    groups = repository.groupsForMovie(movieId),
                )
            }
        }
    }
}
