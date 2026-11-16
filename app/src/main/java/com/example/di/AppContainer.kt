package com.example.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.DefaultSportsDataRepository
import com.example.data.FirebaseLiveTranslator
import com.example.data.abstraction.ChannelDirectorySource
import com.example.data.abstraction.LiveCaptionTranslator
import com.example.data.abstraction.ScoreboardDataSource
import com.example.data.abstraction.SportsDataRepository
import com.example.data.abstraction.UserPreferencesStore
import com.example.data.sources.DataStorePreferencesStore
import com.example.data.sources.EspnScoreboardDataSource
import com.example.data.sources.IptvChannelDirectorySource
import com.example.ui.viewmodel.SportsViewModel
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val httpClient: OkHttpClient by lazy {
        val cacheDir = File(appContext.cacheDir, "http_cache")
        OkHttpClient.Builder()
            .cache(Cache(cacheDir, 16L * 1024 * 1024))
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val preferencesStore: UserPreferencesStore by lazy {
        DataStorePreferencesStore(appContext)
    }

    private val channelSource: ChannelDirectorySource by lazy {
        IptvChannelDirectorySource(
            httpClient = httpClient,
            cacheDir = appContext.cacheDir
        )
    }

    private val scoreboardSource: ScoreboardDataSource by lazy {
        EspnScoreboardDataSource(httpClient)
    }

    val sportsRepository: SportsDataRepository by lazy {
        DefaultSportsDataRepository(
            channelSource = channelSource,
            scoreboardSource = scoreboardSource,
            preferencesStore = preferencesStore
        )
    }

    val captionTranslator: LiveCaptionTranslator by lazy {
        FirebaseLiveTranslator()
    }

    val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SportsViewModel::class.java)) {
                    return SportsViewModel(
                        repository = sportsRepository,
                        translator = captionTranslator
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
