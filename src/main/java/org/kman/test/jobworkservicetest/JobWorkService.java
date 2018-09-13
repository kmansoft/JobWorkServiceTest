package org.kman.test.jobworkservicetest;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class JobWorkService extends JobService {
	private static final String NM_CHANNEL_ID = "jobworkservice";

	private NotificationManager mNM;
	private CommandProcessor mCurProcessor;

	public static final int JOB_ID_1 = 1;

	public static final int JOB_ID_NETWORK_1 = 101;
	public static final int JOB_ID_NETWORK_2 = 102;

	/**
	 * This is a task to dequeue and process work in the background.
	 */
	@SuppressLint("StaticFieldLeak")
	final class CommandProcessor extends AsyncTask<Void, Void, Void> {
		private final JobParameters mParams;

		CommandProcessor(JobParameters params) {
			mParams = params;
		}

		@Override
		protected Void doInBackground(Void... params) {
			boolean cancelled;
			JobWorkItem work;

			final int notificationId = mParams.getJobId();

			/*
			 * Iterate over available work.  Once dequeueWork() returns null, the
			 * job's work queue is empty and the job has stopped, so we can let this
			 * async task complete.
			 */
			while (!(cancelled = isCancelled()) && (work = mParams.dequeueWork()) != null) {
				String txt = work.getIntent().getStringExtra("name");
				Log.i("JobWorkService", "Processing work: " + work + ", msg: " + txt);
				showNotification(notificationId, txt);
				// Process work here...  we'll pretend by sleeping.
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					Log.w("JobWorkService", "Interrupted", e);
				}
				// Tell system we have finished processing the work.
				Log.i("JobWorkService", "Done with: " + work);
				mParams.completeWork(work);
			}
			hideNotification(notificationId);
			if (cancelled) {
				Log.i("JobWorkService", "CANCELLED!");
			}
			return null;
		}
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this, R.string.job_service_created, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDestroy() {
		hideNotification(JOB_ID_1);
		Toast.makeText(this, R.string.job_service_destroyed, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onStartJob(JobParameters params) {
		// Start task to pull work out of the queue and process it.
		mCurProcessor = new CommandProcessor(params);
		mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		// Allow the job to continue running while we process work.
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		// Have the processor cancel its current work.
		mCurProcessor.cancel(true);
		// Tell the system to reschedule the job -- the only reason we would be here is
		// because the job needs to stop for some reason before it has completed all of
		// its work, so we would like it to remain to finish that work in the future.
		return true;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification(int notificationId, String text) {
		if (mNM.getNotificationChannel(NM_CHANNEL_ID) == null) {
			final NotificationChannel channel = new NotificationChannel(NM_CHANNEL_ID, getString(R.string.app_name),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(getString(R.string.service_name));
			mNM.createNotificationChannel(channel);
		}

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, JobWorkServiceActivity
				.class), 0);
		// Set the info for the views that show in the notification panel.
		Notification.Builder noteBuilder = new Notification.Builder(this, NM_CHANNEL_ID)
				.setSmallIcon(R.drawable.stat_sample) // the status icon
				.setTicker(text)  // the status text
				.setWhen(System.currentTimeMillis())  // the time stamp
				.setContentTitle(getText(R.string.service_start_arguments_label))  // the label
				.setContentText(text)  // the contents of the entry
				.setContentIntent(contentIntent);  // The intent to send when the entry is clicked
		// We show this for as long as our service is processing a command.
		noteBuilder.setOngoing(true);
		// Send the notification.
		mNM.notify(notificationId, noteBuilder.build());
	}

	private void hideNotification(int notificationId) {
		mNM.cancel(notificationId);
	}
}
