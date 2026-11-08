package com.cinema4d.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.data.repository.CinemaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoviesUiState(
    val movies: List<Movie> = emptyList(),
    val featured: Movie? = null,
    val query: String = "",
)

class MoviesViewModel(
    private val repository: CinemaRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<MoviesUiState> = query
        .map { q ->
            val filtered = repository.movies.filter {
                q.isBlank() || it.title.contains(q, ignoreCase = true) ||
                    it.genres.any { g -> g.contains(q, ignoreCase = true) }
            }
            MoviesUiState(
                movies = filtered,
                featured = repository.movies.firstOrNull { it.isFeatured },
                query = q,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MoviesUiState())

    fun onQueryChange(value: String) {
        viewModelScope.launch { query.value = value }
    }
}
