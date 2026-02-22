package org.crimsoncode2026.auth

import android.content.Context

/**
 * Provides Android application context to shared code
 *
 * Must be initialized from Application.onCreate() or MainActivity.onCreate()
 */
object ContextProvider {

    private var applicationContext: Context? = null

    /**
     * Initialize with application context
     * Call from Application.onCreate()
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Get application context
     * @throws IllegalStateException if not initialized
     */
    fun getContext(): Context {
        return applicationContext ?: throw IllegalStateException(
            "ContextProvider must be initialized before use. Call initialize() in Application.onCreate()"
        )
    }
}
