package info.kalagato.com.extractor

import info.kalagato.com.extractor.Util.scheduleJob
import android.app.job.JobService
import android.app.job.JobParameters
import info.kalagato.com.extractor.Extractor

/**
 * JobService to be scheduled by the JobScheduler.
 * start another service
 */
class TestJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val extractor = Extractor()
        // create a channel and provide the details
//        extractor.createNotificationChannel(this);
        extractor.getAppsData(this)
        scheduleJob(applicationContext) // reschedule the job
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        private const val TAG = "SyncService"
    }
}