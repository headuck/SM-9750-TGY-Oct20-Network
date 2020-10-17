package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class DhcpResultsParcelable implements Parcelable {
    public static final Parcelable.Creator<DhcpResultsParcelable> CREATOR = new Parcelable.Creator<DhcpResultsParcelable>() {
        /* class android.net.DhcpResultsParcelable.C00001 */

        @Override // android.os.Parcelable.Creator
        public DhcpResultsParcelable createFromParcel(Parcel parcel) {
            DhcpResultsParcelable dhcpResultsParcelable = new DhcpResultsParcelable();
            dhcpResultsParcelable.readFromParcel(parcel);
            return dhcpResultsParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public DhcpResultsParcelable[] newArray(int i) {
            return new DhcpResultsParcelable[i];
        }
    };
    public StaticIpConfiguration baseConfiguration;
    public int leaseDuration;
    public int mtu;
    public String serverAddress;
    public String serverHostName;
    public String vendorInfo;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        if (this.baseConfiguration != null) {
            parcel.writeInt(1);
            this.baseConfiguration.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.leaseDuration);
        parcel.writeInt(this.mtu);
        parcel.writeString(this.serverAddress);
        parcel.writeString(this.vendorInfo);
        parcel.writeString(this.serverHostName);
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
                if (parcel.readInt() != 0) {
                    this.baseConfiguration = (StaticIpConfiguration) StaticIpConfiguration.CREATOR.createFromParcel(parcel);
                } else {
                    this.baseConfiguration = null;
                }
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.leaseDuration = parcel.readInt();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.mtu = parcel.readInt();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.serverAddress = parcel.readString();
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.vendorInfo = parcel.readString();
                                if (parcel.dataPosition() - dataPosition < readInt) {
                                    this.serverHostName = parcel.readString();
                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                        parcel.setDataPosition(dataPosition + readInt);
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
