package android.net.shared;

import android.net.NetworkCapabilities;

public class NetworkMonitorUtils {
    public static boolean isPrivateDnsValidationRequired(NetworkCapabilities networkCapabilities) {
        return networkCapabilities != null && networkCapabilities.hasCapability(12) && networkCapabilities.hasCapability(13) && networkCapabilities.hasCapability(14);
    }

    public static boolean isValidationRequired(NetworkCapabilities networkCapabilities) {
        return isPrivateDnsValidationRequired(networkCapabilities) && networkCapabilities.hasCapability(15);
    }
}
