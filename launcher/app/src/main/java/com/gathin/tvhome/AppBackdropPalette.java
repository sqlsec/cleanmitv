package com.gathin.tvhome;

/** Pure colour policy: follow the visible card's dominant plate, ignoring small corner badges. */
final class AppBackdropPalette {
    static final int NEUTRAL = 0xFFB2B2B2;

    private AppBackdropPalette() { }

    static int dominant(int[] pixels, int width, int height) {
        return palette(pixels, width, height, 1)[0];
    }

    /**
     * The artwork's colours, weightiest hue first: multi-hue artwork (a gradient mark, a badge beside a logo)
     * yields one entry per distinct hue, so the room can carry all of them; monochrome artwork yields one.
     * A hue qualifies while it carries at least an eighth of the artwork's chroma, and everything shares the
     * same classification as before — grey plates and tiny badges stay out.
     */
    static int[] palette(int[] pixels, int width, int height, int maxColours) {
        if (maxColours <= 0 || pixels == null || width <= 0 || height <= 0 || pixels.length < width * height)
            return new int[]{NEUTRAL};
        double[] weights = new double[12];
        double[][] sums = new double[12][3];
        double totalWeight = 0, neutralWeight = 0, neutralRed = 0, neutralGreen = 0, neutralBlue = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int alpha = pixel >>> 24;
                int red = (pixel >>> 16) & 255, green = (pixel >>> 8) & 255, blue = pixel & 255;
                int max = Math.max(red, Math.max(green, blue));
                int min = Math.min(red, Math.min(green, blue));
                if (alpha < 128) continue;
                boolean edge = x < width / 5 || x >= width * 4 / 5 || y < height / 5 || y >= height * 4 / 5;
                double area = alpha / 255.0 * (edge ? 0.35 : 1.0);
                totalWeight += area;
                if (max < 60) continue;
                if (max - min < max * 0.22) {
                    neutralWeight += area;
                    neutralRed += red * area;
                    neutralGreen += green * area;
                    neutralBlue += blue * area;
                    continue;
                }
                float hue;
                if (max == red) hue = 60f * (green - blue) / (max - min);
                else if (max == green) hue = 120f + 60f * (blue - red) / (max - min);
                else hue = 240f + 60f * (red - green) / (max - min);
                if (hue < 0) hue += 360;
                int bin = Math.min(11, (int) (hue / 30));
                double weight = (max - min) / 255.0 * area;
                weights[bin] += weight;
                sums[bin][0] += red * weight;
                sums[bin][1] += green * weight;
                sums[bin][2] += blue * weight;
            }
        }
        // A white or grey plate does not get to wash the artwork out: the palette is the artwork's hues, and
        // only an artwork with no qualifying hue at all falls back to its neutral plate (or grey).
        // The weightiest hues first; a hue smaller than an eighth of the artwork's area is a detail, not a
        // colour of this app.
        int[] order = new int[12];
        for (int i = 0; i < 12; i++) order[i] = i;
        for (int i = 1; i < 12; i++) {
            int hold = order[i];
            int j = i - 1;
            while (j >= 0 && weights[order[j]] < weights[hold]) { order[j + 1] = order[j]; j--; }
            order[j + 1] = hold;
        }
        double floor = Math.max(totalWeight * 0.125, width * height * 0.008);
        int count = 0;
        while (count < maxColours && count < 12 && weights[order[count]] >= floor) count++;
        if (count == 0) {
            if (neutralWeight == 0) return new int[]{NEUTRAL};
            return new int[]{0xFF000000 | ((int) Math.round(neutralRed / neutralWeight) << 16)
                    | ((int) Math.round(neutralGreen / neutralWeight) << 8)
                    | (int) Math.round(neutralBlue / neutralWeight)};
        }
        int[] colours = new int[count];
        for (int i = 0; i < count; i++) {
            int bin = order[i];
            colours[i] = 0xFF000000 | (Math.round((float) (sums[bin][0] / weights[bin])) << 16)
                    | (Math.round((float) (sums[bin][1] / weights[bin])) << 8)
                    | Math.round((float) (sums[bin][2] / weights[bin]));
        }
        return colours;
    }

    static int blend(int from, int to, float fraction) {
        float t = Math.max(0f, Math.min(1f, fraction));
        int red = Math.round(((from >>> 16) & 255) * (1 - t) + ((to >>> 16) & 255) * t);
        int green = Math.round(((from >>> 8) & 255) * (1 - t) + ((to >>> 8) & 255) * t);
        int blue = Math.round((from & 255) * (1 - t) + (to & 255) * t);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    static int base(int colour, boolean day) {
        return blend(anchor(colour, day ? 0xFFF4F4F4 : 0xFF111111,
                day ? 0xFFF0F4FA : 0xFF070D18), colour, day ? 0.07f : 0.12f);
    }

    static int accent(int colour, boolean day) {
        return blend(anchor(colour, day ? 0xFFF4F4F4 : 0xFF1B1B1B,
                day ? 0xFFF0F4FA : 0xFF0B1220), colour, day ? 0.31f : 0.52f);
    }

    /** Fade the underlying tint continuously too: a neutral icon must not inherit a blue base. */
    private static int anchor(int colour, int neutral, int tinted) {
        int red = (colour >>> 16) & 255, green = (colour >>> 8) & 255, blue = colour & 255;
        int max = Math.max(red, Math.max(green, blue)), min = Math.min(red, Math.min(green, blue));
        float chroma = max == 0 ? 0 : (max - min) / (max * 0.22f);
        return blend(neutral, tinted, chroma);
    }
}
