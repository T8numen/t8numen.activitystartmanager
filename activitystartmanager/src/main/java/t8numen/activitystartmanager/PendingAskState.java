package t8numen.activitystartmanager;

import android.os.SystemClock;

final class PendingAskState {
    private final String requestId;
    private final ActivityRef sourceRef;
    private final ActivityRef targetRef;
    private final String intentUri;
    private final long expiresAt;

    PendingAskState(String requestId, ActivityRef sourceRef, ActivityRef targetRef, String intentUri, long expiresAt) {
        this.requestId = requestId;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
        this.intentUri = intentUri;
        this.expiresAt = expiresAt;
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

    String getIntentUri() {
        return intentUri;
    }

    long getExpiresAt() {
        return expiresAt;
    }

    boolean isExpired() {
        return expiresAt <= SystemClock.elapsedRealtime();
    }
}
