package com.cinema4d.app.di

import android.content.Context
import com.cinema4d.app.data.repository.CinemaRepository
import com.cinema4d.app.data.repository.SeatMapRepository

object AppContainer {
    private lateinit var appContext: Context

    val seatMapRepository by lazy { SeatMapRepository(appContext) }
    val cinemaRepository by lazy { CinemaRepository(appContext, seatMapRepository) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
