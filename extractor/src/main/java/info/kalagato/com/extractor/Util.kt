package info.kalagato.com.extractor

import android.content.ComponentName
import info.kalagato.com.extractor.TestJobService
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.content.ContextCompat

object Util {
    private const val MY_PREFS_NAME = "info.kalagato.com.extractortest"
    private const val LAST_SYNC_DATE_TIME = "LAST_SYNC_DATE_TIME"
    private const val LAST_UPLOAD_DATE_TIME = "LAST_UPLOAD_DATE_TIME"

    // schedule the start of the service every 10 - 30 seconds
    @JvmStatic
    fun scheduleJob(context: Context) {
        val serviceComponent = ComponentName(context, TestJobService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        builder.setMinimumLatency((10 * 60 * 1000).toLong()) // wait at least
        builder.setOverrideDeadline((30 * 60 * 1000).toLong()) // maximum delay
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        var jobScheduler: JobScheduler? = null
        jobScheduler = context.getSystemService(JobScheduler::class.java)
        jobScheduler.allPendingJobs
        jobScheduler.schedule(builder.build())
    }

    @SuppressLint("HardwareIds")
    @JvmStatic
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    @JvmStatic
    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(
            MY_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        return prefs.getLong(LAST_SYNC_DATE_TIME, 0)
    }

    @JvmStatic
    fun setLastSyncDateTime(context: Context, date: Long) {
        val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(LAST_SYNC_DATE_TIME, date)
        editor.apply()
    }

    @JvmStatic
    fun setLastUploadTime(context: Context, date: Long) {
        val editor = context.getSharedPreferences(MY_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(LAST_UPLOAD_DATE_TIME, date)
        editor.apply()
    }

    @JvmStatic
    fun getLastUploadTime(context: Context): Long {
        val prefs = context.getSharedPreferences(
            MY_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        return prefs.getLong(LAST_UPLOAD_DATE_TIME, 0)
    }

    fun createNotificationChannel(context: Context?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Constant.CHANNEL_ID,
                "Example Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = ContextCompat.getSystemService(
                context!!, NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}