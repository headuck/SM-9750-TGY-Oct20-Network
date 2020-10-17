package android.net;

import android.net.ipmemorystore.Blob;
import android.net.ipmemorystore.IOnBlobRetrievedListener;
import android.net.ipmemorystore.IOnL2KeyResponseListener;
import android.net.ipmemorystore.IOnNetworkAttributesRetrievedListener;
import android.net.ipmemorystore.IOnSameL3NetworkResponseListener;
import android.net.ipmemorystore.IOnStatusListener;
import android.net.ipmemorystore.NetworkAttributesParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IIpMemoryStore extends IInterface {
    void factoryReset() throws RemoteException;

    void findL2Key(NetworkAttributesParcelable networkAttributesParcelable, IOnL2KeyResponseListener iOnL2KeyResponseListener) throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void isSameNetwork(String str, String str2, IOnSameL3NetworkResponseListener iOnSameL3NetworkResponseListener) throws RemoteException;

    void retrieveBlob(String str, String str2, String str3, IOnBlobRetrievedListener iOnBlobRetrievedListener) throws RemoteException;

    void retrieveNetworkAttributes(String str, IOnNetworkAttributesRetrievedListener iOnNetworkAttributesRetrievedListener) throws RemoteException;

    void storeBlob(String str, String str2, String str3, Blob blob, IOnStatusListener iOnStatusListener) throws RemoteException;

    void storeNetworkAttributes(String str, NetworkAttributesParcelable networkAttributesParcelable, IOnStatusListener iOnStatusListener) throws RemoteException;

    public static abstract class Stub extends Binder implements IIpMemoryStore {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.IIpMemoryStore");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 16777215) {
                parcel.enforceInterface("android.net.IIpMemoryStore");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                NetworkAttributesParcelable networkAttributesParcelable = null;
                NetworkAttributesParcelable networkAttributesParcelable2 = null;
                Blob blob = null;
                switch (i) {
                    case 1:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        String readString = parcel.readString();
                        if (parcel.readInt() != 0) {
                            networkAttributesParcelable = NetworkAttributesParcelable.CREATOR.createFromParcel(parcel);
                        }
                        storeNetworkAttributes(readString, networkAttributesParcelable, IOnStatusListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 2:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        String readString2 = parcel.readString();
                        String readString3 = parcel.readString();
                        String readString4 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            blob = Blob.CREATOR.createFromParcel(parcel);
                        }
                        storeBlob(readString2, readString3, readString4, blob, IOnStatusListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 3:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        if (parcel.readInt() != 0) {
                            networkAttributesParcelable2 = NetworkAttributesParcelable.CREATOR.createFromParcel(parcel);
                        }
                        findL2Key(networkAttributesParcelable2, IOnL2KeyResponseListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 4:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        isSameNetwork(parcel.readString(), parcel.readString(), IOnSameL3NetworkResponseListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 5:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        retrieveNetworkAttributes(parcel.readString(), IOnNetworkAttributesRetrievedListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 6:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        retrieveBlob(parcel.readString(), parcel.readString(), parcel.readString(), IOnBlobRetrievedListener.Stub.asInterface(parcel.readStrongBinder()));
                        return true;
                    case 7:
                        parcel.enforceInterface("android.net.IIpMemoryStore");
                        factoryReset();
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("android.net.IIpMemoryStore");
                return true;
            }
        }
    }
}
