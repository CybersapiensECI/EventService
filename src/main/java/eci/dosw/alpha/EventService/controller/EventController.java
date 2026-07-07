package eci.dosw.alpha.EventService.controller;

import eci.dosw.alpha.EventService.dto.CreateEventRequest;
import eci.dosw.alpha.EventService.dto.RSVPRequest;
import eci.dosw.alpha.EventService.dto.RSVPResponse;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Eventos", description = "Gestión de eventos universitarios: listado, filtros, creación y reservas RSVP")
@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Operation(summary = "Listar eventos",
               description = "Devuelve todos los eventos. Acepta filtros opcionales de categoría y rango de fechas (ISO YYYY-MM-DD).")
    @ApiResponse(responseCode = "200", description = "Lista de eventos (puede ser vacía)")
    @GetMapping
    public ResponseEntity<List<Event>> getEvents(
            @Parameter(description = "Categoría del evento (ej. TECH, BIENESTAR)")
            @RequestParam(required = false) String category,
            @Parameter(description = "Fecha de inicio del rango (YYYY-MM-DD)")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "Fecha de fin del rango (YYYY-MM-DD)")
            @RequestParam(required = false) String endDate) {

        List<Event> events = eventService.getEventsByFilter(category, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Obtener evento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento encontrado"),
        @ApiResponse(responseCode = "500", description = "Evento no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEvent(
            @Parameter(description = "ID del evento") @PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @Operation(summary = "Crear evento",
               description = "Registra un nuevo evento. El estado se fija en ACTIVE y el cupo disponible se inicializa con la capacidad total.")
    @ApiResponse(responseCode = "200", description = "Evento creado")
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setDate(request.getDate());
        event.setCapacity(request.getCapacity());
        return ResponseEntity.ok(eventService.createEvent(event));
    }

    @Operation(summary = "Confirmar RSVP",
               description = "Reserva un cupo para el usuario en el evento. E1: evento no activo → 409. E2: cupo agotado → 409.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "RSVP confirmado correctamente"),
        @ApiResponse(responseCode = "409", description = "E1: evento no activo / E2: sin cupo disponible")
    })
    @PostMapping("/{id}/rsvp")
    public ResponseEntity<RSVPResponse> confirmRSVP(
            @Parameter(description = "ID del evento") @PathVariable String id,
            @RequestBody RSVPRequest request) {
        return ResponseEntity.ok(eventService.confirmRSVP(request.getUserId(), id));
    }

    @Operation(summary = "Cancelar RSVP",
               description = "Cancela la reserva del usuario y libera el cupo. E3: RSVP no encontrado → 404.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "RSVP cancelado y cupo liberado"),
        @ApiResponse(responseCode = "404", description = "E3: RSVP no encontrado para ese usuario/evento")
    })
    @PutMapping("/{id}/rsvp")
    public ResponseEntity<RSVPResponse> cancelRSVP(
            @Parameter(description = "ID del evento") @PathVariable String id,
            @RequestBody RSVPRequest request) {
        return ResponseEntity.ok(eventService.cancelRSVP(request.getUserId(), id));
    }

    @Operation(summary = "Agenda del usuario",
               description = "Devuelve los IDs de los eventos en los que el usuario tiene RSVP confirmado.")
    @ApiResponse(responseCode = "200", description = "Lista de IDs de eventos confirmados")
    @GetMapping("/agenda")
    public ResponseEntity<List<String>> getAgenda(
            @Parameter(description = "ID del usuario") @RequestParam String userId) {
        return ResponseEntity.ok(eventService.getUserAgenda(userId));
    }
}
