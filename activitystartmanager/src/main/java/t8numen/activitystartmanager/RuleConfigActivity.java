package t8numen.activitystartmanager;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RuleConfigActivity extends Activity {
    private final List<BackgroundColorSpan> mConflictSpans = new ArrayList<>();
    private RuleEditText mRuleEditor;
    private PopupWindow mMorePopupWindow;
    private TextView mRuleHelpText;
    private TextView mRuleSummaryText;
    private TextView mRuleCheckResult;
    private TextView mRecentRecordsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OldRulesImportResult oldRulesImportResult = RuleRepository.importOldRulesIfNeeded(this);
        setContentView(R.layout.activity_rule_config);
        setTitle(R.string.app_name);

        TextView versionText = findViewById(R.id.version_text);
        View oldModuleWarningContainer = findViewById(R.id.old_module_warning_container);
        TextView oldModuleWarningClose = findViewById(R.id.old_module_warning_close);
        mRuleHelpText = findViewById(R.id.rule_help_text);
        mRuleSummaryText = findViewById(R.id.rule_summary_text);
        mRuleCheckResult = findViewById(R.id.rule_check_result);
        mRecentRecordsText = findViewById(R.id.recent_records_text);
        mRuleEditor = findViewById(R.id.rule_editor);
        HorizontalScrollView ruleEditorScroll = findViewById(R.id.rule_editor_horizontal_scroll);
        Button moreButton = findViewById(R.id.more_button);
        Button saveButton = findViewById(R.id.save_button);
        Button checkButton = findViewById(R.id.check_button);

        versionText.setText("v" + getVersionName());
        oldModuleWarningContainer.setVisibility(shouldShowOldModuleWarning() ? View.VISIBLE : View.GONE);
        oldModuleWarningClose.setOnClickListener(v -> dismissOldModuleWarning(oldModuleWarningContainer));
        mRuleEditor.setText(RuleRepository.loadRulesFromLocalPrefs(this));
        mRuleEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateRuleSummary();
            }
        });
        updateRuleSummary();
        installEditorBlankAreaZoom(ruleEditorScroll);
        moreButton.setOnClickListener(v -> toggleMoreMenu(moreButton));
        saveButton.setOnClickListener(v -> saveRules());
        checkButton.setOnClickListener(v -> checkRules());
        if (oldRulesImportResult.changedRules()) {
            Toast.makeText(this, oldRuleImportMessage(oldRulesImportResult), Toast.LENGTH_SHORT).show();
        }
    }

    private void installEditorBlankAreaZoom(HorizontalScrollView ruleEditorScroll) {
        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(
                this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        mRuleEditor.scaleText(detector.getScaleFactor());
                        return true;
                    }
                }
        );
        ruleEditorScroll.setOnTouchListener((view, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            boolean multiTouch = event.getPointerCount() > 1;
            if (multiTouch) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                view.getParent().requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        dismissMoreMenu();
        super.onDestroy();
    }

    private void saveRules() {
        String rawRules = mRuleEditor.getText().toString();
        List<RuleConflict> conflicts = RuleParser.findConflicts(RuleParser.parse(rawRules));
        RuleRepository.saveRulesToLocalPrefs(this, rawRules);
        if (conflicts.isEmpty()) {
            Toast.makeText(this, R.string.rules_saved, Toast.LENGTH_SHORT).show();
        } else {
            highlightConflictLines(conflicts);
            Toast.makeText(this,
                    getString(R.string.rules_saved_conflicts, conflicts.size()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleMoreMenu(View anchor) {
        if (mMorePopupWindow != null && mMorePopupWindow.isShowing()) {
            dismissMoreMenu();
            return;
        }
        showMoreMenu(anchor);
    }

    private void showMoreMenu(View anchor) {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundColor(Color.WHITE);
        menu.setPadding(0, dp(4), 0, dp(4));

        addMenuItem(menu, getString(helpMenuText()), this::toggleHelpText);
        addMenuItem(menu, getString(overlayMenuText()), this::openOverlaySettings);
        addMenuItem(menu, getString(R.string.format_rules), this::formatRules);
        addMenuItem(menu, getString(R.string.copy_rules), this::copyRules);
        addMenuItem(menu, getString(R.string.export_rules), this::exportRules);
        addMenuItem(menu, getString(R.string.import_old_rules), this::importOldRules);
        addMenuItem(menu, getString(recentRecordsMenuText()), this::toggleRecentRecords);
        addMenuItem(menu, getString(R.string.clear_recent_records), this::clearRecentRecords);
        addMenuItem(menu, getString(diagnosticLogMenuText()), this::toggleDiagnosticLog);

        PopupWindow popupWindow = new PopupWindow(
                menu,
                dp(176),
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popupWindow.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= 21) {
            popupWindow.setElevation(dp(8));
        }
        popupWindow.setOnDismissListener(() -> mMorePopupWindow = null);
        mMorePopupWindow = popupWindow;
        popupWindow.showAsDropDown(anchor, 0, 0, Gravity.END);
    }

    private void addMenuItem(LinearLayout menu, String text, Runnable action) {
        if (menu.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(Color.rgb(224, 224, 224));
            menu.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            ));
        }
        TextView item = new TextView(this);
        item.setText(text);
        item.setTextColor(Color.rgb(33, 33, 33));
        item.setTextSize(15);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), 0, dp(16), 0);
        item.setOnClickListener(v -> {
            dismissMoreMenu();
            action.run();
        });
        menu.addView(item, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        ));
    }

    private void dismissMoreMenu() {
        if (mMorePopupWindow != null) {
            mMorePopupWindow.dismiss();
            mMorePopupWindow = null;
        }
    }

    private int helpMenuText() {
        return mRuleHelpText.getVisibility() == View.VISIBLE
                ? R.string.hide_rule_help
                : R.string.show_rule_help;
    }

    private int recentRecordsMenuText() {
        return mRecentRecordsText.getVisibility() == View.VISIBLE
                ? R.string.hide_recent_records
                : R.string.show_recent_records;
    }

    private int diagnosticLogMenuText() {
        return ModuleSettingsStore.loadDiagnosticLogEnabledLocal(this)
                ? R.string.diagnostic_log_on
                : R.string.diagnostic_log_off;
    }

    private int overlayMenuText() {
        return Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(this)
                ? R.string.overlay_permission_granted
                : R.string.overlay_permission;
    }

    private void toggleHelpText() {
        boolean show = mRuleHelpText.getVisibility() != View.VISIBLE;
        mRuleHelpText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void toggleRecentRecords() {
        boolean show = mRecentRecordsText.getVisibility() != View.VISIBLE;
        if (show) {
            refreshRecentRecords();
        }
        mRecentRecordsText.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void refreshRecentRecords() {
        mRecentRecordsText.setText(formatRecentRecords(RecentLaunchRecordStore.loadLocal(this)));
    }

    private void clearRecentRecords() {
        RecentLaunchRecordStore.clearLocal(this);
        if (mRecentRecordsText.getVisibility() == View.VISIBLE) {
            refreshRecentRecords();
        }
        Toast.makeText(this, R.string.recent_records_cleared, Toast.LENGTH_SHORT).show();
    }

    private void toggleDiagnosticLog() {
        boolean enabled = !ModuleSettingsStore.loadDiagnosticLogEnabledLocal(this);
        ModuleSettingsStore.saveDiagnosticLogEnabledLocal(this, enabled);
        Toast.makeText(this,
                enabled ? R.string.diagnostic_log_enabled : R.string.diagnostic_log_disabled,
                Toast.LENGTH_SHORT).show();
    }

    private void updateRuleSummary() {
        if (mRuleSummaryText == null || mRuleEditor == null) {
            return;
        }
        RuleStats stats = buildRuleStats(mRuleEditor.getText().toString());
        mRuleSummaryText.setText(getString(
                R.string.rule_summary_format,
                stats.validCount,
                stats.agreeCount,
                stats.disagreeCount,
                stats.askCount,
                stats.invalidCount,
                stats.conflictCount
        ));
    }

    private void formatRules() {
        mRuleEditor.setText(formatRuleText(mRuleEditor.getText().toString()));
        Toast.makeText(this, R.string.rules_formatted, Toast.LENGTH_SHORT).show();
    }

    private void checkRules() {
        clearConflictHighlights();
        List<Integer> invalidLines = findInvalidLineNumbers(mRuleEditor.getText().toString());
        List<RuleConflict> conflicts = RuleParser.findConflicts(RuleParser.parse(mRuleEditor.getText().toString()));
        StringBuilder result = new StringBuilder();
        if (invalidLines.isEmpty() && conflicts.isEmpty()) {
            result.append(getString(R.string.rules_check_ok));
            Toast.makeText(this, R.string.rules_check_ok, Toast.LENGTH_SHORT).show();
        } else {
            if (!invalidLines.isEmpty()) {
                result.append(getString(R.string.rules_check_invalid, joinLineNumbers(invalidLines)));
                Toast.makeText(this,
                        getString(R.string.rules_check_invalid, joinLineNumbers(invalidLines)),
                        Toast.LENGTH_SHORT).show();
            }
            if (!conflicts.isEmpty()) {
                if (result.length() > 0) {
                    result.append('\n');
                }
                result.append(getString(R.string.rules_check_conflicts, conflicts.size()));
                highlightConflictLines(conflicts);
            }
        }
        mRuleCheckResult.setText(result.toString());
    }

    private void copyRules() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), mRuleEditor.getText().toString()));
        Toast.makeText(this, R.string.rules_copied, Toast.LENGTH_SHORT).show();
    }

    private void exportRules() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, mRuleEditor.getText().toString());
        startActivity(Intent.createChooser(intent, getString(R.string.export_rules_title)));
    }

    private void importOldRules() {
        OldRulesImportResult result = RuleRepository.importOldRules(this);
        if (result.changedRules()) {
            mRuleEditor.setText(RuleRepository.loadRulesFromLocalPrefs(this));
        }
        Toast.makeText(this, oldRuleImportMessage(result), Toast.LENGTH_SHORT).show();
    }

    private String formatRuleText(String rawRules) {
        if (rawRules == null || rawRules.trim().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String[] lines = rawRules.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String formatted = trimmed;
            if (!trimmed.startsWith("#")) {
                String[] parts = trimmed.split("\\s+");
                if (parts.length == 3 && RuleAction.fromToken(parts[0]) != null) {
                    formatted = canonicalAction(parts[0]) + " " + parts[1] + " " + parts[2];
                }
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(formatted);
        }
        return builder.toString();
    }

    private List<Integer> findInvalidLineNumbers(String rawRules) {
        List<Integer> invalidLines = new ArrayList<>();
        if (rawRules == null || rawRules.isEmpty()) {
            return invalidLines;
        }
        String[] lines = rawRules.split("\\r?\\n");
        for (int index = 0; index < lines.length; index++) {
            String trimmed = lines[index] == null ? "" : lines[index].trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 3 || RuleAction.fromToken(parts[0]) == null) {
                invalidLines.add(Integer.valueOf(index + 1));
            }
        }
        return invalidLines;
    }

    private String joinLineNumbers(List<Integer> lineNumbers) {
        StringBuilder builder = new StringBuilder();
        for (Integer lineNumber : lineNumbers) {
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(lineNumber);
        }
        if (builder.length() == 0) {
            return "";
        }
        return "第 " + builder + " 行";
    }

    private void highlightConflictLines(List<RuleConflict> conflicts) {
        clearConflictHighlights();
        Set<Integer> lineNumbers = new HashSet<>();
        for (RuleConflict conflict : conflicts) {
            lineNumbers.add(Integer.valueOf(conflict.getPrimaryLineNumber()));
            lineNumbers.add(Integer.valueOf(conflict.getRelatedLineNumber()));
        }
        Editable editable = mRuleEditor.getText();
        for (Integer lineNumber : lineNumbers) {
            int start = findLineStart(editable, lineNumber.intValue());
            int end = findLineEnd(editable, start);
            if (start < 0 || end <= start) {
                continue;
            }
            BackgroundColorSpan span = new BackgroundColorSpan(Color.rgb(255, 236, 179));
            editable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mConflictSpans.add(span);
        }
        mRuleEditor.postDelayed(this::clearConflictHighlights, 1000L);
    }

    private void clearConflictHighlights() {
        Editable editable = mRuleEditor.getText();
        for (BackgroundColorSpan span : mConflictSpans) {
            editable.removeSpan(span);
        }
        mConflictSpans.clear();
    }

    private int findLineStart(CharSequence text, int lineNumber) {
        if (lineNumber <= 0) {
            return -1;
        }
        int currentLine = 1;
        int start = 0;
        while (currentLine < lineNumber && start < text.length()) {
            if (text.charAt(start) == '\n') {
                currentLine++;
            }
            start++;
        }
        return currentLine == lineNumber ? start : -1;
    }

    private int findLineEnd(CharSequence text, int start) {
        int end = start;
        while (end < text.length() && text.charAt(end) != '\n') {
            end++;
        }
        return end;
    }

    private void openOverlaySettings() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean isOldModuleInstalled() {
        try {
            getPackageManager().getPackageInfo(ModuleConfig.OLD_MODULE_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private String canonicalAction(String token) {
        if ("allow".equalsIgnoreCase(token)) {
            return "agree";
        }
        return token.toLowerCase();
    }

    private String oldRuleImportMessage(OldRulesImportResult result) {
        if (result == null) {
            return getString(R.string.old_rules_not_found);
        }
        switch (result.getStatus()) {
            case IMPORTED:
                return getString(R.string.old_rules_imported_count, result.getImportedCount());
            case MERGED:
                return getString(R.string.old_rules_merged_count, result.getImportedCount());
            case ALREADY_PRESENT:
                return getString(R.string.old_rules_already_present);
            case SKIPPED_EXISTING_RULES:
                return getString(R.string.old_rules_skipped_existing);
            case NOT_FOUND:
            default:
                return getString(R.string.old_rules_not_found);
        }
    }

    private String formatRecentRecords(String rawRecords) {
        if (rawRecords == null || rawRecords.trim().isEmpty()) {
            return getString(R.string.recent_records_empty);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        StringBuilder builder = new StringBuilder();
        String[] lines = rawRecords.split("\\r?\\n");
        for (String line : lines) {
            String[] parts = line == null ? new String[0] : line.split("\\t", -1);
            if (parts.length < 6) {
                continue;
            }
            long time = parseLong(parts[0]);
            if (time <= 0L) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(dateFormat.format(new Date(time)))
                    .append(' ')
                    .append(parts[1])
                    .append(' ')
                    .append(formatComponent(parts[2], parts[3]))
                    .append(" -> ")
                    .append(formatComponent(parts[4], parts[5]));
        }
        if (builder.length() == 0) {
            return getString(R.string.recent_records_empty);
        }
        return builder.toString();
    }

    private String formatComponent(String packageName, String className) {
        if (packageName == null || packageName.isEmpty()) {
            return "";
        }
        if (className == null || className.isEmpty()) {
            return packageName;
        }
        if (className.startsWith(packageName + ".")) {
            return packageName + "/." + className.substring(packageName.length() + 1);
        }
        return packageName + "/" + className;
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private RuleStats buildRuleStats(String rawRules) {
        RuleStats stats = new RuleStats();
        List<ActivityLaunchRule> rules = RuleParser.parse(rawRules);
        stats.validCount = rules.size();
        for (ActivityLaunchRule rule : rules) {
            if (rule.getAction() == RuleAction.AGREE) {
                stats.agreeCount++;
            } else if (rule.getAction() == RuleAction.DISAGREE) {
                stats.disagreeCount++;
            } else if (rule.getAction() == RuleAction.ASK) {
                stats.askCount++;
            }
        }
        stats.invalidCount = findInvalidLineNumbers(rawRules).size();
        stats.conflictCount = RuleParser.findConflicts(rules).size();
        return stats;
    }

    private static final class RuleStats {
        int validCount;
        int agreeCount;
        int disagreeCount;
        int askCount;
        int invalidCount;
        int conflictCount;
    }

    private boolean shouldShowOldModuleWarning() {
        return isOldModuleInstalled()
                && !getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(ModuleConfig.KEY_OLD_MODULE_WARNING_DISMISSED, false);
    }

    private void dismissOldModuleWarning(View oldModuleWarningContainer) {
        getSharedPreferences(ModuleConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(ModuleConfig.KEY_OLD_MODULE_WARNING_DISMISSED, true)
                .apply();
        oldModuleWarningContainer.setVisibility(View.GONE);
    }
}
