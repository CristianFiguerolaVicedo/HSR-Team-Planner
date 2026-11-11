package com.crifigvic.hsrplanner.team;

import com.crifigvic.hsrplanner.team.validation.TeamValidationResult;
import lombok.Getter;

/** Thrown when saving a team that breaks a game rule. Answered as 422 with the full validation. */
@Getter
public class InvalidTeamException extends RuntimeException {

    private final TeamValidationResult validation;

    public InvalidTeamException(TeamValidationResult validation) {
        super("The team breaks one or more rules");
        this.validation = validation;
    }
}
