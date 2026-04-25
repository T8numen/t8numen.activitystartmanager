package t8numen.activitystartmanager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.IBinder;

import de.robv.android.xposed.XposedHelpers;

final class ActivityStarterRequestCompat {
    private final Object request;

    private ActivityStarterRequestCompat(Object request) {
        this.request = request;
    }

    static ActivityStarterRequestCompat from(Object request) {
        if (request == null) {
            return null;
        }
        return new ActivityStarterRequestCompat(request);
    }

    Intent getIntent() {
        return (Intent) safeGetField("intent");
    }

    ActivityInfo getActivityInfo() {
        return (ActivityInfo) safeGetField("activityInfo");
    }

    Object getResolveInfo() {
        return safeGetField("resolveInfo");
    }

    String getCallingPackage() {
        return (String) safeGetField("callingPackage");
    }

    IBinder getResultTo() {
        return (IBinder) safeGetField("resultTo");
    }

    int getCallingUid() {
        int callingUid = safeGetIntField("callingUid", -1);
        if (callingUid < 0) {
            callingUid = safeGetIntField("realCallingUid", -1);
        }
        return callingUid;
    }

    void setIntent(Intent intent) {
        safeSetField("intent", intent);
    }

    void rewrite(AllowLaunchRecord record) {
        Intent rewrittenIntent = record.getIntent();
        ActivityInfo rewrittenInfo = record.getActivityInfo();
        if (rewrittenInfo != null && rewrittenIntent.getComponent() == null) {
            rewrittenIntent.setComponent(new ComponentName(
                    rewrittenInfo.packageName,
                    ActivityRef.normalizeClassName(rewrittenInfo.packageName, rewrittenInfo.name)
            ));
        }
        safeSetField("intent", rewrittenIntent);
        safeSetField("activityInfo", rewrittenInfo);
        safeSetField("resolveInfo", record.getResolveInfo());
        if (record.getCallingPackage() != null) {
            safeSetField("callingPackage", record.getCallingPackage());
        }
    }

    private Object safeGetField(String fieldName) {
        try {
            return XposedHelpers.getObjectField(request, fieldName);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private int safeGetIntField(String fieldName, int fallback) {
        try {
            return XposedHelpers.getIntField(request, fieldName);
        } catch (Throwable throwable) {
            return fallback;
        }
    }

    private void safeSetField(String fieldName, Object value) {
        try {
            XposedHelpers.setObjectField(request, fieldName, value);
        } catch (Throwable throwable) {
            LogUtil.writeLog("set request field failed " + fieldName + ": " + throwable);
        }
    }
}
