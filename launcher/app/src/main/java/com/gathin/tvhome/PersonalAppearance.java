package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.time.LocalTime;

/** Small, local-only presentation layer. No services, network, or global display writes. */
final class PersonalAppearance {
    static final String MODE_KEY = "appearance_mode";
    static final String BACKGROUND_KEY = "background_style";
    static final String AMBIENT_MOTION_KEY = "ambient_background_motion";

    private PersonalAppearance() { }

    static boolean enabled(Context context) {
        return context.getResources().getBoolean(R.bool.personalized_tv);
    }

    static String mode(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getString(MODE_KEY, "night");
    }

    static boolean appBackdrop(Context context) {
        return enabled(context) && "app".equals(context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getString(BACKGROUND_KEY, "app"));
    }

    /** Continuous ambient drift; focus colour changes remain governed by the main animations switch. */
    static boolean ambientMotion(Context context) {
        return enabled(context) && context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getBoolean(AMBIENT_MOTION_KEY, true);
    }

    static boolean day(Context context) {
        return enabled(context) && AppearancePolicy.isDay(mode(context), LocalTime.now().getHour());
    }

    static int foreground(Context context) {
        return day(context) ? 0xFF162536 : 0xFFF6F8FC;
    }

    static int secondary(Context context) {
        return day(context) ? 0xC41D3349 : 0xB8DDE7F5;
    }

    static int background(Context context) {
        return day(context) ? 0xFFE2EBF4 : 0xFF08101D;
    }

    static void main(Activity activity) {
        if (!enabled(activity)) return;
        int[] strong = {R.id.clock, R.id.heroName};
        int[] soft = {R.id.greeting, R.id.date, R.id.heroMeta, R.id.heroDots};
        for (int id : strong) text(activity.findViewById(id), foreground(activity));
        for (int id : soft) text(activity.findViewById(id), secondary(activity));
        activity.findViewById(R.id.clockDivider).setBackgroundColor(day(activity) ? 0x33172A3F : 0x33FFFFFF);
        ViewGroup bar = activity.findViewById(R.id.topButtons);
        for (int i = 0; i < bar.getChildCount(); i++) {
            if (bar.getChildAt(i) instanceof TextView) button((TextView) bar.getChildAt(i), day(activity));
        }
        requestRefreshRate(activity);
    }

    static void settings(Activity activity) {
        if (!enabled(activity)) return;
        View content = activity.findViewById(android.R.id.content);
        content.setBackgroundColor(background(activity));
        tintTree(content, day(activity));
        requestRefreshRate(activity);
    }

    private static void tintTree(View view, boolean day) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (view.isFocusable()) button(text, day);
            else text.setTextColor(day ? 0xFF24384F : 0xFFE5ECF7);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (view instanceof android.widget.ScrollView) view.setBackgroundColor(day ? 0xFFE2EBF4 : 0xFF08101D);
            for (int i = 0; i < group.getChildCount(); i++) tintTree(group.getChildAt(i), day);
        }
    }

    private static void text(View view, int color) {
        if (view instanceof TextView) ((TextView) view).setTextColor(color);
    }

    private static GradientDrawable plate(Context context, int top, int bottom, int rim) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        float density = context.getResources().getDisplayMetrics().density;
        drawable.setCornerRadius(21 * density);
        drawable.setStroke(Math.max(1, Math.round(density * 0.7f)), rim);
        return drawable;
    }

    private static void button(TextView view, boolean day) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, plate(view.getContext(), 0xF7FFFFFF, 0xE9FFFFFF, Color.WHITE));
        states.addState(new int[]{android.R.attr.state_selected}, plate(view.getContext(), day ? 0xA6FFFFFF : 0x376DA9F5,
                day ? 0x75FFFFFF : 0x1A6DA9F5, day ? 0xAAFFFFFF : 0x777EAFF2));
        states.addState(new int[]{}, plate(view.getContext(), day ? 0x7AFFFFFF : 0x29FFFFFF,
                day ? 0x3DFFFFFF : 0x0BFFFFFF, day ? 0xA0FFFFFF : 0x36FFFFFF));
        view.setBackground(states);
        ColorStateList colors = new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{0xFF17283D, day ? 0xFF24384F : 0xFFF5F8FC});
        view.setTextColor(colors);
        view.setCompoundDrawableTintList(colors);
    }

    /** Select only a mode the firmware actually advertises, at the current resolution. */
    static void requestRefreshRate(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (display == null) return;
        Display.Mode current = display.getMode();
        Display.Mode best = current;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == current.getPhysicalWidth()
                    && mode.getPhysicalHeight() == current.getPhysicalHeight()
                    && mode.getRefreshRate() > best.getRefreshRate()) best = mode;
        }
        WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
        attributes.preferredDisplayModeId = best.getModeId();
        attributes.preferredRefreshRate = best.getRefreshRate();
        activity.getWindow().setAttributes(attributes);
    }

    static String wallpaperName(String filename) {
        String[] names = {"暮色群峰", "黄金海岸", "月下湖泊", "极光之夜", "沙丘微光", "云海之上", "镜湖", "茶山", "雪山晚霞", "城市灯火", "热气球日出", "雨季", "灯塔海岸", "秋日山谷", "流光玻璃", "青紫极光"};
        if (filename.length() > 2) {
            try {
                int index = Integer.parseInt(filename.substring(0, 2)) - 1;
                if (index >= 0 && index < names.length) return names[index];
            } catch (NumberFormatException ignored) { }
        }
        return filename;
    }
}
