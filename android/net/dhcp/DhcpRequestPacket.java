package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* access modifiers changed from: package-private */
public class DhcpRequestPacket extends DhcpPacket {
    /* JADX WARNING: Illegal instructions before constructor call */
    DhcpRequestPacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, byte[] bArr, boolean z) {
        super(i, s, inet4Address, r5, r5, inet4Address2, bArr, z);
        Inet4Address inet4Address3 = DhcpPacket.INADDR_ANY;
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(dhcpPacket);
        sb.append(" REQUEST, desired IP ");
        sb.append(this.mRequestedIp);
        sb.append(" from host '");
        sb.append(this.mHostName);
        sb.append("', param list length ");
        byte[] bArr = this.mRequestedParams;
        sb.append(bArr == null ? 0 : bArr.length);
        return sb.toString();
    }

    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer allocate = ByteBuffer.allocate(1500);
        fillInPacket(i, DhcpPacket.INADDR_BROADCAST, DhcpPacket.INADDR_ANY, s, s2, allocate, (byte) 1, this.mBroadcast);
        allocate.flip();
        return allocate;
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 3);
        DhcpPacket.addTlv(byteBuffer, (byte) 61, getClientId());
        if (!DhcpPacket.INADDR_ANY.equals(this.mRequestedIp)) {
            DhcpPacket.addTlv(byteBuffer, (byte) 50, this.mRequestedIp);
        }
        if (!DhcpPacket.INADDR_ANY.equals(this.mServerIdentifier)) {
            DhcpPacket.addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        }
        addCommonClientTlvs(byteBuffer);
        DhcpPacket.addTlv(byteBuffer, (byte) 55, this.mRequestedParams);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
