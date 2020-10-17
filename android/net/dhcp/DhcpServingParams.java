package android.net.dhcp;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.networkstack.shared.Inet4AddressUtils;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DhcpServingParams {
    public final Set<Inet4Address> defaultRouters;
    public final long dhcpLeaseTimeSecs;
    public final Set<Inet4Address> dnsServers;
    public final Set<Inet4Address> excludedAddrs;
    public final int linkMtu;
    public final boolean metered;
    public final LinkAddress serverAddr;

    public static class InvalidParameterException extends Exception {
        public InvalidParameterException(String str) {
            super(str);
        }
    }

    private DhcpServingParams(LinkAddress linkAddress, Set<Inet4Address> set, Set<Inet4Address> set2, Set<Inet4Address> set3, long j, int i, boolean z) {
        this.serverAddr = linkAddress;
        this.defaultRouters = set;
        this.dnsServers = set2;
        this.excludedAddrs = set3;
        this.dhcpLeaseTimeSecs = j;
        this.linkMtu = i;
        this.metered = z;
    }

    public static DhcpServingParams fromParcelableObject(DhcpServingParamsParcel dhcpServingParamsParcel) throws InvalidParameterException {
        if (dhcpServingParamsParcel != null) {
            LinkAddress linkAddress = new LinkAddress(Inet4AddressUtils.intToInet4AddressHTH(dhcpServingParamsParcel.serverAddr), dhcpServingParamsParcel.serverAddrPrefixLength);
            Builder builder = new Builder();
            builder.setServerAddr(linkAddress);
            builder.setDefaultRouters(toInet4AddressSet(dhcpServingParamsParcel.defaultRouters));
            builder.setDnsServers(toInet4AddressSet(dhcpServingParamsParcel.dnsServers));
            builder.setExcludedAddrs(toInet4AddressSet(dhcpServingParamsParcel.excludedAddrs));
            builder.setDhcpLeaseTimeSecs(dhcpServingParamsParcel.dhcpLeaseTimeSecs);
            builder.setLinkMtu(dhcpServingParamsParcel.linkMtu);
            builder.setMetered(dhcpServingParamsParcel.metered);
            return builder.build();
        }
        throw new InvalidParameterException("Null serving parameters");
    }

    private static Set<Inet4Address> toInet4AddressSet(int[] iArr) {
        if (iArr == null) {
            return new HashSet(0);
        }
        HashSet hashSet = new HashSet();
        for (int i : iArr) {
            hashSet.add(Inet4AddressUtils.intToInet4AddressHTH(i));
        }
        return hashSet;
    }

    public Inet4Address getServerInet4Addr() {
        return (Inet4Address) this.serverAddr.getAddress();
    }

    public Inet4Address getPrefixMaskAsAddress() {
        return Inet4AddressUtils.getPrefixMaskAsInet4Address(this.serverAddr.getPrefixLength());
    }

    public Inet4Address getBroadcastAddress() {
        return Inet4AddressUtils.getBroadcastAddress(getServerInet4Addr(), this.serverAddr.getPrefixLength());
    }

    public static class Builder {
        private Set<Inet4Address> mDefaultRouters;
        private long mDhcpLeaseTimeSecs;
        private Set<Inet4Address> mDnsServers;
        private Set<Inet4Address> mExcludedAddrs;
        private int mLinkMtu = 0;
        private boolean mMetered;
        private LinkAddress mServerAddr;

        public Builder setServerAddr(LinkAddress linkAddress) {
            this.mServerAddr = linkAddress;
            return this;
        }

        public Builder setDefaultRouters(Set<Inet4Address> set) {
            this.mDefaultRouters = set;
            return this;
        }

        public Builder setDnsServers(Set<Inet4Address> set) {
            this.mDnsServers = set;
            return this;
        }

        public Builder setExcludedAddrs(Set<Inet4Address> set) {
            this.mExcludedAddrs = set;
            return this;
        }

        public Builder setDhcpLeaseTimeSecs(long j) {
            this.mDhcpLeaseTimeSecs = j;
            return this;
        }

        public Builder setLinkMtu(int i) {
            this.mLinkMtu = i;
            return this;
        }

        public Builder setMetered(boolean z) {
            this.mMetered = z;
            return this;
        }

        public DhcpServingParams build() throws InvalidParameterException {
            if (this.mServerAddr == null) {
                throw new InvalidParameterException("Missing serverAddr");
            } else if (this.mDefaultRouters == null) {
                throw new InvalidParameterException("Missing defaultRouters");
            } else if (this.mDnsServers != null) {
                long j = this.mDhcpLeaseTimeSecs;
                if (j <= 0 || j > Integer.toUnsignedLong(-1)) {
                    throw new InvalidParameterException("Invalid lease time: " + this.mDhcpLeaseTimeSecs);
                }
                int i = this.mLinkMtu;
                if (i != 0 && (i < 68 || i > 65535)) {
                    throw new InvalidParameterException("Invalid link MTU: " + this.mLinkMtu);
                } else if (!this.mServerAddr.isIpv4()) {
                    throw new InvalidParameterException("serverAddr must be IPv4");
                } else if (this.mServerAddr.getPrefixLength() < 16 || this.mServerAddr.getPrefixLength() > 30) {
                    throw new InvalidParameterException("Prefix length is not in supported range");
                } else {
                    IpPrefix makeIpPrefix = DhcpServingParams.makeIpPrefix(this.mServerAddr);
                    for (Inet4Address inet4Address : this.mDefaultRouters) {
                        if (!makeIpPrefix.contains(inet4Address)) {
                            throw new InvalidParameterException(String.format("Default router %s is not in server prefix %s", inet4Address, this.mServerAddr));
                        }
                    }
                    HashSet hashSet = new HashSet();
                    Set<Inet4Address> set = this.mExcludedAddrs;
                    if (set != null) {
                        hashSet.addAll(set);
                    }
                    hashSet.add((Inet4Address) this.mServerAddr.getAddress());
                    hashSet.addAll(this.mDefaultRouters);
                    hashSet.addAll(this.mDnsServers);
                    return new DhcpServingParams(this.mServerAddr, Collections.unmodifiableSet(new HashSet(this.mDefaultRouters)), Collections.unmodifiableSet(new HashSet(this.mDnsServers)), Collections.unmodifiableSet(hashSet), this.mDhcpLeaseTimeSecs, this.mLinkMtu, this.mMetered);
                }
            } else {
                throw new InvalidParameterException("Missing dnsServers");
            }
        }
    }

    static IpPrefix makeIpPrefix(LinkAddress linkAddress) {
        return new IpPrefix(linkAddress.getAddress(), linkAddress.getPrefixLength());
    }
}
