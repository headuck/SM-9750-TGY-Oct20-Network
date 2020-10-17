package android.net;

import android.net.IIpMemoryStoreCallbacks;
import android.net.INetworkMonitorCallbacks;
import android.net.dhcp.DhcpServingParamsParcel;
import android.net.dhcp.IDhcpServerCallbacks;
import android.net.p000ip.IIpClientCallbacks;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkStackConnector extends IInterface {
    void fetchIpMemoryStore(IIpMemoryStoreCallbacks iIpMemoryStoreCallbacks) throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void makeDhcpServer(String str, DhcpServingParamsParcel dhcpServingParamsParcel, IDhcpServerCallbacks iDhcpServerCallbacks) throws RemoteException;

    void makeIpClient(String str, IIpClientCallbacks iIpClientCallbacks) throws RemoteException;

    void makeNetworkMonitor(Network network, String str, INetworkMonitorCallbacks iNetworkMonitorCallbacks) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkStackConnector {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.INetworkStackConnector");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            DhcpServingParamsParcel dhcpServingParamsParcel = null;
            Network network = null;
            if (i == 1) {
                parcel.enforceInterface("android.net.INetworkStackConnector");
                String readString = parcel.readString();
                if (parcel.readInt() != 0) {
                    dhcpServingParamsParcel = DhcpServingParamsParcel.CREATOR.createFromParcel(parcel);
                }
                makeDhcpServer(readString, dhcpServingParamsParcel, IDhcpServerCallbacks.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 2) {
                parcel.enforceInterface("android.net.INetworkStackConnector");
                if (parcel.readInt() != 0) {
                    network = (Network) Network.CREATOR.createFromParcel(parcel);
                }
                makeNetworkMonitor(network, parcel.readString(), INetworkMonitorCallbacks.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 3) {
                parcel.enforceInterface("android.net.INetworkStackConnector");
                makeIpClient(parcel.readString(), IIpClientCallbacks.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 4) {
                parcel.enforceInterface("android.net.INetworkStackConnector");
                fetchIpMemoryStore(IIpMemoryStoreCallbacks.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 16777215) {
                parcel.enforceInterface("android.net.INetworkStackConnector");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            } else {
                parcel2.writeString("android.net.INetworkStackConnector");
                return true;
            }
        }
    }
}
