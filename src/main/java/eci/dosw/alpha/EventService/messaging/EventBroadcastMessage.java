package eci.dosw.alpha.EventService.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Publicidad de evento nuevo: notification-service la consume y manda un
 * push a todos los dispositivos (topic FCM "events_broadcast"), no un
 * recordatorio dirigido a un usuario puntual.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventBroadcastMessage {

    private String id;
    private String name;
    private String description;
    private String category;
    private String date;
}
