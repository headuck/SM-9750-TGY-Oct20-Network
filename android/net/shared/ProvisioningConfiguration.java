package android.net.shared;

import android.net.Network;
import android.net.ProvisioningConfigurationParcelable;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import java.util.Objects;
import java.util.StringJoiner;

public class ProvisioningConfiguration {
    public ApfCapabilities mApfCapabilities;
    public String mDisplayName;
    public boolean mEnableIPv4;
    public boolean mEnableIPv6;
    public int mIPv6AddrGenMode;
    public InitialConfiguration mInitialConfig;
    public Network mNetwork;
    public int mProvisioningTimeoutMs;
    public int mRequestedPreDhcpActionMs;
    public StaticIpConfiguration mStaticIpConfig;
    public boolean mUsingIpReachabilityMonitor;
    public boolean mUsingMultinetworkPolicyTracker;

    public ProvisioningConfiguration() {
        this.mEnableIPv4 = true;
        this.mEnableIPv6 = true;
        this.mUsingMultinetworkPolicyTracker = true;
        this.mUsingIpReachabilityMonitor = true;
        this.mProvisioningTimeoutMs = 18000;
        this.mIPv6AddrGenMode = 2;
        this.mNetwork = null;
        this.mDisplayName = null;
    }

    public ProvisioningConfiguration(ProvisioningConfiguration provisioningConfiguration) {
        this.mEnableIPv4 = true;
        this.mEnableIPv6 = true;
        this.mUsingMultinetworkPolicyTracker = true;
        this.mUsingIpReachabilityMonitor = true;
        this.mProvisioningTimeoutMs = 18000;
        this.mIPv6AddrGenMode = 2;
        StaticIpConfiguration staticIpConfiguration = null;
        this.mNetwork = null;
        this.mDisplayName = null;
        this.mEnableIPv4 = provisioningConfiguration.mEnableIPv4;
        this.mEnableIPv6 = provisioningConfiguration.mEnableIPv6;
        this.mUsingMultinetworkPolicyTracker = provisioningConfiguration.mUsingMultinetworkPolicyTracker;
        this.mUsingIpReachabilityMonitor = provisioningConfiguration.mUsingIpReachabilityMonitor;
        this.mRequestedPreDhcpActionMs = provisioningConfiguration.mRequestedPreDhcpActionMs;
        this.mInitialConfig = InitialConfiguration.copy(provisioningConfiguration.mInitialConfig);
        StaticIpConfiguration staticIpConfiguration2 = provisioningConfiguration.mStaticIpConfig;
        this.mStaticIpConfig = staticIpConfiguration2 != null ? new StaticIpConfiguration(staticIpConfiguration2) : staticIpConfiguration;
        this.mApfCapabilities = provisioningConfiguration.mApfCapabilities;
        this.mProvisioningTimeoutMs = provisioningConfiguration.mProvisioningTimeoutMs;
        this.mIPv6AddrGenMode = provisioningConfiguration.mIPv6AddrGenMode;
        this.mNetwork = provisioningConfiguration.mNetwork;
        this.mDisplayName = provisioningConfiguration.mDisplayName;
    }

