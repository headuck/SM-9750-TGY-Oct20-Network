package android.net.dhcp;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IDhcpServerCallbacks extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void onDhcpServerCreated(int i, IDhcpServer iDhcpServer) throws RemoteException;

    public static abstract class Stub extends Binder implements IDhcpServerCallbacks {
        public static IDhcpServerCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.dhcp.IDhcpServerCallbacks");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IDhcpServerCallbacks)) {
                return new Proxy(iBinder);
            }
            return (IDhcpServerCallbacks) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IDhcpServerCallbacks {
            public static IDhcpServerCallbacks sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.dhcp.IDhcpServerCallbacks
            public void onDhcpServerCreated(int i, IDhcpServer iDhcpServer) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.dhcp.IDhcpServerCallbacks");
                    obtain.writeInt(i);
                    obtain.writeStrongBinder(iDhcpServer != null ? iDhcpServer.asBinder() : null);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onDhcpServerCreated(i, iDhcpServer);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.dhcp.IDhcpServerCallbacks
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel obtain = Parcel.obtain();
                    Parcel obtain2 = Parcel.obtain();
                    try {
                        obtain.writeInterfaceToken("android.net.dhcp.IDhcpServerCallbacks");
                        this.mRemote.transact(16777215, obtain, obtain2, 0);
                        obtain2.readException();
                        this.mCachedVersion = obtain2.readInt();
                    } finally {
                        obtain2.recycle();
                        obtain.recycle();
                    }
                }
                return this.mCachedVersion;
            }
        }

        public static IDhcpServerCallbacks getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
