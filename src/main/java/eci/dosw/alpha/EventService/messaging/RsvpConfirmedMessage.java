package eci.dosw.alpha.EventService.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** GamificationService la consume para desbloquear monas de asistencia a eventos. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RsvpConfirmedMessage {
    private String userId;
    private String eventId;
    private String eventName;
}