    public static ProvisioningConfiguration fromStableParcelable(ProvisioningConfigurationParcelable provisioningConfigurationParcelable) {
        StaticIpConfiguration staticIpConfiguration = null;
        if (provisioningConfigurationParcelable == null) {
            return null;
        }
        ProvisioningConfiguration provisioningConfiguration = new ProvisioningConfiguration();
        provisioningConfiguration.mEnableIPv4 = provisioningConfigurationParcelable.enableIPv4;
        provisioningConfiguration.mEnableIPv6 = provisioningConfigurationParcelable.enableIPv6;
        provisioningConfiguration.mUsingMultinetworkPolicyTracker = provisioningConfigurationParcelable.usingMultinetworkPolicyTracker;
        provisioningConfiguration.mUsingIpReachabilityMonitor = provisioningConfigurationParcelable.usingIpReachabilityMonitor;
        provisioningConfiguration.mRequestedPreDhcpActionMs = provisioningConfigurationParcelable.requestedPreDhcpActionMs;
        provisioningConfiguration.mInitialConfig = InitialConfiguration.fromStableParcelable(provisioningConfigurationParcelable.initialConfig);
        StaticIpConfiguration staticIpConfiguration2 = provisioningConfigurationParcelable.staticIpConfig;
        if (staticIpConfiguration2 != null) {
            staticIpConfiguration = new StaticIpConfiguration(staticIpConfiguration2);
        }
        provisioningConfiguration.mStaticIpConfig = staticIpConfiguration;
        provisioningConfiguration.mApfCapabilities = provisioningConfigurationParcelable.apfCapabilities;
        provisioningConfiguration.mProvisioningTimeoutMs = provisioningConfigurationParcelable.provisioningTimeoutMs;
        provisioningConfiguration.mIPv6AddrGenMode = provisioningConfigurationParcelable.ipv6AddrGenMode;
        provisioningConfiguration.mNetwork = provisioningConfigurationParcelable.network;
        provisioningConfiguration.mDisplayName = provisioningConfigurationParcelable.displayName;
        return provisioningConfiguration;
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", ProvisioningConfiguration.class.getSimpleName() + "{", "}");
        StringJoiner add = stringJoiner.add("mEnableIPv4: " + this.mEnableIPv4);
        StringJoiner add2 = add.add("mEnableIPv6: " + this.mEnableIPv6);
        StringJoiner add3 = add2.add("mUsingMultinetworkPolicyTracker: " + this.mUsingMultinetworkPolicyTracker);
        StringJoiner add4 = add3.add("mUsingIpReachabilityMonitor: " + this.mUsingIpReachabilityMonitor);
        StringJoiner add5 = add4.add("mRequestedPreDhcpActionMs: " + this.mRequestedPreDhcpActionMs);
        StringJoiner add6 = add5.add("mInitialConfig: " + this.mInitialConfig);
        StringJoiner add7 = add6.add("mStaticIpConfig: " + this.mStaticIpConfig);
        StringJoiner add8 = add7.add("mApfCapabilities: " + this.mApfCapabilities);
        StringJoiner add9 = add8.add("mProvisioningTimeoutMs: " + this.mProvisioningTimeoutMs);
        StringJoiner add10 = add9.add("mIPv6AddrGenMode: " + this.mIPv6AddrGenMode);
        StringJoiner add11 = add10.add("mNetwork: " + this.mNetwork);
        return add11.add("mDisplayName: " + this.mDisplayName).toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ProvisioningConfiguration)) {
            return false;
        }
        ProvisioningConfiguration provisioningConfiguration = (ProvisioningConfiguration) obj;
        if (this.mEnableIPv4 == provisioningConfiguration.mEnableIPv4 && this.mEnableIPv6 == provisioningConfiguration.mEnableIPv6 && this.mUsingMultinetworkPolicyTracker == provisioningConfiguration.mUsingMultinetworkPolicyTracker && this.mUsingIpReachabilityMonitor == provisioningConfiguration.mUsingIpReachabilityMonitor && this.mRequestedPreDhcpActionMs == provisioningConfiguration.mRequestedPreDhcpActionMs && Objects.equals(this.mInitialConfig, provisioningConfiguration.mInitialConfig) && Objects.equals(this.mStaticIpConfig, provisioningConfiguration.mStaticIpConfig) && Objects.equals(this.mApfCapabilities, provisioningConfiguration.mApfCapabilities) && this.mProvisioningTimeoutMs == provisioningConfiguration.mProvisioningTimeoutMs && this.mIPv6AddrGenMode == provisioningConfiguration.mIPv6AddrGenMode && Objects.equals(this.mNetwork, provisioningConfiguration.mNetwork) && Objects.equals(this.mDisplayName, provisioningConfiguration.mDisplayName)) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        InitialConfiguration initialConfiguration = this.mInitialConfig;
        return initialConfiguration == null || initialConfiguration.isValid();
    }
}
