package com.android.server.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DnsResolver;
import android.net.INetworkMonitorCallbacks;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.captiveportal.CaptivePortalProbeResult;
import android.net.captiveportal.CaptivePortalProbeSpec;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.NetworkEvent;
import android.net.metrics.ValidationProbeEvent;
import android.net.networkstack.util.RingBufferIndices;
import android.net.networkstack.util.State;
import android.net.networkstack.util.StateMachine;
import android.net.shared.NetworkMonitorUtils;
import android.net.shared.PrivateDnsConfig;
import android.net.util.NetworkStackUtils;
import android.net.util.SharedLog;
import android.net.util.Stopwatch;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CellIdentity;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.networkstack.R$array;
import com.android.networkstack.R$integer;
import com.android.networkstack.R$string;
import com.android.networkstack.metrics.DataStallDetectionStats;
import com.android.networkstack.metrics.DataStallStatsUtils;
import com.android.networkstack.util.DnsUtils;
import com.android.server.connectivity.NetworkMonitor;
import com.samsung.android.feature.SemCscFeature;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class NetworkMonitor extends StateMachine {
    static final String CONFIG_CAPTIVE_PORTAL_DNS_PROBE_TIMEOUT = "captive_portal_dns_probe_timeout";
    private static final String[] SECONDARY_HTTP_URLS_CHINA = {"http://www.qq.com", "http://www.baidu.com", "http://m.taobao.com", "http://m.hao123.com"};
    private static final String TAG = "NetworkMonitor";
    private static final boolean VDBG_STALL = Log.isLoggable(TAG, 3);
    private static final int[][] mPrivateIpRange = {new int[]{167772160, -16777216}, new int[]{-1408237568, -1048576}, new int[]{-1062731776, -65536}};
    private boolean mAcceptPartialConnectivity;
    private final INetworkMonitorCallbacks mCallback;
    private final int mCallbackVersion;
    private final CaptivePortalProbeSpec[] mCaptivePortalFallbackSpecs;
    private final URL[] mCaptivePortalFallbackUrls;
    private final URL mCaptivePortalHttpUrl;
    private URL mCaptivePortalHttpsUrl;
    private final State mCaptivePortalState;
    private final String mCaptivePortalUserAgent;
    private boolean mCheckForDnsPrivateIpResponse;
    private final Network mCleartextDnsNetwork;
    private final ConnectivityManager mCm;
    private boolean mCollectDataStallMetrics;
    private final int mConsecutiveDnsTimeoutThreshold;
    private Context mContext;
    private String mCountryIso;
    private final int mDataStallEvaluationType;
    private final int mDataStallMinEvaluateTime;
    private final int mDataStallValidDnsTimeThreshold;
    private final State mDefaultState;
    private final Dependencies mDependencies;
    private final DataStallStatsUtils mDetectionStatsUtils;
    private String mDeviceCountryIso;
    private final DnsStallDetector mDnsStallDetector;
    private boolean mDontDisplaySigninNotification;
    private int mEvaluateAttempts;
    private final State mEvaluatingPrivateDnsState;
    private final State mEvaluatingState;
    private final EvaluationState mEvaluationState;
    private final Stopwatch mEvaluationTimer;
    private boolean mIgnorePrivateIpResponse;
    private boolean mInitialPrivateIpCheckDone;
    protected boolean mIsCaptivePortalCheckEnabled;
    private boolean mIsWifiOnly;
    private CaptivePortalProbeResult mLastPortalProbeResult;
    private long mLastProbeTime;
    private CustomIntentReceiver mLaunchCaptivePortalAppBroadcastReceiver;
    private LinkProperties mLinkProperties;
    private final State mMaybeNotifyState;
    private final IpConnectivityLog mMetricsLog;
    private final Network mNetwork;
    private NetworkCapabilities mNetworkCapabilities;
    private int mNextFallbackUrlIndex;
    private String mPrivateDnsProviderHostname;
    private volatile int mProbeToken;
    private final State mProbingState;
    private final Random mRandom;
    private int mReevaluateDelayMs;
    private int mReevaluateToken;
    private boolean mRunFullParallelCheck;
    private String mScanResultsCountryIso;
    private String mTelephonyCountryIso;
    private final TelephonyManager mTelephonyManager;
    private int mUidResponsibleForReeval;
    private boolean mUseHttps;
    private boolean mUserDoesNotWant;
    private final State mValidatedState;
    private final SharedLog mValidationLogs;
    private int mValidations;
    private final State mWaitingForNextProbeState;
    private final WifiManager mWifiManager;

    static /* synthetic */ int access$3408(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mValidations;
        networkMonitor.mValidations = i + 1;
        return i;
    }

    static /* synthetic */ int access$4804(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mReevaluateToken + 1;
        networkMonitor.mReevaluateToken = i;
        return i;
    }

    static /* synthetic */ int access$4928(NetworkMonitor networkMonitor, int i) {
        int i2 = networkMonitor.mReevaluateDelayMs * i;
        networkMonitor.mReevaluateDelayMs = i2;
        return i2;
    }

    static /* synthetic */ int access$5008(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mEvaluateAttempts;
        networkMonitor.mEvaluateAttempts = i + 1;
        return i;
    }

    static /* synthetic */ int access$6304(NetworkMonitor networkMonitor) {
        int i = networkMonitor.mProbeToken + 1;
        networkMonitor.mProbeToken = i;
        return i;
    }

    /* access modifiers changed from: package-private */
    public enum EvaluationResult {
        VALIDATED(true),
        CAPTIVE_PORTAL(false);
        
        final boolean mIsValidated;

        private EvaluationResult(boolean z) {
            this.mIsValidated = z;
        }
    }

    /* access modifiers changed from: package-private */
    public enum ValidationStage {
        FIRST_VALIDATION(true),
        REVALIDATION(false);
        
        final boolean mIsFirstValidation;

        private ValidationStage(boolean z) {
            this.mIsFirstValidation = z;
        }
    }

    private int getCallbackVersion(INetworkMonitorCallbacks iNetworkMonitorCallbacks) {
        int i;
        try {
            i = iNetworkMonitorCallbacks.getInterfaceVersion();
        } catch (RemoteException unused) {
            i = 0;
        }
        if (i == 10000) {
            return 0;
        }
        return i;
    }

    public NetworkMonitor(Context context, INetworkMonitorCallbacks iNetworkMonitorCallbacks, Network network, SharedLog sharedLog) {
        this(context, iNetworkMonitorCallbacks, network, new IpConnectivityLog(), sharedLog, Dependencies.DEFAULT, new DataStallStatsUtils());
    }

    protected NetworkMonitor(Context context, INetworkMonitorCallbacks iNetworkMonitorCallbacks, Network network, IpConnectivityLog ipConnectivityLog, SharedLog sharedLog, Dependencies dependencies, DataStallStatsUtils dataStallStatsUtils) {
        super(TAG + "/" + network.toString());
        boolean z = false;
        this.mReevaluateToken = 0;
        this.mUidResponsibleForReeval = -1;
        this.mPrivateDnsProviderHostname = "";
        this.mValidations = 0;
        this.mUserDoesNotWant = false;
        this.mDontDisplaySigninNotification = false;
        this.mDefaultState = new DefaultState();
        this.mValidatedState = new ValidatedState();
        this.mMaybeNotifyState = new MaybeNotifyState();
        this.mEvaluatingState = new EvaluatingState();
        this.mCaptivePortalState = new CaptivePortalState();
        this.mEvaluatingPrivateDnsState = new EvaluatingPrivateDnsState();
        this.mProbingState = new ProbingState();
        this.mWaitingForNextProbeState = new WaitingForNextProbeState();
        this.mLaunchCaptivePortalAppBroadcastReceiver = null;
        this.mEvaluationTimer = new Stopwatch();
        this.mLastPortalProbeResult = CaptivePortalProbeResult.FAILED;
        this.mNextFallbackUrlIndex = 0;
        this.mReevaluateDelayMs = 1000;
        this.mEvaluateAttempts = 0;
        this.mProbeToken = 0;
        this.mAcceptPartialConnectivity = false;
        this.mEvaluationState = new EvaluationState();
        this.mCheckForDnsPrivateIpResponse = false;
        this.mInitialPrivateIpCheckDone = false;
        this.mIgnorePrivateIpResponse = false;
        this.mRunFullParallelCheck = true;
        this.mIsWifiOnly = false;
        this.mCountryIso = null;
        this.mTelephonyCountryIso = null;
        this.mDeviceCountryIso = null;
        this.mScanResultsCountryIso = null;
        setDbg(false);
        this.mContext = context;
        this.mMetricsLog = ipConnectivityLog;
        this.mValidationLogs = sharedLog;
        this.mCallback = iNetworkMonitorCallbacks;
        this.mCallbackVersion = getCallbackVersion(iNetworkMonitorCallbacks);
        this.mDependencies = dependencies;
        this.mDetectionStatsUtils = dataStallStatsUtils;
        this.mNetwork = network;
        this.mCleartextDnsNetwork = dependencies.getPrivateDnsBypassNetwork(network);
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mCm = (ConnectivityManager) context.getSystemService("connectivity");
        addState(this.mDefaultState);
        addState(this.mMaybeNotifyState, this.mDefaultState);
        addState(this.mEvaluatingState, this.mMaybeNotifyState);
        addState(this.mProbingState, this.mEvaluatingState);
        addState(this.mWaitingForNextProbeState, this.mEvaluatingState);
        addState(this.mCaptivePortalState, this.mMaybeNotifyState);
        addState(this.mEvaluatingPrivateDnsState, this.mDefaultState);
        addState(this.mValidatedState, this.mDefaultState);
        setInitialState(this.mDefaultState);
        updateConfigForCtcRoaming();
        this.mIsCaptivePortalCheckEnabled = getIsCaptivePortalCheckEnabled();
        this.mUseHttps = getUseHttpsValidation();
        this.mCaptivePortalUserAgent = getCaptivePortalUserAgent();
        this.mCaptivePortalHttpsUrl = makeURL(getCaptivePortalServerHttpsUrl());
        this.mCaptivePortalHttpUrl = makeURL(getCaptivePortalServerHttpUrl());
        this.mCaptivePortalFallbackUrls = makeCaptivePortalFallbackUrls();
        this.mCaptivePortalFallbackSpecs = makeCaptivePortalFallbackProbeSpecs();
        this.mRandom = dependencies.getRandom();
        this.mConsecutiveDnsTimeoutThreshold = getConsecutiveDnsTimeoutThreshold();
        this.mDnsStallDetector = new DnsStallDetector(this.mConsecutiveDnsTimeoutThreshold);
        this.mDataStallMinEvaluateTime = getDataStallMinEvaluateTime();
        this.mDataStallValidDnsTimeThreshold = getDataStallValidDnsTimeThreshold();
        this.mDataStallEvaluationType = getDataStallEvaluationType();
        this.mLinkProperties = new LinkProperties();
        this.mNetworkCapabilities = new NetworkCapabilities(null);
        this.mIsWifiOnly = ("wifi-only".equalsIgnoreCase(SystemProperties.get("ro.carrier", "Unknown").trim()) || "yes".equalsIgnoreCase(SystemProperties.get("ro.radio.noril", "no").trim())) ? true : z;
    }

    public void setAcceptPartialConnectivity() {
        sendMessage(18);
    }

    public void forceReevaluation(int i) {
        sendMessage(8, i, 0);
    }

    public void notifyDnsResponse(int i) {
        sendMessage(17, i);
    }

    public void notifyPrivateDnsSettingsChanged(PrivateDnsConfig privateDnsConfig) {
        removeMessages(13);
        sendMessage(13, privateDnsConfig);
    }

    public void notifyNetworkConnected(LinkProperties linkProperties, NetworkCapabilities networkCapabilities) {
        sendMessage(1, new Pair(new LinkProperties(linkProperties), new NetworkCapabilities(networkCapabilities)));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateConnectedNetworkAttributes(Message message) {
        Pair pair = (Pair) message.obj;
        this.mLinkProperties = (LinkProperties) pair.first;
        this.mNetworkCapabilities = (NetworkCapabilities) pair.second;
    }

    public void notifyNetworkDisconnected() {
        sendMessage(7);
    }

    public void notifyLinkPropertiesChanged(LinkProperties linkProperties) {
        sendMessage(19, new LinkProperties(linkProperties));
    }

    public void notifyNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities) {
        sendMessage(20, new NetworkCapabilities(networkCapabilities));
    }

    public void launchCaptivePortalApp() {
        sendMessage(11);
    }

    public void notifyCaptivePortalAppFinished(int i) {
        sendMessage(9, i);
    }

    /* access modifiers changed from: protected */
    @Override // android.net.networkstack.util.StateMachine
    public void log(String str) {
        Log.d(TAG + "/" + this.mCleartextDnsNetwork.toString(), str);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void validationLog(int i, Object obj, String str) {
        validationLog(String.format("%s %s %s", ValidationProbeEvent.getProbeName(i), obj, str));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void validationLog(String str) {
        log(str);
        this.mValidationLogs.log(str);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private ValidationStage validationStage() {
        return this.mValidations == 0 ? ValidationStage.FIRST_VALIDATION : ValidationStage.REVALIDATION;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isValidationRequired() {
        return NetworkMonitorUtils.isValidationRequired(this.mNetworkCapabilities);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean isPrivateDnsValidationRequired() {
        return NetworkMonitorUtils.isPrivateDnsValidationRequired(this.mNetworkCapabilities);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void notifyNetworkTested(int i, String str) {
        try {
            this.mCallback.notifyNetworkTested(i, str);
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending network test result", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void showProvisioningNotification(String str) {
        try {
            this.mCallback.showProvisioningNotification(str, this.mContext.getPackageName());
        } catch (RemoteException e) {
            Log.e(TAG, "Error showing provisioning notification", e);
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void hideProvisioningNotification() {
        try {
            this.mCallback.hideProvisioningNotification();
        } catch (RemoteException e) {
            Log.e(TAG, "Error hiding provisioning notification", e);
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                NetworkMonitor.this.updateConnectedNetworkAttributes(message);
                NetworkMonitor.this.logNetworkEvent(1);
                if (NetworkMonitor.this.mNetworkCapabilities.hasTransport(1)) {
                    NetworkMonitor networkMonitor = NetworkMonitor.this;
                    networkMonitor.mCheckForDnsPrivateIpResponse = networkMonitor.getCheckForDnsPrivateIpResponse();
                    if (NetworkMonitor.this.mIsWifiOnly) {
                        NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                        networkMonitor2.mCaptivePortalHttpsUrl = networkMonitor2.makeURL("https://connectivitycheck.gstatic.com/generate_204");
                    }
                    NetworkMonitor.this.updateCountryIsoCode();
                }
                NetworkMonitor networkMonitor3 = NetworkMonitor.this;
                networkMonitor3.transitionTo(networkMonitor3.mEvaluatingState);
                return true;
            } else if (i != 7) {
                if (i != 8) {
                    if (i == 9) {
                        NetworkMonitor networkMonitor4 = NetworkMonitor.this;
                        networkMonitor4.log("CaptivePortal App responded with " + message.arg1);
                        NetworkMonitor.this.mUseHttps = false;
                        int i2 = message.arg1;
                        if (i2 == 0) {
                            NetworkMonitor.this.sendMessage(8, 0, 0);
                        } else if (i2 == 1) {
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor.this.mUserDoesNotWant = true;
                            NetworkMonitor.this.mEvaluationState.reportEvaluationResult(0, null);
                            NetworkMonitor.this.mUidResponsibleForReeval = 0;
                            NetworkMonitor networkMonitor5 = NetworkMonitor.this;
                            networkMonitor5.transitionTo(networkMonitor5.mEvaluatingState);
                        } else if (i2 == 2) {
                            NetworkMonitor.this.mDontDisplaySigninNotification = true;
                            NetworkMonitor networkMonitor6 = NetworkMonitor.this;
                            networkMonitor6.transitionTo(networkMonitor6.mEvaluatingPrivateDnsState);
                        }
                        return true;
                    } else if (i != 12) {
                        if (i != 13) {
                            switch (i) {
                                case 17:
                                    NetworkMonitor.this.mDnsStallDetector.accumulateConsecutiveDnsTimeoutCount(message.arg1);
                                    break;
                                case 18:
                                    NetworkMonitor.this.maybeDisableHttpsProbing(true);
                                    break;
                                case 19:
                                    NetworkMonitor.this.mLinkProperties = (LinkProperties) message.obj;
                                    break;
                                case 20:
                                    NetworkMonitor.this.mNetworkCapabilities = (NetworkCapabilities) message.obj;
                                    break;
                            }
                        } else {
                            PrivateDnsConfig privateDnsConfig = (PrivateDnsConfig) message.obj;
                            if (!NetworkMonitor.this.isPrivateDnsValidationRequired() || privateDnsConfig == null || !privateDnsConfig.inStrictMode()) {
                                NetworkMonitor.this.mPrivateDnsProviderHostname = "";
                            } else {
                                NetworkMonitor.this.mPrivateDnsProviderHostname = privateDnsConfig.hostname;
                                NetworkMonitor.this.sendMessage(15);
                            }
                        }
                        return true;
                    }
                }
                int consecutiveTimeoutCount = NetworkMonitor.this.mDnsStallDetector.getConsecutiveTimeoutCount();
                NetworkMonitor networkMonitor7 = NetworkMonitor.this;
                networkMonitor7.validationLog("Forcing reevaluation for UID " + message.arg1 + ". Dns signal count: " + consecutiveTimeoutCount);
                NetworkMonitor.this.mUidResponsibleForReeval = message.arg1;
                NetworkMonitor networkMonitor8 = NetworkMonitor.this;
                networkMonitor8.transitionTo(networkMonitor8.mEvaluatingState);
                return true;
            } else {
                NetworkMonitor.this.logNetworkEvent(7);
                NetworkMonitor.this.quit();
                return true;
            }
        }
    }

    private class ValidatedState extends State {
        private ValidatedState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.maybeLogEvaluationResult(networkMonitor.networkEventType(networkMonitor.validationStage(), EvaluationResult.VALIDATED));
            NetworkMonitor.this.mEvaluationState.reportEvaluationResult((NetworkMonitor.this.mUseHttps || !NetworkMonitor.this.mAcceptPartialConnectivity) ? 1 : 3, null);
            NetworkMonitor.access$3408(NetworkMonitor.this);
            NetworkMonitor.this.mRunFullParallelCheck = false;
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 1) {
                NetworkMonitor.this.updateConnectedNetworkAttributes(message);
                NetworkMonitor networkMonitor = NetworkMonitor.this;
                networkMonitor.transitionTo(networkMonitor.mValidatedState);
            } else if (i == 15) {
                NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                networkMonitor2.transitionTo(networkMonitor2.mEvaluatingPrivateDnsState);
            } else if (i != 17) {
                return false;
            } else {
                NetworkMonitor.this.mDnsStallDetector.accumulateConsecutiveDnsTimeoutCount(message.arg1);
                if (NetworkMonitor.this.isDataStall()) {
                    NetworkMonitor.this.mCollectDataStallMetrics = true;
                    NetworkMonitor.this.validationLog("Suspecting data stall, reevaluate");
                    NetworkMonitor networkMonitor3 = NetworkMonitor.this;
                    networkMonitor3.transitionTo(networkMonitor3.mEvaluatingState);
                }
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void writeDataStallStats(CaptivePortalProbeResult captivePortalProbeResult) {
        int[] transportTypes;
        for (int i : this.mNetworkCapabilities.getTransportTypes()) {
            DataStallStatsUtils.write(buildDataStallDetectionStats(i), captivePortalProbeResult);
        }
        this.mCollectDataStallMetrics = false;
    }

    /* access modifiers changed from: protected */
    public DataStallDetectionStats buildDataStallDetectionStats(int i) {
        int i2;
        DataStallDetectionStats.Builder builder = new DataStallDetectionStats.Builder();
        if (VDBG_STALL) {
            log("collectDataStallMetrics: type=" + i);
        }
        builder.setEvaluationType(1);
        builder.setNetworkType(i);
        if (i == 0) {
            boolean z = !this.mNetworkCapabilities.hasCapability(18);
            SignalStrength signalStrength = this.mTelephonyManager.getSignalStrength();
            int dataNetworkType = this.mTelephonyManager.getDataNetworkType();
            String networkOperator = this.mTelephonyManager.getNetworkOperator();
            String simOperator = this.mTelephonyManager.getSimOperator();
            if (signalStrength != null) {
                i2 = signalStrength.getLevel();
            } else {
                i2 = 0;
            }
            builder.setCellData(dataNetworkType, z, networkOperator, simOperator, i2);
        } else if (i == 1) {
            builder.setWiFiData(this.mWifiManager.getConnectionInfo());
        }
        addDnsEvents(builder);
        return builder.build();
    }

    /* access modifiers changed from: protected */
    public void addDnsEvents(DataStallDetectionStats.Builder builder) {
        int size = this.mDnsStallDetector.mResultIndices.size();
        int i = 1;
        while (i <= 20 && i <= size) {
            int indexOf = this.mDnsStallDetector.mResultIndices.indexOf(size - i);
            builder.addDnsEvent(this.mDnsStallDetector.mDnsEvents[indexOf].mReturnCode, this.mDnsStallDetector.mDnsEvents[indexOf].mTimeStamp);
            i++;
        }
    }

    private class MaybeNotifyState extends State {
        private MaybeNotifyState() {
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            if (message.what != 11) {
                return false;
            }
            Bundle bundle = new Bundle();
            Network network = new Network(NetworkMonitor.this.mCleartextDnsNetwork);
            bundle.putParcelable("android.net.extra.NETWORK", network);
            CaptivePortalProbeResult captivePortalProbeResult = NetworkMonitor.this.mLastPortalProbeResult;
            bundle.putString("android.net.extra.CAPTIVE_PORTAL_URL", captivePortalProbeResult.detectUrl);
            CaptivePortalProbeSpec captivePortalProbeSpec = captivePortalProbeResult.probeSpec;
            if (captivePortalProbeSpec != null) {
                bundle.putString("android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC", captivePortalProbeSpec.getEncodedSpec());
            }
            bundle.putString("android.net.extra.CAPTIVE_PORTAL_USER_AGENT", NetworkMonitor.this.mCaptivePortalUserAgent);
            NetworkMonitor.this.mCm.startCaptivePortalApp(network, bundle);
            return true;
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver != null) {
                NetworkMonitor.this.mContext.unregisterReceiver(NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver);
                NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver = null;
            }
            NetworkMonitor.this.hideProvisioningNotification();
        }
    }

    private class EvaluatingState extends State {
        private EvaluatingState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            if (!NetworkMonitor.this.mEvaluationTimer.isStarted()) {
                NetworkMonitor.this.mEvaluationTimer.start();
            }
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.sendMessage(6, NetworkMonitor.access$4804(networkMonitor), 0);
            if (NetworkMonitor.this.mUidResponsibleForReeval != -1) {
                TrafficStats.setThreadStatsUid(NetworkMonitor.this.mUidResponsibleForReeval);
                NetworkMonitor.this.mUidResponsibleForReeval = -1;
            }
            NetworkMonitor.this.mReevaluateDelayMs = 1000;
            NetworkMonitor.this.mEvaluateAttempts = 0;
            NetworkMonitor.this.mEvaluationState.clearProbeResults();
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 6) {
                if (message.arg1 == NetworkMonitor.this.mReevaluateToken && !NetworkMonitor.this.mUserDoesNotWant) {
                    if (!NetworkMonitor.this.isValidationRequired()) {
                        if (NetworkMonitor.this.isPrivateDnsValidationRequired()) {
                            NetworkMonitor.this.validationLog("Network would not satisfy default request, resolving private DNS");
                            NetworkMonitor networkMonitor = NetworkMonitor.this;
                            networkMonitor.transitionTo(networkMonitor.mEvaluatingPrivateDnsState);
                        } else {
                            NetworkMonitor.this.validationLog("Network would not satisfy default request, not validating");
                            NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                            networkMonitor2.transitionTo(networkMonitor2.mValidatedState);
                        }
                        return true;
                    }
                    NetworkMonitor.access$5008(NetworkMonitor.this);
                    NetworkMonitor networkMonitor3 = NetworkMonitor.this;
                    networkMonitor3.transitionTo(networkMonitor3.mProbingState);
                }
                return true;
            } else if (i == 8) {
                return NetworkMonitor.this.mEvaluateAttempts < 5;
            } else {
                if (i != 18) {
                    return false;
                }
                NetworkMonitor.this.maybeDisableHttpsProbing(true);
                NetworkMonitor networkMonitor4 = NetworkMonitor.this;
                networkMonitor4.transitionTo(networkMonitor4.mEvaluatingPrivateDnsState);
                return true;
            }
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            TrafficStats.clearThreadStatsUid();
        }
    }

    /* access modifiers changed from: private */
    public class CustomIntentReceiver extends BroadcastReceiver {
        private final String mAction;
        private final int mToken;
        private final int mWhat;

        CustomIntentReceiver(String str, int i, int i2) {
            this.mToken = i;
            this.mWhat = i2;
            this.mAction = str + "_" + NetworkMonitor.this.mCleartextDnsNetwork.getNetworkHandle() + "_" + i;
            NetworkMonitor.this.mContext.registerReceiver(this, new IntentFilter(this.mAction));
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(this.mAction)) {
                NetworkMonitor networkMonitor = NetworkMonitor.this;
                networkMonitor.sendMessage(networkMonitor.obtainMessage(this.mWhat, this.mToken));
            }
        }
    }

    private class CaptivePortalState extends State {
        private CaptivePortalState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.maybeLogEvaluationResult(networkMonitor.networkEventType(networkMonitor.validationStage(), EvaluationResult.CAPTIVE_PORTAL));
            if (!NetworkMonitor.this.mDontDisplaySigninNotification) {
                if (NetworkMonitor.this.mLaunchCaptivePortalAppBroadcastReceiver == null) {
                    NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                    networkMonitor2.mLaunchCaptivePortalAppBroadcastReceiver = new CustomIntentReceiver("android.net.netmon.launchCaptivePortalApp", new Random().nextInt(), 11);
                    NetworkMonitor networkMonitor3 = NetworkMonitor.this;
                    networkMonitor3.showProvisioningNotification(networkMonitor3.mLaunchCaptivePortalAppBroadcastReceiver.mAction);
                }
                NetworkMonitor.this.sendMessageDelayed(12, 0, 600000);
                NetworkMonitor.access$3408(NetworkMonitor.this);
            }
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            NetworkMonitor.this.removeMessages(12);
        }
    }

    private class EvaluatingPrivateDnsState extends State {
        private PrivateDnsConfig mPrivateDnsConfig;
        private int mPrivateDnsReevalDelayMs;

        private EvaluatingPrivateDnsState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            this.mPrivateDnsReevalDelayMs = 1000;
            this.mPrivateDnsConfig = null;
            NetworkMonitor.this.sendMessage(15);
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            if (message.what != 15) {
                return false;
            }
            if (inStrictMode()) {
                if (!isStrictModeHostnameResolved()) {
                    resolveStrictModeHostname();
                    if (isStrictModeHostnameResolved()) {
                        notifyPrivateDnsConfigResolved();
                    } else {
                        handlePrivateDnsEvaluationFailure();
                        return true;
                    }
                }
                if (!sendPrivateDnsProbe()) {
                    handlePrivateDnsEvaluationFailure();
                    return true;
                }
            }
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.transitionTo(networkMonitor.mValidatedState);
            return true;
        }

        private boolean inStrictMode() {
            return !TextUtils.isEmpty(NetworkMonitor.this.mPrivateDnsProviderHostname);
        }

        private boolean isStrictModeHostnameResolved() {
            PrivateDnsConfig privateDnsConfig = this.mPrivateDnsConfig;
            return privateDnsConfig != null && privateDnsConfig.hostname.equals(NetworkMonitor.this.mPrivateDnsProviderHostname) && this.mPrivateDnsConfig.ips.length > 0;
        }

        private void resolveStrictModeHostname() {
            try {
                this.mPrivateDnsConfig = new PrivateDnsConfig(NetworkMonitor.this.mPrivateDnsProviderHostname, DnsUtils.getAllByName(NetworkMonitor.this.mDependencies.getDnsResolver(), NetworkMonitor.this.mCleartextDnsNetwork, NetworkMonitor.this.mPrivateDnsProviderHostname, NetworkMonitor.this.getDnsProbeTimeout()));
                NetworkMonitor networkMonitor = NetworkMonitor.this;
                networkMonitor.validationLog("Strict mode hostname resolved: " + this.mPrivateDnsConfig);
            } catch (UnknownHostException e) {
                this.mPrivateDnsConfig = null;
                NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                networkMonitor2.validationLog("Strict mode hostname resolution failed: " + e.getMessage());
            }
            NetworkMonitor.this.mEvaluationState.noteProbeResult(64, this.mPrivateDnsConfig != null);
        }

        private void notifyPrivateDnsConfigResolved() {
            try {
                NetworkMonitor.this.mCallback.notifyPrivateDnsConfigResolved(this.mPrivateDnsConfig.toParcel());
            } catch (RemoteException e) {
                Log.e(NetworkMonitor.TAG, "Error sending private DNS config resolved notification", e);
            }
        }

        private void handlePrivateDnsEvaluationFailure() {
            NetworkMonitor.this.mEvaluationState.reportEvaluationResult(0, null);
            NetworkMonitor.this.sendMessageDelayed(15, (long) this.mPrivateDnsReevalDelayMs);
            this.mPrivateDnsReevalDelayMs *= 2;
            if (this.mPrivateDnsReevalDelayMs > 600000) {
                this.mPrivateDnsReevalDelayMs = 600000;
            }
        }

        /* JADX DEBUG: Multi-variable search result rejected for r0v4, resolved type: com.android.server.connectivity.NetworkMonitor$EvaluationState */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r6v0, types: [int, boolean] */
        /* JADX WARN: Type inference failed for: r6v12 */
        /* JADX WARN: Type inference failed for: r6v13 */
        private boolean sendPrivateDnsProbe() {
            long j;
            ?? r6;
            UnknownHostException e;
            boolean z;
            String str = UUID.randomUUID().toString().substring(0, 8) + "-dnsotls-ds.metric.gstatic.com";
            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();
            try {
                InetAddress[] allByName = NetworkMonitor.this.mNetwork.getAllByName(str);
                j = stopwatch.stop();
                Object arrays = Arrays.toString(allByName);
                boolean z2 = allByName != null && allByName.length > 0;
                try {
                    NetworkMonitor.this.validationLog(5, str, String.format("%dms: %s", Long.valueOf(j), arrays));
                    r6 = z2;
                } catch (UnknownHostException e2) {
                    e = e2;
                    z = z2;
                }
            } catch (UnknownHostException e3) {
                e = e3;
                z = false;
                long stop = stopwatch.stop();
                NetworkMonitor.this.validationLog(5, str, String.format("%dms - Error: %s", Long.valueOf(stop), e.getMessage()));
                j = stop;
                r6 = z;
                NetworkMonitor.this.logValidationProbe(j, 5, r6);
                NetworkMonitor.this.mEvaluationState.noteProbeResult(64, r6);
                return r6;
            }
            NetworkMonitor.this.logValidationProbe(j, 5, r6);
            NetworkMonitor.this.mEvaluationState.noteProbeResult(64, r6);
            return r6;
        }
    }

    /* access modifiers changed from: private */
    public class ProbingState extends State {
        private Thread mThread;

        private ProbingState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            if (NetworkMonitor.this.mEvaluateAttempts >= 5) {
                TrafficStats.clearThreadStatsUid();
            }
            int access$6304 = NetworkMonitor.access$6304(NetworkMonitor.this);
            if (!NetworkMonitor.this.mNetworkCapabilities.hasTransport(1) || !NetworkMonitor.this.inChinaNetwork()) {
                this.mThread = new Thread(new Runnable(access$6304) {
                    /* class com.android.server.connectivity.RunnableC0025xfc800274 */
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NetworkMonitor.ProbingState.this.lambda$enter$1$NetworkMonitor$ProbingState(this.f$1);
                    }
                });
            } else {
                if (NetworkMonitor.this.getRoamEventfromWCM()) {
                    NetworkMonitor.this.mRunFullParallelCheck = true;
                    Log.d(NetworkMonitor.TAG, "Wi-Fi Roam Event received from WCM. Run full parallel check");
                    NetworkMonitor.this.setRoamEventFromWCM(0);
                }
                this.mThread = new Thread(new Runnable(access$6304) {
                    /* class com.android.server.connectivity.RunnableC0026x813e2562 */
                    private final /* synthetic */ int f$1;

                    {
                        this.f$1 = r2;
                    }

                    public final void run() {
                        NetworkMonitor.ProbingState.this.lambda$enter$0$NetworkMonitor$ProbingState(this.f$1);
                    }
                });
            }
            this.mThread.start();
        }

        public /* synthetic */ void lambda$enter$0$NetworkMonitor$ProbingState(int i) {
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.sendMessage(networkMonitor.obtainMessage(16, i, 0, networkMonitor.isCaptivePortalForChinaWifi()));
        }

        public /* synthetic */ void lambda$enter$1$NetworkMonitor$ProbingState(int i) {
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            networkMonitor.sendMessage(networkMonitor.obtainMessage(16, i, 0, networkMonitor.isCaptivePortal()));
        }

        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            switch (message.what) {
                case 16:
                    if (message.arg1 != NetworkMonitor.this.mProbeToken) {
                        return true;
                    }
                    CaptivePortalProbeResult captivePortalProbeResult = (CaptivePortalProbeResult) message.obj;
                    NetworkMonitor.this.mLastProbeTime = SystemClock.elapsedRealtime();
                    if (NetworkMonitor.this.mCollectDataStallMetrics) {
                        NetworkMonitor.this.writeDataStallStats(captivePortalProbeResult);
                    }
                    if (captivePortalProbeResult.isSuccessful()) {
                        NetworkMonitor networkMonitor = NetworkMonitor.this;
                        networkMonitor.transitionTo(networkMonitor.mEvaluatingPrivateDnsState);
                    } else if (captivePortalProbeResult.isPortal()) {
                        NetworkMonitor.this.mEvaluationState.reportEvaluationResult(0, captivePortalProbeResult.redirectUrl);
                        NetworkMonitor.this.mLastPortalProbeResult = captivePortalProbeResult;
                        NetworkMonitor networkMonitor2 = NetworkMonitor.this;
                        networkMonitor2.transitionTo(networkMonitor2.mCaptivePortalState);
                    } else if (captivePortalProbeResult.isPartialConnectivity()) {
                        NetworkMonitor.this.mEvaluationState.reportEvaluationResult(2, null);
                        NetworkMonitor networkMonitor3 = NetworkMonitor.this;
                        networkMonitor3.maybeDisableHttpsProbing(networkMonitor3.mAcceptPartialConnectivity);
                        if (NetworkMonitor.this.mAcceptPartialConnectivity) {
                            NetworkMonitor networkMonitor4 = NetworkMonitor.this;
                            networkMonitor4.transitionTo(networkMonitor4.mEvaluatingPrivateDnsState);
                        } else {
                            NetworkMonitor networkMonitor5 = NetworkMonitor.this;
                            networkMonitor5.transitionTo(networkMonitor5.mWaitingForNextProbeState);
                        }
                    } else {
                        NetworkMonitor.this.logNetworkEvent(3);
                        NetworkMonitor.this.mEvaluationState.reportEvaluationResult(0, null);
                        NetworkMonitor networkMonitor6 = NetworkMonitor.this;
                        networkMonitor6.transitionTo(networkMonitor6.mWaitingForNextProbeState);
                    }
                    return true;
                case 17:
                case 18:
                    return false;
                default:
                    NetworkMonitor.this.deferMessage(message);
                    return true;
            }
        }

        @Override // android.net.networkstack.util.State
        public void exit() {
            if (this.mThread.isAlive()) {
                this.mThread.interrupt();
            }
            this.mThread = null;
        }
    }

    private class WaitingForNextProbeState extends State {
        @Override // android.net.networkstack.util.State
        public boolean processMessage(Message message) {
            return false;
        }

        private WaitingForNextProbeState() {
        }

        @Override // android.net.networkstack.util.State
        public void enter() {
            scheduleNextProbe();
        }

        private void scheduleNextProbe() {
            NetworkMonitor networkMonitor = NetworkMonitor.this;
            Message obtainMessage = networkMonitor.obtainMessage(6, NetworkMonitor.access$4804(networkMonitor), 0);
            NetworkMonitor networkMonitor2 = NetworkMonitor.this;
            networkMonitor2.sendMessageDelayed(obtainMessage, (long) networkMonitor2.mReevaluateDelayMs);
            NetworkMonitor.access$4928(NetworkMonitor.this, 2);
            if (NetworkMonitor.this.mReevaluateDelayMs > 600000) {
                NetworkMonitor.this.mReevaluateDelayMs = 600000;
            }
        }
    }

    /* access modifiers changed from: private */
    public static class OneAddressPerFamilyNetwork extends Network {
        OneAddressPerFamilyNetwork(Network network) {
            super(network.getPrivateDnsBypassingCopy());
        }

        @Override // android.net.Network
        public InetAddress[] getAllByName(String str) throws UnknownHostException {
            List<InetAddress> asList = Arrays.asList(super.getAllByName(str));
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            linkedHashMap.put(((InetAddress) asList.get(0)).getClass(), (InetAddress) asList.get(0));
            Collections.shuffle(asList);
            for (InetAddress inetAddress : asList) {
                linkedHashMap.put(inetAddress.getClass(), inetAddress);
            }
            return (InetAddress[]) linkedHashMap.values().toArray(new InetAddress[linkedHashMap.size()]);
        }
    }

    private boolean getIsCaptivePortalCheckEnabled() {
        return this.mDependencies.getSetting(this.mContext, "captive_portal_mode", 1) != 0;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean getCheckForDnsPrivateIpResponse() {
        return this.mDependencies.getSetting(this.mContext, "check_private_ip_mode", 0) == 1;
    }

    private boolean getUseHttpsValidation() {
        return this.mDependencies.getDeviceConfigPropertyInt("connectivity", "captive_portal_use_https", 1) == 1;
    }

    private String getCaptivePortalServerHttpsUrl() {
        return getSettingFromResource(this.mContext, R$string.config_captive_portal_https_url, R$string.default_captive_portal_https_url, "captive_portal_https_url");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private int getDnsProbeTimeout() {
        return getIntSetting(this.mContext, R$integer.config_captive_portal_dns_probe_timeout, CONFIG_CAPTIVE_PORTAL_DNS_PROBE_TIMEOUT, R$integer.default_captive_portal_dns_probe_timeout);
    }

    /* access modifiers changed from: package-private */
    public int getIntSetting(Context context, int i, String str, int i2) {
        Resources resources = context.getResources();
        try {
            return resources.getInteger(i);
        } catch (Resources.NotFoundException unused) {
            return this.mDependencies.getDeviceConfigPropertyInt("connectivity", str, resources.getInteger(i2));
        }
    }

    public String getCaptivePortalServerHttpUrl() {
        return getSettingFromResource(this.mContext, R$string.config_captive_portal_http_url, R$string.default_captive_portal_http_url, "captive_portal_http_url");
    }

    private int getConsecutiveDnsTimeoutThreshold() {
        return this.mDependencies.getDeviceConfigPropertyInt("connectivity", "data_stall_consecutive_dns_timeout_threshold", 5);
    }

    private int getDataStallMinEvaluateTime() {
        return this.mDependencies.getDeviceConfigPropertyInt("connectivity", "data_stall_min_evaluate_interval", 60000);
    }

    private int getDataStallValidDnsTimeThreshold() {
        return this.mDependencies.getDeviceConfigPropertyInt("connectivity", "data_stall_valid_dns_time_threshold", 1800000);
    }

    private int getDataStallEvaluationType() {
        return this.mDependencies.getDeviceConfigPropertyInt("connectivity", "data_stall_evaluation_type", 1);
    }

    private URL[] makeCaptivePortalFallbackUrls() {
        URL[] urlArr;
        try {
            String setting = this.mDependencies.getSetting(this.mContext, "captive_portal_fallback_url", (String) null);
            if (!TextUtils.isEmpty(setting)) {
                String deviceConfigProperty = this.mDependencies.getDeviceConfigProperty("connectivity", "captive_portal_other_fallback_urls", "");
                urlArr = (URL[]) convertStrings((setting + "," + deviceConfigProperty).split(","), new Function() {
                    /* class com.android.server.connectivity.$$Lambda$NetworkMonitor$Kh6WmPzS24WnRman3oAjx8S3_Q */

                    @Override // java.util.function.Function
                    public final Object apply(Object obj) {
                        return NetworkMonitor.m6lambda$Kh6WmPzS24WnRman3oAjx8S3_Q(NetworkMonitor.this, (String) obj);
                    }
                }, new URL[0]);
            } else {
                urlArr = new URL[0];
            }
            return (URL[]) getArrayConfig(urlArr, R$array.config_captive_portal_fallback_urls, R$array.default_captive_portal_fallback_urls, new Function() {
                /* class com.android.server.connectivity.$$Lambda$NetworkMonitor$Kh6WmPzS24WnRman3oAjx8S3_Q */

                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return NetworkMonitor.m6lambda$Kh6WmPzS24WnRman3oAjx8S3_Q(NetworkMonitor.this, (String) obj);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing configured fallback URLs", e);
            return new URL[0];
        }
    }

    private CaptivePortalProbeSpec[] makeCaptivePortalFallbackProbeSpecs() {
        try {
            String deviceConfigProperty = this.mDependencies.getDeviceConfigProperty("connectivity", "captive_portal_fallback_probe_specs", null);
            CaptivePortalProbeSpec[] captivePortalProbeSpecArr = new CaptivePortalProbeSpec[0];
            if (!TextUtils.isEmpty(deviceConfigProperty)) {
                captivePortalProbeSpecArr = (CaptivePortalProbeSpec[]) CaptivePortalProbeSpec.parseCaptivePortalProbeSpecs(deviceConfigProperty).toArray(captivePortalProbeSpecArr);
            }
            return (CaptivePortalProbeSpec[]) getArrayConfig(captivePortalProbeSpecArr, R$array.config_captive_portal_fallback_probe_specs, R$array.default_captive_portal_fallback_probe_specs, $$Lambda$xxQKW2zlaIKTbt0V96C5vfmzCto.INSTANCE);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing configured fallback probe specs", e);
            return null;
        }
    }

    private String getSettingFromResource(Context context, int i, int i2, String str) {
        Resources resources = context.getResources();
        String string = resources.getString(i);
        if (!TextUtils.isEmpty(string)) {
            return string;
        }
        String setting = this.mDependencies.getSetting(context, str, (String) null);
        if (!TextUtils.isEmpty(setting)) {
            return setting;
        }
        return resources.getString(i2);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.server.connectivity.NetworkMonitor */
    /* JADX WARN: Multi-variable type inference failed */
    private <T> T[] getArrayConfig(T[] tArr, int i, int i2, Function<String, T> function) {
        Resources resources = this.mContext.getResources();
        String[] stringArray = resources.getStringArray(i);
        if (stringArray.length == 0) {
            if (tArr.length > 0) {
                return tArr;
            }
            stringArray = resources.getStringArray(i2);
        }
        return (T[]) convertStrings(stringArray, function, Arrays.copyOf(tArr, 0));
    }

    private <T> T[] convertStrings(String[] strArr, Function<String, T> function, T[] tArr) {
        ArrayList arrayList = new ArrayList(strArr.length);
        for (String str : strArr) {
            T t = null;
            try {
                t = function.apply(str);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing configuration", e);
            }
            if (t != null) {
                arrayList.add(t);
            }
        }
        return (T[]) arrayList.toArray(tArr);
    }

    private String getCaptivePortalUserAgent() {
        return this.mDependencies.getDeviceConfigProperty("connectivity", "captive_portal_user_agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.32 Safari/537.36");
    }

    private URL nextFallbackUrl() {
        if (this.mCaptivePortalFallbackUrls.length == 0) {
            return null;
        }
        int abs = Math.abs(this.mNextFallbackUrlIndex) % this.mCaptivePortalFallbackUrls.length;
        this.mNextFallbackUrlIndex += this.mRandom.nextInt();
        return this.mCaptivePortalFallbackUrls[abs];
    }

    private CaptivePortalProbeSpec nextFallbackSpec() {
        if (NetworkStackUtils.isEmpty(this.mCaptivePortalFallbackSpecs)) {
            return null;
        }
        int abs = Math.abs(this.mRandom.nextInt());
        CaptivePortalProbeSpec[] captivePortalProbeSpecArr = this.mCaptivePortalFallbackSpecs;
        return captivePortalProbeSpecArr[abs % captivePortalProbeSpecArr.length];
    }

    /* access modifiers changed from: protected */
    public CaptivePortalProbeResult isCaptivePortal() {
        URL url;
        CaptivePortalProbeResult captivePortalProbeResult;
        if (!this.mIsCaptivePortalCheckEnabled) {
            validationLog("Validation disabled.");
            return CaptivePortalProbeResult.SUCCESS;
        }
        URL url2 = this.mCaptivePortalHttpsUrl;
        URL url3 = this.mCaptivePortalHttpUrl;
        ProxyInfo httpProxy = this.mLinkProperties.getHttpProxy();
        if (httpProxy == null || Uri.EMPTY.equals(httpProxy.getPacFileUrl())) {
            url = null;
        } else {
            url = makeURL(httpProxy.getPacFileUrl().toString());
            if (url == null) {
                return CaptivePortalProbeResult.FAILED;
            }
        }
        if (url == null && (url3 == null || url2 == null)) {
            return CaptivePortalProbeResult.FAILED;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (url != null) {
            captivePortalProbeResult = sendDnsAndHttpProbes(null, url, 3);
            reportHttpProbeResult(8, captivePortalProbeResult);
        } else if (this.mUseHttps) {
            captivePortalProbeResult = sendParallelHttpProbes(httpProxy, url2, url3);
        } else {
            captivePortalProbeResult = sendDnsAndHttpProbes(httpProxy, url3, 1);
            reportHttpProbeResult(8, captivePortalProbeResult);
        }
        long elapsedRealtime2 = SystemClock.elapsedRealtime();
        sendNetworkConditionsBroadcast(true, captivePortalProbeResult.isPortal(), elapsedRealtime, elapsedRealtime2);
        log("isCaptivePortal: isSuccessful()=" + captivePortalProbeResult.isSuccessful() + " isPortal()=" + captivePortalProbeResult.isPortal() + " RedirectUrl=" + captivePortalProbeResult.redirectUrl + " isPartialConnectivity()=" + captivePortalProbeResult.isPartialConnectivity() + " Time=" + (elapsedRealtime2 - elapsedRealtime) + "ms");
        return captivePortalProbeResult;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private CaptivePortalProbeResult sendDnsAndHttpProbes(ProxyInfo proxyInfo, URL url, int i) {
        return sendDnsAndHttpProbes(proxyInfo, url, i, null);
    }

    private CaptivePortalProbeResult sendDnsAndHttpProbes(ProxyInfo proxyInfo, URL url, int i, CaptivePortalProbeSpec captivePortalProbeSpec) {
        InetAddress[] sendDnsProbe = sendDnsProbe(proxyInfo != null ? proxyInfo.getHost() : url.getHost());
        if (!this.mCheckForDnsPrivateIpResponse || i == 3 || proxyInfo != null || this.mIgnorePrivateIpResponse || !isPrivateIpAddress(sendDnsProbe)) {
            return sendHttpProbe(url, i, captivePortalProbeSpec);
        }
        return CaptivePortalProbeResult.PRIVATE_IP;
    }

    /* access modifiers changed from: protected */
    public InetAddress[] sendDnsProbeWithTimeout(String str, int i) throws UnknownHostException {
        return DnsUtils.getAllByName(this.mDependencies.getDnsResolver(), this.mCleartextDnsNetwork, str, -1, this.mNetworkCapabilities.hasTransport(1) ? 4 : 0, i);
    }

    private InetAddress[] sendDnsProbe(String str) {
        InetAddress[] inetAddressArr;
        String str2;
        int i;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        ValidationProbeEvent.getProbeName(0);
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        try {
            InetAddress[] sendDnsProbeWithTimeout = sendDnsProbeWithTimeout(str, getDnsProbeTimeout());
            StringBuffer stringBuffer = new StringBuffer();
            for (InetAddress inetAddress : sendDnsProbeWithTimeout) {
                stringBuffer.append(',');
                stringBuffer.append(inetAddress.getHostAddress());
            }
            str2 = "OK " + stringBuffer.substring(1);
            inetAddressArr = sendDnsProbeWithTimeout;
            i = 1;
        } catch (UnknownHostException unused) {
            str2 = "FAIL";
            inetAddressArr = null;
            i = 0;
        }
        long stop = stopwatch.stop();
        validationLog(0, str, String.format("%dms %s", Long.valueOf(stop), str2));
        logValidationProbe(stop, 0, i);
        return inetAddressArr;
    }

    private boolean isPrivateIpAddress(InetAddress[] inetAddressArr) {
        if (inetAddressArr == null) {
            return false;
        }
        for (InetAddress inetAddress : inetAddressArr) {
            if (inetAddress instanceof Inet4Address) {
                int hashCode = inetAddress.hashCode();
                int i = 0;
                while (true) {
                    int[][] iArr = mPrivateIpRange;
                    if (i >= iArr.length) {
                        continue;
                        break;
                    } else if ((iArr[i][1] & hashCode) == iArr[i][0]) {
                        return true;
                    } else {
                        i++;
                    }
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00d7, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00d8, code lost:
        r8 = null;
        r7 = r9;
        r16 = r13;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00dd, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0100, code lost:
        r7.disconnect();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x0123, code lost:
        r9.disconnect();
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00dd A[ExcHandler: all (th java.lang.Throwable), Splitter:B:7:0x0027] */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0100  */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x0112  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:54:0x0123  */
    public CaptivePortalProbeResult sendHttpProbe(URL url, int i, CaptivePortalProbeSpec captivePortalProbeSpec) {
        int i2;
        String str;
        String str2;
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-127);
        HttpURLConnection httpURLConnection = null;
        try {
            HttpURLConnection httpURLConnection2 = (HttpURLConnection) this.mCleartextDnsNetwork.openConnection(url);
            try {
                httpURLConnection2.setInstanceFollowRedirects(i == 3);
                httpURLConnection2.setConnectTimeout(10000);
                httpURLConnection2.setReadTimeout(10000);
                httpURLConnection2.setRequestProperty("Connection", "close");
                httpURLConnection2.setUseCaches(false);
                if (this.mCaptivePortalUserAgent != null) {
                    httpURLConnection2.setRequestProperty("User-Agent", this.mCaptivePortalUserAgent);
                }
                String obj = httpURLConnection2.getRequestProperties().toString();
                long elapsedRealtime = SystemClock.elapsedRealtime();
                int responseCode = httpURLConnection2.getResponseCode();
                str = httpURLConnection2.getHeaderField("location");
                long elapsedRealtime2 = SystemClock.elapsedRealtime();
                validationLog(i, url, "time=" + (elapsedRealtime2 - elapsedRealtime) + "ms ret=" + responseCode + " request=" + obj + " headers=" + httpURLConnection2.getHeaderFields());
                if (responseCode == 200) {
                    long contentLengthLong = httpURLConnection2.getContentLengthLong();
                    if (i == 3) {
                        validationLog(i, url, "PAC fetch 200 response interpreted as 204 response.");
                        i2 = 204;
                    } else {
                        if (contentLengthLong == -1) {
                            if (httpURLConnection2.getInputStream().read() == -1) {
                                validationLog(i, url, "Empty 200 response interpreted as failed response.");
                            }
                        } else if (contentLengthLong <= 4) {
                            validationLog(i, url, "200 response with Content-length <= 4 interpreted as failed response.");
                        }
                        i2 = 599;
                    }
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                    logValidationProbe(stopwatch.stop(), i, i2);
                    if (captivePortalProbeSpec == null) {
                        return new CaptivePortalProbeResult(i2, str, url.toString());
                    }
                    return captivePortalProbeSpec.getResult(i2, str);
                }
                i2 = responseCode;
                if (httpURLConnection2 != null) {
                }
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            } catch (IOException e) {
                e = e;
                str2 = null;
                httpURLConnection = httpURLConnection2;
                int i3 = 599;
                try {
                    validationLog(i, url, "Probe failed with exception " + e);
                    if (httpURLConnection != null) {
                    }
                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                    str = str2;
                    logValidationProbe(stopwatch.stop(), i, i2);
                    if (captivePortalProbeSpec == null) {
                    }
                } catch (Throwable th) {
                    th = th;
                    httpURLConnection2 = httpURLConnection;
                    if (httpURLConnection2 != null) {
                    }
                    TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                    throw th;
                }
            } catch (Throwable th2) {
            }
        } catch (IOException e2) {
            e = e2;
            str2 = null;
            int i32 = 599;
            validationLog(i, url, "Probe failed with exception " + e);
            if (httpURLConnection != null) {
            }
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            str = str2;
            logValidationProbe(stopwatch.stop(), i, i2);
            if (captivePortalProbeSpec == null) {
            }
        }
        logValidationProbe(stopwatch.stop(), i, i2);
        if (captivePortalProbeSpec == null) {
        }
    }

    private CaptivePortalProbeResult sendParallelHttpProbes(ProxyInfo proxyInfo, URL url, URL url2) {
        CaptivePortalProbeResult captivePortalProbeResult;
        CountDownLatch countDownLatch = new CountDownLatch(2);
        boolean z = true;
        if (this.mCheckForDnsPrivateIpResponse && !this.mInitialPrivateIpCheckDone && proxyInfo == null) {
            InetAddress[] sendDnsProbe = sendDnsProbe(url2.getHost());
            if (isPrivateIpAddress(sendDnsProbe) && !matchingAddressExists(sendDnsProbe, sendDnsProbe(url.getHost()))) {
                this.mIgnorePrivateIpResponse = true;
                Log.d(TAG, "Different PRIVATE IP for different URL, ignore Private IP responses.");
            }
            this.mInitialPrivateIpCheckDone = true;
        }
        AnonymousClass1ProbeThread r9 = new Thread(true, proxyInfo, url, url2, countDownLatch) {
            /* class com.android.server.connectivity.NetworkMonitor.AnonymousClass1ProbeThread */
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            final /* synthetic */ URL val$httpUrl;
            final /* synthetic */ URL val$httpsUrl;
            final /* synthetic */ CountDownLatch val$latch;
            final /* synthetic */ ProxyInfo val$proxy;

            /* JADX WARN: Incorrect args count in method signature: (Z)V */
            {
                this.val$proxy = r3;
                this.val$httpsUrl = r4;
                this.val$httpUrl = r5;
                this.val$latch = r6;
                this.mIsHttps = r2;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    while (this.val$latch.getCount() > 0) {
                        this.val$latch.countDown();
                    }
                }
                this.val$latch.countDown();
            }
        };
        AnonymousClass1ProbeThread r10 = new Thread(false, proxyInfo, url, url2, countDownLatch) {
            /* class com.android.server.connectivity.NetworkMonitor.AnonymousClass1ProbeThread */
            private final boolean mIsHttps;
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            final /* synthetic */ URL val$httpUrl;
            final /* synthetic */ URL val$httpsUrl;
            final /* synthetic */ CountDownLatch val$latch;
            final /* synthetic */ ProxyInfo val$proxy;

            /* JADX WARN: Incorrect args count in method signature: (Z)V */
            {
                this.val$proxy = r3;
                this.val$httpsUrl = r4;
                this.val$httpUrl = r5;
                this.val$latch = r6;
                this.mIsHttps = r2;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            public void run() {
                if (this.mIsHttps) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpsUrl, 2);
                } else {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                }
                if ((this.mIsHttps && this.mResult.isSuccessful()) || (!this.mIsHttps && this.mResult.isPortal())) {
                    while (this.val$latch.getCount() > 0) {
                        this.val$latch.countDown();
                    }
                }
                this.val$latch.countDown();
            }
        };
        try {
            r9.start();
            r10.start();
            countDownLatch.await(3000, TimeUnit.MILLISECONDS);
            CaptivePortalProbeResult result = r9.result();
            CaptivePortalProbeResult result2 = r10.result();
            if (result2.isPortal()) {
                reportHttpProbeResult(8, result2);
                return result2;
            } else if (result.isPortal() || result.isSuccessful()) {
                reportHttpProbeResult(16, result);
                return result;
            } else if (!this.mCheckForDnsPrivateIpResponse || (!result2.isDnsPrivateIpResponse() && !result.isDnsPrivateIpResponse())) {
                CaptivePortalProbeSpec nextFallbackSpec = nextFallbackSpec();
                URL url3 = nextFallbackSpec != null ? nextFallbackSpec.getUrl() : nextFallbackUrl();
                CaptivePortalProbeResult captivePortalProbeResult2 = null;
                if (url3 != null) {
                    if (this.mCheckForDnsPrivateIpResponse) {
                        captivePortalProbeResult = sendDnsAndHttpProbes(proxyInfo, url3, 4, nextFallbackSpec);
                    } else {
                        captivePortalProbeResult = sendHttpProbe(url3, 4, nextFallbackSpec);
                    }
                    captivePortalProbeResult2 = captivePortalProbeResult;
                    reportHttpProbeResult(32, captivePortalProbeResult2);
                    if (captivePortalProbeResult2.isPortal()) {
                        return captivePortalProbeResult2;
                    }
                }
                try {
                    r10.join();
                    reportHttpProbeResult(8, r10.result());
                    if (r10.result().isPortal()) {
                        return r10.result();
                    }
                    r9.join();
                    reportHttpProbeResult(16, r9.result());
                    if (!r10.result().isSuccessful()) {
                        if (captivePortalProbeResult2 == null || !captivePortalProbeResult2.isSuccessful()) {
                            z = false;
                        }
                    }
                    if (!r9.result().isFailed() || !z) {
                        return r9.result();
                    }
                    return CaptivePortalProbeResult.PARTIAL;
                } catch (InterruptedException unused) {
                    validationLog("Error: http or https probe wait interrupted!");
                    return CaptivePortalProbeResult.FAILED;
                }
            } else {
                validationLog("DNS response to the URL is private IP");
                return CaptivePortalProbeResult.FAILED;
            }
        } catch (InterruptedException unused2) {
            validationLog("Error: probes wait interrupted!");
            return CaptivePortalProbeResult.FAILED;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    public URL makeURL(String str) {
        if (str == null) {
            return null;
        }
        try {
            return new URL(str);
        } catch (MalformedURLException unused) {
            validationLog("Bad URL: " + str);
            return null;
        }
    }

    private void sendNetworkConditionsBroadcast(boolean z, boolean z2, long j, long j2) {
        CellIdentity cellIdentity;
        WifiInfo connectionInfo;
        Intent intent = new Intent("android.net.conn.NETWORK_CONDITIONS_MEASURED");
        if (this.mNetworkCapabilities.hasTransport(1)) {
            if (this.mWifiManager.isScanAlwaysAvailable() && (connectionInfo = this.mWifiManager.getConnectionInfo()) != null) {
                intent.putExtra("extra_ssid", connectionInfo.getSSID());
                intent.putExtra("extra_bssid", connectionInfo.getBSSID());
                intent.putExtra("extra_connectivity_type", 1);
            } else {
                return;
            }
        } else if (this.mNetworkCapabilities.hasTransport(0)) {
            intent.putExtra("extra_network_type", this.mTelephonyManager.getNetworkType());
            ServiceState serviceState = this.mTelephonyManager.getServiceState();
            if (serviceState == null) {
                logw("failed to retrieve ServiceState");
                return;
            }
            NetworkRegistrationInfo networkRegistrationInfo = serviceState.getNetworkRegistrationInfo(2, 1);
            if (networkRegistrationInfo == null) {
                cellIdentity = null;
            } else {
                cellIdentity = networkRegistrationInfo.getCellIdentity();
            }
            intent.putExtra("extra_cellid", cellIdentity);
            intent.putExtra("extra_connectivity_type", 0);
        } else {
            return;
        }
        intent.putExtra("extra_response_received", z);
        intent.putExtra("extra_request_timestamp_ms", j);
        if (z) {
            intent.putExtra("extra_is_captive_portal", z2);
            intent.putExtra("extra_response_timestamp_ms", j2);
        }
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "android.permission.ACCESS_NETWORK_CONDITIONS");
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logNetworkEvent(int i) {
        this.mMetricsLog.log(this.mCleartextDnsNetwork, this.mNetworkCapabilities.getTransportTypes(), new NetworkEvent(i));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private int networkEventType(ValidationStage validationStage, EvaluationResult evaluationResult) {
        return validationStage.mIsFirstValidation ? evaluationResult.mIsValidated ? 8 : 10 : evaluationResult.mIsValidated ? 9 : 11;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void maybeLogEvaluationResult(int i) {
        if (this.mEvaluationTimer.isRunning()) {
            this.mMetricsLog.log(this.mCleartextDnsNetwork, this.mNetworkCapabilities.getTransportTypes(), new NetworkEvent(i, this.mEvaluationTimer.stop()));
            this.mEvaluationTimer.reset();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void logValidationProbe(long j, int i, int i2) {
        this.mMetricsLog.log(this.mCleartextDnsNetwork, this.mNetworkCapabilities.getTransportTypes(), new ValidationProbeEvent.Builder().setProbeType(i, validationStage().mIsFirstValidation).setReturnCode(i2).setDurationMs(j).build());
    }

    /* access modifiers changed from: package-private */
    public static class Dependencies {
        public static final Dependencies DEFAULT = new Dependencies();

        Dependencies() {
        }

        public Network getPrivateDnsBypassNetwork(Network network) {
            return new OneAddressPerFamilyNetwork(network);
        }

        public DnsResolver getDnsResolver() {
            return DnsResolver.getInstance();
        }

        public Random getRandom() {
            return new Random();
        }

        public int getSetting(Context context, String str, int i) {
            return Settings.Global.getInt(context.getContentResolver(), str, i);
        }

        public String getSetting(Context context, String str, String str2) {
            String string = Settings.Global.getString(context.getContentResolver(), str);
            return string != null ? string : str2;
        }

        public String getDeviceConfigProperty(String str, String str2, String str3) {
            return NetworkStackUtils.getDeviceConfigProperty(str, str2, str3);
        }

        public int getDeviceConfigPropertyInt(String str, String str2, int i) {
            return NetworkStackUtils.getDeviceConfigPropertyInt(str, str2, i);
        }
    }

    /* access modifiers changed from: protected */
    public class DnsStallDetector {
        private int mConsecutiveTimeoutCount = 0;
        final DnsResult[] mDnsEvents;
        final RingBufferIndices mResultIndices;
        private int mSize;

        DnsStallDetector(int i) {
            this.mSize = Math.max(20, i);
            int i2 = this.mSize;
            this.mDnsEvents = new DnsResult[i2];
            this.mResultIndices = new RingBufferIndices(i2);
        }

        /* access modifiers changed from: protected */
        public void accumulateConsecutiveDnsTimeoutCount(int i) {
            DnsResult dnsResult = new DnsResult(i);
            this.mDnsEvents[this.mResultIndices.add()] = dnsResult;
            if (dnsResult.isTimeout()) {
                this.mConsecutiveTimeoutCount++;
            } else {
                this.mConsecutiveTimeoutCount = 0;
            }
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean isDataStallSuspected(int i, int i2) {
            if (i <= 0) {
                Log.wtf(NetworkMonitor.TAG, "Timeout count threshold should be larger than 0.");
                return false;
            } else if (this.mConsecutiveTimeoutCount < i) {
                return false;
            } else {
                RingBufferIndices ringBufferIndices = this.mResultIndices;
                if (SystemClock.elapsedRealtime() - this.mDnsEvents[ringBufferIndices.indexOf(ringBufferIndices.size() - i)].mTimeStamp < ((long) i2)) {
                    return true;
                }
                return false;
            }
        }

        /* access modifiers changed from: package-private */
        public int getConsecutiveTimeoutCount() {
            return this.mConsecutiveTimeoutCount;
        }
    }

    /* access modifiers changed from: private */
    public static class DnsResult {
        private final int mReturnCode;
        private final long mTimeStamp = SystemClock.elapsedRealtime();

        DnsResult(int i) {
            this.mReturnCode = i;
        }

        /* access modifiers changed from: private */
        /* access modifiers changed from: public */
        private boolean isTimeout() {
            return this.mReturnCode == 255;
        }
    }

    /* access modifiers changed from: protected */
    public DnsStallDetector getDnsStallDetector() {
        return this.mDnsStallDetector;
    }

    private boolean dataStallEvaluateTypeEnabled(int i) {
        return (i & this.mDataStallEvaluationType) != 0;
    }

    /* access modifiers changed from: protected */
    public long getLastProbeTime() {
        return this.mLastProbeTime;
    }

    /* access modifiers changed from: protected */
    public boolean isDataStall() {
        if (!this.mNetworkCapabilities.hasCapability(11) && SystemClock.elapsedRealtime() - getLastProbeTime() < ((long) this.mDataStallMinEvaluateTime)) {
            return false;
        }
        boolean z = true;
        if (!dataStallEvaluateTypeEnabled(1) || !this.mDnsStallDetector.isDataStallSuspected(this.mConsecutiveDnsTimeoutThreshold, this.mDataStallValidDnsTimeThreshold)) {
            z = false;
        } else {
            logNetworkEvent(12);
        }
        if (VDBG_STALL) {
            log("isDataStall: result=" + z + ", consecutive dns timeout count=" + this.mDnsStallDetector.getConsecutiveTimeoutCount());
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public class EvaluationState {
        private int mEvaluationResult = 0;
        private int mProbeResults = 0;
        private String mRedirectUrl;

        protected EvaluationState() {
        }

        /* access modifiers changed from: protected */
        public void clearProbeResults() {
            this.mProbeResults = 0;
        }

        /* access modifiers changed from: protected */
        public void noteProbeResult(int i, boolean z) {
            if (z) {
                this.mProbeResults = i | this.mProbeResults;
                return;
            }
            this.mProbeResults = (~i) & this.mProbeResults;
        }

        /* access modifiers changed from: protected */
        public void reportEvaluationResult(int i, String str) {
            this.mEvaluationResult = i;
            this.mRedirectUrl = str;
            NetworkMonitor.this.notifyNetworkTested(getNetworkTestResult(), this.mRedirectUrl);
        }

        /* access modifiers changed from: protected */
        public int getNetworkTestResult() {
            if (NetworkMonitor.this.mCallbackVersion >= 3) {
                return this.mEvaluationResult | this.mProbeResults;
            }
            int i = this.mEvaluationResult;
            if ((i & 1) != 0) {
                return 0;
            }
            return (i & 2) != 0 ? 2 : 1;
        }
    }

    /* access modifiers changed from: protected */
    public EvaluationState getEvaluationState() {
        return this.mEvaluationState;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void maybeDisableHttpsProbing(boolean z) {
        this.mAcceptPartialConnectivity = z;
        if ((this.mEvaluationState.getNetworkTestResult() & 2) != 0 && this.mAcceptPartialConnectivity) {
            this.mUseHttps = false;
        }
    }

    /* access modifiers changed from: protected */
    public void reportHttpProbeResult(int i, CaptivePortalProbeResult captivePortalProbeResult) {
        boolean isSuccessful = captivePortalProbeResult.isSuccessful();
        if (isSuccessful) {
            i |= 4;
        }
        this.mEvaluationState.noteProbeResult(i, isSuccessful);
    }

    /* access modifiers changed from: protected */
    public CaptivePortalProbeResult isCaptivePortalForChinaWifi() {
        URL url;
        URL url2;
        URL url3;
        CaptivePortalProbeResult captivePortalProbeResult;
        if (!this.mIsCaptivePortalCheckEnabled) {
            validationLog("Validation disabled.");
            return CaptivePortalProbeResult.SUCCESS;
        }
        URL makeURL = makeURL("http://connectivity.samsung.com.cn/generate_204");
        if (this.mRunFullParallelCheck) {
            url2 = makeURL(SECONDARY_HTTP_URLS_CHINA[0]);
            url = makeURL(SECONDARY_HTTP_URLS_CHINA[1]);
        } else {
            Random random = new Random(SystemClock.elapsedRealtime());
            String[] strArr = SECONDARY_HTTP_URLS_CHINA;
            int length = strArr.length;
            int nextInt = random.nextInt(strArr.length);
            URL makeURL2 = makeURL(SECONDARY_HTTP_URLS_CHINA[nextInt % length]);
            url = makeURL(SECONDARY_HTTP_URLS_CHINA[(nextInt + 1) % length]);
            url2 = makeURL2;
        }
        ProxyInfo httpProxy = this.mLinkProperties.getHttpProxy();
        if (httpProxy == null || Uri.EMPTY.equals(httpProxy.getPacFileUrl())) {
            url3 = null;
        } else {
            url3 = makeURL(httpProxy.getPacFileUrl().toString());
            if (url3 == null) {
                return CaptivePortalProbeResult.FAILED;
            }
        }
        if (url3 == null && (makeURL == null || url2 == null || url == null)) {
            return CaptivePortalProbeResult.FAILED;
        }
        long elapsedRealtime = SystemClock.elapsedRealtime();
        if (url3 != null) {
            captivePortalProbeResult = sendDnsAndHttpProbes(null, url3, 3);
            reportHttpProbeResult(8, captivePortalProbeResult);
        } else {
            captivePortalProbeResult = sendParallelHttpProbesForChinaWifi(httpProxy, makeURL, url2, url, this.mRunFullParallelCheck);
        }
        long elapsedRealtime2 = SystemClock.elapsedRealtime();
        sendNetworkConditionsBroadcast(true, captivePortalProbeResult.isPortal(), elapsedRealtime, elapsedRealtime2);
        log("isCaptivePortalForChinaWifi: isSuccessful()=" + captivePortalProbeResult.isSuccessful() + " isPortal()=" + captivePortalProbeResult.isPortal() + " RedirectUrl=" + captivePortalProbeResult.redirectUrl + " isPartialConnectivity()=" + captivePortalProbeResult.isPartialConnectivity() + " Time=" + (elapsedRealtime2 - elapsedRealtime) + "ms");
        return captivePortalProbeResult;
    }

    private CaptivePortalProbeResult sendParallelHttpProbesForChinaWifi(ProxyInfo proxyInfo, URL url, URL url2, URL url3, boolean z) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch countDownLatch2 = new CountDownLatch(2);
        if (this.mCheckForDnsPrivateIpResponse && !this.mInitialPrivateIpCheckDone && proxyInfo == null) {
            InetAddress[] sendDnsProbe = sendDnsProbe(url.getHost());
            if (isPrivateIpAddress(sendDnsProbe) && !matchingAddressExists(sendDnsProbe, sendDnsProbe(url2.getHost()))) {
                this.mIgnorePrivateIpResponse = true;
                Log.d(TAG, "Different PRIVATE IP for different URL, ignore Private IP responses.");
            }
            this.mInitialPrivateIpCheckDone = true;
        }
        AnonymousClass2ProbeThread r13 = new Thread(0, proxyInfo, url, z, url2, url3, countDownLatch, countDownLatch2) {
            /* class com.android.server.connectivity.NetworkMonitor.AnonymousClass2ProbeThread */
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            private final int mType;
            final /* synthetic */ URL val$httpUrl;
            final /* synthetic */ URL val$httpUrlCn;
            final /* synthetic */ URL val$httpUrlCn2;
            final /* synthetic */ CountDownLatch val$latch;
            final /* synthetic */ CountDownLatch val$latch2;
            final /* synthetic */ ProxyInfo val$proxy;
            final /* synthetic */ boolean val$runFullParallelCheck;

            /* JADX WARN: Incorrect args count in method signature: (I)V */
            {
                this.val$proxy = r3;
                this.val$httpUrl = r4;
                this.val$runFullParallelCheck = r5;
                this.val$httpUrlCn = r6;
                this.val$httpUrlCn2 = r7;
                this.val$latch = r8;
                this.val$latch2 = r9;
                this.mType = r2;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            public void run() {
                int i = this.mType;
                if (i == 0) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                } else if (this.val$runFullParallelCheck && i == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn2, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn2, 1);
                }
                if (this.mType == 0) {
                    this.val$latch.countDown();
                }
                int i2 = this.mType;
                if (i2 == 1 || i2 == 2) {
                    if (!this.val$runFullParallelCheck) {
                        this.val$latch2.countDown();
                    }
                    this.val$latch2.countDown();
                }
            }
        };
        AnonymousClass2ProbeThread r14 = new Thread(1, proxyInfo, url, z, url2, url3, countDownLatch, countDownLatch2) {
            /* class com.android.server.connectivity.NetworkMonitor.AnonymousClass2ProbeThread */
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            private final int mType;
            final /* synthetic */ URL val$httpUrl;
            final /* synthetic */ URL val$httpUrlCn;
            final /* synthetic */ URL val$httpUrlCn2;
            final /* synthetic */ CountDownLatch val$latch;
            final /* synthetic */ CountDownLatch val$latch2;
            final /* synthetic */ ProxyInfo val$proxy;
            final /* synthetic */ boolean val$runFullParallelCheck;

            /* JADX WARN: Incorrect args count in method signature: (I)V */
            {
                this.val$proxy = r3;
                this.val$httpUrl = r4;
                this.val$runFullParallelCheck = r5;
                this.val$httpUrlCn = r6;
                this.val$httpUrlCn2 = r7;
                this.val$latch = r8;
                this.val$latch2 = r9;
                this.mType = r2;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            public void run() {
                int i = this.mType;
                if (i == 0) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                } else if (this.val$runFullParallelCheck && i == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn2, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn2, 1);
                }
                if (this.mType == 0) {
                    this.val$latch.countDown();
                }
                int i2 = this.mType;
                if (i2 == 1 || i2 == 2) {
                    if (!this.val$runFullParallelCheck) {
                        this.val$latch2.countDown();
                    }
                    this.val$latch2.countDown();
                }
            }
        };
        AnonymousClass2ProbeThread r15 = new Thread(2, proxyInfo, url, z, url2, url3, countDownLatch, countDownLatch2) {
            /* class com.android.server.connectivity.NetworkMonitor.AnonymousClass2ProbeThread */
            private volatile CaptivePortalProbeResult mResult = CaptivePortalProbeResult.FAILED;
            private final int mType;
            final /* synthetic */ URL val$httpUrl;
            final /* synthetic */ URL val$httpUrlCn;
            final /* synthetic */ URL val$httpUrlCn2;
            final /* synthetic */ CountDownLatch val$latch;
            final /* synthetic */ CountDownLatch val$latch2;
            final /* synthetic */ ProxyInfo val$proxy;
            final /* synthetic */ boolean val$runFullParallelCheck;

            /* JADX WARN: Incorrect args count in method signature: (I)V */
            {
                this.val$proxy = r3;
                this.val$httpUrl = r4;
                this.val$runFullParallelCheck = r5;
                this.val$httpUrlCn = r6;
                this.val$httpUrlCn2 = r7;
                this.val$latch = r8;
                this.val$latch2 = r9;
                this.mType = r2;
            }

            public CaptivePortalProbeResult result() {
                return this.mResult;
            }

            public void run() {
                int i = this.mType;
                if (i == 0) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrl, 1);
                } else if (this.val$runFullParallelCheck && i == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndHttpProbes(this.val$proxy, this.val$httpUrlCn2, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 1) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn, 1);
                } else if (!this.val$runFullParallelCheck && this.mType == 2) {
                    this.mResult = NetworkMonitor.this.sendDnsAndCheckSocketSetup(this.val$proxy, this.val$httpUrlCn2, 1);
                }
                if (this.mType == 0) {
                    this.val$latch.countDown();
                }
                int i2 = this.mType;
                if (i2 == 1 || i2 == 2) {
                    if (!this.val$runFullParallelCheck) {
                        this.val$latch2.countDown();
                    }
                    this.val$latch2.countDown();
                }
            }
        };
        if (z) {
            try {
                r13.start();
                countDownLatch.await(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException unused) {
                validationLog("Error: probes wait interrupted!");
                return CaptivePortalProbeResult.FAILED;
            }
        }
        CaptivePortalProbeResult result = r13.result();
        if (result.isPortal()) {
            reportHttpProbeResult(8, result);
            return result;
        } else if (result.isSuccessful()) {
            reportHttpProbeResult(16, result);
            return result;
        } else if (!this.mCheckForDnsPrivateIpResponse || !result.isDnsPrivateIpResponse()) {
            try {
                r14.start();
                r15.start();
                countDownLatch2.await(3000, TimeUnit.MILLISECONDS);
                CaptivePortalProbeResult result2 = r14.result();
                CaptivePortalProbeResult result3 = r15.result();
                if (z && result2.isPortal() && result3.isPortal()) {
                    String str = result2.redirectUrl;
                    String str2 = result3.redirectUrl;
                    if (!(str == null || str2 == null)) {
                        try {
                            URL url4 = new URL(str);
                            if (url4.getHost().equals(new URL(str2).getHost())) {
                                String str3 = TAG;
                                Log.d(str3, "RDTTSL - " + url4.getHost());
                                reportHttpProbeResult(32, result2);
                                return result2;
                            }
                        } catch (MalformedURLException e) {
                            String str4 = TAG;
                            Log.e(str4, "sendParallelHttpProbesForChinaWifi MalformedURLException: " + e);
                        }
                    }
                }
                if (this.mCheckForDnsPrivateIpResponse && (result2.isDnsPrivateIpResponse() || result3.isDnsPrivateIpResponse())) {
                    validationLog("DNS response to the URL is private IP - CHINA WIFI");
                    return CaptivePortalProbeResult.FAILED;
                } else if (result2.mHttpResponseCode == 599 && result3.mHttpResponseCode == 599) {
                    try {
                        r13.join();
                        reportHttpProbeResult(8, r13.result());
                        reportHttpProbeResult(16, r13.result());
                        return r13.result();
                    } catch (InterruptedException unused2) {
                        validationLog("Error: http or https probe wait interrupted!");
                        return CaptivePortalProbeResult.FAILED;
                    }
                } else {
                    CaptivePortalProbeResult captivePortalProbeResult = new CaptivePortalProbeResult(204, null, result2.mHttpResponseCode != 599 ? result2.detectUrl : result3.detectUrl);
                    reportHttpProbeResult(16, captivePortalProbeResult);
                    return captivePortalProbeResult;
                }
            } catch (InterruptedException unused3) {
                validationLog("Error: probes wait interrupted! - CHINA WIFI");
                return CaptivePortalProbeResult.FAILED;
            }
        } else {
            validationLog("DNS response to the URL is private IP - CHINA WIFI");
            return CaptivePortalProbeResult.FAILED;
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private CaptivePortalProbeResult sendDnsAndCheckSocketSetup(ProxyInfo proxyInfo, URL url, int i) {
        InetAddress[] sendDnsProbe = sendDnsProbe(proxyInfo != null ? proxyInfo.getHost() : url.getHost());
        if (!this.mCheckForDnsPrivateIpResponse || i == 3 || proxyInfo != null || !isPrivateIpAddress(sendDnsProbe)) {
            return checkSocketSetup(url, i, null);
        }
        return CaptivePortalProbeResult.PRIVATE_IP;
    }

    /* access modifiers changed from: protected */
    public CaptivePortalProbeResult checkSocketSetup(URL url, int i, CaptivePortalProbeSpec captivePortalProbeSpec) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-127);
        int i2 = 599;
        try {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            Socket createSocket = this.mNetwork.getSocketFactory().createSocket(url.getHost(), 80);
            long elapsedRealtime2 = SystemClock.elapsedRealtime();
            i2 = 204;
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("checkSocketSetup:  time=");
            long j = elapsedRealtime2 - elapsedRealtime;
            sb.append(j);
            sb.append("ms  ret=");
            sb.append(204);
            sb.append("  socket=");
            sb.append(createSocket.toString());
            Log.d(str, sb.toString());
            validationLog(i, url, "time=" + j + "ms ret=" + 204 + " [checkSocketSetup] socket=" + createSocket.toString());
        } catch (IOException e) {
            validationLog(i, url, "checkSocketSetup failed with exception " + e);
        } catch (Throwable th) {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            throw th;
        }
        TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
        logValidationProbe(stopwatch.stop(), i, i2);
        if (captivePortalProbeSpec == null) {
            return new CaptivePortalProbeResult(i2, null, url.toString());
        }
        return captivePortalProbeSpec.getResult(i2, null);
    }

    public boolean inChinaNetwork() {
        if (this.mIsWifiOnly) {
            updateCountryIsoCode();
            if (isChineseIso(this.mScanResultsCountryIso) || isChineseIso(this.mDeviceCountryIso)) {
                String str = TAG;
                Log.d(str, "Wi-Fi Only CISO: " + this.mScanResultsCountryIso + "/" + this.mDeviceCountryIso);
                return true;
            }
            String str2 = TAG;
            Log.d(str2, "Wi-Fi Only ISO: " + this.mScanResultsCountryIso + "/" + this.mDeviceCountryIso);
            return false;
        }
        String str3 = this.mCountryIso;
        if (str3 == null || str3.length() != 2) {
            updateCountryIsoCode();
        }
        if (!isChineseIso(this.mCountryIso)) {
            return false;
        }
        String str4 = TAG;
        Log.d(str4, "CISO: " + this.mCountryIso);
        return true;
    }

    private boolean isChineseIso(String str) {
        return "cn".equalsIgnoreCase(str) || "hk".equalsIgnoreCase(str) || "mo".equalsIgnoreCase(str);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void updateCountryIsoCode() {
        TelephonyManager telephonyManager = this.mTelephonyManager;
        if (telephonyManager != null) {
            this.mTelephonyCountryIso = telephonyManager.getNetworkCountryIso();
            String str = TAG;
            Log.d(str, "updateCountryIsoCode() via TelephonyManager : ISO : " + this.mTelephonyCountryIso);
        }
        try {
            this.mDeviceCountryIso = SemCscFeature.getInstance().getString("CountryISO");
            String str2 = TAG;
            Log.d(str2, "updateCountryIsoCode() via SemCscFeature : ISO : " + this.mDeviceCountryIso);
        } catch (Exception unused) {
        }
        if (this.mScanResultsCountryIso == null) {
            this.mScanResultsCountryIso = this.mDependencies.getSetting(this.mContext, "wifi_wcm_country_code_from_scan_result", (String) null);
            String str3 = TAG;
            Log.d(str3, "updateCountryIsoCode() via Scan Results : ISO : " + this.mScanResultsCountryIso);
        }
        String str4 = this.mTelephonyCountryIso;
        if (str4 == null || str4.length() != 2) {
            String str5 = this.mDeviceCountryIso;
            if (str5 != null && str5.length() == 2) {
                this.mCountryIso = this.mDeviceCountryIso;
            }
        } else {
            this.mCountryIso = this.mTelephonyCountryIso;
        }
        String str6 = TAG;
        Log.d(str6, "updateCountryIsoCode() ISO : " + this.mCountryIso + " [" + this.mTelephonyCountryIso + "/" + this.mDeviceCountryIso + "/" + this.mScanResultsCountryIso + "]");
    }

    private boolean matchingAddressExists(InetAddress[] inetAddressArr, InetAddress[] inetAddressArr2) {
        if (!(inetAddressArr == null || inetAddressArr2 == null)) {
            for (InetAddress inetAddress : inetAddressArr) {
                for (InetAddress inetAddress2 : inetAddressArr2) {
                    if ((inetAddress instanceof Inet4Address) && (inetAddress2 instanceof Inet4Address) && inetAddress.equals(inetAddress2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setRoamEventFromWCM(int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "wifi_wcm_event_roam_complete", i);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private boolean getRoamEventfromWCM() {
        return this.mDependencies.getSetting(this.mContext, "wifi_wcm_event_roam_complete", 0) == 1;
    }

    private void updateConfigForCtcRoaming() {
        int slotIndex = SubscriptionManager.getSlotIndex(SubscriptionManager.getDefaultDataSubscriptionId());
        String semGetTelephonyProperty = TelephonyManager.semGetTelephonyProperty(slotIndex, "ril.simoperator", "ETC");
        String semGetTelephonyProperty2 = TelephonyManager.semGetTelephonyProperty(slotIndex, "gsm.sim.operator.numeric", "");
        if (!"CTC".equals(semGetTelephonyProperty)) {
            return;
        }
        if ("20404".equals(semGetTelephonyProperty2) || "45403".equals(semGetTelephonyProperty2) || "45431".equals(semGetTelephonyProperty2)) {
            Configuration configuration = this.mContext.getResources().getConfiguration();
            configuration.mcc = 460;
            this.mContext = this.mContext.createConfigurationContext(configuration);
            log("update mcc to 460 in configuration");
        }
    }
}
