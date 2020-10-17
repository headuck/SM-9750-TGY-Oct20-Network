package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Iterator;

/* access modifiers changed from: package-private */
public class DhcpAckPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpAckPacket(int i, short s, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr) {
        super(i, s, inet4Address3, inet4Address4, inet4Address, inet4Address2, bArr, z);
        this.mBroadcast = z;
        this.mSrcIp = inet4Address;
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        Iterator<Inet4Address> it = this.mDnsServers.iterator();
        String str = " DNS servers: ";
        while (it.hasNext()) {
            str = str + it.next().toString() + " ";
        }
        return dhcpPacket + " ACK: your new IP " + this.mYourIp + ", netmask " + this.mSubnetMask + ", gateways " + this.mGateways + str + ", lease time " + this.mLeaseTime;
    }

    public ByteBuffer buildPacket(int i, short s, short s2) {
        ByteBuffer allocate = ByteBuffer.allocate(1500);
        fillInPacket(i, this.mBroadcast ? DhcpPacket.INADDR_BROADCAST : this.mYourIp, this.mBroadcast ? DhcpPacket.INADDR_ANY : this.mSrcIp, s, s2, allocate, (byte) 2, this.mBroadcast);
        allocate.flip();
        return allocate;
    }

    /* access modifiers changed from: package-private */
    @Override // android.net.dhcp.DhcpPacket
    public void finishPacket(ByteBuffer byteBuffer) {
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 5);
        DhcpPacket.addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addCommonServerTlvs(byteBuffer);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
