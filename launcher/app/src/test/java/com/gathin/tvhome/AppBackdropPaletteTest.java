package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AppBackdropPaletteTest {
    @Test public void smallRedCornerBadgeDoesNotReplaceBlueBrandColour() {
        int[] pixels = new int[24 * 24];
        Arrays.fill(pixels, 0xFF2288DD);
        for (int y = 0; y < 4; y++) for (int x = 20; x < 24; x++) pixels[y * 24 + x] = 0xFFFF0000;
        int result = AppBackdropPalette.dominant(pixels, 24, 24);
        assertTrue((result & 255) > ((result >>> 16) & 255) * 2);
    }
    @Test public void mostlyWhiteCardWithSmallGreenLogoUsesItsVisibleWhitePlate() {
        int[] pixels = new int[400];
        Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 7; y < 13; y++) for (int x = 7; x < 13; x++) pixels[y * 20 + x] = 0xFF13C584;
        int result = AppBackdropPalette.dominant(pixels, 20, 20);
        assertEquals(0xFFFFFFFF, result);
    }
    @Test public void substantialGreenArtworkStillUsesItsBrandColour() {
        int[] pixels = new int[400];
        Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 3; y < 17; y++) for (int x = 3; x < 17; x++) pixels[y * 20 + x] = 0xFF13C584;
        int result = AppBackdropPalette.dominant(pixels, 20, 20);
        assertTrue(((result >>> 8) & 255) > ((result >>> 16) & 255) * 2);
    }
    @Test public void transparentArtworkUsesNeutralFallbackAndWhiteArtworkStaysWhite() {
        assertEquals(AppBackdropPalette.NEUTRAL, AppBackdropPalette.dominant(new int[64], 8, 8));
        int[] white = new int[64]; Arrays.fill(white, 0xFFFFFFFF);
        assertEquals(0xFFFFFFFF, AppBackdropPalette.dominant(white, 8, 8));
    }
    @Test public void neutralIconsHaveNoBlueTintInEitherAppearance() {
        for (int colour : new int[]{0xFFFFFFFF, 0xFFAAAAAA, 0xFF444444, AppBackdropPalette.NEUTRAL}) {
            for (boolean day : new boolean[]{true, false}) {
                for (int actual : new int[]{AppBackdropPalette.base(colour, day), AppBackdropPalette.accent(colour, day)}) {
                    assertEquals((actual >>> 16) & 255, (actual >>> 8) & 255);
                    assertEquals((actual >>> 8) & 255, actual & 255);
                }
            }
        }
    }
    @Test public void blackLogoOnWhitePlateKeepsTheNeutralPlate() {
        int[] pixels = new int[400]; Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 7; y < 13; y++) for (int x = 7; x < 13; x++) pixels[y * 20 + x] = 0xFF101010;
        assertEquals(0xFFFFFFFF, AppBackdropPalette.dominant(pixels, 20, 20));
    }
    @Test public void lowSaturationWarmArtworkKeepsItsWarmth() {
        int[] pixels = new int[64]; Arrays.fill(pixels, 0xFFE9DDCE);
        int colour = AppBackdropPalette.dominant(pixels, 8, 8);
        assertEquals(0xFFE9DDCE, colour);
        int actual = AppBackdropPalette.accent(colour, false);
        assertTrue(((actual >>> 16) & 255) > (actual & 255));
    }
    @Test public void retargetingContinuesFromDisplayedColour() {
        int displayed = AppBackdropPalette.blend(0xFF2288DD, 0xFFFF6699, 0.4f);
        assertEquals(displayed, AppBackdropPalette.blend(displayed, 0xFF00CC99, 0f));
        assertEquals(0xFF00CC99, AppBackdropPalette.blend(displayed, 0xFF00CC99, 1f));
    }
    @Test public void interpolationIsOpaqueAndClampsAtTheEndpoints() {
        assertEquals(0xFF2288DD, AppBackdropPalette.blend(0xFF2288DD, 0xFFFF6699, -1));
        assertEquals(0xFFFF6699, AppBackdropPalette.blend(0xFF2288DD, 0xFFFF6699, 2));
        assertEquals(255, AppBackdropPalette.base(0xFF00FF00, false) >>> 24);
    }
    @Test public void roomColourSitsHalfwayBetweenIconAndFill() {
        int mixed = AppBackdropPalette.blend(0xFF2076D2, 0xFF101820, 0.5f);
        assertEquals(0x18, (mixed >>> 16) & 255);
        assertEquals(0x47, (mixed >>> 8) & 255);
        assertEquals(0x79, mixed & 255);
        assertEquals(255, (mixed >>> 24) & 255);
    }
    @Test public void multiHueArtworkYieldsDistinctPalette() {
        // A mark that is blue on one side and red on the other contributes both hues, weightiest first.
        int w = 20, h = 10;
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            pixels[y * w + x] = x < 12 ? 0xFF2288DD : 0xFFDD3322;
        int[] palette = AppBackdropPalette.palette(pixels, w, h, 3);
        assertEquals(2, palette.length);
        assertEquals(0xFF2288DD, palette[0]);
        assertEquals(0xFFDD3322, palette[1]);
    }
    @Test public void monochromeArtworkYieldsASingleColour() {
        int[] pixels = new int[200];
        java.util.Arrays.fill(pixels, 0xFF2288DD);
        int[] palette = AppBackdropPalette.palette(pixels, 20, 10, 3);
        assertEquals(1, palette.length);
        assertEquals(0xFF2288DD, palette[0]);
    }
    @Test public void mostlyNeutralArtworkStillYieldsOnePlateColour() {
        int[] pixels = new int[400];
        java.util.Arrays.fill(pixels, 0xFFFFFFFF);
        for (int y = 7; y < 13; y++) for (int x = 7; x < 13; x++) pixels[y * 20 + x] = 0xFF13C584;
        int[] palette = AppBackdropPalette.palette(pixels, 20, 20, 3);
        assertEquals(1, palette.length);
        assertEquals(0xFFFFFFFF, palette[0]);
    }
    @Test public void dominantRemainsTheWeightiestPaletteEntry() {
        int w = 20, h = 10;
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            pixels[y * w + x] = x < 12 ? 0xFF2288DD : 0xFFDD3322;
        assertEquals(AppBackdropPalette.palette(pixels, w, h, 3)[0], AppBackdropPalette.dominant(pixels, w, h));
    }
    @Test public void roomColourKeepsBothParentsPresent() {
        // Neither parent wins: a bright icon over a near-black fill lands clearly between the two.
        int mixed = AppBackdropPalette.blend(0xFFE0B040, 0xFF101418, 0.5f);
        int red = (mixed >>> 16) & 255, green = (mixed >>> 8) & 255, blue = mixed & 255;
        assertTrue(red > 0x10 && red < 0xE0);
        assertTrue(green > 0x10 && green < 0xB0);
        assertTrue(blue > 0x14 && blue < 0x40);
    }
}
