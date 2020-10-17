package android.net.p000ip;

import android.net.MacAddress;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkErrorMessage;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkNeighborMessage;
import android.net.netlink.StructNdMsg;
import android.net.util.NetworkStackUtils;
import android.net.util.PacketReader;
import android.net.util.SharedLog;
import android.net.util.SocketUtils;
import android.os.Handler;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;

/* renamed from: android.net.ip.IpNeighborMonitor */
public class IpNeighborMonitor extends PacketReader {
    private static final String TAG = "IpNeighborMonitor";
    private final NeighborEventConsumer mConsumer;
    private final SharedLog mLog;

    /* renamed from: android.net.ip.IpNeighborMonitor$NeighborEventConsumer */
    public interface NeighborEventConsumer {
        void accept(NeighborEvent neighborEvent);
    }

    public static int startKernelNeighborProbe(int i, InetAddress inetAddress) {
        String str = "probing ip=" + inetAddress.getHostAddress() + "%" + i;
        try {
            NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_ROUTE, RtNetlinkNeighborMessage.newNewNeighborMessage(1, inetAddress, 16, i, null));
            return 0;
        } catch (ErrnoException e) {
            Log.e(TAG, "Error " + str + ": " + e);
            return -e.errno;
        }
    }

    /* renamed from: android.net.ip.IpNeighborMonitor$NeighborEvent */
    public static class NeighborEvent {
        final long elapsedMs;
        final int ifindex;

        /* renamed from: ip */
        final InetAddress f5ip;
        final MacAddress macAddr;
        final short msgType;
        final short nudState;

        public NeighborEvent(long j, short s, int i, InetAddress inetAddress, short s2, MacAddress macAddress) {
            this.elapsedMs = j;
            this.msgType = s;
            this.ifindex = i;
            this.f5ip = inetAddress;
            this.nudState = s2;
            this.macAddr = macAddress;
        }

        public String toString() {
            StringJoiner stringJoiner = new StringJoiner(",", "NeighborEvent{", "}");
            StringJoiner add = stringJoiner.add("@" + this.elapsedMs).add(NetlinkConstants.stringForNlMsgType(this.msgType));
            StringJoiner add2 = add.add("if=" + this.ifindex).add(this.f5ip.getHostAddress()).add(StructNdMsg.stringForNudState(this.nudState));
            return add2.add("[" + this.macAddr + "]").toString();
        }
    }

    public IpNeighborMonitor(Handler handler, SharedLog sharedLog, NeighborEventConsumer neighborEventConsumer) {
        super(handler, 8192);
        this.mLog = sharedLog.forSubComponent(TAG);
        this.mConsumer = neighborEventConsumer == null ? $$Lambda$IpNeighborMonitor$4TdKAwtCtq9Ri1cSdW1mKm0JycM.INSTANCE : neighborEventConsumer;
    }

    /* access modifiers changed from: protected */
    @Override // android.net.util.FdEventsReader
    public FileDescriptor createFd() {
        FileDescriptor fileDescriptor;
        try {
            fileDescriptor = Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM | OsConstants.SOCK_NONBLOCK, OsConstants.NETLINK_ROUTE);
            try {
                Os.bind(fileDescriptor, SocketUtils.makeNetlinkSocketAddress(0, OsConstants.RTMGRP_NEIGH));
                NetlinkSocket.connectToKernel(fileDescriptor);
                return fileDescriptor;
            } catch (ErrnoException | SocketException e) {
                e = e;
                logError("Failed to create rtnetlink socket", e);
                NetworkStackUtils.closeSocketQuietly(fileDescriptor);
                return null;
            }
        } catch (ErrnoException | SocketException e2) {
            e = e2;
            fileDescriptor = null;
            logError("Failed to create rtnetlink socket", e);
            NetworkStackUtils.closeSocketQuietly(fileDescriptor);
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public void handlePacket(byte[] bArr, int i) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        ByteBuffer wrap = ByteBuffer.wrap(bArr, 0, i);
        wrap.order(ByteOrder.nativeOrder());
        parseNetlinkMessageBuffer(wrap, elapsedRealtime);
    }

    private void parseNetlinkMessageBuffer(ByteBuffer byteBuffer, long j) {
        while (byteBuffer.remaining() > 0) {
            int position = byteBuffer.position();
            NetlinkMessage parse = NetlinkMessage.parse(byteBuffer);
            if (parse == null || parse.getHeader() == null) {
                byteBuffer.position(position);
                SharedLog sharedLog = this.mLog;
                sharedLog.mo563e("unparsable netlink msg: " + NetlinkConstants.hexify(byteBuffer));
                return;
            }
            int i = parse.getHeader().nlmsg_pid;
            if (i != 0) {
                SharedLog sharedLog2 = this.mLog;
                sharedLog2.mo563e("non-kernel source portId: " + Integer.toUnsignedLong(i));
                return;
            } else if (parse instanceof NetlinkErrorMessage) {
                SharedLog sharedLog3 = this.mLog;
                sharedLog3.mo563e("netlink error: " + parse);
            } else if (!(parse instanceof RtNetlinkNeighborMessage)) {
                SharedLog sharedLog4 = this.mLog;
                sharedLog4.mo567i("non-rtnetlink neighbor msg: " + parse);
            } else {
                evaluateRtNetlinkNeighborMessage((RtNetlinkNeighborMessage) parse, j);
            }
        }
    }

    private void evaluateRtNetlinkNeighborMessage(RtNetlinkNeighborMessage rtNetlinkNeighborMessage, long j) {
        short s;
        short s2 = rtNetlinkNeighborMessage.getHeader().nlmsg_type;
        StructNdMsg ndHeader = rtNetlinkNeighborMessage.getNdHeader();
        if (ndHeader == null) {
            this.mLog.mo563e("RtNetlinkNeighborMessage without ND message header!");
            return;
        }
        int i = ndHeader.ndm_ifindex;
        InetAddress destination = rtNetlinkNeighborMessage.getDestination();
        if (s2 == 29) {
            s = 0;
        } else {
            s = ndHeader.ndm_state;
        }
        this.mConsumer.accept(new NeighborEvent(j, s2, i, destination, s, getMacAddress(rtNetlinkNeighborMessage.getLinkLayerAddress())));
    }

    private static MacAddress getMacAddress(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        try {
            return MacAddress.fromBytes(bArr);
        } catch (IllegalArgumentException unused) {
            String str = TAG;
            Log.e(str, "Failed to parse link-layer address: " + NetlinkConstants.hexify(bArr));
            return null;
        }
    }
}
