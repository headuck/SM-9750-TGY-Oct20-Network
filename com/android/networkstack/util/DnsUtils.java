package com.android.networkstack.util;

import android.net.DnsResolver;
import android.net.Network;
import android.net.TrafficStats;
import android.util.Log;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DnsUtils {
    private static final String TAG = "DnsUtils";

    public static InetAddress[] getAllByName(DnsResolver dnsResolver, Network network, String str, int i) throws UnknownHostException {
        ArrayList arrayList = new ArrayList();
        try {
            arrayList.addAll(Arrays.asList(getAllByName(dnsResolver, network, str, 28, 4, i)));
        } catch (UnknownHostException unused) {
        }
        try {
            arrayList.addAll(Arrays.asList(getAllByName(dnsResolver, network, str, 1, 4, i)));
        } catch (UnknownHostException unused2) {
        }
        if (arrayList.size() != 0) {
            return (InetAddress[]) arrayList.toArray(new InetAddress[0]);
        }
        throw new UnknownHostException(str);
    }

    public static InetAddress[] getAllByName(DnsResolver dnsResolver, Network network, final String str, int i, int i2, int i3) throws UnknownHostException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference atomicReference = new AtomicReference();
        C00241 r7 = new DnsResolver.Callback<List<InetAddress>>() {
            /* class com.android.networkstack.util.DnsUtils.C00241 */

            public void onAnswer(List<InetAddress> list, int i) {
                if (i == 0) {
                    atomicReference.set(list);
                }
                countDownLatch.countDown();
            }

            public void onError(DnsResolver.DnsException dnsException) {
                String str = DnsUtils.TAG;
                Log.d(str, "DNS error resolving " + str + ": " + dnsException.getMessage());
                countDownLatch.countDown();
            }
        };
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-127);
        if (i == -1) {
            dnsResolver.query(network, str, i2, $$Lambda$DnsUtils$wfjeqM32iamO5uPDgvFfbCPZOg.INSTANCE, null, r7);
        } else {
            dnsResolver.query(network, str, i, i2, $$Lambda$DnsUtils$yTVtff6rgdpoAiwKNFKah2CA4.INSTANCE, null, r7);
        }
        TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        try {
            countDownLatch.await((long) i3, TimeUnit.MILLISECONDS);
        } catch (InterruptedException unused) {
        }
        List list = (List) atomicReference.get();
        if (list != null && list.size() != 0) {
            return (InetAddress[]) list.toArray(new InetAddress[0]);
        }
        throw new UnknownHostException(str);
    }
}
