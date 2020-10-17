package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class UidRangeParcel implements Parcelable {
    public static final Parcelable.Creator<UidRangeParcel> CREATOR = new Parcelable.Creator<UidRangeParcel>() {
        /* class android.net.UidRangeParcel.C00081 */

        @Override // android.os.Parcelable.Creator
        public UidRangeParcel createFromParcel(Parcel parcel) {
            UidRangeParcel uidRangeParcel = new UidRangeParcel();
            uidRangeParcel.readFromParcel(parcel);
            return uidRangeParcel;
        }

        @Override // android.os.Parcelable.Creator
        public UidRangeParcel[] newArray(int i) {
            return new UidRangeParcel[i];
        }
    };
    public int start;
    public int stop;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeInt(this.start);
        parcel.writeInt(this.stop);
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
                this.start = parcel.readInt();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.stop = parcel.readInt();
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
