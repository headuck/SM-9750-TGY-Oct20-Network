package com.android.server.connectivity.ipmemorystore;

public class RelevanceUtils {
    public static final int CAPPED_RELEVANCE = 1000000;
    public static final long CAPPED_RELEVANCE_LIFETIME_MS = 34560000000L;
    private static final double IRRELEVANCE_FLOOR = ((powF(2.0d) * 1000000.0d) / (1.0d - powF(2.0d)));
    private static final double LOG_DECAY_FACTOR = Math.log(0.5d);
    public static final int RELEVANCE_BUMP = 40000;

    private static double logF(double d) {
        return Math.log(d) / LOG_DECAY_FACTOR;
    }

    private static double powF(double d) {
        return Math.pow(0.5d, d);
    }

    public static long bumpExpiryDuration(long j) {
        double powF = powF(((double) j) / 1.728E10d);
        double d = IRRELEVANCE_FLOOR;
        return Math.min((long) (logF(d / ((d / powF) + 40000.0d)) * 1.728E10d), (long) CAPPED_RELEVANCE_LIFETIME_MS);
    }

    public static long bumpExpiryDate(long j) {
        long currentTimeMillis = System.currentTimeMillis();
        return currentTimeMillis + bumpExpiryDuration(j - currentTimeMillis);
    }
}
