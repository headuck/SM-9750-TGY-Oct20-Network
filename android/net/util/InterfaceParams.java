package android.net.util;

import android.net.MacAddress;
import android.net.networkstack.util.Preconditions;
import android.text.TextUtils;
import java.net.NetworkInterface;
import java.net.SocketException;

public class InterfaceParams {
    public final int defaultMtu;
    public final int index;
    public final MacAddress macAddr;
    public final String name;

    public static InterfaceParams getByName(String str) {
        NetworkInterface networkInterfaceByName = getNetworkInterfaceByName(str);
        if (networkInterfaceByName == null) {
            return null;
        }
        try {
            return new InterfaceParams(str, networkInterfaceByName.getIndex(), getMacAddress(networkInterfaceByName), networkInterfaceByName.getMTU());
        } catch (IllegalArgumentException | SocketException unused) {
            return null;
        }
    }

    public InterfaceParams(String str, int i, MacAddress macAddress, int i2) {
        boolean z = true;
        Preconditions.checkArgument(!TextUtils.isEmpty(str), "impossible interface name");
        Preconditions.checkArgument(i <= 0 ? false : z, "invalid interface index");
        this.name = str;
        this.index = i;
        this.macAddr = macAddress == null ? MacAddress.fromBytes(new byte[]{2, 0, 0, 0, 0, 0}) : macAddress;
        this.defaultMtu = i2 > 1280 ? i2 : 1280;
    }

    public String toString() {
        return String.format("%s/%d/%s/%d", this.name, Integer.valueOf(this.index), this.macAddr, Integer.valueOf(this.defaultMtu));
    }

    private static NetworkInterface getNetworkInterfaceByName(String str) {
        try {
            return NetworkInterface.getByName(str);
        } catch (NullPointerException | SocketException unused) {
            return null;
        }
    }

    private static MacAddress getMacAddress(NetworkInterface networkInterface) {
        try {
            return MacAddress.fromBytes(networkInterface.getHardwareAddress());
        } catch (IllegalArgumentException | NullPointerException | SocketException unused) {
            return null;
        }
    }
}
