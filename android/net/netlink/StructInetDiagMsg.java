package android.net.netlink;

import java.nio.ByteBuffer;

public class StructInetDiagMsg {
    public int idiag_uid;

    public static StructInetDiagMsg parse(ByteBuffer byteBuffer) {
        StructInetDiagMsg structInetDiagMsg = new StructInetDiagMsg();
        structInetDiagMsg.idiag_uid = byteBuffer.getInt(80);
        return structInetDiagMsg;
    }

    public String toString() {
        return "StructInetDiagMsg{ idiag_uid{" + this.idiag_uid + "}, }";
    }
}
