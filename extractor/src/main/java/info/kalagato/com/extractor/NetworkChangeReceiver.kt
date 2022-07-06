package info.kalagato.com.extractor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import info.kalagato.com.extractor.SyncService
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.core.content.ContextCompat

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isOnline(context)) {
            // Do something
            val serviceIntent = Intent(context, SyncService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        //should check null because in airplane mode it will be null
        return netInfo != null && netInfo.isConnected
    }
}