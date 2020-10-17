package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class TetherStatsParcel implements Parcelable {
    public static final Parcelable.Creator<TetherStatsParcel> CREATOR = new Parcelable.Creator<TetherStatsParcel>() {
        /* class android.net.TetherStatsParcel.C00071 */

        @Override // android.os.Parcelable.Creator
        public TetherStatsParcel createFromParcel(Parcel parcel) {
            TetherStatsParcel tetherStatsParcel = new TetherStatsParcel();
            tetherStatsParcel.readFromParcel(parcel);
            return tetherStatsParcel;
        }

        @Override // android.os.Parcelable.Creator
        public TetherStatsParcel[] newArray(int i) {
            return new TetherStatsParcel[i];
        }
    };
    public String iface;
    public long rxBytes;
    public long rxPackets;
    public long txBytes;
    public long txPackets;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeString(this.iface);
        parcel.writeLong(this.rxBytes);
        parcel.writeLong(this.rxPackets);
        parcel.writeLong(this.txBytes);
        parcel.writeLong(this.txPackets);
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
                this.iface = parcel.readString();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.rxBytes = parcel.readLong();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.rxPackets = parcel.readLong();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.txBytes = parcel.readLong();
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.txPackets = parcel.readLong();
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
