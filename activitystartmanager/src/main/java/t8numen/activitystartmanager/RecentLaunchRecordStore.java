package t8numen.activitystartmanager;

import android.content.ContentValues;
import android.content.Context;

public final class RecentLaunchRecordStore {
    private RecentLaunchRecordStore() {
    }

    public static void saveAsync(Context context, String action, ActivityRef sourceRef, ActivityRef targetRef) {
        if (context == null || action == null || sourceRef == null || targetRef == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(ModuleConfig.COLUMN_RECORD_TIME, Long.valueOf(System.currentTimeMillis()));
        values.put(ModuleConfig.COLUMN_RECORD_ACTION, action);
        values.put(ModuleConfig.COLUMN_RECORD_SOURCE_PACKAGE, sourceRef.getPackageName());
        values.put(ModuleConfig.COLUMN_RECORD_SOURCE_CLASS, sourceRef.getClassName());
        values.put(ModuleConfig.COLUMN_RECORD_TARGET_PACKAGE, targetRef.getPackageName());
        values.put(ModuleConfig.COLUMN_RECORD_TARGET_CLASS, targetRef.getClassName());
        Thread thread = new Thread(() -> {
            try {
                context.getContentResolver().update(ModuleConfig.RECENT_RECORDS_URI, values, null, null);
            } catch (Throwable throwable) {
                LogUtil.writeLog("save recent launch record failed: " + throwable);
            }
        }, "ALC-recent-record");
        thread.setDaemon(true);
        thread.start();
    }

    public static String loadLocal(Context context) {
        if (context == null) {
            return "";
        }
        return context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(ModuleConfig.KEY_RECENT_RECORDS, "");
    }

    public static void clearLocal(Context context) {
        if (context == null) {
            return;
        }
        context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(ModuleConfig.KEY_RECENT_RECORDS)
                .apply();
    }
}
