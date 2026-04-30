package com.audio.transcribe.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * DEPRECATED: This handler is disabled.
 * Use TranscriptionWebSocketHandler instead for audio transcription.
 */
@Component
public class DeepgramWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeepgramWebSocketHandler.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("DeepgramWebSocketHandler is deprecated. Use TranscriptionWebSocketHandler instead.");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // No-op
    }
}