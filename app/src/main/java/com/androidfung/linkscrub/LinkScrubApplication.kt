package com.androidfung.linkscrub

import android.app.Application
import com.androidfung.linkscrub.di.AppContainer
import com.androidfung.linkscrub.di.DefaultAppContainer

class LinkScrubApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
