package eci.dosw.alpha.EventService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSVPResponse {

    private String rsvpStatus;
    private int availableCapacity;
    private List<String> agendaItems;
}
