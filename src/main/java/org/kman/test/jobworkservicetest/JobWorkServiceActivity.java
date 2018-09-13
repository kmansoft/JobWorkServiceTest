package org.kman.test.jobworkservicetest;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobWorkItem;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
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

		button = findViewById(R.id.enqueue3);
		button.setOnClickListener(mEnqueue3Listener);

		button = findViewById(R.id.kill);
		button.setOnClickListener(mKillListener);
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
	private View.OnClickListener mEnqueue3Listener = new View.OnClickListener() {
		public void onClick(View v) {
			mJobScheduler.enqueue(mJobInfo, new JobWorkItem(new Intent("com.example.android.apis.THREE").putExtra
					("name", "Three")));
		}
	};
	private View.OnClickListener mKillListener = new View.OnClickListener() {
		public void onClick(View v) {
			// This is to simulate the service being killed while it is
			// running in the background.
			Process.killProcess(Process.myPid());
		}
	};
}
