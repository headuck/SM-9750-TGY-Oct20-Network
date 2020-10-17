package android.net.ipmemorystore;

import android.os.Parcel;
import android.os.Parcelable;

public class StatusParcelable implements Parcelable {
    public static final Parcelable.Creator<StatusParcelable> CREATOR = new Parcelable.Creator<StatusParcelable>() {
        /* class android.net.ipmemorystore.StatusParcelable.C00211 */

        @Override // android.os.Parcelable.Creator
        public StatusParcelable createFromParcel(Parcel parcel) {
            StatusParcelable statusParcelable = new StatusParcelable();
            statusParcelable.readFromParcel(parcel);
            return statusParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public StatusParcelable[] newArray(int i) {
            return new StatusParcelable[i];
        }
    };
    public int resultCode;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeInt(this.resultCode);
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
                this.resultCode = parcel.readInt();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    parcel.setDataPosition(dataPosition + readInt);
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
