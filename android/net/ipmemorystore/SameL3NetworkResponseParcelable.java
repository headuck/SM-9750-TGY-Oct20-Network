package android.net.ipmemorystore;

import android.os.Parcel;
import android.os.Parcelable;

public class SameL3NetworkResponseParcelable implements Parcelable {
    public static final Parcelable.Creator<SameL3NetworkResponseParcelable> CREATOR = new Parcelable.Creator<SameL3NetworkResponseParcelable>() {
        /* class android.net.ipmemorystore.SameL3NetworkResponseParcelable.C00201 */

        @Override // android.os.Parcelable.Creator
        public SameL3NetworkResponseParcelable createFromParcel(Parcel parcel) {
            SameL3NetworkResponseParcelable sameL3NetworkResponseParcelable = new SameL3NetworkResponseParcelable();
            sameL3NetworkResponseParcelable.readFromParcel(parcel);
            return sameL3NetworkResponseParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public SameL3NetworkResponseParcelable[] newArray(int i) {
            return new SameL3NetworkResponseParcelable[i];
        }
    };
    public float confidence;
    public String l2Key1;
    public String l2Key2;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeString(this.l2Key1);
        parcel.writeString(this.l2Key2);
        parcel.writeFloat(this.confidence);
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
                this.l2Key1 = parcel.readString();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.l2Key2 = parcel.readString();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.confidence = parcel.readFloat();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            parcel.setDataPosition(dataPosition + readInt);
                        }
                    }
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
