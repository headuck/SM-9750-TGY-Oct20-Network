package android.net.networkstack;

import android.net.LinkAddress;
import android.net.StaticIpConfiguration;
import android.net.networkstack.shared.InetAddressUtils;
import android.os.Parcel;
import android.os.Parcelable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;

public final class DhcpResults implements Parcelable {
    public static final Parcelable.Creator<DhcpResults> CREATOR = new Parcelable.Creator<DhcpResults>() {
        /* class android.net.networkstack.DhcpResults.C00221 */

        @Override // android.os.Parcelable.Creator
        public DhcpResults createFromParcel(Parcel parcel) {
            return DhcpResults.readFromParcel(parcel);
        }

        @Override // android.os.Parcelable.Creator
        public DhcpResults[] newArray(int i) {
            return new DhcpResults[i];
        }
    };
    public final ArrayList<InetAddress> dnsServers;
    public String domains;
    public InetAddress gateway;
    public LinkAddress ipAddress;
    public int leaseDuration;
    public int mtu;
    public Inet4Address serverAddress;
    public String serverHostName;
    public String vendorInfo;

    public int describeContents() {
        return 0;
    }

    public DhcpResults() {
        this.dnsServers = new ArrayList<>();
    }

    public StaticIpConfiguration toStaticIpConfiguration() {
        return new StaticIpConfiguration.Builder().setIpAddress(this.ipAddress).setGateway(this.gateway).setDnsServers(this.dnsServers).setDomains(this.domains).build();
    }

    public DhcpResults(StaticIpConfiguration staticIpConfiguration) {
        this.dnsServers = new ArrayList<>();
        if (staticIpConfiguration != null) {
            this.ipAddress = staticIpConfiguration.getIpAddress();
            this.gateway = staticIpConfiguration.getGateway();
            this.dnsServers.addAll(staticIpConfiguration.getDnsServers());
            this.domains = staticIpConfiguration.getDomains();
        }
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    public DhcpResults(DhcpResults dhcpResults) {
        this(dhcpResults == null ? null : dhcpResults.toStaticIpConfiguration());
        if (dhcpResults != null) {
            this.serverAddress = dhcpResults.serverAddress;
            this.vendorInfo = dhcpResults.vendorInfo;
            this.leaseDuration = dhcpResults.leaseDuration;
            this.mtu = dhcpResults.mtu;
            this.serverHostName = dhcpResults.serverHostName;
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(super.toString());
        stringBuffer.append(" DHCP server ");
        stringBuffer.append(this.serverAddress);
        stringBuffer.append(" Vendor info ");
        stringBuffer.append(this.vendorInfo);
        stringBuffer.append(" lease ");
        stringBuffer.append(this.leaseDuration);
        stringBuffer.append(" seconds");
        if (this.mtu != 0) {
            stringBuffer.append(" MTU ");
            stringBuffer.append(this.mtu);
        }
        stringBuffer.append(" Servername ");
        stringBuffer.append(this.serverHostName);
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpResults)) {
            return false;
        }
        DhcpResults dhcpResults = (DhcpResults) obj;
        return toStaticIpConfiguration().equals(dhcpResults.toStaticIpConfiguration()) && Objects.equals(this.serverAddress, dhcpResults.serverAddress) && Objects.equals(this.vendorInfo, dhcpResults.vendorInfo) && Objects.equals(this.serverHostName, dhcpResults.serverHostName) && this.leaseDuration == dhcpResults.leaseDuration && this.mtu == dhcpResults.mtu;
    }

    public void writeToParcel(Parcel parcel, int i) {
        toStaticIpConfiguration().writeToParcel(parcel, i);
        parcel.writeInt(this.leaseDuration);
        parcel.writeInt(this.mtu);
        InetAddressUtils.parcelInetAddress(parcel, this.serverAddress, i);
        parcel.writeString(this.vendorInfo);
        parcel.writeString(this.serverHostName);
    }

    /* access modifiers changed from: private */
    public static DhcpResults readFromParcel(Parcel parcel) {
        DhcpResults dhcpResults = new DhcpResults((StaticIpConfiguration) StaticIpConfiguration.CREATOR.createFromParcel(parcel));
        dhcpResults.leaseDuration = parcel.readInt();
        dhcpResults.mtu = parcel.readInt();
        dhcpResults.serverAddress = (Inet4Address) InetAddressUtils.unparcelInetAddress(parcel);
        dhcpResults.vendorInfo = parcel.readString();
        dhcpResults.serverHostName = parcel.readString();
        return dhcpResults;
    }
}
