package com.devtrails.backend.triggerevent;

import com.devtrails.backend.config.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerEventService {

    private final TriggerEventRepository repo;

    // ── CREATE (called by Python when disruption starts) ──────────
    @Transactional
    public TriggerEventDTO.EventResponse createEvent(TriggerEventDTO.CreateRequest req) {

        // Idempotent — if same event_id arrives twice, return existing
        if (repo.existsByEventId(req.getEventId())) {
            TriggerEvent existing = repo.findByEventId(req.getEventId()).get();
            log.info("Duplicate event_id {} — returning existing", req.getEventId());
            return toResponse(existing);
        }

        TriggerEvent event = new TriggerEvent();
        event.setEventId(req.getEventId());
        event.setTriggerType(req.getTriggerType());
        event.setZoneId(req.getZoneId());
        event.setZoneName(req.getZoneName());
        event.setTriggerValue(req.getTriggerValue() != null
                ? BigDecimal.valueOf(req.getTriggerValue()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        event.setStatus("active");
        event.setStartedAt(LocalDateTime.now());

        repo.save(event);
        log.info("TriggerEvent created: {} | type={} | zone={}", req.getEventId(), req.getTriggerType(), req.getZoneId());
        return toResponse(event);
    }

    // ── RESOLVE (called by Python when condition normalises) ───────
    @Transactional
    public TriggerEventDTO.EventResponse resolveEvent(String eventId, TriggerEventDTO.ResolveRequest req) {

        TriggerEvent event = repo.findByEventId(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Trigger event not found: " + eventId));

        if ("resolved".equals(event.getStatus())) {
            log.info("Event {} already resolved", eventId);
            return toResponse(event);
        }

        LocalDateTime now = LocalDateTime.now();
        event.setEndedAt(now);
        event.setStatus("resolved");

        // Calculate actual disrupted hours
        long minutes = ChronoUnit.MINUTES.between(event.getStartedAt(), now);
        double hours = minutes / 60.0;
        event.setDisruptedHours(BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP));

        // Store live workers as comma-separated string
        if (req.getActiveWorkerIds() != null && !req.getActiveWorkerIds().isEmpty()) {
            event.setAffectedWorkerIds(String.join(",", req.getActiveWorkerIds()));
        }

        repo.save(event);
        log.info("TriggerEvent resolved: {} | disrupted={}hrs | workers={}",
                eventId, event.getDisruptedHours(), event.getAffectedWorkerIds());
        return toResponse(event);
    }

    // ── GET ACTIVE EVENTS FOR A ZONE ──────────────────────────────
    public List<TriggerEventDTO.EventResponse> getActiveByZone(String zoneId) {
        return repo.findActiveByZone(zoneId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── GET ALL ACTIVE (for admin / trigger engine) ────────────────
    public List<TriggerEventDTO.EventResponse> getAllActive() {
        return repo.findByStatus("active")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── GET ALL RESOLVED (for claim listener) ─────────────────────
    public List<TriggerEvent> getAllResolved() {
        return repo.findAllResolved();
    }

    // ── HELPER ────────────────────────────────────────────────────
    private TriggerEventDTO.EventResponse toResponse(TriggerEvent e) {
        long elapsed = e.getStartedAt() != null
                ? ChronoUnit.SECONDS.between(e.getStartedAt(), LocalDateTime.now())
                : 0;

        return new TriggerEventDTO.EventResponse(
                e.getEventId(),
                e.getTriggerType(),
                e.getZoneId(),
                e.getZoneName(),
                e.getTriggerValue(),
                e.getStatus(),
                e.getStartedAt(),
                e.getEndedAt(),
                e.getDisruptedHours(),
                e.getAffectedWorkerIds(),
                "active".equals(e.getStatus()) ? elapsed : null,
                "active".equals(e.getStatus())
                        ? "Disruption ongoing in " + e.getZoneName()
                        : "Disruption ended after " + e.getDisruptedHours() + " hrs"
        );
    }
}
