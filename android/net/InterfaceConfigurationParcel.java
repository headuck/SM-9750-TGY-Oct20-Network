package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class InterfaceConfigurationParcel implements Parcelable {
    public static final Parcelable.Creator<InterfaceConfigurationParcel> CREATOR = new Parcelable.Creator<InterfaceConfigurationParcel>() {
        /* class android.net.InterfaceConfigurationParcel.C00021 */

        @Override // android.os.Parcelable.Creator
        public InterfaceConfigurationParcel createFromParcel(Parcel parcel) {
            InterfaceConfigurationParcel interfaceConfigurationParcel = new InterfaceConfigurationParcel();
            interfaceConfigurationParcel.readFromParcel(parcel);
            return interfaceConfigurationParcel;
        }

        @Override // android.os.Parcelable.Creator
        public InterfaceConfigurationParcel[] newArray(int i) {
            return new InterfaceConfigurationParcel[i];
        }
    };
    public String[] flags;
    public String hwAddr;
    public String ifName;
    public String ipv4Addr;
    public int prefixLength;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeString(this.ifName);
        parcel.writeString(this.hwAddr);
        parcel.writeString(this.ipv4Addr);
        parcel.writeInt(this.prefixLength);
        parcel.writeStringArray(this.flags);
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
                this.ifName = parcel.readString();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.hwAddr = parcel.readString();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.ipv4Addr = parcel.readString();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.prefixLength = parcel.readInt();
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.flags = parcel.createStringArray();
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
