package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOnStatusListener extends IInterface {
    void onComplete(StatusParcelable statusParcelable) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnStatusListener {
        public static IOnStatusListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ipmemorystore.IOnStatusListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOnStatusListener)) {
                return new Proxy(iBinder);
            }
            return (IOnStatusListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IOnStatusListener {
            public static IOnStatusListener sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.ipmemorystore.IOnStatusListener
            public void onComplete(StatusParcelable statusParcelable) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ipmemorystore.IOnStatusListener");
                    if (statusParcelable != null) {
                        obtain.writeInt(1);
                        statusParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onComplete(statusParcelable);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IOnStatusListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
