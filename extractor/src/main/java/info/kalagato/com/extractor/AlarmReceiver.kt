package info.kalagato.com.extractor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import info.kalagato.com.extractor.readers.ReadSMSService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
//        Log.d("alarm-sms", "BroadcastReceiver received!");
        val serviceIntent = Intent(context, ReadSMSService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}