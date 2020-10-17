package android.net.ipmemorystore;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkAttributesParcelable implements Parcelable {
    public static final Parcelable.Creator<NetworkAttributesParcelable> CREATOR = new Parcelable.Creator<NetworkAttributesParcelable>() {
        /* class android.net.ipmemorystore.NetworkAttributesParcelable.C00191 */

        @Override // android.os.Parcelable.Creator
        public NetworkAttributesParcelable createFromParcel(Parcel parcel) {
            NetworkAttributesParcelable networkAttributesParcelable = new NetworkAttributesParcelable();
            networkAttributesParcelable.readFromParcel(parcel);
            return networkAttributesParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public NetworkAttributesParcelable[] newArray(int i) {
            return new NetworkAttributesParcelable[i];
        }
    };
    public byte[] assignedV4Address;
    public long assignedV4AddressExpiry;
    public Blob[] dnsAddresses;
    public String groupHint;
    public int mtu;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeByteArray(this.assignedV4Address);
        parcel.writeLong(this.assignedV4AddressExpiry);
        parcel.writeString(this.groupHint);
        parcel.writeTypedArray(this.dnsAddresses, 0);
        parcel.writeInt(this.mtu);
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
                this.assignedV4Address = parcel.createByteArray();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.assignedV4AddressExpiry = parcel.readLong();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.groupHint = parcel.readString();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.dnsAddresses = (Blob[]) parcel.createTypedArray(Blob.CREATOR);
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.mtu = parcel.readInt();
                                if (parcel.dataPosition() - dataPosition < readInt) {
                                    parcel.setDataPosition(dataPosition + readInt);
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
