package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* access modifiers changed from: package-private */
public class DhcpNakPacket extends DhcpPacket {
    /* JADX WARNING: Illegal instructions before constructor call */
    DhcpNakPacket(int i, short s, Inet4Address inet4Address, byte[] bArr, boolean z) {
        super(i, s, r5, r5, r5, inet4Address, bArr, z);
        Inet4Address inet4Address2 = DhcpPacket.INADDR_ANY;
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(dhcpPacket);
        sb.append(" NAK, reason ");
        String str = this.mMessage;
        if (str == null) {
            str = "(none)";
        }
        sb.append(str);
        return sb.toString();
    }

    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer allocate = ByteBuffer.allocate(1500);
        Inet4Address inet4Address = DhcpPacket.INADDR_ANY;
        fillInPacket(i, inet4Address, inet4Address, s, s2, allocate, (byte) 2, this.mBroadcast);
        allocate.flip();
        return allocate;
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 6);
        DhcpPacket.addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        DhcpPacket.addTlv(byteBuffer, (byte) 56, this.mMessage);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
