package android.net.util;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.system.ErrnoException;
import android.system.OsConstants;
import java.io.FileDescriptor;
import java.io.IOException;

public abstract class FdEventsReader<BufferType> {
    private final BufferType mBuffer;
    private FileDescriptor mFd;
    private final Handler mHandler;
    private long mPacketsReceived;
    private final MessageQueue mQueue = this.mHandler.getLooper().getQueue();

    /* access modifiers changed from: protected */
    public abstract FileDescriptor createFd();

    /* access modifiers changed from: protected */
    public abstract void handlePacket(BufferType buffertype, int i);

    /* access modifiers changed from: protected */
    public void logError(String str, Exception exc) {
    }

    /* access modifiers changed from: protected */
    public void onStart() {
    }

    /* access modifiers changed from: protected */
    public void onStop() {
    }

    /* access modifiers changed from: protected */
    public abstract int readPacket(FileDescriptor fileDescriptor, BufferType buffertype) throws Exception;

    protected static void closeFd(FileDescriptor fileDescriptor) {
        try {
            SocketUtils.closeSocket(fileDescriptor);
        } catch (IOException unused) {
        }
    }

    protected FdEventsReader(Handler handler, BufferType buffertype) {
        this.mHandler = handler;
        this.mBuffer = buffertype;
    }

    public void start() {
        if (onCorrectThread()) {
            createAndRegisterFd();
        } else {
            this.mHandler.post(new Runnable() {
                /* class android.net.util.$$Lambda$FdEventsReader$d1xMVeCQOp74kUF5zuSvCSsnlc */

                public final void run() {
                    FdEventsReader.this.lambda$start$0$FdEventsReader();
                }
            });
        }
    }

    public /* synthetic */ void lambda$start$0$FdEventsReader() {
        logError("start() called from off-thread", null);
        createAndRegisterFd();
    }

    public void stop() {
        if (onCorrectThread()) {
            unregisterAndDestroyFd();
        } else {
            this.mHandler.post(new Runnable() {
                /* class android.net.util.$$Lambda$FdEventsReader$Zh3k3NT3RgrspRCrABxG2YVaLA8 */

                public final void run() {
                    FdEventsReader.this.lambda$stop$1$FdEventsReader();
                }
            });
        }
    }

    public /* synthetic */ void lambda$stop$1$FdEventsReader() {
        logError("stop() called from off-thread", null);
        unregisterAndDestroyFd();
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    private void createAndRegisterFd() {
        if (this.mFd == null) {
            try {
                this.mFd = createFd();
            } catch (Exception e) {
                logError("Failed to create socket: ", e);
                closeFd(this.mFd);
                this.mFd = null;
            }
            FileDescriptor fileDescriptor = this.mFd;
            if (fileDescriptor != null) {
                this.mQueue.addOnFileDescriptorEventListener(fileDescriptor, 5, new MessageQueue.OnFileDescriptorEventListener() {
                    /* class android.net.util.$$Lambda$FdEventsReader$MJGveJiu3TqatZaBXlmIyD8DwEE */

                    public final int onFileDescriptorEvents(FileDescriptor fileDescriptor, int i) {
                        return FdEventsReader.this.lambda$createAndRegisterFd$2$FdEventsReader(fileDescriptor, i);
                    }
                });
                onStart();
            }
        }
    }

    public /* synthetic */ int lambda$createAndRegisterFd$2$FdEventsReader(FileDescriptor fileDescriptor, int i) {
        if (isRunning() && handleInput()) {
            return 5;
        }
        unregisterAndDestroyFd();
        return 0;
    }

    private boolean isRunning() {
        FileDescriptor fileDescriptor = this.mFd;
        return fileDescriptor != null && fileDescriptor.valid();
    }

    private boolean handleInput() {
        while (isRunning()) {
            try {
                int readPacket = readPacket(this.mFd, this.mBuffer);
                if (readPacket >= 1) {
                    this.mPacketsReceived++;
                    try {
                        handlePacket(this.mBuffer, readPacket);
                    } catch (Exception e) {
                        logError("handlePacket error: ", e);
                        return false;
                    }
                } else if (!isRunning()) {
                    return false;
                } else {
                    logError("Socket closed, exiting", null);
                    return false;
                }
            } catch (ErrnoException e2) {
                int i = e2.errno;
                if (i == OsConstants.EAGAIN) {
                    return true;
                }
                if (i != OsConstants.EINTR) {
                    if (!isRunning()) {
                        return false;
                    }
                    logError("readPacket error: ", e2);
                    return false;
                }
            } catch (Exception e3) {
                if (!isRunning()) {
                    return false;
                }
                logError("readPacket error: ", e3);
                return false;
            }
        }
        return false;
    }

    private void unregisterAndDestroyFd() {
        FileDescriptor fileDescriptor = this.mFd;
        if (fileDescriptor != null) {
            this.mQueue.removeOnFileDescriptorEventListener(fileDescriptor);
            closeFd(this.mFd);
            this.mFd = null;
            onStop();
        }
    }

    private boolean onCorrectThread() {
        return this.mHandler.getLooper() == Looper.myLooper();
    }
}
