package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class InitialConfigurationParcelable implements Parcelable {
    public static final Parcelable.Creator<InitialConfigurationParcelable> CREATOR = new Parcelable.Creator<InitialConfigurationParcelable>() {
        /* class android.net.InitialConfigurationParcelable.C00011 */

        @Override // android.os.Parcelable.Creator
        public InitialConfigurationParcelable createFromParcel(Parcel parcel) {
            InitialConfigurationParcelable initialConfigurationParcelable = new InitialConfigurationParcelable();
            initialConfigurationParcelable.readFromParcel(parcel);
            return initialConfigurationParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public InitialConfigurationParcelable[] newArray(int i) {
            return new InitialConfigurationParcelable[i];
        }
    };
    public IpPrefix[] directlyConnectedRoutes;
    public String[] dnsServers;
    public String gateway;
    public LinkAddress[] ipAddresses;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeTypedArray(this.ipAddresses, 0);
        parcel.writeTypedArray(this.directlyConnectedRoutes, 0);
        parcel.writeStringArray(this.dnsServers);
        parcel.writeString(this.gateway);
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
                this.ipAddresses = (LinkAddress[]) parcel.createTypedArray(LinkAddress.CREATOR);
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.directlyConnectedRoutes = (IpPrefix[]) parcel.createTypedArray(IpPrefix.CREATOR);
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.dnsServers = parcel.createStringArray();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.gateway = parcel.readString();
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                parcel.setDataPosition(dataPosition + readInt);
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
