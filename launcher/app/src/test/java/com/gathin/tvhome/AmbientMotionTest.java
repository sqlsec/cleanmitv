package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AmbientMotionTest {
    @Test public void ambientLoopIsCappedAtTwentyFramesPerSecond() {
        assertTrue(AmbientMotion.FRAME_MS >= 50);
    }

    @Test public void phaseResumesWhereItPausedAndWrapsWithoutAJump() {
        assertEquals(0.25f, AmbientMotion.phase(0.25f, 0), 0.00001f);
        assertEquals(0.75f, AmbientMotion.phase(0.25f, AmbientMotion.PERIOD_MS / 2), 0.00001f);
        assertEquals(0.25f, AmbientMotion.phase(0.25f, AmbientMotion.PERIOD_MS), 0.00001f);
    }

    @Test public void negativeElapsedTimeCannotRunTheBackgroundBackwards() {
        assertEquals(0.6f, AmbientMotion.phase(0.6f, -100), 0.00001f);
    }
}
