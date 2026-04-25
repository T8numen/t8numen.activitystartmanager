package t8numen.activitystartmanager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class HookPackagePolicy {
    static final String PACKAGE_ANDROID = "android";
    static final String PACKAGE_OPLUS_SECURITY_PERMISSION = "com.oplus.securitypermission";
    static final String PACKAGE_COLOROS_SECURITY_PERMISSION = "com.coloros.securitypermission";
    static final String PACKAGE_PERMISSION_CONTROLLER = "com.android.permissioncontroller";

    private static final String PACKAGE_SYSTEM_UI = "com.android.systemui";

    private static final Set<String> PERMISSION_UI_PACKAGES = new HashSet<>(Arrays.asList(
            PACKAGE_OPLUS_SECURITY_PERMISSION,
            PACKAGE_COLOROS_SECURITY_PERMISSION,
            PACKAGE_PERMISSION_CONTROLLER
    ));
    private static final Set<String> ASK_PROTECTED_PACKAGES = new HashSet<>(Arrays.asList(
            ModuleConfig.MODULE_PACKAGE,
            ModuleConfig.OLD_MODULE_PACKAGE,
            PACKAGE_ANDROID,
            PACKAGE_OPLUS_SECURITY_PERMISSION,
            PACKAGE_COLOROS_SECURITY_PERMISSION,
            PACKAGE_PERMISSION_CONTROLLER,
            "com.google.android.permissioncontroller",
            "org.lsposed.manager"
    ));
    private static final Set<String> ASK_DEFAULT_EXEMPT_PACKAGES = new HashSet<>(Arrays.asList(
            PACKAGE_SYSTEM_UI,
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.coloros.packageinstaller",
            "com.oplus.packageinstaller"
    ));
    private static final Set<String> ASK_LAUNCHER_PACKAGES = new HashSet<>(Arrays.asList(
            "com.android.launcher",
            "com.android.launcher3",
            "com.oplus.launcher",
            "com.coloros.launcher",
            "com.heytap.launcher",
            "net.oneplus.launcher"
    ));

    private HookPackagePolicy() {
    }

    static boolean isPermissionUiPackage(String packageName) {
        return PERMISSION_UI_PACKAGES.contains(packageName);
    }

    static boolean isAskExempt(ActivityRef sourceRef, ActivityRef targetRef, ActivityLaunchRule matchedRule) {
        String sourcePackage = sourceRef == null ? null : sourceRef.getPackageName();
        String targetPackage = targetRef == null ? null : targetRef.getPackageName();
        if (isProtectedAskPackage(sourcePackage) || isProtectedAskPackage(targetPackage)) {
            return true;
        }
        return matchedRule != null
                && matchedRule.isUniversalAsk()
                && (isDefaultAskExempt(sourcePackage)
                || isDefaultAskExempt(targetPackage)
                || isLauncherPackage(sourcePackage)
                || isLauncherPackage(targetPackage));
    }

    private static boolean isProtectedAskPackage(String packageName) {
        return packageName != null && ASK_PROTECTED_PACKAGES.contains(packageName);
    }

    private static boolean isDefaultAskExempt(String packageName) {
        return packageName != null && ASK_DEFAULT_EXEMPT_PACKAGES.contains(packageName);
    }

    private static boolean isLauncherPackage(String packageName) {
        return packageName != null && ASK_LAUNCHER_PACKAGES.contains(packageName);
    }
}
