package com.mebanking.app

import android.app.Application
import com.mebanking.app.di.AppContainer
import com.mebanking.app.di.DefaultAppContainer

open class MEbankingApplication : Application() {

    open val container: AppContainer by lazy {
        DefaultAppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
