package com.android.server;

import android.net.LinkAddress;
import android.net.RouteInfo;

public interface NetworkObserver {
    default void onInterfaceAdded(String str) {
    }

    default void onInterfaceAddressRemoved(LinkAddress linkAddress, String str) {
    }

    default void onInterfaceAddressUpdated(LinkAddress linkAddress, String str) {
    }

    default void onInterfaceChanged(String str, boolean z) {
    }

    default void onInterfaceClassActivityChanged(boolean z, int i, long j, int i2) {
    }

    default void onInterfaceDnsServerInfo(String str, long j, String[] strArr) {
    }

    default void onInterfaceLinkStateChanged(String str, boolean z) {
    }

    default void onInterfaceRemoved(String str) {
    }

    default void onQuotaLimitReached(String str, String str2) {
    }

    default void onRouteRemoved(RouteInfo routeInfo) {
    }

    default void onRouteUpdated(RouteInfo routeInfo) {
    }
}
