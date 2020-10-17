package android.net.networkstack.shared;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Inet4AddressUtils {
    public static Inet4Address intToInet4AddressHTH(int i) {
        try {
            return (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) ((i >> 24) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 8) & 255), (byte) (i & 255)});
        } catch (UnknownHostException unused) {
            throw new AssertionError();
        }
    }

    public static int inet4AddressToIntHTH(Inet4Address inet4Address) throws IllegalArgumentException {
        byte[] address = inet4Address.getAddress();
        return (address[3] & 255) | ((address[0] & 255) << 24) | ((address[1] & 255) << 16) | ((address[2] & 255) << 8);
    }

    public static int prefixLengthToV4NetmaskIntHTH(int i) throws IllegalArgumentException {
        if (i < 0 || i > 32) {
            throw new IllegalArgumentException("Invalid prefix length (0 <= prefix <= 32)");
        } else if (i == 0) {
            return 0;
        } else {
            return -1 << (32 - i);
        }
    }

    public static int netmaskToPrefixLength(Inet4Address inet4Address) {
        int inet4AddressToIntHTH = inet4AddressToIntHTH(inet4Address);
        int bitCount = Integer.bitCount(inet4AddressToIntHTH);
        if (Integer.numberOfTrailingZeros(inet4AddressToIntHTH) == 32 - bitCount) {
            return bitCount;
        }
        throw new IllegalArgumentException("Non-contiguous netmask: " + Integer.toHexString(inet4AddressToIntHTH));
    }

    public static int getImplicitNetmask(Inet4Address inet4Address) {
        int i = inet4Address.getAddress()[0] & 255;
        if (i < 128) {
            return 8;
        }
        if (i < 192) {
            return 16;
        }
        return i < 224 ? 24 : 32;
    }

    public static Inet4Address getBroadcastAddress(Inet4Address inet4Address, int i) throws IllegalArgumentException {
        return intToInet4AddressHTH(inet4AddressToIntHTH(inet4Address) | (~prefixLengthToV4NetmaskIntHTH(i)));
    }

    public static Inet4Address getPrefixMaskAsInet4Address(int i) throws IllegalArgumentException {
        return intToInet4AddressHTH(prefixLengthToV4NetmaskIntHTH(i));
    }
}
