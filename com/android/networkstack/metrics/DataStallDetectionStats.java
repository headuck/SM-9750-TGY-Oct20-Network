package com.android.networkstack.metrics;

import android.net.networkstack.util.HexDump;
import android.net.util.NetworkStackUtils;
import android.net.wifi.WifiInfo;
import com.android.server.connectivity.nano.CellularData;
import com.android.server.connectivity.nano.DnsEvent;
import com.android.server.connectivity.nano.WifiData;
import com.google.protobuf.nano.MessageNano;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class DataStallDetectionStats {
    final byte[] mCellularInfo;
    final byte[] mDns;
    final int mEvaluationType;
    final int mNetworkType;
    final byte[] mWifiInfo;

    public DataStallDetectionStats(byte[] bArr, byte[] bArr2, int[] iArr, long[] jArr, int i, int i2) {
        this.mCellularInfo = emptyCellDataIfNull(bArr);
        this.mWifiInfo = emptyWifiInfoIfNull(bArr2);
        DnsEvent dnsEvent = new DnsEvent();
        dnsEvent.dnsReturnCode = iArr;
        dnsEvent.dnsTime = jArr;
        this.mDns = MessageNano.toByteArray(dnsEvent);
        this.mEvaluationType = i;
        this.mNetworkType = i2;
    }

    private byte[] emptyCellDataIfNull(byte[] bArr) {
        if (bArr != null) {
            return bArr;
        }
        CellularData cellularData = new CellularData();
        cellularData.ratType = 0;
        cellularData.networkMccmnc = "";
        cellularData.simMccmnc = "";
        cellularData.signalStrength = -1;
        return MessageNano.toByteArray(cellularData);
    }

    private byte[] emptyWifiInfoIfNull(byte[] bArr) {
        if (bArr != null) {
            return bArr;
        }
        WifiData wifiData = new WifiData();
        wifiData.wifiBand = 0;
        wifiData.signalStrength = -1;
        return MessageNano.toByteArray(wifiData);
    }

    public String toString() {
        return "type: " + this.mNetworkType + ", evaluation type: " + this.mEvaluationType + ", wifi info: " + HexDump.toHexString(this.mWifiInfo) + ", cell info: " + HexDump.toHexString(this.mCellularInfo) + ", dns: " + HexDump.toHexString(this.mDns);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DataStallDetectionStats)) {
            return false;
        }
        DataStallDetectionStats dataStallDetectionStats = (DataStallDetectionStats) obj;
        if (this.mNetworkType != dataStallDetectionStats.mNetworkType || this.mEvaluationType != dataStallDetectionStats.mEvaluationType || !Arrays.equals(this.mWifiInfo, dataStallDetectionStats.mWifiInfo) || !Arrays.equals(this.mCellularInfo, dataStallDetectionStats.mCellularInfo) || !Arrays.equals(this.mDns, dataStallDetectionStats.mDns)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mNetworkType), Integer.valueOf(this.mEvaluationType), this.mWifiInfo, this.mCellularInfo, this.mDns);
    }

    public static class Builder {
        private byte[] mCellularInfo;
        private final List<Integer> mDnsReturnCode = new ArrayList();
        private final List<Long> mDnsTimeStamp = new ArrayList();
        private int mEvaluationType;
        private int mNetworkType;
        private byte[] mWifiInfo;

        public Builder addDnsEvent(int i, long j) {
            this.mDnsReturnCode.add(Integer.valueOf(i));
            this.mDnsTimeStamp.add(Long.valueOf(j));
            return this;
        }

        public Builder setEvaluationType(int i) {
            this.mEvaluationType = i;
            return this;
        }

        public Builder setNetworkType(int i) {
            this.mNetworkType = i;
            return this;
        }

        public Builder setWiFiData(WifiInfo wifiInfo) {
            WifiData wifiData = new WifiData();
            wifiData.wifiBand = getWifiBand(wifiInfo);
            wifiData.signalStrength = wifiInfo != null ? wifiInfo.getRssi() : -1;
            this.mWifiInfo = MessageNano.toByteArray(wifiData);
            return this;
        }

        private static int getWifiBand(WifiInfo wifiInfo) {
            if (wifiInfo == null) {
                return 0;
            }
            int frequency = wifiInfo.getFrequency();
            if (frequency <= 4900 || frequency >= 5900) {
                return (frequency <= 2400 || frequency >= 2500) ? 0 : 1;
            }
            return 2;
        }

        public Builder setCellData(int i, boolean z, String str, String str2, int i2) {
            CellularData cellularData = new CellularData();
            cellularData.ratType = i;
            cellularData.isRoaming = z;
            cellularData.networkMccmnc = str;
            cellularData.simMccmnc = str2;
            cellularData.signalStrength = i2;
            this.mCellularInfo = MessageNano.toByteArray(cellularData);
            return this;
        }

        public DataStallDetectionStats build() {
            return new DataStallDetectionStats(this.mCellularInfo, this.mWifiInfo, NetworkStackUtils.convertToIntArray(this.mDnsReturnCode), NetworkStackUtils.convertToLongArray(this.mDnsTimeStamp), this.mEvaluationType, this.mNetworkType);
        }
    }
}
