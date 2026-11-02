package com.androidfung.linkscrub.di

import android.content.Context
import com.androidfung.linkscrub.data.local.LinkScrubDatabase
import com.androidfung.linkscrub.data.network.JsoupOpenGraphScraper
import com.androidfung.linkscrub.data.network.OkHttpRedirectUnshortener
import com.androidfung.linkscrub.data.network.OpenGraphScraper
import com.androidfung.linkscrub.data.network.RedirectUnshortener
import com.androidfung.linkscrub.data.repository.HistoryRepository
import com.androidfung.linkscrub.data.repository.RoomHistoryRepository
import com.androidfung.linkscrub.data.repository.SettingsRepository
import com.androidfung.linkscrub.data.repository.SharedPreferencesSettingsRepository
import com.androidfung.linkscrub.domain.CleanUrlUseCase
import com.androidfung.linkscrub.domain.UrlCleaner

/**
 * Application-level dependency injection container for LinkScrub.
 */
interface AppContainer {
    val urlCleaner: UrlCleaner
    val redirectUnshortener: RedirectUnshortener
    val openGraphScraper: OpenGraphScraper
    val historyRepository: HistoryRepository
    val settingsRepository: SettingsRepository
    val cleanUrlUseCase: CleanUrlUseCase
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val urlCleaner: UrlCleaner by lazy {
        UrlCleaner()
    }

    override val redirectUnshortener: RedirectUnshortener by lazy {
        OkHttpRedirectUnshortener()
    }

    override val openGraphScraper: OpenGraphScraper by lazy {
        JsoupOpenGraphScraper()
    }

    override val historyRepository: HistoryRepository by lazy {
        RoomHistoryRepository(LinkScrubDatabase.getInstance(appContext).historyDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SharedPreferencesSettingsRepository(appContext)
    }

    override val cleanUrlUseCase: CleanUrlUseCase by lazy {
        CleanUrlUseCase(
            urlCleaner = urlCleaner,
            redirectUnshortener = redirectUnshortener,
            openGraphScraper = openGraphScraper,
            historyRepository = historyRepository
        )
    }
}
