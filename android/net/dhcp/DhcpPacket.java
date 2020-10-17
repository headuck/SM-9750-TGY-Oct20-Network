package android.net.dhcp;

import android.net.LinkAddress;
import android.net.metrics.DhcpErrorEvent;
import android.net.networkstack.DhcpResults;
import android.net.networkstack.shared.Inet4AddressUtils;
import android.os.Build;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.server.util.NetworkStackConstants;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class DhcpPacket {
    public static final byte[] ETHER_BROADCAST = {-1, -1, -1, -1, -1, -1};
    public static final Inet4Address INADDR_ANY = NetworkStackConstants.IPV4_ADDR_ANY;
    public static final Inet4Address INADDR_BROADCAST = NetworkStackConstants.IPV4_ADDR_ALL;
    private static String mHostNameByDeviceName;
    static String testOverrideHostname = null;
    static String testOverrideVendorId = null;
    protected boolean mBroadcast;
    protected Inet4Address mBroadcastAddress;
    protected byte[] mClientId;
    protected final Inet4Address mClientIp;
    protected final byte[] mClientMac;
    protected List<Inet4Address> mDnsServers;
    protected String mDomainName;
    protected List<Inet4Address> mGateways;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected Short mMaxMessageSize;
    protected String mMessage;
    protected Short mMtu;
    private final Inet4Address mNextIp;
    protected final Inet4Address mRelayIp;
    protected Inet4Address mRequestedIp;
    protected byte[] mRequestedParams;
    protected final short mSecs;
    protected String mServerHostName;
    protected Inet4Address mServerIdentifier;
    protected Inet4Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final int mTransId;
    protected String mVendorId;
    protected String mVendorInfo;
    protected final Inet4Address mYourIp;

    private static int intAbs(short s) {
        return s & 65535;
    }

    private static boolean isPacketServerToServer(short s, short s2) {
        return s == 67 && s2 == 67;
    }

    private static boolean isPacketToOrFromClient(short s, short s2) {
        return s == 68 || s2 == 68;
    }

    /* access modifiers changed from: package-private */
    public abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr, boolean z) {
        this.mTransId = i;
        this.mSecs = s;
        this.mClientIp = inet4Address;
        this.mYourIp = inet4Address2;
        this.mNextIp = inet4Address3;
        this.mRelayIp = inet4Address4;
        this.mClientMac = bArr;
        this.mBroadcast = z;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public boolean hasExplicitClientId() {
        return this.mClientId != null;
    }

    public byte[] getExplicitClientIdOrNull() {
        if (hasExplicitClientId()) {
            return getClientId();
        }
        return null;
    }

    public byte[] getClientId() {
        if (hasExplicitClientId()) {
            byte[] bArr = this.mClientId;
            return Arrays.copyOf(bArr, bArr.length);
        }
        byte[] bArr2 = this.mClientMac;
        byte[] bArr3 = new byte[(bArr2.length + 1)];
        bArr3[0] = 1;
        System.arraycopy(bArr2, 0, bArr3, 1, bArr2.length);
        return bArr3;
    }

    public boolean hasRequestedParam(byte b) {
        byte[] bArr = this.mRequestedParams;
        if (bArr == null) {
            return false;
        }
        for (byte b2 : bArr) {
            if (b2 == b) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void fillInPacket(int i, Inet4Address inet4Address, Inet4Address inet4Address2, short s, short s2, ByteBuffer byteBuffer, byte b, boolean z) {
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        byte[] address = inet4Address.getAddress();
        byte[] address2 = inet4Address2.getAddress();
        byteBuffer.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        if (i == 0) {
            byteBuffer.put(ETHER_BROADCAST);
            byteBuffer.put(this.mClientMac);
            byteBuffer.putShort((short) OsConstants.ETH_P_IP);
        }
        if (i <= 1) {
            i6 = byteBuffer.position();
            byteBuffer.put((byte) 69);
            byteBuffer.put((byte) 16);
            i5 = byteBuffer.position();
            byteBuffer.putShort(0);
            byteBuffer.putShort(0);
            byteBuffer.putShort(16384);
            byteBuffer.put((byte) 64);
            byteBuffer.put((byte) 17);
            i4 = byteBuffer.position();
            byteBuffer.putShort(0);
            byteBuffer.put(address2);
            byteBuffer.put(address);
            i8 = byteBuffer.position();
            i7 = byteBuffer.position();
            byteBuffer.putShort(s2);
            byteBuffer.putShort(s);
            i3 = byteBuffer.position();
            byteBuffer.putShort(0);
            i2 = byteBuffer.position();
            byteBuffer.putShort(0);
        } else {
            i8 = 0;
            i7 = 0;
            i6 = 0;
            i5 = 0;
            i4 = 0;
            i3 = 0;
            i2 = 0;
        }
        byteBuffer.put(b);
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte) this.mClientMac.length);
        byteBuffer.put((byte) 0);
        byteBuffer.putInt(this.mTransId);
        byteBuffer.putShort(this.mSecs);
        if (z) {
            byteBuffer.putShort(Short.MIN_VALUE);
        } else {
            byteBuffer.putShort(0);
        }
        byteBuffer.put(this.mClientIp.getAddress());
        byteBuffer.put(this.mYourIp.getAddress());
        byteBuffer.put(this.mNextIp.getAddress());
        byteBuffer.put(this.mRelayIp.getAddress());
        byteBuffer.put(this.mClientMac);
        byteBuffer.position(byteBuffer.position() + (16 - this.mClientMac.length) + 64 + 128);
        byteBuffer.putInt(1669485411);
        finishPacket(byteBuffer);
        if ((byteBuffer.position() & 1) == 1) {
            byteBuffer.put((byte) 0);
        }
        if (i <= 1) {
            short position = (short) (byteBuffer.position() - i7);
            byteBuffer.putShort(i3, position);
            byteBuffer.putShort(i2, (short) checksum(byteBuffer, intAbs(byteBuffer.getShort(i4 + 2)) + 0 + intAbs(byteBuffer.getShort(i4 + 4)) + intAbs(byteBuffer.getShort(i4 + 6)) + intAbs(byteBuffer.getShort(i4 + 8)) + 17 + position, i7, byteBuffer.position()));
            byteBuffer.putShort(i5, (short) (byteBuffer.position() - i6));
            byteBuffer.putShort(i4, (short) checksum(byteBuffer, 0, i6, i8));
        }
    }

    private int checksum(ByteBuffer byteBuffer, int i, int i2, int i3) {
        int position = byteBuffer.position();
        byteBuffer.position(i2);
        ShortBuffer asShortBuffer = byteBuffer.asShortBuffer();
        byteBuffer.position(position);
        short[] sArr = new short[((i3 - i2) / 2)];
        asShortBuffer.get(sArr);
        for (short s : sArr) {
            i += intAbs(s);
        }
        int length = i2 + (sArr.length * 2);
        if (i3 != length) {
            short s2 = (short) byteBuffer.get(length);
            if (s2 < 0) {
                s2 = (short) (s2 + 256);
            }
            i += s2 * 256;
        }
        int i4 = ((i >> 16) & 65535) + (i & 65535);
        return intAbs((short) (~((i4 + ((i4 >> 16) & 65535)) & 65535)));
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, byte b2) {
        byteBuffer.put(b);
        byteBuffer.put((byte) 1);
        byteBuffer.put(b2);
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, byte[] bArr) {
        if (bArr == null) {
            return;
        }
        if (bArr.length <= 255) {
            byteBuffer.put(b);
            byteBuffer.put((byte) bArr.length);
            byteBuffer.put(bArr);
            return;
        }
        throw new IllegalArgumentException("DHCP option too long: " + bArr.length + " vs. " + 255);
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Inet4Address inet4Address) {
        if (inet4Address != null) {
            addTlv(byteBuffer, b, inet4Address.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, List<Inet4Address> list) {
        if (!(list == null || list.size() == 0)) {
            int size = list.size() * 4;
            if (size <= 255) {
                byteBuffer.put(b);
                byteBuffer.put((byte) size);
                for (Inet4Address inet4Address : list) {
                    byteBuffer.put(inet4Address.getAddress());
                }
                return;
            }
            throw new IllegalArgumentException("DHCP option too long: " + size + " vs. " + 255);
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Short sh) {
        if (sh != null) {
            byteBuffer.put(b);
            byteBuffer.put((byte) 2);
            byteBuffer.putShort(sh.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Integer num) {
        if (num != null) {
            byteBuffer.put(b);
            byteBuffer.put((byte) 4);
            byteBuffer.putInt(num.intValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, String str) {
        if (str != null) {
            try {
                addTlv(byteBuffer, b, str.getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException unused) {
                throw new IllegalArgumentException("String is not US-ASCII: " + str);
            }
        }
    }

    protected static void addTlvEnd(ByteBuffer byteBuffer) {
        byteBuffer.put((byte) -1);
    }

    private String getVendorId() {
        String str = testOverrideVendorId;
        if (str != null) {
            return str;
        }
        return "android-dhcp-" + Build.VERSION.RELEASE;
    }

    private String getHostname() {
        String str = testOverrideHostname;
        if (str != null) {
            return str;
        }
        return SystemProperties.get("net.hostname");
    }

    /* access modifiers changed from: protected */
    public void addCommonClientTlvs(ByteBuffer byteBuffer) {
        String str;
        addTlv(byteBuffer, (byte) 57, (Short) 1500);
        addTlv(byteBuffer, (byte) 60, getVendorId());
        if (!TextUtils.isEmpty(mHostNameByDeviceName)) {
            str = mHostNameByDeviceName;
        } else {
            str = getHostname();
        }
        if (!TextUtils.isEmpty(str)) {
            addTlv(byteBuffer, (byte) 12, str);
        }
    }

    /* access modifiers changed from: protected */
    public void addCommonServerTlvs(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (byte) 51, this.mLeaseTime);
        Integer num = this.mLeaseTime;
        if (!(num == null || num.intValue() == -1)) {
            addTlv(byteBuffer, (byte) 58, Integer.valueOf((int) (Integer.toUnsignedLong(this.mLeaseTime.intValue()) / 2)));
            addTlv(byteBuffer, (byte) 59, Integer.valueOf((int) ((Integer.toUnsignedLong(this.mLeaseTime.intValue()) * 875) / 1000)));
        }
        addTlv(byteBuffer, (byte) 1, this.mSubnetMask);
        addTlv(byteBuffer, (byte) 28, this.mBroadcastAddress);
        addTlv(byteBuffer, (byte) 3, this.mGateways);
        addTlv(byteBuffer, (byte) 6, this.mDnsServers);
        addTlv(byteBuffer, (byte) 15, this.mDomainName);
        addTlv(byteBuffer, (byte) 12, this.mHostName);
        addTlv(byteBuffer, (byte) 43, this.mVendorInfo);
        Short sh = this.mMtu;
        if (sh != null && Short.toUnsignedInt(sh.shortValue()) >= 68) {
            addTlv(byteBuffer, (byte) 26, this.mMtu);
        }
    }

    public static String macToString(byte[] bArr) {
        String str = "";
        for (int i = 0; i < bArr.length; i++) {
            String str2 = "0" + Integer.toHexString(bArr[i]);
            str = str + str2.substring(str2.length() - 2);
            if (i != bArr.length - 1) {
                str = str + ":";
            }
        }
        return str;
    }

    public String toString() {
        return macToString(this.mClientMac);
    }

    private static Inet4Address readIpAddress(ByteBuffer byteBuffer) {
        byte[] bArr = new byte[4];
        byteBuffer.get(bArr);
        try {
            return (Inet4Address) Inet4Address.getByAddress(bArr);
        } catch (UnknownHostException unused) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer byteBuffer, int i, boolean z) {
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        int length = bArr.length;
        if (!z) {
            length = 0;
            while (length < bArr.length && bArr[length] != 0) {
                length++;
            }
        }
        return new String(bArr, 0, length, StandardCharsets.US_ASCII);
    }

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int i, String str, Object... objArr) {
            super(String.format(str, objArr));
            this.errorCode = i;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:146:0x02c9 A[SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x02c2 A[SYNTHETIC] */
    static DhcpPacket decodeFullPacket(ByteBuffer byteBuffer, int i) throws ParseException {
        Inet4Address inet4Address;
        Inet4Address inet4Address2;
        DhcpPacket dhcpPacket;
        int i2;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byte b = 6;
        if (i == 0) {
            if (byteBuffer.remaining() >= 278) {
                byteBuffer.get(new byte[6]);
                byteBuffer.get(new byte[6]);
                short s = byteBuffer.getShort();
                if (s != OsConstants.ETH_P_IP) {
                    throw new ParseException(16908288, "Unexpected L2 type 0x%04x, expected 0x%04x", Short.valueOf(s), Integer.valueOf(OsConstants.ETH_P_IP));
                }
            } else {
                throw new ParseException(16842752, "L2 packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), 278);
            }
        }
        byte b2 = 15;
        if (i > 1) {
            inet4Address = null;
        } else if (byteBuffer.remaining() >= 264) {
            byte b3 = byteBuffer.get();
            int i3 = (b3 & 240) >> 4;
            if (i3 == 4) {
                byteBuffer.get();
                byteBuffer.getShort();
                byteBuffer.getShort();
                byteBuffer.get();
                byteBuffer.get();
                byteBuffer.get();
                byte b4 = byteBuffer.get();
                byteBuffer.getShort();
                Inet4Address readIpAddress = readIpAddress(byteBuffer);
                readIpAddress(byteBuffer);
                if (b4 == 17) {
                    int i4 = (b3 & 15) - 5;
                    for (int i5 = 0; i5 < i4; i5++) {
                        byteBuffer.getInt();
                    }
                    short s2 = byteBuffer.getShort();
                    short s3 = byteBuffer.getShort();
                    byteBuffer.getShort();
                    byteBuffer.getShort();
                    if (isPacketToOrFromClient(s2, s3) || isPacketServerToServer(s2, s3)) {
                        inet4Address = readIpAddress;
                    } else {
                        throw new ParseException(50462720, "Unexpected UDP ports %d->%d", Short.valueOf(s2), Short.valueOf(s3));
                    }
                } else {
                    throw new ParseException(50397184, "Protocol not UDP: %d", Byte.valueOf(b4));
                }
            } else {
                throw new ParseException(33685504, "Invalid IP version %d", Integer.valueOf(i3));
            }
        } else {
            throw new ParseException(33619968, "L3 packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), 264);
        }
        if (i > 2 || byteBuffer.remaining() < 236) {
            throw new ParseException(67174400, "Invalid type or BOOTP packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), 236);
        }
        byteBuffer.get();
        byteBuffer.get();
        int i6 = byteBuffer.get() & 255;
        byteBuffer.get();
        int i7 = byteBuffer.getInt();
        short s4 = byteBuffer.getShort();
        boolean z = (byteBuffer.getShort() & 32768) != 0;
        byte[] bArr = new byte[4];
        try {
            byteBuffer.get(bArr);
            Inet4Address inet4Address3 = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address4 = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address5 = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address6 = (Inet4Address) Inet4Address.getByAddress(bArr);
            if (i6 > 16) {
                i6 = ETHER_BROADCAST.length;
            }
            byte[] bArr2 = new byte[i6];
            byteBuffer.get(bArr2);
            byteBuffer.position(byteBuffer.position() + (16 - i6));
            String readAsciiString = readAsciiString(byteBuffer, 64, false);
            byteBuffer.position(byteBuffer.position() + 128);
            if (byteBuffer.remaining() >= 4) {
                int i8 = byteBuffer.getInt();
                if (i8 == 1669485411) {
                    byte b5 = -1;
                    boolean z2 = true;
                    byte b6 = 0;
                    byte b7 = -1;
                    Inet4Address inet4Address7 = null;
                    Inet4Address inet4Address8 = null;
                    Inet4Address inet4Address9 = null;
                    String str = null;
                    String str2 = null;
                    String str3 = null;
                    byte[] bArr3 = null;
                    String str4 = null;
                    String str5 = null;
                    Inet4Address inet4Address10 = null;
                    Short sh = null;
                    Short sh2 = null;
                    Integer num = null;
                    Integer num2 = null;
                    Integer num3 = null;
                    while (byteBuffer.position() < byteBuffer.limit() && z2) {
                        byte b8 = byteBuffer.get();
                        if (b8 == b5) {
                            z2 = false;
                        } else if (b8 == 0) {
                            continue;
                        } else {
                            try {
                                int i9 = byteBuffer.get() & 255;
                                if (b8 != 1) {
                                    if (b8 == 3) {
                                        int i10 = 0;
                                        while (i2 < i9) {
                                            arrayList2.add(readIpAddress(byteBuffer));
                                            i10 = i2 + 4;
                                        }
                                    } else if (b8 != b) {
                                        if (b8 == 12) {
                                            str4 = readAsciiString(byteBuffer, i9, false);
                                        } else if (b8 != b2) {
                                            if (b8 == 26) {
                                                sh = Short.valueOf(byteBuffer.getShort());
                                            } else if (b8 == 28) {
                                                inet4Address8 = readIpAddress(byteBuffer);
                                            } else if (b8 != 43) {
                                                switch (b8) {
                                                    case 50:
                                                        inet4Address10 = readIpAddress(byteBuffer);
                                                        break;
                                                    case 51:
                                                        num = Integer.valueOf(byteBuffer.getInt());
                                                        break;
                                                    case 52:
                                                        b6 = (byte) (byteBuffer.get() & 3);
                                                        i2 = 1;
                                                        break;
                                                    case 53:
                                                        b7 = byteBuffer.get();
                                                        i2 = 1;
                                                        break;
                                                    case 54:
                                                        inet4Address9 = readIpAddress(byteBuffer);
                                                        break;
                                                    case 55:
                                                        byte[] bArr4 = new byte[i9];
                                                        byteBuffer.get(bArr4);
                                                        bArr3 = bArr4;
                                                        break;
                                                    case 56:
                                                        str = readAsciiString(byteBuffer, i9, false);
                                                        break;
                                                    case 57:
                                                        sh2 = Short.valueOf(byteBuffer.getShort());
                                                        break;
                                                    case 58:
                                                        num2 = Integer.valueOf(byteBuffer.getInt());
                                                        break;
                                                    case 59:
                                                        num3 = Integer.valueOf(byteBuffer.getInt());
                                                        break;
                                                    case 60:
                                                        str2 = readAsciiString(byteBuffer, i9, true);
                                                        break;
                                                    case 61:
                                                        byteBuffer.get(new byte[i9]);
                                                        break;
                                                    default:
                                                        int i11 = 0;
                                                        for (int i12 = 0; i12 < i9; i12++) {
                                                            i11++;
                                                            byteBuffer.get();
                                                        }
                                                        i2 = i11;
                                                        break;
                                                }
                                            } else {
                                                str3 = readAsciiString(byteBuffer, i9, true);
                                            }
                                            i2 = 2;
                                        } else {
                                            str5 = readAsciiString(byteBuffer, i9, false);
                                        }
                                        i2 = i9;
                                    } else {
                                        i2 = 0;
                                        while (i2 < i9) {
                                            arrayList.add(readIpAddress(byteBuffer));
                                            i2 += 4;
                                        }
                                    }
                                    if (i2 == i9) {
                                        throw new ParseException(DhcpErrorEvent.errorCodeWithOption(67305472, b8), "Invalid length %d for option %d, expected %d", Integer.valueOf(i9), Byte.valueOf(b8), Integer.valueOf(i2));
                                    }
                                } else {
                                    inet4Address7 = readIpAddress(byteBuffer);
                                }
                                i2 = 4;
                                if (i2 == i9) {
                                }
                            } catch (BufferUnderflowException unused) {
                                throw new ParseException(DhcpErrorEvent.errorCodeWithOption(83951616, b8), "BufferUnderflowException", new Object[0]);
                            }
                        }
                        b = 6;
                        b2 = 15;
                        b5 = -1;
                    }
                    switch (b7) {
                        case -1:
                            throw new ParseException(67371008, "No DHCP message type option", new Object[0]);
                        case 0:
                        default:
                            throw new ParseException(67436544, "Unimplemented DHCP type %d", Byte.valueOf(b7));
                        case 1:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpDiscoverPacket(i7, s4, inet4Address6, bArr2, z, inet4Address);
                            break;
                        case 2:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpOfferPacket(i7, s4, z, inet4Address, inet4Address6, inet4Address3, inet4Address4, bArr2);
                            break;
                        case 3:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpRequestPacket(i7, s4, inet4Address3, inet4Address6, bArr2, z);
                            break;
                        case 4:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpDeclinePacket(i7, s4, inet4Address3, inet4Address4, inet4Address5, inet4Address6, bArr2);
                            break;
                        case 5:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpAckPacket(i7, s4, z, inet4Address, inet4Address6, inet4Address3, inet4Address4, bArr2);
                            break;
                        case 6:
                            inet4Address2 = inet4Address9;
                            dhcpPacket = new DhcpNakPacket(i7, s4, inet4Address6, bArr2, z);
                            break;
                        case 7:
                            if (inet4Address9 != null) {
                                inet4Address2 = inet4Address9;
                                dhcpPacket = new DhcpReleasePacket(i7, inet4Address9, inet4Address3, inet4Address6, bArr2);
                                break;
                            } else {
                                throw new ParseException(5, "DHCPRELEASE without server identifier", new Object[0]);
                            }
                        case 8:
                            dhcpPacket = new DhcpInformPacket(i7, s4, inet4Address3, inet4Address4, inet4Address5, inet4Address6, bArr2);
                            inet4Address2 = inet4Address9;
                            break;
                    }
                    dhcpPacket.mBroadcastAddress = inet4Address8;
                    dhcpPacket.mClientId = null;
                    dhcpPacket.mDnsServers = arrayList;
                    dhcpPacket.mDomainName = str5;
                    dhcpPacket.mGateways = arrayList2;
                    dhcpPacket.mHostName = str4;
                    dhcpPacket.mLeaseTime = num;
                    dhcpPacket.mMessage = str;
                    dhcpPacket.mMtu = sh;
                    dhcpPacket.mRequestedIp = inet4Address10;
                    dhcpPacket.mRequestedParams = bArr3;
                    dhcpPacket.mServerIdentifier = inet4Address2;
                    dhcpPacket.mSubnetMask = inet4Address7;
                    dhcpPacket.mMaxMessageSize = sh2;
                    dhcpPacket.mT1 = num2;
                    dhcpPacket.mT2 = num3;
                    dhcpPacket.mVendorId = str2;
                    dhcpPacket.mVendorInfo = str3;
                    if ((b6 & 2) == 0) {
                        dhcpPacket.mServerHostName = readAsciiString;
                    } else {
                        dhcpPacket.mServerHostName = "";
                    }
                    return dhcpPacket;
                }
                throw new ParseException(67239936, "Bad magic cookie 0x%08x, should be 0x%08x", Integer.valueOf(i8), 1669485411);
            }
            throw new ParseException(67502080, "not a DHCP message", new Object[0]);
        } catch (UnknownHostException unused2) {
            throw new ParseException(33751040, "Invalid IPv4 address: %s", Arrays.toString(bArr));
        }
    }

    public static DhcpPacket decodeFullPacket(byte[] bArr, int i, int i2) throws ParseException {
        try {
            return decodeFullPacket(ByteBuffer.wrap(bArr, 0, i).order(ByteOrder.BIG_ENDIAN), i2);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e2) {
            throw new ParseException(84082688, e2.getMessage(), new Object[0]);
        }
    }

    public DhcpResults toDhcpResults() {
        int i;
        Inet4Address inet4Address = this.mYourIp;
        if (inet4Address.equals(NetworkStackConstants.IPV4_ADDR_ANY)) {
            inet4Address = this.mClientIp;
            if (inet4Address.equals(NetworkStackConstants.IPV4_ADDR_ANY)) {
                return null;
            }
        }
        Inet4Address inet4Address2 = this.mSubnetMask;
        if (inet4Address2 != null) {
            try {
                i = Inet4AddressUtils.netmaskToPrefixLength(inet4Address2);
            } catch (IllegalArgumentException unused) {
                return null;
            }
        } else {
            i = Inet4AddressUtils.getImplicitNetmask(inet4Address);
        }
        DhcpResults dhcpResults = new DhcpResults();
        try {
            dhcpResults.ipAddress = new LinkAddress(inet4Address, i);
            short s = 0;
            if (this.mGateways.size() > 0) {
                dhcpResults.gateway = this.mGateways.get(0);
            }
            dhcpResults.dnsServers.addAll(this.mDnsServers);
            dhcpResults.domains = this.mDomainName;
            dhcpResults.serverAddress = this.mServerIdentifier;
            dhcpResults.vendorInfo = this.mVendorInfo;
            Integer num = this.mLeaseTime;
            dhcpResults.leaseDuration = num != null ? num.intValue() : -1;
            Short sh = this.mMtu;
            if (sh != null && 1280 <= sh.shortValue() && this.mMtu.shortValue() <= 1500) {
                s = this.mMtu.shortValue();
            }
            dhcpResults.mtu = s;
            dhcpResults.serverHostName = this.mServerHostName;
            return dhcpResults;
        } catch (IllegalArgumentException unused2) {
            return null;
        }
    }

    public long getLeaseTimeMillis() {
        Integer num = this.mLeaseTime;
        if (num == null || num.intValue() == -1) {
            return 0;
        }
        if (this.mLeaseTime.intValue() < 0 || this.mLeaseTime.intValue() >= 60) {
            return (((long) this.mLeaseTime.intValue()) & 4294967295L) * 1000;
        }
        return 60000;
    }

    public static ByteBuffer buildDiscoverPacket(int i, int i2, short s, byte[] bArr, boolean z, byte[] bArr2, String str) {
        Inet4Address inet4Address = INADDR_ANY;
        DhcpDiscoverPacket dhcpDiscoverPacket = new DhcpDiscoverPacket(i2, s, inet4Address, bArr, z, inet4Address);
        dhcpDiscoverPacket.mRequestedParams = bArr2;
        mHostNameByDeviceName = str;
        return dhcpDiscoverPacket.buildPacket(i, 67, 68);
    }

    public static ByteBuffer buildOfferPacket(int i, int i2, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr, Integer num, Inet4Address inet4Address4, Inet4Address inet4Address5, List<Inet4Address> list, List<Inet4Address> list2, Inet4Address inet4Address6, String str, String str2, boolean z2, short s) {
        DhcpOfferPacket dhcpOfferPacket = new DhcpOfferPacket(i2, 0, z, inet4Address, inet4Address2, INADDR_ANY, inet4Address3, bArr);
        dhcpOfferPacket.mGateways = list;
        dhcpOfferPacket.mDnsServers = list2;
        dhcpOfferPacket.mLeaseTime = num;
        dhcpOfferPacket.mDomainName = str;
        dhcpOfferPacket.mHostName = str2;
        dhcpOfferPacket.mServerIdentifier = inet4Address6;
        dhcpOfferPacket.mSubnetMask = inet4Address4;
        dhcpOfferPacket.mBroadcastAddress = inet4Address5;
        dhcpOfferPacket.mMtu = Short.valueOf(s);
        if (z2) {
            dhcpOfferPacket.mVendorInfo = "ANDROID_METERED";
        }
        return dhcpOfferPacket.buildPacket(i, 68, 67);
    }

    public static ByteBuffer buildAckPacket(int i, int i2, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr, Integer num, Inet4Address inet4Address5, Inet4Address inet4Address6, List<Inet4Address> list, List<Inet4Address> list2, Inet4Address inet4Address7, String str, String str2, boolean z2, short s) {
        DhcpAckPacket dhcpAckPacket = new DhcpAckPacket(i2, 0, z, inet4Address, inet4Address2, inet4Address4, inet4Address3, bArr);
        dhcpAckPacket.mGateways = list;
        dhcpAckPacket.mDnsServers = list2;
        dhcpAckPacket.mLeaseTime = num;
        dhcpAckPacket.mDomainName = str;
        dhcpAckPacket.mHostName = str2;
        dhcpAckPacket.mSubnetMask = inet4Address5;
        dhcpAckPacket.mServerIdentifier = inet4Address7;
        dhcpAckPacket.mBroadcastAddress = inet4Address6;
        dhcpAckPacket.mMtu = Short.valueOf(s);
        if (z2) {
            dhcpAckPacket.mVendorInfo = "ANDROID_METERED";
        }
        return dhcpAckPacket.buildPacket(i, 68, 67);
    }

    public static ByteBuffer buildNakPacket(int i, int i2, Inet4Address inet4Address, Inet4Address inet4Address2, byte[] bArr, boolean z, String str) {
        DhcpNakPacket dhcpNakPacket = new DhcpNakPacket(i2, 0, inet4Address2, bArr, z);
        dhcpNakPacket.mMessage = str;
        dhcpNakPacket.mServerIdentifier = inet4Address;
        return dhcpNakPacket.buildPacket(i, 68, 67);
    }

    public static ByteBuffer buildRequestPacket(int i, int i2, short s, Inet4Address inet4Address, boolean z, byte[] bArr, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr2, String str) {
        DhcpRequestPacket dhcpRequestPacket = new DhcpRequestPacket(i2, s, inet4Address, INADDR_ANY, bArr, z);
        dhcpRequestPacket.mRequestedIp = inet4Address2;
        dhcpRequestPacket.mServerIdentifier = inet4Address3;
        mHostNameByDeviceName = str;
        dhcpRequestPacket.mHostName = str;
        dhcpRequestPacket.mRequestedParams = bArr2;
        return dhcpRequestPacket.buildPacket(i, 67, 68);
    }

    public static ByteBuffer buildReleasePacket(int i, int i2, short s, Inet4Address inet4Address, byte[] bArr, Inet4Address inet4Address2) {
        return new DhcpReleasePacket(i2, inet4Address2, inet4Address, INADDR_ANY, bArr).buildPacket(i, 67, 68);
    }
}
