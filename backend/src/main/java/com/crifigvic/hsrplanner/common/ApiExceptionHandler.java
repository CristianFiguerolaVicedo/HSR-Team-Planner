package com.crifigvic.hsrplanner.common;

import com.crifigvic.hsrplanner.importer.ImportException;
import com.crifigvic.hsrplanner.team.InvalidTeamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errors answered as RFC 9457 problem details ({@code application/problem+json}). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Bad input caught by the domain (an out-of-range SPD, for example). */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidTeamException.class)
    ProblemDetail invalidTeam(InvalidTeamException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problem.setProperty("validation", e.getValidation());
        return problem;
    }

    /** Import by UID: the reason decides the status, the message is meant for the player. */
    @ExceptionHandler(ImportException.class)
    ProblemDetail importFailed(ImportException e) {
        HttpStatus status = switch (e.getReason()) {
            case INVALID_UID -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MAINTENANCE -> HttpStatus.SERVICE_UNAVAILABLE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setProperty("reason", e.getReason());
        return problem;
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }
}
