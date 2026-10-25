package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * The room behind Home, as colour weather like the TV's own ambient light: a few enormous soft blooms of
 * light, each carrying one hue of the focused artwork, pour across a near-black base and into one another.
 * Each bloom drifts on its own Lissajous path, breathes, and slowly swings around its hue, so the room's
 * whole palette turns over the way ambient light does — nothing turns around a centre and no shape repeats.
 * Every frame is five bitmap transforms over a flat fill: transforms of one cached texture, never a
 * re-render. The weather leans towards the focused tile on the same easing as its colours.
 * reduced_motion (config.xml) parks it and snaps the colours.
 */
final class AppColorBackdrop extends View {

    /**
     * The master loop. Every drift, breath and hue swing inside it runs a whole number of cycles across this
     * period, so the loop is seamless, while the cycle counts are pairwise awkward (1..5) so no two blooms
     * ever line up twice the same way: the motion looks unpatterned and only truly repeats after this long.
     */
    /** How far the weather leans towards the focused tile, as a share of the screen. */
    private static final float LEAN_X = 0.12f, LEAN_Y = 0.15f;
    /**
     * One row per bloom: anchor x, anchor y, drift x, drift y, x cycles, y cycles, size as a multiple of the
     * screen diagonal, alpha, alpha wobble, hue swing in degrees, hue cycles, phase. The hue swing stays small:
     * the room's colours belong to the focused app, and only breathe around them. The anchors spread the
     * blooms over the four quadrants so their colours pour into each other across the whole screen.
     */
    private static final float[][] BLOOMS = {
            {0.30f, 0.34f, 0.20f, 0.15f, 1f, 2f, 0.92f, 0.62f, 0.18f, 12f, 1f, 0.00f},
            {0.76f, 0.22f, 0.18f, 0.14f, 3f, 1f, 0.85f, 0.56f, 0.19f, 14f, 2f, 0.31f},
            {0.22f, 0.76f, 0.22f, 0.14f, 2f, 3f, 0.88f, 0.60f, 0.18f, 10f, 3f, 0.57f},
            {0.80f, 0.74f, 0.15f, 0.13f, 4f, 2f, 0.78f, 0.54f, 0.19f, 12f, 5f, 0.83f},
    };
    /** One bloom per quadrant; each carries a hue from the focused artwork's palette, cycling if it has fewer. */
    private static final int LAYERS = BLOOMS.length;


    /** One paint per bloom, each carrying its own hue from the focused tile's artwork. */
    private final Paint[] layerPaints = new Paint[LAYERS];
    private final Bitmap bloom;
    private final boolean flow;
    private final float[] leanNow = new float[2], leanFrom = new float[2], leanTo = new float[2];
    private final float[] hsv = new float[3];
    private final int[] current = filled();
    private ValueAnimator animator;
    private int[] target = filled();
    private float t, flowStartT, diagonal;
    private long flowStartedMs;
    private boolean flowRunning;
    private int base;
    private boolean initialized;
    private final Runnable flowFrame = new Runnable() {
        @Override public void run() {
            if (!flowRunning) return;
            updateFlowPhase();
            invalidate();
            postDelayed(this, AmbientMotion.FRAME_MS);
        }
    };

    AppColorBackdrop(Context context, boolean flow) {
        super(context);
        this.flow = flow;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        forceHasOverlappingRendering(false);
        // One baked bloom, drawn four times huge — white-with-alpha, so each hue of the focused artwork rides
        // on its own paint and a focus change is four tints, not a rebuilt texture.
        bloom = bakeBloom(256);
        for (int i = 0; i < LAYERS; i++) {
            layerPaints[i] = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        }
        apply(filled());
    }

    private void startFlow() {
        if (!flow || flowRunning) return;
        flowRunning = true;
        flowStartedMs = SystemClock.uptimeMillis();
        removeCallbacks(flowFrame);
        post(flowFrame);
    }

    private void stopFlow() {
        if (!flowRunning) return;
        updateFlowPhase();
        flowStartT = t;
        flowRunning = false;
        removeCallbacks(flowFrame);
    }

    private void updateFlowPhase() {
        long elapsed = Math.max(0, SystemClock.uptimeMillis() - flowStartedMs);
        t = AmbientMotion.phase(flowStartT, elapsed);
    }

