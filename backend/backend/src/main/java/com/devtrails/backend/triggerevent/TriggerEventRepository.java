package com.devtrails.backend.triggerevent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TriggerEventRepository extends JpaRepository<TriggerEvent, Long> {

    Optional<TriggerEvent> findByEventId(String eventId);

    List<TriggerEvent> findByStatus(String status);

    List<TriggerEvent> findByZoneIdAndStatus(String zoneId, String status);

    // Active events for a specific zone — shown on dashboard
    @Query("SELECT t FROM TriggerEvent t WHERE t.zoneId = :zoneId AND t.status = 'active' ORDER BY t.startedAt DESC")
    List<TriggerEvent> findActiveByZone(@Param("zoneId") String zoneId);

    // Resolved events not yet processed into claims
    @Query("SELECT t FROM TriggerEvent t WHERE t.status = 'resolved' ORDER BY t.endedAt DESC")
    List<TriggerEvent> findAllResolved();

    boolean existsByEventId(String eventId);
}
