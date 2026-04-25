package t8numen.activitystartmanager;

import android.os.SystemClock;

final class PendingAllowState {
    private final String sourcePackage;
    private final String targetPackage;
    private final String targetClassName;
    private final String intentUri;
    private final long expiresAt;

    PendingAllowState(String sourcePackage, String targetPackage, String targetClassName, String intentUri,
            long expiresAt) {
        this.sourcePackage = sourcePackage;
        this.targetPackage = targetPackage;
        this.targetClassName = targetClassName;
        this.intentUri = intentUri;
        this.expiresAt = expiresAt;
    }

    String getSourcePackage() {
        return sourcePackage;
    }

    String getTargetPackage() {
        return targetPackage;
    }

    String getTargetClassName() {
        return targetClassName;
    }

    String getIntentUri() {
        return intentUri;
    }

    boolean isExpired() {
        return SystemClock.elapsedRealtime() > expiresAt;
    }

    String getTargetDisplayName() {
        if (targetPackage == null) {
            return "<unknown>";
        }
        if (targetClassName == null || targetClassName.isEmpty()) {
            return targetPackage;
        }
        if (targetClassName.startsWith(targetPackage + ".")) {
            return targetPackage + "/" + targetClassName.substring(targetPackage.length());
        }
        return targetPackage + "/" + targetClassName;
    }
}
