package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppearancePolicyTest {
    @Test public void automaticChangesOnlyAtSevenAndNineteen() {
        assertFalse(AppearancePolicy.isDay("auto", 6));
        assertTrue(AppearancePolicy.isDay("auto", 7));
        assertTrue(AppearancePolicy.isDay("auto", 18));
        assertFalse(AppearancePolicy.isDay("auto", 19));
        assertFalse(AppearancePolicy.isDay("auto", 0));
    }
    @Test public void explicitChoicesOverrideTime() {
        assertTrue(AppearancePolicy.isDay("day", 0));
        assertFalse(AppearancePolicy.isDay("night", 12));
    }
    @Test public void damagedOrUnknownPreferenceFallsBackToNight() {
        assertFalse(AppearancePolicy.isDay(null, 12));
        assertFalse(AppearancePolicy.isDay("invalid", 12));
        assertEquals("夜间", AppearancePolicy.label("invalid"));
    }
}
