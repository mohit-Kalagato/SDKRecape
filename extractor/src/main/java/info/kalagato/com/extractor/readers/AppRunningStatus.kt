package info.kalagato.com.extractor.readers

import info.kalagato.com.extractor.Util.getDeviceId
import android.os.Environment
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import info.kalagato.com.extractor.readers.AppRunningStatus
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import android.content.Context
import info.kalagato.com.extractor.Constant
import java.io.File
import java.io.FileWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AppRunningStatus {
    var TAG = "app-running-status"
    fun getAllPackageInstalled(context: Context) {
        try {
            val folder = File(
                Environment.getExternalStorageDirectory()
                    .toString() + "/Folder"
            )
            var `var` = false
            if (!folder.exists()) `var` = folder.mkdir()
            val c = Calendar.getInstance().time
            val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val formattedDate = df.format(c)
            val filename = (folder.toString() + "/" + Constant.INSTALLED_APP + "_"
                    + getDeviceId(context) + "_" + formattedDate + ".csv")
            val pm = context.packageManager
            //get a list of installed apps.
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val fw = FileWriter(filename, true)
            fw.append("Installed package")
            fw.append(',')
            fw.append("Source dir")
            fw.append(',')
            fw.append("Launch Activity")
            fw.append(',')
            fw.append('\n')
            for (packageInfo in packages) {
                fw.append(packageInfo.packageName)
                fw.append(',')
                fw.append(packageInfo.sourceDir)
                fw.append(',')
                fw.append(packageInfo.packageName)
                fw.append(",")
                fw.append('\n')
            }
            fw.flush()
            fw.close()
        } catch (e: Exception) {
        }
    }

    companion object {
        fun getActiveApps(context: Context) {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                //
                //        String value =""; // basic date stamp
                //        value += "---------------------------------\n";
                //        value += "Active Apps\n";
                //        value += "=================================\n";
                //
                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy")
                val formattedDate = df.format(c)
                val folder = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/Folder"
                )
                var `var` = false
                if (!folder.exists()) `var` = folder.mkdir()
                val filename = (folder.toString() + "/" + Constant.BACKGROUND_RUNNING_APP + "_"
                        + getDeviceId(context) + "_" + formattedDate + ".csv")
                val fw = FileWriter(filename, true)
                fw.append("package")
                fw.append(',')
                fw.append("Timestamp")
                fw.append(',')
                fw.append('\n')
                for (packageInfo in packages) {

                    //system apps! get out
                    if (!isSTOPPED(packageInfo) && !isSYSTEM(packageInfo)) {

                        //                    value += getApplicationLabel(context, packageInfo.packageName) + "\n" + packageInfo.packageName  + "\n-----------------------\n";
                        fw.append(getApplicationLabel(context, packageInfo.packageName))
                        fw.append(',')
                        fw.append("" + Calendar.getInstance().time.time)
                        fw.append(',')
                        fw.append('\n')
                    }
                }
                fw.flush()
                fw.close()
            } catch (e: Exception) {
            }
        }

        private fun isSTOPPED(pkgInfo: ApplicationInfo): Boolean {
            return pkgInfo.flags and ApplicationInfo.FLAG_STOPPED != 0
        }

        private fun isSYSTEM(pkgInfo: ApplicationInfo): Boolean {
            return pkgInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        }

        fun getApplicationLabel(context: Context, packageName: String): String? {
            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            var label: String? = null
            for (i in packages.indices) {
                val temp = packages[i]
                if (temp.packageName == packageName) label =
                    packageManager.getApplicationLabel(temp).toString()
            }
            return label
        }

        fun getAppUsage(context: Context) {
            try {
                val usageStatsManager =
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager // Context.USAGE_STATS_SERVICE);
                // today
                val beginDate: Calendar = GregorianCalendar()
                // reset hour, minutes, seconds and millis
                beginDate[Calendar.HOUR_OF_DAY] = 0
                beginDate[Calendar.MINUTE] = 0
                beginDate[Calendar.SECOND] = 0
                beginDate[Calendar.MILLISECOND] = 0

                // next day
                val endDate: Calendar = GregorianCalendar()
                // reset hour, minutes, seconds and millis
                endDate[Calendar.HOUR_OF_DAY] = 0
                endDate[Calendar.MINUTE] = 0
                endDate[Calendar.SECOND] = 0
                endDate[Calendar.MILLISECOND] = 0
                endDate.add(Calendar.DAY_OF_MONTH, -7)
                val queryUsageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    beginDate.timeInMillis,
                    endDate.timeInMillis
                )
                val folder = File(
                    Environment.getExternalStorageDirectory()
                        .toString() + "/Folder"
                )
                var `var` = false
                if (!folder.exists()) `var` = folder.mkdir()
                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy")
                val formattedDate = df.format(c)
                val filename = (folder.toString() + "/" + Constant.APP_USAGE + "_"
                        + getDeviceId(context) + "_" + formattedDate + ".csv")
                val fw = FileWriter(filename, true)
                //            Log.d("app-usage","results for " + beginDate.getTime().toGMTString() + " - " + endDate.getTime().toGMTString());
                fw.append("package")
                fw.append(',')
                fw.append("Total Time in foreground")
                fw.append(',')
                fw.append('\n')
                for (app in queryUsageStats) {
//                Log.d("app-usage",app.getPackageName() + " | " + (float) (app.getTotalTimeInForeground() / 1000));
                    fw.append(app.packageName)
                    fw.append(',')
                    fw.append("" + (app.totalTimeInForeground / 1000).toFloat())
                    fw.append(',')
                    fw.append('\n')
                }
                fw.flush()
                fw.close()
            } catch (e: Exception) {
            }
        }
    }
}