package com.uxp.uxpyouappinstaller;

import android.app.Application

class ApkInstallerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: ApkInstallerApplication
            private set
    }
}