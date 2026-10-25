package com.gathin.tvhome;

/** Pure policy for the local TV's explicit day/night/automatic choice. */
final class AppearancePolicy {
    private AppearancePolicy() { }

    static boolean isDay(String mode, int hour) {
        if ("day".equals(mode)) return true;
        if ("auto".equals(mode)) return hour >= 7 && hour < 19;
        return false;
    }

    static String label(String mode) {
        if ("day".equals(mode)) return "日间";
        if ("auto".equals(mode)) return "自动（07:00–19:00 为日间）";
        return "夜间";
    }
}