    /** One enormous soft bloom: a bright core falling away across most of the texture, gaussian-like. */
    private static Bitmap bakeBloom(int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float half = size / 2f;
        p.setShader(new RadialGradient(half, half, half,
                new int[]{0xFFFFFFFF, 0xE6FFFFFF, 0x30FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.38f, 0.72f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, size, size, p);
        return bmp;
    }

    /**
     * The focused app's room colour, and where the weather should lean: the tile's centre as a share of the
     * screen. A rapid refocus reads the colour and the lean where they are, so nothing jumps to an old app.
     */
    void focus(int[] colours, boolean animate, float tileX, float tileY) {
        int w = getWidth(), h = getHeight();
        leanFrom[0] = leanNow[0];
        leanFrom[1] = leanNow[1];
        if (w > 0 && !Float.isNaN(tileX)) {
            leanTo[0] = (tileX - 0.5f) * w * LEAN_X;
            leanTo[1] = (tileY - 0.5f) * h * LEAN_Y;
        }
        // The three layers cycle through whatever the artwork offered: two hues means the mirrored wisp
        // shares the strongest, one hue means the room stays in that single colour as before.
        int[] next = new int[LAYERS];
        for (int i = 0; i < LAYERS; i++) next[i] = 0xFF000000 | colours[i % colours.length];
        boolean colourChanged = !initialized || !java.util.Arrays.equals(next, target);
        boolean leanChanged = leanFrom[0] != leanTo[0] || leanFrom[1] != leanTo[1];
        if (initialized && !colourChanged && !leanChanged) return;
        if (animator != null) animator.cancel();
        target = next;
        // Self-heal: onPause's stop() or a visibility race may have parked the flow; the next focus
        // change, which always comes on a live Home, sets it going again.
        if (flow && !flowRunning && getWindowVisibility() == VISIBLE) startFlow();
        if (!initialized || !animate) {
            initialized = true;
            apply(next);
            leanNow[0] = leanTo[0];
            leanNow[1] = leanTo[1];
            invalidate();
            return;
        }
        initialized = true;
        final int[] from = current.clone();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new PathInterpolator(0.20f, 0f, 0.20f, 1f));
        animator.addUpdateListener(value -> {
            float f = (float) value.getAnimatedValue();
            for (int i = 0; i < LAYERS; i++) current[i] = AppBackdropPalette.blend(from[i], target[i], f);
            base = AppBackdropPalette.base(current[0], PersonalAppearance.day(getContext()));
            // The light walks to the new tile on the same easing as its colours: one motion, not two.
            leanNow[0] = leanFrom[0] + (leanTo[0] - leanFrom[0]) * f;
            leanNow[1] = leanFrom[1] + (leanTo[1] - leanFrom[1]) * f;
            invalidate();
        });
        animator.start();
    }

    /** A colour for every layer, before any artwork has been read. */
    private static int[] filled() {
        int[] colours = new int[LAYERS];
        java.util.Arrays.fill(colours, AppBackdropPalette.NEUTRAL);
        return colours;
    }

    private void apply(int[] colours) {
        System.arraycopy(colours, 0, current, 0, LAYERS);
        base = AppBackdropPalette.base(colours[0], PersonalAppearance.day(getContext()));
        invalidate();
    }

    void stop() {
        if (animator != null) { animator.cancel(); animator = null; }
        stopFlow();
        apply(target);
    }

    @Override protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        canvas.drawColor(base);
        float twoPi = (float) (2 * Math.PI);
        for (int i = 0; i < LAYERS; i++) {
            float[] b = BLOOMS[i];
            float nx = b[0] + b[2] * (float) Math.sin(twoPi * (b[4] * t + b[11]));
            float ny = b[1] + b[3] * (float) Math.sin(twoPi * (b[5] * t + b[11] * 1.7f));
            float size = diagonal * b[6] * (1f + 0.08f * (float) Math.sin(twoPi * ((i + 2) * t + b[11])));
            float alpha = Math.max(0.35f, Math.min(1f, b[7] + b[8] * (float) Math.sin(twoPi * (b[10] * t + b[11] * 2.3f))));
            // The hue breathes a few degrees around the colour the artwork gave this bloom: the room stays
            // unmistakably in the app's own family while never sitting perfectly still.
            Color.colorToHSV(current[i], hsv);
            hsv[0] = (hsv[0] + b[9] * (float) Math.sin(twoPi * (b[10] * t + b[11] * 0.6f)) + 360f) % 360f;
            layerPaints[i].setColorFilter(new PorterDuffColorFilter(Color.HSVToColor(hsv), PorterDuff.Mode.SRC_IN));
            layerPaints[i].setAlpha(Math.round(255f * alpha));
            canvas.save();
            canvas.translate(w * nx + leanNow[0], h * ny + leanNow[1]);
            canvas.scale(size / 256f, size / 256f);
            canvas.drawBitmap(bloom, -128, -128, layerPaints[i]);
            canvas.restore();
        }
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        diagonal = (float) Math.hypot(w, h);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getWindowVisibility() == VISIBLE) startFlow();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) startFlow(); else stopFlow();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
}
