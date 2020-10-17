package android.net.netlink;

import android.system.OsConstants;
import java.nio.ByteBuffer;

public class StructNdMsg {
    public static byte NTF_MASTER = 4;
    public static byte NTF_PROXY = 8;
    public static byte NTF_ROUTER = Byte.MIN_VALUE;
    public static byte NTF_SELF = 2;
    public static byte NTF_USE = 1;
    public byte ndm_family = ((byte) OsConstants.AF_UNSPEC);
    public byte ndm_flags;
    public int ndm_ifindex;
    public short ndm_state;
    public byte ndm_type;

    public static String stringForNudState(short s) {
        if (s == 0) {
            return "NUD_NONE";
        }
        if (s == 1) {
            return "NUD_INCOMPLETE";
        }
        if (s == 2) {
            return "NUD_REACHABLE";
        }
        if (s == 4) {
            return "NUD_STALE";
        }
        if (s == 8) {
            return "NUD_DELAY";
        }
        if (s == 16) {
            return "NUD_PROBE";
        }
        if (s == 32) {
            return "NUD_FAILED";
        }
        if (s == 64) {
            return "NUD_NOARP";
        }
        if (s == 128) {
            return "NUD_PERMANENT";
        }
        return "unknown NUD state: " + String.valueOf((int) s);
    }

    public static String stringForNudFlags(byte b) {
        StringBuilder sb = new StringBuilder();
        if ((NTF_USE & b) != 0) {
            sb.append("NTF_USE");
        }
        if ((NTF_SELF & b) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_SELF");
        }
        if ((NTF_MASTER & b) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_MASTER");
        }
        if ((NTF_PROXY & b) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_PROXY");
        }
        if ((b & NTF_ROUTER) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NTF_ROUTER");
        }
        return sb.toString();
    }

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 12;
    }

    public static StructNdMsg parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNdMsg structNdMsg = new StructNdMsg();
        structNdMsg.ndm_family = byteBuffer.get();
        byteBuffer.get();
        byteBuffer.getShort();
        structNdMsg.ndm_ifindex = byteBuffer.getInt();
        structNdMsg.ndm_state = byteBuffer.getShort();
        structNdMsg.ndm_flags = byteBuffer.get();
        structNdMsg.ndm_type = byteBuffer.get();
        return structNdMsg;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.put(this.ndm_family);
        byteBuffer.put((byte) 0);
        byteBuffer.putShort(0);
        byteBuffer.putInt(this.ndm_ifindex);
        byteBuffer.putShort(this.ndm_state);
        byteBuffer.put(this.ndm_flags);
        byteBuffer.put(this.ndm_type);
    }

    public String toString() {
        return "StructNdMsg{ family{" + NetlinkConstants.stringForAddressFamily(this.ndm_family) + "}, ifindex{" + this.ndm_ifindex + "}, state{" + ("" + ((int) this.ndm_state) + " (" + stringForNudState(this.ndm_state) + ")") + "}, flags{" + ("" + ((int) this.ndm_flags) + " (" + stringForNudFlags(this.ndm_flags) + ")") + "}, type{" + ((int) this.ndm_type) + "} }";
    }
}
