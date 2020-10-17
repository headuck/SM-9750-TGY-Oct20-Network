package android.net.netlink;

import android.net.util.SocketUtils;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NetlinkSocket {
    public static void sendOneShotKernelMessage(int i, byte[] bArr) throws ErrnoException {
        String str;
        FileDescriptor forProto = forProto(i);
        try {
            connectToKernel(forProto);
            sendMessage(forProto, bArr, 0, bArr.length, 300);
            ByteBuffer recvMessage = recvMessage(forProto, 8192, 300);
            NetlinkMessage parse = NetlinkMessage.parse(recvMessage);
            if (parse == null || !(parse instanceof NetlinkErrorMessage) || ((NetlinkErrorMessage) parse).getNlMsgError() == null) {
                if (parse == null) {
                    recvMessage.position(0);
                    str = "raw bytes: " + NetlinkConstants.hexify(recvMessage);
                } else {
                    str = parse.toString();
                }
                Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage, errmsg=" + str);
                throw new ErrnoException(str, OsConstants.EPROTO);
            }
            int i2 = ((NetlinkErrorMessage) parse).getNlMsgError().error;
            if (i2 == 0) {
                try {
                    SocketUtils.closeSocket(forProto);
                } catch (IOException unused) {
                }
            } else {
                Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage, errmsg=" + parse.toString());
                throw new ErrnoException(parse.toString(), Math.abs(i2));
            }
        } catch (InterruptedIOException e) {
            Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage", e);
            throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.ETIMEDOUT, e);
        } catch (SocketException e2) {
            Log.e("NetlinkSocket", "Error in NetlinkSocket.sendOneShotKernelMessage", e2);
            throw new ErrnoException("Error in NetlinkSocket.sendOneShotKernelMessage", OsConstants.EIO, e2);
        } catch (Throwable th) {
            try {
                SocketUtils.closeSocket(forProto);
            } catch (IOException unused2) {
            }
            throw th;
        }
    }

    public static FileDescriptor forProto(int i) throws ErrnoException {
        FileDescriptor socket = Os.socket(OsConstants.AF_NETLINK, OsConstants.SOCK_DGRAM, i);
        Os.setsockoptInt(socket, OsConstants.SOL_SOCKET, OsConstants.SO_RCVBUF, 65536);
        return socket;
    }

    public static void connectToKernel(FileDescriptor fileDescriptor) throws ErrnoException, SocketException {
        Os.connect(fileDescriptor, SocketUtils.makeNetlinkSocketAddress(0, 0));
    }

    private static void checkTimeout(long j) {
        if (j < 0) {
            throw new IllegalArgumentException("Negative timeouts not permitted");
        }
    }

    public static ByteBuffer recvMessage(FileDescriptor fileDescriptor, int i, long j) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(j);
        Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(j));
        ByteBuffer allocate = ByteBuffer.allocate(i);
        int read = Os.read(fileDescriptor, allocate);
        if (read == i) {
            Log.w("NetlinkSocket", "maximum read");
        }
        allocate.position(0);
        allocate.limit(read);
        allocate.order(ByteOrder.nativeOrder());
        return allocate;
    }

    public static int sendMessage(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, long j) throws ErrnoException, IllegalArgumentException, InterruptedIOException {
        checkTimeout(j);
        Os.setsockoptTimeval(fileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(j));
        return Os.write(fileDescriptor, bArr, i, i2);
    }
}
