package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/** Household shortcut to Xiaomi's existing file browser, which has no launcher category. */
final class PersonalSystemApps {
    static final String FILE_PACKAGE = "com.xiaomi.mitv.mediaexplorer";
    private static final String FILE_ACTION = "duokan.mediaexplorer.intent.action.BROWSE_DEVICE_LIST";

    private PersonalSystemApps() { }

    static ResolveInfo fileManager(Context context) {
        if (!PersonalAppearance.enabled(context)) return null;
        ResolveInfo info = context.getPackageManager().resolveActivity(
                new Intent(FILE_ACTION).setPackage(FILE_PACKAGE), PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null || !info.activityInfo.exported
                || !info.activityInfo.enabled || !info.activityInfo.applicationInfo.enabled) return null;
        info.nonLocalizedLabel = "文件管理";
        return info;
    }

    static Intent launch(Context context, String pkg) {
        if (!PersonalAppearance.enabled(context) || !FILE_PACKAGE.equals(pkg)) return null;
        return new Intent(FILE_ACTION).setPackage(FILE_PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
