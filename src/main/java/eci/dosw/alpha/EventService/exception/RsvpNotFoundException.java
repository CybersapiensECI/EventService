package eci.dosw.alpha.EventService.exception;

public class RsvpNotFoundException extends RuntimeException {
    public RsvpNotFoundException(String userId, String eventId) {
        super("RSVP inexistente para usuario '" + userId + "' en evento '" + eventId + "'.");
    }
}
