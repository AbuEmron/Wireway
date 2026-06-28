package com.wirewaypro.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] generates the root DI container that
 * every Activity / ViewModel pulls dependencies from.
 */
@HiltAndroidApp
class WirewayApplication : Application()
