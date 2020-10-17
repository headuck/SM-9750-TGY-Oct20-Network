package com.android.server.connectivity.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import java.io.IOException;

public final class CellularData extends MessageNano {
    public boolean isRoaming;
    public String networkMccmnc;
    public int ratType;
    public int signalStrength;
    public String simMccmnc;

    public CellularData() {
        clear();
    }

    public CellularData clear() {
        this.ratType = 0;
        this.isRoaming = false;
        this.networkMccmnc = "";
        this.simMccmnc = "";
        this.signalStrength = 0;
        this.cachedSize = -1;
        return this;
    }

    @Override // com.google.protobuf.nano.MessageNano
    public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
        int i = this.ratType;
        if (i != 0) {
            codedOutputByteBufferNano.writeInt32(1, i);
        }
        boolean z = this.isRoaming;
        if (z) {
            codedOutputByteBufferNano.writeBool(2, z);
        }
        if (!this.networkMccmnc.equals("")) {
            codedOutputByteBufferNano.writeString(3, this.networkMccmnc);
        }
        if (!this.simMccmnc.equals("")) {
            codedOutputByteBufferNano.writeString(4, this.simMccmnc);
        }
        int i2 = this.signalStrength;
        if (i2 != 0) {
            codedOutputByteBufferNano.writeInt32(5, i2);
        }
        super.writeTo(codedOutputByteBufferNano);
    }

    /* access modifiers changed from: protected */
    @Override // com.google.protobuf.nano.MessageNano
    public int computeSerializedSize() {
        int computeSerializedSize = super.computeSerializedSize();
        int i = this.ratType;
        if (i != 0) {
            computeSerializedSize += CodedOutputByteBufferNano.computeInt32Size(1, i);
        }
        boolean z = this.isRoaming;
        if (z) {
            computeSerializedSize += CodedOutputByteBufferNano.computeBoolSize(2, z);
        }
        if (!this.networkMccmnc.equals("")) {
            computeSerializedSize += CodedOutputByteBufferNano.computeStringSize(3, this.networkMccmnc);
        }
        if (!this.simMccmnc.equals("")) {
            computeSerializedSize += CodedOutputByteBufferNano.computeStringSize(4, this.simMccmnc);
        }
        int i2 = this.signalStrength;
        return i2 != 0 ? computeSerializedSize + CodedOutputByteBufferNano.computeInt32Size(5, i2) : computeSerializedSize;
    }
}
