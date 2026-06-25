package eci.dosw.alpha.EventService;

import eci.dosw.alpha.EventService.dto.RSVPResponse;
import eci.dosw.alpha.EventService.exception.CapacityExhaustedException;
import eci.dosw.alpha.EventService.exception.EventNotActiveException;
import eci.dosw.alpha.EventService.exception.RsvpNotFoundException;
import eci.dosw.alpha.EventService.model.Event;
import eci.dosw.alpha.EventService.model.RSVP;
import eci.dosw.alpha.EventService.repository.EventRepository;
import eci.dosw.alpha.EventService.repository.RSVPRepository;
import eci.dosw.alpha.EventService.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceApplicationTests {

    @Mock EventRepository eventRepository;
    @Mock RSVPRepository rsvpRepository;

    @InjectMocks EventService service;

    private Event activeEvent;

    @BeforeEach
    void setUp() {
        activeEvent = new Event();
        activeEvent.setId("ev1");
        activeEvent.setStatus("ACTIVE");
        activeEvent.setCapacity(10);
        activeEvent.setAvailableCapacity(5);
        activeEvent.setCategory("TECH");
        activeEvent.setDate("2026-09-01");
    }

    // ── getAllEvents ──────────────────────────────────────────────────────────

    @Test
    void getAllEvents_returnsList() {
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent));
        assertThat(service.getAllEvents()).hasSize(1);
    }

    // ── getEventsByCategory ───────────────────────────────────────────────────

    @Test
    void getEventsByCategory_returnsFiltered() {
        when(eventRepository.findByCategory("TECH")).thenReturn(List.of(activeEvent));
        assertThat(service.getEventsByCategory("TECH")).hasSize(1);
    }

    // ── getEventsByFilter ─────────────────────────────────────────────────────

    @Test
    void getEventsByFilter_noFilters_returnsAll() {
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent));
        assertThat(service.getEventsByFilter(null, null, null)).hasSize(1);
    }

    @Test
    void getEventsByFilter_byCategory_returnsFiltered() {
        when(eventRepository.findByCategory("TECH")).thenReturn(List.of(activeEvent));
        assertThat(service.getEventsByFilter("TECH", null, null)).hasSize(1);
    }

    @Test
    void getEventsByFilter_startDate_excludesBefore() {
        Event old = new Event(); old.setDate("2025-01-01");
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent, old));

        List<Event> result = service.getEventsByFilter(null, "2026-01-01", null);

        assertThat(result).containsOnly(activeEvent);
    }

    @Test
    void getEventsByFilter_endDate_excludesAfter() {
        Event future = new Event(); future.setDate("2027-12-31");
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent, future));

        List<Event> result = service.getEventsByFilter(null, null, "2026-12-31");

        assertThat(result).containsOnly(activeEvent);
    }

    @Test
    void getEventsByFilter_rangeFiltersCorrectly() {
        Event tooOld   = new Event(); tooOld.setDate("2024-01-01");
        Event tooNew   = new Event(); tooNew.setDate("2027-01-01");
        when(eventRepository.findAll()).thenReturn(List.of(activeEvent, tooOld, tooNew));

        List<Event> result = service.getEventsByFilter(null, "2026-01-01", "2026-12-31");

        assertThat(result).containsOnly(activeEvent);
    }

    // ── getEventById ─────────────────────────────────────────────────────────

    @Test
    void getEventById_found_returnsEvent() {
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));
        assertThat(service.getEventById("ev1").getId()).isEqualTo("ev1");
    }

    @Test
    void getEventById_notFound_throws() {
        when(eventRepository.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getEventById("nope"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // ── createEvent ───────────────────────────────────────────────────────────

    @Test
    void createEvent_setsStatusAndCapacity() {
        Event ev = new Event();
        ev.setCapacity(20);
        when(eventRepository.save(ev)).thenReturn(ev);

        Event result = service.createEvent(ev);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getAvailableCapacity()).isEqualTo(20);
    }

    // ── confirmRSVP ───────────────────────────────────────────────────────────

    @Test
    void confirmRSVP_success_returnsConfirmed() {
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.empty());
        when(rsvpRepository.findByUserIdAndStatus("u1", "CONFIRMED")).thenReturn(List.of());
        when(eventRepository.save(any())).thenReturn(activeEvent);

        RSVPResponse res = service.confirmRSVP("u1", "ev1");

        assertThat(res.getRsvpStatus()).isEqualTo("CONFIRMED");
        assertThat(res.getAvailableCapacity()).isEqualTo(4);
    }

    @Test
    void confirmRSVP_E1_eventNotActive_throwsEventNotActiveException() {
        activeEvent.setStatus("CANCELLED");
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));

        assertThatThrownBy(() -> service.confirmRSVP("u1", "ev1"))
                .isInstanceOf(EventNotActiveException.class);
    }

    @Test
    void confirmRSVP_E2_noCapacity_throwsCapacityExhausted() {
        activeEvent.setAvailableCapacity(0);
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));

        assertThatThrownBy(() -> service.confirmRSVP("u1", "ev1"))
                .isInstanceOf(CapacityExhaustedException.class);
    }

    @Test
    void confirmRSVP_alreadyConfirmed_throws() {
        RSVP existing = new RSVP();
        existing.setStatus("CONFIRMED");
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.confirmRSVP("u1", "ev1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("RSVP confirmado");
    }

    @Test
    void confirmRSVP_reconfirmsAfterCancelled_success() {
        RSVP cancelled = new RSVP();
        cancelled.setStatus("CANCELLED");
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.of(cancelled));
        when(rsvpRepository.findByUserIdAndStatus("u1", "CONFIRMED")).thenReturn(List.of());
        when(eventRepository.save(any())).thenReturn(activeEvent);

        RSVPResponse res = service.confirmRSVP("u1", "ev1");

        assertThat(res.getRsvpStatus()).isEqualTo("CONFIRMED");
    }

    // ── cancelRSVP ────────────────────────────────────────────────────────────

    @Test
    void cancelRSVP_success_returnsCancelled() {
        RSVP rsvp = new RSVP();
        rsvp.setStatus("CONFIRMED");
        rsvp.setEventId("ev1");
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.of(rsvp));
        when(eventRepository.findById("ev1")).thenReturn(Optional.of(activeEvent));
        when(rsvpRepository.findByUserIdAndStatus("u1", "CONFIRMED")).thenReturn(List.of());
        when(eventRepository.save(any())).thenReturn(activeEvent);

        RSVPResponse res = service.cancelRSVP("u1", "ev1");

        assertThat(res.getRsvpStatus()).isEqualTo("CANCELLED");
        assertThat(res.getAvailableCapacity()).isEqualTo(6);
    }

    @Test
    void cancelRSVP_E3_rsvpNotFound_throwsRsvpNotFoundException() {
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelRSVP("u1", "ev1"))
                .isInstanceOf(RsvpNotFoundException.class);
    }

    @Test
    void cancelRSVP_alreadyCancelled_throws() {
        RSVP rsvp = new RSVP();
        rsvp.setStatus("CANCELLED");
        when(rsvpRepository.findByUserIdAndEventId("u1", "ev1")).thenReturn(Optional.of(rsvp));

        assertThatThrownBy(() -> service.cancelRSVP("u1", "ev1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cancelado");
    }

    // ── getUserAgenda ─────────────────────────────────────────────────────────

    @Test
    void getUserAgenda_returnsConfirmedEventIds() {
        RSVP r1 = new RSVP(); r1.setEventId("ev1");
        RSVP r2 = new RSVP(); r2.setEventId("ev2");
        when(rsvpRepository.findByUserIdAndStatus("u1", "CONFIRMED")).thenReturn(List.of(r1, r2));

        List<String> agenda = service.getUserAgenda("u1");

        assertThat(agenda).containsExactlyInAnyOrder("ev1", "ev2");
    }

    @Test
    void getUserAgenda_empty_returnsEmptyList() {
        when(rsvpRepository.findByUserIdAndStatus("u1", "CONFIRMED")).thenReturn(List.of());
        assertThat(service.getUserAgenda("u1")).isEmpty();
    }
}
