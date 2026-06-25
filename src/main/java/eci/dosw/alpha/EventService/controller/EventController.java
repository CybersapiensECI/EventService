package eci.dosw.alpha.EventService.controller;

import eci.dosw.alpha.EventService.dto.RSVPRequest;
import eci.dosw.alpha.EventService.dto.RSVPResponse;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Listar eventos con filtros opcionales: categoría y rango de fechas (ISO YYYY-MM-DD).
     * RF B13: filtrar por categoría + dateRange.
     */
    @GetMapping
    public ResponseEntity<List<Event>> getEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        List<Event> events = eventService.getEventsByFilter(category, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        return ResponseEntity.ok(eventService.createEvent(event));
    }

    /**
     * Confirmar RSVP. Output: rsvpStatus, availableCapacity, agendaItems.
     * E1 → 409, E2 → 409 (manejados por GlobalExceptionHandler).
     */
    @PostMapping("/{id}/rsvp")
    public ResponseEntity<RSVPResponse> confirmRSVP(
            @PathVariable String id,
            @RequestBody RSVPRequest request) {

        return ResponseEntity.ok(eventService.confirmRSVP(request.getUserId(), id));
    }

    /**
     * Cancelar RSVP. Libera cupo.
     * E3: RSVP inexistente → 404.
     */
    @PutMapping("/{id}/rsvp")
    public ResponseEntity<RSVPResponse> cancelRSVP(
            @PathVariable String id,
            @RequestBody RSVPRequest request) {

        return ResponseEntity.ok(eventService.cancelRSVP(request.getUserId(), id));
    }

    /** Agenda del usuario: lista de eventIds con RSVP confirmado. */
    @GetMapping("/agenda")
    public ResponseEntity<List<String>> getAgenda(@RequestParam String userId) {
        return ResponseEntity.ok(eventService.getUserAgenda(userId));
    }
}
