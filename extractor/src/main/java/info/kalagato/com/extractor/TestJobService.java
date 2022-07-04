package info.kalagato.com.extractor;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
public class TestJobService extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Extractor extractor = new Extractor();
        // create a channel and provide the details
//        extractor.createNotificationChannel(this);
        extractor.getAppsData(this);
        Util.scheduleJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

}
