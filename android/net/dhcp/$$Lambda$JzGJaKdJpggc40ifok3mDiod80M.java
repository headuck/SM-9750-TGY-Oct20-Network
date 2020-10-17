package android.net.dhcp;

import java.util.function.Function;

/* renamed from: android.net.dhcp.-$$Lambda$JzGJaKdJpggc40ifok3mDiod80M  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$JzGJaKdJpggc40ifok3mDiod80M implements Function {
    public static final /* synthetic */ $$Lambda$JzGJaKdJpggc40ifok3mDiod80M INSTANCE = new $$Lambda$JzGJaKdJpggc40ifok3mDiod80M();

    private /* synthetic */ $$Lambda$JzGJaKdJpggc40ifok3mDiod80M() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return Long.valueOf(((DhcpLease) obj).getExpTime());
    }
}
