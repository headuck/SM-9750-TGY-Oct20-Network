package android.net.p000ip;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NattKeepalivePacketDataParcelable;
import android.net.NetworkStackIpMemoryStore;
import android.net.ProvisioningConfigurationParcelable;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.TcpKeepalivePacketDataParcelable;
import android.net.apf.ApfCapabilities;
import android.net.apf.ApfFilter;
import android.net.dhcp.DhcpClient;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpManagerEvent;
import android.net.networkstack.DhcpResults;
import android.net.networkstack.util.IState;
import android.net.networkstack.util.IndentingPrintWriter;
import android.net.networkstack.util.MessageUtils;
import android.net.networkstack.util.Preconditions;
import android.net.networkstack.util.State;
import android.net.networkstack.util.StateMachine;
import android.net.networkstack.util.WakeupMessage;
import android.net.p000ip.IIpClient;
import android.net.p000ip.IpClient;
import android.net.p000ip.IpClientLinkObserver;
import android.net.p000ip.IpReachabilityMonitor;
import android.net.shared.InitialConfiguration;
import android.net.shared.IpConfigurationParcelableUtil;
import android.net.shared.ProvisioningConfiguration;
import android.net.util.InterfaceParams;
import android.net.util.SharedLog;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.server.NetworkObserverRegistry;
import com.android.server.NetworkStackService;
import com.android.server.util.PermissionUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* renamed from: android.net.ip.IpClient */
public class IpClient extends StateMachine {
    private static final int CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF = 13;
    private static final int CMD_CONFIRM = 4;
    private static final int CMD_JUMP_RUNNING_TO_STOPPING = 101;
    private static final int CMD_JUMP_STARTED_TO_RUNNING = 100;
    private static final int CMD_JUMP_STOPPING_TO_STOPPED = 102;
    private static final int CMD_RELEASE = 20;
    private static final int CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF = 14;
    private static final int CMD_SET_MULTICAST_FILTER = 9;
    private static final int CMD_START = 3;
    private static final int CMD_STOP = 2;
    private static final int CMD_TERMINATE_AFTER_STOP = 1;
    private static final int CMD_UPDATE_HTTP_PROXY = 8;
    private static final int CMD_UPDATE_L2KEY_GROUPHINT = 15;
    private static final int CMD_UPDATE_TCP_BUFFER_SIZES = 7;
    private static final int EVENT_DHCPACTION_TIMEOUT = 11;
    private static final int EVENT_NETLINK_LINKPROPERTIES_CHANGED = 6;
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 5;
    private static final int EVENT_PROVISIONING_TIMEOUT = 10;
    private static final int EVENT_READ_PACKET_FILTER_COMPLETE = 12;
    private static final Class[] sMessageClasses;
    private static final ConcurrentHashMap<String, LocalLog> sPktLogs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, SharedLog> sSmLogs = new ConcurrentHashMap<>();
    private static final SparseArray<String> sWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private final ConditionVariable mApfDataSnapshotComplete;
    private ApfFilter mApfFilter;
    protected final IpClientCallbacksWrapper mCallback;
    private final String mClatInterfaceName;
    private final ConnectivityManager mCm;
    private ProvisioningConfiguration mConfiguration;
    private final LocalLog mConnectivityPacketLog;
    private final Context mContext;
    private final Dependencies mDependencies;
    private final WakeupMessage mDhcpActionTimeoutAlarm;
    private DhcpClient mDhcpClient;
    private DhcpResults mDhcpResults;
    private String mGroupHint;
    private ProxyInfo mHttpProxy;
    private final InterfaceController mInterfaceCtrl;
    private final String mInterfaceName;
    private InterfaceParams mInterfaceParams;
    private final NetworkStackIpMemoryStore mIpMemoryStore;
    private IpReachabilityMonitor mIpReachabilityMonitor;
    private String mL2Key;
    private final IpClientLinkObserver mLinkObserver;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MessageHandlingLogger mMsgStateLogger;
    private boolean mMulticastFiltering;
    private final INetd mNetd;
    private final NetworkObserverRegistry mObserverRegistry;
    private final WakeupMessage mProvisioningTimeoutAlarm;
    private final State mRunningState;
    private final CountDownLatch mShutdownLatch;
    private long mStartTimeMillis;
    private final State mStartedState;
    private final State mStoppedState;
    private final State mStoppingState;
    private final String mTag;
    private String mTcpBufferSizes;

    private void maybeSaveNetworkToIpMemoryStore() {
    }

    static {
        Class[] clsArr = new Class[CMD_STOP];
        clsArr[0] = IpClient.class;
        clsArr[CMD_TERMINATE_AFTER_STOP] = DhcpClient.class;
        sMessageClasses = clsArr;
    }

    public static void dumpAllLogs(PrintWriter printWriter, Set<String> set) {
        for (String str : sSmLogs.keySet()) {
            if (!set.contains(str)) {
                Object[] objArr = new Object[CMD_TERMINATE_AFTER_STOP];
                objArr[0] = str;
                printWriter.println(String.format("--- BEGIN %s ---", objArr));
                SharedLog sharedLog = sSmLogs.get(str);
                if (sharedLog != null) {
                    printWriter.println("State machine log:");
                    sharedLog.dump(null, printWriter, null);
                }
                printWriter.println("");
                LocalLog localLog = sPktLogs.get(str);
                if (localLog != null) {
                    printWriter.println("Connectivity packet log:");
                    localLog.readOnlyLocalLog().dump(null, printWriter, null);
                }
                Object[] objArr2 = new Object[CMD_TERMINATE_AFTER_STOP];
                objArr2[0] = str;
                printWriter.println(String.format("--- END %s ---", objArr2));
            }
        }
    }

    /* renamed from: android.net.ip.IpClient$IpClientCallbacksWrapper */
    public static class IpClientCallbacksWrapper {
        private final IIpClientCallbacks mCallback;
        private final SharedLog mLog;

        protected IpClientCallbacksWrapper(IIpClientCallbacks iIpClientCallbacks, SharedLog sharedLog) {
            this.mCallback = iIpClientCallbacks;
            this.mLog = sharedLog;
        }

        private void log(String str) {
            SharedLog sharedLog = this.mLog;
            sharedLog.log("INVOKE " + str);
        }

        private void log(String str, Throwable th) {
            SharedLog sharedLog = this.mLog;
            sharedLog.mo564e("INVOKE " + str, th);
        }

        public void onPreDhcpAction() {
            log("onPreDhcpAction()");
            try {
                this.mCallback.onPreDhcpAction();
            } catch (RemoteException e) {
                log("Failed to call onPreDhcpAction", e);
            }
        }

