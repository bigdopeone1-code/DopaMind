package com.dopamind.app

import android.app.Application
import com.dopamind.app.core.di.AppContainer

class DopaMindApplication : Application() {
    val container: AppContainer by lazy { AppContainer(applicationContext) }
}
