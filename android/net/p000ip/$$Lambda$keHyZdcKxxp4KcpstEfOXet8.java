package android.net.p000ip;

import android.net.LinkAddress;
import java.util.function.Predicate;

/* renamed from: android.net.ip.-$$Lambda$k-eHyZd-cKxxp4Kcp-stEfOXet8  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$keHyZdcKxxp4KcpstEfOXet8 implements Predicate {
    public static final /* synthetic */ $$Lambda$keHyZdcKxxp4KcpstEfOXet8 INSTANCE = new $$Lambda$keHyZdcKxxp4KcpstEfOXet8();

    private /* synthetic */ $$Lambda$keHyZdcKxxp4KcpstEfOXet8() {
    }

    @Override // java.util.function.Predicate
    public final boolean test(Object obj) {
        return ((LinkAddress) obj).isIpv6();
    }
}
