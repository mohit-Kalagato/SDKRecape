package info.kalagato.com.extractor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import info.kalagato.com.extractor.readers.ReadSMSService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d("alarm-sms", "BroadcastReceiver received!");
        Intent serviceIntent = new Intent(context, ReadSMSService.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
