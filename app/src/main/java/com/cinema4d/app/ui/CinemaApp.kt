package com.cinema4d.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
    AnimatedContent(
        targetState = route,
        modifier = Modifier.fillMaxSize(),
        label = "screen",
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(
                durationMillis = 460,
                easing = FastOutSlowInEasing,
            )
            val fadeSpec = tween<Float>(durationMillis = 320, easing = LinearOutSlowInEasing)
            val scaleSpec = tween<Float>(durationMillis = 460, easing = FastOutSlowInEasing)
            if (forward) {
                // new screen eases in from the right while the old one settles back
                (slideInHorizontally(slideSpec) { it / 4 } +
                    fadeIn(fadeSpec) +
                    scaleIn(scaleSpec, initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(200)) +
                        slideOutHorizontally(slideSpec) { -it / 12 } +
                        scaleOut(scaleSpec, targetScale = 0.98f))
            } else {
                (fadeIn(fadeSpec) +
                    slideInHorizontally(slideSpec) { -it / 12 } +
                    scaleIn(scaleSpec, initialScale = 0.98f)) togetherWith
                    (fadeOut(tween(200)) +
                        slideOutHorizontally(slideSpec) { it / 4 } +
                        scaleOut(scaleSpec, targetScale = 0.96f))
            }
        },
    ) { r ->
        Box(modifier = Modifier.fillMaxSize()) {
        when (r) {
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
}
