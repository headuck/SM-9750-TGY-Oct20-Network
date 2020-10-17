package com.android.networkstack.metrics;

import android.net.captiveportal.CaptivePortalProbeResult;

public class DataStallStatsUtils {
    private static int probeResultToEnum(CaptivePortalProbeResult captivePortalProbeResult) {
        if (captivePortalProbeResult == null) {
            return 2;
        }
        if (captivePortalProbeResult.isSuccessful()) {
            return 1;
        }
        if (captivePortalProbeResult.isPortal()) {
            return 3;
        }
        if (captivePortalProbeResult.isPartialConnectivity()) {
            return 4;
        }
        return 2;
    }

    public static void write(DataStallDetectionStats dataStallDetectionStats, CaptivePortalProbeResult captivePortalProbeResult) {
        NetworkStackStatsLog.write(121, dataStallDetectionStats.mEvaluationType, probeResultToEnum(captivePortalProbeResult), dataStallDetectionStats.mNetworkType, dataStallDetectionStats.mWifiInfo, dataStallDetectionStats.mCellularInfo, dataStallDetectionStats.mDns);
    }
}
