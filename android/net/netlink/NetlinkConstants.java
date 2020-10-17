package android.net.netlink;

import android.net.networkstack.util.HexDump;
import android.system.OsConstants;
import java.nio.ByteBuffer;

public class NetlinkConstants {
    public static final int alignedLengthOf(short s) {
        return alignedLengthOf(s & 65535);
    }

    public static final int alignedLengthOf(int i) {
        if (i <= 0) {
            return 0;
        }
        return (((i + 4) - 1) / 4) * 4;
    }

    public static String stringForAddressFamily(int i) {
        if (i == OsConstants.AF_INET) {
            return "AF_INET";
        }
        if (i == OsConstants.AF_INET6) {
            return "AF_INET6";
        }
        if (i == OsConstants.AF_NETLINK) {
            return "AF_NETLINK";
        }
        return String.valueOf(i);
    }

    public static String hexify(byte[] bArr) {
        return bArr == null ? "(null)" : HexDump.toHexString(bArr);
    }

    public static String hexify(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return "(null)";
        }
        return HexDump.toHexString(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
    }

    public static String stringForNlMsgType(short s) {
        if (s == 1) {
            return "NLMSG_NOOP";
        }
        if (s == 2) {
            return "NLMSG_ERROR";
        }
        if (s == 3) {
            return "NLMSG_DONE";
        }
        if (s == 4) {
            return "NLMSG_OVERRUN";
        }
        if (s == 68) {
            return "RTM_NEWNDUSEROPT";
        }
        switch (s) {
            case 16:
                return "RTM_NEWLINK";
            case 17:
                return "RTM_DELLINK";
            case 18:
                return "RTM_GETLINK";
            case 19:
                return "RTM_SETLINK";
            case 20:
                return "RTM_NEWADDR";
            case 21:
                return "RTM_DELADDR";
            case 22:
                return "RTM_GETADDR";
            default:
                switch (s) {
                    case 24:
                        return "RTM_NEWROUTE";
                    case 25:
                        return "RTM_DELROUTE";
                    case 26:
                        return "RTM_GETROUTE";
                    default:
                        switch (s) {
                            case 28:
                                return "RTM_NEWNEIGH";
                            case 29:
                                return "RTM_DELNEIGH";
                            case 30:
                                return "RTM_GETNEIGH";
                            default:
                                switch (s) {
                                    case 32:
                                        return "RTM_NEWRULE";
                                    case 33:
                                        return "RTM_DELRULE";
                                    case 34:
                                        return "RTM_GETRULE";
                                    default:
                                        return "unknown RTM type: " + String.valueOf((int) s);
                                }
                        }
                }
        }
    }
}
