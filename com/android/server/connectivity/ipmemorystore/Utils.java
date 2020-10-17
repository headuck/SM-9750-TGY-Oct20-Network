package com.android.server.connectivity.ipmemorystore;

public class Utils {
    public static String byteArrayToString(byte[] bArr) {
        if (bArr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("[");
        if (bArr.length <= 24) {
            appendByteArray(sb, bArr, 0, bArr.length);
        } else {
            appendByteArray(sb, bArr, 0, 16);
            sb.append("...");
            appendByteArray(sb, bArr, bArr.length - 8, bArr.length);
        }
        sb.append("]");
        return sb.toString();
    }

    private static void appendByteArray(StringBuilder sb, byte[] bArr, int i, int i2) {
        while (i < i2) {
            sb.append(String.format("%02X", Byte.valueOf(bArr[i])));
            i++;
        }
    }
}
