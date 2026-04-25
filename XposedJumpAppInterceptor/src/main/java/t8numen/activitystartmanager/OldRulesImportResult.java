package t8numen.activitystartmanager;

public final class OldRulesImportResult {
    public enum Status {
        IMPORTED,
        MERGED,
        ALREADY_PRESENT,
        SKIPPED_EXISTING_RULES,
        NOT_FOUND
    }

    private final Status status;
    private final int importedCount;

    private OldRulesImportResult(Status status, int importedCount) {
        this.status = status;
        this.importedCount = importedCount;
    }

    public static OldRulesImportResult imported(int importedCount) {
        return new OldRulesImportResult(Status.IMPORTED, importedCount);
    }

    public static OldRulesImportResult merged(int importedCount) {
        return new OldRulesImportResult(Status.MERGED, importedCount);
    }

    public static OldRulesImportResult alreadyPresent() {
        return new OldRulesImportResult(Status.ALREADY_PRESENT, 0);
    }

    public static OldRulesImportResult skippedExistingRules() {
        return new OldRulesImportResult(Status.SKIPPED_EXISTING_RULES, 0);
    }

    public static OldRulesImportResult notFound() {
        return new OldRulesImportResult(Status.NOT_FOUND, 0);
    }

    public Status getStatus() {
        return status;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public boolean changedRules() {
        return status == Status.IMPORTED || status == Status.MERGED;
    }
}
