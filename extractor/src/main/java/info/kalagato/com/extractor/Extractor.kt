package info.kalagato.com.extractor

import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import info.kalagato.com.extractor.readers.AppRunningStatus
import android.content.Intent
import androidx.core.content.ContextCompat
import info.kalagato.com.extractor.readers.LocationReader

class Extractor {
    private val channelId = "123"
    private val name = ""
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                name,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    fun getAppsData(context: Context) {
        AppRunningStatus.getActiveApps(context)
        val serviceIntent = Intent(context, LocationReader::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}