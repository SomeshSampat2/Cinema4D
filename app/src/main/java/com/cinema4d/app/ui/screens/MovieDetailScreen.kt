package com.cinema4d.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.cinema4d.app.data.model.Show
import com.cinema4d.app.data.model.TheatreShowGroup
import com.cinema4d.app.di.AppContainer
import com.cinema4d.app.ui.components.AmbientBackdrop
import com.cinema4d.app.ui.components.ErrorView
import com.cinema4d.app.ui.components.GlassChip
import com.cinema4d.app.ui.components.GlassIconButton
import com.cinema4d.app.ui.components.GlassSurface
import com.cinema4d.app.ui.components.LoadingView
import com.cinema4d.app.ui.theme.Gold500
import com.cinema4d.app.ui.theme.AccentBright
import com.cinema4d.app.utils.FormatUtils
import com.cinema4d.app.viewmodel.MovieDetailViewModel
import com.cinema4d.app.ui.components.GlassTextPrimary
import com.cinema4d.app.ui.components.GlassTextSecondary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer

@Composable
fun MovieDetailScreen(
    movieId: String,
    onBack: () -> Unit,
    onShowSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: MovieDetailViewModel = viewModel(
        key = "detail_$movieId",
        factory = viewModelFactory {
            initializer { MovieDetailViewModel(AppContainer.cinemaRepository, movieId) }
        }
    )
    val state by vm.state.collectAsState()

    AmbientBackdrop(modifier = modifier) {
        val movie = state.movie
        when {
            state.isLoading -> LoadingView()
            state.error != null -> ErrorView(state.error ?: "Error")
            movie != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 36.dp),
                ) {
                    item {
                        // Poster hero with floating glass back button and scrim
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(430.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/${movie.posterAsset}",
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = 0.35f),
                                            0.45f to Color.Transparent,
                                            0.75f to Color(0xFF0A0812).copy(alpha = 0.75f),
                                            1f to Color(0xFF0A0812),
                                        )
                                    ),
                            )
                            GlassIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(start = 18.dp, top = 8.dp)
                                    .align(Alignment.TopStart),
                            )
                            // trailer affordance
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                            ) {
                                Text(
                                    movie.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star,
                                        null,
                                        tint = Gold500,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        " ${movie.rating} · ${FormatUtils.ratingCount(movie.ratingCount)} ratings",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.72f),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassChip(movie.certificate)
                                GlassChip(FormatUtils.duration(movie.durationMin))
                                movie.genres.take(2).forEach { GlassChip(it) }
                            }
                            Spacer(Modifier.height(16.dp))
                            GlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        movie.tagline,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AccentBright,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        movie.synopsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GlassTextPrimary.copy(alpha = 0.82f),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Director · ${movie.director}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GlassTextSecondary,
                                    )
                                    Text(
                                        "Cast · ${movie.cast.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GlassTextSecondary,
                                        modifier = Modifier.padding(top = 3.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Theatres & Showtimes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GlassTextPrimary,
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    state.groups.forEach { group ->
                        item(key = "theatre_${group.theatre.id}") {
                            TheatreSection(
                                group = group,
                                onShowSelected = onShowSelected,
                                modifier = Modifier.padding(horizontal = 18.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TheatreSection(
    group: TheatreShowGroup,
    onShowSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theatre = group.theatre
    val dates = group.shows.map { it.dateLabel }.distinct()
    var selectedDate by rememberSaveable(group.theatre.id) { mutableStateOf(dates.firstOrNull() ?: "") }
    val dateShows = group.shows.filter { it.dateLabel == selectedDate }

    GlassSurface(shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        theatre.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${theatre.area} · ${theatre.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassTextSecondary,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Icon(Icons.Filled.Star, null, tint = Gold500, modifier = Modifier.size(13.dp))
                    Text(
                        " ${theatre.avgRating}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                theatre.amenities.joinToString("  ·  "),
                style = MaterialTheme.typography.labelSmall,
                color = GlassTextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { date ->
                    GlassChip(
                        text = date,
                        selected = date == selectedDate,
                        onClick = { selectedDate = date },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            dateShows.forEach { show ->
                ShowTimeRow(show = show, onClick = { onShowSelected(show.id) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ShowTimeRow(show: Show, onClick: () -> Unit) {
    val repo = AppContainer.cinemaRepository
    GlassSurface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    show.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${repo.formatById(show.formatId).label} · ${show.screenName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTextSecondary,
                )
            }
            Text(
                "from ${FormatUtils.inr(repo.minPriceFor(show))}",
                style = MaterialTheme.typography.labelLarge,
                color = AccentBright,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
