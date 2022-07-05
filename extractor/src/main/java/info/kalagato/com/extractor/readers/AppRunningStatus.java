package info.kalagato.com.extractor.readers;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import info.kalagato.com.extractor.Constant;
import info.kalagato.com.extractor.Util;

public class AppRunningStatus {

    String TAG = "app-running-status";

    public void getAllPackageInstalled(Context context){
        try {
            File folder = new File(Environment.getExternalStorageDirectory()
                    + "/Folder");

            boolean var = false;
            if (!folder.exists())
                var = folder.mkdir();

            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(c);

            final String filename = folder.toString() + "/" + Constant.INSTALLED_APP + "_"
                    + Util.getDeviceId(context) + "_" + formattedDate + ".csv";

            final PackageManager pm = context.getPackageManager();
            //get a list of installed apps.
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            FileWriter fw = new FileWriter(filename,true);
            fw.append("Installed package");
            fw.append(',');
            fw.append("Source dir");
            fw.append(',');
            fw.append("Launch Activity");
            fw.append(',');
            fw.append('\n');

            for (ApplicationInfo packageInfo : packages) {
                fw.append(packageInfo.packageName);
                fw.append(',');
                fw.append(packageInfo.sourceDir);
                fw.append(',');
                fw.append(packageInfo.packageName);
                fw.append(",");
                fw.append('\n');
            }
            fw.flush();
            fw.close();

        } catch (Exception e) { }
    }

    public static void getActiveApps(Context context) {
        try
        {
                PackageManager pm = context.getPackageManager();
                List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        //
        //        String value =""; // basic date stamp
        //        value += "---------------------------------\n";
        //        value += "Active Apps\n";
        //        value += "=================================\n";
        //

                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(c);

                File folder = new File(Environment.getExternalStorageDirectory()
                        + "/Folder");

                boolean var = false;
                if (!folder.exists())
                    var = folder.mkdir();

                final String filename = folder.toString() + "/" + Constant.BACKGROUND_RUNNING_APP + "_"
                        + Util.getDeviceId(context) + "_" + formattedDate + ".csv";

                FileWriter fw = new FileWriter(filename,true);

                fw.append("package");
                fw.append(',');
                fw.append("Timestamp");
                fw.append(',');
                fw.append('\n');

                for (ApplicationInfo packageInfo : packages) {

                    //system apps! get out
                    if (!isSTOPPED(packageInfo) && !isSYSTEM(packageInfo)) {

    //                    value += getApplicationLabel(context, packageInfo.packageName) + "\n" + packageInfo.packageName  + "\n-----------------------\n";
                        fw.append(getApplicationLabel(context, packageInfo.packageName));
                        fw.append(',');
                        fw.append(""+Calendar.getInstance().getTime().getTime());
                        fw.append(',');
                        fw.append('\n');
                    }
                }

                fw.flush();
                fw.close();

        } catch (Exception e) { }

    }
    private static boolean isSTOPPED(ApplicationInfo pkgInfo) {

        return ((pkgInfo.flags & ApplicationInfo.FLAG_STOPPED) != 0);
    }

    private static boolean isSYSTEM(ApplicationInfo pkgInfo) {

        return ((pkgInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static String getApplicationLabel(Context context, String packageName) {

        PackageManager        packageManager = context.getPackageManager();
        List<ApplicationInfo> packages       = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        String                label          = null;

        for (int i = 0; i < packages.size(); i++) {

            ApplicationInfo temp = packages.get(i);

            if (temp.packageName.equals(packageName))
                label = packageManager.getApplicationLabel(temp).toString();
        }

        return label;
    }


    public static void getAppUsage(Context context){
        try {
        final UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);// Context.USAGE_STATS_SERVICE);
        // today
        Calendar beginDate = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        beginDate.set(Calendar.HOUR_OF_DAY, 0);
        beginDate.set(Calendar.MINUTE, 0);
        beginDate.set(Calendar.SECOND, 0);
        beginDate.set(Calendar.MILLISECOND, 0);

        // next day
        Calendar endDate = new GregorianCalendar();
        // reset hour, minutes, seconds and millis
        endDate.set(Calendar.HOUR_OF_DAY, 0);
        endDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.SECOND, 0);
        endDate.set(Calendar.MILLISECOND, 0);
        endDate.add(Calendar.DAY_OF_MONTH, -7);

        final List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginDate.getTimeInMillis(), endDate.getTimeInMillis());


        File folder = new File(Environment.getExternalStorageDirectory()
                + "/Folder");

        boolean var = false;
        if (!folder.exists())
            var = folder.mkdir();

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
        String formattedDate = df.format(c);



        final String filename = folder.toString() + "/" + Constant.APP_USAGE + "_"
                + Util.getDeviceId(context) + "_" + formattedDate + ".csv";




            FileWriter fw = new FileWriter(filename,true);
//            Log.d("app-usage","results for " + beginDate.getTime().toGMTString() + " - " + endDate.getTime().toGMTString());

            fw.append("package");
            fw.append(',');
            fw.append("Total Time in foreground");
            fw.append(',');
            fw.append('\n');

            for (UsageStats app : queryUsageStats) {
//                Log.d("app-usage",app.getPackageName() + " | " + (float) (app.getTotalTimeInForeground() / 1000));
                fw.append(app.getPackageName() );
                fw.append(',');
                fw.append(""+(float) (app.getTotalTimeInForeground() / 1000));
                fw.append(',');
                fw.append('\n');
            }

            fw.flush();
            fw.close();

        } catch (Exception e) {
        }

    }
}
