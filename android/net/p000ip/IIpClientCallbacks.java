package android.net.p000ip;

import android.net.DhcpResultsParcelable;
import android.net.LinkProperties;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* renamed from: android.net.ip.IIpClientCallbacks */
public interface IIpClientCallbacks extends IInterface {
    int getInterfaceVersion() throws RemoteException;

    void installPacketFilter(byte[] bArr) throws RemoteException;

    void onIpClientCreated(IIpClient iIpClient) throws RemoteException;

    void onLinkPropertiesChange(LinkProperties linkProperties) throws RemoteException;

    void onNewDhcpResults(DhcpResultsParcelable dhcpResultsParcelable) throws RemoteException;

    void onPostDhcpAction() throws RemoteException;

    void onPreDhcpAction() throws RemoteException;

    void onProvisioningFailure(LinkProperties linkProperties) throws RemoteException;

    void onProvisioningSuccess(LinkProperties linkProperties) throws RemoteException;

    void onQuit() throws RemoteException;

    void onReachabilityLost(String str) throws RemoteException;

    void setFallbackMulticastFilter(boolean z) throws RemoteException;

    void setNeighborDiscoveryOffload(boolean z) throws RemoteException;

    void startReadPacketFilter() throws RemoteException;

    /* renamed from: android.net.ip.IIpClientCallbacks$Stub */
    public static abstract class Stub extends Binder implements IIpClientCallbacks {
        public static IIpClientCallbacks asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("android.net.ip.IIpClientCallbacks");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof IIpClientCallbacks)) {
                return new Proxy(iBinder);
            }
            return (IIpClientCallbacks) queryLocalInterface;
        }

        /* access modifiers changed from: private */
        /* renamed from: android.net.ip.IIpClientCallbacks$Stub$Proxy */
        public static class Proxy implements IIpClientCallbacks {
            public static IIpClientCallbacks sDefaultImpl;
            private int mCachedVersion = -1;
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onIpClientCreated(IIpClient iIpClient) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    obtain.writeStrongBinder(iIpClient != null ? iIpClient.asBinder() : null);
                    if (this.mRemote.transact(1, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onIpClientCreated(iIpClient);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onPreDhcpAction() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (this.mRemote.transact(2, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onPreDhcpAction();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onPostDhcpAction() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (this.mRemote.transact(3, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onPostDhcpAction();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onNewDhcpResults(DhcpResultsParcelable dhcpResultsParcelable) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (dhcpResultsParcelable != null) {
                        obtain.writeInt(1);
                        dhcpResultsParcelable.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(4, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onNewDhcpResults(dhcpResultsParcelable);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onProvisioningSuccess(LinkProperties linkProperties) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (linkProperties != null) {
                        obtain.writeInt(1);
                        linkProperties.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(5, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onProvisioningSuccess(linkProperties);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onProvisioningFailure(LinkProperties linkProperties) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (linkProperties != null) {
                        obtain.writeInt(1);
                        linkProperties.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(6, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onProvisioningFailure(linkProperties);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onLinkPropertiesChange(LinkProperties linkProperties) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (linkProperties != null) {
                        obtain.writeInt(1);
                        linkProperties.writeToParcel(obtain, 0);
                    } else {
                        obtain.writeInt(0);
                    }
                    if (this.mRemote.transact(7, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onLinkPropertiesChange(linkProperties);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onReachabilityLost(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    obtain.writeString(str);
                    if (this.mRemote.transact(8, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onReachabilityLost(str);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void onQuit() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (this.mRemote.transact(9, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().onQuit();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void installPacketFilter(byte[] bArr) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    obtain.writeByteArray(bArr);
                    if (this.mRemote.transact(10, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().installPacketFilter(bArr);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void startReadPacketFilter() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    if (this.mRemote.transact(11, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().startReadPacketFilter();
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void setFallbackMulticastFilter(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    obtain.writeInt(z ? 1 : 0);
                    if (this.mRemote.transact(12, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().setFallbackMulticastFilter(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public void setNeighborDiscoveryOffload(boolean z) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
                    obtain.writeInt(z ? 1 : 0);
                    if (this.mRemote.transact(13, obtain, null, 1) || Stub.getDefaultImpl() == null) {
                        obtain.recycle();
                    } else {
                        Stub.getDefaultImpl().setNeighborDiscoveryOffload(z);
                    }
                } finally {
                    obtain.recycle();
                }
            }

            @Override // android.net.p000ip.IIpClientCallbacks
            public int getInterfaceVersion() throws RemoteException {
                if (this.mCachedVersion == -1) {
                    Parcel obtain = Parcel.obtain();
                    Parcel obtain2 = Parcel.obtain();
                    try {
                        obtain.writeInterfaceToken("android.net.ip.IIpClientCallbacks");
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

        public static IIpClientCallbacks getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }
}
