package android.net.dhcp;

import android.content.Context;
import android.icu.text.Transliterator;
import android.net.InetAddresses;
import android.net.TrafficStats;
import android.net.dhcp.DhcpPacket;
import android.net.metrics.DhcpClientEvent;
import android.net.metrics.DhcpErrorEvent;
import android.net.metrics.IpConnectivityLog;
import android.net.networkstack.DhcpResults;
import android.net.networkstack.util.HexDump;
import android.net.networkstack.util.MessageUtils;
import android.net.networkstack.util.State;
import android.net.networkstack.util.StateMachine;
import android.net.networkstack.util.WakeupMessage;
import android.net.util.InterfaceParams;
import android.net.util.NetworkStackUtils;
import android.net.util.SocketUtils;
import android.os.Debug;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import com.android.networkstack.R$array;
import com.android.server.util.NetworkStackConstants;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class DhcpClient extends StateMachine {
    public static final int CMD_CLEAR_LINKADDRESS = 1007;
    public static final int CMD_CONFIGURE_LINKADDRESS = 1008;
    private static final int CMD_EXPIRE_DHCP = 1106;
    private static final int CMD_KICK = 1101;
    public static final int CMD_ON_QUIT = 1005;
    public static final int CMD_POST_DHCP_ACTION = 1004;
    public static final int CMD_PRE_DHCP_ACTION = 1003;
    public static final int CMD_PRE_DHCP_ACTION_COMPLETE = 1006;
    private static final int CMD_REBIND_DHCP = 1105;
    private static final int CMD_RECEIVED_PACKET = 1102;
    public static final int CMD_RELEASE = 1011;
    private static final int CMD_RENEW_DHCP = 1104;
    public static final int CMD_START_DHCP = 1001;
    public static final int CMD_STOP_DHCP = 1002;
    private static final int CMD_TIMEOUT = 1103;
    private static final boolean DBG = Debug.semIsProductDev();
    public static final int EVENT_LINKADDRESS_CONFIGURED = 1009;
    private static final boolean MSG_DBG = Log.isLoggable("DhcpClient", 3);
    private static final boolean PACKET_DBG = Log.isLoggable("DhcpClient", 3);
    static final byte[] REQUESTED_PARAMS = {1, 3, 6, 15, 26, 28, 51, 58, 59, 43};
    private static final boolean STATE_DBG = Log.isLoggable("DhcpClient", 3);
    private static final Class[] sMessageClasses = {DhcpClient.class};
    private static final SparseArray<String> sMessageNames = MessageUtils.findMessageNames(sMessageClasses);
    private State mConfiguringInterfaceState = new ConfiguringInterfaceState();
    private final Context mContext;
    private final StateMachine mController;
    private State mDhcpBoundState = new DhcpBoundState();
    private State mDhcpHaveLeaseState = new DhcpHaveLeaseState();
    private State mDhcpInitRebootState = new DhcpInitRebootState();
    private State mDhcpInitState = new DhcpInitState();
    private DhcpResults mDhcpLease;
    private long mDhcpLeaseExpiry;
    private State mDhcpRebindingState = new DhcpRebindingState();
    private State mDhcpRebootingState = new DhcpRebootingState();
    private State mDhcpRenewingState = new DhcpRenewingState();
    private State mDhcpRequestingState = new DhcpRequestingState();
    private State mDhcpSelectingState = new DhcpSelectingState();
    private State mDhcpState = new DhcpState();
    private final WakeupMessage mExpiryAlarm;
    private byte[] mHwAddr;
    private InterfaceParams mIface;
    private final String mIfaceName;
    private SocketAddress mInterfaceBroadcastAddr;
    private final WakeupMessage mKickAlarm;
    private long mLastBoundExitTime;
    private long mLastInitEnterTime;
    private final IpConnectivityLog mMetricsLog = new IpConnectivityLog();
    private DhcpResults mOffer;
    private FileDescriptor mPacketSock;
    private final Random mRandom;
    private final WakeupMessage mRebindAlarm;
    private ReceiveThread mReceiveThread;
    private boolean mRegisteredForPreDhcpNotification;
    private final WakeupMessage mRenewAlarm;
    private State mStoppedState = new StoppedState();
    private final WakeupMessage mTimeoutAlarm;
    private int mTransactionId;
    private long mTransactionStartMillis;
    private FileDescriptor mUdpSock;
    private State mWaitBeforeRenewalState = new WaitBeforeRenewalState(this.mDhcpRenewingState);
    private State mWaitBeforeStartState = new WaitBeforeStartState(this.mDhcpInitState);

    private WakeupMessage makeWakeupMessage(String str, int i) {
        return new WakeupMessage(this.mContext, getHandler(), DhcpClient.class.getSimpleName() + "." + this.mIfaceName + "." + str, i);
    }

    private DhcpClient(Context context, StateMachine stateMachine, String str) {
        super("DhcpClient", stateMachine.getHandler());
        this.mContext = context;
        this.mController = stateMachine;
        this.mIfaceName = str;
        addState(this.mStoppedState);
        addState(this.mDhcpState);
        addState(this.mDhcpInitState, this.mDhcpState);
        addState(this.mWaitBeforeStartState, this.mDhcpState);
        addState(this.mDhcpSelectingState, this.mDhcpState);
        addState(this.mDhcpRequestingState, this.mDhcpState);
        addState(this.mDhcpHaveLeaseState, this.mDhcpState);
        addState(this.mConfiguringInterfaceState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpBoundState, this.mDhcpHaveLeaseState);
        addState(this.mWaitBeforeRenewalState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRenewingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpRebindingState, this.mDhcpHaveLeaseState);
        addState(this.mDhcpInitRebootState, this.mDhcpState);
        addState(this.mDhcpRebootingState, this.mDhcpState);
        setInitialState(this.mStoppedState);
        this.mRandom = new Random();
        this.mKickAlarm = makeWakeupMessage("KICK", CMD_KICK);
        this.mTimeoutAlarm = makeWakeupMessage("TIMEOUT", CMD_TIMEOUT);
        this.mRenewAlarm = makeWakeupMessage("RENEW", CMD_RENEW_DHCP);
        this.mRebindAlarm = makeWakeupMessage("REBIND", CMD_REBIND_DHCP);
        this.mExpiryAlarm = makeWakeupMessage("EXPIRY", CMD_EXPIRE_DHCP);
    }

    public void registerForPreDhcpNotification() {
        this.mRegisteredForPreDhcpNotification = true;
    }

    public static DhcpClient makeDhcpClient(Context context, StateMachine stateMachine, InterfaceParams interfaceParams) {
        DhcpClient dhcpClient = new DhcpClient(context, stateMachine, interfaceParams.name);
        dhcpClient.mIface = interfaceParams;
        dhcpClient.start();
        return dhcpClient;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean initInterface() {
        if (this.mIface == null) {
            this.mIface = InterfaceParams.getByName(this.mIfaceName);
        }
        InterfaceParams interfaceParams = this.mIface;
        if (interfaceParams == null) {
            Log.e("DhcpClient", "Can't determine InterfaceParams for " + this.mIfaceName);
            return DBG;
        }
        this.mHwAddr = interfaceParams.macAddr.toByteArray();
        this.mInterfaceBroadcastAddr = SocketUtils.makePacketSocketAddress(this.mIface.index, DhcpPacket.ETHER_BROADCAST);
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void startNewTransaction() {
        this.mTransactionId = this.mRandom.nextInt();
        this.mTransactionStartMillis = SystemClock.elapsedRealtime();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean initSockets() {
        if (!initPacketSocket() || !initUdpSocket()) {
            return DBG;
        }
        return true;
    }

    private boolean initPacketSocket() {
        try {
            this.mPacketSock = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IP);
            Os.bind(this.mPacketSock, SocketUtils.makePacketSocketAddress((short) OsConstants.ETH_P_IP, this.mIface.index));
            NetworkStackUtils.attachDhcpFilter(this.mPacketSock);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error creating packet socket", e);
            return DBG;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean initUdpSocket() {
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-511);
        try {
            this.mUdpSock = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP);
            SocketUtils.bindSocketToInterface(this.mUdpSock, this.mIfaceName);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_REUSEADDR, 1);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_BROADCAST, 1);
            Os.setsockoptInt(this.mUdpSock, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 0);
            Os.bind(this.mUdpSock, NetworkStackConstants.IPV4_ADDR_ANY, 68);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error creating UDP socket", e);
            return DBG;
        } finally {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean connectUdpSock(Inet4Address inet4Address) {
        try {
            Os.connect(this.mUdpSock, inet4Address, 67);
            return true;
        } catch (ErrnoException | SocketException e) {
            Log.e("DhcpClient", "Error connecting UDP socket", e);
            return DBG;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void closeSockets() {
        NetworkStackUtils.closeSocketQuietly(this.mUdpSock);
        NetworkStackUtils.closeSocketQuietly(this.mPacketSock);
    }

    /* access modifiers changed from: package-private */
    public class ReceiveThread extends Thread {
        private final byte[] mPacket = new byte[1500];
        private volatile boolean mStopped = DhcpClient.DBG;

        ReceiveThread() {
        }

        public void halt() {
            this.mStopped = true;
            DhcpClient.this.closeSockets();
        }

        /* JADX WARNING: Code restructure failed: missing block: B:14:0x004b, code lost:
            r3 = e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:0x004c, code lost:
            r2 = 0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:18:0x006b, code lost:
            android.util.Log.d("DhcpClient", android.net.networkstack.util.HexDump.dumpHexString(r8.mPacket, 0, r2));
         */
        /* JADX WARNING: Code restructure failed: missing block: B:21:0x007a, code lost:
            android.util.EventLog.writeEvent(1397638484, "31850211", -1, android.net.dhcp.DhcpPacket.ParseException.class.getName());
         */
        /* JADX WARNING: Code restructure failed: missing block: B:23:0x00a1, code lost:
            r0 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x00a6, code lost:
            if (r8.mStopped == false) goto L_0x00a8;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:26:0x00a8, code lost:
            android.util.Log.e("DhcpClient", "Read error", r0);
            r8.this$0.logError(84017152);
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x006b  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x007a  */
        /* JADX WARNING: Removed duplicated region for block: B:23:0x00a1 A[ExcHandler: ErrnoException | IOException (r0v5 'e' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:6:0x0012] */
        public void run() {
            if (DhcpClient.DBG) {
                Log.d("DhcpClient", "Receive thread started");
            }
            while (!this.mStopped) {
                try {
                    int i = Os.read(DhcpClient.this.mPacketSock, this.mPacket, 0, this.mPacket.length);
                    DhcpPacket decodeFullPacket = DhcpPacket.decodeFullPacket(this.mPacket, i, 0);
                    if (DhcpClient.DBG) {
                        Log.d("DhcpClient", "Received packet: " + decodeFullPacket);
                    }
                    DhcpClient.this.sendMessage(DhcpClient.CMD_RECEIVED_PACKET, decodeFullPacket);
                } catch (ErrnoException | IOException e) {
                } catch (DhcpPacket.ParseException e2) {
                    e = e2;
                    Log.e("DhcpClient", "Can't parse packet: " + e.getMessage());
                    if (DhcpClient.PACKET_DBG) {
                    }
                    if (e.errorCode == 67502080) {
                    }
                    DhcpClient.this.logError(e.errorCode);
                }
            }
            if (DhcpClient.DBG) {
                Log.d("DhcpClient", "Receive thread stopped");
            }
        }
    }

    private short getSecs() {
        return (short) ((int) ((SystemClock.elapsedRealtime() - this.mTransactionStartMillis) / 1000));
    }

    private boolean transmitPacket(ByteBuffer byteBuffer, String str, int i, Inet4Address inet4Address) {
        if (i == 0) {
            try {
                if (DBG) {
                    Log.d("DhcpClient", "Broadcasting " + str);
                }
                Os.sendto(this.mPacketSock, byteBuffer.array(), 0, byteBuffer.limit(), 0, this.mInterfaceBroadcastAddr);
            } catch (ErrnoException e) {
                e = e;
                Log.e("DhcpClient", "Can't send packet: ", e);
                return DBG;
            } catch (IOException e2) {
                e = e2;
                Log.e("DhcpClient", "Can't send packet: ", e);
                return DBG;
            }
        } else if (i != 2 || !inet4Address.equals(DhcpPacket.INADDR_BROADCAST)) {
            if (DBG) {
                Log.d("DhcpClient", String.format("Unicasting %s to %s", str, Os.getpeername(this.mUdpSock)));
            }
            Os.write(this.mUdpSock, byteBuffer);
        } else {
            if (DBG) {
                Log.d("DhcpClient", "Broadcasting " + str);
            }
            Os.sendto(this.mUdpSock, byteBuffer, 0, inet4Address, 67);
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean sendDiscoverPacket() {
        return transmitPacket(DhcpPacket.buildDiscoverPacket(0, this.mTransactionId, getSecs(), this.mHwAddr, DBG, REQUESTED_PARAMS, getHostNameFromDeviceName()), "DHCPDISCOVER", 0, DhcpPacket.INADDR_BROADCAST);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean sendRequestPacket(Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4) {
        int i = DhcpPacket.INADDR_ANY.equals(inet4Address) ? 0 : 2;
        ByteBuffer buildRequestPacket = DhcpPacket.buildRequestPacket(i, this.mTransactionId, getSecs(), inet4Address, DBG, this.mHwAddr, inet4Address2, inet4Address3, REQUESTED_PARAMS, getHostNameFromDeviceName());
        String hostAddress = inet4Address3 != null ? inet4Address3.getHostAddress() : null;
        return transmitPacket(buildRequestPacket, "DHCPREQUEST ciaddr=" + inet4Address.getHostAddress() + " request=" + inet4Address2.getHostAddress() + " serverid=" + hostAddress, i, inet4Address4);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean sendReleasePacket() {
        if (this.mDhcpLease == null) {
            Log.d("DhcpClient", "mDhcpLease is null, Skip sendReleasePacket");
            return DBG;
        }
        Log.d("DhcpClient", "sendReleasePacket");
        ByteBuffer buildReleasePacket = DhcpPacket.buildReleasePacket(2, this.mTransactionId, getSecs(), (Inet4Address) this.mDhcpLease.ipAddress.getAddress(), this.mHwAddr, this.mDhcpLease.serverAddress);
        Inet4Address inet4Address = this.mDhcpLease.serverAddress;
        if (inet4Address == null) {
            inet4Address = DhcpPacket.INADDR_BROADCAST;
        }
        return transmitPacket(buildReleasePacket, "DHCPRELEASE", 2, inet4Address);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void scheduleLeaseTimers() {
        if (this.mDhcpLeaseExpiry == 0) {
            Log.d("DhcpClient", "Infinite lease, no timer scheduling needed");
            return;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        long j = this.mDhcpLeaseExpiry - elapsedRealtime;
        long j2 = j / 2;
        long j3 = (7 * j) / 8;
        this.mRenewAlarm.schedule(elapsedRealtime + j2);
        this.mRebindAlarm.schedule(elapsedRealtime + j3);
        this.mExpiryAlarm.schedule(elapsedRealtime + j);
        Log.d("DhcpClient", "Scheduling renewal in " + (j2 / 1000) + "s");
        Log.d("DhcpClient", "Scheduling rebind in " + (j3 / 1000) + "s");
        Log.d("DhcpClient", "Scheduling expiry in " + (j / 1000) + "s");
    }

    private void notifySuccess() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 1, 0, new DhcpResults(this.mDhcpLease));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyFailure() {
        this.mController.sendMessage(CMD_POST_DHCP_ACTION, 2, 0, null);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void acceptDhcpResults(DhcpResults dhcpResults, String str) {
        this.mDhcpLease = dhcpResults;
        if (this.mDhcpLease.dnsServers.isEmpty()) {
            String[] stringArray = this.mContext.getResources().getStringArray(R$array.config_default_dns_servers);
            for (String str2 : stringArray) {
                try {
                    this.mDhcpLease.dnsServers.add(InetAddresses.parseNumericAddress(str2));
                } catch (IllegalArgumentException e) {
                    Log.e("DhcpClient", "Invalid default DNS server: " + str2, e);
                }
            }
        }
        this.mOffer = null;
        Log.d("DhcpClient", str + " lease: " + this.mDhcpLease);
        notifySuccess();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void clearDhcpState() {
        this.mDhcpLease = null;
        this.mDhcpLeaseExpiry = 0;
        this.mOffer = null;
    }

    public void doQuit() {
        Log.d("DhcpClient", "doQuit");
        quit();
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public void onQuitting() {
        Log.d("DhcpClient", "onQuitting");
        this.mController.sendMessage(CMD_ON_QUIT);
    }

    /* access modifiers changed from: package-private */
    public abstract class LoggingState extends State {
        private long mEnterTimeMs;

        LoggingState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            if (DhcpClient.STATE_DBG) {
                Log.d("DhcpClient", "Entering state " + getName());
            }
            this.mEnterTimeMs = SystemClock.elapsedRealtime();
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            DhcpClient.this.logState(getName(), (int) (SystemClock.elapsedRealtime() - this.mEnterTimeMs));
        }

        private String messageName(int i) {
            return (String) DhcpClient.sMessageNames.get(i, Integer.toString(i));
        }

        private String messageToString(Message message) {
            long uptimeMillis = SystemClock.uptimeMillis();
            return " " + (message.getWhen() - uptimeMillis) + messageName(message.what) + " " + message.arg1 + " " + message.arg2 + " " + message.obj;
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            if (!DhcpClient.MSG_DBG) {
                return DhcpClient.DBG;
            }
            Log.d("DhcpClient", getName() + messageToString(message));
            return DhcpClient.DBG;
        }

        @Override // android.net.networkstack.util.IState, android.net.networkstack.util.State
        public String getName() {
            return getClass().getSimpleName();
        }
    }

    abstract class WaitBeforeOtherState extends LoggingState {
        protected State mOtherState;

        WaitBeforeOtherState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION);
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != 1006) {
                return DhcpClient.DBG;
            }
            DhcpClient.this.transitionTo(this.mOtherState);
            return true;
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            if (message.what != 1001) {
                return DhcpClient.DBG;
            }
            if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.transitionTo(dhcpClient.mWaitBeforeStartState);
                return true;
            }
            DhcpClient dhcpClient2 = DhcpClient.this;
            dhcpClient2.transitionTo(dhcpClient2.mDhcpInitState);
            return true;
        }
    }

    class WaitBeforeStartState extends WaitBeforeOtherState {
        public WaitBeforeStartState(State state) {
            super();
            this.mOtherState = state;
        }
    }

    class WaitBeforeRenewalState extends WaitBeforeOtherState {
        public WaitBeforeRenewalState(State state) {
            super();
            this.mOtherState = state;
        }
    }

    class DhcpState extends State {
        DhcpState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            DhcpClient.this.clearDhcpState();
            if (!DhcpClient.this.initInterface() || !DhcpClient.this.initSockets()) {
                DhcpClient.this.notifyFailure();
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.transitionTo(dhcpClient.mStoppedState);
                return;
            }
            DhcpClient dhcpClient2 = DhcpClient.this;
            dhcpClient2.mReceiveThread = new ReceiveThread();
            DhcpClient.this.mReceiveThread.start();
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            if (DhcpClient.this.mReceiveThread != null) {
                DhcpClient.this.mReceiveThread.halt();
                DhcpClient.this.mReceiveThread = null;
            }
            DhcpClient.this.clearDhcpState();
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != 1002) {
                return DhcpClient.DBG;
            }
            DhcpClient dhcpClient = DhcpClient.this;
            dhcpClient.transitionTo(dhcpClient.mStoppedState);
            return true;
        }
    }

    public boolean isValidPacket(DhcpPacket dhcpPacket) {
        int transactionId = dhcpPacket.getTransactionId();
        if (transactionId != this.mTransactionId) {
            Log.d("DhcpClient", "Unexpected transaction ID " + transactionId + ", expected " + this.mTransactionId);
            return DBG;
        } else if (Arrays.equals(dhcpPacket.getClientMac(), this.mHwAddr)) {
            return true;
        } else {
            Log.d("DhcpClient", "MAC addr mismatch: got " + HexDump.toHexString(dhcpPacket.getClientMac()) + ", expected " + HexDump.toHexString(dhcpPacket.getClientMac()));
            return DBG;
        }
    }

    public void setDhcpLeaseExpiry(DhcpPacket dhcpPacket) {
        long leaseTimeMillis = dhcpPacket.getLeaseTimeMillis();
        long j = 0;
        if (leaseTimeMillis > 0) {
            j = SystemClock.elapsedRealtime() + leaseTimeMillis;
        }
        this.mDhcpLeaseExpiry = j;
    }

    /* access modifiers changed from: package-private */
    public abstract class PacketRetransmittingState extends LoggingState {
        protected int mTimeout = 0;
        private int mTimer;

        /* access modifiers changed from: protected */
        public abstract void receivePacket(DhcpPacket dhcpPacket);

        /* access modifiers changed from: protected */
        public abstract boolean sendPacket();

        /* access modifiers changed from: protected */
        public void timeout() {
        }

        PacketRetransmittingState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            initTimer();
            maybeInitTimeout();
            DhcpClient.this.sendMessage(DhcpClient.CMD_KICK);
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            super.processMessage(message);
            switch (message.what) {
                case DhcpClient.CMD_KICK /*{ENCODED_INT: 1101}*/:
                    sendPacket();
                    scheduleKick();
                    return true;
                case DhcpClient.CMD_RECEIVED_PACKET /*{ENCODED_INT: 1102}*/:
                    receivePacket((DhcpPacket) message.obj);
                    return true;
                case DhcpClient.CMD_TIMEOUT /*{ENCODED_INT: 1103}*/:
                    timeout();
                    return true;
                default:
                    return DhcpClient.DBG;
            }
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void exit() {
            super.exit();
            DhcpClient.this.mKickAlarm.cancel();
            DhcpClient.this.mTimeoutAlarm.cancel();
        }

        /* access modifiers changed from: protected */
        public void initTimer() {
            this.mTimer = 1000;
        }

        /* access modifiers changed from: protected */
        public int jitterTimer(int i) {
            int i2 = i / 10;
            return i + (DhcpClient.this.mRandom.nextInt(i2 * 2) - i2);
        }

        /* access modifiers changed from: protected */
        public void scheduleKick() {
            DhcpClient.this.mKickAlarm.schedule(SystemClock.elapsedRealtime() + ((long) jitterTimer(this.mTimer)));
            this.mTimer *= 2;
            if (this.mTimer > 1024000) {
                this.mTimer = 1024000;
            }
        }

        /* access modifiers changed from: protected */
        public void maybeInitTimeout() {
            if (this.mTimeout > 0) {
                DhcpClient.this.mTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeout));
            }
        }
    }

    class DhcpInitState extends PacketRetransmittingState {
        public DhcpInitState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.dhcp.DhcpClient.PacketRetransmittingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            DhcpClient.this.startNewTransaction();
            DhcpClient.this.mLastInitEnterTime = SystemClock.elapsedRealtime();
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public boolean sendPacket() {
            return DhcpClient.this.sendDiscoverPacket();
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket) && (dhcpPacket instanceof DhcpOfferPacket)) {
                DhcpClient.this.mOffer = dhcpPacket.toDhcpResults();
                if (DhcpClient.this.mOffer != null) {
                    Log.d("DhcpClient", "Got pending lease: " + DhcpClient.this.mOffer);
                    DhcpClient dhcpClient = DhcpClient.this;
                    dhcpClient.transitionTo(dhcpClient.mDhcpRequestingState);
                }
            }
        }
    }

    class DhcpSelectingState extends LoggingState {
        DhcpSelectingState() {
            super();
        }
    }

    class DhcpRequestingState extends PacketRetransmittingState {
        public DhcpRequestingState() {
            super();
            this.mTimeout = 18000;
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public boolean sendPacket() {
            DhcpClient dhcpClient = DhcpClient.this;
            return dhcpClient.sendRequestPacket(DhcpPacket.INADDR_ANY, (Inet4Address) dhcpClient.mOffer.ipAddress.getAddress(), DhcpClient.this.mOffer.serverAddress, DhcpPacket.INADDR_BROADCAST);
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket)) {
                if (dhcpPacket instanceof DhcpAckPacket) {
                    DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                    if (dhcpResults != null) {
                        DhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                        DhcpClient.this.acceptDhcpResults(dhcpResults, "Confirmed");
                        DhcpClient dhcpClient = DhcpClient.this;
                        dhcpClient.transitionTo(dhcpClient.mConfiguringInterfaceState);
                    }
                } else if (dhcpPacket instanceof DhcpNakPacket) {
                    Log.d("DhcpClient", "Received NAK, returning to INIT");
                    DhcpClient.this.mOffer = null;
                    DhcpClient dhcpClient2 = DhcpClient.this;
                    dhcpClient2.transitionTo(dhcpClient2.mDhcpInitState);
                }
            }
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public void timeout() {
            DhcpClient dhcpClient = DhcpClient.this;
            dhcpClient.transitionTo(dhcpClient.mDhcpInitState);
        }
    }

    class DhcpHaveLeaseState extends State {
        DhcpHaveLeaseState() {
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1011) {
                DhcpClient.this.sendReleasePacket();
                return true;
            } else if (i != DhcpClient.CMD_EXPIRE_DHCP) {
                return DhcpClient.DBG;
            } else {
                Log.d("DhcpClient", "Lease expired!");
                DhcpClient.this.notifyFailure();
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.transitionTo(dhcpClient.mDhcpInitState);
                return true;
            }
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            DhcpClient.this.mRenewAlarm.cancel();
            DhcpClient.this.mRebindAlarm.cancel();
            DhcpClient.this.mExpiryAlarm.cancel();
            DhcpClient.this.clearDhcpState();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CLEAR_LINKADDRESS);
        }
    }

    class ConfiguringInterfaceState extends LoggingState {
        ConfiguringInterfaceState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            DhcpClient.this.mController.sendMessage(DhcpClient.CMD_CONFIGURE_LINKADDRESS, DhcpClient.this.mDhcpLease.ipAddress);
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != 1009) {
                return DhcpClient.DBG;
            }
            DhcpClient dhcpClient = DhcpClient.this;
            dhcpClient.transitionTo(dhcpClient.mDhcpBoundState);
            return true;
        }
    }

    class DhcpBoundState extends LoggingState {
        DhcpBoundState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            if (DhcpClient.this.mDhcpLease.serverAddress != null) {
                DhcpClient dhcpClient = DhcpClient.this;
                if (!dhcpClient.connectUdpSock(dhcpClient.mDhcpLease.serverAddress)) {
                    DhcpClient.this.notifyFailure();
                    DhcpClient dhcpClient2 = DhcpClient.this;
                    dhcpClient2.transitionTo(dhcpClient2.mStoppedState);
                }
            }
            DhcpClient.this.scheduleLeaseTimers();
            logTimeToBoundState();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public void exit() {
            super.exit();
            DhcpClient.this.mLastBoundExitTime = SystemClock.elapsedRealtime();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            super.processMessage(message);
            if (message.what != DhcpClient.CMD_RENEW_DHCP) {
                return DhcpClient.DBG;
            }
            if (DhcpClient.this.mRegisteredForPreDhcpNotification) {
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.transitionTo(dhcpClient.mWaitBeforeRenewalState);
                return true;
            }
            DhcpClient dhcpClient2 = DhcpClient.this;
            dhcpClient2.transitionTo(dhcpClient2.mDhcpRenewingState);
            return true;
        }

        private void logTimeToBoundState() {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            if (DhcpClient.this.mLastBoundExitTime > DhcpClient.this.mLastInitEnterTime) {
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.logState("RenewingBoundState", (int) (elapsedRealtime - dhcpClient.mLastBoundExitTime));
                return;
            }
            DhcpClient dhcpClient2 = DhcpClient.this;
            dhcpClient2.logState("InitialBoundState", (int) (elapsedRealtime - dhcpClient2.mLastInitEnterTime));
        }
    }

    abstract class DhcpReacquiringState extends PacketRetransmittingState {
        protected String mLeaseMsg;

        /* access modifiers changed from: protected */
        public abstract Inet4Address packetDestination();

        DhcpReacquiringState() {
            super();
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.dhcp.DhcpClient.PacketRetransmittingState, android.net.networkstack.util.State
        public void enter() {
            super.enter();
            DhcpClient.this.startNewTransaction();
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public boolean sendPacket() {
            DhcpClient dhcpClient = DhcpClient.this;
            return dhcpClient.sendRequestPacket((Inet4Address) dhcpClient.mDhcpLease.ipAddress.getAddress(), DhcpPacket.INADDR_ANY, null, packetDestination());
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.PacketRetransmittingState
        public void receivePacket(DhcpPacket dhcpPacket) {
            if (DhcpClient.this.isValidPacket(dhcpPacket)) {
                if (dhcpPacket instanceof DhcpAckPacket) {
                    DhcpResults dhcpResults = dhcpPacket.toDhcpResults();
                    if (dhcpResults != null) {
                        if (!DhcpClient.this.mDhcpLease.ipAddress.equals(dhcpResults.ipAddress)) {
                            Log.d("DhcpClient", "Renewed lease not for our current IP address!");
                            DhcpClient.this.notifyFailure();
                            DhcpClient dhcpClient = DhcpClient.this;
                            dhcpClient.transitionTo(dhcpClient.mDhcpInitState);
                        }
                        DhcpClient.this.setDhcpLeaseExpiry(dhcpPacket);
                        DhcpClient.this.acceptDhcpResults(dhcpResults, this.mLeaseMsg);
                        DhcpClient dhcpClient2 = DhcpClient.this;
                        dhcpClient2.transitionTo(dhcpClient2.mDhcpBoundState);
                    }
                } else if (dhcpPacket instanceof DhcpNakPacket) {
                    Log.d("DhcpClient", "Received NAK, returning to INIT");
                    DhcpClient.this.notifyFailure();
                    DhcpClient dhcpClient3 = DhcpClient.this;
                    dhcpClient3.transitionTo(dhcpClient3.mDhcpInitState);
                }
            }
        }
    }

    class DhcpRenewingState extends DhcpReacquiringState {
        public DhcpRenewingState() {
            super();
            this.mLeaseMsg = "Renewed";
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.dhcp.DhcpClient.PacketRetransmittingState, android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            if (message.what != DhcpClient.CMD_REBIND_DHCP) {
                return DhcpClient.DBG;
            }
            DhcpClient dhcpClient = DhcpClient.this;
            dhcpClient.transitionTo(dhcpClient.mDhcpRebindingState);
            return true;
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.DhcpReacquiringState
        public Inet4Address packetDestination() {
            return DhcpClient.this.mDhcpLease.serverAddress != null ? DhcpClient.this.mDhcpLease.serverAddress : DhcpPacket.INADDR_BROADCAST;
        }
    }

    class DhcpRebindingState extends DhcpReacquiringState {
        public DhcpRebindingState() {
            super();
            this.mLeaseMsg = "Rebound";
        }

        @Override // android.net.dhcp.DhcpClient.LoggingState, android.net.dhcp.DhcpClient.PacketRetransmittingState, android.net.networkstack.util.State, android.net.dhcp.DhcpClient.DhcpReacquiringState
        public void enter() {
            super.enter();
            NetworkStackUtils.closeSocketQuietly(DhcpClient.this.mUdpSock);
            if (!DhcpClient.this.initUdpSocket()) {
                Log.e("DhcpClient", "Failed to recreate UDP socket");
                DhcpClient dhcpClient = DhcpClient.this;
                dhcpClient.transitionTo(dhcpClient.mDhcpInitState);
            }
        }

        /* access modifiers changed from: protected */
        @Override // android.net.dhcp.DhcpClient.DhcpReacquiringState
        public Inet4Address packetDestination() {
            return DhcpPacket.INADDR_BROADCAST;
        }
    }

    class DhcpInitRebootState extends LoggingState {
        DhcpInitRebootState() {
            super();
        }
    }

    class DhcpRebootingState extends LoggingState {
        DhcpRebootingState() {
            super();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logError(int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpErrorEvent(i));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logState(String str, int i) {
        this.mMetricsLog.log(this.mIfaceName, new DhcpClientEvent.Builder().setMsg(str).setDurationMs(i).build());
    }

    private String getHostNameFromDeviceName() {
        String str;
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "device_name");
        if (string != null) {
            String replaceAll = Transliterator.getInstance("Any-latin; nfd; [:nonspacing mark:] remove; nfc").transliterate(string).replaceAll("[^[[a-z][A-Z][0-9][ ][-]]]", "").replaceAll(" ", "-");
            if (replaceAll.length() > 0 && replaceAll.charAt(0) == '-') {
                replaceAll = replaceAll.replaceFirst("-+", "");
            }
            if (replaceAll.length() > 0 && replaceAll.charAt(replaceAll.length() - 1) == '-') {
                replaceAll = replaceLast(replaceAll);
            }
            str = replaceAll.replaceAll("-+", "-");
        } else {
            str = null;
        }
        Log.d("DhcpClient", "hostname = " + str);
        return str;
    }

    private String replaceLast(String str) {
        return new StringBuffer(new StringBuffer(str).reverse().toString().replaceFirst("-+", "")).reverse().toString();
    }
}
