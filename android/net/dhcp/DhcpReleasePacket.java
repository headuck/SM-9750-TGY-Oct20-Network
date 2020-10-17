package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;

/* access modifiers changed from: package-private */
public class DhcpReleasePacket extends DhcpPacket {
    final Inet4Address mClientAddr;

    /* JADX WARNING: Illegal instructions before constructor call */
    public DhcpReleasePacket(int i, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr) {
        super(i, 0, inet4Address2, r5, r5, inet4Address3, bArr, false);
        Inet4Address inet4Address4 = DhcpPacket.INADDR_ANY;
        this.mServerIdentifier = inet4Address;
        this.mClientAddr = inet4Address2;
    }

    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer allocate = ByteBuffer.allocate(1500);
        fillInPacket(i, this.mServerIdentifier, this.mClientIp, s, s2, allocate, (byte) 2, this.mBroadcast);
        allocate.flip();
        return allocate;
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 7);
        DhcpPacket.addTlv(byteBuffer, (byte) 61, getClientId());
        DhcpPacket.addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addCommonClientTlvs(byteBuffer);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
