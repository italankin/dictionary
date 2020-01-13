package com.italankin.dictionary.di.modules

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.italankin.dictionary.BuildConfig
import com.italankin.dictionary.api.DictionaryApiClient
import com.italankin.dictionary.utils.SharedPrefs
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import javax.inject.Singleton

/**
 * Main context module, contains main dependencies.
 */
@Module
class MainModule {

    @Provides
    @Singleton
    fun provideClipboardManager(context: Context): ClipboardManager {
        return context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    @Provides
    @Singleton
    fun provideInputMethodManager(context: Context): InputMethodManager {
        return context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    @Provides
    @Singleton
    fun provideSharedPrefs(context: Context, gson: Gson): SharedPrefs {
        return SharedPrefs(context, gson)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        val builder = GsonBuilder()
        if (BuildConfig.DEBUG) {
            builder.setPrettyPrinting()
        }
        return builder.create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logger = object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) = Timber.tag("OkHttp").d(message)
            }
            builder.addInterceptor(HttpLoggingInterceptor(logger))
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApiClient(client: OkHttpClient?): DictionaryApiClient {
        return DictionaryApiClient(client!!, BuildConfig.BASE_URL, BuildConfig.API_KEY)
    }
}
