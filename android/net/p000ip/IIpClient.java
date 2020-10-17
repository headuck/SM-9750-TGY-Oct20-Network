package android.net.p000ip;

import android.net.NattKeepalivePacketDataParcelable;
import android.net.ProvisioningConfigurationParcelable;
import android.net.ProxyInfo;
import android.net.TcpKeepalivePacketDataParcelable;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* renamed from: android.net.ip.IIpClient */
public interface IIpClient extends IInterface {
    void addKeepalivePacketFilter(int i, TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable) throws RemoteException;

    void addNattKeepalivePacketFilter(int i, NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable) throws RemoteException;

    void completedPreDhcpAction() throws RemoteException;

    void confirmConfiguration() throws RemoteException;

    int getInterfaceVersion() throws RemoteException;

    void readPacketFilterComplete(byte[] bArr) throws RemoteException;

    void removeKeepalivePacketFilter(int i) throws RemoteException;

    void sendDhcpReleasePacket() throws RemoteException;

    void setHttpProxy(ProxyInfo proxyInfo) throws RemoteException;

    void setL2KeyAndGroupHint(String str, String str2) throws RemoteException;

    void setMulticastFilter(boolean z) throws RemoteException;

    void setTcpBufferSizes(String str) throws RemoteException;

    void shutdown() throws RemoteException;

    void startProvisioning(ProvisioningConfigurationParcelable provisioningConfigurationParcelable) throws RemoteException;

    void stop() throws RemoteException;

    /* renamed from: android.net.ip.IIpClient$Stub */
    public static abstract class Stub extends Binder implements IIpClient {
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "android.net.ip.IIpClient");
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 16777215) {
                parcel.enforceInterface("android.net.ip.IIpClient");
                parcel2.writeNoException();
                parcel2.writeInt(getInterfaceVersion());
                return true;
            } else if (i != 1598968902) {
                ProvisioningConfigurationParcelable provisioningConfigurationParcelable = null;
                NattKeepalivePacketDataParcelable nattKeepalivePacketDataParcelable = null;
                TcpKeepalivePacketDataParcelable tcpKeepalivePacketDataParcelable = null;
                ProxyInfo proxyInfo = null;
                switch (i) {
                    case 1:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        completedPreDhcpAction();
                        return true;
                    case 2:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        confirmConfiguration();
                        return true;
                    case 3:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        readPacketFilterComplete(parcel.createByteArray());
                        return true;
                    case 4:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        shutdown();
                        return true;
                    case 5:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        if (parcel.readInt() != 0) {
                            provisioningConfigurationParcelable = ProvisioningConfigurationParcelable.CREATOR.createFromParcel(parcel);
                        }
                        startProvisioning(provisioningConfigurationParcelable);
                        return true;
                    case 6:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        stop();
                        return true;
                    case 7:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        setTcpBufferSizes(parcel.readString());
                        return true;
                    case 8:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        if (parcel.readInt() != 0) {
                            proxyInfo = (ProxyInfo) ProxyInfo.CREATOR.createFromParcel(parcel);
                        }
                        setHttpProxy(proxyInfo);
                        return true;
                    case 9:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        setMulticastFilter(parcel.readInt() != 0);
                        return true;
                    case 10:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        int readInt = parcel.readInt();
                        if (parcel.readInt() != 0) {
                            tcpKeepalivePacketDataParcelable = TcpKeepalivePacketDataParcelable.CREATOR.createFromParcel(parcel);
                        }
                        addKeepalivePacketFilter(readInt, tcpKeepalivePacketDataParcelable);
                        return true;
                    case 11:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        removeKeepalivePacketFilter(parcel.readInt());
                        return true;
                    case 12:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        sendDhcpReleasePacket();
                        return true;
                    case 13:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        setL2KeyAndGroupHint(parcel.readString(), parcel.readString());
                        return true;
                    case 14:
                        parcel.enforceInterface("android.net.ip.IIpClient");
                        int readInt2 = parcel.readInt();
                        if (parcel.readInt() != 0) {
                            nattKeepalivePacketDataParcelable = NattKeepalivePacketDataParcelable.CREATOR.createFromParcel(parcel);
                        }
                        addNattKeepalivePacketFilter(readInt2, nattKeepalivePacketDataParcelable);
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } else {
                parcel2.writeString("android.net.ip.IIpClient");
                return true;
            }
        }
    }
}
