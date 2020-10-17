package android.net.ipmemorystore;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IOnNetworkAttributesRetrievedListener extends IInterface {
    void onNetworkAttributesRetrieved(StatusParcelable statusParcelable, String str, NetworkAttributesParcelable networkAttributesParcelable) throws RemoteException;

    public static abstract class Stub extends Binder implements IOnNetworkAttributesRetrievedListener {
        public static IOnNetworkAttributesRetrievedListener asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IOnNetworkAttributesRetrievedListener)) {
                return new Proxy(iBinder);
            }
            return (IOnNetworkAttributesRetrievedListener) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        public static class Proxy implements IOnNetworkAttributesRetrievedListener {
            public static IOnNetworkAttributesRetrievedListener sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener
            public void onNetworkAttributesRetrieved(StatusParcelable statusParcelable, String str, NetworkAttributesParcelable networkAttributesParcelable) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener");
                    if (statusParcelable != null) {
                        obtain.writeInt(1);
                        statusParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    obtain.writeString(str);
                    if (networkAttributesParcelable != null) {
                        obtain.writeInt(1);
                        networkAttributesParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onNetworkAttributesRetrieved(statusParcelable, str, networkAttributesParcelable);
                    }
                } finally {
                    obtain.recycle();
                }
            }
        }

        public static IOnNetworkAttributesRetrievedListener getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
