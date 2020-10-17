package android.net.util;

import android.net.MacAddress;
import android.net.dhcp.DhcpPacket;
import android.system.OsConstants;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;

public class ConnectivityPacketSummary {
    private final byte[] mBytes;
    private final byte[] mHwAddr;
    private final int mLength;
    private final ByteBuffer mPacket = ByteBuffer.wrap(this.mBytes, 0, this.mLength);
    private final String mSummary;

    public static int asUint(byte b) {
        return b & 255;
    }

    public static int asUint(short s) {
        return s & 65535;
    }

    public static String summarize(MacAddress macAddress, byte[] bArr, int i) {
        if (macAddress == null || bArr == null) {
            return null;
        }
        return new ConnectivityPacketSummary(macAddress, bArr, Math.min(i, bArr.length)).toString();
    }

    private ConnectivityPacketSummary(MacAddress macAddress, byte[] bArr, int i) {
        this.mHwAddr = macAddress.toByteArray();
        this.mBytes = bArr;
        this.mLength = Math.min(i, this.mBytes.length);
        this.mPacket.order(ByteOrder.BIG_ENDIAN);
        StringJoiner stringJoiner = new StringJoiner(" ");
        parseEther(stringJoiner);
        this.mSummary = stringJoiner.toString();
    }

    public String toString() {
        return this.mSummary;
    }

