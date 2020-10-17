package com.android.server.connectivity.ipmemorystore;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.Status;
import android.net.ipmemorystore.StatusParcelable;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public final class RegularMaintenanceJobService extends JobService {
    private static final ArrayList<InterruptMaintenance> sInterruptList = new ArrayList<>();
    private static IpMemoryStoreService sIpMemoryStoreService;

    public static final class InterruptMaintenance {
        private volatile boolean mIsInterrupted = false;
        private final int mJobId;

        public InterruptMaintenance(int i) {
            this.mJobId = i;
        }

        public int getJobId() {
            return this.mJobId;
        }

        public void setInterrupted(boolean z) {
            this.mIsInterrupted = z;
        }

        public boolean isInterrupted() {
            return this.mIsInterrupted;
        }
    }

    public boolean onStartJob(final JobParameters jobParameters) {
        if (sIpMemoryStoreService == null) {
            Log.wtf("RegularMaintenanceJobService", "Can not start job because sIpMemoryStoreService is null.");
            return false;
        }
        final InterruptMaintenance interruptMaintenance = new InterruptMaintenance(jobParameters.getJobId());
        sInterruptList.add(interruptMaintenance);
        sIpMemoryStoreService.fullMaintenance(new IOnStatusListener() {
            /* class com.android.server.connectivity.ipmemorystore.RegularMaintenanceJobService.C00281 */

            public IBinder asBinder() {
                return null;
            }

            @Override // android.net.ipmemorystore.IOnStatusListener
            public void onComplete(StatusParcelable statusParcelable) throws RemoteException {
                Status status = new Status(statusParcelable);
                if (!status.isSuccess()) {
                    Log.e("RegularMaintenanceJobService", "Regular maintenance failed. Error is " + status.resultCode);
                }
                RegularMaintenanceJobService.sInterruptList.remove(interruptMaintenance);
                RegularMaintenanceJobService.this.jobFinished(jobParameters, !status.isSuccess());
            }
        }, interruptMaintenance);
        return true;
    }

    public boolean onStopJob(JobParameters jobParameters) {
        int jobId = jobParameters.getJobId();
        Iterator<InterruptMaintenance> it = sInterruptList.iterator();
        while (it.hasNext()) {
            InterruptMaintenance next = it.next();
            if (next.getJobId() == jobId) {
                next.setInterrupted(true);
            }
        }
        return true;
    }

    static void schedule(Context context, IpMemoryStoreService ipMemoryStoreService) {
        ((JobScheduler) context.getSystemService("jobscheduler")).schedule(new JobInfo.Builder(3345678, new ComponentName(context, RegularMaintenanceJobService.class)).setRequiresDeviceIdle(true).setRequiresCharging(true).setRequiresBatteryNotLow(true).setPeriodic(TimeUnit.HOURS.toMillis(24)).build());
        sIpMemoryStoreService = ipMemoryStoreService;
    }
}
