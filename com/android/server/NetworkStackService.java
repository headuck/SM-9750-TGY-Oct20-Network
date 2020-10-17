package com.android.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.IIpMemoryStore;
import android.net.IIpMemoryStoreCallbacks;
import android.net.INetd;
import android.net.INetworkMonitor;
import android.net.INetworkMonitorCallbacks;
import android.net.INetworkStackConnector;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.PrivateDnsConfigParcel;
import android.net.dhcp.DhcpServer;
import android.net.dhcp.DhcpServingParams;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.networkstack.util.IndentingPrintWriter;
import android.net.p000ip.IIpClientCallbacks;
import android.net.p000ip.IpClient;
import android.net.shared.PrivateDnsConfig;
import android.net.util.SharedLog;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.ArraySet;
import com.android.server.connectivity.NetworkMonitor;
import com.android.server.connectivity.ipmemorystore.IpMemoryStoreService;
import com.android.server.util.PermissionUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class NetworkStackService extends Service {
    private static final String TAG = "NetworkStackService";
    private static NetworkStackConnector sConnector;

    public interface NetworkStackServiceManager {
        IIpMemoryStore getIpMemoryStoreService();
    }

    public static synchronized IBinder makeConnector(Context context) {
        NetworkStackConnector networkStackConnector;
        synchronized (NetworkStackService.class) {
            if (sConnector == null) {
                sConnector = new NetworkStackConnector(context);
            }
            networkStackConnector = sConnector;
        }
        return networkStackConnector;
    }

    public IBinder onBind(Intent intent) {
        return makeConnector(this);
    }

    /* access modifiers changed from: private */
    public static class NetworkStackConnector extends INetworkStackConnector.Stub implements NetworkStackServiceManager {
        private final ConnectivityManager mCm;
        private final Context mContext;
        private final ArraySet<Integer> mFrameworkAidlVersions = new ArraySet<>(1);
        private final ArrayList<WeakReference<IpClient>> mIpClients = new ArrayList<>();
        private final IpMemoryStoreService mIpMemoryStoreService;
        private final SharedLog mLog = new SharedLog(NetworkStackService.TAG);
        private final INetd mNetd;
        private final int mNetdAidlVersion;
        private final NetworkObserverRegistry mObserverRegistry;
        private final ArrayDeque<SharedLog> mValidationLogs = new ArrayDeque<>(10);

        @Override // android.net.INetworkStackConnector
        public int getInterfaceVersion() {
            return 3;
        }

        private SharedLog addValidationLogs(Network network, String str) {
            SharedLog sharedLog = new SharedLog(20, network + " - " + str);
            synchronized (this.mValidationLogs) {
                while (this.mValidationLogs.size() >= 10) {
                    this.mValidationLogs.removeLast();
                }
                this.mValidationLogs.addFirst(sharedLog);
            }
            return sharedLog;
        }

        NetworkStackConnector(Context context) {
            int i;
            this.mContext = context;
            this.mNetd = INetd.Stub.asInterface((IBinder) context.getSystemService("netd"));
            this.mObserverRegistry = new NetworkObserverRegistry();
            this.mCm = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
            this.mIpMemoryStoreService = new IpMemoryStoreService(context);
            try {
                i = this.mNetd.getInterfaceVersion();
            } catch (RemoteException e) {
                this.mLog.mo564e("Error obtaining INetd version", e);
                i = -1;
            }
            this.mNetdAidlVersion = i;
            try {
                this.mObserverRegistry.register(this.mNetd);
            } catch (RemoteException e2) {
                this.mLog.mo564e("Error registering observer on Netd", e2);
            }
        }

        private void updateSystemAidlVersion(int i) {
            synchronized (this.mFrameworkAidlVersions) {
                this.mFrameworkAidlVersions.add(Integer.valueOf(i));
            }
        }

        @Override // android.net.INetworkStackConnector
        public void makeDhcpServer(String str, DhcpServingParamsParcel dhcpServingParamsParcel, IDhcpServerCallbacks iDhcpServerCallbacks) throws RemoteException {
            PermissionUtil.checkNetworkStackCallingPermission();
            updateSystemAidlVersion(iDhcpServerCallbacks.getInterfaceVersion());
            try {
                DhcpServingParams fromParcelableObject = DhcpServingParams.fromParcelableObject(dhcpServingParamsParcel);
                SharedLog sharedLog = this.mLog;
                iDhcpServerCallbacks.onDhcpServerCreated(1, new DhcpServer(str, fromParcelableObject, sharedLog.forSubComponent(str + ".DHCP")));
            } catch (DhcpServingParams.InvalidParameterException e) {
                this.mLog.mo564e("Invalid DhcpServingParams", e);
                iDhcpServerCallbacks.onDhcpServerCreated(2, null);
            } catch (Exception e2) {
                this.mLog.mo564e("Unknown error starting DhcpServer", e2);
                iDhcpServerCallbacks.onDhcpServerCreated(3, null);
            }
        }

        @Override // android.net.INetworkStackConnector
        public void makeNetworkMonitor(Network network, String str, INetworkMonitorCallbacks iNetworkMonitorCallbacks) throws RemoteException {
            PermissionUtil.checkNetworkStackCallingPermission();
            updateSystemAidlVersion(iNetworkMonitorCallbacks.getInterfaceVersion());
            iNetworkMonitorCallbacks.onNetworkMonitorCreated(new NetworkMonitorImpl(new NetworkMonitor(this.mContext, iNetworkMonitorCallbacks, network, addValidationLogs(network, str))));
        }

        @Override // android.net.INetworkStackConnector
        public void makeIpClient(String str, IIpClientCallbacks iIpClientCallbacks) throws RemoteException {
            PermissionUtil.checkNetworkStackCallingPermission();
            updateSystemAidlVersion(iIpClientCallbacks.getInterfaceVersion());
            IpClient ipClient = new IpClient(this.mContext, str, iIpClientCallbacks, this.mObserverRegistry, this);
            synchronized (this.mIpClients) {
                Iterator<WeakReference<IpClient>> it = this.mIpClients.iterator();
                while (it.hasNext()) {
                    if (it.next().get() == null) {
                        it.remove();
                    }
                }
                this.mIpClients.add(new WeakReference<>(ipClient));
            }
            iIpClientCallbacks.onIpClientCreated(ipClient.makeConnector());
        }

        @Override // com.android.server.NetworkStackService.NetworkStackServiceManager
        public IIpMemoryStore getIpMemoryStoreService() {
            return this.mIpMemoryStoreService;
        }

        @Override // android.net.INetworkStackConnector
        public void fetchIpMemoryStore(IIpMemoryStoreCallbacks iIpMemoryStoreCallbacks) throws RemoteException {
            PermissionUtil.checkNetworkStackCallingPermission();
            updateSystemAidlVersion(iIpMemoryStoreCallbacks.getInterfaceVersion());
            iIpMemoryStoreCallbacks.onIpMemoryStoreFetched(this.mIpMemoryStoreService);
        }

        /* access modifiers changed from: protected */
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            PermissionUtil.checkDumpPermission();
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            indentingPrintWriter.println("NetworkStack version:");
            dumpVersion(indentingPrintWriter);
            indentingPrintWriter.println();
            if (strArr == null || strArr.length < 1 || !"version".equals(strArr[0])) {
                indentingPrintWriter.println("NetworkStack logs:");
                this.mLog.dump(fileDescriptor, indentingPrintWriter, strArr);
                indentingPrintWriter.println();
                indentingPrintWriter.println("Recently active IpClient logs:");
                ArrayList arrayList = new ArrayList();
                HashSet hashSet = new HashSet();
                synchronized (this.mIpClients) {
                    Iterator<WeakReference<IpClient>> it = this.mIpClients.iterator();
                    while (it.hasNext()) {
                        IpClient ipClient = it.next().get();
                        if (ipClient != null) {
                            arrayList.add(ipClient);
                        }
                    }
                }
                Iterator it2 = arrayList.iterator();
                while (it2.hasNext()) {
                    IpClient ipClient2 = (IpClient) it2.next();
                    indentingPrintWriter.println(ipClient2.getName());
                    indentingPrintWriter.increaseIndent();
                    ipClient2.dump(fileDescriptor, indentingPrintWriter, strArr);
                    indentingPrintWriter.decreaseIndent();
                    hashSet.add(ipClient2.getInterfaceName());
                }
                indentingPrintWriter.println();
                indentingPrintWriter.println("Other IpClient logs:");
                IpClient.dumpAllLogs(printWriter, hashSet);
                indentingPrintWriter.println();
                indentingPrintWriter.println("Validation logs (most recent first):");
                synchronized (this.mValidationLogs) {
                    Iterator<SharedLog> it3 = this.mValidationLogs.iterator();
                    while (it3.hasNext()) {
                        SharedLog next = it3.next();
                        indentingPrintWriter.println(next.getTag());
                        indentingPrintWriter.increaseIndent();
                        next.dump(fileDescriptor, indentingPrintWriter, strArr);
                        indentingPrintWriter.decreaseIndent();
                    }
                }
            }
        }

        private void dumpVersion(PrintWriter printWriter) {
            printWriter.println("NetworkStackConnector: " + 3);
            synchronized (this.mFrameworkAidlVersions) {
                printWriter.println("SystemServer: " + this.mFrameworkAidlVersions);
            }
            printWriter.println("Netd: " + this.mNetdAidlVersion);
        }
    }

    private static class NetworkMonitorImpl extends INetworkMonitor.Stub {
        private final NetworkMonitor mNm;

        @Override // android.net.INetworkMonitor
        public int getInterfaceVersion() {
            return 3;
        }

        NetworkMonitorImpl(NetworkMonitor networkMonitor) {
            this.mNm = networkMonitor;
        }

        @Override // android.net.INetworkMonitor
        public void start() {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.start();
        }

        @Override // android.net.INetworkMonitor
        public void launchCaptivePortalApp() {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.launchCaptivePortalApp();
        }

        @Override // android.net.INetworkMonitor
        public void notifyCaptivePortalAppFinished(int i) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyCaptivePortalAppFinished(i);
        }

        @Override // android.net.INetworkMonitor
        public void setAcceptPartialConnectivity() {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.setAcceptPartialConnectivity();
        }

        @Override // android.net.INetworkMonitor
        public void forceReevaluation(int i) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.forceReevaluation(i);
        }

        @Override // android.net.INetworkMonitor
        public void notifyPrivateDnsChanged(PrivateDnsConfigParcel privateDnsConfigParcel) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyPrivateDnsSettingsChanged(PrivateDnsConfig.fromParcel(privateDnsConfigParcel));
        }

        @Override // android.net.INetworkMonitor
        public void notifyDnsResponse(int i) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyDnsResponse(i);
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkConnected(LinkProperties linkProperties, NetworkCapabilities networkCapabilities) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyNetworkConnected(linkProperties, networkCapabilities);
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkDisconnected() {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyNetworkDisconnected();
        }

        @Override // android.net.INetworkMonitor
        public void notifyLinkPropertiesChanged(LinkProperties linkProperties) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyLinkPropertiesChanged(linkProperties);
        }

        @Override // android.net.INetworkMonitor
        public void notifyNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities) {
            PermissionUtil.checkNetworkStackCallingPermission();
            this.mNm.notifyNetworkCapabilitiesChanged(networkCapabilities);
        }
    }
}
