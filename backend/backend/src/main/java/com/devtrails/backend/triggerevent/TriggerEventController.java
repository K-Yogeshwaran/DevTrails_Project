package com.devtrails.backend.triggerevent;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trigger-events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TriggerEventController {

    private final TriggerEventService service;

    // Called by Python trigger engine when disruption starts
    @PostMapping
    public ResponseEntity<TriggerEventDTO.EventResponse> create(
            @RequestBody TriggerEventDTO.CreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createEvent(req));
    }

    // Called by Python trigger engine when condition normalises
    // Uses request body for eventId to avoid colon-in-URL path variable issues
    @PostMapping("/resolve")
    public ResponseEntity<TriggerEventDTO.EventResponse> resolve(
            @RequestBody TriggerEventDTO.ResolveRequest req) {
        return ResponseEntity.ok(service.resolveEvent(req.getEventId(), req));
    }

    // Called by frontend — active disruptions in a specific zone
    @GetMapping("/active/{zoneId}")
    public ResponseEntity<List<TriggerEventDTO.EventResponse>> getActiveByZone(
            @PathVariable String zoneId) {
        return ResponseEntity.ok(service.getActiveByZone(zoneId));
    }

    // Called by frontend — all active events
    @GetMapping("/active")
    public ResponseEntity<List<TriggerEventDTO.EventResponse>> getAllActive() {
        return ResponseEntity.ok(service.getAllActive());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "trigger-event-service"));
    }
}
