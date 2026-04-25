package t8numen.activitystartmanager;

public final class RulePattern {
    private enum Type {
        ANY,
        SYSTEM,
        PACKAGE_PREFIX,
        PACKAGE_EXACT,
        COMPONENT_EXACT
    }

    private final Type type;
    private final String value;

    private RulePattern(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    public static RulePattern parse(String token) {
        if (token == null || token.isEmpty() || "*".equals(token)) {
            return new RulePattern(Type.ANY, "*");
        }
        if ("system".equals(token)) {
            return new RulePattern(Type.SYSTEM, token);
        }
        if (token.contains("/")) {
            return new RulePattern(Type.COMPONENT_EXACT, normalizeComponent(token));
        }
        if (token.endsWith(".*")) {
            return new RulePattern(Type.PACKAGE_PREFIX, token.substring(0, token.length() - 2));
        }
        return new RulePattern(Type.PACKAGE_EXACT, token);
    }

    public boolean matches(ActivityRef activityRef) {
        if (type == Type.ANY) {
            return activityRef != null && !activityRef.isSystemApp();
        }
        if (activityRef == null || activityRef.getPackageName() == null) {
            return false;
        }
        switch (type) {
            case SYSTEM:
                return activityRef.isSystemApp();
            case PACKAGE_PREFIX:
                return activityRef.getPackageName().equals(value) || activityRef.getPackageName().startsWith(value + ".");
            case PACKAGE_EXACT:
                return activityRef.getPackageName().equals(value);
            case COMPONENT_EXACT:
                return value.equals(activityRef.getNormalizedComponent());
            default:
                return false;
        }
    }

    public boolean isAny() {
        return type == Type.ANY;
    }

    boolean canMatchNonSystemAppPattern() {
        return type != Type.SYSTEM;
    }

    String getSpecificPackageName() {
        switch (type) {
            case PACKAGE_EXACT:
                return value;
            case COMPONENT_EXACT:
                return getComponentPackage();
            case ANY:
            case SYSTEM:
            case PACKAGE_PREFIX:
            default:
                return null;
        }
    }

    public boolean explicitlyMatchesPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        switch (type) {
            case PACKAGE_PREFIX:
            case PACKAGE_EXACT:
                return isSameOrChildPackage(packageName, value);
            case COMPONENT_EXACT:
                return packageName.equals(getComponentPackage());
            case ANY:
            case SYSTEM:
            default:
                return false;
        }
    }

    public boolean covers(RulePattern other) {
        if (other == null) {
            return false;
        }
        if (type == Type.ANY) {
            return other.type != Type.SYSTEM;
        }
        switch (type) {
            case PACKAGE_PREFIX:
                return coversPackagePattern(other);
            case PACKAGE_EXACT:
                return other.type == Type.PACKAGE_EXACT && value.equals(other.value)
                        || other.type == Type.COMPONENT_EXACT && value.equals(other.getComponentPackage());
            case COMPONENT_EXACT:
                return other.type == Type.COMPONENT_EXACT && value.equals(other.value);
            case SYSTEM:
                return other.type == Type.SYSTEM;
            case ANY:
            default:
                return false;
        }
    }

    public boolean overlaps(RulePattern other) {
        if (other == null) {
            return false;
        }
        if (type == Type.ANY && other.type == Type.SYSTEM
                || type == Type.SYSTEM && other.type == Type.ANY) {
            return false;
        }
        if (type == Type.ANY || other.type == Type.ANY) {
            return true;
        }
        if (type == Type.COMPONENT_EXACT && other.type == Type.COMPONENT_EXACT) {
            return value.equals(other.value);
        }
        return covers(other) || other.covers(this);
    }

    private boolean coversPackagePattern(RulePattern other) {
        switch (other.type) {
            case PACKAGE_PREFIX:
            case PACKAGE_EXACT:
                return isSameOrChildPackage(other.value, value);
            case COMPONENT_EXACT:
                return isSameOrChildPackage(other.getComponentPackage(), value);
            case ANY:
            default:
                return false;
        }
    }

    private String getComponentPackage() {
        int separator = value.indexOf('/');
        if (separator <= 0) {
            return value;
        }
        return value.substring(0, separator);
    }

    private static boolean isSameOrChildPackage(String packageName, String parentPackage) {
        return packageName != null
                && parentPackage != null
                && (packageName.equals(parentPackage) || packageName.startsWith(parentPackage + "."));
    }

    private static String normalizeComponent(String rawComponent) {
        String[] parts = rawComponent.split("/", 2);
        if (parts.length != 2) {
            return rawComponent;
        }
        String packageName = parts[0];
        String className = parts[1];
        if (className.startsWith(".")) {
            className = packageName + className;
        }
        return packageName + "/" + className;
    }
}
