package com.ds.extractorsdk

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import info.kalagato.com.extractor.readers.ReadSMSService


class MainActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private lateinit var listView:ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listView = findViewById(R.id.list_view)

        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        val information = getSystemDetail()
        Toast.makeText(this,information,Toast.LENGTH_SHORT).show()
         val ip = getWifiDetails(this)
        //AppRunningStatus.getActiveApps(this)

        Log.d("TAG", "onCreate: $ip")
        /*requestPermissions(arrayOf(Manifest.permission.READ_SMS,Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),101)*/

    }

    private fun getWifiDetails(context: Context): String? {
        var ssid: String? = null
        val connManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connManager.activeNetwork
        val networkInfo = connManager.getNetworkCapabilities(nw)
       /* if (networkInfo!!.isConnected) {
            val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
            val connectionInfo = wifiManager.connectionInfo
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.ssid)) {
                ssid = connectionInfo.ssid
            }
        }*/
        return ssid
    }


    private fun getDarkMode(){
        when (resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {}
            Configuration.UI_MODE_NIGHT_NO -> {}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {}
        }

    }


    // getting IMEI numbers only for below SDK 29 versions
   /* @SuppressLint("HardwareIds")
    private fun getIMEI(): String? {
        var IMEI = ""
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val telephonyMgr = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    IMEI =  telephonyMgr.imei
                }else {
                    telephonyMgr.deviceId
                }
            }
        }
        return IMEI
    }
*/



    // Function to find out type of network
    private fun mGetNetworkClass(context: Context): String? {

        // ConnectionManager instance
        val mConnectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mInfo = mConnectivityManager.activeNetworkInfo

        // If not connected, "-" will be displayed
        if (mInfo == null || !mInfo.isConnected) return "-"

        // If Connected to Wifi
        if (mInfo.type == ConnectivityManager.TYPE_WIFI) return "WIFI"

        // If Connected to Mobile
        if (mInfo.type == ConnectivityManager.TYPE_MOBILE) {
            return when (mInfo.subtype) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN, TelephonyManager.NETWORK_TYPE_GSM -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN, 19 -> "4G"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "?"
            }
        }
        return "?"
    }


    @SuppressLint("Range")
    fun getAllSms(): List<Sms>? {
        val lstSms: MutableList<Sms> = ArrayList()
        var objSms = Sms()
        val message = Uri.parse("content://sms/")
        val cr: ContentResolver = contentResolver
        val c: Cursor? = cr.query(message, null, null, null, null)
        startManagingCursor(c)
        val totalSMS: Int = c!!.count
        if (c.moveToFirst()) {
            for (i in 0 until totalSMS) {
                objSms = Sms()
                objSms.id = c.getString(c.getColumnIndexOrThrow("_id"))
                objSms.address = c.getString(c.getColumnIndexOrThrow("address"))
                objSms.msg = c.getString(c.getColumnIndexOrThrow("body"))
                objSms.readState = c.getString(c.getColumnIndex("read"))
                objSms.time = c.getString(c.getColumnIndexOrThrow("date"))
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.folderName = ("inbox")
                } else {
                    objSms.folderName = ("sent")
                }
                lstSms.add(objSms)
                c.moveToNext()
            }
        }
        // else {
        // throw new RuntimeException("You have no SMS");
        // }
        c.close()
        return lstSms
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: MutableMap<String, Boolean> ->
            val deniedList: List<String> = result.filter {
                !it.value
            }.map {
                it.key
            }

            when {
                deniedList.isNotEmpty() -> {
                    val map = deniedList.groupBy { permission ->
                        if (shouldShowRequestPermissionRationale(permission)) "DENIED" else "EXPLAINED"
                    }
                    map["DENIED"]?.let {
                        // request denied , request again
                        println("Permission denied")
                        requestPermissions(arrayOf(Manifest.permission.READ_SMS,Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),101)
                    }
                    map["EXPLAINED"]?.let {
                        //request denied ,send to settings
                        println("Permission Explain denied")
                        Snackbar.make(this.findViewById(android.R.id.content), "Please open app setting to grant the permission that is required to open the app", Snackbar.LENGTH_INDEFINITE)
                            .setAction("Setting"){
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri: Uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                            .show()

                    }

                }
                else -> {
                    //All request are permitted
                    //val list = getAllSms()
                    //setting alarm for SMS and App data
                    //setting alarm for SMS and App data
                    val serviceIntent = Intent(this, ReadSMSService::class.java)
                    ContextCompat.startForegroundService(this, serviceIntent)
                    // for location and background app data
                    // for location and background app data
                   // Util.scheduleJob(applicationContext)


                }
            }
        }


    @SuppressLint("HardwareIds")
    private fun getSystemDetail(): String {
        return "Brand: ${Build.BRAND} \n" +
                "DeviceID: ${
                    Settings.Secure.getString(
                        contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                } \n" +
                "Model: ${Build.MODEL} \n" +
                "ID: ${Build.ID} \n" +
                "SDK: ${Build.VERSION.SDK_INT} \n" +
                "Manufacture: ${Build.MANUFACTURER} \n" +
                "Brand: ${Build.BRAND} \n" +
                "User: ${Build.USER} \n" +
                "Type: ${Build.TYPE} \n" +
                "Base: ${Build.VERSION_CODES.BASE} \n" +
                "Incremental: ${Build.VERSION.INCREMENTAL} \n" +
                "Board: ${Build.BOARD} \n" +
                "Host: ${Build.HOST} \n" +
                "FingerPrint: ${Build.FINGERPRINT} \n" +
                "Version Code: ${Build.VERSION.RELEASE}"
    }


    // 2nd method to get the permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for ( permission in permissions){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)){
                //denied
                Log.e("denied", permission);
            }else{
                if(ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED){
                    //allowed
                    Log.e("allowed", permission)
                } else{
                    //set to never ask again
                    Log.e("set to never ask again", permission)
                    //do something here.
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}