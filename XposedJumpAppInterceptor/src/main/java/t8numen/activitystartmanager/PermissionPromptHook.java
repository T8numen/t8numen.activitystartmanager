package t8numen.activitystartmanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

final class PermissionPromptHook {
    private PermissionPromptHook() {
    }

    static void hook(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    if (!packageName.equals(activity.getPackageName())) {
                        return;
                    }
                    maybeReplayPendingLaunch(activity);
                }
            });
            LogUtil.writeLog("hooked permission UI package: " + packageName);
        } catch (Throwable throwable) {
            LogUtil.writeLog("hook permission UI package failed for " + packageName + ": " + throwable);
        }
    }

    private static void maybeReplayPendingLaunch(Activity activity) {
        PendingAllowState state = PendingAllowStore.peek(activity);
        if (state == null) {
            return;
        }

        String activityName = activity.getClass().getName();
        LogUtil.writeDiagnosticLog("diag.pm.ui_appeared package=" + activity.getPackageName()
                + " class=" + activityName
                + " sourcePackage=" + safeText(state.getSourcePackage())
                + " target=" + state.getTargetDisplayName());

        PendingAllowStore.clear(activity);
        try {
            Intent replayIntent = Intent.parseUri(state.getIntentUri(), Intent.URI_INTENT_SCHEME);
            if (replayIntent.getComponent() == null && state.getTargetPackage() != null) {
                if (state.getTargetClassName() != null && !state.getTargetClassName().isEmpty()) {
                    replayIntent.setComponent(new ComponentName(
                            state.getTargetPackage(),
                            state.getTargetClassName()
                    ));
                } else {
                    replayIntent.setPackage(state.getTargetPackage());
                }
            }
            replayIntent.putExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY, true);
            activity.startActivity(replayIntent);
            activity.finish();
            LogUtil.writeDiagnosticLog("diag.pm.ui_replayed package=" + activity.getPackageName()
                    + " class=" + activityName
                    + " target=" + state.getTargetDisplayName());
        } catch (Throwable throwable) {
            LogUtil.writeLog("diag.pm.ui_replay_failed package=" + activity.getPackageName()
                    + " class=" + activityName
                    + " error=" + throwable);
        }
    }

    private static String safeText(String value) {
        if (value == null || value.isEmpty()) {
            return "<null>";
        }
        return value;
    }
}
