package info.kalagato.com.extractor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;


import androidx.core.content.ContextCompat;

import info.kalagato.com.extractor.readers.AppRunningStatus;
import info.kalagato.com.extractor.readers.LocationReader;

public class Extractor {

    private String channelId = "123";
    private String name = "";

    public void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelId,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void getAppsData(Context context){
        new AppRunningStatus().getActiveApps(context);
        Intent serviceIntent = new Intent(context, LocationReader.class);
        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
