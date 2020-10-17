package android.net.shared;

import android.net.PrivateDnsConfigParcel;
import android.text.TextUtils;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

public class PrivateDnsConfig {
    public final String hostname;
    public final InetAddress[] ips;
    public final boolean useTls;

    public PrivateDnsConfig() {
        this(false);
    }

    public PrivateDnsConfig(boolean z) {
        this.useTls = z;
        this.hostname = "";
        this.ips = new InetAddress[0];
    }

    public PrivateDnsConfig(String str, InetAddress[] inetAddressArr) {
        this.useTls = !TextUtils.isEmpty(str);
        this.hostname = !this.useTls ? "" : str;
        this.ips = inetAddressArr == null ? new InetAddress[0] : inetAddressArr;
    }

    public boolean inStrictMode() {
        return this.useTls && !TextUtils.isEmpty(this.hostname);
    }

    public String toString() {
        return PrivateDnsConfig.class.getSimpleName() + "{" + this.useTls + ":" + this.hostname + "/" + Arrays.toString(this.ips) + "}";
    }

    public PrivateDnsConfigParcel toParcel() {
        PrivateDnsConfigParcel privateDnsConfigParcel = new PrivateDnsConfigParcel();
        privateDnsConfigParcel.hostname = this.hostname;
        privateDnsConfigParcel.ips = (String[]) ParcelableUtil.toParcelableArray(Arrays.asList(this.ips), $$Lambda$OsobWheG5dMvEj_cOJtueqUBqBI.INSTANCE, String.class);
        return privateDnsConfigParcel;
    }

    public static PrivateDnsConfig fromParcel(PrivateDnsConfigParcel privateDnsConfigParcel) {
        String[] strArr = privateDnsConfigParcel.ips;
        ArrayList fromParcelableArray = ParcelableUtil.fromParcelableArray(strArr, $$Lambda$SYWvjOUPlAZ_O2Z6yfFU9np1858.INSTANCE);
        return new PrivateDnsConfig(privateDnsConfigParcel.hostname, (InetAddress[]) fromParcelableArray.toArray(new InetAddress[strArr.length]));
    }
}
