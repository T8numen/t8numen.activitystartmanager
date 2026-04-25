package t8numen.activitystartmanager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.Process;

import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    private static final String CLASS_ACTIVITY_STARTER = "com.android.server.wm.ActivityStarter";
    private static final String CLASS_ACTIVITY_STARTER_REQUEST = "com.android.server.wm.ActivityStarter$Request";
    private static final String CLASS_ACTIVITY_RECORD = "com.android.server.wm.ActivityRecord";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (HookPackagePolicy.PACKAGE_ANDROID.equals(lpparam.packageName)) {
            hookActivityStarter(lpparam.classLoader);
            return;
        }
        if (HookPackagePolicy.isPermissionUiPackage(lpparam.packageName)) {
            PermissionPromptHook.hook(lpparam.packageName);
        }
    }

    private void hookActivityStarter(ClassLoader classLoader) {
        try {
            Class<?> activityStarterClass = XposedHelpers.findClass(CLASS_ACTIVITY_STARTER, classLoader);
            Class<?> requestClass = XposedHelpers.findClass(CLASS_ACTIVITY_STARTER_REQUEST, classLoader);
            Class<?> activityRecordClass = XposedHelpers.findClass(CLASS_ACTIVITY_RECORD, classLoader);
            XposedHelpers.findAndHookMethod(activityStarterClass, "executeRequest", requestClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    handleBeforeExecuteRequest(param, activityRecordClass);
                }
            });
            LogUtil.writeLog("hooked android ActivityStarter.executeRequest");
            warmUpRules(classLoader);
        } catch (Throwable throwable) {
            LogUtil.writeLog("hook android ActivityStarter.executeRequest failed: " + throwable);
        }
    }

    private void warmUpRules(ClassLoader classLoader) {
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < 30; attempt++) {
                Context context = getSystemContextFromActivityThread(classLoader);
                if (context != null) {
                    SystemServerBridge.ensureReceivers(context);
                    RuleRepository.forceRefreshAsync(context);
                    ModuleSettingsStore.forceRefreshAsync(context);
                    LogUtil.writeLog("rule cache warmup requested");
                    return;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
            LogUtil.writeLog("rule cache warmup skipped: system context unavailable");
        }, "ALC-rule-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    private Context getSystemContextFromActivityThread(ClassLoader classLoader) {
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
            if (activityThread == null) {
                return null;
            }
            return (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
        } catch (Throwable throwable) {
            return null;
        }
    }

    private void handleBeforeExecuteRequest(XC_MethodHook.MethodHookParam param, Class<?> activityRecordClass) {
        ActivityStarterRequestCompat request = ActivityStarterRequestCompat.from(param.args[0]);
        if (request == null) {
            return;
        }

        Intent intent = request.getIntent();
        ActivityInfo activityInfo = request.getActivityInfo();
        Object resolveInfo = request.getResolveInfo();
        String callingPackage = request.getCallingPackage();
        IBinder resultTo = request.getResultTo();
        Context systemContext = getSystemContext(param.thisObject);
        if (systemContext == null || intent == null) {
            return;
        }
        SystemServerBridge.ensureReceivers(systemContext);

        boolean internalReplay = intent.getBooleanExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY, false);
        ActivityRef sourceRef = resolveSourceRef(activityRecordClass, resultTo, callingPackage);
        if (sourceRef == null) {
            sourceRef = resolveSourceRefFromCallingUid(systemContext, request);
        }
        ActivityRef targetRef = resolveTargetRef(intent, activityInfo, resolveInfo);
        sourceRef = markSystemApp(systemContext, sourceRef);
        targetRef = markSystemApp(systemContext, targetRef);
        if (internalReplay) {
            String askRequestId = intent.getStringExtra(ModuleConfig.EXTRA_ASK_REQUEST_ID);
            if (PermissionBypassController.restoreAskReplay(systemContext, request, intent, askRequestId)) {
                return;
            }
        }
        if (sourceRef == null && internalReplay) {
            sourceRef = ActivityRef.fromPackage(callingPackage == null ? ModuleConfig.MODULE_PACKAGE : callingPackage);
        }
        if (sourceRef == null || targetRef == null) {
            return;
        }

        if (internalReplay) {
            PermissionBypassController.recordInternalReplayAllowed(
                    systemContext,
                    request,
                    sourceRef,
                    targetRef,
                    intent,
                    activityInfo,
                    resolveInfo,
                    callingPackage
            );
            return;
        }

        if (PermissionBypassController.tryBypassPermissionPrompt(
                systemContext,
                request,
                sourceRef,
                targetRef,
                callingPackage
        )) {
            return;
        }

        List<ActivityLaunchRule> rules = RuleRepository.loadRulesFromProvider(systemContext);
        ActivityLaunchRule matchedRule = RuleParser.findEffectiveMatch(rules, sourceRef, targetRef);
        if (matchedRule == null) {
            return;
        }

        RuleAction action = matchedRule.getAction();
        LogUtil.writeDiagnosticLog("rule " + action + " matched: "
                + sourceRef.getDisplayName() + " -> " + targetRef.getDisplayName());
        if (action == RuleAction.DISAGREE) {
            RecentLaunchRecordStore.saveAsync(systemContext, "DISAGREE", sourceRef, targetRef);
            param.setResult(Integer.valueOf(FrameworkCompat.getStartAborted()));
            return;
        }
        if (action == RuleAction.AGREE) {
            RecentLaunchRecordStore.saveAsync(systemContext, "AGREE", sourceRef, targetRef);
            PermissionBypassController.recordAgree(
                    systemContext,
                    sourceRef,
                    targetRef,
                    intent,
                    activityInfo,
                    resolveInfo,
                    callingPackage
            );
            return;
        }
        if (HookPackagePolicy.isAskExempt(sourceRef, targetRef, matchedRule)) {
            RecentLaunchRecordStore.saveAsync(systemContext, "ASK_EXEMPT", sourceRef, targetRef);
            LogUtil.writeDiagnosticLog("ask exempted: " + describeRef(sourceRef) + " -> " + describeRef(targetRef));
            return;
        }
        Intent askIntent = new Intent(intent);
        askIntent.removeExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY);
        String requestId = PendingAskStore.newRequestId();
        AskLaunchRegistry.record(
                requestId,
                sourceRef,
                targetRef,
                askIntent,
                activityInfo,
                resolveInfo,
                callingPackage
        );
        RecentLaunchRecordStore.saveAsync(systemContext, "ASK", sourceRef, targetRef);
        PendingAskStore.saveAsync(systemContext, requestId, sourceRef, targetRef, askIntent);
        param.setResult(Integer.valueOf(FrameworkCompat.getStartAborted()));
        LogUtil.writeDiagnosticLog("ask pending saved and original launch aborted request=" + requestId + ": "
                + describeRef(sourceRef) + " -> " + describeRef(targetRef));
    }

    private Context getSystemContext(Object activityStarter) {
        try {
            Object service = XposedHelpers.getObjectField(activityStarter, "mService");
            return (Context) XposedHelpers.getObjectField(service, "mContext");
        } catch (Throwable throwable) {
            LogUtil.writeLog("get system context failed: " + throwable);
            return null;
        }
    }

    private ActivityRef resolveSourceRef(Class<?> activityRecordClass, IBinder resultTo, String callingPackage) {
        Object sourceRecord = null;
        if (resultTo != null) {
            try {
                sourceRecord = XposedHelpers.callStaticMethod(activityRecordClass, "isInAnyTask", resultTo);
            } catch (Throwable throwable) {
                LogUtil.writeLog("resolve source activity from resultTo failed: " + throwable);
            }
        }
        if (sourceRecord != null) {
            String shortComponentName = (String) XposedHelpers.getObjectField(sourceRecord, "shortComponentName");
            String packageName = (String) XposedHelpers.getObjectField(sourceRecord, "packageName");
            return ActivityRef.fromShortComponent(packageName, shortComponentName);
        }
        return ActivityRef.fromPackage(callingPackage);
    }

    private ActivityRef resolveSourceRefFromCallingUid(Context context, ActivityStarterRequestCompat request) {
        int callingUid = request == null ? -1 : request.getCallingUid();
        if (context == null || callingUid < 0) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            String[] packages = packageManager.getPackagesForUid(callingUid);
            if (packages != null && packages.length > 0) {
                return ActivityRef.fromPackage(packages[0])
                        .withSystemApp(isSystemUid(callingUid) || isSystemPackage(context, packages[0]));
            }
        } catch (Throwable throwable) {
            LogUtil.writeLog("resolve source package from uid failed: " + throwable);
        }
        return null;
    }

    private ActivityRef markSystemApp(Context context, ActivityRef ref) {
        if (ref == null || ref.isSystemApp()) {
            return ref;
        }
        return ref.withSystemApp(isSystemPackage(context, ref.getPackageName()));
    }

    private boolean isSystemPackage(Context context, String packageName) {
        if (context == null || packageName == null) {
            return false;
        }
        if (HookPackagePolicy.PACKAGE_ANDROID.equals(packageName)) {
            return true;
        }
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
            return isSystemUid(applicationInfo.uid)
                    || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    || (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private boolean isSystemUid(int uid) {
        return uid >= 0 && uid < Process.FIRST_APPLICATION_UID;
    }

    private ActivityRef resolveTargetRef(Intent intent, ActivityInfo activityInfo, Object resolveInfo) {
        if (activityInfo != null) {
            return ActivityRef.fromActivityInfo(activityInfo);
        }
        ActivityInfo resolvedActivityInfo = resolveActivityInfo(resolveInfo);
        if (resolvedActivityInfo != null) {
            return ActivityRef.fromActivityInfo(resolvedActivityInfo);
        }
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            return ActivityRef.fromComponent(componentName);
        }
        if (intent.getPackage() != null) {
            return ActivityRef.fromPackage(intent.getPackage());
        }
        return null;
    }

    private ActivityInfo resolveActivityInfo(Object resolveInfo) {
        if (resolveInfo instanceof ResolveInfo) {
            return ((ResolveInfo) resolveInfo).activityInfo;
        }
        try {
            Object value = XposedHelpers.getObjectField(resolveInfo, "activityInfo");
            if (value instanceof ActivityInfo) {
                return (ActivityInfo) value;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String describeRef(ActivityRef ref) {
        if (ref == null) {
            return "<null>";
        }
        return ref.getDisplayName();
    }

}
