package android.net.dhcp;

import android.net.IpPrefix;
import android.net.MacAddress;
import android.net.dhcp.DhcpServer;
import android.net.networkstack.shared.Inet4AddressUtils;
import android.net.util.SharedLog;
import android.util.ArrayMap;
import com.android.server.util.NetworkStackConstants;
import java.net.Inet4Address;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/* access modifiers changed from: package-private */
public class DhcpLeaseRepository {
    private final DhcpServer.Clock mClock;
    private final ArrayMap<Inet4Address, DhcpLease> mCommittedLeases = new ArrayMap<>();
    private final LinkedHashMap<Inet4Address, Long> mDeclinedAddrs = new LinkedHashMap<>();
    private long mLeaseTimeMs;
    private final SharedLog mLog;
    private long mNextExpirationCheck = Long.MAX_VALUE;
    private int mNumAddresses;
    private IpPrefix mPrefix;
    private Set<Inet4Address> mReservedAddrs;
    private int mSubnetAddr;
    private int mSubnetMask;

    static class DhcpLeaseException extends Exception {
        DhcpLeaseException(String str) {
            super(str);
        }
    }

    /* access modifiers changed from: package-private */
    public static class OutOfAddressesException extends DhcpLeaseException {
        OutOfAddressesException(String str) {
            super(str);
        }
    }

    /* access modifiers changed from: package-private */
    public static class InvalidAddressException extends DhcpLeaseException {
        InvalidAddressException(String str) {
            super(str);
        }
    }

    /* access modifiers changed from: package-private */
    public static class InvalidSubnetException extends DhcpLeaseException {
        InvalidSubnetException(String str) {
            super(str);
        }
    }

    DhcpLeaseRepository(IpPrefix ipPrefix, Set<Inet4Address> set, long j, SharedLog sharedLog, DhcpServer.Clock clock) {
        updateParams(ipPrefix, set, j);
        this.mLog = sharedLog;
        this.mClock = clock;
    }

    public void updateParams(IpPrefix ipPrefix, Set<Inet4Address> set, long j) {
        this.mPrefix = ipPrefix;
        this.mReservedAddrs = Collections.unmodifiableSet(new HashSet(set));
        this.mSubnetMask = Inet4AddressUtils.prefixLengthToV4NetmaskIntHTH(ipPrefix.getPrefixLength());
        this.mSubnetAddr = Inet4AddressUtils.inet4AddressToIntHTH((Inet4Address) ipPrefix.getAddress()) & this.mSubnetMask;
        this.mNumAddresses = 1 << (32 - ipPrefix.getPrefixLength());
        this.mLeaseTimeMs = j;
        cleanMap(this.mCommittedLeases);
        cleanMap(this.mDeclinedAddrs);
    }

