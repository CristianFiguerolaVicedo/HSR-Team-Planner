package com.crifigvic.hsrplanner.team.validation;

/** @param position slot the issue refers to, or null when it concerns the whole team */
public record ValidationIssue(IssueCode code, IssueCode.Severity severity, Integer position, String message) {

    static ValidationIssue of(IssueCode code, Integer position, String message) {
        return new ValidationIssue(code, code.severity(), position, message);
    }
}
