package android.net.dhcp;

import android.net.MacAddress;
import android.net.networkstack.util.HexDump;
import android.text.TextUtils;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Objects;

public class DhcpLease {
    private final byte[] mClientId;
    private final long mExpTime;
    private final String mHostname;
    private final MacAddress mHwAddr;
    private final Inet4Address mNetAddr;

    public DhcpLease(byte[] bArr, MacAddress macAddress, Inet4Address inet4Address, long j, String str) {
        byte[] bArr2;
        if (bArr == null) {
            bArr2 = null;
        } else {
            bArr2 = Arrays.copyOf(bArr, bArr.length);
        }
        this.mClientId = bArr2;
        this.mHwAddr = macAddress;
        this.mNetAddr = inet4Address;
        this.mExpTime = j;
        this.mHostname = str;
    }

    public String getHostname() {
        return this.mHostname;
    }

    public Inet4Address getNetAddr() {
        return this.mNetAddr;
    }

    public long getExpTime() {
        return this.mExpTime;
    }

    public DhcpLease renewedLease(long j, String str) {
        byte[] bArr = this.mClientId;
        MacAddress macAddress = this.mHwAddr;
        Inet4Address inet4Address = this.mNetAddr;
        long max = Math.max(j, this.mExpTime);
        if (str == null) {
            str = this.mHostname;
        }
        return new DhcpLease(bArr, macAddress, inet4Address, max, str);
    }

    public boolean matchesClient(byte[] bArr, MacAddress macAddress) {
        byte[] bArr2 = this.mClientId;
        if (bArr2 != null) {
            return Arrays.equals(bArr2, bArr);
        }
        return bArr == null && this.mHwAddr.equals(macAddress);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DhcpLease)) {
            return false;
        }
        DhcpLease dhcpLease = (DhcpLease) obj;
        if (!Arrays.equals(this.mClientId, dhcpLease.mClientId) || !this.mHwAddr.equals(dhcpLease.mHwAddr) || !this.mNetAddr.equals(dhcpLease.mNetAddr) || this.mExpTime != dhcpLease.mExpTime || !TextUtils.equals(this.mHostname, dhcpLease.mHostname)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.mClientId, this.mHwAddr, this.mNetAddr, this.mHostname, Long.valueOf(this.mExpTime));
    }

    static String clientIdToString(byte[] bArr) {
        return bArr == null ? "null" : HexDump.toHexString(bArr);
    }

    static String inet4AddrToString(Inet4Address inet4Address) {
        return inet4Address == null ? "null" : inet4Address.getHostAddress();
    }

    public String toString() {
        return String.format("clientId: %s, hwAddr: %s, netAddr: %s, expTime: %d, hostname: %s", clientIdToString(this.mClientId), this.mHwAddr.toString(), inet4AddrToString(this.mNetAddr), Long.valueOf(this.mExpTime), this.mHostname);
    }
}
