package com.audio.transcribe.websocket;

import com.audio.transcribe.service.DeepgramService;
import com.audio.transcribe.service.DeepgramLiveConnection;
import com.audio.transcribe.CommandHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class TranscriptionWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DeepgramService deepgramService;

    @Autowired
    private CommandHandler commandHandler;

    private final Map<String, DeepgramLiveConnection> connections = new HashMap<>();
    private final Map<String, StringBuilder> transcriptionBuffers = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("WebSocket connection established: {}", sessionId);

        try {
            // Start Deepgram live transcription
            DeepgramLiveConnection connection = deepgramService.startLiveTranscription(session, this);

            if (connection != null) {
                connections.put(sessionId, connection);
                transcriptionBuffers.put(sessionId, new StringBuilder());

                // Send connection confirmation
                sendMessage(session, createMessage("status", "Connection established. Ready for audio."));
                log.info("Deepgram connection ready for session: {}", sessionId);
            } else {
                // Fallback if Deepgram is not available
                sendMessage(session, createMessage("warning", "Deepgram not available. Please configure DEEPGRAM_API_KEY."));
                log.warn("Deepgram connection failed for session: {}. Fallback mode active.", sessionId);
            }
        } catch (Exception e) {
            log.error("Error in WebSocket connection setup", e);
            try {
                sendMessage(session, createMessage("error", "Connection failed"));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sessionId = session.getId();

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            if ("audio".equals(type)) {
                String base64Audio = json.get("data").asText();
                byte[] audioData = Base64.getDecoder().decode(base64Audio);

                // Send audio to Deepgram
                DeepgramLiveConnection connection = connections.get(sessionId);
                if (connection != null && connection.isConnected()) {
                    connection.sendAudioChunk(audioData);
                } else {
                    log.warn("Deepgram connection not available for session: {}", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling text message", e);
            try {
                sendMessage(session, createMessage("error", "Failed to process audio"));
            } catch (Exception ex) {
                log.error("Failed to send error message", ex);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String sessionId = session.getId();

        try {
            byte[] audioData = message.getPayload().array();

            // Send audio to Deepgram
            DeepgramLiveConnection connection = connections.get(sessionId);
            if (connection != null && connection.isConnected()) {
                connection.sendAudioChunk(audioData);
            } else {
                log.warn("Deepgram connection not available for session: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error handling binary message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        String sessionId = session.getId();
        log.info("WebSocket connection closed: {} - {}", sessionId, closeStatus);

        // Clean up
        DeepgramLiveConnection connection = connections.remove(sessionId);
        if (connection != null) {
            connection.close();
        }
        transcriptionBuffers.remove(sessionId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = session.getId();
        log.error("WebSocket transport error for session {}: {}", sessionId, exception.getMessage());

        DeepgramLiveConnection connection = connections.remove(sessionId);
        if (connection != null) {
            connection.close();
        }
        transcriptionBuffers.remove(sessionId);
    }

    /**
     * Send a message back to the client
     */
    private void sendMessage(WebSocketSession session, String message) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    /**
     * Create a JSON message to send to client
     */
    private String createMessage(String type, String text) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", type);
            msg.put("text", text);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.error("Error creating message", e);
            return "{\"type\":\"error\",\"text\":\"Internal error\"}";
        }
    }

    /**
     * Helper to send transcript result to frontend
     */
    public void sendTranscriptResult(WebSocketSession session, String transcript, boolean isFinal) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "transcript");
            msg.put("text", transcript);
            msg.put("isFinal", isFinal);

            String jsonMessage = objectMapper.writeValueAsString(msg);
            sendMessage(session, jsonMessage);

            // If final, process through CommandHandler
            if (isFinal && transcript != null && !transcript.isBlank()) {
                commandHandler.handleTranscription(transcript);
            }
        } catch (Exception e) {
            log.error("Error sending transcript result", e);
        }
    }
}
