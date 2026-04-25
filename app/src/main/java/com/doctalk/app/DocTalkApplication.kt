package com.doctalk.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for DocTalk
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class DocTalkApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Application initialization code can go here
        // For example: logging, crash reporting setup, etc.
    }
}
