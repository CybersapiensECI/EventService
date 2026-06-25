package eci.dosw.alpha.EventService.exception;

public class EventNotActiveException extends RuntimeException {
    public EventNotActiveException(String eventId) {
        super("El evento '" + eventId + "' está cancelado o finalizado y no acepta RSVP.");
    }
}
