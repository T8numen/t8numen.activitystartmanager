package t8numen.activitystartmanager;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RuleParserTest {
    @Test
    public void parse_ignoresCommentsBlanksAndInvalidLines() {
        List<ActivityLaunchRule> rules = RuleParser.parse(
                "# comment\n"
                        + "\n"
                        + "allow com.source.app com.target.app\n"
                        + "bad line\n"
                        + "ask * com.target.app/.MainActivity\n");

        assertEquals(2, rules.size());
        assertEquals(RuleAction.AGREE, rules.get(0).getAction());
        assertEquals(RuleAction.ASK, rules.get(1).getAction());
    }

    @Test
    public void findEffectiveMatch_prefersFirstAgreeOrDisagreeBeforeAsk() {
        List<ActivityLaunchRule> rules = RuleParser.parse(
                "ask * com.target.app\n"
                        + "agree com.source.app com.target.app\n"
                        + "disagree com.source.app com.target.app/.BlockedActivity\n");

        ActivityLaunchRule match = RuleParser.findEffectiveMatch(
                rules,
                app("com.source.app"),
                activity("com.target.app", ".BlockedActivity")
        );

        assertEquals(RuleAction.AGREE, match.getAction());
        assertEquals(2, match.getDisplayLineNumber());
    }

    @Test
    public void findEffectiveMatch_usesAskWhenAgreeAndDisagreeMiss() {
        List<ActivityLaunchRule> rules = RuleParser.parse(
                "agree com.source.app com.other.app\n"
                        + "ask * com.target.app\n");

        ActivityLaunchRule match = RuleParser.findEffectiveMatch(
                rules,
                app("com.another.source"),
                app("com.target.app")
        );

        assertEquals(RuleAction.ASK, match.getAction());
        assertEquals(2, match.getDisplayLineNumber());
    }

    @Test
    public void findEffectiveMatch_allowsInternalLaunchByDefault() {
        List<ActivityLaunchRule> rules = RuleParser.parse("ask * *");

        ActivityLaunchRule match = RuleParser.findEffectiveMatch(
                rules,
                activity("com.same.app", ".SourceActivity"),
                activity("com.same.app", ".TargetActivity")
        );

        assertNull(match);
    }

    @Test
    public void findEffectiveMatch_universalAskMatchesUserAppsOnly() {
        List<ActivityLaunchRule> rules = RuleParser.parse("ask * *");

        ActivityLaunchRule userMatch = RuleParser.findEffectiveMatch(
                rules,
                app("com.source.app"),
                app("com.target.app")
        );
        ActivityLaunchRule systemSourceMatch = RuleParser.findEffectiveMatch(
                rules,
                app("com.android.settings").withSystemApp(true),
                app("com.target.app")
        );
        ActivityLaunchRule systemTargetMatch = RuleParser.findEffectiveMatch(
                rules,
                app("com.source.app"),
                app("com.android.settings").withSystemApp(true)
        );

        assertEquals(RuleAction.ASK, userMatch.getAction());
        assertNull(systemSourceMatch);
        assertNull(systemTargetMatch);
    }

    @Test
    public void findEffectiveMatch_supportsExplicitInternalAskAndDisagree() {
        List<ActivityLaunchRule> rules = RuleParser.parse(
                "ask com.same.app com.same.app\n"
                        + "disagree com.same.app com.same.app/.BlockedActivity\n");

        ActivityLaunchRule askMatch = RuleParser.findEffectiveMatch(
                rules,
                activity("com.same.app", ".SourceActivity"),
                activity("com.same.app", ".TargetActivity")
        );
        ActivityLaunchRule disagreeMatch = RuleParser.findEffectiveMatch(
                rules,
                activity("com.same.app", ".SourceActivity"),
                activity("com.same.app", ".BlockedActivity")
        );

        assertEquals(RuleAction.ASK, askMatch.getAction());
        assertEquals(RuleAction.DISAGREE, disagreeMatch.getAction());
    }

    @Test
    public void findEffectiveMatch_supportsSystemAlias() {
        List<ActivityLaunchRule> rules = RuleParser.parse(
                "ask system *\n"
                        + "ask * system\n");

        ActivityLaunchRule systemSourceMatch = RuleParser.findEffectiveMatch(
                rules,
                app("com.android.vending").withSystemApp(true),
                app("com.target.app")
        );
        ActivityLaunchRule systemTargetMatch = RuleParser.findEffectiveMatch(
                rules,
                app("com.source.app"),
                app("com.android.settings").withSystemApp(true)
        );

        assertEquals(RuleAction.ASK, systemSourceMatch.getAction());
        assertEquals(RuleAction.ASK, systemTargetMatch.getAction());
    }

    @Test
    public void findConflicts_treatsAnyAndSystemAsDisjoint() {
        List<RuleConflict> conflicts = RuleParser.findConflicts(RuleParser.parse(
                "agree * *\n"
                        + "disagree system *\n"));

        assertEquals(0, conflicts.size());
    }

    @Test
    public void findConflicts_reportsLaterConflictingAgreeOrDisagree() {
        List<RuleConflict> conflicts = RuleParser.findConflicts(RuleParser.parse(
                "agree com.source.app *\n"
                        + "disagree com.source.app com.target.app\n"
                        + "ask * com.target.app\n"));

        assertEquals(1, conflicts.size());
        assertEquals(2, conflicts.get(0).getPrimaryLineNumber());
        assertEquals(1, conflicts.get(0).getRelatedLineNumber());
    }

    private ActivityRef app(String packageName) {
        return ActivityRef.fromPackage(packageName);
    }

    private ActivityRef activity(String packageName, String className) {
        return ActivityRef.fromShortComponent(packageName, packageName + "/" + className);
    }
}
