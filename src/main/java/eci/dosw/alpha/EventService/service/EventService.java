package eci.dosw.alpha.EventService.service


import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.model.RSVP;
import eci.dosw.alpha.EventService.repository.EventRepository;
import eci.dosw.alpha.EventService.repository.RSVPRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;

    public EventService(EventRepository eventRepository, RSVPRepository rsvpRepository) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
    }

    // Listar eventos
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> getEventsByCategory(String category) {
        return eventRepository.findByCategory(category);
    }

    public Event getEventById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
    }

    // Crear evento
    public Event createEvent(Event event) {
        event.setAvailableCapacity(event.getCapacity());
        event.setStatus("ACTIVE");
        return eventRepository.save(event);
    }

    // RSVP
    public String confirmRSVP(String userId, String eventId) {

        Event event = getEventById(eventId);

        if (!event.getStatus().equals("ACTIVE")) {
            throw new RuntimeException("Evento no disponible");
        }

        if (event.getAvailableCapacity() <= 0) {
            throw new RuntimeException("Cupo agotado");
        }

        RSVP existing = rsvpRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElse(null);

        if (existing != null && existing.getStatus().equals("CONFIRMED")) {
            throw new RuntimeException("Ya confirmaste asistencia");
        }

        RSVP rsvp = new RSVP();
        rsvp.setUserId(userId);
        rsvp.setEventId(eventId);
        rsvp.setStatus("CONFIRMED");

        rsvpRepository.save(rsvp);

        event.setAvailableCapacity(event.getAvailableCapacity() - 1);
        eventRepository.save(event);

        return "RSVP confirmado";
    }

    // Cancelar RSVP
    public String cancelRSVP(String userId, String eventId) {

        RSVP rsvp = rsvpRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new RuntimeException("RSVP no existe"));

        if (rsvp.getStatus().equals("CANCELLED")) {
            throw new RuntimeException("RSVP ya estaba cancelado");
        }

        rsvp.setStatus("CANCELLED");
        rsvpRepository.save(rsvp);

        Event event = getEventById(eventId);
        event.setAvailableCapacity(event.getAvailableCapacity() + 1);
        eventRepository.save(event);

        return "RSVP cancelado";
    }
}