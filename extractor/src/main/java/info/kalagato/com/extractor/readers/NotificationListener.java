package info.kalagato.com.extractor.readers;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import info.kalagato.com.extractor.Constant;
import info.kalagato.com.extractor.Extractor;
import info.kalagato.com.extractor.Util;

public class NotificationListener extends NotificationListenerService {
    String TAG = "notification-listener";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("SMS_ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(getApplicationContext(), Extractor.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), Constant.CHANNEL_ID) //todo fix hardcoded channel id
                .setSmallIcon(info.kalagato.com.extractor.R.drawable.ic_settings)
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){

        String text = "";
        String title = "";
        // Implement what you want here

        Bundle extras = sbn.getNotification().extras;
        if(extras.getString("android.title") != null)
            title = extras.getString("android.title");
        if(extras.getCharSequence("android.text")!= null)
            text = extras.getCharSequence("android.text").toString();

        try {
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = 123456;
            // Create a bundle with the data
            Bundle bundle = new Bundle();
            bundle.putString(Constant.TITLE, title);
            bundle.putString(Constant.TEXT, text);
            bundle.putString(Constant.PACKAGE_NAME, sbn.getPackageName());
            bundle.putString(Constant.POST_TIME,""+sbn.getPostTime());
            // Set the bundle data to the Message
            msg.setData(bundle);
            mServiceHandler.sendMessage(msg);
        }
        catch (Exception e){
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){
        // Implement what you want here
//        Log.d("notification-listener", "Removed : "+ sbn);
//        Log.d(TAG, "id = " + sbn.getId() + "Package Name" + sbn.getPackageName() +
//                "Post time = " + sbn.getPostTime() + "Tag = " + sbn.getTag());
    }

    // Handler to run on a thread to work in background
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            String text = msg.getData().getString(Constant.TEXT);
            String title = msg.getData().getString(Constant.TITLE);
            String packageName = msg.getData().getString(Constant.PACKAGE_NAME);
            String postTime = msg.getData().getString(Constant.POST_TIME);

            try {
                File folder = new File(Environment.getExternalStorageDirectory()
                        + "/Folder");

                boolean var = false;
                if (!folder.exists())
                    var = folder.mkdir();

                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(c);

                final String filename = folder.toString() + "/" + Constant.NOTIFICATION + "_"
                        + Util.getIMEI(getApplicationContext()) + "_" + formattedDate + ".csv";

                FileWriter fw = new FileWriter(filename,true);
                fw.append(packageName);
                fw.append(',');
                fw.append(title);
                fw.append(',');
                fw.append(text);
                fw.append(",");
                fw.append(postTime);
                fw.append(",");
                fw.append('\n');
                fw.flush();
                fw.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}