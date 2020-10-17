package com.android.server.connectivity;

import android.net.captiveportal.CaptivePortalProbeSpec;
import java.util.function.Function;

/* renamed from: com.android.server.connectivity.-$$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto implements Function {
    public static final /* synthetic */ $$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto INSTANCE = new $$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto();

    private /* synthetic */ $$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return CaptivePortalProbeSpec.parseSpecOrNull((String) obj);
    }
}
