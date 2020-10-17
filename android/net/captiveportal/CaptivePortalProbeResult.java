package android.net.captiveportal;

public final class CaptivePortalProbeResult {
    public static final CaptivePortalProbeResult FAILED = new CaptivePortalProbeResult(599);
    public static final CaptivePortalProbeResult PARTIAL = new CaptivePortalProbeResult(-1);
    public static final CaptivePortalProbeResult PRIVATE_IP = new CaptivePortalProbeResult(-2);
    public static final CaptivePortalProbeResult SUCCESS = new CaptivePortalProbeResult(204);
    public final String detectUrl;
    public final int mHttpResponseCode;
    public final CaptivePortalProbeSpec probeSpec;
    public final String redirectUrl;

    public CaptivePortalProbeResult(int i) {
        this(i, null, null);
    }

    public CaptivePortalProbeResult(int i, String str, String str2) {
        this(i, str, str2, null);
    }

    public CaptivePortalProbeResult(int i, String str, String str2, CaptivePortalProbeSpec captivePortalProbeSpec) {
        this.mHttpResponseCode = i;
        this.redirectUrl = str;
        this.detectUrl = str2;
        this.probeSpec = captivePortalProbeSpec;
    }

    public boolean isSuccessful() {
        return this.mHttpResponseCode == 204;
    }

    public boolean isPortal() {
        int i;
        return !isSuccessful() && (i = this.mHttpResponseCode) >= 200 && i <= 399;
    }

    public boolean isFailed() {
        return !isSuccessful() && !isPortal();
    }

    public boolean isPartialConnectivity() {
        return this.mHttpResponseCode == -1;
    }

    public boolean isDnsPrivateIpResponse() {
        return this.mHttpResponseCode == -2;
    }
}
