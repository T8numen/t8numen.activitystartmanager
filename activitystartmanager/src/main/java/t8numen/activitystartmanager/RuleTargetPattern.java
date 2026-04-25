package t8numen.activitystartmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RuleTargetPattern {
    private final RulePattern includePattern;
    private final List<RulePattern> excludedPatterns;

    private RuleTargetPattern(RulePattern includePattern, List<RulePattern> excludedPatterns) {
        this.includePattern = includePattern;
        this.excludedPatterns = excludedPatterns;
    }

    static RuleTargetPattern from(RulePattern pattern) {
        return new RuleTargetPattern(pattern, Collections.emptyList());
    }

    static RuleTargetPattern parse(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        int separator = token.indexOf('|');
        if (separator < 0) {
            return new RuleTargetPattern(RulePattern.parse(token), Collections.emptyList());
        }
        if (separator == 0 || separator == token.length() - 1 || separator != token.lastIndexOf('|')) {
            return null;
        }
        String includeToken = token.substring(0, separator);
        String excludedToken = token.substring(separator + 1);
        String[] excludedParts = excludedToken.split(",", -1);
        List<RulePattern> exclusions = new ArrayList<>();
        for (String excludedPart : excludedParts) {
            if (excludedPart == null || excludedPart.isEmpty() || excludedPart.contains("|")) {
                return null;
            }
            exclusions.add(RulePattern.parse(excludedPart));
        }
        return new RuleTargetPattern(RulePattern.parse(includeToken), Collections.unmodifiableList(exclusions));
    }

    boolean matches(ActivityRef activityRef) {
        if (!includePattern.matches(activityRef)) {
            return false;
        }
        for (RulePattern excludedPattern : excludedPatterns) {
            if (excludedPattern.matches(activityRef)) {
                return false;
            }
        }
        return true;
    }

    boolean isAny() {
        return includePattern.isAny() && excludedPatterns.isEmpty();
    }

    boolean explicitlyMatchesPackage(String packageName) {
        return includePattern.explicitlyMatchesPackage(packageName);
    }

    boolean covers(RuleTargetPattern other) {
        if (other == null || !includePattern.covers(other.includePattern)) {
            return false;
        }
        for (RulePattern excludedPattern : excludedPatterns) {
            if (excludedPattern.overlaps(other.includePattern)
                    && !other.exclusionsCover(excludedPattern)) {
                return false;
            }
        }
        return true;
    }

    boolean covers(RulePattern other) {
        return covers(new RuleTargetPattern(other, Collections.emptyList()));
    }

    boolean overlaps(RuleTargetPattern other) {
        if (other == null || !includePattern.overlaps(other.includePattern)) {
            return false;
        }
        return !exclusionsCover(other.includePattern)
                && !other.exclusionsCover(includePattern);
    }

    boolean overlaps(RulePattern other) {
        return overlaps(new RuleTargetPattern(other, Collections.emptyList()));
    }

    String getSpecificPackageName() {
        return includePattern.getSpecificPackageName();
    }

    private boolean exclusionsCover(RulePattern pattern) {
        for (RulePattern excludedPattern : excludedPatterns) {
            if (excludedPattern.covers(pattern)) {
                return true;
            }
        }
        return false;
    }
}
