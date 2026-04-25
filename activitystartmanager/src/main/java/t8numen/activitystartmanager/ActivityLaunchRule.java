package t8numen.activitystartmanager;

public final class ActivityLaunchRule {
    private static final RulePattern ANY_USER_APP_PATTERN = RulePattern.parse("*");

    private final RuleAction action;
    private final RulePattern sourcePattern;
    private final RulePattern targetPattern;
    private final RulePattern participantPattern;
    private final int lineNumber;

    public ActivityLaunchRule(RuleAction action, RulePattern sourcePattern, RulePattern targetPattern, int lineNumber) {
        this.action = action;
        this.sourcePattern = sourcePattern;
        this.targetPattern = targetPattern;
        this.participantPattern = null;
        this.lineNumber = lineNumber;
    }

    public ActivityLaunchRule(RuleAction action, RulePattern participantPattern, int lineNumber) {
        this.action = action;
        this.sourcePattern = null;
        this.targetPattern = null;
        this.participantPattern = participantPattern;
        this.lineNumber = lineNumber;
    }

    public RuleAction getAction() {
        return action;
    }

    public int getDisplayLineNumber() {
        return lineNumber + 1;
    }

    public boolean matches(ActivityRef source, ActivityRef target) {
        if (isParticipantRule()) {
            return matchesParticipantRule(source, target);
        }
        return sourcePattern.matches(source) && targetPattern.matches(target);
    }

    public boolean isUniversalAsk() {
        return !isParticipantRule() && action == RuleAction.ASK && sourcePattern.isAny() && targetPattern.isAny();
    }

    public boolean isExplicitInternalRule(ActivityRef source, ActivityRef target) {
        if (isParticipantRule()) {
            return false;
        }
        if (source == null || target == null || source.getPackageName() == null
                || !source.getPackageName().equals(target.getPackageName())) {
            return false;
        }
        String packageName = source.getPackageName();
        return sourcePattern.explicitlyMatchesPackage(packageName)
                && targetPattern.explicitlyMatchesPackage(packageName);
    }

    public boolean covers(ActivityLaunchRule other) {
        if (other == null) {
            return false;
        }
        if (isParticipantRule()) {
            return coversFromParticipantRule(other);
        }
        if (other.isParticipantRule()) {
            return coversParticipantRule(other.participantPattern);
        }
        return sourcePattern.covers(other.sourcePattern)
                && targetPattern.covers(other.targetPattern);
    }

    public boolean overlaps(ActivityLaunchRule other) {
        if (other == null) {
            return false;
        }
        if (isParticipantRule() && other.isParticipantRule()) {
            return participantPattern.overlaps(other.participantPattern)
                    || participantPattern.canMatchNonSystemAppPattern()
                    && other.participantPattern.canMatchNonSystemAppPattern();
        }
        if (isParticipantRule()) {
            return overlapsFromParticipantRule(other);
        }
        if (other.isParticipantRule()) {
            return other.overlapsFromParticipantRule(this);
        }
        return sourcePattern.overlaps(other.sourcePattern)
                && targetPattern.overlaps(other.targetPattern);
    }

    private boolean isParticipantRule() {
        return participantPattern != null;
    }

    private boolean matchesParticipantRule(ActivityRef source, ActivityRef target) {
        return participantPattern.matches(source) && isNonSystemApp(target)
                || isNonSystemApp(source) && participantPattern.matches(target);
    }

    private boolean isNonSystemApp(ActivityRef activityRef) {
        return activityRef != null && !activityRef.isSystemApp();
    }

    private boolean coversFromParticipantRule(ActivityLaunchRule other) {
        if (other.isParticipantRule()) {
            return participantPattern.covers(other.participantPattern);
        }
        if (other.isSpecificInternalPattern()) {
            return false;
        }
        return participantPattern.covers(other.sourcePattern)
                && ANY_USER_APP_PATTERN.covers(other.targetPattern)
                || ANY_USER_APP_PATTERN.covers(other.sourcePattern)
                && participantPattern.covers(other.targetPattern);
    }

    private boolean coversParticipantRule(RulePattern otherParticipantPattern) {
        return sourcePattern.covers(otherParticipantPattern)
                && targetPattern.covers(ANY_USER_APP_PATTERN)
                && sourcePattern.covers(ANY_USER_APP_PATTERN)
                && targetPattern.covers(otherParticipantPattern);
    }

    private boolean overlapsFromParticipantRule(ActivityLaunchRule other) {
        if (other.isSpecificInternalPattern()) {
            return false;
        }
        return participantPattern.overlaps(other.sourcePattern)
                && ANY_USER_APP_PATTERN.overlaps(other.targetPattern)
                || ANY_USER_APP_PATTERN.overlaps(other.sourcePattern)
                && participantPattern.overlaps(other.targetPattern);
    }

    private boolean isSpecificInternalPattern() {
        if (isParticipantRule()) {
            return false;
        }
        String sourcePackage = sourcePattern.getSpecificPackageName();
        String targetPackage = targetPattern.getSpecificPackageName();
        return sourcePackage != null && sourcePackage.equals(targetPackage);
    }
}
