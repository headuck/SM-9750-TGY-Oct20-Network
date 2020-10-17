package android.net.netlink;

import android.system.Os;
import android.system.OsConstants;
import java.nio.ByteBuffer;

public class StructNdaCacheInfo {
    private static final long CLOCK_TICKS_PER_SECOND = Os.sysconf(OsConstants._SC_CLK_TCK);
    public int ndm_confirmed;
    public int ndm_refcnt;
    public int ndm_updated;
    public int ndm_used;

    private static boolean hasAvailableSpace(ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.remaining() >= 16;
    }

    public static StructNdaCacheInfo parse(ByteBuffer byteBuffer) {
        if (!hasAvailableSpace(byteBuffer)) {
            return null;
        }
        StructNdaCacheInfo structNdaCacheInfo = new StructNdaCacheInfo();
        structNdaCacheInfo.ndm_used = byteBuffer.getInt();
        structNdaCacheInfo.ndm_confirmed = byteBuffer.getInt();
        structNdaCacheInfo.ndm_updated = byteBuffer.getInt();
        structNdaCacheInfo.ndm_refcnt = byteBuffer.getInt();
        return structNdaCacheInfo;
    }

    private static long ticksToMilliSeconds(int i) {
        return ((((long) i) & -1) * 1000) / CLOCK_TICKS_PER_SECOND;
    }

    public long lastUsed() {
        return ticksToMilliSeconds(this.ndm_used);
    }

    public long lastConfirmed() {
        return ticksToMilliSeconds(this.ndm_confirmed);
    }

    public long lastUpdated() {
        return ticksToMilliSeconds(this.ndm_updated);
    }

    public String toString() {
        return "NdaCacheInfo{ ndm_used{" + lastUsed() + "}, ndm_confirmed{" + lastConfirmed() + "}, ndm_updated{" + lastUpdated() + "}, ndm_refcnt{" + this.ndm_refcnt + "} }";
    }
}
