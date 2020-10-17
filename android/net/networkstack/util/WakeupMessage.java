package android.net.networkstack.util;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class WakeupMessage implements AlarmManager.OnAlarmListener {
    private final AlarmManager mAlarmManager;
    protected final int mArg1;
    protected final int mArg2;
    protected final int mCmd;
    protected final String mCmdName;
    protected final Handler mHandler;
    protected final Object mObj;
    private final Runnable mRunnable;
    private boolean mScheduled;

    public WakeupMessage(Context context, Handler handler, String str, int i, int i2, int i3, Object obj) {
        this.mAlarmManager = getAlarmManager(context);
        this.mHandler = handler;
        this.mCmdName = str;
        this.mCmd = i;
        this.mArg1 = i2;
        this.mArg2 = i3;
        this.mObj = obj;
        this.mRunnable = null;
    }

    public WakeupMessage(Context context, Handler handler, String str, int i) {
        this(context, handler, str, i, 0, 0, null);
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService("alarm");
    }

    public synchronized void schedule(long j) {
        this.mAlarmManager.setExact(2, j, this.mCmdName, this, this.mHandler);
        this.mScheduled = true;
    }

    public synchronized void cancel() {
        if (this.mScheduled) {
            this.mAlarmManager.cancel(this);
            this.mScheduled = false;
        }
    }

    public void onAlarm() {
        boolean z;
        Message message;
        synchronized (this) {
            z = this.mScheduled;
            this.mScheduled = false;
        }
        if (z) {
            Runnable runnable = this.mRunnable;
            if (runnable == null) {
                message = this.mHandler.obtainMessage(this.mCmd, this.mArg1, this.mArg2, this.mObj);
            } else {
                message = Message.obtain(this.mHandler, runnable);
            }
            this.mHandler.dispatchMessage(message);
            message.recycle();
        }
    }
}