        public void onPostDhcpAction() {
            log("onPostDhcpAction()");
            try {
                this.mCallback.onPostDhcpAction();
            } catch (RemoteException e) {
                log("Failed to call onPostDhcpAction", e);
            }
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            log("onNewDhcpResults({" + dhcpResults + "})");
            try {
                this.mCallback.onNewDhcpResults(IpConfigurationParcelableUtil.toStableParcelable(dhcpResults));
            } catch (RemoteException e) {
                log("Failed to call onNewDhcpResults", e);
            }
        }

        public void onProvisioningSuccess(LinkProperties linkProperties) {
            log("onProvisioningSuccess({" + linkProperties + "})");
            try {
                this.mCallback.onProvisioningSuccess(linkProperties);
            } catch (RemoteException e) {
                log("Failed to call onProvisioningSuccess", e);
            }
        }

        public void onProvisioningFailure(LinkProperties linkProperties) {
            log("onProvisioningFailure({" + linkProperties + "})");
            try {
                this.mCallback.onProvisioningFailure(linkProperties);
            } catch (RemoteException e) {
                log("Failed to call onProvisioningFailure", e);
            }
        }

        public void onLinkPropertiesChange(LinkProperties linkProperties) {
            log("onLinkPropertiesChange({" + linkProperties + "})");
            try {
                this.mCallback.onLinkPropertiesChange(linkProperties);
            } catch (RemoteException e) {
                log("Failed to call onLinkPropertiesChange", e);
            }
        }

        public void onReachabilityLost(String str) {
            log("onReachabilityLost(" + str + ")");
            try {
                this.mCallback.onReachabilityLost(str);
            } catch (RemoteException e) {
                log("Failed to call onReachabilityLost", e);
            }
        }

        public void onQuit() {
            log("onQuit()");
            try {
                this.mCallback.onQuit();
            } catch (RemoteException e) {
                log("Failed to call onQuit", e);
            }
        }

        public void installPacketFilter(byte[] bArr) {
            log("installPacketFilter(byte[" + bArr.length + "])");
            try {
                this.mCallback.installPacketFilter(bArr);
            } catch (RemoteException e) {
                log("Failed to call installPacketFilter", e);
            }
        }

        public void startReadPacketFilter() {
            log("startReadPacketFilter()");
            try {
                this.mCallback.startReadPacketFilter();
            } catch (RemoteException e) {
                log("Failed to call startReadPacketFilter", e);
            }
        }

        public void setFallbackMulticastFilter(boolean z) {
            log("setFallbackMulticastFilter(" + z + ")");
            try {
                this.mCallback.setFallbackMulticastFilter(z);
            } catch (RemoteException e) {
                log("Failed to call setFallbackMulticastFilter", e);
            }
        }

        public void setNeighborDiscoveryOffload(boolean z) {
            log("setNeighborDiscoveryOffload(" + z + ")");
            try {
                this.mCallback.setNeighborDiscoveryOffload(z);
            } catch (RemoteException e) {
                log("Failed to call setNeighborDiscoveryOffload", e);
            }
        }
    }

    /* renamed from: android.net.ip.IpClient$Dependencies */
    public static class Dependencies {
        public InterfaceParams getInterfaceParams(String str) {
            return InterfaceParams.getByName(str);
        }

        public INetd getNetd(Context context) {
            return INetd.Stub.asInterface((IBinder) context.getSystemService("netd"));
        }
    }

    public IpClient(Context context, String str, IIpClientCallbacks iIpClientCallbacks, NetworkObserverRegistry networkObserverRegistry, NetworkStackService.NetworkStackServiceManager networkStackServiceManager) {
        this(context, str, iIpClientCallbacks, networkObserverRegistry, networkStackServiceManager, new Dependencies());
    }

