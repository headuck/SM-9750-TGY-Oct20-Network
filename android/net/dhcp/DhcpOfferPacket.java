package android.net.dhcp;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

/* access modifiers changed from: package-private */
public class DhcpOfferPacket extends DhcpPacket {
    private final Inet4Address mSrcIp;

    DhcpOfferPacket(int i, short s, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr) {
        super(i, s, inet4Address3, inet4Address4, inet4Address, inet4Address2, bArr, z);
        this.mSrcIp = inet4Address;
    }

    @Override // android.net.dhcp.DhcpPacket
    public String toString() {
        String dhcpPacket = super.toString();
        List<Inet4Address> list = this.mDnsServers;
        String str = ", DNS servers: ";
        if (list != null) {
            Iterator<Inet4Address> it = list.iterator();
            while (it.hasNext()) {
                str = str + it.next() + " ";
            }
        }
        return dhcpPacket + " OFFER, ip " + this.mYourIp + ", mask " + this.mSubnetMask + str + ", gateways " + this.mGateways + " lease time " + this.mLeaseTime + ", domain " + this.mDomainName;
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
        DhcpPacket.addTlv(byteBuffer, (byte) 53, (byte) 2);
        DhcpPacket.addTlv(byteBuffer, (byte) 54, this.mServerIdentifier);
        addCommonServerTlvs(byteBuffer);
        DhcpPacket.addTlvEnd(byteBuffer);
    }
}
