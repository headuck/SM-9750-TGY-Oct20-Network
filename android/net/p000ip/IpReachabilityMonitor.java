package android.net.p000ip;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpReachabilityEvent;
import android.net.p000ip.IpNeighborMonitor;
import android.net.util.InterfaceParams;
import android.net.util.SharedLog;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* renamed from: android.net.ip.IpReachabilityMonitor */
public class IpReachabilityMonitor {
    private static final boolean DBG = Log.isLoggable("IpReachabilityMonitor", 3);
    private static final boolean VDBG = Log.isLoggable("IpReachabilityMonitor", 2);
    private final Callback mCallback;
    private final ConnectivityManager mCm;
    private final Dependencies mDependencies;
    private final InterfaceParams mInterfaceParams;
    private final IpNeighborMonitor mIpNeighborMonitor;
    private volatile long mLastProbeTimeMs;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private Map<InetAddress, IpNeighborMonitor.NeighborEvent> mNeighborWatchList;
    private final boolean mUsingMultinetworkPolicyTracker;

    /* renamed from: android.net.ip.IpReachabilityMonitor$Callback */
    public interface Callback {
        void notifyLost(InetAddress inetAddress, String str);
    }

    private static long getProbeWakeLockDuration() {
        return 3500;
    }

    private static int nudFailureEventType(boolean z, boolean z2) {
        return z ? z2 ? 768 : 512 : z2 ? 1280 : 1024;
    }

    /* access modifiers changed from: package-private */
    /* renamed from: android.net.ip.IpReachabilityMonitor$Dependencies */
    public interface Dependencies {
        void acquireWakeLock(long j);

