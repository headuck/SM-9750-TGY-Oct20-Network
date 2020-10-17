package android.net.dhcp;

import android.net.INetworkStackStatusCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDhcpServer extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void start(INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    void stop(INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    void updateParams(DhcpServingParamsParcel dhcpServingParamsParcel, INetworkStackStatusCallback iNetworkStackStatusCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IDhcpServer {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.dhcp.IDhcpServer");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1) {
                parcel.enforceInterface("android.net.dhcp.IDhcpServer");
                start(INetworkStackStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 2) {
                parcel.enforceInterface("android.net.dhcp.IDhcpServer");
                updateParams(parcel.readInt() != 0 ? DhcpServingParamsParcel.CREATOR.createFromParcel(parcel) : null, INetworkStackStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 3) {
                parcel.enforceInterface("android.net.dhcp.IDhcpServer");
                stop(INetworkStackStatusCallback.Stub.asInterface(parcel.readStrongBinder()));
                return true;
            } else if (i == 16777215) {
                parcel.enforceInterface("android.net.dhcp.IDhcpServer");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            } else {
                parcel2.writeString("android.net.dhcp.IDhcpServer");
                return true;
            }
        }
    }
}
