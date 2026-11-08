package com.cinema4d.app.data.repository

import android.content.Context
import com.cinema4d.app.data.model.SeatMapDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SeatMapRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val mutex = Mutex()
    private var cached: SeatMapDocument? = null

    suspend fun loadSeatMap(): SeatMapDocument = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            context.assets.open(SEAT_MAP_ASSET).bufferedReader().use { reader ->
                json.decodeFromString(SeatMapDocument.serializer(), reader.readText())
            }
        }.also { cached = it }
    }

    fun hallModelAsset(): String = HALL_MODEL_ASSET

    companion object {
        const val SEAT_MAP_ASSET = "seat_map.json"
        const val HALL_MODEL_ASSET = "models/hall.glb"
    }
}