        static default Dependencies makeDefault(Context context, String str) {
            final PowerManager.WakeLock newWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "IpReachabilityMonitor." + str);
            return new Dependencies() {
                /* class android.net.p000ip.IpReachabilityMonitor.Dependencies.C00171 */

                @Override // android.net.p000ip.IpReachabilityMonitor.Dependencies
                public void acquireWakeLock(long j) {
                    newWakeLock.acquire(j);
                }
            };
        }
    }

    public IpReachabilityMonitor(Context context, InterfaceParams interfaceParams, Handler handler, SharedLog sharedLog, Callback callback, boolean z) {
        this(context, interfaceParams, handler, sharedLog, callback, z, Dependencies.makeDefault(context, interfaceParams.name));
    }

    IpReachabilityMonitor(Context context, InterfaceParams interfaceParams, Handler handler, SharedLog sharedLog, Callback callback, boolean z, Dependencies dependencies) {
        this.mMetricsLog = new IpConnectivityLog();
        this.mLinkProperties = new LinkProperties();
        this.mNeighborWatchList = new HashMap();
        if (interfaceParams != null) {
            this.mInterfaceParams = interfaceParams;
            this.mLog = sharedLog.forSubComponent("IpReachabilityMonitor");
            this.mCallback = callback;
            this.mUsingMultinetworkPolicyTracker = z;
            this.mCm = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
            this.mDependencies = dependencies;
            this.mIpNeighborMonitor = new IpNeighborMonitor(handler, this.mLog, new IpNeighborMonitor.NeighborEventConsumer() {
                /* class android.net.p000ip.$$Lambda$IpReachabilityMonitor$5Sg30oRgfU2r5ogQj53SRYnnFiQ */

                @Override // android.net.p000ip.IpNeighborMonitor.NeighborEventConsumer
                public final void accept(IpNeighborMonitor.NeighborEvent neighborEvent) {
                    IpReachabilityMonitor.this.lambda$new$0$IpReachabilityMonitor(neighborEvent);
                }
            });
            this.mIpNeighborMonitor.start();
            return;
        }
        throw new IllegalArgumentException("null InterfaceParams");
    }

    public /* synthetic */ void lambda$new$0$IpReachabilityMonitor(IpNeighborMonitor.NeighborEvent neighborEvent) {
        if (this.mInterfaceParams.index == neighborEvent.ifindex && this.mNeighborWatchList.containsKey(neighborEvent.f5ip)) {
            IpNeighborMonitor.NeighborEvent put = this.mNeighborWatchList.put(neighborEvent.f5ip, neighborEvent);
            if (neighborEvent.nudState == 32) {
                SharedLog sharedLog = this.mLog;
                sharedLog.mo570w("ALERT neighbor went from: " + put + " to: " + neighborEvent);
                handleNeighborLost(neighborEvent);
            }
        }
    }

    public void stop() {
        this.mIpNeighborMonitor.stop();
        clearLinkProperties();
    }

    public void dump(PrintWriter printWriter) {
        if (Looper.myLooper() == this.mIpNeighborMonitor.getHandler().getLooper()) {
            printWriter.println(describeWatchList("\n"));
            return;
        }
        ConditionVariable conditionVariable = new ConditionVariable(false);
        this.mIpNeighborMonitor.getHandler().post(new Runnable(printWriter, conditionVariable) {
            /* class android.net.p000ip.$$Lambda$IpReachabilityMonitor$c1J5Y17FHcHurCdAcdDM8R0lYUE */
            private final /* synthetic */ PrintWriter f$1;
            private final /* synthetic */ ConditionVariable f$2;

            {
                this.f$1 = r2;
                this.f$2 = r3;
            }

            public final void run() {
                IpReachabilityMonitor.this.lambda$dump$1$IpReachabilityMonitor(this.f$1, this.f$2);
            }
        });
        if (!conditionVariable.block(1000)) {
            printWriter.println("Timed out waiting for IpReachabilityMonitor dump");
        }
    }

    public /* synthetic */ void lambda$dump$1$IpReachabilityMonitor(PrintWriter printWriter, ConditionVariable conditionVariable) {
        printWriter.println(describeWatchList("\n"));
        conditionVariable.open();
    }

    private String describeWatchList() {
        return describeWatchList(" ");
    }

    private String describeWatchList(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("iface{" + this.mInterfaceParams + "}," + str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("ntable=[");
        sb2.append(str);
        sb.append(sb2.toString());
        String str2 = "";
        for (Map.Entry<InetAddress, IpNeighborMonitor.NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            sb.append(str2);
            sb.append(entry.getKey().getHostAddress() + "/" + entry.getValue());
            StringBuilder sb3 = new StringBuilder();
            sb3.append(",");
            sb3.append(str);
            str2 = sb3.toString();
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean isOnLink(List<RouteInfo> list, InetAddress inetAddress) {
        for (RouteInfo routeInfo : list) {
            if (!routeInfo.hasGateway() && routeInfo.matches(inetAddress)) {
                return true;
            }
        }
        return false;
    }

    public void updateLinkProperties(LinkProperties linkProperties) {
        if (!this.mInterfaceParams.name.equals(linkProperties.getInterfaceName())) {
            Log.wtf("IpReachabilityMonitor", "requested LinkProperties interface '" + linkProperties.getInterfaceName() + "' does not match: " + this.mInterfaceParams.name);
            return;
        }
        this.mLinkProperties = new LinkProperties(linkProperties);
        HashMap hashMap = new HashMap();
        List<RouteInfo> routes = this.mLinkProperties.getRoutes();
        for (RouteInfo routeInfo : routes) {
            if (routeInfo.hasGateway()) {
                InetAddress gateway = routeInfo.getGateway();
                if (isOnLink(routes, gateway)) {
                    hashMap.put(gateway, this.mNeighborWatchList.getOrDefault(gateway, null));
                }
            }
        }
        for (InetAddress inetAddress : linkProperties.getDnsServers()) {
            if (isOnLink(routes, inetAddress)) {
                hashMap.put(inetAddress, this.mNeighborWatchList.getOrDefault(inetAddress, null));
            }
        }
        this.mNeighborWatchList = hashMap;
        if (DBG) {
            Log.d("IpReachabilityMonitor", "watch: " + describeWatchList());
        }
    }

    public void clearLinkProperties() {
        this.mLinkProperties.clear();
        this.mNeighborWatchList.clear();
        if (DBG) {
            Log.d("IpReachabilityMonitor", "clear: " + describeWatchList());
        }
    }

    private void handleNeighborLost(IpNeighborMonitor.NeighborEvent neighborEvent) {
        LinkProperties linkProperties = new LinkProperties(this.mLinkProperties);
        InetAddress inetAddress = null;
        for (Map.Entry<InetAddress, IpNeighborMonitor.NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            if (entry.getValue().nudState == 32) {
                inetAddress = entry.getKey();
                for (RouteInfo routeInfo : this.mLinkProperties.getRoutes()) {
                    if (inetAddress.equals(routeInfo.getGateway())) {
                        linkProperties.removeRoute(routeInfo);
                    }
                }
                if (avoidingBadLinks() || !(inetAddress instanceof Inet6Address)) {
                    linkProperties.removeDnsServer(inetAddress);
                }
            }
        }
        boolean z = (this.mLinkProperties.isIpv4Provisioned() && !linkProperties.isIpv4Provisioned()) || (this.mLinkProperties.isIpv6Provisioned() && !linkProperties.isIpv6Provisioned());
        if (z) {
            String str = "FAILURE: LOST_PROVISIONING, " + neighborEvent;
            Log.w("IpReachabilityMonitor", str);
            Callback callback = this.mCallback;
            if (callback != null) {
                callback.notifyLost(inetAddress, str);
            }
        }
        logNudFailed(z);
    }

    private boolean avoidingBadLinks() {
        return !this.mUsingMultinetworkPolicyTracker || this.mCm.shouldAvoidBadWifi();
    }

    public void probeAll() {
        ArrayList<InetAddress> arrayList = new ArrayList(this.mNeighborWatchList.keySet());
        if (!arrayList.isEmpty()) {
            this.mDependencies.acquireWakeLock(getProbeWakeLockDuration());
        }
        for (InetAddress inetAddress : arrayList) {
            int startKernelNeighborProbe = IpNeighborMonitor.startKernelNeighborProbe(this.mInterfaceParams.index, inetAddress);
            this.mLog.log(String.format("put neighbor %s into NUD_PROBE state (rval=%d)", inetAddress.getHostAddress(), Integer.valueOf(startKernelNeighborProbe)));
            logEvent(256, startKernelNeighborProbe);
        }
        this.mLastProbeTimeMs = SystemClock.elapsedRealtime();
    }

    private void logEvent(int i, int i2) {
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent(i | (i2 & 255)));
    }

    private void logNudFailed(boolean z) {
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent(nudFailureEventType(SystemClock.elapsedRealtime() - this.mLastProbeTimeMs < getProbeWakeLockDuration(), z)));
    }
}
