package com.audio.transcribe.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * This controller is disabled. 
 * Audio transcription now works via WebSocket connections at /ws/transcribe
 */
@RestController
@RequestMapping("/api/deepgram")
class DeepgramController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Use WebSocket connection at /ws/transcribe for audio transcription"
        ));
    }
}