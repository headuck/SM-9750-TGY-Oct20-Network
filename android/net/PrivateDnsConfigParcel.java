package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class PrivateDnsConfigParcel implements Parcelable {
    public static final Parcelable.Creator<PrivateDnsConfigParcel> CREATOR = new Parcelable.Creator<PrivateDnsConfigParcel>() {
        /* class android.net.PrivateDnsConfigParcel.C00041 */

        @Override // android.os.Parcelable.Creator
        public PrivateDnsConfigParcel createFromParcel(Parcel parcel) {
            PrivateDnsConfigParcel privateDnsConfigParcel = new PrivateDnsConfigParcel();
            privateDnsConfigParcel.readFromParcel(parcel);
            return privateDnsConfigParcel;
        }

        @Override // android.os.Parcelable.Creator
        public PrivateDnsConfigParcel[] newArray(int i) {
            return new PrivateDnsConfigParcel[i];
        }
    };
    public String hostname;
    public String[] ips;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeString(this.hostname);
        parcel.writeStringArray(this.ips);
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
                this.hostname = parcel.readString();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.ips = parcel.createStringArray();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        parcel.setDataPosition(dataPosition + readInt);
                    }
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
