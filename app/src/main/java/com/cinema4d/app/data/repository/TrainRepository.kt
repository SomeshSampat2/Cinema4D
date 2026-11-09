package com.cinema4d.app.data.repository

import android.content.Context
import com.cinema4d.app.data.model.SeatInfo
import com.cinema4d.app.data.model.SeatMapDocument
import com.cinema4d.app.data.model.Train
import com.cinema4d.app.data.model.TrainClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class TrainRepository(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    val trains: List<Train> = listOf(
        Train(
            id = "karnataka_exp",
            number = "12627",
            name = "Karnataka Express",
            fromCode = "SBC", fromName = "KSR Bengaluru",
            toCode = "NDLS", toName = "New Delhi",
            departure = "06:20", arrival = "05:10", arrivalDay = "+1",
            durationLabel = "22h 50m",
            runsOn = "Runs daily",
            rating = 4.3f,
            classes = listOf(
                TrainClass("sl", "SL", "Sleeper", 755, "AVAILABLE 46"),
                TrainClass("3a", "3A", "AC 3 Tier", 1_990, "AVAILABLE 22"),
                TrainClass("2a", "2A", "AC 2 Tier", 2_845, "RAC 4"),
            ),
        ),
        Train(
            id = "tamil_nadu_exp",
            number = "12622",
            name = "Tamil Nadu Express",
            fromCode = "MAS", fromName = "Chennai Central",
            toCode = "NDLS", toName = "New Delhi",
            departure = "22:30", arrival = "07:15", arrivalDay = "+2",
            durationLabel = "32h 45m",
            runsOn = "Runs daily",
            rating = 4.1f,
            classes = listOf(
                TrainClass("sl", "SL", "Sleeper", 820, "AVAILABLE 31"),
                TrainClass("3a", "3A", "AC 3 Tier", 2_175, "AVAILABLE 12"),
                TrainClass("2a", "2A", "AC 2 Tier", 3_110, "AVAILABLE 6"),
            ),
        ),
        Train(
            id = "hampi_exp",
            number = "16591",
            name = "Hampi Express",
            fromCode = "SBC", fromName = "KSR Bengaluru",
            toCode = "HPT", toName = "Hosapete",
            departure = "22:40", arrival = "08:35", arrivalDay = "+1",
            durationLabel = "9h 55m",
            runsOn = "Runs daily",
            rating = 4.2f,
            classes = listOf(
                TrainClass("sl", "SL", "Sleeper", 445, "AVAILABLE 58"),
                TrainClass("3a", "3A", "AC 3 Tier", 1_120, "AVAILABLE 27"),
                TrainClass("2a", "2A", "AC 2 Tier", 1_565, "AVAILABLE 9"),
            ),
        ),
        Train(
            id = "rajdhani",
            number = "22221",
            name = "Rajdhani Express",
            fromCode = "CSMT", fromName = "Mumbai CSMT",
            toCode = "NZM", toName = "Hazrat Nizamuddin",
            departure = "16:00", arrival = "09:55", arrivalDay = "+1",
            durationLabel = "17h 55m",
            runsOn = "Runs daily",
            rating = 4.6f,
            classes = listOf(
                TrainClass("3a", "3A", "AC 3 Tier", 2_585, "AVAILABLE 14"),
                TrainClass("2a", "2A", "AC 2 Tier", 3_690, "AVAILABLE 8"),
                TrainClass("1a", "1A", "AC First Class", 6_120, "AVAILABLE 2"),
            ),
        ),
    )

    val dates: List<String> = listOf("Today", "Tomorrow", "Wed, 11 Nov", "Thu, 12 Nov")

    fun trainById(id: String): Train? = trains.firstOrNull { it.id == id }

    fun classById(train: Train, classId: String): TrainClass? =
        train.classes.firstOrNull { it.id == classId }

    fun seatMap(): SeatMapDocument = runCatching {
        val raw = appContext.assets.open(TRAIN_SEAT_MAP_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<SeatMapDocument>(raw)
    }.getOrElse { SeatMapDocument() }

    fun occupiedSeats(trainId: String, classId: String): Set<String> {
        val ids = seatMap().seats.map { it.id }
        val seed = (trainId + classId).hashCode()
        val count = (ids.size * OCCUPIED_FRACTION).toInt().coerceAtLeast(6)
        return ids.shuffled(kotlin.random.Random(seed)).take(count).toSet()
    }

    fun priceFor(train: Train, classId: String, seat: SeatInfo): Int {
        val base = classById(train, classId)?.price ?: 0
        // lower berths carry a small premium in the concept
        return if (seat.tier == "lower") base + 60 else base
    }

    companion object {
        const val TRAIN_SEAT_MAP_ASSET = "train_seat_map.json"
        const val COACH_MODEL_ASSET = "models/coach.glb"
        const val WINDOW_VIDEO_ASSET = "videos/train_window.mp4"
        private const val OCCUPIED_FRACTION = 0.18f
    }
}
