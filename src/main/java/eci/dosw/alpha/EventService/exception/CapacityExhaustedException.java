package eci.dosw.alpha.EventService.exception;

public class CapacityExhaustedException extends RuntimeException {
    public CapacityExhaustedException(String eventId) {
        super("Cupo agotado para el evento '" + eventId + "'.");
    }
}
