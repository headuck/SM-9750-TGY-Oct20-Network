package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* access modifiers changed from: package-private */
public class DhcpDiscoverPacket extends DhcpPacket {
    final Inet4Address mSrcIp;

    /* JADX WARNING: Illegal instructions before constructor call */
    DhcpDiscoverPacket(int i, short s, Inet4Address inet4Address, byte[] bArr, boolean z, Inet4Address inet4Address2) {
        super(i, s, r5, r5, r5, inet4Address, bArr, z);
        Inet4Address inet4Address3 = DhcpPacket.INADDR_ANY;
        this.mSrcIp = inet4Address2;
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(dhcpPacket);
        sb.append(" DISCOVER ");
        sb.append(this.mBroadcast ? "broadcast " : "unicast ");
        return sb.toString();
    }

    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer allocate = ByteBuffer.allocate(1500);
        fillInPacket(i, DhcpPacket.INADDR_BROADCAST, this.mSrcIp, s, s2, allocate, (byte) 1, this.mBroadcast);
        allocate.flip();
        return allocate;
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 1);
        DhcpPacket.addTlv(byteBuffer, (byte) 61, getClientId());
        addCommonClientTlvs(byteBuffer);
        DhcpPacket.addTlv(byteBuffer, (byte) 55, this.mRequestedParams);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