    private void parseEther(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 14) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        this.mPacket.position(6);
        ByteBuffer byteBuffer = (ByteBuffer) this.mPacket.slice().limit(6);
        stringJoiner.add(ByteBuffer.wrap(this.mHwAddr).equals(byteBuffer) ? "TX" : "RX");
        stringJoiner.add(getMacAddressString(byteBuffer));
        this.mPacket.position(0);
        stringJoiner.add(">").add(getMacAddressString((ByteBuffer) this.mPacket.slice().limit(6)));
        this.mPacket.position(12);
        int asUint = asUint(this.mPacket.getShort());
        if (asUint == 2048) {
            stringJoiner.add("ipv4");
            parseIPv4(stringJoiner);
        } else if (asUint == 2054) {
            stringJoiner.add("arp");
            parseARP(stringJoiner);
        } else if (asUint != 34525) {
            stringJoiner.add("ethtype").add(asString(asUint));
        } else {
            stringJoiner.add("ipv6");
            parseIPv6(stringJoiner);
        }
    }

    private void parseARP(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 28) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
        } else if (asUint(this.mPacket.getShort()) == 1 && asUint(this.mPacket.getShort()) == 2048 && asUint(this.mPacket.get()) == 6 && asUint(this.mPacket.get()) == 4) {
            int asUint = asUint(this.mPacket.getShort());
            String macAddressString = getMacAddressString(this.mPacket);
            String iPv4AddressString = getIPv4AddressString(this.mPacket);
            getMacAddressString(this.mPacket);
            String iPv4AddressString2 = getIPv4AddressString(this.mPacket);
            if (asUint == 1) {
                stringJoiner.add("who-has").add(iPv4AddressString2);
            } else if (asUint == 2) {
                stringJoiner.add("reply").add(iPv4AddressString).add(macAddressString);
            } else {
                stringJoiner.add("unknown opcode").add(asString(asUint));
            }
        } else {
            stringJoiner.add("unexpected header");
        }
    }

    private void parseIPv4(StringJoiner stringJoiner) {
        if (!this.mPacket.hasRemaining()) {
            stringJoiner.add("runt");
            return;
        }
        int position = this.mPacket.position();
        int i = (this.mPacket.get(position) & 15) * 4;
        if (this.mPacket.remaining() < i || this.mPacket.remaining() < 20) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int i2 = i + position;
        this.mPacket.position(position + 6);
        boolean z = (asUint(this.mPacket.getShort()) & 8191) != 0;
        this.mPacket.position(position + 9);
        int asUint = asUint(this.mPacket.get());
        this.mPacket.position(position + 12);
        String iPv4AddressString = getIPv4AddressString(this.mPacket);
        this.mPacket.position(position + 16);
        stringJoiner.add(iPv4AddressString).add(">").add(getIPv4AddressString(this.mPacket));
        this.mPacket.position(i2);
        if (asUint == OsConstants.IPPROTO_UDP) {
            stringJoiner.add("udp");
            if (z) {
                stringJoiner.add("fragment");
            } else {
                parseUDP(stringJoiner);
            }
        } else {
            stringJoiner.add("proto").add(asString(asUint));
            if (z) {
                stringJoiner.add("fragment");
            }
        }
    }

    private void parseIPv6(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 40) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int position = this.mPacket.position();
        this.mPacket.position(position + 6);
        int asUint = asUint(this.mPacket.get());
        this.mPacket.position(position + 8);
        String iPv6AddressString = getIPv6AddressString(this.mPacket);
        stringJoiner.add(iPv6AddressString).add(">").add(getIPv6AddressString(this.mPacket));
        this.mPacket.position(position + 40);
        if (asUint == OsConstants.IPPROTO_ICMPV6) {
            stringJoiner.add("icmp6");
            parseICMPv6(stringJoiner);
            return;
        }
        stringJoiner.add("proto").add(asString(asUint));
    }

    private void parseICMPv6(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 4) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int asUint = asUint(this.mPacket.get());
        int asUint2 = asUint(this.mPacket.get());
        this.mPacket.getShort();
        switch (asUint) {
            case 133:
                stringJoiner.add("rs");
                parseICMPv6RouterSolicitation(stringJoiner);
                return;
            case 134:
                stringJoiner.add("ra");
                parseICMPv6RouterAdvertisement(stringJoiner);
                return;
            case 135:
                stringJoiner.add("ns");
                parseICMPv6NeighborMessage(stringJoiner);
                return;
            case 136:
                stringJoiner.add("na");
                parseICMPv6NeighborMessage(stringJoiner);
                return;
            default:
                stringJoiner.add("type").add(asString(asUint));
                stringJoiner.add("code").add(asString(asUint2));
                return;
        }
    }

    private void parseICMPv6RouterSolicitation(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 4) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 4);
        parseICMPv6NeighborDiscoveryOptions(stringJoiner);
    }

    private void parseICMPv6RouterAdvertisement(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 12) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 12);
        parseICMPv6NeighborDiscoveryOptions(stringJoiner);
    }

    private void parseICMPv6NeighborMessage(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 20) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        ByteBuffer byteBuffer = this.mPacket;
        byteBuffer.position(byteBuffer.position() + 4);
        stringJoiner.add(getIPv6AddressString(this.mPacket));
        parseICMPv6NeighborDiscoveryOptions(stringJoiner);
    }

    private void parseICMPv6NeighborDiscoveryOptions(StringJoiner stringJoiner) {
        while (this.mPacket.remaining() >= 8) {
            int asUint = asUint(this.mPacket.get());
            int asUint2 = (asUint(this.mPacket.get()) * 8) - 2;
            if (asUint2 < 0 || asUint2 > this.mPacket.remaining()) {
                stringJoiner.add("<malformed>");
                return;
            }
            int position = this.mPacket.position();
            if (asUint == 1) {
                stringJoiner.add("slla");
                stringJoiner.add(getMacAddressString(this.mPacket));
            } else if (asUint == 2) {
                stringJoiner.add("tlla");
                stringJoiner.add(getMacAddressString(this.mPacket));
            } else if (asUint == 5) {
                stringJoiner.add("mtu");
                this.mPacket.getShort();
                stringJoiner.add(asString(this.mPacket.getInt()));
            }
            this.mPacket.position(position + asUint2);
        }
    }

    private void parseUDP(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 8) {
            stringJoiner.add("runt:").add(asString(this.mPacket.remaining()));
            return;
        }
        int position = this.mPacket.position();
        int asUint = asUint(this.mPacket.getShort());
        int asUint2 = asUint(this.mPacket.getShort());
        stringJoiner.add(asString(asUint)).add(">").add(asString(asUint2));
        this.mPacket.position(position + 8);
        if (asUint == 68 || asUint2 == 68) {
            stringJoiner.add("dhcp4");
            parseDHCPv4(stringJoiner);
        }
    }

    private void parseDHCPv4(StringJoiner stringJoiner) {
        try {
            stringJoiner.add(DhcpPacket.decodeFullPacket(this.mBytes, this.mLength, 0).toString());
        } catch (DhcpPacket.ParseException e) {
            stringJoiner.add("parse error: " + e);
        }
    }

    private static String getIPv4AddressString(ByteBuffer byteBuffer) {
        return getIpAddressString(byteBuffer, 4);
    }

    private static String getIPv6AddressString(ByteBuffer byteBuffer) {
        return getIpAddressString(byteBuffer, 16);
    }

    private static String getIpAddressString(ByteBuffer byteBuffer, int i) {
        if (byteBuffer == null || byteBuffer.remaining() < i) {
            return "invalid";
        }
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr, 0, i);
        try {
            return InetAddress.getByAddress(bArr).getHostAddress();
        } catch (UnknownHostException unused) {
            return "unknown";
        }
    }

    private static String getMacAddressString(ByteBuffer byteBuffer) {
        if (byteBuffer == null || byteBuffer.remaining() < 6) {
            return "invalid";
        }
        byte[] bArr = new byte[6];
        int i = 0;
        byteBuffer.get(bArr, 0, bArr.length);
        Object[] objArr = new Object[bArr.length];
        int length = bArr.length;
        int i2 = 0;
        while (i < length) {
            objArr[i2] = new Byte(bArr[i]);
            i++;
            i2++;
        }
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x", objArr);
    }

    public static String asString(int i) {
        return Integer.toString(i);
    }
}
