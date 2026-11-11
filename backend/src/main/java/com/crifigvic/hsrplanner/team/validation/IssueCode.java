package com.crifigvic.hsrplanner.team.validation;

public enum IssueCode {
    EMPTY_TEAM(Severity.ERROR),
    TOO_MANY_SLOTS(Severity.ERROR),
    INVALID_POSITION(Severity.ERROR),
    DUPLICATE_POSITION(Severity.ERROR),
    DUPLICATE_CHARACTER(Severity.ERROR),
    DUPLICATE_VARIANT(Severity.ERROR),
    LIGHT_CONE_PATH_MISMATCH(Severity.WARNING),
    NO_SUSTAIN(Severity.WARNING),
    NO_DPS(Severity.WARNING),
    SEVERAL_SUSTAINS(Severity.INFO);

    private final Severity severity;

    IssueCode(Severity severity) {
        this.severity = severity;
    }

    public Severity severity() {
        return severity;
    }

    public enum Severity {
        /** The team can't be saved. */
        ERROR,
        /** Legal in game but probably a mistake. */
        WARNING,
        /** Worth knowing, not a problem. */
        INFO
    }
}
