package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class TcpKeepalivePacketDataParcelable implements Parcelable {
    public static final Parcelable.Creator<TcpKeepalivePacketDataParcelable> CREATOR = new Parcelable.Creator<TcpKeepalivePacketDataParcelable>() {
        /* class android.net.TcpKeepalivePacketDataParcelable.C00061 */

        @Override // android.os.Parcelable.Creator
        public TcpKeepalivePacketDataParcelable createFromParcel(Parcel parcel) {
            TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable = new TcpKeepalivePacketDataParcelable();
            tcpKeepalivePacketDataParcelable.readFromParcel(parcel);
            return tcpKeepalivePacketDataParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public TcpKeepalivePacketDataParcelable[] newArray(int i) {
            return new TcpKeepalivePacketDataParcelable[i];
        }
    };
    public int ack;
    public byte[] dstAddress;
    public int dstPort;
    public int rcvWnd;
    public int rcvWndScale;
    public int seq;
    public byte[] srcAddress;
    public int srcPort;
    public int tos;
    public int ttl;

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
        parcel.writeInt(this.seq);
        parcel.writeInt(this.ack);
        parcel.writeInt(this.rcvWnd);
        parcel.writeInt(this.rcvWndScale);
        parcel.writeInt(this.tos);
        parcel.writeInt(this.ttl);
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
                                this.seq = parcel.readInt();
                                if (parcel.dataPosition() - dataPosition < readInt) {
                                    this.ack = parcel.readInt();
                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                        this.rcvWnd = parcel.readInt();
                                        if (parcel.dataPosition() - dataPosition < readInt) {
                                            this.rcvWndScale = parcel.readInt();
                                            if (parcel.dataPosition() - dataPosition < readInt) {
                                                this.tos = parcel.readInt();
                                                if (parcel.dataPosition() - dataPosition < readInt) {
                                                    this.ttl = parcel.readInt();
                                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                                        parcel.setDataPosition(dataPosition + readInt);
                                                    }
                                                }
                                            }
                                        }
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
