package info.kalagato.com.extractor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

import androidx.core.content.ContextCompat;

public class Util {

    private static final String MY_PREFS_NAME = "info.kalagato.com.extractortest";
    private static final String LAST_SYNC_DATE_TIME = "LAST_SYNC_DATE_TIME";

    // schedule the start of the service every 10 - 30 seconds
    public static void scheduleJob(Context context) {
        ComponentName serviceComponent = new ComponentName(context, TestJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(10 * 60 * 1000); // wait at least
        builder.setOverrideDeadline(30 * 60 * 1000); // maximum delay
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = null;
        jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.getAllPendingJobs();
        jobScheduler.schedule(builder.build());
    }

    public static String getDeviceId(Context context){
        @SuppressLint("HardwareIds") String mId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return mId;
    }

    public static long getLastSyncTime(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        long restoredText = prefs.getLong(LAST_SYNC_DATE_TIME, 0);

        return  restoredText;
    }
    public static void setLastSyncDateTime(Context context,long date) {
        SharedPreferences.Editor editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        editor.putLong(LAST_SYNC_DATE_TIME,date);
        editor.apply();
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    Constant.CHANNEL_ID,
                    "Example Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = ContextCompat.getSystemService(context,NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}