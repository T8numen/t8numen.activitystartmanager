package t8numen.activitystartmanager;

import android.app.ActivityManager;

import de.robv.android.xposed.XposedHelpers;

public final class FrameworkCompat {
    private static Integer sStartAborted;

    private FrameworkCompat() {
    }

    public static int getStartAborted() {
        if (sStartAborted != null) {
            return sStartAborted.intValue();
        }
        try {
            sStartAborted = Integer.valueOf(XposedHelpers.getStaticIntField(ActivityManager.class, "START_ABORTED"));
        } catch (Throwable throwable) {
            LogUtil.writeLog("load START_ABORTED failed, fallback to 0: " + throwable);
            sStartAborted = Integer.valueOf(0);
        }
        return sStartAborted.intValue();
    }
}
