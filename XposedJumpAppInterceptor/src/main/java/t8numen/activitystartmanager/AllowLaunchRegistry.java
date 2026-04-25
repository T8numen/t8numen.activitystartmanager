package t8numen.activitystartmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class AllowLaunchRegistry {
    private static final Object LOCK = new Object();
    private static final ArrayDeque<AllowLaunchRecord> RECORDS = new ArrayDeque<>();
    private static final Set<String> PERMISSION_MANAGER_PACKAGES = new HashSet<>();
    private static final String[] PERMISSION_MANAGER_PREFIXES = new String[]{
            "com.oplus.",
            "com.coloros.",
            "com.oppo."
    };

    static {
        PERMISSION_MANAGER_PACKAGES.add("com.android.permissioncontroller");
        PERMISSION_MANAGER_PACKAGES.add("com.oplus.securitypermission");
        PERMISSION_MANAGER_PACKAGES.add("com.coloros.securitypermission");
        PERMISSION_MANAGER_PACKAGES.add("com.coloros.safecenter");
        PERMISSION_MANAGER_PACKAGES.add("com.coloros.safe");
        PERMISSION_MANAGER_PACKAGES.add("com.oppo.safe");
    }

    private AllowLaunchRegistry() {
    }

    static void recordAllow(ActivityRef sourceRef, ActivityRef targetRef, Intent intent, ActivityInfo activityInfo,
            Object resolveInfo, String callingPackage) {
        if (sourceRef == null || targetRef == null || intent == null) {
            return;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            if (RECORDS.size() >= ModuleConfig.MAX_ALLOW_RECORDS) {
                RECORDS.removeFirst();
            }
            RECORDS.addLast(new AllowLaunchRecord(
                    sourceRef.getPackageName(),
                    targetRef,
                    intent,
                    activityInfo,
                    resolveInfo,
                    callingPackage
            ));
        }
    }

    static AllowLaunchRecord findForPermissionPrompt(ActivityRef sourceRef, ActivityRef targetRef, String callingPackage) {
        if (targetRef == null || !PERMISSION_MANAGER_PACKAGES.contains(targetRef.getPackageName())) {
            return null;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            Iterator<AllowLaunchRecord> iterator = RECORDS.descendingIterator();
            while (iterator.hasNext()) {
                AllowLaunchRecord record = iterator.next();
                String sourcePackage = sourceRef == null ? null : sourceRef.getPackageName();
                if (matches(record, sourcePackage, callingPackage)) {
                    iterator.remove();
                    return record;
                }
            }
        }
        return null;
    }

    static String describePendingCandidate(ActivityRef sourceRef, ActivityRef targetRef, String callingPackage) {
        if (targetRef == null || targetRef.getPackageName() == null) {
            return null;
        }
        if (!looksLikePermissionManager(targetRef.getPackageName())) {
            return null;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            Iterator<AllowLaunchRecord> iterator = RECORDS.descendingIterator();
            while (iterator.hasNext()) {
                AllowLaunchRecord record = iterator.next();
                String sourcePackage = sourceRef == null ? null : sourceRef.getPackageName();
                if (matches(record, sourcePackage, callingPackage)) {
                    return record.getTargetRef().getDisplayName();
                }
            }
        }
        return null;
    }

    private static boolean matches(AllowLaunchRecord record, String sourcePackage, String callingPackage) {
        if (record == null) {
            return false;
        }
        if (sourcePackage != null && sourcePackage.equals(record.getSourcePackage())) {
            return true;
        }
        return callingPackage != null && callingPackage.equals(record.getSourcePackage());
    }

    private static void pruneExpiredLocked() {
        while (!RECORDS.isEmpty() && RECORDS.peekFirst().isExpired()) {
            RECORDS.removeFirst();
        }
    }

    private static boolean looksLikePermissionManager(String packageName) {
        if (PERMISSION_MANAGER_PACKAGES.contains(packageName)) {
            return true;
        }
        for (String prefix : PERMISSION_MANAGER_PREFIXES) {
            if (packageName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
