package t8numen.activitystartmanager;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

public final class ModuleSettingsStore {
    private ModuleSettingsStore() {
    }

    public static boolean loadDiagnosticLogEnabledLocal(Context context) {
        if (context == null) {
            return false;
        }
        return context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(ModuleConfig.KEY_DIAGNOSTIC_LOG_ENABLED, false);
    }

    public static void saveDiagnosticLogEnabledLocal(Context context, boolean enabled) {
        if (context == null) {
            return;
        }
        context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ModuleConfig.KEY_DIAGNOSTIC_LOG_ENABLED, enabled)
                .apply();
        LogUtil.setDiagnosticEnabled(enabled);
        try {
            context.getContentResolver().notifyChange(ModuleConfig.SETTINGS_URI, null);
            Intent intent = new Intent(ModuleConfig.ACTION_RULES_UPDATED);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(intent);
        } catch (Throwable throwable) {
            LogUtil.writeLog("notify settings updated failed: " + throwable);
        }
    }

    public static void forceRefreshAsync(Context context) {
        Thread thread = new Thread(() -> forceRefreshNow(context), "ALC-settings-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    public static void forceRefreshNow(Context context) {
        LogUtil.setDiagnosticEnabled(loadDiagnosticLogEnabledFromProvider(context));
    }

    private static boolean loadDiagnosticLogEnabledFromProvider(Context context) {
        if (context == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ModuleConfig.SETTINGS_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(ModuleConfig.COLUMN_DIAGNOSTIC_LOG_ENABLED);
                return columnIndex >= 0 && cursor.getInt(columnIndex) != 0;
            }
        } catch (Throwable throwable) {
            LogUtil.writeLog("query settings failed: " + throwable);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }
}
