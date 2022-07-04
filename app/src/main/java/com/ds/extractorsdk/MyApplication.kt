package com.ds.extractorsdk

import android.app.Application
import info.kalagato.com.extractor.Util

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Util.createNotificationChannel(applicationContext)
    }
}