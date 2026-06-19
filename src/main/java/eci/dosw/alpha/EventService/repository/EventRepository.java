package eci.dosw.alpha.EventService.repository;

import eci.dosw.alpha.EventService.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {

    List<Event> findByCategory(String category);

}