package android.net.netlink;

import android.system.OsConstants;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RtNetlinkNeighborMessage extends NetlinkMessage {
    private StructNdaCacheInfo mCacheInfo = null;
    private InetAddress mDestination = null;
    private byte[] mLinkLayerAddr = null;
    private StructNdMsg mNdmsg = null;
    private int mNumProbes = 0;

    private static StructNlAttr findNextAttrOfType(short s, ByteBuffer byteBuffer) {
        while (byteBuffer != null && byteBuffer.remaining() > 0) {
            StructNlAttr peek = StructNlAttr.peek(byteBuffer);
            if (peek == null) {
                return null;
            }
            if (peek.nla_type == s) {
                return StructNlAttr.parse(byteBuffer);
            }
            if (byteBuffer.remaining() < peek.getAlignedLength()) {
                return null;
            }
            byteBuffer.position(byteBuffer.position() + peek.getAlignedLength());
        }
        return null;
    }

    public static RtNetlinkNeighborMessage parse(StructNlMsgHdr structNlMsgHdr, ByteBuffer byteBuffer) {
        RtNetlinkNeighborMessage rtNetlinkNeighborMessage = new RtNetlinkNeighborMessage(structNlMsgHdr);
        rtNetlinkNeighborMessage.mNdmsg = StructNdMsg.parse(byteBuffer);
        if (rtNetlinkNeighborMessage.mNdmsg == null) {
            return null;
        }
        int position = byteBuffer.position();
        StructNlAttr findNextAttrOfType = findNextAttrOfType(1, byteBuffer);
        if (findNextAttrOfType != null) {
            rtNetlinkNeighborMessage.mDestination = findNextAttrOfType.getValueAsInetAddress();
        }
        byteBuffer.position(position);
        StructNlAttr findNextAttrOfType2 = findNextAttrOfType(2, byteBuffer);
        if (findNextAttrOfType2 != null) {
            rtNetlinkNeighborMessage.mLinkLayerAddr = findNextAttrOfType2.nla_value;
        }
        byteBuffer.position(position);
        StructNlAttr findNextAttrOfType3 = findNextAttrOfType(4, byteBuffer);
        if (findNextAttrOfType3 != null) {
            rtNetlinkNeighborMessage.mNumProbes = findNextAttrOfType3.getValueAsInt(0);
        }
        byteBuffer.position(position);
        StructNlAttr findNextAttrOfType4 = findNextAttrOfType(3, byteBuffer);
        if (findNextAttrOfType4 != null) {
            rtNetlinkNeighborMessage.mCacheInfo = StructNdaCacheInfo.parse(findNextAttrOfType4.getValueAsByteBuffer());
        }
        int alignedLengthOf = NetlinkConstants.alignedLengthOf(rtNetlinkNeighborMessage.mHeader.nlmsg_len - 28);
        if (byteBuffer.remaining() < alignedLengthOf) {
            byteBuffer.position(byteBuffer.limit());
        } else {
            byteBuffer.position(position + alignedLengthOf);
        }
        return rtNetlinkNeighborMessage;
    }

    public static byte[] newNewNeighborMessage(int i, InetAddress inetAddress, short s, int i2, byte[] bArr) {
        StructNlMsgHdr structNlMsgHdr = new StructNlMsgHdr();
        structNlMsgHdr.nlmsg_type = 28;
        structNlMsgHdr.nlmsg_flags = 261;
        structNlMsgHdr.nlmsg_seq = i;
        RtNetlinkNeighborMessage rtNetlinkNeighborMessage = new RtNetlinkNeighborMessage(structNlMsgHdr);
        rtNetlinkNeighborMessage.mNdmsg = new StructNdMsg();
        rtNetlinkNeighborMessage.mNdmsg.ndm_family = (byte) (inetAddress instanceof Inet6Address ? OsConstants.AF_INET6 : OsConstants.AF_INET);
        StructNdMsg structNdMsg = rtNetlinkNeighborMessage.mNdmsg;
        structNdMsg.ndm_ifindex = i2;
        structNdMsg.ndm_state = s;
        rtNetlinkNeighborMessage.mDestination = inetAddress;
        rtNetlinkNeighborMessage.mLinkLayerAddr = bArr;
        byte[] bArr2 = new byte[rtNetlinkNeighborMessage.getRequiredSpace()];
        structNlMsgHdr.nlmsg_len = bArr2.length;
        ByteBuffer wrap = ByteBuffer.wrap(bArr2);
        wrap.order(ByteOrder.nativeOrder());
        rtNetlinkNeighborMessage.pack(wrap);
        return bArr2;
    }

    private RtNetlinkNeighborMessage(StructNlMsgHdr structNlMsgHdr) {
        super(structNlMsgHdr);
    }

    public StructNdMsg getNdHeader() {
        return this.mNdmsg;
    }

    public InetAddress getDestination() {
        return this.mDestination;
    }

    public byte[] getLinkLayerAddress() {
        return this.mLinkLayerAddr;
    }

    public int getRequiredSpace() {
        InetAddress inetAddress = this.mDestination;
        int i = 28;
        if (inetAddress != null) {
            i = 28 + NetlinkConstants.alignedLengthOf(inetAddress.getAddress().length + 4);
        }
        byte[] bArr = this.mLinkLayerAddr;
        return bArr != null ? i + NetlinkConstants.alignedLengthOf(bArr.length + 4) : i;
    }

    private static void packNlAttr(short s, byte[] bArr, ByteBuffer byteBuffer) {
        StructNlAttr structNlAttr = new StructNlAttr();
        structNlAttr.nla_type = s;
        structNlAttr.nla_value = bArr;
        structNlAttr.nla_len = (short) (structNlAttr.nla_value.length + 4);
        structNlAttr.pack(byteBuffer);
    }

    public void pack(ByteBuffer byteBuffer) {
        getHeader().pack(byteBuffer);
        this.mNdmsg.pack(byteBuffer);
        InetAddress inetAddress = this.mDestination;
        if (inetAddress != null) {
            packNlAttr(1, inetAddress.getAddress(), byteBuffer);
        }
        byte[] bArr = this.mLinkLayerAddr;
        if (bArr != null) {
            packNlAttr(2, bArr, byteBuffer);
        }
    }

    @Override // android.net.netlink.NetlinkMessage
    public String toString() {
        InetAddress inetAddress = this.mDestination;
        String str = "";
        String hostAddress = inetAddress == null ? str : inetAddress.getHostAddress();
        StringBuilder sb = new StringBuilder();
        sb.append("RtNetlinkNeighborMessage{ nlmsghdr{");
        StructNlMsgHdr structNlMsgHdr = this.mHeader;
        sb.append(structNlMsgHdr == null ? str : structNlMsgHdr.toString());
        sb.append("}, ndmsg{");
        StructNdMsg structNdMsg = this.mNdmsg;
        sb.append(structNdMsg == null ? str : structNdMsg.toString());
        sb.append("}, destination{");
        sb.append(hostAddress);
        sb.append("} linklayeraddr{");
        sb.append(NetlinkConstants.hexify(this.mLinkLayerAddr));
        sb.append("} probes{");
        sb.append(this.mNumProbes);
        sb.append("} cacheinfo{");
        StructNdaCacheInfo structNdaCacheInfo = this.mCacheInfo;
        if (structNdaCacheInfo != null) {
            str = structNdaCacheInfo.toString();
        }
        sb.append(str);
        sb.append("} }");
        return sb.toString();
    }
}
