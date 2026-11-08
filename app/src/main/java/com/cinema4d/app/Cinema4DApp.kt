package com.cinema4d.app

import android.app.Application
import com.cinema4d.app.di.AppContainer

class Cinema4DApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
