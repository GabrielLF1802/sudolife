package com.sudolife.adapter.driving.rest;

import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.exception.UserAlreadyExistsException;
import com.sudolife.application.service.strava.exception.DuplicateStravaAthleteOwnershipException;
import com.sudolife.application.service.strava.exception.StravaAccountLinkingException;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.exception.InvalidTrainingProfileException;
import com.sudolife.application.service.training.exception.CoachingProfileRequiredException;
import com.sudolife.application.service.training.exception.ConservativeRunningPlanNotRequiredException;
import com.sudolife.application.service.training.exception.UnsafeAiRunningPlanException;
import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("USER_ALREADY_EXISTS", exception.getMessage()));
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticatedUserNotFound(AuthenticatedUserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATED_USER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateStravaAthleteOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateStravaAthlete(
            DuplicateStravaAthleteOwnershipException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getFailureCode(), exception.getMessage()));
    }

    @ExceptionHandler(StravaAccountLinkingException.class)
    public ResponseEntity<ErrorResponse> handleStravaAccountLinking(StravaAccountLinkingException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getFailureCode(), exception.getMessage()));
    }

    @ExceptionHandler(StravaActivityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStravaActivityNotFound(StravaActivityNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("STRAVA_ACTIVITY_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTrainingProfileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTrainingProfile(InvalidTrainingProfileException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getFailureCode(), exception.getMessage()));
    }

    @ExceptionHandler(InvalidCoachingProfileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCoachingProfile(InvalidCoachingProfileException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getFailureCode(), exception.getMessage()));
    }

    @ExceptionHandler(CoachingProfileRequiredException.class)
    public ResponseEntity<ErrorResponse> handleCoachingProfileRequired(CoachingProfileRequiredException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("COACHING_PROFILE_REQUIRED", exception.getMessage()));
    }

    @ExceptionHandler(ConservativeRunningPlanNotRequiredException.class)
    public ResponseEntity<ErrorResponse> handleConservativeRunningPlanNotRequired(
            ConservativeRunningPlanNotRequiredException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONSERVATIVE_RUNNING_PLAN_NOT_REQUIRED", exception.getMessage()));
    }

    @ExceptionHandler(UnsafeAiRunningPlanException.class)
    public ResponseEntity<ErrorResponse> handleUnsafeAiRunningPlan(UnsafeAiRunningPlanException exception) {
        return ResponseEntity.unprocessableEntity().body(new ErrorResponse("UNSAFE_AI_RUNNING_PLAN", exception.getMessage()));
    }

    @ExceptionHandler(AdaptiveRunningPlanNotFoundException.class)
    public ResponseEntity<Void> handleAdaptiveRunningPlanNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }

    public record ErrorResponse(String code, String message) {
    }
}
