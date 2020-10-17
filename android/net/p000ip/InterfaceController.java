package android.net.p000ip;

import android.net.INetd;
import android.net.InterfaceConfigurationParcel;
import android.net.LinkAddress;
import android.net.util.SharedLog;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;
import java.net.Inet4Address;
import java.net.InetAddress;

/* renamed from: android.net.ip.InterfaceController */
public class InterfaceController {
    private final String mIfName;
    private final SharedLog mLog;
    private final INetd mNetd;

    public InterfaceController(String str, INetd iNetd, SharedLog sharedLog) {
        this.mIfName = str;
        this.mNetd = iNetd;
        this.mLog = sharedLog;
    }

    private boolean setInterfaceAddress(LinkAddress linkAddress) {
        InterfaceConfigurationParcel interfaceConfigurationParcel = new InterfaceConfigurationParcel();
        interfaceConfigurationParcel.ifName = this.mIfName;
        interfaceConfigurationParcel.ipv4Addr = linkAddress.getAddress().getHostAddress();
        interfaceConfigurationParcel.prefixLength = linkAddress.getPrefixLength();
        interfaceConfigurationParcel.hwAddr = "";
        interfaceConfigurationParcel.flags = new String[0];
        try {
            this.mNetd.interfaceSetCfg(interfaceConfigurationParcel);
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            logError("Setting IPv4 address to %s/%d failed: %s", interfaceConfigurationParcel.ipv4Addr, Integer.valueOf(interfaceConfigurationParcel.prefixLength), e);
            return false;
        }
    }

    public boolean setIPv4Address(LinkAddress linkAddress) {
        if (!(linkAddress.getAddress() instanceof Inet4Address)) {
            return false;
        }
        return setInterfaceAddress(linkAddress);
    }

    public boolean clearIPv4Address() {
        return setInterfaceAddress(new LinkAddress("0.0.0.0/0"));
    }

    private boolean setEnableIPv6(boolean z) {
        try {
            this.mNetd.interfaceSetEnableIPv6(this.mIfName, z);
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            Object[] objArr = new Object[2];
            objArr[0] = z ? "enabling" : "disabling";
            objArr[1] = e;
            logError("%s IPv6 failed: %s", objArr);
            return false;
        }
    }

    public boolean enableIPv6() {
        return setEnableIPv6(true);
    }

    public boolean disableIPv6() {
        return setEnableIPv6(false);
    }

    public boolean setIPv6PrivacyExtensions(boolean z) {
        try {
            this.mNetd.interfaceSetIPv6PrivacyExtensions(this.mIfName, z);
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            Object[] objArr = new Object[2];
            objArr[0] = z ? "enabling" : "disabling";
            objArr[1] = e;
            logError("error %s IPv6 privacy extensions: %s", objArr);
            return false;
        }
    }

    public boolean setIPv6AddrGenModeIfSupported(int i) {
        try {
            this.mNetd.setIPv6AddrGenMode(this.mIfName, i);
        } catch (RemoteException e) {
            logError("Unable to set IPv6 addrgen mode: %s", e);
            return false;
        } catch (ServiceSpecificException e2) {
            if (e2.errorCode != OsConstants.EOPNOTSUPP) {
                logError("Unable to set IPv6 addrgen mode: %s", e2);
                return false;
            }
        }
        return true;
    }

    public boolean addAddress(LinkAddress linkAddress) {
        return addAddress(linkAddress.getAddress(), linkAddress.getPrefixLength());
    }

    public boolean addAddress(InetAddress inetAddress, int i) {
        try {
            this.mNetd.interfaceAddAddress(this.mIfName, inetAddress.getHostAddress(), i);
            return true;
        } catch (RemoteException | ServiceSpecificException e) {
            logError("failed to add %s/%d: %s", inetAddress, Integer.valueOf(i), e);
            return false;
        }
    }

    public boolean clearAllAddresses() {
        try {
            this.mNetd.interfaceClearAddrs(this.mIfName);
            return true;
        } catch (Exception e) {
            logError("Failed to clear addresses: %s", e);
            return false;
        }
    }

    private void logError(String str, Object... objArr) {
        this.mLog.mo563e(String.format(str, objArr));
    }
}
