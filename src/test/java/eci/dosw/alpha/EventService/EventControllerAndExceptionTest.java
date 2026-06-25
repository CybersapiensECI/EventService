package eci.dosw.alpha.EventService;

import eci.dosw.alpha.EventService.controller.EventController;
import eci.dosw.alpha.EventService.dto.RSVPRequest;
import eci.dosw.alpha.EventService.dto.RSVPResponse;
import eci.dosw.alpha.EventService.exception.CapacityExhaustedException;
import eci.dosw.alpha.EventService.exception.EventNotActiveException;
import eci.dosw.alpha.EventService.exception.GlobalExceptionHandler;
import eci.dosw.alpha.EventService.exception.RsvpNotFoundException;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerAndExceptionTest {

    @Mock EventService eventService;
    @InjectMocks EventController controller;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── EventController ────────────────────────────────────────────────────────

    @Test
    void getEvents_noFilters_delegatesToService() {
        Event ev = new Event();
        when(eventService.getEventsByFilter(null, null, null)).thenReturn(List.of(ev));

        ResponseEntity<List<Event>> res = controller.getEvents(null, null, null);

        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void getEvents_withCategory_passesParams() {
        when(eventService.getEventsByFilter("TECH", "2026-01-01", "2026-12-31")).thenReturn(List.of());

        ResponseEntity<List<Event>> res = controller.getEvents("TECH", "2026-01-01", "2026-12-31");

        assertThat(res.getBody()).isEmpty();
        verify(eventService).getEventsByFilter("TECH", "2026-01-01", "2026-12-31");
    }

    @Test
    void getEvent_byId_delegatesToService() {
        Event ev = new Event();
        ev.setId("ev1");
        when(eventService.getEventById("ev1")).thenReturn(ev);

        assertThat(controller.getEvent("ev1").getBody().getId()).isEqualTo("ev1");
    }

    @Test
    void createEvent_delegatesToService() {
        Event ev = new Event();
        when(eventService.createEvent(ev)).thenReturn(ev);

        assertThat(controller.createEvent(ev).getBody()).isEqualTo(ev);
    }

    @Test
    void confirmRSVP_delegatesToService() {
        RSVPRequest req = new RSVPRequest();
        req.setUserId("u1");
        RSVPResponse rsvpRes = new RSVPResponse("CONFIRMED", 4, List.of("ev1"));
        when(eventService.confirmRSVP("u1", "ev1")).thenReturn(rsvpRes);

        ResponseEntity<RSVPResponse> res = controller.confirmRSVP("ev1", req);

        assertThat(res.getBody().getRsvpStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void cancelRSVP_delegatesToService() {
        RSVPRequest req = new RSVPRequest();
        req.setUserId("u1");
        RSVPResponse rsvpRes = new RSVPResponse("CANCELLED", 5, List.of());
        when(eventService.cancelRSVP("u1", "ev1")).thenReturn(rsvpRes);

        ResponseEntity<RSVPResponse> res = controller.cancelRSVP("ev1", req);

        assertThat(res.getBody().getRsvpStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void getAgenda_delegatesToService() {
        when(eventService.getUserAgenda("u1")).thenReturn(List.of("ev1", "ev2"));

        ResponseEntity<List<String>> res = controller.getAgenda("u1");

        assertThat(res.getBody()).containsExactlyInAnyOrder("ev1", "ev2");
    }

    // ── GlobalExceptionHandler ─────────────────────────────────────────────────

    @Test
    void handler_eventNotActive_returns409WithE1() {
        Map<String, String> body = handler.handleEventNotActive(new EventNotActiveException("ev1"));
        assertThat(body.get("code")).isEqualTo("E1");
        assertThat(body.get("error")).contains("ev1");
    }

    @Test
    void handler_capacityExhausted_returns409WithE2() {
        Map<String, String> body = handler.handleCapacityExhausted(new CapacityExhaustedException("ev2"));
        assertThat(body.get("code")).isEqualTo("E2");
        assertThat(body.get("error")).contains("ev2");
    }

    @Test
    void handler_rsvpNotFound_returns404WithE3() {
        Map<String, String> body = handler.handleRsvpNotFound(new RsvpNotFoundException("u1", "ev3"));
        assertThat(body.get("code")).isEqualTo("E3");
        assertThat(body.get("error")).contains("u1");
    }

    @Test
    void handler_generic_returns500() {
        Map<String, String> body = handler.handleGeneric(new RuntimeException("boom"));
        assertThat(body.get("error")).isEqualTo("boom");
    }

    // ── Exception messages ─────────────────────────────────────────────────────

    @Test
    void eventNotActiveException_messageContainsId() {
        assertThat(new EventNotActiveException("myEv").getMessage()).contains("myEv");
    }

    @Test
    void capacityExhaustedException_messageContainsId() {
        assertThat(new CapacityExhaustedException("myEv").getMessage()).contains("myEv");
    }

    @Test
    void rsvpNotFoundException_messageContainsBoth() {
        RsvpNotFoundException ex = new RsvpNotFoundException("u99", "ev99");
        assertThat(ex.getMessage()).contains("u99").contains("ev99");
    }
}
