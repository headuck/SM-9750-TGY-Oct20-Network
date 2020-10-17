package android.util;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

public final class LocalLog {
    private final Deque<String> mLog = new ArrayDeque(this.mMaxLines);
    private final int mMaxLines;

    public LocalLog(int i) {
        this.mMaxLines = Math.max(0, i);
    }

    public void log(String str) {
        if (this.mMaxLines > 0) {
            append(String.format("%s - %s", LocalDateTime.now(), str));
        }
    }

    private synchronized void append(String str) {
        while (this.mLog.size() >= this.mMaxLines) {
            this.mLog.remove();
        }
        this.mLog.add(str);
    }

    public synchronized void dump(PrintWriter printWriter) {
        for (String str : this.mLog) {
            printWriter.println(str);
        }
    }

    public static class ReadOnlyLocalLog {
        private final LocalLog mLog;

        ReadOnlyLocalLog(LocalLog localLog) {
            this.mLog = localLog;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            this.mLog.dump(printWriter);
        }
    }

    public ReadOnlyLocalLog readOnlyLocalLog() {
        return new ReadOnlyLocalLog(this);
    }
}
