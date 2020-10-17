package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkMonitor extends IInterface {
    void forceReevaluation(int i) throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void launchCaptivePortalApp() throws RemoteException;

    void notifyCaptivePortalAppFinished(int i) throws RemoteException;

    void notifyDnsResponse(int i) throws RemoteException;

    void notifyLinkPropertiesChanged(LinkProperties linkProperties) throws RemoteException;

    void notifyNetworkCapabilitiesChanged(NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkConnected(LinkProperties linkProperties, NetworkCapabilities networkCapabilities) throws RemoteException;

    void notifyNetworkDisconnected() throws RemoteException;

    void notifyPrivateDnsChanged(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException;

    void setAcceptPartialConnectivity() throws RemoteException;

    void start() throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkMonitor {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.INetworkMonitor");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 16777215) {
                parcel.enforceInterface("android.net.INetworkMonitor");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                PrivateDnsConfigParcel privateDnsConfigParcel = null;
                NetworkCapabilities networkCapabilities = null;
                LinkProperties linkProperties = null;
                NetworkCapabilities networkCapabilities2 = null;
                switch (i) {
                    case 1:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        start();
                        return true;
                    case 2:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        launchCaptivePortalApp();
                        return true;
                    case 3:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        notifyCaptivePortalAppFinished(parcel.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        setAcceptPartialConnectivity();
                        return true;
                    case 5:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        forceReevaluation(parcel.readInt());
                        return true;
                    case 6:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        if (parcel.readInt() != 0) {
                            privateDnsConfigParcel = PrivateDnsConfigParcel.CREATOR.createFromParcel(parcel);
                        }
                        notifyPrivateDnsChanged(privateDnsConfigParcel);
                        return true;
                    case 7:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        notifyDnsResponse(parcel.readInt());
                        return true;
                    case 8:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        LinkProperties linkProperties2 = parcel.readInt() != 0 ? (LinkProperties) LinkProperties.CREATOR.createFromParcel(parcel) : null;
                        if (parcel.readInt() != 0) {
                            networkCapabilities2 = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(parcel);
                        }
                        notifyNetworkConnected(linkProperties2, networkCapabilities2);
                        return true;
                    case 9:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        notifyNetworkDisconnected();
                        return true;
                    case 10:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        if (parcel.readInt() != 0) {
                            linkProperties = (LinkProperties) LinkProperties.CREATOR.createFromParcel(parcel);
                        }
                        notifyLinkPropertiesChanged(linkProperties);
                        return true;
                    case 11:
                        parcel.enforceInterface("android.net.INetworkMonitor");
                        if (parcel.readInt() != 0) {
                            networkCapabilities = (NetworkCapabilities) NetworkCapabilities.CREATOR.createFromParcel(parcel);
                        }
                        notifyNetworkCapabilitiesChanged(networkCapabilities);
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("android.net.INetworkMonitor");
                return true;
            }
        }
    }
}
