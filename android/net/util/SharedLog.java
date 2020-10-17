package android.net.util;

import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.StringJoiner;

public class SharedLog {
    private final String mComponent;
    private final LocalLog mLocalLog;
    private final String mTag;

    /* access modifiers changed from: private */
    public enum Category {
        NONE,
        ERROR,
        MARK,
        WARN
    }

    public SharedLog(String str) {
        this(500, str);
    }

    public SharedLog(int i, String str) {
        this(new LocalLog(i), str, str);
    }

    private SharedLog(LocalLog localLog, String str, String str2) {
        this.mLocalLog = localLog;
        this.mTag = str;
        this.mComponent = str2;
    }

    public String getTag() {
        return this.mTag;
    }

    public SharedLog forSubComponent(String str) {
        if (!isRootLogInstance()) {
            str = this.mComponent + "." + str;
        }
        return new SharedLog(this.mLocalLog, this.mTag, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        this.mLocalLog.readOnlyLocalLog().dump(fileDescriptor, printWriter, strArr);
    }

    /* renamed from: e */
    public void mo563e(String str) {
        Log.e(this.mTag, record(Category.ERROR, str));
    }

    /* renamed from: e */
    public void mo564e(String str, Throwable th) {
        if (th == null) {
            mo563e(str);
            return;
        }
        String str2 = this.mTag;
        Category category = Category.ERROR;
        Log.e(str2, record(category, str + ": " + th.getMessage()), th);
    }

    /* renamed from: i */
    public void mo567i(String str) {
        Log.i(this.mTag, record(Category.NONE, str));
    }

    /* renamed from: w */
    public void mo570w(String str) {
        Log.w(this.mTag, record(Category.WARN, str));
    }

    public void log(String str) {
        record(Category.NONE, str);
    }

    public void logf(String str, Object... objArr) {
        log(String.format(str, objArr));
    }

    private String record(Category category, String str) {
        String logLine = logLine(category, str);
        this.mLocalLog.log(logLine);
        return logLine;
    }

    private String logLine(Category category, String str) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!isRootLogInstance()) {
            stringJoiner.add("[" + this.mComponent + "]");
        }
        if (category != Category.NONE) {
            stringJoiner.add(category.toString());
        }
        return stringJoiner.add(str).toString();
    }

    private boolean isRootLogInstance() {
        return TextUtils.isEmpty(this.mComponent) || this.mComponent.equals(this.mTag);
    }
}
