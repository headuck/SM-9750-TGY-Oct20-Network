package android.net;

import android.net.apf.ApfCapabilities;
import android.os.Parcel;
import android.os.Parcelable;

public class ProvisioningConfigurationParcelable implements Parcelable {
    public static final Parcelable.Creator<ProvisioningConfigurationParcelable> CREATOR = new Parcelable.Creator<ProvisioningConfigurationParcelable>() {
        /* class android.net.ProvisioningConfigurationParcelable.C00051 */

        @Override // android.os.Parcelable.Creator
        public ProvisioningConfigurationParcelable createFromParcel(Parcel parcel) {
            ProvisioningConfigurationParcelable provisioningConfigurationParcelable = new ProvisioningConfigurationParcelable();
            provisioningConfigurationParcelable.readFromParcel(parcel);
            return provisioningConfigurationParcelable;
        }

        @Override // android.os.Parcelable.Creator
        public ProvisioningConfigurationParcelable[] newArray(int i) {
            return new ProvisioningConfigurationParcelable[i];
        }
    };
    public ApfCapabilities apfCapabilities;
    public String displayName;
    public boolean enableIPv4;
    public boolean enableIPv6;
    public InitialConfigurationParcelable initialConfig;
    public int ipv6AddrGenMode;
    public Network network;
    public int provisioningTimeoutMs;
    public int requestedPreDhcpActionMs;
    public StaticIpConfiguration staticIpConfig;
    public boolean usingIpReachabilityMonitor;
    public boolean usingMultinetworkPolicyTracker;

    public int describeContents() {
        return 0;
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int dataPosition = parcel.dataPosition();
        parcel.writeInt(0);
        parcel.writeInt(this.enableIPv4 ? 1 : 0);
        parcel.writeInt(this.enableIPv6 ? 1 : 0);
        parcel.writeInt(this.usingMultinetworkPolicyTracker ? 1 : 0);
        parcel.writeInt(this.usingIpReachabilityMonitor ? 1 : 0);
        parcel.writeInt(this.requestedPreDhcpActionMs);
        if (this.initialConfig != null) {
            parcel.writeInt(1);
            this.initialConfig.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.staticIpConfig != null) {
            parcel.writeInt(1);
            this.staticIpConfig.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.apfCapabilities != null) {
            parcel.writeInt(1);
            this.apfCapabilities.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.provisioningTimeoutMs);
        parcel.writeInt(this.ipv6AddrGenMode);
        if (this.network != null) {
            parcel.writeInt(1);
            this.network.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.displayName);
        int dataPosition2 = parcel.dataPosition();
        parcel.setDataPosition(dataPosition);
        parcel.writeInt(dataPosition2 - dataPosition);
        parcel.setDataPosition(dataPosition2);
    }

    public final void readFromParcel(Parcel parcel) {
        int dataPosition = parcel.dataPosition();
        int readInt = parcel.readInt();
        if (readInt >= 0) {
            try {
                boolean z = true;
                this.enableIPv4 = parcel.readInt() != 0;
                if (parcel.dataPosition() - dataPosition < readInt) {
                    this.enableIPv6 = parcel.readInt() != 0;
                    if (parcel.dataPosition() - dataPosition < readInt) {
                        this.usingMultinetworkPolicyTracker = parcel.readInt() != 0;
                        if (parcel.dataPosition() - dataPosition < readInt) {
                            if (parcel.readInt() == 0) {
                                z = false;
                            }
                            this.usingIpReachabilityMonitor = z;
                            if (parcel.dataPosition() - dataPosition < readInt) {
                                this.requestedPreDhcpActionMs = parcel.readInt();
                                if (parcel.dataPosition() - dataPosition < readInt) {
                                    if (parcel.readInt() != 0) {
                                        this.initialConfig = InitialConfigurationParcelable.CREATOR.createFromParcel(parcel);
                                    } else {
                                        this.initialConfig = null;
                                    }
                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                        if (parcel.readInt() != 0) {
                                            this.staticIpConfig = (StaticIpConfiguration) StaticIpConfiguration.CREATOR.createFromParcel(parcel);
                                        } else {
                                            this.staticIpConfig = null;
                                        }
                                        if (parcel.dataPosition() - dataPosition < readInt) {
                                            if (parcel.readInt() != 0) {
                                                this.apfCapabilities = (ApfCapabilities) ApfCapabilities.CREATOR.createFromParcel(parcel);
                                            } else {
                                                this.apfCapabilities = null;
                                            }
                                            if (parcel.dataPosition() - dataPosition < readInt) {
                                                this.provisioningTimeoutMs = parcel.readInt();
                                                if (parcel.dataPosition() - dataPosition < readInt) {
                                                    this.ipv6AddrGenMode = parcel.readInt();
                                                    if (parcel.dataPosition() - dataPosition < readInt) {
                                                        if (parcel.readInt() != 0) {
                                                            this.network = (Network) Network.CREATOR.createFromParcel(parcel);
                                                        } else {
                                                            this.network = null;
                                                        }
                                                        if (parcel.dataPosition() - dataPosition < readInt) {
                                                            this.displayName = parcel.readString();
                                                            if (parcel.dataPosition() - dataPosition < readInt) {
                                                                parcel.setDataPosition(dataPosition + readInt);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                parcel.setDataPosition(dataPosition + readInt);
            }
        }
    }
}
