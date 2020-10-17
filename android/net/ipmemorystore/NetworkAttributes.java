package android.net.ipmemorystore;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class NetworkAttributes {
    public static final float TOTAL_WEIGHT = 850.0f;
    public final Inet4Address assignedV4Address;
    public final Long assignedV4AddressExpiry;
    public final List<InetAddress> dnsAddresses;
    public final String groupHint;
    public final Integer mtu;

    public NetworkAttributes(Inet4Address inet4Address, Long l, String str, List<InetAddress> list, Integer num) {
        List<InetAddress> list2;
        if (num != null && num.intValue() < 0) {
            throw new IllegalArgumentException("MTU can't be negative");
        } else if (l == null || l.longValue() > 0) {
            this.assignedV4Address = inet4Address;
            this.assignedV4AddressExpiry = l;
            this.groupHint = str;
            if (list == null) {
                list2 = null;
            } else {
                list2 = Collections.unmodifiableList(new ArrayList(list));
            }
            this.dnsAddresses = list2;
            this.mtu = num;
        } else {
            throw new IllegalArgumentException("lease expiry can't be negative or zero");
        }
    }

    /* JADX WARNING: Illegal instructions before constructor call */
    public NetworkAttributes(NetworkAttributesParcelable networkAttributesParcelable) {
        this(r2, r3, r0, r5, r8 >= 0 ? Integer.valueOf(r8) : null);
        Inet4Address inet4Address = (Inet4Address) getByAddressOrNull(networkAttributesParcelable.assignedV4Address);
        long j = networkAttributesParcelable.assignedV4AddressExpiry;
        Long valueOf = j > 0 ? Long.valueOf(j) : null;
        String str = networkAttributesParcelable.groupHint;
        List<InetAddress> blobArrayToInetAddressList = blobArrayToInetAddressList(networkAttributesParcelable.dnsAddresses);
        int i = networkAttributesParcelable.mtu;
    }

    private static InetAddress getByAddressOrNull(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException unused) {
            return null;
        }
    }

    private static List<InetAddress> blobArrayToInetAddressList(Blob[] blobArr) {
        if (blobArr == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(blobArr.length);
        for (Blob blob : blobArr) {
            InetAddress byAddressOrNull = getByAddressOrNull(blob.data);
            if (byAddressOrNull != null) {
                arrayList.add(byAddressOrNull);
            }
        }
        return arrayList;
    }

    private static Blob[] inetAddressListToBlobArray(List<InetAddress> list) {
        if (list == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            InetAddress inetAddress = list.get(i);
            if (inetAddress != null) {
                Blob blob = new Blob();
                blob.data = inetAddress.getAddress();
                arrayList.add(blob);
            }
        }
        return (Blob[]) arrayList.toArray(new Blob[0]);
    }

    public NetworkAttributesParcelable toParcelable() {
        NetworkAttributesParcelable networkAttributesParcelable = new NetworkAttributesParcelable();
        Inet4Address inet4Address = this.assignedV4Address;
        networkAttributesParcelable.assignedV4Address = inet4Address == null ? null : inet4Address.getAddress();
        Long l = this.assignedV4AddressExpiry;
        networkAttributesParcelable.assignedV4AddressExpiry = l == null ? 0 : l.longValue();
        networkAttributesParcelable.groupHint = this.groupHint;
        networkAttributesParcelable.dnsAddresses = inetAddressListToBlobArray(this.dnsAddresses);
        Integer num = this.mtu;
        networkAttributesParcelable.mtu = num == null ? -1 : num.intValue();
        return networkAttributesParcelable;
    }

    private float samenessContribution(float f, Object obj, Object obj2) {
        if (obj == null) {
            if (obj2 == null) {
                return f * 0.25f;
            }
            return 0.0f;
        } else if (Objects.equals(obj, obj2)) {
            return f;
        } else {
            return 0.0f;
        }
    }

    public float getNetworkGroupSamenessConfidence(NetworkAttributes networkAttributes) {
        float samenessContribution = samenessContribution(300.0f, this.assignedV4Address, networkAttributes.assignedV4Address) + samenessContribution(0.0f, this.assignedV4AddressExpiry, networkAttributes.assignedV4AddressExpiry) + samenessContribution(300.0f, this.groupHint, networkAttributes.groupHint) + samenessContribution(200.0f, this.dnsAddresses, networkAttributes.dnsAddresses) + samenessContribution(50.0f, this.mtu, networkAttributes.mtu);
        return samenessContribution < 520.0f ? samenessContribution / 1040.0f : (((samenessContribution - 520.0f) / 330.0f) / 2.0f) + 0.5f;
    }

    public static class Builder {
        private Inet4Address mAssignedAddress;
        private Long mAssignedAddressExpiry;
        private List<InetAddress> mDnsAddresses;
        private String mGroupHint;
        private Integer mMtu;

        public Builder setAssignedV4Address(Inet4Address inet4Address) {
            this.mAssignedAddress = inet4Address;
            return this;
        }

        public Builder setAssignedV4AddressExpiry(Long l) {
            if (l == null || l.longValue() > 0) {
                this.mAssignedAddressExpiry = l;
                return this;
            }
            throw new IllegalArgumentException("lease expiry can't be negative or zero");
        }

        public Builder setGroupHint(String str) {
            this.mGroupHint = str;
            return this;
        }

        public Builder setDnsAddresses(List<InetAddress> list) {
            if (list != null) {
                for (InetAddress inetAddress : list) {
                    if (inetAddress == null) {
                        throw new IllegalArgumentException("Null DNS address");
                    }
                }
            }
            this.mDnsAddresses = list;
            return this;
        }

        public Builder setMtu(Integer num) {
            if (num == null || num.intValue() >= 0) {
                this.mMtu = num;
                return this;
            }
            throw new IllegalArgumentException("MTU can't be negative");
        }

        public NetworkAttributes build() {
            return new NetworkAttributes(this.mAssignedAddress, this.mAssignedAddressExpiry, this.mGroupHint, this.mDnsAddresses, this.mMtu);
        }
    }

    public boolean isEmpty() {
        return this.assignedV4Address == null && this.assignedV4AddressExpiry == null && this.groupHint == null && this.dnsAddresses == null && this.mtu == null;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkAttributes)) {
            return false;
        }
        NetworkAttributes networkAttributes = (NetworkAttributes) obj;
        if (!Objects.equals(this.assignedV4Address, networkAttributes.assignedV4Address) || !Objects.equals(this.assignedV4AddressExpiry, networkAttributes.assignedV4AddressExpiry) || !Objects.equals(this.groupHint, networkAttributes.groupHint) || !Objects.equals(this.dnsAddresses, networkAttributes.dnsAddresses) || !Objects.equals(this.mtu, networkAttributes.mtu)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.assignedV4Address, this.assignedV4AddressExpiry, this.groupHint, this.dnsAddresses, this.mtu);
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(" ", "{", "}");
        ArrayList arrayList = new ArrayList();
        if (this.assignedV4Address != null) {
            stringJoiner.add("assignedV4Addr :");
            stringJoiner.add(this.assignedV4Address.toString());
        } else {
            arrayList.add("assignedV4Addr");
        }
        if (this.assignedV4AddressExpiry != null) {
            stringJoiner.add("assignedV4AddressExpiry :");
            stringJoiner.add(this.assignedV4AddressExpiry.toString());
        } else {
            arrayList.add("assignedV4AddressExpiry");
        }
        if (this.groupHint != null) {
            stringJoiner.add("groupHint :");
            stringJoiner.add(this.groupHint);
        } else {
            arrayList.add("groupHint");
        }
        if (this.dnsAddresses != null) {
            stringJoiner.add("dnsAddr : [");
            for (InetAddress inetAddress : this.dnsAddresses) {
                stringJoiner.add(inetAddress.getHostAddress());
            }
            stringJoiner.add("]");
        } else {
            arrayList.add("dnsAddr");
        }
        if (this.mtu != null) {
            stringJoiner.add("mtu :");
            stringJoiner.add(this.mtu.toString());
        } else {
            arrayList.add("mtu");
        }
        if (!arrayList.isEmpty()) {
            stringJoiner.add("; Null fields : [");
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                stringJoiner.add((String) it.next());
            }
            stringJoiner.add("]");
        }
        return stringJoiner.toString();
    }
}
