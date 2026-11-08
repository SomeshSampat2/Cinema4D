package com.cinema4d.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cinema4d.app.ui.navigation.CinemaNavigator
import com.cinema4d.app.ui.navigation.CinemaRoute
import com.cinema4d.app.ui.screens.MovieDetailScreen
import com.cinema4d.app.ui.screens.MoviesScreen
import com.cinema4d.app.ui.screens.SeatSelectionScreen
import com.cinema4d.app.ui.screens.TicketScreen
import com.cinema4d.app.ui.theme.Cinema4DTheme

@Composable
fun CinemaApp(modifier: Modifier = Modifier) {
    var isLight by rememberSaveable { mutableStateOf(false) }
    Cinema4DTheme(darkTheme = !isLight) {
    val navigator = remember { CinemaNavigator() }
    val backStack by navigator.backStack.collectAsState()
    val route = backStack.lastOrNull() ?: CinemaRoute.Movies

    BackHandler(enabled = backStack.size > 1) {
        navigator.pop()
    }

    // Material's text defaults follow LocalContentColor; without this the whole
    // app renders black text on the dark background.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (val r = route) {
            is CinemaRoute.Movies -> MoviesScreen(
                onMovieClick = { navigator.push(CinemaRoute.MovieDetail(it)) },
                isLight = isLight,
                onLightChange = { isLight = it },
            )
            is CinemaRoute.MovieDetail -> MovieDetailScreen(
                movieId = r.movieId,
                onBack = { navigator.pop() },
                onShowSelected = { navigator.push(CinemaRoute.Seats(it)) },
            )
            is CinemaRoute.Seats -> SeatSelectionScreen(
                showId = r.showId,
                onBack = { navigator.pop() },
                onProceed = { seats -> navigator.push(CinemaRoute.Ticket(r.showId, seats)) },
            )
            is CinemaRoute.Ticket -> TicketScreen(
                showId = r.showId,
                seatIds = r.seatIds,
                onDone = { navigator.popToRoot() },
            )
        }
    }
    }
    }
}
