package com.gathin.tvhome;

/** Timing policy for the slow background drift, kept pure so wrapping and the frame budget stay tested. */
final class AmbientMotion {
    static final long PERIOD_MS = 47000;
    static final long FRAME_MS = 50; // 20 fps; the fastest bloom moves less than a pixel per frame at 1080p.

    private AmbientMotion() { }

    static float phase(float start, long elapsedMs) {
        float value = start + Math.max(0, elapsedMs) / (float) PERIOD_MS;
        return value - (float) Math.floor(value);
    }
}
