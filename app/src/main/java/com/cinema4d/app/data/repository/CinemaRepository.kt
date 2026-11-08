package com.cinema4d.app.data.repository

import android.content.Context
import com.cinema4d.app.data.model.Movie
import com.cinema4d.app.data.model.Show
import com.cinema4d.app.data.model.ShowFormat
import com.cinema4d.app.data.model.Theatre
import com.cinema4d.app.data.model.TheatreShowGroup

class CinemaRepository(
    context: Context,
    private val seatMapRepository: SeatMapRepository,
) {
    private val appContext = context.applicationContext

    val movies: List<Movie> = listOf(
        Movie(
            id = "avengers_doomsday",
            title = "Avengers: Doomsday",
            posterAsset = "posters/avengers_doomsday.jpg",
            screenArtAsset = "posters/screen_avengers_doomsday.jpg",
            trailerAsset = "videos/avengers_doomsday.mp4",
            rating = 8.9f,
            ratingCount = 214_800,
            durationMin = 166,
            genres = listOf("Action", "Sci-Fi"),
            languages = listOf("English", "Hindi", "Tamil", "Telugu"),
            certificate = "UA",
            tagline = "Whatever it takes.",
            synopsis = "The Multiverse fractures as old heroes return and new alliances are " +
                "forged. When Doctor Doom seizes control of the shattered timelines, the " +
                "Avengers must assemble one last time to stop him from rewriting reality itself.",
            director = "Anthony Russo, Joe Russo",
            cast = listOf("Robert Downey Jr.", "Chris Hemsworth", "Pedro Pascal", "Vanessa Kirby"),
            isFeatured = true,
        ),
        Movie(
            id = "interstellar",
            title = "Interstellar",
            posterAsset = "posters/interstellar.jpg",
            screenArtAsset = "posters/screen_interstellar.jpg",
            trailerAsset = "videos/interstellar.mp4",
            rating = 8.7f,
            ratingCount = 198_600,
            durationMin = 169,
            genres = listOf("Sci-Fi", "Drama"),
            languages = listOf("English", "Hindi"),
            certificate = "UA",
            tagline = "Mankind was born on Earth. It was never meant to die here.",
            synopsis = "With Earth's crops failing, a former NASA pilot leads a desperate " +
                "mission through a wormhole near Saturn in search of a new home for humanity. " +
                "What he finds on the other side will cost him everything he left behind.",
            director = "Christopher Nolan",
            cast = listOf("Matthew McConaughey", "Anne Hathaway", "Jessica Chastain", "Michael Caine"),
        ),
        Movie(
            id = "oppenheimer",
            title = "Oppenheimer",
            posterAsset = "posters/oppenheimer.jpg",
            screenArtAsset = "posters/screen_oppenheimer.jpg",
            trailerAsset = "videos/oppenheimer.mp4",
            rating = 8.6f,
            ratingCount = 176_300,
            durationMin = 180,
            genres = listOf("Drama", "History"),
            languages = listOf("English", "Hindi"),
            certificate = "A",
            tagline = "The world forever changes.",
            synopsis = "The story of J. Robert Oppenheimer and his role in the development of " +
                "the atomic bomb. A brilliant physicist is forced to weigh the cost of ending " +
                "a war against the world he unleashes in doing so.",
            director = "Christopher Nolan",
            cast = listOf("Cillian Murphy", "Emily Blunt", "Robert Downey Jr.", "Matt Damon"),
        ),
        Movie(
            id = "dune_part_two",
            title = "Dune: Part Two",
            posterAsset = "posters/dune_part_two.jpg",
            screenArtAsset = "posters/screen_dune_part_two.jpg",
            trailerAsset = "videos/dune_part_two.mp4",
            rating = 8.5f,
            ratingCount = 152_900,
            durationMin = 166,
            genres = listOf("Sci-Fi", "Adventure"),
            languages = listOf("English", "Hindi", "Tamil"),
            certificate = "UA",
            tagline = "Long live the fighters.",
            synopsis = "Paul Atreides unites with Chani and the Fremen while seeking revenge " +
                "against the conspirators who destroyed his family. Facing a choice between " +
                "the love of his life and the fate of the universe, he must prevent a terrible future.",
            director = "Denis Villeneuve",
            cast = listOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Austin Butler"),
        ),
        Movie(
            id = "spider_verse",
            title = "Spider-Man: Across the Spider-Verse",
            posterAsset = "posters/spider_verse.jpg",
            screenArtAsset = "posters/screen_spider_verse.jpg",
            trailerAsset = "videos/spider_verse.mp4",
            rating = 8.6f,
            ratingCount = 143_700,
            durationMin = 140,
            genres = listOf("Animation", "Action"),
            languages = listOf("English", "Hindi", "Tamil", "Telugu"),
            certificate = "U",
            tagline = "It's how you wear the mask that matters.",
            synopsis = "Miles Morales catapults across the Multiverse, where he encounters a " +
                "team of Spider-People charged with protecting its very existence. When the " +
                "heroes clash over how to handle a new threat, Miles must redefine what it means to be a hero.",
            director = "Joaquim Dos Santos, Kemp Powers",
            cast = listOf("Shameik Moore", "Hailee Steinfeld", "Oscar Isaac", "Jason Schwartzman"),
        ),
        Movie(
            id = "the_batman",
            title = "The Batman",
            posterAsset = "posters/the_batman.jpg",
            screenArtAsset = "posters/screen_the_batman.jpg",
            trailerAsset = "videos/the_batman.mp4",
            rating = 7.8f,
            ratingCount = 118_400,
            durationMin = 176,
            genres = listOf("Action", "Crime"),
            languages = listOf("English", "Hindi"),
            certificate = "UA",
            tagline = "Unmask the truth.",
            synopsis = "In his second year of fighting crime, Batman uncovers corruption in " +
                "Gotham City that connects to his own family while facing a serial killer known " +
                "as the Riddler who is targeting the city's elite.",
            director = "Matt Reeves",
            cast = listOf("Robert Pattinson", "Zoë Kravitz", "Paul Dano", "Colin Farrell"),
        ),
    )

    val theatres: List<Theatre> = listOf(
        Theatre(
            id = "cinepolis_imax",
            name = "Cinepolis IMAX",
            area = "Phoenix Mall, Indiranagar",
            distanceKm = 2.4,
            avgRating = 4.6f,
            amenities = listOf("IMAX", "Dolby Atmos", "Recliners", "Food Court"),
        ),
        Theatre(
            id = "pvr_prime",
            name = "PVR Prime",
            area = "Forum Mall, Koramangala",
            distanceKm = 4.1,
            avgRating = 4.4f,
            amenities = listOf("4DX", "Dolby Atmos", "Recliners", "Valet"),
        ),
        Theatre(
            id = "mirage_screens",
            name = "Mirage Cinemas",
            area = "UB City, Viman Nagar",
            distanceKm = 6.8,
            avgRating = 4.1f,
            amenities = listOf("IMAX", "Food Court", "Parking"),
        ),
    )

    private val formats = listOf(
        ShowFormat("2d", "2D", 1.0),
        ShowFormat("imax", "IMAX", 1.35),
        ShowFormat("4dx", "4DX", 1.45),
        ShowFormat("dolby", "Dolby Atmos", 1.2),
    )

    private val times = listOf("11:00 AM", "2:30 PM", "4:15 PM", "6:45 PM", "8:00 PM", "10:30 PM")
    private val dates = listOf("Today", "Tomorrow", "Sun, 9 Nov", "Mon, 10 Nov")
    private val screens = listOf("Screen 1", "Screen 2 · Dolby", "Screen 3 · IMAX", "Screen 4 · 4DX")
    private val basePrices = mapOf(
        "prime" to 450,
        "classic" to 320,
        "recliner" to 680,
    )

    val shows: List<Show> = buildShows()

    private fun buildShows(): List<Show> {
        val out = mutableListOf<Show>()
        movies.forEachIndexed { mi, movie ->
            theatres.forEachIndexed { ti, theatre ->
                // each movie plays at each theatre on 2 of 4 dates with rotating slots
                val dateStart = (mi + ti) % dates.size
                val slotStart = (mi * 2 + ti) % times.size
                for (d in 0 until 2) {
                    val dateLabel = dates[(dateStart + d) % dates.size]
                    for (s in 0 until 3) {
                        val timeIdx = (slotStart + s * 2) % times.size
                        val format = formats[(mi + ti + s + d) % formats.size]
                        val screenIdx = (mi + ti + s) % screens.size
                        val idx = out.size
                        out += Show(
                            id = "show_${movie.id}_${theatre.id}_${d}_$s",
                            movieId = movie.id,
                            theatreId = theatre.id,
                            dateLabel = dateLabel,
                            time = times[timeIdx],
                            formatId = format.id,
                            screenName = screens[screenIdx],
                            tierPrices = basePrices,
                            occupiedSeed = 10_000 + idx * 37,
                        )
                    }
                }
            }
        }
        return out
    }

    fun movieById(id: String): Movie? = movies.firstOrNull { it.id == id }
    fun theatreById(id: String): Theatre? = theatres.firstOrNull { it.id == id }
    fun showById(id: String): Show? = shows.firstOrNull { it.id == id }
    fun formatById(id: String): ShowFormat = formats.firstOrNull { it.id == id } ?: formats.first()

    fun showsForMovie(movieId: String): List<Show> =
        shows.filter { it.movieId == movieId }.sortedWith(
            compareBy<Show> { dates.indexOf(it.dateLabel).let { i -> if (i < 0) 99 else i } }
                .thenBy { timeSortKey(it.time) }
        )

    fun groupsForMovie(movieId: String): List<TheatreShowGroup> {
        val movieShows = showsForMovie(movieId)
        return theatres.mapNotNull { theatre ->
            val ts = movieShows.filter { it.theatreId == theatre.id }
            if (ts.isEmpty()) null else TheatreShowGroup(theatre, ts)
        }
    }

    private fun timeSortKey(time: String): Int {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.replace(" ", "")?.toIntOrNull() ?: 0
        val pm = time.contains("PM", true) && h != 12
        val hh = if (time.contains("AM", true) && h == 12) 0 else if (pm) h + 12 else h
        return hh * 60 + m
    }

    fun priceFor(show: Show, tier: String): Int {
        val base = show.tierPrices[tier] ?: show.tierPrices.values.first()
        return (base * formatById(show.formatId).multiplier).toInt()
    }

    fun minPriceFor(show: Show): Int = show.tierPrices.keys.minOf { priceFor(show, it) }

    suspend fun occupiedSeats(show: Show): Set<String> {
        val doc = seatMapRepository.loadSeatMap()
        val ids = doc.seats.map { it.id }
        val count = (ids.size * OCCUPIED_FRACTION).toInt().coerceAtLeast(10)
        return ids.shuffled(kotlin.random.Random(show.occupiedSeed)).take(count).toSet()
    }

    suspend fun seatMap() = seatMapRepository.loadSeatMap()

    companion object {
        private const val OCCUPIED_FRACTION = 0.13
    }
}