    IpClient(Context context, String str, IIpClientCallbacks iIpClientCallbacks, NetworkObserverRegistry networkObserverRegistry, NetworkStackService.NetworkStackServiceManager networkStackServiceManager, Dependencies dependencies) {
        super(IpClient.class.getSimpleName() + "." + str);
        this.mStoppedState = new StoppedState();
        this.mStoppingState = new StoppingState();
        this.mStartedState = new StartedState();
        this.mRunningState = new RunningState();
        this.mMetricsLog = new IpConnectivityLog();
        this.mApfDataSnapshotComplete = new ConditionVariable();
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(iIpClientCallbacks);
        this.mTag = getName();
        this.mContext = context;
        this.mInterfaceName = str;
        this.mClatInterfaceName = "v4-" + str;
        this.mDependencies = dependencies;
        this.mShutdownLatch = new CountDownLatch(CMD_TERMINATE_AFTER_STOP);
        this.mCm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mObserverRegistry = networkObserverRegistry;
        this.mIpMemoryStore = new NetworkStackIpMemoryStore(context, networkStackServiceManager.getIpMemoryStoreService());
        sSmLogs.putIfAbsent(this.mInterfaceName, new SharedLog(500, this.mTag));
        this.mLog = sSmLogs.get(this.mInterfaceName);
        sPktLogs.putIfAbsent(this.mInterfaceName, new LocalLog(CMD_JUMP_STARTED_TO_RUNNING));
        this.mConnectivityPacketLog = sPktLogs.get(this.mInterfaceName);
        this.mMsgStateLogger = new MessageHandlingLogger();
        this.mCallback = new IpClientCallbacksWrapper(iIpClientCallbacks, this.mLog);
        this.mNetd = dependencies.getNetd(this.mContext);
        this.mInterfaceCtrl = new InterfaceController(this.mInterfaceName, this.mNetd, this.mLog);
        this.mLinkObserver = new IpClientLinkObserver(this.mInterfaceName, new IpClientLinkObserver.Callback() {
            /* class android.net.p000ip.$$Lambda$IpClient$Xbs0rzp92CjTlBulMQZy1zSmqUE */

            @Override // android.net.p000ip.IpClientLinkObserver.Callback
            public final void update() {
                IpClient.this.lambda$new$0$IpClient();
            }
        }) {
            /* class android.net.p000ip.IpClient.C00151 */

            @Override // com.android.server.NetworkObserver
            public void onInterfaceAdded(String str) {
                super.onInterfaceAdded(str);
                if (IpClient.this.mClatInterfaceName.equals(str)) {
                    IpClient.this.mCallback.setNeighborDiscoveryOffload(false);
                } else if (!IpClient.this.mInterfaceName.equals(str)) {
                    return;
                }
                logMsg("interfaceAdded(" + str + ")");
            }

            @Override // android.net.p000ip.IpClientLinkObserver, com.android.server.NetworkObserver
            public void onInterfaceRemoved(String str) {
                super.onInterfaceRemoved(str);
                if (IpClient.this.mClatInterfaceName.equals(str)) {
                    IpClient.this.mCallback.setNeighborDiscoveryOffload(true);
                } else if (!IpClient.this.mInterfaceName.equals(str)) {
                    return;
                }
                logMsg("interfaceRemoved(" + str + ")");
            }

            private void logMsg(String str) {
                Log.d(IpClient.this.mTag, str);
                IpClient.this.getHandler().post(new Runnable(str) {
                    /* class android.net.p000ip.$$Lambda$IpClient$1$9fQ1SZgZXSwOLpm6cTU88p3I8c */
                    private final /* synthetic */ String f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        IpClient.C00151.this.lambda$logMsg$0$IpClient$1(this.f$1);
                    }
                });
            }

            public /* synthetic */ void lambda$logMsg$0$IpClient$1(String str) {
                SharedLog sharedLog = IpClient.this.mLog;
                sharedLog.log("OBSERVED " + str);
            }
        };
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        Context context2 = this.mContext;
        Handler handler = getHandler();
        this.mProvisioningTimeoutAlarm = new WakeupMessage(context2, handler, this.mTag + ".EVENT_PROVISIONING_TIMEOUT", EVENT_PROVISIONING_TIMEOUT);
        Context context3 = this.mContext;
        Handler handler2 = getHandler();
        this.mDhcpActionTimeoutAlarm = new WakeupMessage(context3, handler2, this.mTag + ".EVENT_DHCPACTION_TIMEOUT", EVENT_DHCPACTION_TIMEOUT);
        configureAndStartStateMachine();
        startStateMachineUpdaters();
    }

    public /* synthetic */ void lambda$new$0$IpClient() {
        sendMessage(EVENT_NETLINK_LINKPROPERTIES_CHANGED);
    }

    public IIpClient makeConnector() {
        return new IpClientConnector();
    }

    /* renamed from: android.net.ip.IpClient$IpClientConnector */
    class IpClientConnector extends IIpClient.Stub {
        @Override // android.net.p000ip.IIpClient
        public int getInterfaceVersion() {
            return IpClient.CMD_START;
        }

        IpClientConnector() {
        }

        @Override // android.net.p000ip.IIpClient
        public void completedPreDhcpAction() {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.completedPreDhcpAction();
        }

        @Override // android.net.p000ip.IIpClient
        public void confirmConfiguration() {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.confirmConfiguration();
        }

        @Override // android.net.p000ip.IIpClient
        public void readPacketFilterComplete(byte[] bArr) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.readPacketFilterComplete(bArr);
        }

        @Override // android.net.p000ip.IIpClient
        public void shutdown() {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.shutdown();
        }

        @Override // android.net.p000ip.IIpClient
        public void startProvisioning(ProvisioningConfigurationParcelable provisioningConfigurationParcelable) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.startProvisioning(ProvisioningConfiguration.fromStableParcelable(provisioningConfigurationParcelable));
        }

        @Override // android.net.p000ip.IIpClient
        public void stop() {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.stop();
        }

        @Override // android.net.p000ip.IIpClient
        public void setL2KeyAndGroupHint(String str, String str2) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.setL2KeyAndGroupHint(str, str2);
        }

        @Override // android.net.p000ip.IIpClient
        public void setTcpBufferSizes(String str) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.setTcpBufferSizes(str);
        }

        @Override // android.net.p000ip.IIpClient
        public void setHttpProxy(ProxyInfo proxyInfo) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.setHttpProxy(proxyInfo);
        }

        @Override // android.net.p000ip.IIpClient
        public void setMulticastFilter(boolean z) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.setMulticastFilter(z);
        }

        @Override // android.net.p000ip.IIpClient
        public void addKeepalivePacketFilter(int i, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.addKeepalivePacketFilter(i, tcpKeepalivePacketDataParcelable);
        }

        @Override // android.net.p000ip.IIpClient
        public void addNattKeepalivePacketFilter(int i, NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.addNattKeepalivePacketFilter(i, nattKeepalivePacketDataParcelable);
        }

        @Override // android.net.p000ip.IIpClient
        public void removeKeepalivePacketFilter(int i) {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.removeKeepalivePacketFilter(i);
        }

        @Override // android.net.p000ip.IIpClient
        public void sendDhcpReleasePacket() {
            PermissionUtil.checkNetworkStackCallingPermission();
            IpClient.this.sendDhcpReleasePacket();
        }
    }

    public String getInterfaceName() {
        return this.mInterfaceName;
    }

    private void configureAndStartStateMachine() {
        addState(this.mStoppedState);
        addState(this.mStartedState);
        addState(this.mRunningState, this.mStartedState);
        addState(this.mStoppingState);
        setInitialState(this.mStoppedState);
        super.start();
    }

    private void startStateMachineUpdaters() {
        this.mObserverRegistry.registerObserverForNonblockingCallback(this.mLinkObserver);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopStateMachineUpdaters() {
        this.mObserverRegistry.unregisterObserver(this.mLinkObserver);
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public void onQuitting() {
        this.mCallback.onQuit();
        this.mShutdownLatch.countDown();
    }

    public void shutdown() {
        stop();
        sendMessage(CMD_TERMINATE_AFTER_STOP);
    }

    public void startProvisioning(ProvisioningConfiguration provisioningConfiguration) {
        if (!provisioningConfiguration.isValid()) {
            doImmediateProvisioningFailure(CMD_UPDATE_TCP_BUFFER_SIZES);
            return;
        }
        this.mInterfaceParams = this.mDependencies.getInterfaceParams(this.mInterfaceName);
        if (this.mInterfaceParams == null) {
            logError("Failed to find InterfaceParams for " + this.mInterfaceName, new Object[0]);
            doImmediateProvisioningFailure(CMD_UPDATE_HTTP_PROXY);
            return;
        }
        this.mCallback.setNeighborDiscoveryOffload(true);
        sendMessage(CMD_START, new ProvisioningConfiguration(provisioningConfiguration));
    }

    public void stop() {
        sendMessage(CMD_STOP);
    }

    public void confirmConfiguration() {
        sendMessage(CMD_CONFIRM);
    }

    public void completedPreDhcpAction() {
        sendMessage(EVENT_PRE_DHCP_ACTION_COMPLETE);
    }

    public void readPacketFilterComplete(byte[] bArr) {
        sendMessage(EVENT_READ_PACKET_FILTER_COMPLETE, bArr);
    }

    public void setTcpBufferSizes(String str) {
        sendMessage(CMD_UPDATE_TCP_BUFFER_SIZES, str);
    }

    public void setL2KeyAndGroupHint(String str, String str2) {
        sendMessage(CMD_UPDATE_L2KEY_GROUPHINT, new Pair(str, str2));
    }

    public void setHttpProxy(ProxyInfo proxyInfo) {
        sendMessage(CMD_UPDATE_HTTP_PROXY, proxyInfo);
    }

    public void setMulticastFilter(boolean z) {
        sendMessage(CMD_SET_MULTICAST_FILTER, Boolean.valueOf(z));
    }

    public void addKeepalivePacketFilter(int i, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) {
        sendMessage(CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF, i, 0, tcpKeepalivePacketDataParcelable);
    }

    public void addNattKeepalivePacketFilter(int i, NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) {
        sendMessage(CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF, i, 0, nattKeepalivePacketDataParcelable);
    }

    public void removeKeepalivePacketFilter(int i) {
        sendMessage(CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF, i, 0);
    }

    public void sendDhcpReleasePacket() {
        sendMessage(CMD_RELEASE);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr == null || strArr.length <= 0 || !"confirm".equals(strArr[0])) {
            ApfFilter apfFilter = this.mApfFilter;
            ProvisioningConfiguration provisioningConfiguration = this.mConfiguration;
            ApfCapabilities apfCapabilities = provisioningConfiguration != null ? provisioningConfiguration.mApfCapabilities : null;
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            indentingPrintWriter.println(this.mTag + " APF dump:");
            indentingPrintWriter.increaseIndent();
            if (apfFilter != null) {
                if (apfCapabilities.hasDataAccess()) {
                    this.mApfDataSnapshotComplete.close();
                    this.mCallback.startReadPacketFilter();
                    if (!this.mApfDataSnapshotComplete.block(1000)) {
                        indentingPrintWriter.print("TIMEOUT: DUMPING STALE APF SNAPSHOT");
                    }
                }
                apfFilter.dump(indentingPrintWriter);
            } else {
                indentingPrintWriter.print("No active ApfFilter; ");
                if (provisioningConfiguration == null) {
                    indentingPrintWriter.println("IpClient not yet started.");
                } else if (apfCapabilities == null || apfCapabilities.apfVersionSupported == 0) {
                    indentingPrintWriter.println("Hardware does not support APF.");
                } else {
                    indentingPrintWriter.println("ApfFilter not yet started, APF capabilities: " + apfCapabilities);
                }
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println(this.mTag + " current ProvisioningConfiguration:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(Objects.toString(provisioningConfiguration, "N/A"));
            indentingPrintWriter.decreaseIndent();
            IpReachabilityMonitor ipReachabilityMonitor = this.mIpReachabilityMonitor;
            if (ipReachabilityMonitor != null) {
                indentingPrintWriter.println();
                indentingPrintWriter.println(this.mTag + " current IpReachabilityMonitor state:");
                indentingPrintWriter.increaseIndent();
                ipReachabilityMonitor.dump(indentingPrintWriter);
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.println();
            indentingPrintWriter.println(this.mTag + " StateMachine dump:");
            indentingPrintWriter.increaseIndent();
            this.mLog.dump(fileDescriptor, indentingPrintWriter, strArr);
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println(this.mTag + " connectivity packet log:");
            indentingPrintWriter.println();
            indentingPrintWriter.println("Debug with python and scapy via:");
            indentingPrintWriter.println("shell$ python");
            indentingPrintWriter.println(">>> from scapy import all as scapy");
            indentingPrintWriter.println(">>> scapy.Ether(\"<paste_hex_string>\".decode(\"hex\")).show2()");
            indentingPrintWriter.println();
            indentingPrintWriter.increaseIndent();
            this.mConnectivityPacketLog.readOnlyLocalLog().dump(fileDescriptor, indentingPrintWriter, strArr);
            indentingPrintWriter.decreaseIndent();
            return;
        }
        confirmConfiguration();
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public String getWhatToString(int i) {
        SparseArray<String> sparseArray = sWhatToString;
        return sparseArray.get(i, "UNKNOWN: " + Integer.toString(i));
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public String getLogRecString(Message message) {
        Object[] objArr = new Object[EVENT_NETLINK_LINKPROPERTIES_CHANGED];
        objArr[0] = this.mInterfaceName;
        InterfaceParams interfaceParams = this.mInterfaceParams;
        objArr[CMD_TERMINATE_AFTER_STOP] = Integer.valueOf(interfaceParams == null ? -1 : interfaceParams.index);
        objArr[CMD_STOP] = Integer.valueOf(message.arg1);
        objArr[CMD_START] = Integer.valueOf(message.arg2);
        objArr[CMD_CONFIRM] = Objects.toString(message.obj);
        objArr[EVENT_PRE_DHCP_ACTION_COMPLETE] = this.mMsgStateLogger;
        String format = String.format("%s/%d %d %d %s [%s]", objArr);
        this.mLog.log(getWhatToString(message.what) + " " + format);
        this.mMsgStateLogger.reset();
        return format;
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public boolean recordLogRec(Message message) {
        boolean z = message.what != EVENT_NETLINK_LINKPROPERTIES_CHANGED;
        if (!z) {
            this.mMsgStateLogger.reset();
        }
        return z;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logError(String str, Object... objArr) {
        String str2 = "ERROR " + String.format(str, objArr);
        Log.e(this.mTag, str2);
        this.mLog.log(str2);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void resetLinkProperties() {
        this.mLinkObserver.clearLinkProperties();
        this.mConfiguration = null;
        this.mDhcpResults = null;
        this.mTcpBufferSizes = "";
        this.mHttpProxy = null;
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void recordMetric(int i) {
        long j = 0;
        if (this.mStartTimeMillis > 0) {
            j = SystemClock.elapsedRealtime() - this.mStartTimeMillis;
        }
        this.mMetricsLog.log(this.mInterfaceName, new IpManagerEvent(i, j));
    }

    static boolean isProvisioned(LinkProperties linkProperties, InitialConfiguration initialConfiguration) {
        if (linkProperties.hasIpv4Address() || linkProperties.isProvisioned()) {
            return true;
        }
        if (initialConfiguration == null) {
            return false;
        }
        return initialConfiguration.isProvisionedBy(linkProperties.getLinkAddresses(), linkProperties.getRoutes());
    }

    private int compareProvisioning(LinkProperties linkProperties, LinkProperties linkProperties2) {
        int i;
        ProvisioningConfiguration provisioningConfiguration = this.mConfiguration;
        InitialConfiguration initialConfiguration = provisioningConfiguration != null ? provisioningConfiguration.mInitialConfig : null;
        boolean isProvisioned = isProvisioned(linkProperties, initialConfiguration);
        boolean isProvisioned2 = isProvisioned(linkProperties2, initialConfiguration);
        boolean z = true;
        if (!isProvisioned && isProvisioned2) {
            i = CMD_START;
        } else if (!isProvisioned || !isProvisioned2) {
            i = (isProvisioned || isProvisioned2) ? CMD_STOP : CMD_TERMINATE_AFTER_STOP;
        } else {
            i = CMD_CONFIRM;
        }
        if (linkProperties.isIpv6Provisioned()) {
            linkProperties2.isIpv6Provisioned();
        }
        if (!linkProperties.hasIpv4Address() || linkProperties2.hasIpv4Address()) {
            z = false;
        }
        if (linkProperties.hasIpv6DefaultRoute()) {
            linkProperties2.hasIpv6DefaultRoute();
        }
        if (z) {
            i = CMD_STOP;
        }
        linkProperties.hasGlobalIpv6Address();
        return i;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void dispatchCallback(int i, LinkProperties linkProperties) {
        if (i == CMD_STOP) {
            recordMetric(CMD_STOP);
            this.mCallback.onProvisioningFailure(linkProperties);
        } else if (i != CMD_START) {
            this.mCallback.onLinkPropertiesChange(linkProperties);
        } else {
            recordMetric(CMD_TERMINATE_AFTER_STOP);
            this.mCallback.onProvisioningSuccess(linkProperties);
        }
    }

    private int setLinkProperties(LinkProperties linkProperties) {
        ApfFilter apfFilter = this.mApfFilter;
        if (apfFilter != null) {
            apfFilter.setLinkProperties(linkProperties);
        }
        IpReachabilityMonitor ipReachabilityMonitor = this.mIpReachabilityMonitor;
        if (ipReachabilityMonitor != null) {
            ipReachabilityMonitor.updateLinkProperties(linkProperties);
        }
        int compareProvisioning = compareProvisioning(this.mLinkProperties, linkProperties);
        this.mLinkProperties = new LinkProperties(linkProperties);
        if (compareProvisioning == CMD_START) {
            this.mProvisioningTimeoutAlarm.cancel();
        }
        return compareProvisioning;
    }

    private LinkProperties assembleLinkProperties() {
        InitialConfiguration initialConfiguration;
        LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(this.mInterfaceName);
        LinkProperties linkProperties2 = this.mLinkObserver.getLinkProperties();
        linkProperties.setLinkAddresses(linkProperties2.getLinkAddresses());
        for (RouteInfo routeInfo : linkProperties2.getRoutes()) {
            linkProperties.addRoute(routeInfo);
        }
        addAllReachableDnsServers(linkProperties, linkProperties2.getDnsServers());
        DhcpResults dhcpResults = this.mDhcpResults;
        if (dhcpResults != null) {
            for (RouteInfo routeInfo2 : dhcpResults.toStaticIpConfiguration().getRoutes(this.mInterfaceName)) {
                linkProperties.addRoute(routeInfo2);
            }
            addAllReachableDnsServers(linkProperties, this.mDhcpResults.dnsServers);
            linkProperties.setDomains(this.mDhcpResults.domains);
            int i = this.mDhcpResults.mtu;
            if (i != 0) {
                linkProperties.setMtu(i);
            }
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
            linkProperties.setTcpBufferSizes(this.mTcpBufferSizes);
        }
        ProxyInfo proxyInfo = this.mHttpProxy;
        if (proxyInfo != null) {
            linkProperties.setHttpProxy(proxyInfo);
        }
        ProvisioningConfiguration provisioningConfiguration = this.mConfiguration;
        if (!(provisioningConfiguration == null || (initialConfiguration = provisioningConfiguration.mInitialConfig) == null)) {
            if (initialConfiguration.isProvisionedBy(linkProperties.getLinkAddresses(), null)) {
                for (IpPrefix ipPrefix : initialConfiguration.directlyConnectedRoutes) {
                    linkProperties.addRoute(new RouteInfo(ipPrefix, null, this.mInterfaceName, CMD_TERMINATE_AFTER_STOP));
                }
            }
            addAllReachableDnsServers(linkProperties, initialConfiguration.dnsServers);
        }
        return linkProperties;
    }

    private static void addAllReachableDnsServers(LinkProperties linkProperties, Iterable<InetAddress> iterable) {
        for (InetAddress inetAddress : iterable) {
            if (!inetAddress.isAnyLocalAddress() && linkProperties.isReachable(inetAddress)) {
                linkProperties.addDnsServer(inetAddress);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean handleLinkPropertiesUpdate(boolean z) {
        LinkProperties assembleLinkProperties = assembleLinkProperties();
        if (Objects.equals(assembleLinkProperties, this.mLinkProperties)) {
            return true;
        }
        int linkProperties = setLinkProperties(assembleLinkProperties);
        maybeSaveNetworkToIpMemoryStore();
        if (z) {
            dispatchCallback(linkProperties, assembleLinkProperties);
        }
        if (linkProperties != CMD_STOP) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleIPv4Success(DhcpResults dhcpResults) {
        this.mDhcpResults = new DhcpResults(dhcpResults);
        LinkProperties assembleLinkProperties = assembleLinkProperties();
        int linkProperties = setLinkProperties(assembleLinkProperties);
        this.mCallback.onNewDhcpResults(dhcpResults);
        maybeSaveNetworkToIpMemoryStore();
        dispatchCallback(linkProperties, assembleLinkProperties);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleIPv4Failure() {
        this.mInterfaceCtrl.clearIPv4Address();
        this.mDhcpResults = null;
        this.mCallback.onNewDhcpResults(null);
        handleProvisioningFailure();
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void handleProvisioningFailure() {
        LinkProperties assembleLinkProperties = assembleLinkProperties();
        int linkProperties = setLinkProperties(assembleLinkProperties);
        if (linkProperties == CMD_TERMINATE_AFTER_STOP) {
            linkProperties = CMD_STOP;
        }
        dispatchCallback(linkProperties, assembleLinkProperties);
        if (linkProperties == CMD_STOP) {
            transitionTo(this.mStoppingState);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void doImmediateProvisioningFailure(int i) {
        Object[] objArr = new Object[CMD_TERMINATE_AFTER_STOP];
        objArr[0] = Integer.valueOf(i);
        logError("onProvisioningFailure(): %s", objArr);
        recordMetric(i);
        this.mCallback.onProvisioningFailure(new LinkProperties(this.mLinkProperties));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean startIPv4() {
        StaticIpConfiguration staticIpConfiguration = this.mConfiguration.mStaticIpConfig;
        if (staticIpConfiguration == null) {
            this.mDhcpClient = DhcpClient.makeDhcpClient(this.mContext, this, this.mInterfaceParams);
            this.mDhcpClient.registerForPreDhcpNotification();
            this.mDhcpClient.sendMessage(DhcpClient.CMD_START_DHCP);
            return true;
        } else if (!this.mInterfaceCtrl.setIPv4Address(staticIpConfiguration.getIpAddress())) {
            return false;
        } else {
            handleIPv4Success(new DhcpResults(this.mConfiguration.mStaticIpConfig));
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean startIPv6() {
        if (!this.mInterfaceCtrl.setIPv6PrivacyExtensions(true) || !this.mInterfaceCtrl.setIPv6AddrGenModeIfSupported(this.mConfiguration.mIPv6AddrGenMode) || !this.mInterfaceCtrl.enableIPv6()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean applyInitialConfig(InitialConfiguration initialConfiguration) {
        for (LinkAddress linkAddress : findAll(initialConfiguration.ipAddresses, $$Lambda$keHyZdcKxxp4KcpstEfOXet8.INSTANCE)) {
            if (!this.mInterfaceCtrl.addAddress(linkAddress)) {
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean startIpReachabilityMonitor() {
        try {
            setNeighborParameters(this.mNetd, this.mInterfaceName, EVENT_PRE_DHCP_ACTION_COMPLETE, 750);
        } catch (Exception e) {
            this.mLog.mo564e("Failed to adjust neighbor parameters", e);
        }
        try {
            this.mIpReachabilityMonitor = new IpReachabilityMonitor(this.mContext, this.mInterfaceParams, getHandler(), this.mLog, new IpReachabilityMonitor.Callback() {
                /* class android.net.p000ip.IpClient.C00162 */

                @Override // android.net.p000ip.IpReachabilityMonitor.Callback
                public void notifyLost(InetAddress inetAddress, String str) {
                    IpClient.this.mCallback.onReachabilityLost(str);
                }
            }, this.mConfiguration.mUsingMultinetworkPolicyTracker);
        } catch (IllegalArgumentException e2) {
            Object[] objArr = new Object[CMD_TERMINATE_AFTER_STOP];
            objArr[0] = e2;
            logError("IpReachabilityMonitor failure: %s", objArr);
            this.mIpReachabilityMonitor = null;
        }
        return this.mIpReachabilityMonitor != null;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void stopAllIP() {
        this.mInterfaceCtrl.disableIPv6();
        this.mInterfaceCtrl.clearAllAddresses();
    }

    /* renamed from: android.net.ip.IpClient$StoppedState */
    class StoppedState extends State {
        StoppedState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            IpClient.this.stopAllIP();
            IpClient.this.resetLinkProperties();
            if (IpClient.this.mStartTimeMillis > 0) {
                IpClient ipClient = IpClient.this;
                ipClient.mCallback.onLinkPropertiesChange(new LinkProperties(ipClient.mLinkProperties));
                IpClient.this.recordMetric(IpClient.CMD_START);
                IpClient.this.mStartTimeMillis = 0;
            }
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == IpClient.CMD_TERMINATE_AFTER_STOP) {
                IpClient.this.stopStateMachineUpdaters();
                IpClient.this.quit();
            } else if (i != IpClient.CMD_STOP) {
                if (i == IpClient.CMD_START) {
                    IpClient.this.mConfiguration = (ProvisioningConfiguration) message.obj;
                    IpClient ipClient = IpClient.this;
                    ipClient.transitionTo(ipClient.mStartedState);
                } else if (i == IpClient.CMD_UPDATE_L2KEY_GROUPHINT) {
                    Pair pair = (Pair) message.obj;
                    IpClient.this.mL2Key = (String) pair.first;
                    IpClient.this.mGroupHint = (String) pair.second;
                } else if (i != IpClient.CMD_RELEASE) {
                    if (i != 1005) {
                        switch (i) {
                            case IpClient.EVENT_NETLINK_LINKPROPERTIES_CHANGED /*{ENCODED_INT: 6}*/:
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case IpClient.CMD_UPDATE_TCP_BUFFER_SIZES /*{ENCODED_INT: 7}*/:
                                IpClient.this.mTcpBufferSizes = (String) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case IpClient.CMD_UPDATE_HTTP_PROXY /*{ENCODED_INT: 8}*/:
                                IpClient.this.mHttpProxy = (ProxyInfo) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case IpClient.CMD_SET_MULTICAST_FILTER /*{ENCODED_INT: 9}*/:
                                IpClient.this.mMulticastFiltering = ((Boolean) message.obj).booleanValue();
                                break;
                            default:
                                return false;
                        }
                    } else {
                        IpClient.this.logError("Unexpected CMD_ON_QUIT (already stopped).", new Object[0]);
                    }
                }
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    /* renamed from: android.net.ip.IpClient$StoppingState */
    class StoppingState extends State {
        StoppingState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            if (IpClient.this.mDhcpClient == null) {
                IpClient ipClient = IpClient.this;
                ipClient.deferMessage(ipClient.obtainMessage(IpClient.CMD_JUMP_STOPPING_TO_STOPPED));
            }
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != IpClient.CMD_STOP) {
                if (i != IpClient.CMD_RELEASE) {
                    if (i == IpClient.CMD_JUMP_STOPPING_TO_STOPPED) {
                        IpClient ipClient = IpClient.this;
                        ipClient.transitionTo(ipClient.mStoppedState);
                    } else if (i == 1005) {
                        IpClient.this.mDhcpClient = null;
                        IpClient ipClient2 = IpClient.this;
                        ipClient2.transitionTo(ipClient2.mStoppedState);
                    } else if (i != 1007) {
                        IpClient.this.deferMessage(message);
                    } else {
                        IpClient.this.mInterfaceCtrl.clearIPv4Address();
                    }
                } else if (IpClient.this.mDhcpClient != null) {
                    IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_RELEASE);
                }
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    /* renamed from: android.net.ip.IpClient$StartedState */
    class StartedState extends State {
        StartedState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            IpClient.this.mStartTimeMillis = SystemClock.elapsedRealtime();
            if (IpClient.this.mConfiguration.mProvisioningTimeoutMs > 0) {
                IpClient.this.mProvisioningTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mProvisioningTimeoutMs));
            }
            if (readyToProceed()) {
                IpClient ipClient = IpClient.this;
                ipClient.deferMessage(ipClient.obtainMessage(IpClient.CMD_JUMP_STARTED_TO_RUNNING));
                return;
            }
            IpClient.this.stopAllIP();
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            IpClient.this.mProvisioningTimeoutAlarm.cancel();
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == IpClient.CMD_STOP) {
                IpClient ipClient = IpClient.this;
                ipClient.transitionTo(ipClient.mStoppingState);
            } else if (i == IpClient.EVENT_NETLINK_LINKPROPERTIES_CHANGED) {
                IpClient.this.handleLinkPropertiesUpdate(false);
                if (readyToProceed()) {
                    IpClient ipClient2 = IpClient.this;
                    ipClient2.transitionTo(ipClient2.mRunningState);
                }
            } else if (i == IpClient.EVENT_PROVISIONING_TIMEOUT) {
                IpClient.this.handleProvisioningFailure();
            } else if (i == IpClient.CMD_UPDATE_L2KEY_GROUPHINT) {
                Pair pair = (Pair) message.obj;
                IpClient.this.mL2Key = (String) pair.first;
                IpClient.this.mGroupHint = (String) pair.second;
            } else if (i != IpClient.CMD_JUMP_STARTED_TO_RUNNING) {
                IpClient.this.deferMessage(message);
            } else {
                IpClient ipClient3 = IpClient.this;
                ipClient3.transitionTo(ipClient3.mRunningState);
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }

        private boolean readyToProceed() {
            return !IpClient.this.mLinkProperties.hasIpv4Address() && !IpClient.this.mLinkProperties.hasGlobalIpv6Address();
        }
    }

    /* renamed from: android.net.ip.IpClient$RunningState */
    class RunningState extends State {
        private boolean mDhcpActionInFlight;
        private ConnectivityPacketTracker mPacketTracker;

        RunningState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            ApfFilter.ApfConfiguration apfConfiguration = new ApfFilter.ApfConfiguration();
            apfConfiguration.apfCapabilities = IpClient.this.mConfiguration.mApfCapabilities;
            apfConfiguration.multicastFilter = IpClient.this.mMulticastFiltering;
            apfConfiguration.ieee802_3Filter = ApfCapabilities.getApfDrop8023Frames();
            apfConfiguration.ethTypeBlackList = ApfCapabilities.getApfEtherTypeBlackList();
            IpClient ipClient = IpClient.this;
            ipClient.mApfFilter = ApfFilter.maybeCreate(ipClient.mContext, apfConfiguration, IpClient.this.mInterfaceParams, IpClient.this.mCallback);
            if (IpClient.this.mApfFilter == null) {
                IpClient ipClient2 = IpClient.this;
                ipClient2.mCallback.setFallbackMulticastFilter(ipClient2.mMulticastFiltering);
            }
            this.mPacketTracker = createPacketTracker();
            ConnectivityPacketTracker connectivityPacketTracker = this.mPacketTracker;
            if (connectivityPacketTracker != null) {
                connectivityPacketTracker.start(IpClient.this.mConfiguration.mDisplayName);
            }
            if (IpClient.this.mConfiguration.mEnableIPv6 && !IpClient.this.startIPv6()) {
                IpClient.this.doImmediateProvisioningFailure(IpClient.EVENT_PRE_DHCP_ACTION_COMPLETE);
                enqueueJumpToStoppingState();
            } else if (!IpClient.this.mConfiguration.mEnableIPv4 || IpClient.this.startIPv4()) {
                InitialConfiguration initialConfiguration = IpClient.this.mConfiguration.mInitialConfig;
                if (initialConfiguration != null && !IpClient.this.applyInitialConfig(initialConfiguration)) {
                    IpClient.this.doImmediateProvisioningFailure(IpClient.CMD_UPDATE_TCP_BUFFER_SIZES);
                    enqueueJumpToStoppingState();
                } else if (IpClient.this.mConfiguration.mUsingIpReachabilityMonitor && !IpClient.this.startIpReachabilityMonitor()) {
                    IpClient.this.doImmediateProvisioningFailure(IpClient.EVENT_NETLINK_LINKPROPERTIES_CHANGED);
                    enqueueJumpToStoppingState();
                }
            } else {
                IpClient.this.doImmediateProvisioningFailure(IpClient.CMD_CONFIRM);
                enqueueJumpToStoppingState();
            }
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            stopDhcpAction();
            if (IpClient.this.mIpReachabilityMonitor != null) {
                IpClient.this.mIpReachabilityMonitor.stop();
                IpClient.this.mIpReachabilityMonitor = null;
            }
            if (IpClient.this.mDhcpClient != null) {
                IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_STOP_DHCP);
                IpClient.this.mDhcpClient.doQuit();
            }
            ConnectivityPacketTracker connectivityPacketTracker = this.mPacketTracker;
            if (connectivityPacketTracker != null) {
                connectivityPacketTracker.stop();
                this.mPacketTracker = null;
            }
            if (IpClient.this.mApfFilter != null) {
                IpClient.this.mApfFilter.shutdown();
                IpClient.this.mApfFilter = null;
            }
            IpClient.this.resetLinkProperties();
        }

        private void enqueueJumpToStoppingState() {
            IpClient ipClient = IpClient.this;
            ipClient.deferMessage(ipClient.obtainMessage(IpClient.CMD_JUMP_RUNNING_TO_STOPPING));
        }

        private ConnectivityPacketTracker createPacketTracker() {
            try {
                return new ConnectivityPacketTracker(IpClient.this.getHandler(), IpClient.this.mInterfaceParams, IpClient.this.mConnectivityPacketLog);
            } catch (IllegalArgumentException unused) {
                return null;
            }
        }

        private void ensureDhcpAction() {
            if (!this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPreDhcpAction();
                this.mDhcpActionInFlight = true;
                IpClient.this.mDhcpActionTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mRequestedPreDhcpActionMs));
            }
        }

        private void stopDhcpAction() {
            IpClient.this.mDhcpActionTimeoutAlarm.cancel();
            if (this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPostDhcpAction();
                this.mDhcpActionInFlight = false;
            }
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != IpClient.CMD_RELEASE) {
                if (i != IpClient.CMD_JUMP_RUNNING_TO_STOPPING) {
                    if (i == 1007) {
                        IpClient.this.mInterfaceCtrl.clearIPv4Address();
                    } else if (i != 1008) {
                        switch (i) {
                            case IpClient.CMD_STOP /*{ENCODED_INT: 2}*/:
                                break;
                            case IpClient.CMD_START /*{ENCODED_INT: 3}*/:
                                IpClient.this.logError("ALERT: START received in StartedState. Please fix caller.", new Object[0]);
                                break;
                            case IpClient.CMD_CONFIRM /*{ENCODED_INT: 4}*/:
                                if (IpClient.this.mIpReachabilityMonitor != null) {
                                    IpClient.this.mIpReachabilityMonitor.probeAll();
                                    break;
                                }
                                break;
                            case IpClient.EVENT_PRE_DHCP_ACTION_COMPLETE /*{ENCODED_INT: 5}*/:
                                if (IpClient.this.mDhcpClient != null) {
                                    IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE);
                                    break;
                                }
                                break;
                            case IpClient.EVENT_NETLINK_LINKPROPERTIES_CHANGED /*{ENCODED_INT: 6}*/:
                                if (!IpClient.this.handleLinkPropertiesUpdate(true)) {
                                    IpClient ipClient = IpClient.this;
                                    ipClient.transitionTo(ipClient.mStoppingState);
                                    break;
                                }
                                break;
                            case IpClient.CMD_UPDATE_TCP_BUFFER_SIZES /*{ENCODED_INT: 7}*/:
                                IpClient.this.mTcpBufferSizes = (String) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(true);
                                break;
                            case IpClient.CMD_UPDATE_HTTP_PROXY /*{ENCODED_INT: 8}*/:
                                IpClient.this.mHttpProxy = (ProxyInfo) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(true);
                                break;
                            case IpClient.CMD_SET_MULTICAST_FILTER /*{ENCODED_INT: 9}*/:
                                IpClient.this.mMulticastFiltering = ((Boolean) message.obj).booleanValue();
                                if (IpClient.this.mApfFilter == null) {
                                    IpClient ipClient2 = IpClient.this;
                                    ipClient2.mCallback.setFallbackMulticastFilter(ipClient2.mMulticastFiltering);
                                    break;
                                } else {
                                    IpClient.this.mApfFilter.setMulticastFilter(IpClient.this.mMulticastFiltering);
                                    break;
                                }
                            default:
                                switch (i) {
                                    case IpClient.EVENT_DHCPACTION_TIMEOUT /*{ENCODED_INT: 11}*/:
                                        stopDhcpAction();
                                        break;
                                    case IpClient.EVENT_READ_PACKET_FILTER_COMPLETE /*{ENCODED_INT: 12}*/:
                                        if (IpClient.this.mApfFilter != null) {
                                            IpClient.this.mApfFilter.setDataSnapshot((byte[]) message.obj);
                                        }
                                        IpClient.this.mApfDataSnapshotComplete.open();
                                        break;
                                    case IpClient.CMD_ADD_KEEPALIVE_PACKET_FILTER_TO_APF /*{ENCODED_INT: 13}*/:
                                        int i2 = message.arg1;
                                        if (IpClient.this.mApfFilter != null) {
                                            Object obj = message.obj;
                                            if (!(obj instanceof NattKeepalivePacketDataParcelable)) {
                                                if (obj instanceof TcpKeepalivePacketDataParcelable) {
                                                    IpClient.this.mApfFilter.addTcpKeepalivePacketFilter(i2, (TcpKeepalivePacketDataParcelable) message.obj);
                                                    break;
                                                }
                                            } else {
                                                IpClient.this.mApfFilter.addNattKeepalivePacketFilter(i2, (NattKeepalivePacketDataParcelable) message.obj);
                                                break;
                                            }
                                        }
                                        break;
                                    case IpClient.CMD_REMOVE_KEEPALIVE_PACKET_FILTER_FROM_APF /*{ENCODED_INT: 14}*/:
                                        int i3 = message.arg1;
                                        if (IpClient.this.mApfFilter != null) {
                                            IpClient.this.mApfFilter.removeKeepalivePacketFilter(i3);
                                            break;
                                        }
                                        break;
                                    default:
                                        switch (i) {
                                            case DhcpClient.CMD_PRE_DHCP_ACTION:
                                                if (IpClient.this.mConfiguration.mRequestedPreDhcpActionMs <= 0) {
                                                    IpClient.this.sendMessage(IpClient.EVENT_PRE_DHCP_ACTION_COMPLETE);
                                                    break;
                                                } else {
                                                    ensureDhcpAction();
                                                    break;
                                                }
                                            case DhcpClient.CMD_POST_DHCP_ACTION:
                                                stopDhcpAction();
                                                int i4 = message.arg1;
                                                if (i4 != IpClient.CMD_TERMINATE_AFTER_STOP) {
                                                    if (i4 == IpClient.CMD_STOP) {
                                                        IpClient.this.handleIPv4Failure();
                                                        break;
                                                    } else {
                                                        IpClient ipClient3 = IpClient.this;
                                                        Object[] objArr = new Object[IpClient.CMD_TERMINATE_AFTER_STOP];
                                                        objArr[0] = Integer.valueOf(i4);
                                                        ipClient3.logError("Unknown CMD_POST_DHCP_ACTION status: %s", objArr);
                                                        break;
                                                    }
                                                } else {
                                                    IpClient.this.handleIPv4Success((DhcpResults) message.obj);
                                                    break;
                                                }
                                            case DhcpClient.CMD_ON_QUIT:
                                                IpClient.this.logError("Unexpected CMD_ON_QUIT.", new Object[0]);
                                                IpClient.this.mDhcpClient = null;
                                                break;
                                            default:
                                                return false;
                                        }
                                }
                        }
                    } else {
                        if (IpClient.this.mInterfaceCtrl.setIPv4Address((LinkAddress) message.obj)) {
                            IpClient.this.mDhcpClient.sendMessage(DhcpClient.EVENT_LINKADDRESS_CONFIGURED);
                        } else {
                            IpClient.this.logError("Failed to set IPv4 address.", new Object[0]);
                            IpClient ipClient4 = IpClient.this;
                            ipClient4.dispatchCallback(IpClient.CMD_STOP, new LinkProperties(ipClient4.mLinkProperties));
                            IpClient ipClient5 = IpClient.this;
                            ipClient5.transitionTo(ipClient5.mStoppingState);
                        }
                    }
                }
                IpClient ipClient6 = IpClient.this;
                ipClient6.transitionTo(ipClient6.mStoppingState);
            } else if (IpClient.this.mDhcpClient != null) {
                IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_RELEASE);
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* renamed from: android.net.ip.IpClient$MessageHandlingLogger */
    public static class MessageHandlingLogger {
        public String processedInState;
        public String receivedInState;

        private MessageHandlingLogger() {
        }

        public void reset() {
            this.processedInState = null;
            this.receivedInState = null;
        }

        public void handled(State state, IState iState) {
            this.processedInState = state.getClass().getSimpleName();
            this.receivedInState = iState.getName();
        }

        public String toString() {
            Object[] objArr = new Object[IpClient.CMD_STOP];
            objArr[0] = this.receivedInState;
            objArr[IpClient.CMD_TERMINATE_AFTER_STOP] = this.processedInState;
            return String.format("rcvd_in=%s, proc_in=%s", objArr);
        }
    }

    private static void setNeighborParameters(INetd iNetd, String str, int i, int i2) throws RemoteException, IllegalArgumentException {
        Preconditions.checkNotNull(iNetd);
        Preconditions.checkArgument(TextUtils.isEmpty(str) ^ CMD_TERMINATE_AFTER_STOP);
        Preconditions.checkArgument(i > 0 ? CMD_TERMINATE_AFTER_STOP : false);
        Preconditions.checkArgument(i2 > 0 ? CMD_TERMINATE_AFTER_STOP : false);
        Integer[] numArr = new Integer[CMD_STOP];
        numArr[0] = Integer.valueOf((int) CMD_CONFIRM);
        numArr[CMD_TERMINATE_AFTER_STOP] = Integer.valueOf((int) EVENT_NETLINK_LINKPROPERTIES_CHANGED);
        int length = numArr.length;
        for (int i3 = 0; i3 < length; i3 += CMD_TERMINATE_AFTER_STOP) {
            int intValue = numArr[i3].intValue();
            iNetd.setProcSysNet(intValue, CMD_STOP, str, "retrans_time_ms", Integer.toString(i2));
            iNetd.setProcSysNet(intValue, CMD_STOP, str, "ucast_solicit", Integer.toString(i));
        }
    }

    static <T> List<T> findAll(Collection<T> collection, Predicate<T> predicate) {
        return (List) collection.stream().filter(predicate).collect(Collectors.toList());
    }
}
