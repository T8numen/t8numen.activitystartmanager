package t8numen.activitystartmanager;

import android.net.Uri;

public final class ModuleConfig {
    public static final String MODULE_PACKAGE = "t8numen.activitystartmanager";
    public static final String OLD_MODULE_PACKAGE = "com.magicianguo.xposedjumpappinterceptor";
    public static final String PREFS_NAME = "activity_launch_rules";
    public static final String KEY_RULE_TEXT = "rule_text";
    public static final String KEY_OLD_RULES_IMPORTED = "old_rules_imported";
    public static final String KEY_OLD_RULES_IMPORT_STATUS = "old_rules_import_status";
    public static final String KEY_OLD_RULES_IMPORT_COUNT = "old_rules_import_count";
    public static final String KEY_OLD_MODULE_WARNING_DISMISSED = "old_module_warning_dismissed";
    public static final String KEY_DIAGNOSTIC_LOG_ENABLED = "diagnostic_log_enabled";
    public static final String PROVIDER_AUTHORITY = MODULE_PACKAGE + ".rules";
    public static final String OLD_PROVIDER_AUTHORITY = OLD_MODULE_PACKAGE + ".rules";
    public static final String PATH_CURRENT_RULES = "current";
    public static final String PATH_PENDING_ALLOW = "pending_allow";
    public static final String PATH_PENDING_ASK = "pending_ask";
    public static final String PATH_RECENT_RECORDS = "recent_records";
    public static final String PATH_SETTINGS = "settings";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + PATH_CURRENT_RULES);
    public static final Uri OLD_PROVIDER_URI = Uri.parse("content://" + OLD_PROVIDER_AUTHORITY + "/" + PATH_CURRENT_RULES);
    public static final Uri PENDING_ALLOW_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + PATH_PENDING_ALLOW);
    public static final Uri PENDING_ASK_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + PATH_PENDING_ASK);
    public static final Uri RECENT_RECORDS_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + PATH_RECENT_RECORDS);
    public static final Uri SETTINGS_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + PATH_SETTINGS);
    public static final String ACTION_ASK_UPDATED = MODULE_PACKAGE + ".action.ASK_UPDATED";
    public static final String ACTION_ASK_DECISION = MODULE_PACKAGE + ".action.ASK_DECISION";
    public static final String ACTION_RULES_UPDATED = MODULE_PACKAGE + ".action.RULES_UPDATED";
    public static final String COLUMN_RULE_TEXT = "rule_text";
    public static final String COLUMN_ASK_REQUEST_ID = "ask_request_id";
    public static final String COLUMN_ASK_SOURCE_PACKAGE = "ask_source_package";
    public static final String COLUMN_ASK_SOURCE_CLASS = "ask_source_class";
    public static final String COLUMN_ASK_TARGET_PACKAGE = "ask_target_package";
    public static final String COLUMN_ASK_TARGET_CLASS = "ask_target_class";
    public static final String COLUMN_ASK_INTENT_URI = "ask_intent_uri";
    public static final String COLUMN_ASK_EXPIRES_AT = "ask_expires_at";
    public static final String COLUMN_PENDING_SOURCE_PACKAGE = "source_package";
    public static final String COLUMN_PENDING_TARGET_PACKAGE = "target_package";
    public static final String COLUMN_PENDING_TARGET_CLASS = "target_class";
    public static final String COLUMN_PENDING_INTENT_URI = "intent_uri";
    public static final String COLUMN_PENDING_EXPIRES_AT = "expires_at";
    public static final String COLUMN_RECENT_RECORDS = "recent_records";
    public static final String COLUMN_RECORD_TIME = "record_time";
    public static final String COLUMN_RECORD_ACTION = "record_action";
    public static final String COLUMN_RECORD_SOURCE_PACKAGE = "record_source_package";
    public static final String COLUMN_RECORD_SOURCE_CLASS = "record_source_class";
    public static final String COLUMN_RECORD_TARGET_PACKAGE = "record_target_package";
    public static final String COLUMN_RECORD_TARGET_CLASS = "record_target_class";
    public static final String COLUMN_DIAGNOSTIC_LOG_ENABLED = "diagnostic_log_enabled";
    public static final String KEY_RECENT_RECORDS = "recent_records";
    public static final long CACHE_TTL_MS = 5000L;
    public static final long ALLOW_RECORD_TTL_MS = 4000L;
    public static final long PENDING_ALLOW_TTL_MS = 8000L;
    public static final long ASK_TIMEOUT_MS = 15000L;
    public static final int MAX_ALLOW_RECORDS = 16;
    public static final int MAX_RECENT_RECORDS = 50;
    public static final String EXTRA_INTERNAL_REPLAY =
            MODULE_PACKAGE + ".extra.INTERNAL_REPLAY";
    public static final String EXTRA_ASK_REQUEST_ID =
            MODULE_PACKAGE + ".extra.ASK_REQUEST_ID";
    public static final String EXTRA_ASK_DECISION =
            MODULE_PACKAGE + ".extra.ASK_DECISION";
    public static final String ASK_DECISION_APPROVE = "approve";
    public static final String ASK_DECISION_REJECT = "reject";
    public static final String DEFAULT_RULES = "";

    private ModuleConfig() {
    }
}
