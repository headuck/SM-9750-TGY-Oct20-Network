package android.net.p000ip;

import android.net.InetAddresses;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import com.android.server.NetworkObserver;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/* renamed from: android.net.ip.IpClientLinkObserver */
public class IpClientLinkObserver implements NetworkObserver {
    private final Callback mCallback;
    private DnsServerRepository mDnsServerRepository;
    private final String mInterfaceName;
    private final LinkProperties mLinkProperties = new LinkProperties();
    private final String mTag;

    /* renamed from: android.net.ip.IpClientLinkObserver$Callback */
    public interface Callback {
        void update();
    }

    private void maybeLog(String str, Object obj) {
    }

    private void maybeLog(String str, String str2, LinkAddress linkAddress) {
    }

    public IpClientLinkObserver(String str, Callback callback) {
        this.mTag = "NetlinkTracker/" + str;
        this.mInterfaceName = str;
        this.mCallback = callback;
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        this.mDnsServerRepository = new DnsServerRepository();
    }

    @Override // com.android.server.NetworkObserver
    public void onInterfaceRemoved(String str) {
        maybeLog("interfaceRemoved", str);
        if (this.mInterfaceName.equals(str)) {
            clearLinkProperties();
            this.mCallback.update();
        }
    }

    @Override // com.android.server.NetworkObserver
    public void onInterfaceAddressUpdated(LinkAddress linkAddress, String str) {
        boolean addLinkAddress;
        if (this.mInterfaceName.equals(str)) {
            maybeLog("addressUpdated", str, linkAddress);
            synchronized (this) {
                addLinkAddress = this.mLinkProperties.addLinkAddress(linkAddress);
            }
            if (addLinkAddress) {
                this.mCallback.update();
            }
        }
    }

    @Override // com.android.server.NetworkObserver
    public void onInterfaceAddressRemoved(LinkAddress linkAddress, String str) {
        boolean removeLinkAddress;
        if (this.mInterfaceName.equals(str)) {
            maybeLog("addressRemoved", str, linkAddress);
            synchronized (this) {
                removeLinkAddress = this.mLinkProperties.removeLinkAddress(linkAddress);
            }
            if (removeLinkAddress) {
                this.mCallback.update();
            }
        }
    }

    @Override // com.android.server.NetworkObserver
    public void onRouteUpdated(RouteInfo routeInfo) {
        boolean addRoute;
        if (this.mInterfaceName.equals(routeInfo.getInterface())) {
            maybeLog("routeUpdated", routeInfo);
            synchronized (this) {
                addRoute = this.mLinkProperties.addRoute(routeInfo);
            }
            if (addRoute) {
                this.mCallback.update();
            }
        }
    }

    @Override // com.android.server.NetworkObserver
    public void onRouteRemoved(RouteInfo routeInfo) {
        boolean removeRoute;
        if (this.mInterfaceName.equals(routeInfo.getInterface())) {
            maybeLog("routeRemoved", routeInfo);
            synchronized (this) {
                removeRoute = this.mLinkProperties.removeRoute(routeInfo);
            }
            if (removeRoute) {
                this.mCallback.update();
            }
        }
    }

    @Override // com.android.server.NetworkObserver
    public void onInterfaceDnsServerInfo(String str, long j, String[] strArr) {
        if (this.mInterfaceName.equals(str)) {
            maybeLog("interfaceDnsServerInfo", Arrays.toString(strArr));
            if (this.mDnsServerRepository.addServers(j, strArr)) {
                synchronized (this) {
                    this.mDnsServerRepository.setDnsServersOn(this.mLinkProperties);
                }
                this.mCallback.update();
            }
        }
    }

    public synchronized LinkProperties getLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    public synchronized void clearLinkProperties() {
        this.mDnsServerRepository = new DnsServerRepository();
        this.mLinkProperties.clear();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    /* access modifiers changed from: private */
    /* renamed from: android.net.ip.IpClientLinkObserver$DnsServerRepository */
    public static class DnsServerRepository {
        private ArrayList<DnsServerEntry> mAllServers = new ArrayList<>(12);
        private Set<InetAddress> mCurrentServers = new HashSet();
        private HashMap<InetAddress, DnsServerEntry> mIndex = new HashMap<>(12);

        DnsServerRepository() {
        }

        public synchronized void setDnsServersOn(LinkProperties linkProperties) {
            linkProperties.setDnsServers(this.mCurrentServers);
        }

        public synchronized boolean addServers(long j, String[] strArr) {
            long currentTimeMillis = System.currentTimeMillis();
            long j2 = (j * 1000) + currentTimeMillis;
            for (String str : strArr) {
                try {
                    InetAddress parseNumericAddress = InetAddresses.parseNumericAddress(str);
                    if (!updateExistingEntry(parseNumericAddress, j2) && j2 > currentTimeMillis) {
                        DnsServerEntry dnsServerEntry = new DnsServerEntry(parseNumericAddress, j2);
                        this.mAllServers.add(dnsServerEntry);
                        this.mIndex.put(parseNumericAddress, dnsServerEntry);
                    }
                } catch (IllegalArgumentException unused) {
                }
            }
            Collections.sort(this.mAllServers);
            return updateCurrentServers();
        }

        private synchronized boolean updateExistingEntry(InetAddress inetAddress, long j) {
            DnsServerEntry dnsServerEntry = this.mIndex.get(inetAddress);
            if (dnsServerEntry == null) {
                return false;
            }
            dnsServerEntry.expiry = j;
            return true;
        }

        private synchronized boolean updateCurrentServers() {
            boolean z;
            long currentTimeMillis = System.currentTimeMillis();
            z = false;
            int size = this.mAllServers.size() - 1;
            while (size >= 0 && (size >= 12 || this.mAllServers.get(size).expiry < currentTimeMillis)) {
                DnsServerEntry remove = this.mAllServers.remove(size);
                this.mIndex.remove(remove.address);
                z |= this.mCurrentServers.remove(remove.address);
                size--;
            }
            Iterator<DnsServerEntry> it = this.mAllServers.iterator();
            while (it.hasNext()) {
                DnsServerEntry next = it.next();
                if (this.mCurrentServers.size() >= 3) {
                    break;
                }
                z |= this.mCurrentServers.add(next.address);
            }
            return z;
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: android.net.ip.IpClientLinkObserver$DnsServerEntry */
    public static class DnsServerEntry implements Comparable<DnsServerEntry> {
        public final InetAddress address;
        public long expiry;

        DnsServerEntry(InetAddress inetAddress, long j) throws IllegalArgumentException {
            this.address = inetAddress;
            this.expiry = j;
        }

        public int compareTo(DnsServerEntry dnsServerEntry) {
            return Long.compare(dnsServerEntry.expiry, this.expiry);
        }
    }
}
