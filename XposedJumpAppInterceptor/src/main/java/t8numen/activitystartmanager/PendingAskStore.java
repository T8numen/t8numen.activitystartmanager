package t8numen.activitystartmanager;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PendingAskStore {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ALC-ask-provider");
        thread.setDaemon(true);
        return thread;
    });

    private PendingAskStore() {
    }

    static void saveAsync(Context context, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        saveAsync(context, newRequestId(), sourceRef, targetRef, intent);
    }

    static void saveAsync(Context context, String requestId, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        Intent safeIntent = intent == null ? null : new Intent(intent);
        EXECUTOR.execute(() -> {
            String savedRequestId = save(context, requestId, sourceRef, targetRef, safeIntent);
            if (savedRequestId != null) {
                notifyUpdated(context);
            }
        });
    }

    static String newRequestId() {
        return Long.toString(SystemClock.elapsedRealtimeNanos());
    }

    static String save(Context context, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        return save(context, newRequestId(), sourceRef, targetRef, intent);
    }

    static String save(Context context, String requestId, ActivityRef sourceRef, ActivityRef targetRef, Intent intent) {
        if (context == null || sourceRef == null || targetRef == null || intent == null) {
            return null;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(ModuleConfig.COLUMN_ASK_REQUEST_ID, requestId);
            values.put(ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE, sourceRef.getPackageName());
            values.put(ModuleConfig.COLUMN_ASK_SOURCE_CLASS, sourceRef.getClassName());
            values.put(ModuleConfig.COLUMN_ASK_TARGET_PACKAGE, targetRef.getPackageName());
            values.put(ModuleConfig.COLUMN_ASK_TARGET_CLASS, targetRef.getClassName());
            values.put(ModuleConfig.COLUMN_ASK_INTENT_URI, intent.toUri(Intent.URI_INTENT_SCHEME));
            values.put(ModuleConfig.COLUMN_ASK_EXPIRES_AT,
                    SystemClock.elapsedRealtime() + ModuleConfig.ASK_TIMEOUT_MS);
            context.getContentResolver().update(ModuleConfig.PENDING_ASK_URI, values, null, null);
            return requestId;
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending ask save failed: " + throwable);
            return null;
        }
    }

    static PendingAskState peek(Context context) {
        if (context == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ModuleConfig.PENDING_ASK_URI, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            PendingAskState state = new PendingAskState(
                    getString(cursor, ModuleConfig.COLUMN_ASK_REQUEST_ID),
                    buildRef(
                            getString(cursor, ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE),
                            getString(cursor, ModuleConfig.COLUMN_ASK_SOURCE_CLASS)
                    ),
                    buildRef(
                            getString(cursor, ModuleConfig.COLUMN_ASK_TARGET_PACKAGE),
                            getString(cursor, ModuleConfig.COLUMN_ASK_TARGET_CLASS)
                    ),
                    getString(cursor, ModuleConfig.COLUMN_ASK_INTENT_URI),
                    getLong(cursor, ModuleConfig.COLUMN_ASK_EXPIRES_AT)
            );
            if (state.getRequestId() == null
                    || state.getSourceRef() == null
                    || state.getTargetRef() == null
                    || state.getIntentUri() == null
                    || state.isExpired()) {
                clear(context);
                return null;
            }
            return state;
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending ask query failed: " + throwable);
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
            context.getContentResolver().delete(ModuleConfig.PENDING_ASK_URI, null, null);
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending ask clear failed: " + throwable);
        }
    }

    private static void notifyUpdated(Context context) {
        if (context == null) {
            return;
        }
        try {
            Intent intent = new Intent(ModuleConfig.ACTION_ASK_UPDATED);
            intent.setPackage(ModuleConfig.MODULE_PACKAGE);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(intent);
        } catch (Throwable throwable) {
            LogUtil.writeLog("pending ask notify failed: " + throwable);
        }
    }

    private static ActivityRef buildRef(String packageName, String className) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        if (className == null || className.isEmpty()) {
            return ActivityRef.fromPackage(packageName);
        }
        return ActivityRef.fromShortComponent(packageName, packageName + "/" + className);
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
