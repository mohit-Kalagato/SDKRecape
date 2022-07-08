package info.kalagato.com.extractor.readers

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.*
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.gson.JsonObject
import info.kalagato.com.extractor.Constant
import info.kalagato.com.extractor.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class ReadGeneralInformation : Service() {

    private val deviceDetail = JSONObject()
    private val wifiDetails = JSONObject()
    private val locationDetails = JSONObject()
    private val jsonObject = JSONObject()
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate() {


        // creating location updates
       /* fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    Log.d("TAG", "onLocationResult: $location")
                }
            }
        }*/
        CoroutineScope(Dispatchers.IO).launch {

            //getting the device information from user device
            getSystemDetail(deviceDetail)
            getDarkMode(deviceDetail)
            getWifiDetails(applicationContext,wifiDetails)
            withContext(Dispatchers.Main){
                getLocation(applicationContext)
            }

            jsonObject.put("device Details",deviceDetail)
            jsonObject.put("wifi details",wifiDetails)
            jsonObject.put("location details",locationDetails)

        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @SuppressLint("HardwareIds")
    private fun getSystemDetail(deviceDetail: JSONObject): JSONObject {
        deviceDetail.put("Brand", Build.BRAND)
        deviceDetail.put(
            "DeviceID",
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        )
        deviceDetail.put("Model: ", Build.MODEL)
        deviceDetail.put("ID", Build.ID)
        deviceDetail.put("SDK", Build.VERSION.SDK_INT)
        deviceDetail.put("Manufacture", Build.MANUFACTURER)
        deviceDetail.put("Brand", Build.BRAND)
        deviceDetail.put("User", Build.USER)
        deviceDetail.put("Type", Build.TYPE)
        deviceDetail.put("Base", Build.VERSION_CODES.BASE)
        deviceDetail.put("Board", Build.BOARD)
        deviceDetail.put("Host", Build.HOST)
        deviceDetail.put("FingerPrint", Build.FINGERPRINT)
        deviceDetail.put("Version Code", Build.VERSION.RELEASE)
        deviceDetail.put("Supported Network", supportedNetwork(applicationContext))

        return deviceDetail
    }

    private fun getDarkMode(deviceDetail: JSONObject) {
        when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {deviceDetail.put("Dark Mode","YES")}
            Configuration.UI_MODE_NIGHT_NO -> {deviceDetail.put("Dark Mode","NO")}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {deviceDetail.put("Dark Mode","NA")}
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
            wifiDetails.put("Ip Address",info.ipAddress)
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
            val locationListener: LocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Called when a new location is found by the network location provider.

                    locationDetails.put("Latitude",location.latitude)
                    locationDetails.put("Longitude",location.longitude)
                    locationDetails.put("Time",location.time)
                    locationDetails.put("Provider",location.provider)
                    locationDetails.put("Accuracy",location.accuracy)
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
                        stopSelf()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("TAG", "onLocationChanged: " + e.localizedMessage)
                    }
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
                LocationManager.NETWORK_PROVIDER,
                0,
                0f,
                locationListener
            )
        } catch (e: Exception) {
            Log.d("TAG", "getLocation: ${e.localizedMessage} ")
        }
    }

}