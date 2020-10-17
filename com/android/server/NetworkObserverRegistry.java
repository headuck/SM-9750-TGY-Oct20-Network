package com.android.server;

import android.net.INetd;
import android.net.INetdUnsolicitedEventListener;
import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.RouteInfo;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.NetworkObserverRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkObserverRegistry extends INetdUnsolicitedEventListener.Stub {
    private static final String TAG = "NetworkObserverRegistry";
    private final ConcurrentHashMap<NetworkObserver, Optional<Handler>> mObservers = new ConcurrentHashMap<>();

    /* access modifiers changed from: private */
    @FunctionalInterface
    public interface NetworkObserverEventCallback {
        void sendCallback(NetworkObserver networkObserver);
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public int getInterfaceVersion() {
        return 2;
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onStrictCleartextDetected(int i, String str) {
    }

    NetworkObserverRegistry() {
    }

    /* access modifiers changed from: package-private */
    public void register(INetd iNetd) throws RemoteException {
        iNetd.registerUnsolicitedEventListener(this);
    }

    public void registerObserverForNonblockingCallback(NetworkObserver networkObserver) {
        this.mObservers.put(networkObserver, Optional.empty());
    }

    public void unregisterObserver(NetworkObserver networkObserver) {
        this.mObservers.remove(networkObserver);
    }

    private void invokeForAllObservers(NetworkObserverEventCallback networkObserverEventCallback) {
        for (Map.Entry<NetworkObserver, Optional<Handler>> entry : this.mObservers.entrySet()) {
            NetworkObserver key = entry.getKey();
            Optional<Handler> value = entry.getValue();
            if (value.isPresent()) {
                value.get().post(new Runnable(key) {
                    /* class com.android.server.$$Lambda$NetworkObserverRegistry$H4HK5CAHD8RiPWQITzTDllgbL4 */
                    private final /* synthetic */ NetworkObserver f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NetworkObserverRegistry.NetworkObserverEventCallback.this.sendCallback(this.f$1);
                    }
                });
                return;
            }
            try {
                networkObserverEventCallback.sendCallback(key);
            } catch (RuntimeException e) {
                Log.e(TAG, "Error sending callback to observer", e);
            }
        }
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceClassActivityChanged(boolean z, int i, long j, int i2) {
        invokeForAllObservers(new NetworkObserverEventCallback(z, i, j, i2) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$fcEOcHlx3mGqmmxRXUf2YAdCs0 */
            private final /* synthetic */ boolean f$0;
            private final /* synthetic */ int f$1;
            private final /* synthetic */ long f$2;
            private final /* synthetic */ int f$3;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r3;
                this.f$3 = r5;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceClassActivityChanged(this.f$0, this.f$1, this.f$2, this.f$3);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onQuotaLimitReached(String str, String str2) {
        invokeForAllObservers(new NetworkObserverEventCallback(str, str2) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$815ITXsLeTAxmrzAqYtqGfAqVg */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ String f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onQuotaLimitReached(this.f$0, this.f$1);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceDnsServerInfo(String str, long j, String[] strArr) {
        invokeForAllObservers(new NetworkObserverEventCallback(str, j, strArr) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$34jsWTE0YzFSky0b3w6EbkYk7M */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ long f$1;
            private final /* synthetic */ String[] f$2;

            {
                this.f$0 = r1;
                this.f$1 = r2;
                this.f$2 = r4;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceDnsServerInfo(this.f$0, this.f$1, this.f$2);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAddressUpdated(String str, String str2, int i, int i2) {
        invokeForAllObservers(new NetworkObserverEventCallback(new LinkAddress(str, i, i2), str2) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$oJaQ4CVyRqydS6yisqflfNfbo7Q */
            private final /* synthetic */ LinkAddress f$0;
            private final /* synthetic */ String f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceAddressUpdated(this.f$0, this.f$1);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAddressRemoved(String str, String str2, int i, int i2) {
        invokeForAllObservers(new NetworkObserverEventCallback(new LinkAddress(str, i, i2), str2) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$cNchMEkfuWKCf_o2aUKAsODVRa8 */
            private final /* synthetic */ LinkAddress f$0;
            private final /* synthetic */ String f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceAddressRemoved(this.f$0, this.f$1);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceAdded(String str) {
        invokeForAllObservers(new NetworkObserverEventCallback(str) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$grqMVS3HQMFFTPGP0XEs18mEdBU */
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceAdded(this.f$0);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceRemoved(String str) {
        invokeForAllObservers(new NetworkObserverEventCallback(str) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$NlFbtfr_35xW3_TGFF6cMmduLrI */
            private final /* synthetic */ String f$0;

            {
                this.f$0 = r1;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceRemoved(this.f$0);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceChanged(String str, boolean z) {
        invokeForAllObservers(new NetworkObserverEventCallback(str, z) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$QLVE3TUrVjZh61B27gpY4vNXP2s */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ boolean f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceChanged(this.f$0, this.f$1);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onInterfaceLinkStateChanged(String str, boolean z) {
        invokeForAllObservers(new NetworkObserverEventCallback(str, z) {
            /* class com.android.server.$$Lambda$NetworkObserverRegistry$Q7H1NC1hN07wYLtZvR9DmONSxWg */
            private final /* synthetic */ String f$0;
            private final /* synthetic */ boolean f$1;

            {
                this.f$0 = r1;
                this.f$1 = r2;
            }

            @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
            public final void sendCallback(NetworkObserver networkObserver) {
                networkObserver.onInterfaceLinkStateChanged(this.f$0, this.f$1);
            }
        });
    }

    @Override // android.net.INetdUnsolicitedEventListener
    public void onRouteChanged(boolean z, String str, String str2, String str3) {
        RouteInfo routeInfo = new RouteInfo(new IpPrefix(str), "".equals(str2) ? null : InetAddresses.parseNumericAddress(str2), str3, 1);
        if (z) {
            invokeForAllObservers(new NetworkObserverEventCallback(routeInfo) {
                /* class com.android.server.$$Lambda$NetworkObserverRegistry$RCPZvFLbsGihvWKvZ4iE71kq8D8 */
                private final /* synthetic */ RouteInfo f$0;

                {
                    this.f$0 = r1;
                }

                @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
                public final void sendCallback(NetworkObserver networkObserver) {
                    networkObserver.onRouteUpdated(this.f$0);
                }
            });
        } else {
            invokeForAllObservers(new NetworkObserverEventCallback(routeInfo) {
                /* class com.android.server.$$Lambda$NetworkObserverRegistry$PUSn88ltcws_eZ2MBwzioYlxRb8 */
                private final /* synthetic */ RouteInfo f$0;

                {
                    this.f$0 = r1;
                }

                @Override // com.android.server.NetworkObserverRegistry.NetworkObserverEventCallback
                public final void sendCallback(NetworkObserver networkObserver) {
                    networkObserver.onRouteRemoved(this.f$0);
                }
            });
        }
    }
}
