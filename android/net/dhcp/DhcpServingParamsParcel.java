package android.net.dhcp;

import android.os.Parcel;
import android.os.Parcelable;

public class DhcpServingParamsParcel implements Parcelable {
    public static final Parcelable.Creator<DhcpServingParamsParcel> CREATOR = new Parcelable.Creator<DhcpServingParamsParcel>() {
        /* class android.net.dhcp.DhcpServingParamsParcel.C00141 */

        @Override // android.os.Parcelable.Creator
        public DhcpServingParamsParcel createFromParcel(Parcel parcel) {
            DhcpServingParamsParcel dhcpServingParamsParcel = new DhcpServingParamsParcel();
            dhcpServingParamsParcel.readFromParcel(parcel);
            return dhcpServingParamsParcel;
        }

        @Override // android.os.Parcelable.Creator
        public DhcpServingParamsParcel[] newArray(int i) {
            return new DhcpServingParamsParcel[i];
        }
    };
    public int[] defaultRouters;
    public long dhcpLeaseTimeSecs;
    public int[] dnsServers;
    public int[] excludedAddrs;
    public int linkMtu;
    public boolean metered;
    public int serverAddr;
    public int serverAddrPrefixLength;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeInt(this.serverAddr);
        parcel.writeInt(this.serverAddrPrefixLength);
        parcel.writeIntArray(this.defaultRouters);
        parcel.writeIntArray(this.dnsServers);
        parcel.writeIntArray(this.excludedAddrs);
        parcel.writeLong(this.dhcpLeaseTimeSecs);
        parcel.writeInt(this.linkMtu);
        parcel.writeInt(this.metered ? 1 : 0);
        int dataPosition2 = parcel.dataPosition();
        parcel.setDataPosition(dataPosition);
        parcel.writeInt(dataPosition2 - dataPosition);
        parcel.setDataPosition(dataPosition2);
    }

    public final void readFromParcel(Parcel parcel) {
        int dataPosition = parcel.dataPosition();
        int readInt = parcel.readInt();
        if (readInt >= 0) {
            try {
                this.serverAddr = parcel.readInt();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.serverAddrPrefixLength = parcel.readInt();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.defaultRouters = parcel.createIntArray();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.dnsServers = parcel.createIntArray();
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.excludedAddrs = parcel.createIntArray();
                                if (parcel.dataPosition() - dataPosition < readInt) {
                                    this.dhcpLeaseTimeSecs = parcel.readLong();
                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                        this.linkMtu = parcel.readInt();
                                        if (parcel.dataPosition() - dataPosition < readInt) {
                                            this.metered = parcel.readInt() != 0;
                                            if (parcel.dataPosition() - dataPosition < readInt) {
                                                parcel.setDataPosition(dataPosition + readInt);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
