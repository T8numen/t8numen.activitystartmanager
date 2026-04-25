package t8numen.activitystartmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;

final class AskLaunchRecord {
    private final String requestId;
    private final ActivityRef sourceRef;
    private final ActivityRef targetRef;
    private final Intent intent;
    private final ActivityInfo activityInfo;
    private final Object resolveInfo;
    private final String callingPackage;
    private final long expiresAt;

    AskLaunchRecord(String requestId, ActivityRef sourceRef, ActivityRef targetRef, Intent intent,
            ActivityInfo activityInfo, Object resolveInfo, String callingPackage) {
        this.requestId = requestId;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
        this.intent = new Intent(intent);
        this.activityInfo = activityInfo;
        this.resolveInfo = resolveInfo;
        this.callingPackage = callingPackage;
        this.expiresAt = SystemClock.elapsedRealtime() + ModuleConfig.ASK_TIMEOUT_MS + 5000L;
    }

    String getRequestId() {
        return requestId;
    }

    ActivityRef getSourceRef() {
        return sourceRef;
    }

    ActivityRef getTargetRef() {
        return targetRef;
    }

    Intent getIntent() {
        return new Intent(intent);
    }

    ActivityInfo getActivityInfo() {
        return activityInfo;
    }

    Object getResolveInfo() {
        return resolveInfo;
    }

    String getCallingPackage() {
        return callingPackage;
    }

    boolean isExpired() {
        return expiresAt <= SystemClock.elapsedRealtime();
    }
}
