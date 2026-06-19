package eci.dosw.alpha.EventService.controller;

import eci.dosw.alpha.EventService.dto.RSVPRequest;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // Listar eventos
    @GetMapping
    public List<Event> getEvents(@RequestParam(required = false) String category) {

        if (category != null) {
            return eventService.getEventsByCategory(category);
        }

        return eventService.getAllEvents();
    }

    // Obtener evento
    @GetMapping("/{id}")
    public Event getEvent(@PathVariable String id) {
        return eventService.getEventById(id);
    }

    // Crear evento
    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return eventService.createEvent(event);
    }

    // Confirmar RSVP
    @PostMapping("/{id}/rsvp")
    public String confirmRSVP(@PathVariable String id,
                              @RequestBody RSVPRequest request) {

        return eventService.confirmRSVP(request.getUserId(), id);
    }

    // Cancelar RSVP
    @PutMapping("/{id}/rsvp")
    public String cancelRSVP(@PathVariable String id,
                             @RequestBody Map<String, String> body) {

        String userId = body.get("userId");
        return eventService.cancelRSVP(userId, id);
    }
}