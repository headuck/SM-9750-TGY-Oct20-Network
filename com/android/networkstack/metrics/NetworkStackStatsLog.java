package com.android.networkstack.metrics;

import android.os.SystemClock;
import android.util.StatsLog;

public class NetworkStackStatsLog {
    public static void write(int i, int i2, int i3, int i4, byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        int length = bArr.length + 5 + 31;
        if (bArr2 == null) {
            bArr2 = new byte[0];
        }
        int length2 = length + bArr2.length + 5;
        if (bArr3 == null) {
            bArr3 = new byte[0];
        }
        int length3 = length2 + bArr3.length + 5;
        if (length3 <= 4064) {
            byte[] bArr4 = new byte[length3];
            bArr4[0] = 3;
            bArr4[1] = 8;
            long elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos();
            bArr4[2] = 1;
            copyLong(bArr4, 3, elapsedRealtimeNanos);
            bArr4[11] = 0;
            copyInt(bArr4, 12, i);
            bArr4[16] = 0;
            copyInt(bArr4, 17, i2);
            bArr4[21] = 0;
            copyInt(bArr4, 22, i3);
            bArr4[26] = 0;
            copyInt(bArr4, 27, i4);
            bArr4[31] = 2;
            copyInt(bArr4, 32, bArr.length);
            System.arraycopy(bArr, 0, bArr4, 36, bArr.length);
            int length4 = 31 + bArr.length + 5;
            bArr4[length4] = 2;
            copyInt(bArr4, length4 + 1, bArr2.length);
            System.arraycopy(bArr2, 0, bArr4, length4 + 5, bArr2.length);
            int length5 = length4 + bArr2.length + 5;
            bArr4[length5] = 2;
            copyInt(bArr4, length5 + 1, bArr3.length);
            System.arraycopy(bArr3, 0, bArr4, length5 + 5, bArr3.length);
            StatsLog.writeRaw(bArr4, length5 + bArr3.length + 5);
        }
    }

    private static void copyInt(byte[] bArr, int i, int i2) {
        bArr[i] = (byte) i2;
        bArr[i + 1] = (byte) (i2 >> 8);
        bArr[i + 2] = (byte) (i2 >> 16);
        bArr[i + 3] = (byte) (i2 >> 24);
    }

    private static void copyLong(byte[] bArr, int i, long j) {
        bArr[i] = (byte) ((int) j);
        bArr[i + 1] = (byte) ((int) (j >> 8));
        bArr[i + 2] = (byte) ((int) (j >> 16));
        bArr[i + 3] = (byte) ((int) (j >> 24));
        bArr[i + 4] = (byte) ((int) (j >> 32));
        bArr[i + 5] = (byte) ((int) (j >> 40));
        bArr[i + 6] = (byte) ((int) (j >> 48));
        bArr[i + 7] = (byte) ((int) (j >> 56));
    }
}
