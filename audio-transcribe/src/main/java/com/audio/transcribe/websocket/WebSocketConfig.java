package com.audio.transcribe.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TranscriptionWebSocketHandler transcriptionWebSocketHandler;

    public WebSocketConfig(TranscriptionWebSocketHandler transcriptionWebSocketHandler) {
        this.transcriptionWebSocketHandler = transcriptionWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(transcriptionWebSocketHandler, "/ws/transcribe")
                .setAllowedOrigins("*"); // Restrict to your frontend origin in production
    }
}