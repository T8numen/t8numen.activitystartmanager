package t8numen.activitystartmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RuleRepository {
    private static final Object sLock = new Object();
    private static long sLastLoadedAt;
    private static boolean sRefreshInFlight;
    private static String sCachedRuleText = ModuleConfig.DEFAULT_RULES;
    private static List<ActivityLaunchRule> sCachedRules =
            Collections.unmodifiableList(RuleParser.parse(ModuleConfig.DEFAULT_RULES));

    private RuleRepository() {
    }

    public static List<ActivityLaunchRule> loadRulesFromProvider(Context context) {
        requestRefreshIfNeeded(context);
        synchronized (sLock) {
            return sCachedRules;
        }
    }

    public static void forceRefreshAsync(Context context) {
        startRefresh(context, true);
    }

    public static void forceRefreshNow(Context context) {
        String ruleText = queryRulesOrNull(context, ModuleConfig.PROVIDER_URI);
        if (ruleText != null) {
            replaceCachedRules(ruleText);
            return;
        }
        synchronized (sLock) {
            sRefreshInFlight = false;
        }
    }

    public static void replaceCachedRules(String rawRules) {
        String safeRules = rawRules == null ? "" : rawRules;
        synchronized (sLock) {
            sCachedRuleText = safeRules;
            sCachedRules = Collections.unmodifiableList(RuleParser.parse(safeRules));
            sLastLoadedAt = SystemClock.elapsedRealtime();
            sRefreshInFlight = false;
        }
    }

    private static void requestRefreshIfNeeded(Context context) {
        startRefresh(context, false);
    }

    private static void startRefresh(Context context, boolean force) {
        long now = SystemClock.elapsedRealtime();
        synchronized (sLock) {
            if (sRefreshInFlight || (!force && now - sLastLoadedAt < ModuleConfig.CACHE_TTL_MS)) {
                return;
            }
            sRefreshInFlight = true;
            sLastLoadedAt = now;
        }
        Thread refreshThread = new Thread(() -> {
            try {
                String ruleText = queryRulesOrNull(context, ModuleConfig.PROVIDER_URI);
                if (ruleText == null) {
                    synchronized (sLock) {
                        sLastLoadedAt = 0L;
                    }
                    return;
                }
                synchronized (sLock) {
                    if (!ruleText.equals(sCachedRuleText)) {
                        sCachedRuleText = ruleText;
                        sCachedRules = Collections.unmodifiableList(RuleParser.parse(ruleText));
                    }
                    sLastLoadedAt = SystemClock.elapsedRealtime();
                }
            } finally {
                synchronized (sLock) {
                    sRefreshInFlight = false;
                }
            }
        }, "ALC-rule-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    public static String loadRulesFromLocalPrefs(Context context) {
        return getPreferences(context).getString(ModuleConfig.KEY_RULE_TEXT, ModuleConfig.DEFAULT_RULES);
    }

    public static OldRulesImportResult importOldRulesIfNeeded(Context context) {
        if (context == null) {
            return OldRulesImportResult.notFound();
        }
        SharedPreferences preferences = getPreferences(context);
        if (preferences.getBoolean(ModuleConfig.KEY_OLD_RULES_IMPORTED, false)) {
            return OldRulesImportResult.alreadyPresent();
        }
        if (!isBlank(loadRulesFromLocalPrefs(context))) {
            OldRulesImportResult result = OldRulesImportResult.skippedExistingRules();
            recordOldRulesImportResult(preferences, result);
            return result;
        }
        return importOldRules(context);
    }

    public static OldRulesImportResult importOldRules(Context context) {
        if (context == null) {
            return OldRulesImportResult.notFound();
        }
        List<String> oldRuleLines = cleanImportedRuleLines(queryRules(context, ModuleConfig.OLD_PROVIDER_URI));
        if (oldRuleLines.isEmpty()) {
            OldRulesImportResult result = OldRulesImportResult.notFound();
            recordOldRulesImportResult(getPreferences(context), result);
            return result;
        }
        String currentRules = loadRulesFromLocalPrefs(context);
        if (isBlank(currentRules)) {
            saveRulesToLocalPrefs(context, joinRuleLines(oldRuleLines));
            OldRulesImportResult result = OldRulesImportResult.imported(oldRuleLines.size());
            recordOldRulesImportResult(getPreferences(context), result);
            return result;
        }
        MergeResult mergeResult = mergeImportedRules(currentRules, oldRuleLines);
        if (mergeResult.getImportedCount() <= 0) {
            OldRulesImportResult result = OldRulesImportResult.alreadyPresent();
            recordOldRulesImportResult(getPreferences(context), result);
            return result;
        }
        saveRulesToLocalPrefs(context, mergeResult.getRuleText());
        OldRulesImportResult result = OldRulesImportResult.merged(mergeResult.getImportedCount());
        recordOldRulesImportResult(getPreferences(context), result);
        return result;
    }

    public static void saveRulesToLocalPrefs(Context context, String rawRules) {
        String safeRules = rawRules == null ? "" : rawRules;
        getPreferences(context).edit().putString(ModuleConfig.KEY_RULE_TEXT, safeRules).apply();
        replaceCachedRules(safeRules);
        try {
            context.getContentResolver().notifyChange(ModuleConfig.PROVIDER_URI, null);
            Intent intent = new Intent(ModuleConfig.ACTION_RULES_UPDATED);
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(intent);
        } catch (Throwable throwable) {
            LogUtil.writeLog("notify rules updated failed: " + throwable);
        }
    }

    private static String queryRules(Context context) {
        if (context == null) {
            return ModuleConfig.DEFAULT_RULES;
        }
        return queryRules(context, ModuleConfig.PROVIDER_URI);
    }

    private static String queryRules(Context context, android.net.Uri uri) {
        String rules = queryRulesOrNull(context, uri);
        return rules == null ? ModuleConfig.DEFAULT_RULES : rules;
    }

    private static String queryRulesOrNull(Context context, android.net.Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(ModuleConfig.COLUMN_RULE_TEXT);
                if (columnIndex >= 0) {
                    String value = cursor.getString(columnIndex);
                    return value == null ? "" : value;
                }
            }
        } catch (Throwable throwable) {
            LogUtil.writeLog("Failed to query rules from provider: " + throwable);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static List<String> cleanImportedRuleLines(String rawRules) {
        List<String> ruleLines = new ArrayList<>();
        if (rawRules == null || rawRules.trim().isEmpty()) {
            return ruleLines;
        }
        Set<String> seen = new LinkedHashSet<>();
        String[] lines = rawRules.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String normalized = normalizeRuleLine(trimmed);
            if (seen.add(normalized)) {
                ruleLines.add(normalized);
            }
        }
        return ruleLines;
    }

    private static MergeResult mergeImportedRules(String currentRules, List<String> importedRules) {
        String baseRules = trimTrailingLineBreaks(currentRules == null ? "" : currentRules);
        StringBuilder builder = new StringBuilder(baseRules);
        Set<String> existingRules = new LinkedHashSet<>();
        String[] currentLines = baseRules.split("\\r?\\n");
        for (String line : currentLines) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                existingRules.add(normalizeRuleLine(trimmed));
            }
        }
        int importedCount = 0;
        for (String importedRule : importedRules) {
            String normalized = normalizeRuleLine(importedRule);
            if (!existingRules.add(normalized)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(normalized);
            importedCount++;
        }
        return new MergeResult(builder.toString(), importedCount);
    }

    private static String joinRuleLines(List<String> ruleLines) {
        StringBuilder builder = new StringBuilder();
        for (String ruleLine : ruleLines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(ruleLine);
        }
        return builder.toString();
    }

    private static String normalizeRuleLine(String ruleLine) {
        return ruleLine == null ? "" : ruleLine.trim().replaceAll("\\s+", " ");
    }

    private static String trimTrailingLineBreaks(String value) {
        int end = value.length();
        while (end > 0) {
            char ch = value.charAt(end - 1);
            if (ch != '\n' && ch != '\r') {
                break;
            }
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void recordOldRulesImportResult(SharedPreferences preferences, OldRulesImportResult result) {
        if (preferences == null || result == null) {
            return;
        }
        boolean terminal = result.getStatus() != OldRulesImportResult.Status.NOT_FOUND;
        preferences.edit()
                .putBoolean(ModuleConfig.KEY_OLD_RULES_IMPORTED, terminal)
                .putString(ModuleConfig.KEY_OLD_RULES_IMPORT_STATUS, result.getStatus().name())
                .putInt(ModuleConfig.KEY_OLD_RULES_IMPORT_COUNT, result.getImportedCount())
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static final class MergeResult {
        private final String ruleText;
        private final int importedCount;

        MergeResult(String ruleText, int importedCount) {
            this.ruleText = ruleText;
            this.importedCount = importedCount;
        }

        String getRuleText() {
            return ruleText;
        }

        int getImportedCount() {
            return importedCount;
        }
    }
}
