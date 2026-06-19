package eci.dosw.alpha.EventService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String name;
    private String description;
    private String category;

    private String date;

    private int capacity;
    private int availableCapacity;

    private String status; // ACTIVE, CANCELLED
}
