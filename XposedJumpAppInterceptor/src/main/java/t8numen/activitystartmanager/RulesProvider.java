package t8numen.activitystartmanager;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class RulesProvider extends ContentProvider {
    private static final int MATCH_RULES = 1;
    private static final int MATCH_PENDING_ALLOW = 2;
    private static final int MATCH_PENDING_ASK = 3;
    private static final int MATCH_RECENT_RECORDS = 4;
    private static final int MATCH_SETTINGS = 5;

    private static final UriMatcher URI_MATCHER = buildUriMatcher();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (URI_MATCHER.match(uri)) {
            case MATCH_RULES:
                return queryRules();
            case MATCH_PENDING_ALLOW:
                return queryPendingAllow();
            case MATCH_PENDING_ASK:
                return queryPendingAsk();
            case MATCH_RECENT_RECORDS:
                return queryRecentRecords();
            case MATCH_SETTINGS:
                return querySettings();
            default:
                throw new IllegalArgumentException("Unsupported uri: " + uri);
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case MATCH_RULES:
                return "vnd.android.cursor.item/vnd." + ModuleConfig.PROVIDER_AUTHORITY + ".rules";
            case MATCH_PENDING_ALLOW:
                return "vnd.android.cursor.item/vnd." + ModuleConfig.PROVIDER_AUTHORITY + ".pending_allow";
            case MATCH_PENDING_ASK:
                return "vnd.android.cursor.item/vnd." + ModuleConfig.PROVIDER_AUTHORITY + ".pending_ask";
            case MATCH_RECENT_RECORDS:
                return "vnd.android.cursor.item/vnd." + ModuleConfig.PROVIDER_AUTHORITY + ".recent_records";
            case MATCH_SETTINGS:
                return "vnd.android.cursor.item/vnd." + ModuleConfig.PROVIDER_AUTHORITY + ".settings";
            default:
                throw new IllegalArgumentException("Unsupported uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        switch (URI_MATCHER.match(uri)) {
            case MATCH_PENDING_ALLOW:
                clearPendingAllow();
                return 1;
            case MATCH_PENDING_ASK:
                clearPendingAsk();
                return 1;
            case MATCH_RECENT_RECORDS:
                clearRecentRecords();
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values == null) {
            return 0;
        }
        switch (URI_MATCHER.match(uri)) {
            case MATCH_PENDING_ALLOW:
                savePendingAllow(values);
                return 1;
            case MATCH_PENDING_ASK:
                savePendingAsk(values);
                return 1;
            case MATCH_RECENT_RECORDS:
                saveRecentRecord(values);
                return 1;
            default:
                return 0;
        }
    }

    private Cursor queryRules() {
        MatrixCursor cursor = new MatrixCursor(new String[]{ModuleConfig.COLUMN_RULE_TEXT});
        cursor.addRow(new Object[]{RuleRepository.loadRulesFromLocalPrefs(getContext())});
        return cursor;
    }

    private Cursor queryPendingAllow() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE,
                ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE,
                ModuleConfig.COLUMN_PENDING_TARGET_CLASS,
                ModuleConfig.COLUMN_PENDING_INTENT_URI,
                ModuleConfig.COLUMN_PENDING_EXPIRES_AT
        });
        PendingAllowState state = loadPendingAllow();
        if (state != null && !state.isExpired()) {
            cursor.addRow(new Object[]{
                    state.getSourcePackage(),
                    state.getTargetPackage(),
                    state.getTargetClassName(),
                    state.getIntentUri(),
                    Long.valueOf(loadPendingExpiresAt())
            });
        }
        return cursor;
    }

    private Cursor queryPendingAsk() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ModuleConfig.COLUMN_ASK_REQUEST_ID,
                ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE,
                ModuleConfig.COLUMN_ASK_SOURCE_CLASS,
                ModuleConfig.COLUMN_ASK_TARGET_PACKAGE,
                ModuleConfig.COLUMN_ASK_TARGET_CLASS,
                ModuleConfig.COLUMN_ASK_INTENT_URI,
                ModuleConfig.COLUMN_ASK_EXPIRES_AT
        });
        PendingAskState state = loadPendingAsk();
        if (state != null && !state.isExpired()) {
            cursor.addRow(new Object[]{
                    state.getRequestId(),
                    state.getSourceRef().getPackageName(),
                    state.getSourceRef().getClassName(),
                    state.getTargetRef().getPackageName(),
                    state.getTargetRef().getClassName(),
                    state.getIntentUri(),
                    Long.valueOf(state.getExpiresAt())
            });
        }
        return cursor;
    }

    private Cursor queryRecentRecords() {
        MatrixCursor cursor = new MatrixCursor(new String[]{ModuleConfig.COLUMN_RECENT_RECORDS});
        cursor.addRow(new Object[]{getPreferences().getString(ModuleConfig.KEY_RECENT_RECORDS, "")});
        return cursor;
    }

    private Cursor querySettings() {
        MatrixCursor cursor = new MatrixCursor(new String[]{ModuleConfig.COLUMN_DIAGNOSTIC_LOG_ENABLED});
        boolean enabled = getPreferences().getBoolean(ModuleConfig.KEY_DIAGNOSTIC_LOG_ENABLED, false);
        cursor.addRow(new Object[]{Integer.valueOf(enabled ? 1 : 0)});
        return cursor;
    }

    private PendingAllowState loadPendingAllow() {
        if (getContext() == null) {
            return null;
        }
        String sourcePackage = getPreferences().getString(ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE, null);
        String targetPackage = getPreferences().getString(ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE, null);
        String targetClass = getPreferences().getString(ModuleConfig.COLUMN_PENDING_TARGET_CLASS, null);
        String intentUri = getPreferences().getString(ModuleConfig.COLUMN_PENDING_INTENT_URI, null);
        long expiresAt = loadPendingExpiresAt();
        if (sourcePackage == null || targetPackage == null || intentUri == null || expiresAt <= 0L) {
            return null;
        }
        return new PendingAllowState(sourcePackage, targetPackage, targetClass, intentUri, expiresAt);
    }

    private long loadPendingExpiresAt() {
        return getPreferences().getLong(ModuleConfig.COLUMN_PENDING_EXPIRES_AT, 0L);
    }

    private void savePendingAllow(ContentValues values) {
        getPreferences().edit()
                .putString(ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE,
                        values.getAsString(ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE))
                .putString(ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE,
                        values.getAsString(ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE))
                .putString(ModuleConfig.COLUMN_PENDING_TARGET_CLASS,
                        values.getAsString(ModuleConfig.COLUMN_PENDING_TARGET_CLASS))
                .putString(ModuleConfig.COLUMN_PENDING_INTENT_URI,
                        values.getAsString(ModuleConfig.COLUMN_PENDING_INTENT_URI))
                .putLong(ModuleConfig.COLUMN_PENDING_EXPIRES_AT,
                        safeGetLong(values, ModuleConfig.COLUMN_PENDING_EXPIRES_AT))
                .apply();
    }

    private PendingAskState loadPendingAsk() {
        if (getContext() == null) {
            return null;
        }
        String requestId = getPreferences().getString(ModuleConfig.COLUMN_ASK_REQUEST_ID, null);
        String sourcePackage = getPreferences().getString(ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE, null);
        String sourceClass = getPreferences().getString(ModuleConfig.COLUMN_ASK_SOURCE_CLASS, null);
        String targetPackage = getPreferences().getString(ModuleConfig.COLUMN_ASK_TARGET_PACKAGE, null);
        String targetClass = getPreferences().getString(ModuleConfig.COLUMN_ASK_TARGET_CLASS, null);
        String intentUri = getPreferences().getString(ModuleConfig.COLUMN_ASK_INTENT_URI, null);
        long expiresAt = getPreferences().getLong(ModuleConfig.COLUMN_ASK_EXPIRES_AT, 0L);
        ActivityRef sourceRef = buildRef(sourcePackage, sourceClass);
        ActivityRef targetRef = buildRef(targetPackage, targetClass);
        if (requestId == null || sourceRef == null || targetRef == null || intentUri == null || expiresAt <= 0L) {
            return null;
        }
        return new PendingAskState(requestId, sourceRef, targetRef, intentUri, expiresAt);
    }

    private void savePendingAsk(ContentValues values) {
        getPreferences().edit()
                .putString(ModuleConfig.COLUMN_ASK_REQUEST_ID,
                        values.getAsString(ModuleConfig.COLUMN_ASK_REQUEST_ID))
                .putString(ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE,
                        values.getAsString(ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE))
                .putString(ModuleConfig.COLUMN_ASK_SOURCE_CLASS,
                        values.getAsString(ModuleConfig.COLUMN_ASK_SOURCE_CLASS))
                .putString(ModuleConfig.COLUMN_ASK_TARGET_PACKAGE,
                        values.getAsString(ModuleConfig.COLUMN_ASK_TARGET_PACKAGE))
                .putString(ModuleConfig.COLUMN_ASK_TARGET_CLASS,
                        values.getAsString(ModuleConfig.COLUMN_ASK_TARGET_CLASS))
                .putString(ModuleConfig.COLUMN_ASK_INTENT_URI,
                        values.getAsString(ModuleConfig.COLUMN_ASK_INTENT_URI))
                .putLong(ModuleConfig.COLUMN_ASK_EXPIRES_AT,
                        safeGetLong(values, ModuleConfig.COLUMN_ASK_EXPIRES_AT))
                .apply();
        notifyPendingAskChanged();
    }

    private void clearPendingAllow() {
        getPreferences().edit()
                .remove(ModuleConfig.COLUMN_PENDING_SOURCE_PACKAGE)
                .remove(ModuleConfig.COLUMN_PENDING_TARGET_PACKAGE)
                .remove(ModuleConfig.COLUMN_PENDING_TARGET_CLASS)
                .remove(ModuleConfig.COLUMN_PENDING_INTENT_URI)
                .remove(ModuleConfig.COLUMN_PENDING_EXPIRES_AT)
                .apply();
    }

    private void clearPendingAsk() {
        getPreferences().edit()
                .remove(ModuleConfig.COLUMN_ASK_REQUEST_ID)
                .remove(ModuleConfig.COLUMN_ASK_SOURCE_PACKAGE)
                .remove(ModuleConfig.COLUMN_ASK_SOURCE_CLASS)
                .remove(ModuleConfig.COLUMN_ASK_TARGET_PACKAGE)
                .remove(ModuleConfig.COLUMN_ASK_TARGET_CLASS)
                .remove(ModuleConfig.COLUMN_ASK_INTENT_URI)
                .remove(ModuleConfig.COLUMN_ASK_EXPIRES_AT)
                .apply();
        notifyPendingAskChanged();
    }

    private void saveRecentRecord(ContentValues values) {
        String recordLine = buildRecentRecordLine(values);
        if (recordLine.isEmpty()) {
            return;
        }
        String current = getPreferences().getString(ModuleConfig.KEY_RECENT_RECORDS, "");
        StringBuilder builder = new StringBuilder(recordLine);
        int count = 1;
        if (current != null && !current.trim().isEmpty()) {
            String[] lines = current.split("\\r?\\n");
            for (String line : lines) {
                if (count >= ModuleConfig.MAX_RECENT_RECORDS) {
                    break;
                }
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                builder.append('\n').append(trimmed);
                count++;
            }
        }
        getPreferences().edit()
                .putString(ModuleConfig.KEY_RECENT_RECORDS, builder.toString())
                .apply();
        notifyRecentRecordsChanged();
    }

    private String buildRecentRecordLine(ContentValues values) {
        if (values == null) {
            return "";
        }
        long time = safeGetLong(values, ModuleConfig.COLUMN_RECORD_TIME);
        String action = safeRecordValue(values.getAsString(ModuleConfig.COLUMN_RECORD_ACTION));
        String sourcePackage = safeRecordValue(values.getAsString(ModuleConfig.COLUMN_RECORD_SOURCE_PACKAGE));
        String sourceClass = safeRecordValue(values.getAsString(ModuleConfig.COLUMN_RECORD_SOURCE_CLASS));
        String targetPackage = safeRecordValue(values.getAsString(ModuleConfig.COLUMN_RECORD_TARGET_PACKAGE));
        String targetClass = safeRecordValue(values.getAsString(ModuleConfig.COLUMN_RECORD_TARGET_CLASS));
        if (time <= 0L || action.isEmpty() || sourcePackage.isEmpty() || targetPackage.isEmpty()) {
            return "";
        }
        return time + "\t" + action + "\t" + sourcePackage + "\t" + sourceClass
                + "\t" + targetPackage + "\t" + targetClass;
    }

    private String safeRecordValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private void clearRecentRecords() {
        getPreferences().edit()
                .remove(ModuleConfig.KEY_RECENT_RECORDS)
                .apply();
        notifyRecentRecordsChanged();
    }

    private void notifyPendingAskChanged() {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(ModuleConfig.PENDING_ASK_URI, null);
        }
    }

    private void notifyRecentRecordsChanged() {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(ModuleConfig.RECENT_RECORDS_URI, null);
        }
    }

    private ActivityRef buildRef(String packageName, String className) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        if (className == null || className.isEmpty()) {
            return ActivityRef.fromPackage(packageName);
        }
        return ActivityRef.fromShortComponent(packageName, packageName + "/" + className);
    }

    private android.content.SharedPreferences getPreferences() {
        return getContext().getSharedPreferences(ModuleConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE);
    }

    private long safeGetLong(ContentValues values, String key) {
        Long value = values.getAsLong(key);
        return value == null ? 0L : value.longValue();
    }

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(ModuleConfig.PROVIDER_AUTHORITY, ModuleConfig.PATH_CURRENT_RULES, MATCH_RULES);
        matcher.addURI(ModuleConfig.PROVIDER_AUTHORITY, ModuleConfig.PATH_PENDING_ALLOW, MATCH_PENDING_ALLOW);
        matcher.addURI(ModuleConfig.PROVIDER_AUTHORITY, ModuleConfig.PATH_PENDING_ASK, MATCH_PENDING_ASK);
        matcher.addURI(ModuleConfig.PROVIDER_AUTHORITY, ModuleConfig.PATH_RECENT_RECORDS, MATCH_RECENT_RECORDS);
        matcher.addURI(ModuleConfig.PROVIDER_AUTHORITY, ModuleConfig.PATH_SETTINGS, MATCH_SETTINGS);
        return matcher;
    }
}
