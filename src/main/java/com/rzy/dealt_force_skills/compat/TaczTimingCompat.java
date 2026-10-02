package com.rzy.dealt_force_skills.compat;

public final class TaczTimingCompat {
    private TaczTimingCompat() {
    }

    public static long accelerateRemainingCooldown(long remaining, long elapsed, double multiplier,
                                                   long nativeAdjustment) {
        if (remaining <= 0L || elapsed < 0L || !Double.isFinite(multiplier) || multiplier <= 1.0001D) {
            return remaining;
        }
        double interval = Math.max(0.0D, (double) remaining + elapsed + nativeAdjustment);
        double acceleratedInterval = Math.ceil(interval / multiplier);
        return Math.max(0L, Math.round(acceleratedInterval - elapsed));
    }

    public static long scaleElapsedTimestamp(long timestamp, long now, double multiplier) {
        if (timestamp < 0L || now < timestamp || !Double.isFinite(multiplier) || multiplier <= 0.0D) {
            return timestamp;
        }
        double scaledElapsed = Math.min(Long.MAX_VALUE / 4.0D, (now - timestamp) * multiplier);
        return now - Math.max(0L, Math.round(scaledElapsed));
    }
}
