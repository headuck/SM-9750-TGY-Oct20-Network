package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NattKeepalivePacketDataParcelable implements Parcelable {
    public static final Parcelable.Creator<NattKeepalivePacketDataParcelable> CREATOR = new Parcelable.Creator<NattKeepalivePacketDataParcelable>() {
        /* class android.net.NattKeepalivePacketDataParcelable.C00031 */

        @Override // android.os.Parcelable.Creator
        public NattKeepalivePacketDataParcelable createFromParcel(Parcel parcel) {
            NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable = new NattKeepalivePacketDataParcelable();
            nattKeepalivePacketDataParcelable.readFromParcel(parcel);
            return nattKeepalivePacketDataParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public NattKeepalivePacketDataParcelable[] newArray(int i) {
            return new NattKeepalivePacketDataParcelable[i];
        }
    };
    public byte[] dstAddress;
    public int dstPort;
    public byte[] srcAddress;
    public int srcPort;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeByteArray(this.srcAddress);
        parcel.writeInt(this.srcPort);
        parcel.writeByteArray(this.dstAddress);
        parcel.writeInt(this.dstPort);
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
                this.srcAddress = parcel.createByteArray();
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.srcPort = parcel.readInt();
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.dstAddress = parcel.createByteArray();
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            this.dstPort = parcel.readInt();
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
