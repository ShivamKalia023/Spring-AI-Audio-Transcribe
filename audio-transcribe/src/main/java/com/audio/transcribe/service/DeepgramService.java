package com.audio.transcribe.service;


import com.audio.transcribe.config.DeepgramConfig;
import com.audio.transcribe.websocket.TranscriptionWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Service
public class DeepgramService {

    private static final Logger log = LoggerFactory.getLogger(DeepgramService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEEPGRAM_REST_URL =
            "https://api.deepgram.com/v1/listen?model=nova-2&smart_format=true&punctuate=true";

    // Simplified URL — channels=1 explicit, removed smart_format from WS
    private static final String DEEPGRAM_STREAMING_URL =
            "wss://api.deepgram.com/v1/listen?model=nova-2&punctuate=true&encoding=linear16&sample_rate=16000&channels=1";

    private final OkHttpClient httpClient;
    private final DeepgramConfig deepgramConfig;

    public DeepgramService(OkHttpClient httpClient, DeepgramConfig deepgramConfig) {
        this.httpClient = httpClient;
        this.deepgramConfig = deepgramConfig;
    }

    /**
     * Transcribe a pre-recorded audio file (raw bytes).
     */
    public String transcribeAudio(byte[] audioBytes, String contentType) throws IOException {
        RequestBody body = RequestBody.create(audioBytes, MediaType.parse(contentType));

        Request request = new Request.Builder()
                .url(DEEPGRAM_REST_URL)
                .addHeader("Authorization", "Token " + deepgramConfig.getApiKey())
                .addHeader("Content-Type", contentType)
                .post(body)
                .build();

        log.info("Sending pre-recorded audio to Deepgram ({} bytes, type={})", audioBytes.length, contentType);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                throw new IOException("Deepgram REST API error " + response.code() + ": " + errorBody);
            }
            String result = response.body().string();
            log.info("Deepgram pre-recorded transcription successful");
            return result;
        }
    }

    /**
     * Open a real-time WebSocket connection to Deepgram.
     * Fix: use .header() not .addHeader() to prevent duplicate headers.
     * Only Authorization header needed — no Content-Type for WS upgrade.
     */
    public WebSocket openStreamingConnection(WebSocketListener listener) {
        String apiKey = deepgramConfig.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Deepgram API key is missing — cannot open streaming connection");
            throw new IllegalStateException("DEEPGRAM_API_KEY is not configured");
        }

        Request request = new Request.Builder()
                .url(DEEPGRAM_STREAMING_URL)
                .header("Authorization", "Token " + apiKey)
                .build();

        log.info("Opening Deepgram WebSocket: {}", DEEPGRAM_STREAMING_URL);
        return httpClient.newWebSocket(request, listener);
    }

    /**
     * Validate API key and connectivity against Deepgram REST API.
     */
    public boolean testConnection() {
        String key = deepgramConfig.getApiKey();
        if (key == null || key.isBlank()) {
            log.error("Deepgram API key is not configured!");
            return false;
        }

        Request request = new Request.Builder()
                .url("https://api.deepgram.com/v1/projects")
                .header("Authorization", "Token " + key)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Deepgram connection test passed (HTTP {})", response.code());
                return true;
            } else {
                log.warn("Deepgram connection test returned HTTP {} — check your API key", response.code());
                return false;
            }
        } catch (IOException e) {
            log.error("Deepgram connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Start live transcription for a client WebSocket session.
     */
    public DeepgramLiveConnection startLiveTranscription(WebSocketSession clientSession, TranscriptionWebSocketHandler handler) {
        return new DeepgramLiveConnection(this, clientSession, handler);
    }
}
