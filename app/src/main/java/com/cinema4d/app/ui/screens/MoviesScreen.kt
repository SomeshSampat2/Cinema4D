package com.cinema4d.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.FeaturedMovieHero
import com.cinema4d.app.ui.components.GlassSearchField
import com.cinema4d.app.ui.components.GradientTitle
import com.cinema4d.app.ui.components.LoadingView
import com.cinema4d.app.ui.components.MovieCard
import com.cinema4d.app.ui.navigation.CinemaRoute
import com.cinema4d.app.utils.FormatUtils
import com.cinema4d.app.viewmodel.MoviesViewModel
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer

@Composable
fun MoviesScreen(
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
    onLightChange: (Boolean) -> Unit = {},
) {
    val vm: MoviesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { MoviesViewModel(AppContainer.cinemaRepository) }
        }
    )
    val state by vm.state.collectAsState()

    AmbientBackdrop(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GradientTitle(
                        text = "CINEMA4D",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier,
                    )
                    Text(
                        "Now showing near you",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                    )
                }

            }
            Spacer(Modifier.height(10.dp))
            GlassSearchField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                placeholder = "Search movies, genres…",
            )
            Spacer(Modifier.height(10.dp))

            state.featured?.let { featured ->
                FeaturedMovieHero(
                    movie = featured,
                    onBook = { onMovieClick(featured.id) },
                )
                Spacer(Modifier.height(22.dp))
            }

            if (state.movies.isEmpty()) {
                LoadingView(label = "No movies match your search")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 112.dp),
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
                    }
                }
            }
        }
    }
}
