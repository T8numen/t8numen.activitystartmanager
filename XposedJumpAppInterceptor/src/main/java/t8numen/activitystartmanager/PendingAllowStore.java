package t8numen.activitystartmanager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PendingAllowStore {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ALC-pending-provider");
        thread.setDaemon(true);
        return thread;
    });

    private PendingAllowStore() {
    }

    static void saveAsync(Context context, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        Intent safeIntent = intent == null ? null : new Intent(intent);
        EXECUTOR.execute(() -> save(context, sourceRef, targetRef, safeIntent));
    }

    static void save(Context context, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        if (context == null || sourceRef == null || targetRef == null || intent == null) {
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE, sourceRef.getPackageName());
            values.put(ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE, targetRef.getPackageName());
            values.put(ModuleConfig.COLUMN_PENDING_TARGET_CLASS, targetRef.getClassName());
            values.put(ModuleConfig.COLUMN_PENDING_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME));
            values.put(ModuleConfig.COLUMN_PENDING_EXPIRES_AT,
                    SystemClock.elapsedRealtime() + ModuleConfig.PENDING_ALLOW_TTL_MS);
            context.getContentResolver().update(ModuleConfig.PENDING_ALLOW_URI, values, null, null);
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending allow save failed: " + throwable);
        }
    }

    static PendingAllowState take(Context context) {
        PendingAllowState state = peek(context);
        if (state != null) {
            clear(context);
        }
        return state;
    }

    static PendingAllowState peek(Context context) {
        if (context == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ModuleConfig.PENDING_ALLOW_URI, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            PendingAllowState state = new PendingAllowState(
                    getString(cursor, ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE),
                    getString(cursor, ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE),
                    getString(cursor, ModuleConfig.COLUMN_PENDING_TARGET_CLASS),
                    getString(cursor, ModuleConfig.COLUMN_PENDING_INTENT_URI),
                    getLong(cursor, ModuleConfig.COLUMN_PENDING_EXPIRES_AT)
            );
            if (state.isExpired()) {
                clear(context);
                return null;
            }
            return state;
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending allow query failed: " + throwable);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    static void clearAsync(Context context) {
        EXECUTOR.execute(() -> clear(context));
    }

    static void clear(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.getContentResolver().delete(ModuleConfig.PENDING_ALLOW_URI, null, null);
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending allow clear failed: " + throwable);
        }
    }

    private static String getString(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex < 0) {
            return null;
        }
        return cursor.getString(columnIndex);
    }

    private static long getLong(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex < 0) {
            return 0L;
        }
        return cursor.getLong(columnIndex);
    }
}
