package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOnSameL3NetworkResponseListener extends IInterface {
    void onSameL3NetworkResponse(StatusParcelable statusParcelable, SameL3NetworkResponseParcelable sameL3NetworkResponseParcelable) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnSameL3NetworkResponseListener {
        public static IOnSameL3NetworkResponseListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ipmemorystore.IOnSameL3NetworkResponseListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOnSameL3NetworkResponseListener)) {
                return new Proxy(iBinder);
            }
            return (IOnSameL3NetworkResponseListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IOnSameL3NetworkResponseListener {
            public static IOnSameL3NetworkResponseListener sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.ipmemorystore.IOnSameL3NetworkResponseListener
            public void onSameL3NetworkResponse(StatusParcelable statusParcelable, SameL3NetworkResponseParcelable sameL3NetworkResponseParcelable) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ipmemorystore.IOnSameL3NetworkResponseListener");
                    if (statusParcelable != null) {
                        obtain.writeInt(1);
                        statusParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (sameL3NetworkResponseParcelable != null) {
                        obtain.writeInt(1);
                        sameL3NetworkResponseParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onSameL3NetworkResponse(statusParcelable, sameL3NetworkResponseParcelable);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IOnSameL3NetworkResponseListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
