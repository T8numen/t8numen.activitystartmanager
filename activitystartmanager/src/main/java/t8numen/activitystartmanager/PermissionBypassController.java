package t8numen.activitystartmanager;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

final class PermissionBypassController {
    private PermissionBypassController() {
    }

    static boolean restoreAskReplay(Context context, ActivityStarterRequestCompat request, Intent intent, String requestId) {
        AskLaunchRecord askRecord = AskLaunchRegistry.take(requestId);
        if (askRecord == null) {
            return false;
        }
        intent.removeExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY);
        intent.removeExtra(ModuleConfig.EXTRA_ASK_REQUEST_ID);
        AllowLaunchRegistry.recordAllow(
                askRecord.getSourceRef(),
                askRecord.getTargetRef(),
                askRecord.getIntent(),
                askRecord.getActivityInfo(),
                askRecord.getResolveInfo(),
                askRecord.getCallingPackage()
        );
        PendingAllowStore.saveAsync(context, askRecord.getSourceRef(), askRecord.getTargetRef(), askRecord.getIntent());
        request.rewrite(new AllowLaunchRecord(
                askRecord.getSourceRef().getPackageName(),
                askRecord.getTargetRef(),
                askRecord.getIntent(),
                askRecord.getActivityInfo(),
                askRecord.getResolveInfo(),
                askRecord.getCallingPackage()
        ));
        LogUtil.writeDiagnosticLog("ask replay restored request=" + requestId
                + " source=" + describeRef(askRecord.getSourceRef())
                + " target=" + describeRef(askRecord.getTargetRef()));
        return true;
    }

    static void recordInternalReplayAllowed(
            Context context,
            ActivityStarterRequestCompat request,
            ActivityRef sourceRef,
            ActivityRef targetRef,
            Intent intent,
            ActivityInfo activityInfo,
            Object resolveInfo,
            String callingPackage
    ) {
        intent.removeExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY);
        request.setIntent(intent);
        Intent allowIntent = new Intent(intent);
        AllowLaunchRegistry.recordAllow(
                sourceRef,
                targetRef,
                allowIntent,
                activityInfo,
                resolveInfo,
                callingPackage
        );
        PendingAllowStore.saveAsync(context, sourceRef, targetRef, new Intent(intent));
        LogUtil.writeDiagnosticLog("diag.pm.internal_replay allowed source=" + describeRef(sourceRef)
                + " target=" + describeRef(targetRef));
    }

    static boolean tryBypassPermissionPrompt(
            Context context,
            ActivityStarterRequestCompat request,
            ActivityRef sourceRef,
            ActivityRef targetRef,
            String callingPackage
    ) {
        String pendingCandidate = AllowLaunchRegistry.describePendingCandidate(sourceRef, targetRef, callingPackage);
        if (pendingCandidate != null) {
            LogUtil.writeDiagnosticLog("diag.pm.detected source=" + describeRef(sourceRef)
                    + " target=" + describeRef(targetRef)
                    + " callingPackage=" + safeText(callingPackage)
                    + " candidate=" + pendingCandidate);
        }

        AllowLaunchRecord allowRecord = AllowLaunchRegistry.findForPermissionPrompt(sourceRef, targetRef, callingPackage);
        if (allowRecord == null) {
            return false;
        }
        PendingAllowStore.clearAsync(context);
        request.rewrite(allowRecord);
        LogUtil.writeDiagnosticLog("diag.pm.framework_bypass package=" + safeText(targetRef.getPackageName())
                + " class=" + safeText(targetRef.getClassName())
                + " source=" + describeRef(sourceRef)
                + " original=" + describeRef(allowRecord.getTargetRef()));
        return true;
    }

    static void recordAgree(
            Context context,
            ActivityRef sourceRef,
            ActivityRef targetRef,
            Intent intent,
            ActivityInfo activityInfo,
            Object resolveInfo,
            String callingPackage
    ) {
        Intent allowIntent = new Intent(intent);
        allowIntent.removeExtra(ModuleConfig.EXTRA_INTERNAL_REPLAY);
        AllowLaunchRegistry.recordAllow(
                sourceRef,
                targetRef,
                allowIntent,
                activityInfo,
                resolveInfo,
                callingPackage
        );
        PendingAllowStore.saveAsync(context, sourceRef, targetRef, allowIntent);
        LogUtil.writeDiagnosticLog("diag.pm.pending_saved source=" + describeRef(sourceRef)
                + " target=" + describeRef(targetRef)
                + " callingPackage=" + safeText(callingPackage));
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
