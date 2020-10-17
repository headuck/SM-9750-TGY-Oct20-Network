package android.net.netlink;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class StructNlAttr {
    private ByteOrder mByteOrder = ByteOrder.nativeOrder();
    public short nla_len = 4;
    public short nla_type;
    public byte[] nla_value;

    /* JADX INFO: finally extract failed */
    public static StructNlAttr peek(ByteBuffer byteBuffer) {
        if (byteBuffer == null || byteBuffer.remaining() < 4) {
            return null;
        }
        int position = byteBuffer.position();
        StructNlAttr structNlAttr = new StructNlAttr(byteBuffer.order());
        ByteOrder order = byteBuffer.order();
        byteBuffer.order(ByteOrder.nativeOrder());
        try {
            structNlAttr.nla_len = byteBuffer.getShort();
            structNlAttr.nla_type = byteBuffer.getShort();
            byteBuffer.order(order);
            byteBuffer.position(position);
            if (structNlAttr.nla_len < 4) {
                return null;
            }
            return structNlAttr;
        } catch (Throwable th) {
            byteBuffer.order(order);
            throw th;
        }
    }

    public static StructNlAttr parse(ByteBuffer byteBuffer) {
        StructNlAttr peek = peek(byteBuffer);
        if (peek == null || byteBuffer.remaining() < peek.getAlignedLength()) {
            return null;
        }
        int position = byteBuffer.position();
        byteBuffer.position(position + 4);
        int i = (peek.nla_len & 65535) - 4;
        if (i > 0) {
            peek.nla_value = new byte[i];
            byteBuffer.get(peek.nla_value, 0, i);
            byteBuffer.position(position + peek.getAlignedLength());
        }
        return peek;
    }

    public StructNlAttr() {
    }

    public StructNlAttr(ByteOrder byteOrder) {
        this.mByteOrder = byteOrder;
    }

    public int getAlignedLength() {
        return NetlinkConstants.alignedLengthOf(this.nla_len);
    }

    public ByteBuffer getValueAsByteBuffer() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        ByteBuffer wrap = ByteBuffer.wrap(bArr);
        wrap.order(this.mByteOrder);
        return wrap;
    }

    public int getValueAsInt(int i) {
        ByteBuffer valueAsByteBuffer = getValueAsByteBuffer();
        return (valueAsByteBuffer == null || valueAsByteBuffer.remaining() != 4) ? i : getValueAsByteBuffer().getInt();
    }

    public InetAddress getValueAsInetAddress() {
        byte[] bArr = this.nla_value;
        if (bArr == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bArr);
        } catch (UnknownHostException unused) {
            return null;
        }
    }

    /* JADX INFO: finally extract failed */
    public void pack(ByteBuffer byteBuffer) {
        ByteOrder order = byteBuffer.order();
        int position = byteBuffer.position();
        byteBuffer.order(ByteOrder.nativeOrder());
        try {
            byteBuffer.putShort(this.nla_len);
            byteBuffer.putShort(this.nla_type);
            if (this.nla_value != null) {
                byteBuffer.put(this.nla_value);
            }
            byteBuffer.order(order);
            byteBuffer.position(position + getAlignedLength());
        } catch (Throwable th) {
            byteBuffer.order(order);
            throw th;
        }
    }

    public String toString() {
        return "StructNlAttr{ nla_len{" + ((int) this.nla_len) + "}, nla_type{" + ((int) this.nla_type) + "}, nla_value{" + NetlinkConstants.hexify(this.nla_value) + "}, }";
    }
}
