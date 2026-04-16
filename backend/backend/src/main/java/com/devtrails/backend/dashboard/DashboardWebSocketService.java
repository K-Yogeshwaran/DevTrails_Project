package com.devtrails.backend.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DashboardWebSocketService extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardWebSocketService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Store active sessions by user type and ID
    private final Map<String, List<WebSocketSession>> workerSessions = new ConcurrentHashMap<>();
    private final Map<String, List<WebSocketSession>> insurerSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());
        
        // Extract user information from session attributes
        String userType = (String) session.getAttributes().get("userType");
        String userId = (String) session.getAttributes().get("userId");
        
        if (userId == null || userType == null) {
            log.warn("WebSocket session missing user info: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing user information"));
            return;
        }

        // Add session to appropriate map
        switch (userType.toUpperCase()) {
            case "WORKER":
                workerSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
                break;
            case "INSURER":
                insurerSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
                break;
            case "ADMIN":
                adminSessions.put(userId, session);
                break;
            default:
                log.warn("Unknown user type: {}", userType);
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid user type"));
                return;
        }

        // Send initial data
        sendInitialData(session, userType, userId);
        
        log.info("WebSocket session registered: {} for user: {} ({})", 
                session.getId(), userId, userType);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String userId = (String) session.getAttributes().get("userId");
        String userType = (String) session.getAttributes().get("userType");
        
        log.debug("Received WebSocket message from {}: {}", userId, payload);
        
        try {
            // Handle different message types
            Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
            String messageType = (String) messageData.get("type");
            
            switch (messageType) {
                case "SUBSCRIBE":
                    handleSubscription(session, messageData);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscription(session, messageData);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    log.warn("Unknown message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", e.getMessage(), e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String userType = (String) session.getAttributes().get("userType");
        
        log.info("WebSocket connection closed: {} for user: {} ({}) - {}", 
                session.getId(), userId, userType, status.getReason());
        
        // Remove session from appropriate map
        removeSession(session, userType, userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        String userType = (String) session.getAttributes().get("userType");
        
        log.error("WebSocket transport error for session: {} user: {} ({})", 
                session.getId(), userId, userType, exception);
        
        // Remove session
        removeSession(session, userType, userId);
    }

    // Public methods for sending updates
    @Async
    public void sendWorkerUpdate(String workerId, Object data) {
        List<WebSocketSession> sessions = workerSessions.get(workerId);
        if (sessions != null) {
            DashboardUpdate update = DashboardUpdate.workerUpdate(workerId, data);
            sendToSessions(sessions, update);
        }
    }

    @Async
    public void sendInsurerUpdate(Object data) {
        insurerSessions.values().forEach(sessions -> {
            DashboardUpdate update = DashboardUpdate.insurerUpdate(data);
            sendToSessions(sessions, update);
        });
    }

    @Async
    public void sendAdminUpdate(Object data) {
        DashboardUpdate update = new DashboardUpdate("ADMIN", "admin", data, 
                java.time.LocalDateTime.now(), "DASHBOARD_SERVICE");
        
        adminSessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(update)));
                }
            } catch (IOException e) {
                log.error("Error sending admin update: {}", e.getMessage(), e);
            }
        });
    }

    @Async
    public void broadcastToAllWorkers(Object data) {
        workerSessions.values().forEach(sessions -> {
            DashboardUpdate update = DashboardUpdate.insurerUpdate(data); // Reuse for broadcast
            sendToSessions(sessions, update);
        });
    }

    @Async
    public void sendUpdate(DashboardUpdate update) {
        String targetId = update.getTargetId();
        String updateType = update.getUpdateType();
        
        switch (updateType) {
            case "WORKER":
                sendWorkerUpdate(targetId, update.getData());
                break;
            case "INSURER":
                sendInsurerUpdate(update.getData());
                break;
            case "ADMIN":
                sendAdminUpdate(update.getData());
                break;
            default:
                log.warn("Unknown update type: {}", updateType);
        }
    }

    // Private helper methods
    private void sendInitialData(WebSocketSession session, String userType, String userId) {
        try {
            Map<String, Object> initialData = Map.of(
                    "type", "INITIAL_DATA",
                    "userType", userType,
                    "userId", userId,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initialData)));
        } catch (IOException e) {
            log.error("Error sending initial data: {}", e.getMessage(), e);
        }
    }

    private void handleSubscription(WebSocketSession session, Map<String, Object> messageData) {
        String userId = (String) session.getAttributes().get("userId");
        String userType = (String) session.getAttributes().get("userType");
        String subscriptionType = (String) messageData.get("subscriptionType");
        
        log.info("User {} ({}) subscribed to: {}", userId, userType, subscriptionType);
        
        // Store subscription in session attributes
        session.getAttributes().put("subscription_" + subscriptionType, true);
        
        // Send confirmation
        try {
            Map<String, Object> confirmation = Map.of(
                    "type", "SUBSCRIPTION_CONFIRMED",
                    "subscriptionType", subscriptionType,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmation)));
        } catch (IOException e) {
            log.error("Error sending subscription confirmation: {}", e.getMessage(), e);
        }
    }

    private void handleUnsubscription(WebSocketSession session, Map<String, Object> messageData) {
        String userId = (String) session.getAttributes().get("userId");
        String userType = (String) session.getAttributes().get("userType");
        String subscriptionType = (String) messageData.get("subscriptionType");
        
        log.info("User {} ({}) unsubscribed from: {}", userId, userType, subscriptionType);
        
        // Remove subscription from session attributes
        session.getAttributes().remove("subscription_" + subscriptionType);
        
        // Send confirmation
        try {
            Map<String, Object> confirmation = Map.of(
                    "type", "UNSUBSCRIPTION_CONFIRMED",
                    "subscriptionType", subscriptionType,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmation)));
        } catch (IOException e) {
            log.error("Error sending unsubscription confirmation: {}", e.getMessage(), e);
        }
    }

    private void handlePing(WebSocketSession session) {
        try {
            Map<String, Object> pong = Map.of(
                    "type", "PONG",
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
        } catch (IOException e) {
            log.error("Error sending pong: {}", e.getMessage(), e);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                    "type", "ERROR",
                    "message", errorMessage,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (IOException e) {
            log.error("Error sending error message: {}", e.getMessage(), e);
        }
    }

    private void sendToSessions(List<WebSocketSession> sessions, DashboardUpdate update) {
        if (sessions == null || sessions.isEmpty()) return;
        
        String message;
        try {
            message = objectMapper.writeValueAsString(update);
        } catch (Exception e) {
            log.error("Error serializing update: {}", e.getMessage(), e);
            return;
        }
        
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    return false; // Keep session
                } else {
                    log.debug("Removing closed session: {}", session.getId());
                    return true; // Remove closed session
                }
            } catch (IOException e) {
                log.error("Error sending message to session {}: {}", 
                        session.getId(), e.getMessage(), e);
                return true; // Remove problematic session
            }
        });
    }

    private void removeSession(WebSocketSession session, String userType, String userId) {
        switch (userType.toUpperCase()) {
            case "WORKER":
                List<WebSocketSession> workerSess = workerSessions.get(userId);
                if (workerSess != null) {
                    workerSess.remove(session);
                    if (workerSess.isEmpty()) {
                        workerSessions.remove(userId);
                    }
                }
                break;
            case "INSURER":
                List<WebSocketSession> insurerSess = insurerSessions.get(userId);
                if (insurerSess != null) {
                    insurerSess.remove(session);
                    if (insurerSess.isEmpty()) {
                        insurerSessions.remove(userId);
                    }
                }
                break;
            case "ADMIN":
                adminSessions.remove(userId);
                break;
        }
    }

    // Health check method
    public Map<String, Object> getConnectionStats() {
        return Map.of(
                "totalWorkerConnections", workerSessions.values().stream()
                        .mapToInt(List::size)
                        .sum(),
                "totalInsurerConnections", insurerSessions.values().stream()
                        .mapToInt(List::size)
                        .sum(),
                "totalAdminConnections", adminSessions.size(),
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}
