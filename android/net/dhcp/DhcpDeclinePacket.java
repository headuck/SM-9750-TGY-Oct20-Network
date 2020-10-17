package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* access modifiers changed from: package-private */
public class DhcpDeclinePacket extends DhcpPacket {
    DhcpDeclinePacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr) {
        super(i, s, inet4Address, inet4Address2, inet4Address3, inet4Address4, bArr, false);
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        return dhcpPacket + " DECLINE";
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 4);
        DhcpPacket.addTlv(byteBuffer, (byte) 61, getClientId());
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
