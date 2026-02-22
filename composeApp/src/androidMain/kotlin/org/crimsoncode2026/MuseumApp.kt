package org.crimsoncode2026

import android.app.Application
import org.crimsoncode2026.di.initKoin

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
