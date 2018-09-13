package org.kman.test.jobworkservicetest;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.Process;
import android.view.View;
import android.widget.Button;

public class JobWorkServiceActivity extends Activity {

	JobScheduler mJobScheduler;
	JobInfo mJobInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
		mJobInfo = new JobInfo.Builder(JobWorkService.JOB_ID_1, new ComponentName(this, JobWorkService.class))
				.setOverrideDeadline(0).build();

		setContentView(R.layout.job_work_service_activity);

		// Watch for button clicks.
		Button button = findViewById(R.id.enqueue1);
		button.setOnClickListener(mEnqueue1Listener);

		button = findViewById(R.id.enqueue2);
		button.setOnClickListener(mEnqueue2Listener);

		button = findViewById(R.id.kill);
		button.setOnClickListener(mKillListener);

		// New buttons, jobs with networking
		button = findViewById(R.id.enqueue_network1);
		button.setOnClickListener(mEnqueueNetworkingListener);

		button = findViewById(R.id.enqueue_network2);
		button.setOnClickListener(mEnqueueNetworkingListener);
	}

	private void onClickEnqueueNetwork(View v) {
		final int jobId;

		final int viewId = v.getId();
		if (viewId == R.id.enqueue_network1) {
			jobId = JobWorkService.JOB_ID_NETWORK_1;
		} else {
			jobId = JobWorkService.JOB_ID_NETWORK_2;
		}

		final JobInfo.Builder builder = new JobInfo.Builder(jobId, new ComponentName(this, JobWorkService.class));

		// Parameters
		final Intent intent = new Intent("network");

		// Difference vs. sample code: require networking
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
		if (jobId == JobWorkService.JOB_ID_NETWORK_1) {
			builder.setOverrideDeadline(0);
			intent.putExtra("name", "Network Job 1");
		} else {
			// Difference 2: set min latency, don't set override deadline
			builder.setMinimumLatency(500);
			intent.putExtra("name", "Network Job 2");
		}
		builder.setBackoffCriteria(5 * 60 * 1000, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

		// Difference 3: extras
		final PersistableBundle onceExtras1 = new PersistableBundle();
		onceExtras1.putString("abc", "def");
		onceExtras1.putInt("foo", 1);
		onceExtras1.putInt("bar", 2);
		builder.setExtras(onceExtras1);

		final JobInfo job = builder.build();
		mJobScheduler.enqueue(job, new JobWorkItem(intent));
	}

	private View.OnClickListener mEnqueue1Listener = new View.OnClickListener() {
		public void onClick(View v) {
			mJobScheduler.enqueue(mJobInfo, new JobWorkItem(new Intent("com.example.android.apis.ONE").putExtra
					("name", "One")));
		}
	};
	private View.OnClickListener mEnqueue2Listener = new View.OnClickListener() {
		public void onClick(View v) {
			mJobScheduler.enqueue(mJobInfo, new JobWorkItem(new Intent("com.example.android.apis.TWO").putExtra
					("name", "Two")));
		}
	};
	private View.OnClickListener mKillListener = new View.OnClickListener() {
		public void onClick(View v) {
			// This is to simulate the service being killed while it is
			// running in the background.
			Process.killProcess(Process.myPid());
		}
	};

	private final View.OnClickListener mEnqueueNetworkingListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			onClickEnqueueNetwork(v);
		}
	};
}
