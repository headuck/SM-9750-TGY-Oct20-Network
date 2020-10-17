package com.android.server.connectivity.nano;

import com.google.protobuf.nano.CodedOutputByteBufferNano;
import com.google.protobuf.nano.MessageNano;
import com.google.protobuf.nano.WireFormatNano;
import java.io.IOException;

public final class DnsEvent extends MessageNano {
    public int[] dnsReturnCode;
    public long[] dnsTime;

    public DnsEvent() {
        clear();
    }

    public DnsEvent clear() {
        this.dnsReturnCode = WireFormatNano.EMPTY_INT_ARRAY;
        this.dnsTime = WireFormatNano.EMPTY_LONG_ARRAY;
        this.cachedSize = -1;
        return this;
    }

    @Override // com.google.protobuf.nano.MessageNano
    public void writeTo(CodedOutputByteBufferNano codedOutputByteBufferNano) throws IOException {
        int[] iArr = this.dnsReturnCode;
        int i = 0;
        if (iArr != null && iArr.length > 0) {
            int i2 = 0;
            while (true) {
                int[] iArr2 = this.dnsReturnCode;
                if (i2 >= iArr2.length) {
                    break;
                }
                codedOutputByteBufferNano.writeInt32(1, iArr2[i2]);
                i2++;
            }
        }
        long[] jArr = this.dnsTime;
        if (jArr != null && jArr.length > 0) {
            while (true) {
                long[] jArr2 = this.dnsTime;
                if (i >= jArr2.length) {
                    break;
                }
                codedOutputByteBufferNano.writeInt64(2, jArr2[i]);
                i++;
            }
        }
        super.writeTo(codedOutputByteBufferNano);
    }

    /* access modifiers changed from: protected */
    @Override // com.google.protobuf.nano.MessageNano
    public int computeSerializedSize() {
        int[] iArr;
        int computeSerializedSize = super.computeSerializedSize();
        int[] iArr2 = this.dnsReturnCode;
        int i = 0;
        if (iArr2 != null && iArr2.length > 0) {
            int i2 = 0;
            int i3 = 0;
            while (true) {
                iArr = this.dnsReturnCode;
                if (i2 >= iArr.length) {
                    break;
                }
                i3 += CodedOutputByteBufferNano.computeInt32SizeNoTag(iArr[i2]);
                i2++;
            }
            computeSerializedSize = computeSerializedSize + i3 + (iArr.length * 1);
        }
        long[] jArr = this.dnsTime;
        if (jArr == null || jArr.length <= 0) {
            return computeSerializedSize;
        }
        int i4 = 0;
        while (true) {
            long[] jArr2 = this.dnsTime;
            if (i >= jArr2.length) {
                return computeSerializedSize + i4 + (jArr2.length * 1);
            }
            i4 += CodedOutputByteBufferNano.computeInt64SizeNoTag(jArr2[i]);
            i++;
        }
    }
}
