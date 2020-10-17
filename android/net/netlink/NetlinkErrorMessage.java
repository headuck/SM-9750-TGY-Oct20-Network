package android.net.netlink;

import java.nio.ByteBuffer;

public class NetlinkErrorMessage extends NetlinkMessage {
    private StructNlMsgErr mNlMsgErr = null;

    public static NetlinkErrorMessage parse(StructNlMsgHdr structNlMsgHdr, ByteBuffer byteBuffer) {
        NetlinkErrorMessage netlinkErrorMessage = new NetlinkErrorMessage(structNlMsgHdr);
        netlinkErrorMessage.mNlMsgErr = StructNlMsgErr.parse(byteBuffer);
        if (netlinkErrorMessage.mNlMsgErr == null) {
            return null;
        }
        return netlinkErrorMessage;
    }

    NetlinkErrorMessage(StructNlMsgHdr structNlMsgHdr) {
        super(structNlMsgHdr);
    }

    public StructNlMsgErr getNlMsgError() {
        return this.mNlMsgErr;
    }

    @Override // android.net.netlink.NetlinkMessage
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetlinkErrorMessage{ nlmsghdr{");
        StructNlMsgHdr structNlMsgHdr = this.mHeader;
        String str = "";
        sb.append(structNlMsgHdr == null ? str : structNlMsgHdr.toString());
        sb.append("}, nlmsgerr{");
        StructNlMsgErr structNlMsgErr = this.mNlMsgErr;
        if (structNlMsgErr != null) {
            str = structNlMsgErr.toString();
        }
        sb.append(str);
        sb.append("} }");
        return sb.toString();
    }
}
