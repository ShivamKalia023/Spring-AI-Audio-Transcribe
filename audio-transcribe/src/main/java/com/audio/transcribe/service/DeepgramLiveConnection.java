package com.audio.transcribe.service;

import com.audio.transcribe.websocket.TranscriptionWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

/**
 * Class to manage a live Deepgram WebSocket connection.
 */
public class DeepgramLiveConnection implements WebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(DeepgramLiveConnection.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeepgramService outer;
    private final WebSocketSession clientSession;
    private final TranscriptionWebSocketHandler handler;
    private WebSocket webSocket;
    private boolean connected = false;

    public DeepgramLiveConnection(DeepgramService outer, WebSocketSession clientSession, TranscriptionWebSocketHandler handler) {
        this.outer = outer;
        this.clientSession = clientSession;
        this.handler = handler;
        connect();
    }

    /**
     * Connect to Deepgram WebSocket.
     */
    private void connect() {
        try {
            this.webSocket = outer.openStreamingConnection(this);
            this.connected = true;
            log.info("Deepgram live connection established for client session: {}", clientSession.getId());
        } catch (Exception e) {
            log.error("Failed to connect to Deepgram for client session: {}", clientSession.getId(), e);
            this.connected = false;
        }
    }

    /**
     * Check if the connection is active.
     */
    public boolean isConnected() {
        return connected && webSocket != null;
    }

    /**
     * Send an audio chunk to Deepgram.
     */
    public void sendAudioChunk(byte[] audioData) {
        if (isConnected()) {
            webSocket.send(ByteString.of(audioData));
        } else {
            log.warn("Cannot send audio chunk: Deepgram connection not active for client session: {}", clientSession.getId());
        }
    }

    /**
     * Close the connection.
     */
    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "Closing connection");
            connected = false;
            log.info("Deepgram live connection closed for client session: {}", clientSession.getId());
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info("Deepgram WebSocket opened for client session: {}", clientSession.getId());
        connected = true;
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            JsonNode json = objectMapper.readTree(text);
            String type = json.get("type").asText();

            if ("Results".equals(type)) {
                JsonNode channel = json.get("channel");
                if (channel != null && channel.has("alternatives")) {
                    JsonNode alternatives = channel.get("alternatives");
                    if (alternatives.size() > 0) {
                        JsonNode alt = alternatives.get(0);
                        String transcript = alt.get("transcript").asText();
                        boolean isFinal = json.has("is_final") && json.get("is_final").asBoolean();

                        if (transcript != null && !transcript.trim().isEmpty()) {
                            handler.sendTranscriptResult(clientSession, transcript, isFinal);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Deepgram message for client session: {}", clientSession.getId(), e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        // Binary messages not expected from Deepgram
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        log.info("Deepgram WebSocket closing for client session: {} - code: {}, reason: {}", clientSession.getId(), code, reason);
        connected = false;
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log.info("Deepgram WebSocket closed for client session: {} - code: {}, reason: {}", clientSession.getId(), code, reason);
        connected = false;
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        log.error("Deepgram WebSocket failure for client session: {}", clientSession.getId(), t);
        connected = false;
    }
}
