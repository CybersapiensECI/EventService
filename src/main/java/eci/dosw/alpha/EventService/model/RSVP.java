package eci.dosw.alpha.EventService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "rsvps")
public class RSVP {

    @Id
    private String id;

    private String userId;
    private String eventId;

    private String status; // CONFIRMED, CANCELLED
}