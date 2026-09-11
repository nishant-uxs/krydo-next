package dev.krydo.mobile

import android.app.Application
import dev.krydo.mobile.data.AppContainer

class KrydoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
