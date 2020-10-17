package android.net;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetdUnsolicitedEventListener extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void onInterfaceAdded(String str) throws RemoteException;

    void onInterfaceAddressRemoved(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceAddressUpdated(String str, String str2, int i, int i2) throws RemoteException;

    void onInterfaceChanged(String str, boolean z) throws RemoteException;

    void onInterfaceClassActivityChanged(boolean z, int i, long j, int i2) throws RemoteException;

    void onInterfaceDnsServerInfo(String str, long j, String[] strArr) throws RemoteException;

    void onInterfaceLinkStateChanged(String str, boolean z) throws RemoteException;

    void onInterfaceRemoved(String str) throws RemoteException;

    void onQuotaLimitReached(String str, String str2) throws RemoteException;

    void onRouteChanged(boolean z, String str, String str2, String str3) throws RemoteException;

    void onStrictCleartextDetected(int i, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements INetdUnsolicitedEventListener {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.INetdUnsolicitedEventListener");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 16777215) {
                parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                boolean z = false;
                switch (i) {
                    case 1:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceClassActivityChanged(parcel.readInt() != 0, parcel.readInt(), parcel.readLong(), parcel.readInt());
                        return true;
                    case 2:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onQuotaLimitReached(parcel.readString(), parcel.readString());
                        return true;
                    case 3:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceDnsServerInfo(parcel.readString(), parcel.readLong(), parcel.createStringArray());
                        return true;
                    case 4:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceAddressUpdated(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 5:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceAddressRemoved(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt());
                        return true;
                    case 6:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceAdded(parcel.readString());
                        return true;
                    case 7:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onInterfaceRemoved(parcel.readString());
                        return true;
                    case 8:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        String readString = parcel.readString();
                        if (parcel.readInt() != 0) {
                            z = true;
                        }
                        onInterfaceChanged(readString, z);
                        return true;
                    case 9:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        String readString2 = parcel.readString();
                        if (parcel.readInt() != 0) {
                            z = true;
                        }
                        onInterfaceLinkStateChanged(readString2, z);
                        return true;
                    case 10:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        if (parcel.readInt() != 0) {
                            z = true;
                        }
                        onRouteChanged(z, parcel.readString(), parcel.readString(), parcel.readString());
                        return true;
                    case 11:
                        parcel.enforceInterface("android.net.INetdUnsolicitedEventListener");
                        onStrictCleartextDetected(parcel.readInt(), parcel.readString());
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("android.net.INetdUnsolicitedEventListener");
                return true;
            }
        }
    }
}
