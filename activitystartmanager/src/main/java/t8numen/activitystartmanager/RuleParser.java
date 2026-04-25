package t8numen.activitystartmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuleParser {
    private RuleParser() {
    }

    public static List<ActivityLaunchRule> parse(String rawRules) {
        if (rawRules == null || rawRules.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<ActivityLaunchRule> rules = new ArrayList<>();
        String[] lines = rawRules.split("\\r?\\n");
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length != 3) {
                continue;
            }
            RuleAction action = RuleAction.fromToken(parts[0]);
            if (action == null) {
                continue;
            }
            rules.add(new ActivityLaunchRule(
                    action,
                    RulePattern.parse(parts[1]),
                    RulePattern.parse(parts[2]),
                    index
            ));
        }
        return rules;
    }

    public static ActivityLaunchRule findEffectiveMatch(List<ActivityLaunchRule> rules, ActivityRef source, ActivityRef target) {
        if (rules == null || source == null || target == null) {
            return null;
        }
        if (isInternalLaunch(source, target)) {
            for (ActivityLaunchRule rule : rules) {
                if (rule.getAction() == RuleAction.DISAGREE
                        && rule.isExplicitInternalRule(source, target)
                        && rule.matches(source, target)) {
                    return rule;
                }
            }
            for (ActivityLaunchRule rule : rules) {
                if (rule.getAction() == RuleAction.ASK
                        && rule.isExplicitInternalRule(source, target)
                        && rule.matches(source, target)) {
                    return rule;
                }
            }
            return null;
        }
        for (ActivityLaunchRule rule : rules) {
            if (rule.getAction() != RuleAction.ASK && rule.matches(source, target)) {
                return rule;
            }
        }
        for (ActivityLaunchRule rule : rules) {
            if (rule.getAction() == RuleAction.ASK && rule.matches(source, target)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean isInternalLaunch(ActivityRef source, ActivityRef target) {
        return source.getPackageName() != null && source.getPackageName().equals(target.getPackageName());
    }

    public static List<String> findConflictWarnings(List<ActivityLaunchRule> rules) {
        List<RuleConflict> conflicts = findConflicts(rules);
        if (conflicts.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> warnings = new ArrayList<>();
        for (RuleConflict conflict : conflicts) {
            warnings.add(conflict.getMessage());
        }
        return warnings;
    }

    public static List<RuleConflict> findConflicts(List<ActivityLaunchRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        List<RuleConflict> conflicts = new ArrayList<>();
        for (int earlierIndex = 0; earlierIndex < rules.size(); earlierIndex++) {
            ActivityLaunchRule earlierRule = rules.get(earlierIndex);
            for (int laterIndex = earlierIndex + 1; laterIndex < rules.size(); laterIndex++) {
                ActivityLaunchRule laterRule = rules.get(laterIndex);
                if (earlierRule.getAction() == RuleAction.ASK
                        || laterRule.getAction() == RuleAction.ASK
                        || earlierRule.getAction() == laterRule.getAction()
                        || !earlierRule.overlaps(laterRule)) {
                    continue;
                }
                conflicts.add(new RuleConflict(
                        laterRule.getDisplayLineNumber(),
                        earlierRule.getDisplayLineNumber(),
                        earlierRule.covers(laterRule)
                ));
            }
        }
        return conflicts;
    }
}
