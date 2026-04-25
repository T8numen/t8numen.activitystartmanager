package t8numen.activitystartmanager;

final class RuleConflict {
    private final int primaryLineNumber;
    private final int relatedLineNumber;
    private final boolean covered;

    RuleConflict(int primaryLineNumber, int relatedLineNumber, boolean covered) {
        this.primaryLineNumber = primaryLineNumber;
        this.relatedLineNumber = relatedLineNumber;
        this.covered = covered;
    }

    int getPrimaryLineNumber() {
        return primaryLineNumber;
    }

    int getRelatedLineNumber() {
        return relatedLineNumber;
    }

    String getMessage() {
        if (covered) {
            return "第 " + primaryLineNumber + " 行被第 " + relatedLineNumber + " 行覆盖，不会生效";
        }
        return "第 " + primaryLineNumber + " 行与第 " + relatedLineNumber + " 行部分冲突，重叠部分按靠前规则执行";
    }
}
