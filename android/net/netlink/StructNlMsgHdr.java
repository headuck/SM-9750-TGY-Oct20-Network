package android.net.netlink;

import java.nio.ByteBuffer;

public class StructNlMsgHdr {
    public short nlmsg_flags = 0;
    public int nlmsg_len = 0;
    public int nlmsg_pid = 0;
    public int nlmsg_seq = 0;
    public short nlmsg_type = 0;

    public static String stringForNlMsgFlags(short s) {
        StringBuilder sb = new StringBuilder();
        if ((s & 1) != 0) {
            sb.append("NLM_F_REQUEST");
        }
        if ((s & 2) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NLM_F_MULTI");
        }
        if ((s & 4) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NLM_F_ACK");
        }
        if ((s & 8) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NLM_F_ECHO");
        }
        if ((s & 256) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NLM_F_ROOT");
        }
        if ((s & 512) != 0) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append("NLM_F_MATCH");
        }
        return sb.toString();
    }

    public static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 16;
    }

    public static StructNlMsgHdr parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNlMsgHdr structNlMsgHdr = new StructNlMsgHdr();
        structNlMsgHdr.nlmsg_len = byteBuffer.getInt();
        structNlMsgHdr.nlmsg_type = byteBuffer.getShort();
        structNlMsgHdr.nlmsg_flags = byteBuffer.getShort();
        structNlMsgHdr.nlmsg_seq = byteBuffer.getInt();
        structNlMsgHdr.nlmsg_pid = byteBuffer.getInt();
        if (structNlMsgHdr.nlmsg_len < 16) {
            return null;
        }
        return structNlMsgHdr;
    }

    public void pack(ByteBuffer byteBuffer) {
        byteBuffer.putInt(this.nlmsg_len);
        byteBuffer.putShort(this.nlmsg_type);
        byteBuffer.putShort(this.nlmsg_flags);
        byteBuffer.putInt(this.nlmsg_seq);
        byteBuffer.putInt(this.nlmsg_pid);
    }

    public String toString() {
        return "StructNlMsgHdr{ nlmsg_len{" + this.nlmsg_len + "}, nlmsg_type{" + ("" + ((int) this.nlmsg_type) + "(" + NetlinkConstants.stringForNlMsgType(this.nlmsg_type) + ")") + "}, nlmsg_flags{" + ("" + ((int) this.nlmsg_flags) + "(" + stringForNlMsgFlags(this.nlmsg_flags) + ")") + ")}, nlmsg_seq{" + this.nlmsg_seq + "}, nlmsg_pid{" + this.nlmsg_pid + "} }";
    }
}
