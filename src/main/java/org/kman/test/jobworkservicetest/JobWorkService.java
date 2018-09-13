package org.kman.test.jobworkservicetest;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.app.job.JobWorkItem;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class JobWorkService extends JobService {
	private static final String NM_CHANNEL_ID = "jobworkservice";

	private Handler mHandler;

	private CommandProcessor mCurProcessor;

	/**
	 * This makes it so that the service's Job never stops, even if we complete all de-queued work items.
	 */
	private static final boolean DEQUEUE_ALL_THEN_PROCESS = true;

	public static final int JOB_ID_1 = 1;

	public static final int JOB_ID_NETWORK_1 = 101;
	public static final int JOB_ID_NETWORK_2 = 102;

	/**
	 * This is a task to dequeue and process work in the background.
	 */
	static class CommandProcessor extends AsyncTask<Void, Void, Void> {
		@SuppressLint("StaticFieldLeak")
		private final Context mContext;
		private final JobParameters mParams;

		CommandProcessor(Context context, JobParameters params) {
			mContext = context;
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
				showNotification(mContext, notificationId, txt);
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
			hideNotification(mContext, notificationId);
			if (cancelled) {
				Log.i("JobWorkService", "CANCELLED!");
			}
			return null;
		}
	}

	// Difference vs. sample: executes one JobWorkItem
	static class JobWorkItemTask implements Runnable {

		final Context mContext;
		final JobParameters mParams;
		final JobWorkItem mWorkItem;

		JobWorkItemTask(Context context, JobParameters params, JobWorkItem workItem) {
			mContext = context;
			mParams = params;
			mWorkItem = workItem;
		}

		@Override
		public void run() {

			final int notificationId = mParams.getJobId();

			String txt = mWorkItem.getIntent().getStringExtra("name");
			Log.i("JobWorkService", "Processing work: " + mWorkItem + ", msg: " + txt);
			showNotification(mContext, notificationId, txt);

			// Process work here...  we'll pretend by sleeping.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Log.w("JobWorkService", "Interrupted", e);
			}

			// Tell system we have finished processing the work.
			Log.i("JobWorkService", "Done with: " + mWorkItem);
			mParams.completeWork(mWorkItem);

			hideNotification(mContext, notificationId);
		}
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, R.string.job_service_created, Toast.LENGTH_SHORT).show();
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public void onDestroy() {
		hideNotification(this, JOB_ID_1);
		hideNotification(this, JOB_ID_NETWORK_1);
		hideNotification(this, JOB_ID_NETWORK_2);

		Toast.makeText(this, R.string.job_service_destroyed, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onStartJob(final JobParameters params) {
		final Context context = getApplicationContext();

		if (DEQUEUE_ALL_THEN_PROCESS) {
			// Difference from sample: dequeue all work items at once, queue them into executor
			//
			// See comment in dequeueAllThenProcess

			// Use a handler so we start de-queuing after onStartJob returns back to Android code (we're on main
			// thread here) and we even add a small delay for Android code to do what it needs to do.

			// dequeueAllThenProcess(params);

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					dequeueAllThenProcess(context, params);
				}
			}, 250);
		} else {
			// Start task to pull work out of the queue and process it.
			mCurProcessor = new CommandProcessor(context, params);
			mCurProcessor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}

		// Allow the job to continue running while we process work.
		return true;
	}

	@Override
	public boolean onStopJob(JobParameters params) {
		if (mCurProcessor != null) {
			// Have the processor cancel its current work.
			mCurProcessor.cancel(true);
		}

		// Tell the system to reschedule the job -- the only reason we would be here is
		// because the job needs to stop for some reason before it has completed all of
		// its work, so we would like it to remain to finish that work in the future.
		return true;
	}

	/**
	 * The documentation says:
	 *
	 * https://developer.android.com/reference/android/app/job/JobParameters#dequeueWork()
	 *
	 * You do not, however, have to complete each returned work item before deqeueing the next one -- you can use
	 * dequeueWork() multiple times before completing previous work if you want to process work in parallel, and you
	 * can complete the work in whatever order you want.
	 *
	 * And so we do - dequeue all work items and submit them as runnables to an executor.
	 *
	 * However:
	 *
	 * This makes it so that the service is never stopped and the job is never finished.
	 *
	 * After we've called params.completeWork(item):
	 *
	 * 1 - the Job stays running
	 *
	 * Slot #0: b42dd0b #u0a88/1 org.kman.test.jobworkservicetest/.JobWorkService
	 * Running for: +6s292ms, timeout at: +9m53s741ms
	 * u0a88 tag=*job* /org.kman.test.jobworkservicetest/.JobWorkService
	 * Source: uid=u0a88 user=0 pkg=org.kman.test.jobworkservicetest
	 * Required constraints: DEADLINE
	 * Enqueue time: -6s292ms
	 * Run time: earliest=none, latest=-6s293ms
	 * Evaluated priority: 40
	 * Active at -6s292ms, pending for 0
	 *
	 * Note that there are no work items in this output, the service is running "on its own", the "old way", before
	 * JobWorkItems existed (pre-8.0).
	 *
	 * 2 - The wake lock associated with the job is still held, by Android:
	 *
	 * Wake Locks: size=1
	 * PARTIAL_WAKE_LOCK '*job* /org.kman.test.jobworkservicetest/.JobWorkService' ACQ=-1m16s23ms
	 * LONG (uid=1000 pid=1872 ws=WorkSource{10088})
	 *
	 * Presumably this will time out when the currently "running" stuck job is told to stop - which takes 10 minutes.
	 * A hell of a battery drain.
	 *
	 * 3 - More JobWorkItem's can be enqueue'd but...
	 *
	 * The service never receives them (at least not until the currently "running" stuck job finishes).
	 */
	private static void dequeueAllThenProcess(Context context, JobParameters params) {
		Log.i("JobWorkService", "dequeueThenProcess");

		JobWorkItem item;
		while ((item = params.dequeueWork()) != null) {
			AsyncTask.SERIAL_EXECUTOR.execute(new JobWorkItemTask(context, params, item));
		}
	}

	/**
	 * Show a notification while this service is running.
	 */
	private static void showNotification(Context context, int notificationId, String text) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (nm == null) {
			return;
		}

		if (nm.getNotificationChannel(NM_CHANNEL_ID) == null) {
			final NotificationChannel channel = new NotificationChannel(NM_CHANNEL_ID,
					context.getString(R.string.app_name),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(context.getString(R.string.service_name));
			nm.createNotificationChannel(channel);
		}

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, JobWorkServiceActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		Notification.Builder noteBuilder = new Notification.Builder(context, NM_CHANNEL_ID)
				.setSmallIcon(R.drawable.stat_sample) // the status icon
				.setTicker(text)  // the status text
				.setWhen(System.currentTimeMillis())  // the time stamp
				.setContentTitle(context.getText(R.string.service_start_arguments_label))  // the label
				.setContentText(text)  // the contents of the entry
				.setContentIntent(contentIntent);  // The intent to send when the entry is clicked
		// We show this for as long as our service is processing a command.
		noteBuilder.setOngoing(true);

		// Send the notification.
		nm.notify(notificationId, noteBuilder.build());
	}

	private static void hideNotification(Context context, int notificationId) {
		final NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (nm == null) {
			return;
		}

		nm.cancel(notificationId);
	}
}
