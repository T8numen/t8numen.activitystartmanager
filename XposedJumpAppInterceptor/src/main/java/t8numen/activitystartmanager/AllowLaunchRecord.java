package t8numen.activitystartmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;

final class AllowLaunchRecord {
    private final String sourcePackage;
    private final ActivityRef targetRef;
    private final Intent intent;
    private final ActivityInfo activityInfo;
    private final Object resolveInfo;
    private final String callingPackage;
    private final long createdAt;

    AllowLaunchRecord(String sourcePackage, ActivityRef targetRef, Intent intent, ActivityInfo activityInfo,
            Object resolveInfo, String callingPackage) {
        this.sourcePackage = sourcePackage;
        this.targetRef = targetRef;
        this.intent = intent;
        this.activityInfo = activityInfo;
        this.resolveInfo = resolveInfo;
        this.callingPackage = callingPackage;
        this.createdAt = SystemClock.elapsedRealtime();
    }

    String getSourcePackage() {
        return sourcePackage;
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
        return SystemClock.elapsedRealtime() - createdAt > ModuleConfig.ALLOW_RECORD_TTL_MS;
    }
}
