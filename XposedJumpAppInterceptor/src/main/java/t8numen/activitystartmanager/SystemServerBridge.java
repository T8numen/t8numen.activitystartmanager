package t8numen.activitystartmanager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

final class SystemServerBridge {
    private static final Object RECEIVER_LOCK = new Object();
    private static boolean sReceiverRegistered;

    private SystemServerBridge() {
    }

    static void ensureReceivers(Context systemContext) {
        if (systemContext == null) {
            return;
        }
        synchronized (RECEIVER_LOCK) {
            if (sReceiverRegistered) {
                return;
            }
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ModuleConfig.ACTION_ASK_DECISION);
                filter.addAction(ModuleConfig.ACTION_RULES_UPDATED);
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context receiverContext, Intent intent) {
                        if (intent == null) {
                            return;
                        }
                        if (ModuleConfig.ACTION_RULES_UPDATED.equals(intent.getAction())) {
                            RuleRepository.forceRefreshNow(systemContext);
                            ModuleSettingsStore.forceRefreshNow(systemContext);
                            LogUtil.writeLog("rules cache refreshed by update broadcast");
                            return;
                        }
                        handleAskDecision(systemContext, intent);
                    }
                };
                if (Build.VERSION.SDK_INT >= 33) {
                    systemContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    systemContext.registerReceiver(receiver, filter);
                }
                systemContext.getContentResolver().registerContentObserver(
                        ModuleConfig.PROVIDER_URI,
                        false,
                        new ContentObserver(new Handler(Looper.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange) {
                                RuleRepository.forceRefreshAsync(systemContext);
                                LogUtil.writeLog("rules cache refresh requested by provider change");
                            }
                        }
                );
                systemContext.getContentResolver().registerContentObserver(
                        ModuleConfig.SETTINGS_URI,
                        false,
                        new ContentObserver(new Handler(Looper.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange) {
                                ModuleSettingsStore.forceRefreshAsync(systemContext);
                                LogUtil.writeLog("settings refresh requested by provider change");
                            }
                        }
                );
                sReceiverRegistered = true;
                LogUtil.writeLog("system receivers registered in system_server");
            } catch (Throwable throwable) {
                LogUtil.writeLog("system receivers register failed: " + throwable);
            }
        }
    }

    private static void handleAskDecision(Context systemContext, Intent intent) {
        if (intent == null) {
            return;
        }
        String requestId = intent.getStringExtra(ModuleConfig.EXTRA_ASK_REQUEST_ID);
        String decision = intent.getStringExtra(ModuleConfig.EXTRA_ASK_DECISION);
        if (requestId == null || requestId.isEmpty()) {
            LogUtil.writeLog("ask decision ignored: empty request id");
            return;
        }
        if (ModuleConfig.ASK_DECISION_APPROVE.equals(decision)) {
            approveAskRequest(systemContext, requestId);
            return;
        }
        if (ModuleConfig.ASK_DECISION_REJECT.equals(decision)) {
            AskLaunchRegistry.take(requestId);
            LogUtil.writeDiagnosticLog("ask decision rejected request=" + requestId);
            return;
        }
        LogUtil.writeDiagnosticLog("ask decision ignored request=" + requestId + " decision=" + safeText(decision));
    }

    private static void approveAskRequest(Context systemContext, String requestId) {
        AskLaunchRecord record = AskLaunchRegistry.peek(requestId);
        if (record == null) {
            LogUtil.writeLog("ask decision approve missing request=" + requestId);
            return;
        }
        if (record.isExpired()) {
            AskLaunchRegistry.take(requestId);
            LogUtil.writeLog("ask decision approve expired request=" + requestId);
            return;
        }
        try {
            Intent replayIntent = record.getIntent();
            ensureTarget(replayIntent, record.getTargetRef(), record.getActivityInfo());
            replayIntent.putExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY, true);
            replayIntent.putExtra(ModuleConfig.EXTRA_ASK_REQUEST_ID, requestId);
            replayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            systemContext.startActivity(replayIntent);
            LogUtil.writeDiagnosticLog("ask decision approved from system_server request=" + requestId
                    + " source=" + describeRef(record.getSourceRef())
                    + " target=" + describeRef(record.getTargetRef()));
        } catch (Throwable throwable) {
            LogUtil.writeLog("ask decision approve failed request=" + requestId + ": " + throwable);
        }
    }

    private static void ensureTarget(Intent intent, ActivityRef targetRef, ActivityInfo activityInfo) {
        if (intent == null || intent.getComponent() != null) {
            return;
        }
        if (activityInfo != null && activityInfo.packageName != null && activityInfo.name != null) {
            intent.setComponent(new ComponentName(
                    activityInfo.packageName,
                    ActivityRef.normalizeClassName(activityInfo.packageName, activityInfo.name)
            ));
            return;
        }
        if (targetRef == null || targetRef.getPackageName() == null) {
            return;
        }
        if (targetRef.getClassName() != null && !targetRef.getClassName().isEmpty()) {
            intent.setComponent(new ComponentName(targetRef.getPackageName(), targetRef.getClassName()));
        } else {
            intent.setPackage(targetRef.getPackageName());
        }
    }

    private static String describeRef(ActivityRef ref) {
        if (ref == null) {
            return "<null>";
        }
        return ref.getDisplayName();
    }

    private static String safeText(String value) {
        if (value == null || value.isEmpty()) {
            return "<null>";
        }
        return value;
    }
}
