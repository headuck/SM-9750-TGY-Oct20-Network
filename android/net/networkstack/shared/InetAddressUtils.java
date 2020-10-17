package android.net.networkstack.shared;

import android.os.Parcel;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressUtils {
    public static void parcelInetAddress(Parcel parcel, InetAddress inetAddress, int i) {
        parcel.writeByteArray(inetAddress != null ? inetAddress.getAddress() : null);
    }

    public static InetAddress unparcelInetAddress(Parcel parcel) {
        byte[] createByteArray = parcel.createByteArray();
        if (createByteArray == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(createByteArray);
        } catch (UnknownHostException unused) {
            return null;
        }
    }
}
