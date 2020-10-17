package android.net.dhcp;

import android.net.INetworkStackStatusCallback;
import android.net.MacAddress;
import android.net.TrafficStats;
import android.net.dhcp.DhcpLeaseRepository;
import android.net.dhcp.DhcpPacket;
import android.net.dhcp.DhcpServingParams;
import android.net.dhcp.IDhcpServer;
import android.net.networkstack.shared.Inet4AddressUtils;
import android.net.networkstack.util.HexDump;
import android.net.util.NetworkStackUtils;
import android.net.util.SharedLog;
import android.net.util.SocketUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Pair;
import com.android.server.util.NetworkStackConstants;
import com.android.server.util.PermissionUtil;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DhcpServer extends IDhcpServer.Stub {
    private final Clock mClock;
    private final Dependencies mDeps;
    private volatile ServerHandler mHandler;
    private final HandlerThread mHandlerThread;
    private final String mIfName;
    private final DhcpLeaseRepository mLeaseRepo;
    private final SharedLog mLog;
    private DhcpPacketListener mPacketListener;
    private DhcpServingParams mServingParams;
    private FileDescriptor mSocket;

    public interface Dependencies {
        void addArpEntry(Inet4Address inet4Address, MacAddress macAddress, String str, FileDescriptor fileDescriptor) throws IOException;

        void checkCaller() throws SecurityException;

        Clock makeClock();

        DhcpLeaseRepository makeLeaseRepository(DhcpServingParams dhcpServingParams, SharedLog sharedLog, Clock clock);

        DhcpPacketListener makePacketListener();

        void sendPacket(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, InetAddress inetAddress) throws ErrnoException, IOException;
    }

    @Override // android.net.dhcp.IDhcpServer
    public int getInterfaceVersion() {
        return 3;
    }

    public static class Clock {
        public long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }
    }

    private class DependenciesImpl implements Dependencies {
        private DependenciesImpl() {
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public void sendPacket(FileDescriptor fileDescriptor, ByteBuffer byteBuffer, InetAddress inetAddress) throws ErrnoException, IOException {
            Os.sendto(fileDescriptor, byteBuffer, 0, inetAddress, 68);
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public DhcpLeaseRepository makeLeaseRepository(DhcpServingParams dhcpServingParams, SharedLog sharedLog, Clock clock) {
            return new DhcpLeaseRepository(DhcpServingParams.makeIpPrefix(dhcpServingParams.serverAddr), dhcpServingParams.excludedAddrs, dhcpServingParams.dhcpLeaseTimeSecs * 1000, sharedLog.forSubComponent("Repository"), clock);
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public DhcpPacketListener makePacketListener() {
            return new PacketListener();
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public Clock makeClock() {
            return new Clock();
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public void addArpEntry(Inet4Address inet4Address, MacAddress macAddress, String str, FileDescriptor fileDescriptor) throws IOException {
            NetworkStackUtils.addArpEntry(inet4Address, macAddress, str, fileDescriptor);
        }

        @Override // android.net.dhcp.DhcpServer.Dependencies
        public void checkCaller() {
            PermissionUtil.checkNetworkStackCallingPermission();
        }
    }

    /* access modifiers changed from: private */
    public static class MalformedPacketException extends Exception {
        MalformedPacketException(String str, Throwable th) {
            super(str, th);
        }
    }

    public DhcpServer(String str, DhcpServingParams dhcpServingParams, SharedLog sharedLog) {
        this(new HandlerThread(DhcpServer.class.getSimpleName() + "." + str), str, dhcpServingParams, sharedLog, null);
    }

    DhcpServer(HandlerThread handlerThread, String str, DhcpServingParams dhcpServingParams, SharedLog sharedLog, Dependencies dependencies) {
        dependencies = dependencies == null ? new DependenciesImpl() : dependencies;
        this.mHandlerThread = handlerThread;
        this.mIfName = str;
        this.mServingParams = dhcpServingParams;
        this.mLog = sharedLog;
        this.mDeps = dependencies;
        this.mClock = dependencies.makeClock();
        this.mLeaseRepo = dependencies.makeLeaseRepository(this.mServingParams, this.mLog, this.mClock);
    }

    @Override // android.net.dhcp.IDhcpServer
    public void start(INetworkStackStatusCallback iNetworkStackStatusCallback) {
        this.mDeps.checkCaller();
        this.mHandlerThread.start();
        this.mHandler = new ServerHandler(this.mHandlerThread.getLooper());
        sendMessage(1, iNetworkStackStatusCallback);
    }

    @Override // android.net.dhcp.IDhcpServer
    public void updateParams(DhcpServingParamsParcel dhcpServingParamsParcel, INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException {
        this.mDeps.checkCaller();
        try {
            sendMessage(3, new Pair(DhcpServingParams.fromParcelableObject(dhcpServingParamsParcel), iNetworkStackStatusCallback));
        } catch (DhcpServingParams.InvalidParameterException e) {
            this.mLog.mo564e("Invalid parameters sent to DhcpServer", e);
            if (iNetworkStackStatusCallback != null) {
                iNetworkStackStatusCallback.onStatusAvailable(2);
            }
        }
    }

    @Override // android.net.dhcp.IDhcpServer
    public void stop(INetworkStackStatusCallback iNetworkStackStatusCallback) {
        this.mDeps.checkCaller();
        sendMessage(2, iNetworkStackStatusCallback);
    }

    private void sendMessage(int i, Object obj) {
        if (this.mHandler == null) {
            SharedLog sharedLog = this.mLog;
            sharedLog.mo563e("Attempting to send a command to stopped DhcpServer: " + i);
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(i, obj));
    }

    /* access modifiers changed from: private */
    public class ServerHandler extends Handler {
        ServerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message message) {
            INetworkStackStatusCallback iNetworkStackStatusCallback;
            int i = message.what;
            if (i == 1) {
                DhcpServer dhcpServer = DhcpServer.this;
                dhcpServer.mPacketListener = dhcpServer.mDeps.makePacketListener();
                DhcpServer.this.mPacketListener.start();
                iNetworkStackStatusCallback = (INetworkStackStatusCallback) message.obj;
            } else if (i == 2) {
                if (DhcpServer.this.mPacketListener != null) {
                    DhcpServer.this.mPacketListener.stop();
                    DhcpServer.this.mPacketListener = null;
                }
                DhcpServer.this.mHandlerThread.quitSafely();
                iNetworkStackStatusCallback = (INetworkStackStatusCallback) message.obj;
            } else if (i == 3) {
                Pair pair = (Pair) message.obj;
                DhcpServingParams dhcpServingParams = (DhcpServingParams) pair.first;
                DhcpServer.this.mServingParams = dhcpServingParams;
                DhcpServer.this.mLeaseRepo.updateParams(DhcpServingParams.makeIpPrefix(DhcpServer.this.mServingParams.serverAddr), dhcpServingParams.excludedAddrs, dhcpServingParams.dhcpLeaseTimeSecs);
                iNetworkStackStatusCallback = (INetworkStackStatusCallback) pair.second;
            } else {
                return;
            }
            if (iNetworkStackStatusCallback != null) {
                try {
                    iNetworkStackStatusCallback.onStatusAvailable(1);
                } catch (RemoteException e) {
                    DhcpServer.this.mLog.mo564e("Could not send status back to caller", e);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void processPacket(DhcpPacket dhcpPacket, int i) {
        String simpleName = dhcpPacket.getClass().getSimpleName();
        if (i != 68) {
            this.mLog.logf("Ignored packet of type %s sent from client port %d", simpleName, Integer.valueOf(i));
            return;
        }
        SharedLog sharedLog = this.mLog;
        sharedLog.log("Received packet of type " + simpleName);
        Inet4Address inet4Address = dhcpPacket.mServerIdentifier;
        if (inet4Address == null || inet4Address.equals(this.mServingParams.serverAddr.getAddress())) {
            try {
                if (dhcpPacket instanceof DhcpDiscoverPacket) {
                    processDiscover((DhcpDiscoverPacket) dhcpPacket);
                } else if (dhcpPacket instanceof DhcpRequestPacket) {
                    processRequest((DhcpRequestPacket) dhcpPacket);
                } else if (dhcpPacket instanceof DhcpReleasePacket) {
                    processRelease((DhcpReleasePacket) dhcpPacket);
                } else {
                    SharedLog sharedLog2 = this.mLog;
                    sharedLog2.mo563e("Unknown packet type: " + dhcpPacket.getClass().getSimpleName());
                }
            } catch (MalformedPacketException e) {
                SharedLog sharedLog3 = this.mLog;
                sharedLog3.mo563e("Ignored malformed packet: " + e.getMessage());
            }
        } else {
            SharedLog sharedLog4 = this.mLog;
            sharedLog4.log("Packet ignored due to wrong server identifier: " + inet4Address);
        }
    }

    private void logIgnoredPacketInvalidSubnet(DhcpLeaseRepository.InvalidSubnetException invalidSubnetException) {
        SharedLog sharedLog = this.mLog;
        sharedLog.mo563e("Ignored packet from invalid subnet: " + invalidSubnetException.getMessage());
    }

    private void processDiscover(DhcpDiscoverPacket dhcpDiscoverPacket) throws MalformedPacketException {
        MacAddress macAddr = getMacAddr(dhcpDiscoverPacket);
        try {
            transmitOffer(dhcpDiscoverPacket, this.mLeaseRepo.getOffer(dhcpDiscoverPacket.getExplicitClientIdOrNull(), macAddr, dhcpDiscoverPacket.mRelayIp, dhcpDiscoverPacket.mRequestedIp, dhcpDiscoverPacket.mHostName), macAddr);
        } catch (DhcpLeaseRepository.OutOfAddressesException unused) {
            transmitNak(dhcpDiscoverPacket, "Out of addresses to offer");
        } catch (DhcpLeaseRepository.InvalidSubnetException e) {
            logIgnoredPacketInvalidSubnet(e);
        }
    }

    private void processRequest(DhcpRequestPacket dhcpRequestPacket) throws MalformedPacketException {
        boolean z = dhcpRequestPacket.mServerIdentifier != null;
        MacAddress macAddr = getMacAddr(dhcpRequestPacket);
        try {
            transmitAck(dhcpRequestPacket, this.mLeaseRepo.requestLease(dhcpRequestPacket.getExplicitClientIdOrNull(), macAddr, dhcpRequestPacket.mClientIp, dhcpRequestPacket.mRelayIp, dhcpRequestPacket.mRequestedIp, z, dhcpRequestPacket.mHostName), macAddr);
        } catch (DhcpLeaseRepository.InvalidAddressException unused) {
            transmitNak(dhcpRequestPacket, "Invalid requested address");
        } catch (DhcpLeaseRepository.InvalidSubnetException e) {
            logIgnoredPacketInvalidSubnet(e);
        }
    }

    private void processRelease(DhcpReleasePacket dhcpReleasePacket) throws MalformedPacketException {
        this.mLeaseRepo.releaseLease(dhcpReleasePacket.getExplicitClientIdOrNull(), getMacAddr(dhcpReleasePacket), dhcpReleasePacket.mClientIp);
    }

    private Inet4Address getAckOrOfferDst(DhcpPacket dhcpPacket, DhcpLease dhcpLease, boolean z) {
        if (!isEmpty(dhcpPacket.mRelayIp)) {
            return dhcpPacket.mRelayIp;
        }
        if (z) {
            return NetworkStackConstants.IPV4_ADDR_ALL;
        }
        if (!isEmpty(dhcpPacket.mClientIp)) {
            return dhcpPacket.mClientIp;
        }
        return dhcpLease.getNetAddr();
    }

    private static boolean getBroadcastFlag(DhcpPacket dhcpPacket, DhcpLease dhcpLease) {
        return isEmpty(dhcpPacket.mClientIp) && (dhcpPacket.mBroadcast || isEmpty(dhcpLease.getNetAddr()));
    }

    private static String getHostnameIfRequested(DhcpPacket dhcpPacket, DhcpLease dhcpLease) {
        if (!dhcpPacket.hasRequestedParam((byte) 12) || TextUtils.isEmpty(dhcpLease.getHostname())) {
            return null;
        }
        return dhcpLease.getHostname();
    }

    private boolean transmitOffer(DhcpPacket dhcpPacket, DhcpLease dhcpLease, MacAddress macAddress) {
        boolean broadcastFlag = getBroadcastFlag(dhcpPacket, dhcpLease);
        int leaseTimeout = getLeaseTimeout(dhcpLease);
        Inet4Address prefixMaskAsInet4Address = Inet4AddressUtils.getPrefixMaskAsInet4Address(this.mServingParams.serverAddr.getPrefixLength());
        Inet4Address broadcastAddress = Inet4AddressUtils.getBroadcastAddress(this.mServingParams.getServerInet4Addr(), this.mServingParams.serverAddr.getPrefixLength());
        String hostnameIfRequested = getHostnameIfRequested(dhcpPacket, dhcpLease);
        int i = dhcpPacket.mTransId;
        Inet4Address serverInet4Addr = this.mServingParams.getServerInet4Addr();
        Inet4Address inet4Address = dhcpPacket.mRelayIp;
        Inet4Address netAddr = dhcpLease.getNetAddr();
        byte[] bArr = dhcpPacket.mClientMac;
        Integer valueOf = Integer.valueOf(leaseTimeout);
        ArrayList arrayList = new ArrayList(this.mServingParams.defaultRouters);
        ArrayList arrayList2 = new ArrayList(this.mServingParams.dnsServers);
        Inet4Address serverInet4Addr2 = this.mServingParams.getServerInet4Addr();
        DhcpServingParams dhcpServingParams = this.mServingParams;
        return transmitOfferOrAckPacket(DhcpPacket.buildOfferPacket(2, i, broadcastFlag, serverInet4Addr, inet4Address, netAddr, bArr, valueOf, prefixMaskAsInet4Address, broadcastAddress, arrayList, arrayList2, serverInet4Addr2, null, hostnameIfRequested, dhcpServingParams.metered, (short) dhcpServingParams.linkMtu), dhcpPacket, dhcpLease, macAddress, broadcastFlag);
    }

    private boolean transmitAck(DhcpPacket dhcpPacket, DhcpLease dhcpLease, MacAddress macAddress) {
        boolean broadcastFlag = getBroadcastFlag(dhcpPacket, dhcpLease);
        int leaseTimeout = getLeaseTimeout(dhcpLease);
        String hostnameIfRequested = getHostnameIfRequested(dhcpPacket, dhcpLease);
        int i = dhcpPacket.mTransId;
        Inet4Address serverInet4Addr = this.mServingParams.getServerInet4Addr();
        Inet4Address inet4Address = dhcpPacket.mRelayIp;
        Inet4Address netAddr = dhcpLease.getNetAddr();
        Inet4Address inet4Address2 = dhcpPacket.mClientIp;
        byte[] bArr = dhcpPacket.mClientMac;
        Integer valueOf = Integer.valueOf(leaseTimeout);
        Inet4Address prefixMaskAsAddress = this.mServingParams.getPrefixMaskAsAddress();
        Inet4Address broadcastAddress = this.mServingParams.getBroadcastAddress();
        ArrayList arrayList = new ArrayList(this.mServingParams.defaultRouters);
        ArrayList arrayList2 = new ArrayList(this.mServingParams.dnsServers);
        Inet4Address serverInet4Addr2 = this.mServingParams.getServerInet4Addr();
        DhcpServingParams dhcpServingParams = this.mServingParams;
        return transmitOfferOrAckPacket(DhcpPacket.buildAckPacket(2, i, broadcastFlag, serverInet4Addr, inet4Address, netAddr, inet4Address2, bArr, valueOf, prefixMaskAsAddress, broadcastAddress, arrayList, arrayList2, serverInet4Addr2, null, hostnameIfRequested, dhcpServingParams.metered, (short) dhcpServingParams.linkMtu), dhcpPacket, dhcpLease, macAddress, broadcastFlag);
    }

    private boolean transmitNak(DhcpPacket dhcpPacket, String str) {
        Inet4Address inet4Address;
        SharedLog sharedLog = this.mLog;
        sharedLog.mo570w("Transmitting NAK: " + str);
        ByteBuffer buildNakPacket = DhcpPacket.buildNakPacket(2, dhcpPacket.mTransId, this.mServingParams.getServerInet4Addr(), dhcpPacket.mRelayIp, dhcpPacket.mClientMac, true, str);
        if (isEmpty(dhcpPacket.mRelayIp)) {
            inet4Address = NetworkStackConstants.IPV4_ADDR_ALL;
        } else {
            inet4Address = dhcpPacket.mRelayIp;
        }
        return transmitPacket(buildNakPacket, DhcpNakPacket.class.getSimpleName(), inet4Address);
    }

    private boolean transmitOfferOrAckPacket(ByteBuffer byteBuffer, DhcpPacket dhcpPacket, DhcpLease dhcpLease, MacAddress macAddress, boolean z) {
        this.mLog.logf("Transmitting %s with lease %s", dhcpPacket.getClass().getSimpleName(), dhcpLease);
        if (!addArpEntry(macAddress, dhcpLease.getNetAddr())) {
            return false;
        }
        return transmitPacket(byteBuffer, dhcpPacket.getClass().getSimpleName(), getAckOrOfferDst(dhcpPacket, dhcpLease, z));
    }

    private boolean transmitPacket(ByteBuffer byteBuffer, String str, Inet4Address inet4Address) {
        try {
            this.mDeps.sendPacket(this.mSocket, byteBuffer, inet4Address);
            return true;
        } catch (ErrnoException | IOException e) {
            SharedLog sharedLog = this.mLog;
            sharedLog.mo564e("Can't send packet " + str, e);
            return false;
        }
    }

    private boolean addArpEntry(MacAddress macAddress, Inet4Address inet4Address) {
        try {
            this.mDeps.addArpEntry(inet4Address, macAddress, this.mIfName, this.mSocket);
            return true;
        } catch (IOException e) {
            this.mLog.mo564e("Error adding client to ARP table", e);
            return false;
        }
    }

    private int getLeaseTimeout(DhcpLease dhcpLease) {
        long expTime = (dhcpLease.getExpTime() - this.mClock.elapsedRealtime()) / 1000;
        if (expTime < 0) {
            SharedLog sharedLog = this.mLog;
            sharedLog.mo563e("Processing expired lease " + dhcpLease);
            return 120;
        } else if (expTime >= Integer.toUnsignedLong(-1)) {
            return -1;
        } else {
            return (int) expTime;
        }
    }

    private MacAddress getMacAddr(DhcpPacket dhcpPacket) throws MalformedPacketException {
        try {
            return MacAddress.fromBytes(dhcpPacket.getClientMac());
        } catch (IllegalArgumentException e) {
            throw new MalformedPacketException("Invalid MAC address in packet: " + HexDump.dumpHexString(dhcpPacket.getClientMac()), e);
        }
    }

    private static boolean isEmpty(Inet4Address inet4Address) {
        return inet4Address == null || NetworkStackConstants.IPV4_ADDR_ANY.equals(inet4Address);
    }

    private class PacketListener extends DhcpPacketListener {
        PacketListener() {
            super(DhcpServer.this.mHandler);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpPacketListener
        public void onReceive(DhcpPacket dhcpPacket, Inet4Address inet4Address, int i) {
            DhcpServer.this.processPacket(dhcpPacket, i);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public void logError(String str, Exception exc) {
            SharedLog sharedLog = DhcpServer.this.mLog;
            sharedLog.mo564e("Error receiving packet: " + str, exc);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpPacketListener
        public void logParseError(byte[] bArr, int i, DhcpPacket.ParseException parseException) {
            DhcpServer.this.mLog.mo564e("Error parsing packet", parseException);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.util.FdEventsReader
        public FileDescriptor createFd() {
            int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-509);
            try {
                DhcpServer.this.mSocket = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM | OsConstants.SOCK_NONBLOCK, OsConstants.IPPROTO_UDP);
                SocketUtils.bindSocketToInterface(DhcpServer.this.mSocket, DhcpServer.this.mIfName);
                Os.setsockoptInt(DhcpServer.this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, 1);
                Os.setsockoptInt(DhcpServer.this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, 1);
                Os.bind(DhcpServer.this.mSocket, NetworkStackConstants.IPV4_ADDR_ANY, 67);
                return DhcpServer.this.mSocket;
            } catch (ErrnoException | IOException e) {
                DhcpServer.this.mLog.mo564e("Error creating UDP socket", e);
                DhcpServer.this.stop(null);
                return null;
            } finally {
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            }
        }
    }
}
