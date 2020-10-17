package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkMonitorCallbacks extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void hideProvisioningNotification() throws RemoteException;

    void notifyNetworkTested(int i, String str) throws RemoteException;

    void notifyPrivateDnsConfigResolved(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException;

    void onNetworkMonitorCreated(INetworkMonitor iNetworkMonitor) throws RemoteException;

    void showProvisioningNotification(String str, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkMonitorCallbacks {
        public static INetworkMonitorCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.INetworkMonitorCallbacks");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof INetworkMonitorCallbacks)) {
                return new Proxy(iBinder);
            }
            return (INetworkMonitorCallbacks) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements INetworkMonitorCallbacks {
            public static INetworkMonitorCallbacks sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void onNetworkMonitorCreated(INetworkMonitor iNetworkMonitor) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
                    obtain.writeStrongBinder(iNetworkMonitor != null ? iNetworkMonitor.asBinder() : null);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onNetworkMonitorCreated(iNetworkMonitor);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyNetworkTested(int i, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    if (this.mRemote.transact(2, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().notifyNetworkTested(i, str);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void notifyPrivateDnsConfigResolved(PrivateDnsConfigParcel privateDnsConfigParcel) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
                    if (privateDnsConfigParcel != null) {
                        obtain.writeInt(1);
                        privateDnsConfigParcel.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(3, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().notifyPrivateDnsConfigResolved(privateDnsConfigParcel);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void showProvisioningNotification(String str, String str2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    if (this.mRemote.transact(4, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().showProvisioningNotification(str, str2);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public void hideProvisioningNotification() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
                    if (this.mRemote.transact(5, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().hideProvisioningNotification();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.INetworkMonitorCallbacks
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel obtain = Parcel.obtain();
                    Parcel obtain2 = Parcel.obtain();
                    try {
                        obtain.writeInterfaceToken("android.net.INetworkMonitorCallbacks");
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

        public static INetworkMonitorCallbacks getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
