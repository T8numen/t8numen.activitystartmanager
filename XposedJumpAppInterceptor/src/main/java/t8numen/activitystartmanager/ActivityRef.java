package t8numen.activitystartmanager;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;

public final class ActivityRef {
    private final String packageName;
    private final String className;
    private final boolean systemApp;

    private ActivityRef(String packageName, String className) {
        this(packageName, className, false);
    }

    private ActivityRef(String packageName, String className, boolean systemApp) {
        this.packageName = packageName;
        this.className = className;
        this.systemApp = systemApp;
    }

    public static ActivityRef fromPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return new ActivityRef(packageName, null);
    }

    public static ActivityRef fromComponent(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        return new ActivityRef(componentName.getPackageName(), componentName.getClassName());
    }

    public static ActivityRef fromActivityInfo(ActivityInfo activityInfo) {
        if (activityInfo == null || activityInfo.packageName == null || activityInfo.name == null) {
            return null;
        }
        return new ActivityRef(activityInfo.packageName, normalizeClassName(activityInfo.packageName, activityInfo.name));
    }

    public static ActivityRef fromShortComponent(String fallbackPackage, String shortComponentName) {
        if (shortComponentName == null || shortComponentName.isEmpty()) {
            return fromPackage(fallbackPackage);
        }
        String[] parts = shortComponentName.split("/", 2);
        if (parts.length != 2) {
            return fromPackage(fallbackPackage);
        }
        String packageName = parts[0];
        String className = normalizeClassName(packageName, parts[1]);
        return new ActivityRef(packageName, className);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public boolean isSystemApp() {
        return systemApp;
    }

    public ActivityRef withSystemApp(boolean systemApp) {
        if (this.systemApp == systemApp) {
            return this;
        }
        return new ActivityRef(packageName, className, systemApp);
    }

    public String getNormalizedComponent() {
        if (className == null) {
            return null;
        }
        return packageName + "/" + className;
    }

    public String getDisplayName() {
        if (className == null) {
            return packageName;
        }
        if (className.startsWith(packageName + ".")) {
            return packageName + "/" + className.substring(packageName.length());
        }
        return packageName + "/" + className;
    }

    static String normalizeClassName(String packageName, String className) {
        if (className.startsWith(".")) {
            return packageName + className;
        }
        return className;
    }
}
