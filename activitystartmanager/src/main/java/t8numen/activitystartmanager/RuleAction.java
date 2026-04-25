package t8numen.activitystartmanager;

public enum RuleAction {
    AGREE,
    DISAGREE,
    ASK;

    public static RuleAction fromToken(String token) {
        if ("agree".equalsIgnoreCase(token) || "allow".equalsIgnoreCase(token)) {
            return AGREE;
        }
        if ("disagree".equalsIgnoreCase(token)) {
            return DISAGREE;
        }
        if ("ask".equalsIgnoreCase(token)) {
            return ASK;
        }
        return null;
    }
}
