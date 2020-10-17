package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOnBlobRetrievedListener extends IInterface {
    void onBlobRetrieved(StatusParcelable statusParcelable, String str, String str2, Blob blob) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnBlobRetrievedListener {
        public static IOnBlobRetrievedListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ipmemorystore.IOnBlobRetrievedListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOnBlobRetrievedListener)) {
                return new Proxy(iBinder);
            }
            return (IOnBlobRetrievedListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IOnBlobRetrievedListener {
            public static IOnBlobRetrievedListener sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.ipmemorystore.IOnBlobRetrievedListener
            public void onBlobRetrieved(StatusParcelable statusParcelable, String str, String str2, Blob blob) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ipmemorystore.IOnBlobRetrievedListener");
                    if (statusParcelable != null) {
                        obtain.writeInt(1);
                        statusParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    if (blob != null) {
                        obtain.writeInt(1);
                        blob.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onBlobRetrieved(statusParcelable, str, str2, blob);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IOnBlobRetrievedListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
