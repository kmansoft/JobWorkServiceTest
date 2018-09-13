# JobWorkServiceTest
A test project for a bug in JobService / JobWorkItem

Please see JobWorkService.java and make sure that DEQUEUE_ALL_THEN_PROCESS = true (near the top).

- Run the app
- Click on Enqueue One
- Wait for notification that the job service is processing a work item
- adb shell dumpsys jobscheduler -> you'll see running Service / Job / Workitem
- Wait for notification to go away (work item is done processing)
- adb shell dumpsys jobscheduler -> you'll see **stuck** Service
- adb shell dumpsys power -> you'll see **stuck** wake lock

The documentation says:

https://developer.android.com/reference/android/app/job/JobParameters#dequeueWork()

```
You do not, however, have to complete each returned work item before deqeueing the next one -- you can use
dequeueWork() multiple times before completing previous work if you want to process work in parallel, and you
can complete the work in whatever order you want.
```

And so we do - dequeue all work items and submit them as runnables to an executor.

However:

This makes it so that the service is never stopped and the job is never finished.

1 - While a Job / JobWorkItem is running:

```
Active jobs:
  Slot #0: 4f3a189 #u0a88/1 org.kman.test.jobworkservicetest/.JobWorkService
    Running for: +3s741ms, timeout at: +9m56s293ms
    u0a88 tag=*job*/org.kman.test.jobworkservicetest/.JobWorkService
    Source: uid=u0a88 user=0 pkg=org.kman.test.jobworkservicetest
    Required constraints: DEADLINE
    Executing work:
      #0: #1 1x Intent { act=com.example.android.apis.ONE (has extras) }
    Enqueue time: -3s741ms
    Run time: earliest=none, latest=-3s741ms
    Evaluated priority: 40
    Active at -3s741ms, pending for 0
```

After we've called params.completeWork(item):

1 - the Job stays running

```
Active jobs:
  Slot #0: 4f3a189 #u0a88/1 org.kman.test.jobworkservicetest/.JobWorkService
    Running for: +16s998ms, timeout at: +9m43s36ms
    u0a88 tag=*job*/org.kman.test.jobworkservicetest/.JobWorkService
    Source: uid=u0a88 user=0 pkg=org.kman.test.jobworkservicetest
    Required constraints: DEADLINE
    Enqueue time: -16s998ms
    Run time: earliest=none, latest=-16s998ms
    Evaluated priority: 40
    Active at -16s998ms, pending for 0
```

Note that there are no work items in this output, the service is running "on its own", the "old way", before
JobWorkItems existed (pre-8.0).

2 - The wake lock associated with the job is still held, by Android:

```
Wake Locks: size=1
  PARTIAL_WAKE_LOCK              '*job*/org.kman.test.jobworkservicetest/.JobWorkService' ACQ=-31s208ms (uid=1000 pid=1872 ws=WorkSource{10088})
```

Presumably this will time out when the currently "running" stuck job is told to stop - which takes 10 minutes.
A hell of a battery drain.

3 - More JobWorkItem's can be enqueue'd but...

The service never receives them (at least not until the currently "running" stuck job finishes).

```
  Slot #0: 4f3a189 #u0a88/1 org.kman.test.jobworkservicetest/.JobWorkService
    Running for: +1m33s528ms, timeout at: +8m26s506ms
    u0a88 tag=*job*/org.kman.test.jobworkservicetest/.JobWorkService
    Source: uid=u0a88 user=0 pkg=org.kman.test.jobworkservicetest
    Required constraints: DEADLINE
    Pending work:
      #0: #2 0x Intent { act=com.example.android.apis.ONE (has extras) }
      #1: #3 0x Intent { act=com.example.android.apis.ONE (has extras) }
    Enqueue time: -1m33s528ms
    Run time: earliest=none, latest=-1m33s528ms
    Evaluated priority: 40
    Active at -1m33s528ms, pending for 0
```
