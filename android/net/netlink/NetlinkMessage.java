package android.net.netlink;

import java.nio.ByteBuffer;

public class NetlinkMessage {
    protected StructNlMsgHdr mHeader;

    public static NetlinkMessage parse(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            byteBuffer.position();
        }
        StructNlMsgHdr parse = StructNlMsgHdr.parse(byteBuffer);
        if (parse == null) {
            return null;
        }
        int alignedLengthOf = NetlinkConstants.alignedLengthOf(parse.nlmsg_len) - 16;
        if (alignedLengthOf < 0 || alignedLengthOf > byteBuffer.remaining()) {
            byteBuffer.position(byteBuffer.limit());
            return null;
        }
        short s = parse.nlmsg_type;
        if (s == 2) {
            return NetlinkErrorMessage.parse(parse, byteBuffer);
        }
        if (s == 3) {
            byteBuffer.position(byteBuffer.position() + alignedLengthOf);
            return new NetlinkMessage(parse);
        } else if (s == 20) {
            return InetDiagMessage.parse(parse, byteBuffer);
        } else {
            switch (s) {
                case 28:
                case 29:
                case 30:
                    return RtNetlinkNeighborMessage.parse(parse, byteBuffer);
                default:
                    if (s > 15) {
                        return null;
                    }
                    byteBuffer.position(byteBuffer.position() + alignedLengthOf);
                    return new NetlinkMessage(parse);
            }
        }
    }

    public NetlinkMessage(StructNlMsgHdr structNlMsgHdr) {
        this.mHeader = structNlMsgHdr;
    }

    public StructNlMsgHdr getHeader() {
        return this.mHeader;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetlinkMessage{");
        StructNlMsgHdr structNlMsgHdr = this.mHeader;
        sb.append(structNlMsgHdr == null ? "" : structNlMsgHdr.toString());
        sb.append("}");
        return sb.toString();
    }
}
