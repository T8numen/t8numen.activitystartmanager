package t8numen.activitystartmanager;

import android.content.Intent;
import android.content.pm.ActivityInfo;

import java.util.ArrayDeque;
import java.util.Iterator;

final class AskLaunchRegistry {
    private static final Object LOCK = new Object();
    private static final ArrayDeque<AskLaunchRecord> RECORDS = new ArrayDeque<>();
    private static final int MAX_RECORDS = 8;

    private AskLaunchRegistry() {
    }

    static void record(String requestId, ActivityRef sourceRef, ActivityRef targetRef, Intent intent,
            ActivityInfo activityInfo, Object resolveInfo, String callingPackage) {
        if (requestId == null || sourceRef == null || targetRef == null || intent == null) {
            return;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            if (RECORDS.size() >= MAX_RECORDS) {
                RECORDS.removeFirst();
            }
            RECORDS.addLast(new AskLaunchRecord(
                    requestId,
                    sourceRef,
                    targetRef,
                    intent,
                    activityInfo,
                    resolveInfo,
                    callingPackage
            ));
        }
    }

    static AskLaunchRecord take(String requestId) {
        if (requestId == null) {
            return null;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            Iterator<AskLaunchRecord> iterator = RECORDS.descendingIterator();
            while (iterator.hasNext()) {
                AskLaunchRecord record = iterator.next();
                if (requestId.equals(record.getRequestId())) {
                    iterator.remove();
                    return record;
                }
            }
        }
        return null;
    }

    static AskLaunchRecord peek(String requestId) {
        if (requestId == null) {
            return null;
        }
        synchronized (LOCK) {
            pruneExpiredLocked();
            Iterator<AskLaunchRecord> iterator = RECORDS.descendingIterator();
            while (iterator.hasNext()) {
                AskLaunchRecord record = iterator.next();
                if (requestId.equals(record.getRequestId())) {
                    return record;
                }
            }
        }
        return null;
    }

    private static void pruneExpiredLocked() {
        Iterator<AskLaunchRecord> iterator = RECORDS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) {
                iterator.remove();
            }
        }
    }
}
