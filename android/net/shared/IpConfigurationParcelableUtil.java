package android.net.shared;

import android.net.DhcpResultsParcelable;
import android.net.InetAddresses;
import android.net.networkstack.DhcpResults;
import java.net.InetAddress;

public final class IpConfigurationParcelableUtil {
    public static DhcpResultsParcelable toStableParcelable(DhcpResults dhcpResults) {
        if (dhcpResults == null) {
            return null;
        }
        DhcpResultsParcelable dhcpResultsParcelable = new DhcpResultsParcelable();
        dhcpResultsParcelable.baseConfiguration = dhcpResults.toStaticIpConfiguration();
        dhcpResultsParcelable.leaseDuration = dhcpResults.leaseDuration;
        dhcpResultsParcelable.mtu = dhcpResults.mtu;
        dhcpResultsParcelable.serverAddress = parcelAddress(dhcpResults.serverAddress);
        dhcpResultsParcelable.vendorInfo = dhcpResults.vendorInfo;
        dhcpResultsParcelable.serverHostName = dhcpResults.serverHostName;
        return dhcpResultsParcelable;
    }

    public static String parcelAddress(InetAddress inetAddress) {
        if (inetAddress == null) {
            return null;
        }
        return inetAddress.getHostAddress();
    }

    public static InetAddress unparcelAddress(String str) {
        if (str == null) {
            return null;
        }
        return InetAddresses.parseNumericAddress(str);
    }
}
