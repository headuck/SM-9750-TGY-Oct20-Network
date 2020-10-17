package android.net.ipmemorystore;

import android.os.Parcel;
import android.os.Parcelable;

public class Blob implements Parcelable {
    public static final Parcelable.Creator<Blob> CREATOR = new Parcelable.Creator<Blob>() {
        /* class android.net.ipmemorystore.Blob.C00181 */

        @Override // android.os.Parcelable.Creator
        public Blob createFromParcel(Parcel parcel) {
            Blob blob = new Blob();
            blob.readFromParcel(parcel);
            return blob;
        }

        @Override // android.os.Parcelable.Creator
        public Blob[] newArray(int i) {
            return new Blob[i];
        }
    };
    public byte[] data;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeByteArray(this.data);
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
                this.data = parcel.createByteArray();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    parcel.setDataPosition(dataPosition + readInt);
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
