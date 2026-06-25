package eci.dosw.alpha.EventService.repository;

import eci.dosw.alpha.EventService.model.RSVP;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RSVPRepository extends MongoRepository<RSVP, String> {

    Optional<RSVP> findByUserIdAndEventId(String userId, String eventId);

    List<RSVP> findByUserIdAndStatus(String userId, String status);
}
