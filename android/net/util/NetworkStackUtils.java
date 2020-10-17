package android.net.util;

import android.net.MacAddress;
import android.provider.DeviceConfig;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.function.Predicate;

public class NetworkStackUtils {
    private static native void addArpEntry(byte[] bArr, byte[] bArr2, String str, FileDescriptor fileDescriptor) throws IOException;

    public static native void attachControlPacketFilter(FileDescriptor fileDescriptor, int i) throws SocketException;

    public static native void attachDhcpFilter(FileDescriptor fileDescriptor) throws SocketException;

    public static native void attachRaFilter(FileDescriptor fileDescriptor, int i) throws SocketException;

    static {
        System.loadLibrary("networkstackutilsjni");
    }

    public static <T> boolean isEmpty(T[] tArr) {
        return tArr == null || tArr.length == 0;
    }

    public static void closeSocketQuietly(FileDescriptor fileDescriptor) {
        try {
            SocketUtils.closeSocket(fileDescriptor);
        } catch (IOException unused) {
        }
    }

    public static int[] convertToIntArray(List<Integer> list) {
        int[] iArr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            iArr[i] = list.get(i).intValue();
        }
        return iArr;
    }

    public static long[] convertToLongArray(List<Long> list) {
        long[] jArr = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            jArr[i] = list.get(i).longValue();
        }
        return jArr;
    }

    public static <T> boolean any(SparseArray<T> sparseArray, Predicate<T> predicate) {
        for (int i = 0; i < sparseArray.size(); i++) {
            if (predicate.test(sparseArray.valueAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String getDeviceConfigProperty(String str, String str2, String str3) {
        String property = DeviceConfig.getProperty(str, str2);
        return property != null ? property : str3;
    }

    public static int getDeviceConfigPropertyInt(String str, String str2, int i) {
        String deviceConfigProperty = getDeviceConfigProperty(str, str2, null);
        if (deviceConfigProperty == null) {
            return i;
        }
        try {
            return Integer.parseInt(deviceConfigProperty);
        } catch (NumberFormatException unused) {
            return i;
        }
    }

    public static void addArpEntry(Inet4Address inet4Address, MacAddress macAddress, String str, FileDescriptor fileDescriptor) throws IOException {
        addArpEntry(macAddress.toByteArray(), inet4Address.getAddress(), str, fileDescriptor);
    }

    public static String addressAndPortToString(InetAddress inetAddress, int i) {
        return String.format(inetAddress instanceof Inet6Address ? "[%s]:%d" : "%s:%d", inetAddress.getHostAddress(), Integer.valueOf(i));
    }
}
