package android.net.networkstack.util;

public class HexDump {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final char[] HEX_LOWER_CASE_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static byte[] toByteArray(int i) {
        byte[] bArr = new byte[4];
        bArr[3] = (byte) (i & 255);
        bArr[2] = (byte) ((i >> 8) & 255);
        bArr[1] = (byte) ((i >> 16) & 255);
        bArr[0] = (byte) ((i >> 24) & 255);
        return bArr;
    }

    public static String dumpHexString(byte[] bArr) {
        return bArr == null ? "(null)" : dumpHexString(bArr, 0, bArr.length);
    }

    public static String dumpHexString(byte[] bArr, int i, int i2) {
        if (bArr == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder();
        byte[] bArr2 = new byte[16];
        sb.append("\n0x");
        sb.append(toHexString(i));
        int i3 = i;
        int i4 = 0;
        while (i3 < i + i2) {
            if (i4 == 16) {
                sb.append(" ");
                for (int i5 = 0; i5 < 16; i5++) {
                    if (bArr2[i5] <= 32 || bArr2[i5] >= 126) {
                        sb.append(".");
                    } else {
                        sb.append(new String(bArr2, i5, 1));
                    }
                }
                sb.append("\n0x");
                sb.append(toHexString(i3));
                i4 = 0;
            }
            byte b = bArr[i3];
            sb.append(" ");
            sb.append(HEX_DIGITS[(b >>> 4) & 15]);
            sb.append(HEX_DIGITS[b & 15]);
            bArr2[i4] = b;
            i3++;
            i4++;
        }
        if (i4 != 16) {
            int i6 = ((16 - i4) * 3) + 1;
            for (int i7 = 0; i7 < i6; i7++) {
                sb.append(" ");
            }
            for (int i8 = 0; i8 < i4; i8++) {
                if (bArr2[i8] <= 32 || bArr2[i8] >= 126) {
                    sb.append(".");
                } else {
                    sb.append(new String(bArr2, i8, 1));
                }
            }
        }
        return sb.toString();
    }

    public static String toHexString(byte[] bArr) {
        return toHexString(bArr, 0, bArr.length, true);
    }

    public static String toHexString(byte[] bArr, boolean z) {
        return toHexString(bArr, 0, bArr.length, z);
    }

    public static String toHexString(byte[] bArr, int i, int i2) {
        return toHexString(bArr, i, i2, true);
    }

    public static String toHexString(byte[] bArr, int i, int i2, boolean z) {
        char[] cArr = z ? HEX_DIGITS : HEX_LOWER_CASE_DIGITS;
        char[] cArr2 = new char[(i2 * 2)];
        int i3 = 0;
        for (int i4 = i; i4 < i + i2; i4++) {
            byte b = bArr[i4];
            int i5 = i3 + 1;
            cArr2[i3] = cArr[(b >>> 4) & 15];
            i3 = i5 + 1;
            cArr2[i5] = cArr[b & 15];
        }
        return new String(cArr2);
    }

    public static String toHexString(int i) {
        return toHexString(toByteArray(i));
    }
}
