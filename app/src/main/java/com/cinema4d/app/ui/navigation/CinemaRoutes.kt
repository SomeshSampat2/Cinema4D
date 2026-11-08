package com.cinema4d.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface CinemaRoute {
    /** How deep the screen sits in the flow — drives the transition direction. */
    val depth: Int

    data object Movies : CinemaRoute {
        override val depth = 0
    }

    data class MovieDetail(val movieId: String) : CinemaRoute {
        override val depth = 1
    }

    data class Seats(val showId: String) : CinemaRoute {
        override val depth = 2
    }

    data class Ticket(val showId: String, val seatIds: List<String>) : CinemaRoute {
        override val depth = 3
    }
}

class CinemaNavigator(start: CinemaRoute = CinemaRoute.Movies) {
    private val _backStack = MutableStateFlow(listOf(start))
    val backStack: StateFlow<List<CinemaRoute>> = _backStack.asStateFlow()

    val current: CinemaRoute get() = _backStack.value.last()

    fun push(route: CinemaRoute) {
        _backStack.update { it + route }
    }

    fun pop() {
        _backStack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    fun popToRoot() {
        _backStack.update { listOf(CinemaRoute.Movies) }
    }
}
