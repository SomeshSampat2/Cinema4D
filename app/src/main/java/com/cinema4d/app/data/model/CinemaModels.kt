package com.cinema4d.app.data.model

data class Movie(
    val id: String,
    val title: String,
    val posterAsset: String,
    val screenArtAsset: String,
    val trailerAsset: String,
    val rating: Float,
    val ratingCount: Int,
    val durationMin: Int,
    val genres: List<String>,
    val languages: List<String>,
    val certificate: String,
    val tagline: String,
    val synopsis: String,
    val director: String,
    val cast: List<String>,
    val isFeatured: Boolean = false,
)

data class Theatre(
    val id: String,
    val name: String,
    val area: String,
    val distanceKm: Double,
    val avgRating: Float,
    val amenities: List<String>,
)

data class ShowFormat(
    val id: String,
    val label: String,
    val multiplier: Double,
)

data class Show(
    val id: String,
    val movieId: String,
    val theatreId: String,
    val dateLabel: String,
    val time: String,
    val formatId: String,
    val screenName: String,
    val tierPrices: Map<String, Int>,
    val occupiedSeed: Int,
)

data class TheatreShowGroup(
    val theatre: Theatre,
    val shows: List<Show>,
)
