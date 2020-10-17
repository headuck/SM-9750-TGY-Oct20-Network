package android.net.p000ip;

import android.net.networkstack.util.HexDump;
import android.net.util.ConnectivityPacketSummary;
import android.net.util.FdEventsReader;
import android.net.util.InterfaceParams;
import android.net.util.NetworkStackUtils;
import android.net.util.PacketReader;
import android.net.util.SocketUtils;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;

/* renamed from: android.net.ip.ConnectivityPacketTracker */
public class ConnectivityPacketTracker {
    private static final String TAG = "ConnectivityPacketTracker";
    private String mDisplayName;
    private final LocalLog mLog;
    private final PacketReader mPacketListener;
    private boolean mRunning;
    private final String mTag;

    public ConnectivityPacketTracker(Handler handler, InterfaceParams interfaceParams, LocalLog localLog) {
        if (interfaceParams != null) {
            this.mTag = TAG + "." + interfaceParams.name;
            this.mLog = localLog;
            this.mPacketListener = new PacketListener(handler, interfaceParams);
            return;
        }
        throw new IllegalArgumentException("null InterfaceParams");
    }

    public void start(String str) {
        this.mRunning = true;
        this.mDisplayName = str;
        this.mPacketListener.start();
    }

    public void stop() {
        this.mPacketListener.stop();
        this.mRunning = false;
        this.mDisplayName = null;
    }

    /* renamed from: android.net.ip.ConnectivityPacketTracker$PacketListener */
    private final class PacketListener extends PacketReader {
        private final InterfaceParams mInterface;

        PacketListener(Handler handler, InterfaceParams interfaceParams) {
            super(handler, interfaceParams.defaultMtu);
            this.mInterface = interfaceParams;
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public FileDescriptor createFd() {
            FileDescriptor fileDescriptor;
            try {
                fileDescriptor = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW | OsConstants.SOCK_NONBLOCK, 0);
                try {
                    NetworkStackUtils.attachControlPacketFilter(fileDescriptor, OsConstants.ARPHRD_ETHER);
                    Os.bind(fileDescriptor, SocketUtils.makePacketSocketAddress((short) OsConstants.ETH_P_ALL, this.mInterface.index));
                    return fileDescriptor;
                } catch (ErrnoException | IOException e) {
                    e = e;
                    logError("Failed to create packet tracking socket: ", e);
                    FdEventsReader.closeFd(fileDescriptor);
                    return null;
                }
            } catch (ErrnoException | IOException e2) {
                e = e2;
                fileDescriptor = null;
                logError("Failed to create packet tracking socket: ", e);
                FdEventsReader.closeFd(fileDescriptor);
                return null;
            }
        }

        /* access modifiers changed from: protected */
        public void handlePacket(byte[] bArr, int i) {
            String summarize = ConnectivityPacketSummary.summarize(this.mInterface.macAddr, bArr, i);
            if (summarize != null) {
                addLogEntry(summarize + "\n[" + HexDump.toHexString(bArr, 0, i) + "]");
            }
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public void onStart() {
            String str;
            if (TextUtils.isEmpty(ConnectivityPacketTracker.this.mDisplayName)) {
                str = "--- START ---";
            } else {
                str = String.format("--- START (%s) ---", ConnectivityPacketTracker.this.mDisplayName);
            }
            ConnectivityPacketTracker.this.mLog.log(str);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public void onStop() {
            String str;
            if (TextUtils.isEmpty(ConnectivityPacketTracker.this.mDisplayName)) {
                str = "--- STOP ---";
            } else {
                str = String.format("--- STOP (%s) ---", ConnectivityPacketTracker.this.mDisplayName);
            }
            if (!ConnectivityPacketTracker.this.mRunning) {
                str = str + " (packet listener stopped unexpectedly)";
            }
            ConnectivityPacketTracker.this.mLog.log(str);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public void logError(String str, Exception exc) {
            Log.e(ConnectivityPacketTracker.this.mTag, str, exc);
            addLogEntry(str + exc);
        }

        private void addLogEntry(String str) {
            ConnectivityPacketTracker.this.mLog.log(str);
        }
    }
}