    private <T> void cleanMap(Map<Inet4Address, T> map) {
        Iterator<Map.Entry<Inet4Address, T>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Inet4Address key = it.next().getKey();
            if (!isValidAddress(key) || this.mReservedAddrs.contains(key)) {
                it.remove();
            }
        }
    }

    public DhcpLease getOffer(byte[] bArr, MacAddress macAddress, Inet4Address inet4Address, Inet4Address inet4Address2, String str) throws OutOfAddressesException, InvalidSubnetException {
        long elapsedRealtime = this.mClock.elapsedRealtime();
        long j = elapsedRealtime + this.mLeaseTimeMs;
        removeExpiredLeases(elapsedRealtime);
        checkValidRelayAddr(inet4Address);
        DhcpLease findByClient = findByClient(bArr, macAddress);
        if (findByClient != null) {
            DhcpLease renewedLease = findByClient.renewedLease(j, str);
            SharedLog sharedLog = this.mLog;
            sharedLog.log("Offering extended lease " + renewedLease);
            return renewedLease;
        } else if (inet4Address2 == null || !isValidAddress(inet4Address2) || !isAvailable(inet4Address2)) {
            DhcpLease makeNewOffer = makeNewOffer(bArr, macAddress, j, str);
            SharedLog sharedLog2 = this.mLog;
            sharedLog2.log("Offering new generated lease " + makeNewOffer);
            return makeNewOffer;
        } else {
            DhcpLease dhcpLease = new DhcpLease(bArr, macAddress, inet4Address2, j, str);
            SharedLog sharedLog3 = this.mLog;
            sharedLog3.log("Offering requested lease " + dhcpLease);
            return dhcpLease;
        }
    }

    private void checkValidRelayAddr(Inet4Address inet4Address) throws InvalidSubnetException {
        if (isIpAddrOutsidePrefix(this.mPrefix, inet4Address)) {
            throw new InvalidSubnetException("Lease requested by relay from outside of subnet");
        }
    }

    private static boolean isIpAddrOutsidePrefix(IpPrefix ipPrefix, Inet4Address inet4Address) {
        return inet4Address != null && !inet4Address.equals(NetworkStackConstants.IPV4_ADDR_ANY) && !ipPrefix.contains(inet4Address);
    }

    private DhcpLease findByClient(byte[] bArr, MacAddress macAddress) {
        for (DhcpLease dhcpLease : this.mCommittedLeases.values()) {
            if (dhcpLease.matchesClient(bArr, macAddress)) {
                return dhcpLease;
            }
        }
        return null;
    }

    public DhcpLease requestLease(byte[] bArr, MacAddress macAddress, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, boolean z, String str) throws InvalidAddressException, InvalidSubnetException {
        long elapsedRealtime = this.mClock.elapsedRealtime();
        removeExpiredLeases(elapsedRealtime);
        checkValidRelayAddr(inet4Address2);
        DhcpLease findByClient = findByClient(bArr, macAddress);
        Inet4Address inet4Address4 = inet4Address3 != null ? inet4Address3 : inet4Address;
        if (findByClient != null) {
            if (z && inet4Address3 != null) {
                this.mCommittedLeases.remove(findByClient.getNetAddr());
            } else if (!findByClient.getNetAddr().equals(inet4Address4)) {
                StringBuilder sb = new StringBuilder();
                sb.append("Incorrect address for client in ");
                sb.append(inet4Address3 != null ? "INIT-REBOOT" : "RENEWING/REBINDING");
                throw new InvalidAddressException(sb.toString());
            }
        }
        DhcpLease checkClientAndMakeLease = checkClientAndMakeLease(bArr, macAddress, inet4Address4, str, elapsedRealtime);
        this.mLog.logf("DHCPREQUEST assignedLease %s, reqAddr=%s, sidSet=%s: created/renewed lease %s", findByClient, DhcpLease.inet4AddrToString(inet4Address3), Boolean.valueOf(z), checkClientAndMakeLease);
        return checkClientAndMakeLease;
    }

    private DhcpLease checkClientAndMakeLease(byte[] bArr, MacAddress macAddress, Inet4Address inet4Address, String str, long j) throws InvalidAddressException {
        DhcpLease dhcpLease;
        long j2 = j + this.mLeaseTimeMs;
        DhcpLease orDefault = this.mCommittedLeases.getOrDefault(inet4Address, null);
        if (orDefault == null || orDefault.matchesClient(bArr, macAddress)) {
            if (orDefault != null) {
                dhcpLease = orDefault.renewedLease(j2, str);
            } else if (!isValidAddress(inet4Address) || this.mReservedAddrs.contains(inet4Address)) {
                throw new InvalidAddressException("Lease not found and address unavailable");
            } else {
                dhcpLease = new DhcpLease(bArr, macAddress, inet4Address, j2, str);
            }
            commitLease(dhcpLease);
            return dhcpLease;
        }
        throw new InvalidAddressException("Address in use");
    }

    private void commitLease(DhcpLease dhcpLease) {
        this.mCommittedLeases.put(dhcpLease.getNetAddr(), dhcpLease);
        maybeUpdateEarliestExpiration(dhcpLease.getExpTime());
    }

    public boolean releaseLease(byte[] bArr, MacAddress macAddress, Inet4Address inet4Address) {
        DhcpLease orDefault = this.mCommittedLeases.getOrDefault(inet4Address, null);
        if (orDefault == null) {
            SharedLog sharedLog = this.mLog;
            sharedLog.mo570w("Could not release unknown lease for " + DhcpLease.inet4AddrToString(inet4Address));
            return false;
        } else if (orDefault.matchesClient(bArr, macAddress)) {
            this.mCommittedLeases.remove(inet4Address);
            SharedLog sharedLog2 = this.mLog;
            sharedLog2.log("Released lease " + orDefault);
            return true;
        } else {
            this.mLog.mo570w(String.format("Not releasing lease %s: does not match client (cid %s, hwAddr %s)", orDefault, DhcpLease.clientIdToString(bArr), macAddress));
            return false;
        }
    }

    private void maybeUpdateEarliestExpiration(long j) {
        if (j < this.mNextExpirationCheck) {
            this.mNextExpirationCheck = j;
        }
    }

    private <T> long removeExpired(long j, Map<Inet4Address, T> map, String str, Function<T, Long> function) {
        Iterator<Map.Entry<Inet4Address, T>> it = map.entrySet().iterator();
        long j2 = Long.MAX_VALUE;
        while (it.hasNext()) {
            Map.Entry<Inet4Address, T> next = it.next();
            long longValue = function.apply(next.getValue()).longValue();
            if (longValue <= j) {
                this.mLog.logf("Removing expired %s lease for %s (expTime=%s, currentTime=%s)", str, next.getKey(), Long.valueOf(longValue), Long.valueOf(j));
                it.remove();
            } else {
                j2 = Math.min(j2, longValue);
            }
        }
        return j2;
    }

    private void removeExpiredLeases(long j) {
        if (j >= this.mNextExpirationCheck) {
            this.mNextExpirationCheck = Math.min(removeExpired(j, this.mCommittedLeases, "committed", $$Lambda$JzGJaKdJpggc40ifok3mDiod80M.INSTANCE), removeExpired(j, this.mDeclinedAddrs, "declined", Function.identity()));
        }
    }

    private boolean isAvailable(Inet4Address inet4Address) {
        return !this.mReservedAddrs.contains(inet4Address) && !this.mCommittedLeases.containsKey(inet4Address);
    }

    private int getAddrIndex(int i) {
        return i & (~this.mSubnetMask);
    }

    private int getAddrByIndex(int i) {
        return i | this.mSubnetAddr;
    }

    private int getValidAddress(int i) {
        int addrIndex = getAddrIndex(i);
        int addrByIndex = getAddrByIndex(addrIndex) & 255;
        if (addrByIndex == 255) {
            addrIndex = (addrIndex + 2) % this.mNumAddresses;
        } else if (addrByIndex == 0) {
            addrIndex = (addrIndex + 1) % this.mNumAddresses;
        }
        if (addrIndex == 0 || addrIndex == this.mNumAddresses - 1) {
            addrIndex = 1;
        }
        return getAddrByIndex(addrIndex);
    }

    private boolean isValidAddress(Inet4Address inet4Address) {
        int inet4AddressToIntHTH = Inet4AddressUtils.inet4AddressToIntHTH(inet4Address);
        return getValidAddress(inet4AddressToIntHTH) == inet4AddressToIntHTH;
    }

    private int getNextAddress(int i) {
        return getValidAddress(getAddrByIndex((getAddrIndex(i) + 1) % this.mNumAddresses));
    }

    private int getFirstClientAddress(MacAddress macAddress) {
        byte[] byteArray = macAddress.toByteArray();
        int i = 0;
        for (byte b : byteArray) {
            i += (b << 8) + b + (b << 16);
        }
        return getValidAddress(getAddrByIndex(i % this.mNumAddresses));
    }

    private DhcpLease makeNewOffer(byte[] bArr, MacAddress macAddress, long j, String str) throws OutOfAddressesException {
        int firstClientAddress = getFirstClientAddress(macAddress);
        for (int i = 0; i < this.mNumAddresses; i++) {
            Inet4Address intToInet4AddressHTH = Inet4AddressUtils.intToInet4AddressHTH(firstClientAddress);
            if (isAvailable(intToInet4AddressHTH) && !this.mDeclinedAddrs.containsKey(intToInet4AddressHTH)) {
                return new DhcpLease(bArr, macAddress, intToInet4AddressHTH, j, str);
            }
            firstClientAddress = getNextAddress(firstClientAddress);
        }
        Iterator<Inet4Address> it = this.mDeclinedAddrs.keySet().iterator();
        while (it.hasNext()) {
            Inet4Address next = it.next();
            it.remove();
            this.mLog.logf("Out of addresses in address pool: dropped declined addr %s", DhcpLease.inet4AddrToString(next));
            if (isAvailable(next)) {
                return new DhcpLease(bArr, macAddress, next, j, str);
            }
        }
        throw new OutOfAddressesException("No address available for offer");
    }
}
