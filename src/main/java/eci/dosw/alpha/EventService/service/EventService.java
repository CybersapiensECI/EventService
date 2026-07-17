package eci.dosw.alpha.EventService.service;

import eci.dosw.alpha.EventService.dto.RSVPResponse;
import eci.dosw.alpha.EventService.exception.CapacityExhaustedException;
import eci.dosw.alpha.EventService.exception.EventNotActiveException;
import eci.dosw.alpha.EventService.exception.RsvpNotFoundException;
import eci.dosw.alpha.EventService.messaging.EventBroadcastMessage;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.model.RSVP;
import eci.dosw.alpha.EventService.repository.EventRepository;
import eci.dosw.alpha.EventService.repository.RSVPRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification:notification.exchange}")
    private String notificationExchange;

    private static final String EVENT_CREATED_ROUTING_KEY = "broadcast.event.created";

    public EventService(EventRepository eventRepository, RSVPRepository rsvpRepository,
                         RabbitTemplate rabbitTemplate) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> getEventsByCategory(String category) {
        return eventRepository.findByCategory(category);
    }

    /**
     * Filtro combinado: categoría + rango de fechas (ISO YYYY-MM-DD).
     * Cualquier parámetro es opcional.
     */
    public List<Event> getEventsByFilter(String category, String startDate, String endDate) {
        List<Event> events = category != null
                ? eventRepository.findByCategory(category)
                : eventRepository.findAll();

        if (startDate != null) {
            events = events.stream()
                    .filter(e -> e.getDate() != null && e.getDate().compareTo(startDate) >= 0)
                    .toList();
        }
        if (endDate != null) {
            events = events.stream()
                    .filter(e -> e.getDate() != null && e.getDate().compareTo(endDate) <= 0)
                    .toList();
        }
        return events;
    }

    public Event getEventById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado: " + id));
    }

    public Event createEvent(Event event) {
        event.setAvailableCapacity(event.getCapacity());
        event.setStatus("ACTIVE");
        Event saved = eventRepository.save(event);
        publishEventCreated(saved);
        return saved;
    }

    /**
     * Publicidad del evento nuevo a todos los dispositivos (push vía
     * notification-service). No debe tumbar la creación del evento si
     * RabbitMQ está caído: se registra y se sigue.
     */
    private void publishEventCreated(Event event) {
        try {
            rabbitTemplate.convertAndSend(
                    notificationExchange,
                    EVENT_CREATED_ROUTING_KEY,
                    EventBroadcastMessage.builder()
                            .id(event.getId())
                            .name(event.getName())
                            .description(event.getDescription())
                            .category(event.getCategory())
                            .date(event.getDate())
                            .build());
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento creado {} para notificaciones push: {}",
                    event.getId(), e.getMessage());
        }
    }

    // ── RSVP ──────────────────────────────────────────────────────────────────

    /** E1: evento no activo → 409. E2: cupo agotado → 409. */
    public RSVPResponse confirmRSVP(String userId, String eventId) {
        Event event = getEventById(eventId);

        if (!"ACTIVE".equals(event.getStatus())) {
            throw new EventNotActiveException(eventId);
        }
        if (event.getAvailableCapacity() <= 0) {
            throw new CapacityExhaustedException(eventId);
        }

        RSVP existing = rsvpRepository.findByUserIdAndEventId(userId, eventId).orElse(null);
        if (existing != null && "CONFIRMED".equals(existing.getStatus())) {
            throw new RuntimeException("Ya tienes un RSVP confirmado para este evento.");
        }

        RSVP rsvp = existing != null ? existing : new RSVP();
        rsvp.setUserId(userId);
        rsvp.setEventId(eventId);
        rsvp.setStatus("CONFIRMED");
        rsvpRepository.save(rsvp);

        event.setAvailableCapacity(event.getAvailableCapacity() - 1);
        eventRepository.save(event);

        return new RSVPResponse("CONFIRMED", event.getAvailableCapacity(), getUserAgenda(userId));
    }

    /** E3: RSVP inexistente → 404. Cancelar libera cupo. */
    public RSVPResponse cancelRSVP(String userId, String eventId) {
        RSVP rsvp = rsvpRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new RsvpNotFoundException(userId, eventId));

        if ("CANCELLED".equals(rsvp.getStatus())) {
            throw new RuntimeException("El RSVP ya estaba cancelado.");
        }

        rsvp.setStatus("CANCELLED");
        rsvpRepository.save(rsvp);

        Event event = getEventById(eventId);
        event.setAvailableCapacity(event.getAvailableCapacity() + 1);
        eventRepository.save(event);

        return new RSVPResponse("CANCELLED", event.getAvailableCapacity(), getUserAgenda(userId));
    }

    /** Agenda del usuario: IDs de eventos con RSVP CONFIRMED. */
    public List<String> getUserAgenda(String userId) {
        return rsvpRepository.findByUserIdAndStatus(userId, "CONFIRMED")
                .stream()
                .map(RSVP::getEventId)
                .toList();
    }
}
