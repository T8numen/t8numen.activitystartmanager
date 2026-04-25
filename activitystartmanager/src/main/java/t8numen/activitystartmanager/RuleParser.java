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
            ParsedRuleLine parsedLine = parseRuleLine(line, index);
            if (parsedLine != null) {
                rules.add(parsedLine.toRule());
            }
        }
        return rules;
    }

    public static ActivityLaunchRule findEffectiveMatch(List<ActivityLaunchRule> rules, ActivityRef source, ActivityRef target) {
        if (rules == null || source == null || target == null) {
            return null;
        }
        if (isInternalLaunch(source, target)) {
            for (ActivityLaunchRule rule : rules) {
                if (rule.isHighPriority()
                        && rule.isExplicitInternalRule(source, target)
                        && rule.matches(source, target)) {
                    return rule;
                }
            }
            for (ActivityLaunchRule rule : rules) {
                if (!rule.isHighPriority()
                        && rule.getAction() == RuleAction.DISAGREE
                        && rule.isExplicitInternalRule(source, target)
                        && rule.matches(source, target)) {
                    return rule;
                }
            }
            for (ActivityLaunchRule rule : rules) {
                if (!rule.isHighPriority()
                        && rule.getAction() == RuleAction.ASK
                        && rule.isExplicitInternalRule(source, target)
                        && rule.matches(source, target)) {
                    return rule;
                }
            }
            return null;
        }
        for (ActivityLaunchRule rule : rules) {
            if (rule.isHighPriority() && rule.matches(source, target)) {
                return rule;
            }
        }
        for (ActivityLaunchRule rule : rules) {
            if (!rule.isHighPriority() && rule.getAction() != RuleAction.ASK && rule.matches(source, target)) {
                return rule;
            }
        }
        for (ActivityLaunchRule rule : rules) {
            if (!rule.isHighPriority() && rule.getAction() == RuleAction.ASK && rule.matches(source, target)) {
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
                        || earlierRule.isHighPriority() != laterRule.isHighPriority()
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

    public static boolean isValidRuleLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return !trimmed.isEmpty() && !trimmed.startsWith("#") && parseRuleLine(trimmed, 0) != null;
    }

    public static String formatRuleLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        ParsedRuleLine parsedLine = parseRuleLine(trimmed, 0);
        return parsedLine == null ? trimmed : parsedLine.format();
    }

    private static ParsedRuleLine parseRuleLine(String line, int lineNumber) {
        String[] parts = line.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        boolean highPriority = false;
        int actionIndex = 0;
        String actionToken = parts[0];
        if ("!".equals(actionToken)) {
            highPriority = true;
            actionIndex = 1;
            if (parts.length <= actionIndex) {
                return null;
            }
            actionToken = parts[actionIndex];
        } else if (actionToken.startsWith("!") && actionToken.length() > 1) {
            highPriority = true;
            actionToken = actionToken.substring(1);
        }
        RuleAction action = RuleAction.fromToken(actionToken);
        if (action == null) {
            return null;
        }
        int operandStart = actionIndex + 1;
        int operandCount = parts.length - operandStart;
        if (operandCount == 1) {
            String participantToken = parts[operandStart];
            if (participantToken.contains("|")) {
                return null;
            }
            return ParsedRuleLine.participant(action, highPriority, participantToken, lineNumber);
        }
        if (operandCount == 2) {
            String sourceToken = parts[operandStart];
            String targetToken = parts[operandStart + 1];
            RuleTargetPattern targetPattern = RuleTargetPattern.parse(targetToken);
            if (sourceToken.contains("|") || targetPattern == null) {
                return null;
            }
            return ParsedRuleLine.sourceTarget(action, highPriority, sourceToken, targetToken, targetPattern, lineNumber);
        }
        return null;
    }

    private static final class ParsedRuleLine {
        private final RuleAction action;
        private final boolean highPriority;
        private final String sourceToken;
        private final String targetToken;
        private final RuleTargetPattern targetPattern;
        private final String participantToken;
        private final int lineNumber;

        private ParsedRuleLine(RuleAction action, boolean highPriority, String sourceToken, String targetToken,
                               RuleTargetPattern targetPattern, String participantToken, int lineNumber) {
            this.action = action;
            this.highPriority = highPriority;
            this.sourceToken = sourceToken;
            this.targetToken = targetToken;
            this.targetPattern = targetPattern;
            this.participantToken = participantToken;
            this.lineNumber = lineNumber;
        }

        static ParsedRuleLine sourceTarget(RuleAction action, boolean highPriority, String sourceToken,
                                           String targetToken, RuleTargetPattern targetPattern, int lineNumber) {
            return new ParsedRuleLine(action, highPriority, sourceToken, targetToken, targetPattern, null, lineNumber);
        }

        static ParsedRuleLine participant(RuleAction action, boolean highPriority, String participantToken,
                                          int lineNumber) {
            return new ParsedRuleLine(action, highPriority, null, null, null, participantToken, lineNumber);
        }

        ActivityLaunchRule toRule() {
            if (participantToken != null) {
                return new ActivityLaunchRule(
                        action,
                        RulePattern.parse(participantToken),
                        highPriority,
                        lineNumber
                );
            }
            return new ActivityLaunchRule(
                    action,
                    RulePattern.parse(sourceToken),
                    targetPattern,
                    highPriority,
                    lineNumber
            );
        }

        String format() {
            StringBuilder builder = new StringBuilder();
            if (highPriority) {
                builder.append('!');
            }
            builder.append(action.toToken());
            if (participantToken != null) {
                return builder.append(' ').append(participantToken).toString();
            }
            return builder.append(' ')
                    .append(sourceToken)
                    .append(' ')
                    .append(targetToken)
                    .toString();
        }
    }
}
