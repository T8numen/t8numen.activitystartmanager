package t8numen.activitystartmanager;

public final class ActivityLaunchRule {
    private final RuleAction action;
    private final RulePattern sourcePattern;
    private final RulePattern targetPattern;
    private final int lineNumber;

    public ActivityLaunchRule(RuleAction action, RulePattern sourcePattern, RulePattern targetPattern, int lineNumber) {
        this.action = action;
        this.sourcePattern = sourcePattern;
        this.targetPattern = targetPattern;
        this.lineNumber = lineNumber;
    }

    public RuleAction getAction() {
        return action;
    }

    public int getDisplayLineNumber() {
        return lineNumber + 1;
    }

    public boolean matches(ActivityRef source, ActivityRef target) {
        return sourcePattern.matches(source) && targetPattern.matches(target);
    }

    public boolean isUniversalAsk() {
        return action == RuleAction.ASK && sourcePattern.isAny() && targetPattern.isAny();
    }

    public boolean isExplicitInternalRule(ActivityRef source, ActivityRef target) {
        if (source == null || target == null || source.getPackageName() == null
                || !source.getPackageName().equals(target.getPackageName())) {
            return false;
        }
        String packageName = source.getPackageName();
        return sourcePattern.explicitlyMatchesPackage(packageName)
                && targetPattern.explicitlyMatchesPackage(packageName);
    }

    public boolean covers(ActivityLaunchRule other) {
        return other != null
                && sourcePattern.covers(other.sourcePattern)
                && targetPattern.covers(other.targetPattern);
    }

    public boolean overlaps(ActivityLaunchRule other) {
        return other != null
                && sourcePattern.overlaps(other.sourcePattern)
                && targetPattern.overlaps(other.targetPattern);
    }
}
