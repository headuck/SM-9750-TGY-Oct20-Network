package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IIpMemoryStoreCallbacks extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void onIpMemoryStoreFetched(IIpMemoryStore iIpMemoryStore) throws RemoteException;

    public static abstract class Stub extends Binder implements IIpMemoryStoreCallbacks {
        public static IIpMemoryStoreCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.IIpMemoryStoreCallbacks");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IIpMemoryStoreCallbacks)) {
                return new Proxy(iBinder);
            }
            return (IIpMemoryStoreCallbacks) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IIpMemoryStoreCallbacks {
            public static IIpMemoryStoreCallbacks sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.IIpMemoryStoreCallbacks
            public void onIpMemoryStoreFetched(IIpMemoryStore iIpMemoryStore) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.IIpMemoryStoreCallbacks");
                    obtain.writeStrongBinder(iIpMemoryStore != null ? iIpMemoryStore.asBinder() : null);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onIpMemoryStoreFetched(iIpMemoryStore);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.IIpMemoryStoreCallbacks
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel obtain = Parcel.obtain();
                    Parcel obtain2 = Parcel.obtain();
                    try {
                        obtain.writeInterfaceToken("android.net.IIpMemoryStoreCallbacks");
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

        public static IIpMemoryStoreCallbacks getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
