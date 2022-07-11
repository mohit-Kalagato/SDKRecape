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
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.Extractor
import info.kalagato.com.extractor.R
import info.kalagato.com.extractor.networkCall.ApiClient
import info.kalagato.com.extractor.networkCall.IpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response


class ReadGeneralInformation : Service() {

    private val deviceDetail = JSONObject()
    private val wifiDetails = JSONObject()
    private val locationDetails = JSONObject()
    private val location = JSONObject()
    private val installData = JSONObject()
    private val ipData = JSONObject()
    private val jsonObject = JSONObject()
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {


        // getting location updates
        if (ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            getLocation(applicationContext)
        }



        CoroutineScope(Dispatchers.IO).launch {

            //getting the device information from user device
            getIpLocation()
            getSystemDetail(deviceDetail)
            getDarkMode(deviceDetail)
            getWifiDetails(applicationContext,wifiDetails)

            jsonObject.put("package_name",applicationContext.packageName)
            jsonObject.put("user_id",Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
            jsonObject.put("device_Details",deviceDetail)
            jsonObject.put("wifi_details",wifiDetails)
            location.put("device_location",locationDetails)
            location.put("ip_location",ipData)
            jsonObject.put("location",location)
            applicationContext
                .packageManager
                .getPackageInfo(applicationContext.packageName, 0).apply {
                    installData.put("first_install",this.firstInstallTime)
                    installData.put("last_used",this.lastUpdateTime)
                    installData.put("version_code",this.versionCode)
                    installData.put("version_name",this.versionName)
                }
            jsonObject.put("installation_data",installData)

        }

    }

    private suspend fun getIpLocation() {
        val response: Response<IpResponse> =ApiClient.client.getIpLocation() as Response<IpResponse>
        withContext(Dispatchers.Main){
            if (response.isSuccessful){
                     ipData.put("latitude",response.body()!!.lat)
                     ipData.put("longitude",response.body()!!.lon)
                     ipData.put("country",response.body()!!.country)
                     ipData.put("country_code",response.body()!!.countryCode)
                     ipData.put("region_name",response.body()!!.regionName)
                     ipData.put("city",response.body()!!.city)
                     ipData.put("postal_code",response.body()!!.zip)
                     ipData.put("time_zone",response.body()!!.timezone)
                     ipData.put("time_zone",response.body()!!.timezone)
                     ipData.put("query",response.body()!!.query)
            }else{
                Log.d("TAG", "getIpLocation: ${response.message()}")
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            Log.d("TAG", "getLastKnownLocation: $location")
        }.addOnFailureListener {
            Log.d("TAG", "getLastKnownLocation: ${it.localizedMessage}")
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
    private fun getSystemDetail(deviceDetail: JSONObject): JSONObject {
        deviceDetail.put(
            "device_id",
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        )
        deviceDetail.put("model: ", Build.MODEL)
        deviceDetail.put("id", Build.ID)
        deviceDetail.put("sdk", Build.VERSION.SDK_INT)
        deviceDetail.put("manufacture", Build.MANUFACTURER)
        deviceDetail.put("brand", Build.BRAND)
        deviceDetail.put("user", Build.USER)
        deviceDetail.put("type", Build.TYPE)
        deviceDetail.put("base", Build.VERSION_CODES.BASE)
        deviceDetail.put("board", Build.BOARD)
        deviceDetail.put("host", Build.HOST)
        deviceDetail.put("finger_print", Build.FINGERPRINT)
        deviceDetail.put("version_code", Build.VERSION.RELEASE)
        deviceDetail.put("supported_network", supportedNetwork(applicationContext))

        return deviceDetail
    }

    private fun getDarkMode(deviceDetail: JSONObject) {
        when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {deviceDetail.put("dark_mode","YES")}
            Configuration.UI_MODE_NIGHT_NO -> {deviceDetail.put("dark_mode","NO")}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {deviceDetail.put("dark_mode","NA")}
        }

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

                    locationDetails.put("Latitude",location.latitude)
                    locationDetails.put("Longitude",location.longitude)
                    locationDetails.put("Time",location.time)
                    locationDetails.put("Provider",location.provider)
                    locationDetails.put("Accuracy",location.accuracy)
                    Log.d("TAG", "onLocationChanged: $location ")
                     stopForeground(true)
                    stopSelf()
                    locationManager.removeUpdates(this)
                    /*try {
                        Log.d("TAG", "onLocationChanged: $location")
                        val folder = File(
                            Environment.getExternalStorageDirectory()
                                .toString() + "/Folder"
                        )
                        val mydir = applicationContext.getDir(
                            "mydir",
                            MODE_PRIVATE
                        ) //Creating an internal dir;
                        *//* boolean var = false;
                        if (!folder.exists())
                            var = folder.mkdir();*//*
                        val c = Calendar.getInstance().time
                        val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                        val formattedDate = df.format(c)
                        val filename = (mydir.toString() + "/" + Constant.LOCATION + "_"
                                + Util.getDeviceId(context) + "_" + formattedDate + ".csv")
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("TAG", "onLocationChanged: " + e.localizedMessage)
                    }*/
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