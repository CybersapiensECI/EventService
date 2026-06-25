package eci.dosw.alpha.EventService.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // E1: evento cancelado/finalizado → 409
    @ExceptionHandler(EventNotActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleEventNotActive(EventNotActiveException ex) {
        return Map.of("error", ex.getMessage(), "code", "E1");
    }

    // E2: cupo agotado → 409
    @ExceptionHandler(CapacityExhaustedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleCapacityExhausted(CapacityExhaustedException ex) {
        return Map.of("error", ex.getMessage(), "code", "E2");
    }

    // E3: RSVP inexistente al cancelar → 404
    @ExceptionHandler(RsvpNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleRsvpNotFound(RsvpNotFoundException ex) {
        return Map.of("error", ex.getMessage(), "code", "E3");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneric(RuntimeException ex) {
        return Map.of("error", ex.getMessage());
    }
}
