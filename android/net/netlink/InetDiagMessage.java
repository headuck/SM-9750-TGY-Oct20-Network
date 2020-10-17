package android.net.netlink;

import android.system.OsConstants;
import java.nio.ByteBuffer;

public class InetDiagMessage extends NetlinkMessage {
    private static final int[] FAMILY = {OsConstants.AF_INET6, OsConstants.AF_INET};
    public StructInetDiagMsg mStructInetDiagMsg = new StructInetDiagMsg();

    private InetDiagMessage(StructNlMsgHdr structNlMsgHdr) {
        super(structNlMsgHdr);
    }

    public static InetDiagMessage parse(StructNlMsgHdr structNlMsgHdr, ByteBuffer byteBuffer) {
        InetDiagMessage inetDiagMessage = new InetDiagMessage(structNlMsgHdr);
        inetDiagMessage.mStructInetDiagMsg = StructInetDiagMsg.parse(byteBuffer);
        return inetDiagMessage;
    }

    @Override // android.net.netlink.NetlinkMessage
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InetDiagMessage{ nlmsghdr{");
        StructNlMsgHdr structNlMsgHdr = this.mHeader;
        String str = "";
        sb.append(structNlMsgHdr == null ? str : structNlMsgHdr.toString());
        sb.append("}, inet_diag_msg{");
        StructInetDiagMsg structInetDiagMsg = this.mStructInetDiagMsg;
        if (structInetDiagMsg != null) {
            str = structInetDiagMsg.toString();
        }
        sb.append(str);
        sb.append("} }");
        return sb.toString();
    }
}
