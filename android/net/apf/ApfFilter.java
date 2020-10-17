package android.net.apf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NattKeepalivePacketDataParcelable;
import android.net.TcpKeepalivePacketDataParcelable;
import android.net.apf.ApfFilter;
import android.net.apf.ApfGenerator;
import android.net.metrics.ApfProgramEvent;
import android.net.metrics.ApfStats;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.RaEvent;
import android.net.networkstack.util.HexDump;
import android.net.networkstack.util.IndentingPrintWriter;
import android.net.p000ip.IpClient;
import android.net.util.InterfaceParams;
import android.net.util.NetworkStackUtils;
import android.net.util.SocketUtils;
import android.os.PowerManager;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

public class ApfFilter {
    private static final byte[] ARP_IPV4_HEADER = {0, 1, 8, 0, 6, 4};
    private static final byte[] ETH_BROADCAST_MAC_ADDRESS = {-1, -1, -1, -1, -1, -1};
    private static final byte[] IPV6_ALL_NODES_ADDRESS = {-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private final ApfCapabilities mApfCapabilities;
    private final Context mContext;
    private final String mCountAndDropLabel;
    private final String mCountAndPassLabel;
    private byte[] mDataSnapshot;
    private final BroadcastReceiver mDeviceIdleReceiver = new BroadcastReceiver() {
        /* class android.net.apf.ApfFilter.C00091 */

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                ApfFilter.this.setDozeMode(((PowerManager) context.getSystemService("power")).isDeviceIdleMode());
            }
        }
    };
    private final boolean mDrop802_3Frames;
    private final int[] mEthTypeBlackList;
    byte[] mHardwareAddress;
    private byte[] mIPv4Address;
    private int mIPv4PrefixLength;
    private boolean mInDozeMode;
    private final InterfaceParams mInterfaceParams;
    private final IpClient.IpClientCallbacksWrapper mIpClientCallback;
    private SparseArray<KeepalivePacket> mKeepalivePackets = new SparseArray<>();
    private ApfProgramEvent.Builder mLastInstallEvent;
    private byte[] mLastInstalledProgram;
    private long mLastInstalledProgramMinLifetime;
    private long mLastTimeInstalledProgram;
    private final IpConnectivityLog mMetricsLog;
    private boolean mMulticastFilter;
    private int mNumProgramUpdates = 0;
    private int mNumProgramUpdatesAllowingMulticast = 0;
    private ArrayList<C0011Ra> mRas = new ArrayList<>();
    ReceiveThread mReceiveThread;
    private long mUniqueCounter;

    public static class ApfConfiguration {
        public ApfCapabilities apfCapabilities;
        public int[] ethTypeBlackList;
        public boolean ieee802_3Filter;
        public boolean multicastFilter;
    }

    /* access modifiers changed from: private */
    public enum ProcessRaResult {
        MATCH,
        DROPPED,
        PARSE_ERROR,
        ZERO_LIFETIME,
        UPDATE_NEW_RA,
        UPDATE_EXPIRY
    }

    private static int uint8(byte b) {
        return b & 255;
    }

    public enum Counter {
        RESERVED_OOB,
        TOTAL_PACKETS,
        PASSED_ARP,
        PASSED_DHCP,
        PASSED_IPV4,
        PASSED_IPV6_NON_ICMP,
        PASSED_IPV4_UNICAST,
        PASSED_IPV6_ICMP,
        PASSED_IPV6_UNICAST_NON_ICMP,
        PASSED_ARP_NON_IPV4,
        PASSED_ARP_UNKNOWN,
        PASSED_ARP_UNICAST_REPLY,
        PASSED_NON_IP_UNICAST,
        DROPPED_ETH_BROADCAST,
        DROPPED_RA,
        DROPPED_GARP_REPLY,
        DROPPED_ARP_OTHER_HOST,
        DROPPED_IPV4_L2_BROADCAST,
        DROPPED_IPV4_BROADCAST_ADDR,
        DROPPED_IPV4_BROADCAST_NET,
        DROPPED_IPV4_MULTICAST,
        DROPPED_IPV6_ROUTER_SOLICITATION,
        DROPPED_IPV6_MULTICAST_NA,
        DROPPED_IPV6_MULTICAST,
        DROPPED_IPV6_MULTICAST_PING,
        DROPPED_IPV6_NON_ICMP_MULTICAST,
        DROPPED_802_3_FRAME,
        DROPPED_ETHERTYPE_BLACKLISTED,
        DROPPED_ARP_REPLY_SPA_NO_HOST,
        DROPPED_IPV4_KEEPALIVE_ACK,
        DROPPED_IPV6_KEEPALIVE_ACK,
        DROPPED_IPV4_NATT_KEEPALIVE;

        public int offset() {
            return (-ordinal()) * 4;
        }

        public static int totalSize() {
            return (((Counter[]) Counter.class.getEnumConstants()).length - 1) * 4;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void maybeSetupCounter(ApfGenerator apfGenerator, Counter counter) {
        if (this.mApfCapabilities.hasDataAccess()) {
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R1, counter.offset());
        }
    }

    /* access modifiers changed from: package-private */
    public class ReceiveThread extends Thread {
        private int mDroppedRas = 0;
        private int mMatchingRas = 0;
        private final byte[] mPacket = new byte[1514];
        private int mParseErrors = 0;
        private int mProgramUpdates = 0;
        private int mReceivedRas = 0;
        private final FileDescriptor mSocket;
        private final long mStart = SystemClock.elapsedRealtime();
        private volatile boolean mStopped;
        private int mZeroLifetimeRas = 0;

        public ReceiveThread(FileDescriptor fileDescriptor) {
            this.mSocket = fileDescriptor;
        }

        public void halt() {
            this.mStopped = true;
            NetworkStackUtils.closeSocketQuietly(this.mSocket);
        }

        public void run() {
            ApfFilter.this.log("begin monitoring");
            while (!this.mStopped) {
                try {
                    updateStats(ApfFilter.this.processRa(this.mPacket, Os.read(this.mSocket, this.mPacket, 0, this.mPacket.length)));
                } catch (ErrnoException | IOException e) {
                    if (!this.mStopped) {
                        Log.e("ApfFilter", "Read error", e);
                    }
                }
            }
            logStats();
        }

        private void updateStats(ProcessRaResult processRaResult) {
            this.mReceivedRas++;
            switch (C00102.$SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[processRaResult.ordinal()]) {
                case 1:
                    this.mMatchingRas++;
                    return;
                case 2:
                    this.mDroppedRas++;
                    return;
                case 3:
                    this.mParseErrors++;
                    return;
                case 4:
                    this.mZeroLifetimeRas++;
                    return;
                case 5:
                    this.mMatchingRas++;
                    this.mProgramUpdates++;
                    return;
                case 6:
                    this.mProgramUpdates++;
                    return;
                default:
                    return;
            }
        }

        private void logStats() {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            synchronized (this) {
                ApfFilter.this.mMetricsLog.log(new ApfStats.Builder().setReceivedRas(this.mReceivedRas).setMatchingRas(this.mMatchingRas).setDroppedRas(this.mDroppedRas).setParseErrors(this.mParseErrors).setZeroLifetimeRas(this.mZeroLifetimeRas).setProgramUpdates(this.mProgramUpdates).setDurationMs(elapsedRealtime - this.mStart).setMaxProgramSize(ApfFilter.this.mApfCapabilities.maximumApfProgramSize).setProgramUpdatesAll(ApfFilter.this.mNumProgramUpdates).setProgramUpdatesAllowingMulticast(ApfFilter.this.mNumProgramUpdatesAllowingMulticast).build());
                ApfFilter.this.logApfProgramEventLocked(elapsedRealtime / 1000);
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* renamed from: android.net.apf.ApfFilter$2 */
    public static /* synthetic */ class C00102 {
        static final /* synthetic */ int[] $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult = new int[ProcessRaResult.values().length];

        /* JADX WARNING: Can't wrap try/catch for region: R(12:0|1|2|3|4|5|6|7|8|9|10|(3:11|12|14)) */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing exception handler attribute for start block: B:11:0x0040 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:3:0x0014 */
        /* JADX WARNING: Missing exception handler attribute for start block: B:5:0x001f */
        /* JADX WARNING: Missing exception handler attribute for start block: B:7:0x002a */
        /* JADX WARNING: Missing exception handler attribute for start block: B:9:0x0035 */
        static {
            $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.MATCH.ordinal()] = 1;
            $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.DROPPED.ordinal()] = 2;
            $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.PARSE_ERROR.ordinal()] = 3;
            $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.ZERO_LIFETIME.ordinal()] = 4;
            $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.UPDATE_EXPIRY.ordinal()] = 5;
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.UPDATE_NEW_RA.ordinal()] = 6;
            } catch (NoSuchFieldError unused) {
            }
        }
    }

    ApfFilter(Context context, ApfConfiguration apfConfiguration, InterfaceParams interfaceParams, IpClient.IpClientCallbacksWrapper ipClientCallbacksWrapper, IpConnectivityLog ipConnectivityLog) {
        this.mApfCapabilities = apfConfiguration.apfCapabilities;
        this.mIpClientCallback = ipClientCallbacksWrapper;
        this.mInterfaceParams = interfaceParams;
        this.mMulticastFilter = apfConfiguration.multicastFilter;
        this.mDrop802_3Frames = apfConfiguration.ieee802_3Filter;
        this.mContext = context;
        if (this.mApfCapabilities.hasDataAccess()) {
            this.mCountAndPassLabel = "countAndPass";
            this.mCountAndDropLabel = "countAndDrop";
        } else {
            this.mCountAndPassLabel = "__PASS__";
            this.mCountAndDropLabel = "__DROP__";
        }
        this.mEthTypeBlackList = filterEthTypeBlackList(apfConfiguration.ethTypeBlackList);
        this.mMetricsLog = ipConnectivityLog;
        maybeStartFilter();
        this.mContext.registerReceiver(this.mDeviceIdleReceiver, new IntentFilter("android.os.action.DEVICE_IDLE_MODE_CHANGED"));
    }

    public synchronized void setDataSnapshot(byte[] bArr) {
        this.mDataSnapshot = bArr;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void log(String str) {
        Log.d("ApfFilter", "(" + this.mInterfaceParams.name + "): " + str);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private long getUniqueNumberLocked() {
        long j = this.mUniqueCounter;
        this.mUniqueCounter = 1 + j;
        return j;
    }

    private static int[] filterEthTypeBlackList(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            int i2 = iArr[i];
            if (i2 >= 1536 && i2 <= 65535 && !arrayList.contains(Integer.valueOf(i2))) {
                if (arrayList.size() == 20) {
                    Log.w("ApfFilter", "Passed EthType Black List size too large (" + arrayList.size() + ") using top " + 20 + " protocols");
                    break;
                }
                arrayList.add(Integer.valueOf(i2));
            }
            i++;
        }
        return arrayList.stream().mapToInt($$Lambda$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
    }

    /* access modifiers changed from: package-private */
    public void maybeStartFilter() {
        try {
            this.mHardwareAddress = this.mInterfaceParams.macAddr.toByteArray();
            synchronized (this) {
                if (this.mApfCapabilities.hasDataAccess()) {
                    this.mIpClientCallback.installPacketFilter(new byte[this.mApfCapabilities.maximumApfProgramSize]);
                }
                installNewProgramLocked();
            }
            FileDescriptor socket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IPV6);
            Os.bind(socket, SocketUtils.makePacketSocketAddress((short) OsConstants.ETH_P_IPV6, this.mInterfaceParams.index));
            NetworkStackUtils.attachRaFilter(socket, this.mApfCapabilities.apfPacketFormat);
            this.mReceiveThread = new ReceiveThread(socket);
            this.mReceiveThread.start();
        } catch (ErrnoException | SocketException e) {
            Log.e("ApfFilter", "Error starting filter", e);
        }
    }

    /* access modifiers changed from: protected */
    public long currentTimeSeconds() {
        return SystemClock.elapsedRealtime() / 1000;
    }

    public static class InvalidRaException extends Exception {
        public InvalidRaException(String str) {
            super(str);
        }
    }

    /* access modifiers changed from: package-private */
    /* renamed from: android.net.apf.ApfFilter$Ra */
    public class C0011Ra {
        long mLastSeen;
        long mMinLifetime;
        private final ArrayList<Pair<Integer, Integer>> mNonLifetimes = new ArrayList<>();
        private final ByteBuffer mPacket;
        private final ArrayList<Integer> mPrefixOptionOffsets = new ArrayList<>();
        private final ArrayList<Integer> mRdnssOptionOffsets = new ArrayList<>();
        int seenCount = 0;

        /* access modifiers changed from: package-private */
        public String getLastMatchingPacket() {
            return HexDump.toHexString(this.mPacket.array(), 0, this.mPacket.capacity(), false);
        }

        private String IPv6AddresstoString(int i) {
            int i2;
            try {
                byte[] array = this.mPacket.array();
                if (i >= 0 && (i2 = i + 16) <= array.length) {
                    if (i2 >= i) {
                        return ((Inet6Address) InetAddress.getByAddress(Arrays.copyOfRange(array, i, i2))).getHostAddress();
                    }
                }
            } catch (ClassCastException | UnsupportedOperationException | UnknownHostException unused) {
            }
            return "???";
        }

        private void prefixOptionToString(StringBuffer stringBuffer, int i) {
            stringBuffer.append(String.format("%s/%d %ds/%ds ", IPv6AddresstoString(i + 16), Integer.valueOf(ApfFilter.getUint8(this.mPacket, i + 2)), Long.valueOf(ApfFilter.getUint32(this.mPacket, i + 4)), Long.valueOf(ApfFilter.getUint32(this.mPacket, i + 8))));
        }

        private void rdnssOptionToString(StringBuffer stringBuffer, int i) {
            int uint8 = ApfFilter.getUint8(this.mPacket, i + 1) * 8;
            if (uint8 >= 24) {
                long uint32 = ApfFilter.getUint32(this.mPacket, i + 4);
                int i2 = (uint8 - 8) / 16;
                stringBuffer.append("DNS ");
                stringBuffer.append(uint32);
                stringBuffer.append("s");
                for (int i3 = 0; i3 < i2; i3++) {
                    stringBuffer.append(" ");
                    stringBuffer.append(IPv6AddresstoString(i + 8 + (i3 * 16)));
                }
            }
        }

        public String toString() {
            try {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(String.format("RA %s -> %s %ds ", IPv6AddresstoString(22), IPv6AddresstoString(38), Integer.valueOf(ApfFilter.getUint16(this.mPacket, 60))));
                Iterator<Integer> it = this.mPrefixOptionOffsets.iterator();
                while (it.hasNext()) {
                    prefixOptionToString(stringBuffer, it.next().intValue());
                }
                Iterator<Integer> it2 = this.mRdnssOptionOffsets.iterator();
                while (it2.hasNext()) {
                    rdnssOptionToString(stringBuffer, it2.next().intValue());
                }
                return stringBuffer.toString();
            } catch (IndexOutOfBoundsException | BufferUnderflowException unused) {
                return "<Malformed RA>";
            }
        }

        private int addNonLifetime(int i, int i2, int i3) {
            int position = i2 + this.mPacket.position();
            this.mNonLifetimes.add(new Pair<>(Integer.valueOf(i), Integer.valueOf(position - i)));
            return position + i3;
        }

        private int addNonLifetimeU32(int i) {
            return addNonLifetime(i, 4, 4);
        }

        C0011Ra(byte[] bArr, int i) throws InvalidRaException {
            if (i >= 70) {
                this.mPacket = ByteBuffer.wrap(Arrays.copyOf(bArr, i));
                this.mLastSeen = ApfFilter.this.currentTimeSeconds();
                if (ApfFilter.getUint16(this.mPacket, 12) == OsConstants.ETH_P_IPV6 && ApfFilter.getUint8(this.mPacket, 20) == OsConstants.IPPROTO_ICMPV6 && ApfFilter.getUint8(this.mPacket, 54) == 134) {
                    RaEvent.Builder builder = new RaEvent.Builder();
                    int addNonLifetime = addNonLifetime(addNonLifetime(addNonLifetime(0, 15, 3), 56, 2), 60, 2);
                    builder.updateRouterLifetime((long) ApfFilter.getUint16(this.mPacket, 60));
                    this.mPacket.position(70);
                    while (this.mPacket.hasRemaining()) {
                        int position = this.mPacket.position();
                        int uint8 = ApfFilter.getUint8(this.mPacket, position);
                        int uint82 = ApfFilter.getUint8(this.mPacket, position + 1) * 8;
                        if (uint8 == 3) {
                            int addNonLifetime2 = addNonLifetime(addNonLifetime, 4, 4);
                            builder.updatePrefixValidLifetime(ApfFilter.getUint32(this.mPacket, position + 4));
                            addNonLifetime = addNonLifetime(addNonLifetime2, 8, 4);
                            builder.updatePrefixPreferredLifetime(ApfFilter.getUint32(this.mPacket, position + 8));
                            this.mPrefixOptionOffsets.add(Integer.valueOf(position));
                        } else if (uint8 == 31) {
                            addNonLifetime = addNonLifetimeU32(addNonLifetime);
                            builder.updateDnsslLifetime(ApfFilter.getUint32(this.mPacket, position + 4));
                        } else if (uint8 == 24) {
                            addNonLifetime = addNonLifetimeU32(addNonLifetime);
                            builder.updateRouteInfoLifetime(ApfFilter.getUint32(this.mPacket, position + 4));
                        } else if (uint8 == 25) {
                            this.mRdnssOptionOffsets.add(Integer.valueOf(position));
                            addNonLifetime = addNonLifetimeU32(addNonLifetime);
                            builder.updateRdnssLifetime(ApfFilter.getUint32(this.mPacket, position + 4));
                        }
                        if (uint82 > 0) {
                            this.mPacket.position(position + uint82);
                        } else {
                            throw new InvalidRaException(String.format("Invalid option length opt=%d len=%d", Integer.valueOf(uint8), Integer.valueOf(uint82)));
                        }
                    }
                    addNonLifetime(addNonLifetime, 0, 0);
                    this.mMinLifetime = minLifetime(bArr, i);
                    ApfFilter.this.mMetricsLog.log(builder.build());
                    return;
                }
                throw new InvalidRaException("Not an ICMP6 router advertisement");
            }
            throw new InvalidRaException("Not an ICMP6 router advertisement");
        }

        /* access modifiers changed from: package-private */
        public boolean matches(byte[] bArr, int i) {
            if (i != this.mPacket.capacity()) {
                return false;
            }
            byte[] array = this.mPacket.array();
            Iterator<Pair<Integer, Integer>> it = this.mNonLifetimes.iterator();
            while (it.hasNext()) {
                Pair<Integer, Integer> next = it.next();
                int intValue = ((Integer) next.first).intValue();
                while (true) {
                    if (intValue < ((Integer) next.first).intValue() + ((Integer) next.second).intValue()) {
                        if (bArr[intValue] != array[intValue]) {
                            return false;
                        }
                        intValue++;
                    }
                }
            }
            return true;
        }

        /* access modifiers changed from: package-private */
        public long minLifetime(byte[] bArr, int i) {
            long j;
            ByteBuffer wrap = ByteBuffer.wrap(bArr);
            long j2 = Long.MAX_VALUE;
            int i2 = 0;
            while (true) {
                int i3 = i2 + 1;
                if (i3 >= this.mNonLifetimes.size()) {
                    return j2;
                }
                int intValue = ((Integer) this.mNonLifetimes.get(i2).first).intValue() + ((Integer) this.mNonLifetimes.get(i2).second).intValue();
                if (!(intValue == 15 || intValue == 56)) {
                    int intValue2 = ((Integer) this.mNonLifetimes.get(i3).first).intValue() - intValue;
                    if (intValue2 == 2) {
                        j = (long) ApfFilter.getUint16(wrap, intValue);
                    } else if (intValue2 == 4) {
                        j = ApfFilter.getUint32(wrap, intValue);
                    } else {
                        throw new IllegalStateException("bogus lifetime size " + intValue2);
                    }
                    j2 = Math.min(j2, j);
                }
                i2 = i3;
            }
        }

        /* access modifiers changed from: package-private */
        public long currentLifetime() {
            return this.mMinLifetime - (ApfFilter.this.currentTimeSeconds() - this.mLastSeen);
        }

        /* access modifiers changed from: package-private */
        public boolean isExpired() {
            return currentLifetime() <= 0;
        }

        /* access modifiers changed from: package-private */
        public long generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
            String str = "Ra" + ApfFilter.this.getUniqueNumberLocked();
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 14);
            apfGenerator.addJumpIfR0NotEquals(this.mPacket.capacity(), str);
            int currentLifetime = (int) (currentLifetime() / 6);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 15);
            apfGenerator.addJumpIfR0GreaterThan(currentLifetime, str);
            int i = 0;
            while (i < this.mNonLifetimes.size()) {
                Pair<Integer, Integer> pair = this.mNonLifetimes.get(i);
                if (((Integer) pair.second).intValue() != 0) {
                    apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, ((Integer) pair.first).intValue());
                    apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, Arrays.copyOfRange(this.mPacket.array(), ((Integer) pair.first).intValue(), ((Integer) pair.first).intValue() + ((Integer) pair.second).intValue()), str);
                }
                i++;
                if (i < this.mNonLifetimes.size()) {
                    Pair<Integer, Integer> pair2 = this.mNonLifetimes.get(i);
                    int intValue = ((Integer) pair.first).intValue() + ((Integer) pair.second).intValue();
                    if (!(intValue == 15 || intValue == 56)) {
                        int intValue2 = ((Integer) pair2.first).intValue() - intValue;
                        if (intValue2 == 2) {
                            apfGenerator.addLoad16(ApfGenerator.Register.R0, intValue);
                        } else if (intValue2 == 4) {
                            apfGenerator.addLoad32(ApfGenerator.Register.R0, intValue);
                        } else {
                            throw new IllegalStateException("bogus lifetime size " + intValue2);
                        }
                        apfGenerator.addJumpIfR0LessThan(currentLifetime, str);
                    }
                }
            }
            ApfFilter.this.maybeSetupCounter(apfGenerator, Counter.DROPPED_RA);
            apfGenerator.addJump(ApfFilter.this.mCountAndDropLabel);
            apfGenerator.defineLabel(str);
            return (long) currentLifetime;
        }
    }

    /* access modifiers changed from: private */
    public static abstract class KeepalivePacket {
        /* access modifiers changed from: package-private */
        public abstract void generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException;

        private KeepalivePacket() {
        }
    }

    /* access modifiers changed from: private */
    public class NattKeepaliveResponse extends KeepalivePacket {
        protected final NattKeepaliveResponseData mPacket;
        protected final byte[] mPayload = {-1};
        protected final byte[] mPortFingerprint;
        protected final byte[] mSrcDstAddr;

        protected class NattKeepaliveResponseData {
            public final byte[] dstAddress;
            public final int dstPort;
            public final byte[] srcAddress;
            public final int srcPort;

            NattKeepaliveResponseData(NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) {
                this.srcAddress = nattKeepalivePacketDataParcelable.dstAddress;
                this.srcPort = nattKeepalivePacketDataParcelable.dstPort;
                this.dstAddress = nattKeepalivePacketDataParcelable.srcAddress;
                this.dstPort = nattKeepalivePacketDataParcelable.srcPort;
            }
        }

        NattKeepaliveResponse(NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) {
            super();
            this.mPacket = new NattKeepaliveResponseData(nattKeepalivePacketDataParcelable);
            NattKeepaliveResponseData nattKeepaliveResponseData = this.mPacket;
            this.mSrcDstAddr = ApfFilter.concatArrays(new byte[][]{nattKeepaliveResponseData.srcAddress, nattKeepaliveResponseData.dstAddress});
            NattKeepaliveResponseData nattKeepaliveResponseData2 = this.mPacket;
            this.mPortFingerprint = generatePortFingerprint(nattKeepaliveResponseData2.srcPort, nattKeepaliveResponseData2.dstPort);
        }

        /* access modifiers changed from: package-private */
        public byte[] generatePortFingerprint(int i, int i2) {
            ByteBuffer allocate = ByteBuffer.allocate(4);
            allocate.order(ByteOrder.BIG_ENDIAN);
            allocate.putShort((short) i);
            allocate.putShort((short) i2);
            return allocate.array();
        }

        /* access modifiers changed from: package-private */
        @Override // android.net.apf.ApfFilter.KeepalivePacket
        public void generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
            String str = "natt_keepalive_filter" + ApfFilter.this.getUniqueNumberLocked();
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 26);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mSrcDstAddr, str);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 13);
            apfGenerator.addAdd(8);
            apfGenerator.addSwap();
            apfGenerator.addLoad16(ApfGenerator.Register.R0, 16);
            apfGenerator.addNeg(ApfGenerator.Register.R1);
            apfGenerator.addAddR1();
            apfGenerator.addJumpIfR0NotEquals(1, str);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 13);
            apfGenerator.addAdd(14);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mPortFingerprint, str);
            apfGenerator.addAdd(8);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mPayload, str);
            ApfFilter.this.maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_NATT_KEEPALIVE);
            apfGenerator.addJump(ApfFilter.this.mCountAndDropLabel);
            apfGenerator.defineLabel(str);
        }

        public String toString() {
            try {
                return String.format("%s -> %s", NetworkStackUtils.addressAndPortToString(InetAddress.getByAddress(this.mPacket.srcAddress), this.mPacket.srcPort), NetworkStackUtils.addressAndPortToString(InetAddress.getByAddress(this.mPacket.dstAddress), this.mPacket.dstPort));
            } catch (UnknownHostException unused) {
                return "Unknown host";
            }
        }
    }

    /* access modifiers changed from: private */
    public static abstract class TcpKeepaliveAck extends KeepalivePacket {
        protected final TcpKeepaliveAckData mPacket;
        protected final byte[] mPortSeqAckFingerprint;
        protected final byte[] mSrcDstAddr;

        protected static class TcpKeepaliveAckData {
            public final int ack;
            public final byte[] dstAddress;
            public final int dstPort;
            public final int seq;
            public final byte[] srcAddress;
            public final int srcPort;

            TcpKeepaliveAckData(TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
                this.srcAddress = tcpKeepalivePacketDataParcelable.dstAddress;
                this.srcPort = tcpKeepalivePacketDataParcelable.dstPort;
                this.dstAddress = tcpKeepalivePacketDataParcelable.srcAddress;
                this.dstPort = tcpKeepalivePacketDataParcelable.srcPort;
                this.seq = tcpKeepalivePacketDataParcelable.ack;
                this.ack = tcpKeepalivePacketDataParcelable.seq + 1;
            }
        }

        TcpKeepaliveAck(TcpKeepaliveAckData tcpKeepaliveAckData, byte[] bArr) {
            super();
            this.mPacket = tcpKeepaliveAckData;
            this.mSrcDstAddr = bArr;
            TcpKeepaliveAckData tcpKeepaliveAckData2 = this.mPacket;
            this.mPortSeqAckFingerprint = generatePortSeqAckFingerprint(tcpKeepaliveAckData2.srcPort, tcpKeepaliveAckData2.dstPort, tcpKeepaliveAckData2.seq, tcpKeepaliveAckData2.ack);
        }

        static byte[] generatePortSeqAckFingerprint(int i, int i2, int i3, int i4) {
            ByteBuffer allocate = ByteBuffer.allocate(12);
            allocate.order(ByteOrder.BIG_ENDIAN);
            allocate.putShort((short) i);
            allocate.putShort((short) i2);
            allocate.putInt(i3);
            allocate.putInt(i4);
            return allocate.array();
        }

        public String toString() {
            try {
                return String.format("%s -> %s , seq=%d, ack=%d", NetworkStackUtils.addressAndPortToString(InetAddress.getByAddress(this.mPacket.srcAddress), this.mPacket.srcPort), NetworkStackUtils.addressAndPortToString(InetAddress.getByAddress(this.mPacket.dstAddress), this.mPacket.dstPort), Long.valueOf(Integer.toUnsignedLong(this.mPacket.seq)), Long.valueOf(Integer.toUnsignedLong(this.mPacket.ack)));
            } catch (UnknownHostException unused) {
                return "Unknown host";
            }
        }
    }

    /* access modifiers changed from: private */
    public class TcpKeepaliveAckV4 extends TcpKeepaliveAck {
        TcpKeepaliveAckV4(ApfFilter apfFilter, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
            this(new TcpKeepaliveAck.TcpKeepaliveAckData(tcpKeepalivePacketDataParcelable));
        }

        TcpKeepaliveAckV4(TcpKeepaliveAck.TcpKeepaliveAckData tcpKeepaliveAckData) {
            super(tcpKeepaliveAckData, ApfFilter.concatArrays(new byte[][]{tcpKeepaliveAckData.srcAddress, tcpKeepaliveAckData.dstAddress}));
        }

        /* access modifiers changed from: package-private */
        @Override // android.net.apf.ApfFilter.KeepalivePacket
        public void generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
            String str = "keepalive_ack" + ApfFilter.this.getUniqueNumberLocked();
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 26);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mSrcDstAddr, str);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R1, 13);
            apfGenerator.addLoad8Indexed(ApfGenerator.Register.R0, 26);
            apfGenerator.addRightShift(2);
            apfGenerator.addAddR1();
            apfGenerator.addLoad16(ApfGenerator.Register.R1, 16);
            apfGenerator.addNeg(ApfGenerator.Register.R0);
            apfGenerator.addAddR1();
            apfGenerator.addJumpIfR0NotEquals(0, str);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R1, 13);
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 14);
            apfGenerator.addAddR1();
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mPortSeqAckFingerprint, str);
            ApfFilter.this.maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_KEEPALIVE_ACK);
            apfGenerator.addJump(ApfFilter.this.mCountAndDropLabel);
            apfGenerator.defineLabel(str);
        }
    }

    /* access modifiers changed from: private */
    public class TcpKeepaliveAckV6 extends TcpKeepaliveAck {
        TcpKeepaliveAckV6(ApfFilter apfFilter, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
            this(new TcpKeepaliveAck.TcpKeepaliveAckData(tcpKeepalivePacketDataParcelable));
        }

        TcpKeepaliveAckV6(TcpKeepaliveAck.TcpKeepaliveAckData tcpKeepaliveAckData) {
            super(tcpKeepaliveAckData, ApfFilter.concatArrays(new byte[][]{tcpKeepaliveAckData.srcAddress, tcpKeepaliveAckData.dstAddress}));
        }

        /* access modifiers changed from: package-private */
        @Override // android.net.apf.ApfFilter.KeepalivePacket
        public void generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
            throw new UnsupportedOperationException("IPv6 TCP Keepalive is not supported yet");
        }
    }

    private void generateArpFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 14);
        maybeSetupCounter(apfGenerator, Counter.PASSED_ARP_NON_IPV4);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ARP_IPV4_HEADER, this.mCountAndPassLabel);
        apfGenerator.addLoad16(ApfGenerator.Register.R0, 20);
        apfGenerator.addJumpIfR0Equals(1, "checkTargetIPv4");
        maybeSetupCounter(apfGenerator, Counter.PASSED_ARP_UNKNOWN);
        apfGenerator.addJumpIfR0NotEquals(2, this.mCountAndPassLabel);
        apfGenerator.addLoad32(ApfGenerator.Register.R0, 28);
        maybeSetupCounter(apfGenerator, Counter.DROPPED_ARP_REPLY_SPA_NO_HOST);
        apfGenerator.addJumpIfR0Equals(0, this.mCountAndDropLabel);
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
        maybeSetupCounter(apfGenerator, Counter.PASSED_ARP_UNICAST_REPLY);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
        apfGenerator.defineLabel("checkTargetIPv4");
        if (this.mIPv4Address == null) {
            apfGenerator.addLoad32(ApfGenerator.Register.R0, 38);
            maybeSetupCounter(apfGenerator, Counter.DROPPED_GARP_REPLY);
            apfGenerator.addJumpIfR0Equals(0, this.mCountAndDropLabel);
        } else {
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 38);
            maybeSetupCounter(apfGenerator, Counter.DROPPED_ARP_OTHER_HOST);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mIPv4Address, this.mCountAndDropLabel);
        }
        maybeSetupCounter(apfGenerator, Counter.PASSED_ARP);
        apfGenerator.addJump(this.mCountAndPassLabel);
    }

    private void generateIPv4FilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        if (this.mMulticastFilter) {
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 23);
            apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_UDP, "skip_dhcp_v4_filter");
            apfGenerator.addLoad16(ApfGenerator.Register.R0, 20);
            apfGenerator.addJumpIfR0AnyBitsSet(8191, "skip_dhcp_v4_filter");
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R1, 13);
            apfGenerator.addLoad16Indexed(ApfGenerator.Register.R0, 16);
            apfGenerator.addJumpIfR0NotEquals(68, "skip_dhcp_v4_filter");
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 50);
            apfGenerator.addAddR1();
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mHardwareAddress, "skip_dhcp_v4_filter");
            maybeSetupCounter(apfGenerator, Counter.PASSED_DHCP);
            apfGenerator.addJump(this.mCountAndPassLabel);
            apfGenerator.defineLabel("skip_dhcp_v4_filter");
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 30);
            apfGenerator.addAnd(240);
            maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_MULTICAST);
            apfGenerator.addJumpIfR0Equals(224, this.mCountAndDropLabel);
            maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_BROADCAST_ADDR);
            apfGenerator.addLoad32(ApfGenerator.Register.R0, 30);
            apfGenerator.addJumpIfR0Equals(-1, this.mCountAndDropLabel);
            if (this.mIPv4Address != null && this.mIPv4PrefixLength < 31) {
                maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_BROADCAST_NET);
                apfGenerator.addJumpIfR0Equals(ipv4BroadcastAddress(this.mIPv4Address, this.mIPv4PrefixLength), this.mCountAndDropLabel);
            }
            generateV4KeepaliveFilters(apfGenerator);
            generateV4NattKeepaliveFilters(apfGenerator);
            maybeSetupCounter(apfGenerator, Counter.PASSED_IPV4_UNICAST);
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
            maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV4_L2_BROADCAST);
            apfGenerator.addJump(this.mCountAndDropLabel);
        } else {
            generateV4KeepaliveFilters(apfGenerator);
            generateV4NattKeepaliveFilters(apfGenerator);
        }
        maybeSetupCounter(apfGenerator, Counter.PASSED_IPV4);
        apfGenerator.addJump(this.mCountAndPassLabel);
    }

    private void generateKeepaliveFilters(ApfGenerator apfGenerator, Class<?> cls, int i, int i2, String str) throws ApfGenerator.IllegalInstructionException {
        if (NetworkStackUtils.any(this.mKeepalivePackets, new Predicate(cls) {
            /* class android.net.apf.lambda */
            private final /* synthetic */ Class f$0;

            {
                this.f$0 = r1;
            }

            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ApfFilter.lambda$generateKeepaliveFilters$0(this.f$0, (ApfFilter.KeepalivePacket) obj);
            }
        })) {
            apfGenerator.addLoad8(ApfGenerator.Register.R0, i2);
            apfGenerator.addJumpIfR0NotEquals(i, str);
            for (int i3 = 0; i3 < this.mKeepalivePackets.size(); i3++) {
                KeepalivePacket valueAt = this.mKeepalivePackets.valueAt(i3);
                if (cls.isInstance(valueAt)) {
                    valueAt.generateFilterLocked(apfGenerator);
                }
            }
            apfGenerator.defineLabel(str);
        }
    }

    private void generateV4KeepaliveFilters(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        generateKeepaliveFilters(apfGenerator, TcpKeepaliveAckV4.class, OsConstants.IPPROTO_TCP, 23, "skip_v4_keepalive_filter");
    }

    private void generateV4NattKeepaliveFilters(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        generateKeepaliveFilters(apfGenerator, NattKeepaliveResponse.class, OsConstants.IPPROTO_UDP, 23, "skip_v4_nattkeepalive_filter");
    }

    private void generateIPv6FilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        apfGenerator.addLoad8(ApfGenerator.Register.R0, 20);
        if (this.mMulticastFilter) {
            if (this.mInDozeMode) {
                apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_ICMPV6, "dropAllIPv6Multicast");
                apfGenerator.addLoad8(ApfGenerator.Register.R0, 54);
                apfGenerator.addJumpIfR0NotEquals(128, "skipIPv6MulticastFilter");
            } else {
                apfGenerator.addJumpIfR0Equals(OsConstants.IPPROTO_ICMPV6, "skipIPv6MulticastFilter");
            }
            apfGenerator.defineLabel("dropAllIPv6Multicast");
            maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV6_NON_ICMP_MULTICAST);
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 38);
            apfGenerator.addJumpIfR0Equals(255, this.mCountAndDropLabel);
            generateV6KeepaliveFilters(apfGenerator);
            maybeSetupCounter(apfGenerator, Counter.PASSED_IPV6_UNICAST_NON_ICMP);
            apfGenerator.addJump(this.mCountAndPassLabel);
            apfGenerator.defineLabel("skipIPv6MulticastFilter");
        } else {
            generateV6KeepaliveFilters(apfGenerator);
            maybeSetupCounter(apfGenerator, Counter.PASSED_IPV6_NON_ICMP);
            apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_ICMPV6, this.mCountAndPassLabel);
        }
        apfGenerator.addLoad8(ApfGenerator.Register.R0, 54);
        maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV6_ROUTER_SOLICITATION);
        apfGenerator.addJumpIfR0Equals(133, this.mCountAndDropLabel);
        apfGenerator.addJumpIfR0NotEquals(136, "skipUnsolicitedMulticastNA");
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 38);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, IPV6_ALL_NODES_ADDRESS, "skipUnsolicitedMulticastNA");
        maybeSetupCounter(apfGenerator, Counter.DROPPED_IPV6_MULTICAST_NA);
        apfGenerator.addJump(this.mCountAndDropLabel);
        apfGenerator.defineLabel("skipUnsolicitedMulticastNA");
    }

    private void generateV6KeepaliveFilters(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        generateKeepaliveFilters(apfGenerator, TcpKeepaliveAckV6.class, OsConstants.IPPROTO_TCP, 20, "skip_v6_keepalive_filter");
    }

    private ApfGenerator emitPrologueLocked() throws ApfGenerator.IllegalInstructionException {
        ApfGenerator apfGenerator = new ApfGenerator(this.mApfCapabilities.apfVersionSupported);
        if (this.mApfCapabilities.hasDataAccess()) {
            maybeSetupCounter(apfGenerator, Counter.TOTAL_PACKETS);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
        }
        apfGenerator.addLoad16(ApfGenerator.Register.R0, 12);
        if (this.mDrop802_3Frames) {
            maybeSetupCounter(apfGenerator, Counter.DROPPED_802_3_FRAME);
            apfGenerator.addJumpIfR0LessThan(1536, this.mCountAndDropLabel);
        }
        maybeSetupCounter(apfGenerator, Counter.DROPPED_ETHERTYPE_BLACKLISTED);
        for (int i : this.mEthTypeBlackList) {
            apfGenerator.addJumpIfR0Equals(i, this.mCountAndDropLabel);
        }
        apfGenerator.addJumpIfR0NotEquals(OsConstants.ETH_P_ARP, "skipArpFilters");
        generateArpFilterLocked(apfGenerator);
        apfGenerator.defineLabel("skipArpFilters");
        apfGenerator.addJumpIfR0NotEquals(OsConstants.ETH_P_IP, "skipIPv4Filters");
        generateIPv4FilterLocked(apfGenerator);
        apfGenerator.defineLabel("skipIPv4Filters");
        apfGenerator.addJumpIfR0Equals(OsConstants.ETH_P_IPV6, "IPv6Filters");
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
        maybeSetupCounter(apfGenerator, Counter.PASSED_NON_IP_UNICAST);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
        maybeSetupCounter(apfGenerator, Counter.DROPPED_ETH_BROADCAST);
        apfGenerator.addJump(this.mCountAndDropLabel);
        apfGenerator.defineLabel("IPv6Filters");
        generateIPv6FilterLocked(apfGenerator);
        return apfGenerator;
    }

    private void emitEpilogue(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        if (this.mApfCapabilities.hasDataAccess()) {
            maybeSetupCounter(apfGenerator, Counter.PASSED_IPV6_ICMP);
            apfGenerator.defineLabel(this.mCountAndPassLabel);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
            apfGenerator.addJump("__PASS__");
            apfGenerator.defineLabel(this.mCountAndDropLabel);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
            apfGenerator.addJump("__DROP__");
        }
    }

    /* access modifiers changed from: package-private */
    public void installNewProgramLocked() {
        purgeExpiredRasLocked();
        ArrayList arrayList = new ArrayList();
        ApfCapabilities apfCapabilities = this.mApfCapabilities;
        long j = (long) apfCapabilities.maximumApfProgramSize;
        if (apfCapabilities.hasDataAccess()) {
            j -= (long) Counter.totalSize();
        }
        try {
            ApfGenerator emitPrologueLocked = emitPrologueLocked();
            emitEpilogue(emitPrologueLocked);
            if (((long) emitPrologueLocked.programLengthOverEstimate()) > j) {
                Log.e("ApfFilter", "Program exceeds maximum size " + j);
                return;
            }
            Iterator<C0011Ra> it = this.mRas.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                C0011Ra next = it.next();
                next.generateFilterLocked(emitPrologueLocked);
                if (((long) emitPrologueLocked.programLengthOverEstimate()) > j) {
                    break;
                }
                arrayList.add(next);
            }
            ApfGenerator emitPrologueLocked2 = emitPrologueLocked();
            Iterator it2 = arrayList.iterator();
            long j2 = Long.MAX_VALUE;
            while (it2.hasNext()) {
                j2 = Math.min(j2, ((C0011Ra) it2.next()).generateFilterLocked(emitPrologueLocked2));
            }
            emitEpilogue(emitPrologueLocked2);
            byte[] generate = emitPrologueLocked2.generate();
            long currentTimeSeconds = currentTimeSeconds();
            this.mLastTimeInstalledProgram = currentTimeSeconds;
            this.mLastInstalledProgramMinLifetime = j2;
            this.mLastInstalledProgram = generate;
            boolean z = true;
            this.mNumProgramUpdates++;
            this.mIpClientCallback.installPacketFilter(generate);
            logApfProgramEventLocked(currentTimeSeconds);
            ApfProgramEvent.Builder programLength = new ApfProgramEvent.Builder().setLifetime(j2).setFilteredRas(arrayList.size()).setCurrentRas(this.mRas.size()).setProgramLength(generate.length);
            if (this.mIPv4Address == null) {
                z = false;
            }
            this.mLastInstallEvent = programLength.setFlags(z, this.mMulticastFilter);
        } catch (ApfGenerator.IllegalInstructionException | IllegalStateException e) {
            Log.e("ApfFilter", "Failed to generate APF program.", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logApfProgramEventLocked(long j) {
        ApfProgramEvent.Builder builder = this.mLastInstallEvent;
        if (builder != null) {
            this.mLastInstallEvent = null;
            long j2 = j - this.mLastTimeInstalledProgram;
            builder.setActualLifetime(j2);
            if (j2 >= 2) {
                this.mMetricsLog.log(builder.build());
            }
        }
    }

    private boolean shouldInstallnewProgram() {
        return this.mLastTimeInstalledProgram + this.mLastInstalledProgramMinLifetime < currentTimeSeconds() + 30;
    }

    private void purgeExpiredRasLocked() {
        int i = 0;
        while (i < this.mRas.size()) {
            if (this.mRas.get(i).isExpired()) {
                log("Expiring " + this.mRas.get(i));
                this.mRas.remove(i);
            } else {
                i++;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized ProcessRaResult processRa(byte[] bArr, int i) {
        for (int i2 = 0; i2 < this.mRas.size(); i2++) {
            C0011Ra ra = this.mRas.get(i2);
            if (ra.matches(bArr, i)) {
                ra.mLastSeen = currentTimeSeconds();
                ra.mMinLifetime = ra.minLifetime(bArr, i);
                ra.seenCount++;
                this.mRas.add(0, this.mRas.remove(i2));
                if (shouldInstallnewProgram()) {
                    installNewProgramLocked();
                    return ProcessRaResult.UPDATE_EXPIRY;
                }
                return ProcessRaResult.MATCH;
            }
        }
        purgeExpiredRasLocked();
        if (this.mRas.size() >= 10) {
            return ProcessRaResult.DROPPED;
        }
        try {
            C0011Ra ra2 = new C0011Ra(bArr, i);
            if (ra2.isExpired()) {
                return ProcessRaResult.ZERO_LIFETIME;
            }
            log("Adding " + ra2);
            this.mRas.add(ra2);
            installNewProgramLocked();
            return ProcessRaResult.UPDATE_NEW_RA;
        } catch (Exception e) {
            Log.e("ApfFilter", "Error parsing RA", e);
            return ProcessRaResult.PARSE_ERROR;
        }
    }

    public static ApfFilter maybeCreate(Context context, ApfConfiguration apfConfiguration, InterfaceParams interfaceParams, IpClient.IpClientCallbacksWrapper ipClientCallbacksWrapper) {
        ApfCapabilities apfCapabilities;
        int i;
        if (context == null || apfConfiguration == null || interfaceParams == null || (apfCapabilities = apfConfiguration.apfCapabilities) == null || (i = apfCapabilities.apfVersionSupported) == 0) {
            return null;
        }
        if (apfCapabilities.maximumApfProgramSize < 512) {
            Log.e("ApfFilter", "Unacceptably small APF limit: " + apfCapabilities.maximumApfProgramSize);
            return null;
        } else if (apfCapabilities.apfPacketFormat != OsConstants.ARPHRD_ETHER) {
            return null;
        } else {
            if (ApfGenerator.supportsVersion(i)) {
                return new ApfFilter(context, apfConfiguration, interfaceParams, ipClientCallbacksWrapper, new IpConnectivityLog());
            }
            Log.e("ApfFilter", "Unsupported APF version: " + apfCapabilities.apfVersionSupported);
            return null;
        }
    }

    public synchronized void shutdown() {
        if (this.mReceiveThread != null) {
            log("shutting down");
            this.mReceiveThread.halt();
            this.mReceiveThread = null;
        }
        this.mRas.clear();
        this.mContext.unregisterReceiver(this.mDeviceIdleReceiver);
    }

    public synchronized void setMulticastFilter(boolean z) {
        if (this.mMulticastFilter != z) {
            this.mMulticastFilter = z;
            if (!z) {
                this.mNumProgramUpdatesAllowingMulticast++;
            }
            installNewProgramLocked();
        }
    }

    public synchronized void setDozeMode(boolean z) {
        if (this.mInDozeMode != z) {
            this.mInDozeMode = z;
            installNewProgramLocked();
        }
    }

    private static LinkAddress findIPv4LinkAddress(LinkProperties linkProperties) {
        LinkAddress linkAddress = null;
        for (LinkAddress linkAddress2 : linkProperties.getLinkAddresses()) {
            if (linkAddress2.getAddress() instanceof Inet4Address) {
                if (!(linkAddress == null || linkAddress.isSameAddressAs(linkAddress2))) {
                    return null;
                }
                linkAddress = linkAddress2;
            }
        }
        return linkAddress;
    }

    public synchronized void setLinkProperties(LinkProperties linkProperties) {
        LinkAddress findIPv4LinkAddress = findIPv4LinkAddress(linkProperties);
        byte[] address = findIPv4LinkAddress != null ? findIPv4LinkAddress.getAddress().getAddress() : null;
        int prefixLength = findIPv4LinkAddress != null ? findIPv4LinkAddress.getPrefixLength() : 0;
        if (prefixLength != this.mIPv4PrefixLength || !Arrays.equals(address, this.mIPv4Address)) {
            this.mIPv4Address = address;
            this.mIPv4PrefixLength = prefixLength;
            installNewProgramLocked();
        }
    }

    public synchronized void addTcpKeepalivePacketFilter(int i, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
        KeepalivePacket keepalivePacket;
        log("Adding keepalive ack(" + i + ")");
        if (this.mKeepalivePackets.get(i) == null) {
            char c = tcpKeepalivePacketDataParcelable.srcAddress.length == 4 ? (char) 4 : 6;
            SparseArray<KeepalivePacket> sparseArray = this.mKeepalivePackets;
            if (c == 4) {
                keepalivePacket = new TcpKeepaliveAckV4(this, tcpKeepalivePacketDataParcelable);
            } else {
                keepalivePacket = new TcpKeepaliveAckV6(this, tcpKeepalivePacketDataParcelable);
            }
            sparseArray.put(i, keepalivePacket);
            installNewProgramLocked();
        } else {
            throw new IllegalArgumentException("Keepalive slot " + i + " is occupied");
        }
    }

    public synchronized void addNattKeepalivePacketFilter(int i, NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) {
        log("Adding NAT-T keepalive packet(" + i + ")");
        if (this.mKeepalivePackets.get(i) != null) {
            throw new IllegalArgumentException("NAT-T Keepalive slot " + i + " is occupied");
        } else if (nattKeepalivePacketDataParcelable.srcAddress.length == 4) {
            this.mKeepalivePackets.put(i, new NattKeepaliveResponse(nattKeepalivePacketDataParcelable));
            installNewProgramLocked();
        } else {
            throw new IllegalArgumentException("NAT-T keepalive is only supported on IPv4");
        }
    }

    public synchronized void removeKeepalivePacketFilter(int i) {
        log("Removing keepalive packet(" + i + ")");
        this.mKeepalivePackets.remove(i);
        installNewProgramLocked();
    }

    public static long counterValue(byte[] bArr, Counter counter) throws ArrayIndexOutOfBoundsException {
        int offset = counter.offset();
        if (offset < 0) {
            offset += bArr.length;
        }
        long j = 0;
        for (int i = 0; i < 4; i++) {
            j = (j << 8) | ((long) (bArr[offset] & 255));
            offset++;
        }
        return j;
    }

    public synchronized void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Capabilities: " + this.mApfCapabilities);
        StringBuilder sb = new StringBuilder();
        sb.append("Receive thread: ");
        sb.append(this.mReceiveThread != null ? "RUNNING" : "STOPPED");
        indentingPrintWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Multicast: ");
        sb2.append(this.mMulticastFilter ? "DROP" : "ALLOW");
        indentingPrintWriter.println(sb2.toString());
        try {
            indentingPrintWriter.println("IPv4 address: " + InetAddress.getByAddress(this.mIPv4Address).getHostAddress());
        } catch (NullPointerException | UnknownHostException unused) {
        }
        if (this.mLastTimeInstalledProgram == 0) {
            indentingPrintWriter.println("No program installed.");
            return;
        }
        indentingPrintWriter.println("Program updates: " + this.mNumProgramUpdates);
        indentingPrintWriter.println(String.format("Last program length %d, installed %ds ago, lifetime %ds", Integer.valueOf(this.mLastInstalledProgram.length), Long.valueOf(currentTimeSeconds() - this.mLastTimeInstalledProgram), Long.valueOf(this.mLastInstalledProgramMinLifetime)));
        indentingPrintWriter.println("RA filters:");
        indentingPrintWriter.increaseIndent();
        Iterator<C0011Ra> it = this.mRas.iterator();
        while (it.hasNext()) {
            C0011Ra next = it.next();
            indentingPrintWriter.println(next);
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(String.format("Seen: %d, last %ds ago", Integer.valueOf(next.seenCount), Long.valueOf(currentTimeSeconds() - next.mLastSeen)));
            indentingPrintWriter.println("Last match:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(next.getLastMatchingPacket());
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("TCP Keepalive filters:");
        indentingPrintWriter.increaseIndent();
        for (int i = 0; i < this.mKeepalivePackets.size(); i++) {
            KeepalivePacket valueAt = this.mKeepalivePackets.valueAt(i);
            if (valueAt instanceof TcpKeepaliveAck) {
                indentingPrintWriter.print("Slot ");
                indentingPrintWriter.print(this.mKeepalivePackets.keyAt(i));
                indentingPrintWriter.print(": ");
                indentingPrintWriter.println(valueAt);
            }
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("NAT-T Keepalive filters:");
        indentingPrintWriter.increaseIndent();
        for (int i2 = 0; i2 < this.mKeepalivePackets.size(); i2++) {
            KeepalivePacket valueAt2 = this.mKeepalivePackets.valueAt(i2);
            if (valueAt2 instanceof NattKeepaliveResponse) {
                indentingPrintWriter.print("Slot ");
                indentingPrintWriter.print(this.mKeepalivePackets.keyAt(i2));
                indentingPrintWriter.print(": ");
                indentingPrintWriter.println(valueAt2);
            }
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Last program:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println(HexDump.toHexString(this.mLastInstalledProgram, false));
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("APF packet counters: ");
        indentingPrintWriter.increaseIndent();
        if (!this.mApfCapabilities.hasDataAccess()) {
            indentingPrintWriter.println("APF counters not supported");
        } else if (this.mDataSnapshot == null) {
            indentingPrintWriter.println("No last snapshot.");
        } else {
            try {
                Counter[] counterArr = (Counter[]) Counter.class.getEnumConstants();
                for (Counter counter : Arrays.asList(counterArr).subList(1, counterArr.length)) {
                    long counterValue = counterValue(this.mDataSnapshot, counter);
                    if (counterValue != 0) {
                        indentingPrintWriter.println(counter.toString() + ": " + counterValue);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                indentingPrintWriter.println("Uh-oh: " + e);
            }
        }
        indentingPrintWriter.decreaseIndent();
    }

    public static int ipv4BroadcastAddress(byte[] bArr, int i) {
        return bytesToBEInt(bArr) | ((int) (Integer.toUnsignedLong(-1) >>> i));
    }

    /* access modifiers changed from: private */
    public static int getUint16(ByteBuffer byteBuffer, int i) {
        return byteBuffer.getShort(i) & 65535;
    }

    /* access modifiers changed from: private */
    public static long getUint32(ByteBuffer byteBuffer, int i) {
        return Integer.toUnsignedLong(byteBuffer.getInt(i));
    }

    /* access modifiers changed from: private */
    public static int getUint8(ByteBuffer byteBuffer, int i) {
        return uint8(byteBuffer.get(i));
    }

    private static int bytesToBEInt(byte[] bArr) {
        return (uint8(bArr[0]) << 24) + (uint8(bArr[1]) << 16) + (uint8(bArr[2]) << 8) + uint8(bArr[3]);
    }

    /* access modifiers changed from: private */
    public static byte[] concatArrays(byte[]... bArr) {
        int i = 0;
        for (byte[] bArr2 : bArr) {
            i += bArr2.length;
        }
        byte[] bArr3 = new byte[i];
        int i2 = 0;
        for (byte[] bArr4 : bArr) {
            System.arraycopy(bArr4, 0, bArr3, i2, bArr4.length);
            i2 += bArr4.length;
        }
        return bArr3;
    }
}
