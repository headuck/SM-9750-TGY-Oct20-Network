package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkStackStatusCallback extends IInterface {
    void onStatusAvailable(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkStackStatusCallback {
        public static INetworkStackStatusCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.INetworkStackStatusCallback");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof INetworkStackStatusCallback)) {
                return new Proxy(iBinder);
            }
            return (INetworkStackStatusCallback) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements INetworkStackStatusCallback {
            public static INetworkStackStatusCallback sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.INetworkStackStatusCallback
            public void onStatusAvailable(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.INetworkStackStatusCallback");
                    obtain.writeInt(i);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onStatusAvailable(i);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static INetworkStackStatusCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
