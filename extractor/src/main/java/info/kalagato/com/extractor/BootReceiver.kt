package info.kalagato.com.extractor

import info.kalagato.com.extractor.Util.scheduleJob
import android.content.BroadcastReceiver
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import info.kalagato.com.extractor.AlarmReceiver
import java.util.*

class BootReceiver : BroadcastReceiver() {
    private var alarmMgr: AlarmManager? = null
    private var alarmIntent: PendingIntent? = null
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            // Do my stuff
            scheduleJob(context)
            setAlarm(context)
        }
    }

    fun setAlarm(context: Context) {
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        // Set the alarm to start at 8:30 a.m.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar[Calendar.HOUR_OF_DAY] = 1
        calendar[Calendar.MINUTE] = 1

        // setRepeating() lets you specify a precise custom interval--in this case,
        // 24 hrs.
        // 5 min for testing
//        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+3000,
//                1000 * 60 * 5 , alarmIntent);
        alarmMgr!!.setRepeating(
            AlarmManager.RTC_WAKEUP, calendar.timeInMillis, (
                    1000 * 60 * 60 * 24).toLong(), alarmIntent
        )
    }
}