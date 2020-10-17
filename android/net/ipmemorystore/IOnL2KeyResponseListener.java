package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOnL2KeyResponseListener extends IInterface {
    void onL2KeyResponse(StatusParcelable statusParcelable, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnL2KeyResponseListener {
        public static IOnL2KeyResponseListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ipmemorystore.IOnL2KeyResponseListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOnL2KeyResponseListener)) {
                return new Proxy(iBinder);
            }
            return (IOnL2KeyResponseListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IOnL2KeyResponseListener {
            public static IOnL2KeyResponseListener sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.ipmemorystore.IOnL2KeyResponseListener
            public void onL2KeyResponse(StatusParcelable statusParcelable, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ipmemorystore.IOnL2KeyResponseListener");
                    if (statusParcelable != null) {
                        obtain.writeInt(1);
                        statusParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    obtain.writeString(str);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onL2KeyResponse(statusParcelable, str);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IOnL2KeyResponseListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
