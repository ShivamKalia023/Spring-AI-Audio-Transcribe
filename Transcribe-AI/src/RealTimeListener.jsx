import { useState, useRef, useEffect } from "react";
import axios from "axios";

const API_URL = "http://localhost:8080/api/transcribe";
const WS_URL = "ws://localhost:8080/ws/transcribe";
const SILENCE_THRESHOLD = 0.02;   // RMS below this counts as silence
const SILENCE_DURATION_MS = 3000; // 3 seconds of silence → auto-submit
const AUDIO_CHUNK_SIZE = 8000; // Send audio in ~250ms chunks (32kHz)

// Encode raw Float32 PCM as a 16-bit mono WAV Blob.
function encodeWAV(samples, sampleRate) {
  const len  = samples.length;
  const buf  = new ArrayBuffer(44 + len * 2);
  const view = new DataView(buf);
  const ws   = (off, str) => {
    for (let i = 0; i < str.length; i++) view.setUint8(off + i, str.charCodeAt(i));
  };
  ws(0, "RIFF"); view.setUint32(4, 36 + len * 2, true);
  ws(8, "WAVE"); ws(12, "fmt ");
  view.setUint32(16, 16, true);             // subchunk size
  view.setUint16(20, 1,  true);             // PCM
  view.setUint16(22, 1,  true);             // mono
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true); // byte rate
  view.setUint16(32, 2,  true);             // block align
  view.setUint16(34, 16, true);             // bits per sample
  ws(36, "data"); view.setUint32(40, len * 2, true);
  for (let i = 0; i < len; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(44 + i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
  return new Blob([buf], { type: "audio/wav" });
}

// Encode Float32 PCM as base64 for WebSocket transmission
function encodeAudioBase64(samples, sampleRate) {
  const len  = samples.length;
  const buf  = new ArrayBuffer(44 + len * 2);
  const view = new DataView(buf);
  const ws   = (off, str) => {
    for (let i = 0; i < str.length; i++) view.setUint8(off + i, str.charCodeAt(i));
  };
  ws(0, "RIFF"); view.setUint32(4, 36 + len * 2, true);
  ws(8, "WAVE"); ws(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1,  true);
  view.setUint16(22, 1,  true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2,  true);
  view.setUint16(34, 16, true);
  ws(36, "data"); view.setUint32(40, len * 2, true);
  for (let i = 0; i < len; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(44 + i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
  return btoa(String.fromCharCode(...new Uint8Array(buf)));
}

// Decode a MediaRecorder blob → PCM via AudioContext → re-encode as WAV → send.
// This avoids ScriptProcessorNode entirely and works reliably across browsers.
async function blobToWavAndSend(blob, onStatus, onResult, onError) {
  if (!blob || blob.size < 1000) return;
  try {
    const arrayBuf   = await blob.arrayBuffer();
    const decodeCtx  = new AudioContext();
    const audioBuf   = await decodeCtx.decodeAudioData(arrayBuf);
    await decodeCtx.close();

    const samples    = audioBuf.getChannelData(0);
    const sampleRate = audioBuf.sampleRate;

    // Skip clips shorter than 0.5 s or that are pure silence
    if (samples.length < sampleRate * 0.5) return;
    let peak = 0;
    for (let i = 0; i < samples.length; i++) {
      const a = Math.abs(samples[i]);
      if (a > peak) peak = a;
    }
    if (peak < 0.005) return; // silent recording — skip API call

    const wav = encodeWAV(samples, sampleRate);
    const fd  = new FormData();
    fd.append("file", wav, "audio.wav");

    onStatus("Transcribing…");
    const { data } = await axios.post(API_URL, fd, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    const text = data?.trim();
    if (text) onResult(text);
    onStatus("");
  } catch (err) {
    console.error("Transcription error:", err);
    onError("Transcription failed.");
    onStatus("");
  }
}

const RealTimeListener = () => {
  const [mode, setMode]               = useState("ptt");
  const [isRecording, setIsRecording] = useState(false);
  const [status, setStatus]           = useState("");
  const [transcription, setTranscription] = useState("");
  const [error, setError]             = useState("");
  const [wsStatus, setWsStatus]       = useState("disconnected");

  const streamRef       = useRef(null);
  const recorderRef     = useRef(null);
  const audioCtxRef     = useRef(null);   // used only for AnalyserNode in always-mode
  const silenceTimerRef = useRef(null);   // setInterval id
  const silenceStartRef = useRef(null);   // timestamp when silence began
  const activeRef       = useRef(false);  // true while keep-listening is on
  const wsRef           = useRef(null);   // WebSocket connection ref
  const audioProcessorRef = useRef(null); // Audio processor for WebSocket streaming

  // shared callbacks passed to blobToWavAndSend
  const appendText  = (text) => setTranscription((p) => p + (p ? " " : "") + text);
  const showError   = (msg)  => { setError(msg); setTimeout(() => setError(""), 3000); };

  // ── WebSocket Management ───────────────────────────────────────────────────────
  useEffect(() => {
    const connectWebSocket = () => {
      try {
        const ws = new WebSocket(WS_URL);

        ws.onopen = () => {
          console.log("WebSocket connected");
          setWsStatus("connected");
        };

        ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data);
            if (message.type === "transcript") {
              // Add interim or final transcript
              if (message.text) {
                appendText(message.text);
              }
            } else if (message.type === "status" || message.type === "warning") {
              console.log("Server message:", message.text);
            } else if (message.type === "error") {
              console.error("Server error:", message.text);
              showError(message.text);
            }
          } catch (err) {
            console.error("Error parsing WebSocket message:", err);
          }
        };

        ws.onerror = (err) => {
          console.error("WebSocket error:", err);
          setWsStatus("error");
          showError("WebSocket connection error");
        };

        ws.onclose = () => {
          console.log("WebSocket disconnected");
          setWsStatus("disconnected");
        };

        wsRef.current = ws;
      } catch (err) {
        console.error("Failed to create WebSocket:", err);
        setWsStatus("error");
      }
    };

    // Connect WebSocket on component mount
    connectWebSocket();

    // Cleanup on unmount
    return () => {
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        wsRef.current.close();
      }
    };
  }, []);

  // Send audio chunk via WebSocket
  const sendAudioChunkViaWS = (audioData) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      try {
        const message = JSON.stringify({
          type: "audio",
          data: audioData
        });
        wsRef.current.send(message);
      } catch (err) {
        console.error("Failed to send audio via WebSocket:", err);
      }
    }
  };

  // Process and stream audio from MediaRecorder via WebSocket
  const startWebSocketStreaming = (stream, onChunkSent) => {
    try {
      const audioContext = new AudioContext();
      const mediaStreamSource = audioContext.createMediaStreamSource(stream);
      const scriptProcessor = audioContext.createScriptProcessor(AUDIO_CHUNK_SIZE, 1, 1);

      mediaStreamSource.connect(scriptProcessor);
      scriptProcessor.connect(audioContext.destination);

      scriptProcessor.onaudioprocess = (event) => {
        const audioData = event.inputBuffer.getChannelData(0);
        const base64Audio = encodeAudioBase64(audioData, audioContext.sampleRate);
        sendAudioChunkViaWS(base64Audio);
        if (onChunkSent) onChunkSent();
      };

      audioProcessorRef.current = { scriptProcessor, audioContext, mediaStreamSource };
    } catch (err) {
      console.warn("WebSocket audio streaming not available, will use fallback:", err);
    }
  };

  const stopWebSocketStreaming = () => {
    if (audioProcessorRef.current) {
      const { scriptProcessor, audioContext } = audioProcessorRef.current;
      scriptProcessor.disconnect();
      audioContext.close();
      audioProcessorRef.current = null;
    }
  };

  // ── Push-to-Talk ───────────────────────────────────────────────────────────
  const onPTTStart = async (e) => {
    e.preventDefault();
    if (recorderRef.current) return;
    setError("");
    try {
      const stream   = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      // Try to stream via WebSocket if connected
      if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
        startWebSocketStreaming(stream);
        setStatus("Recording (streaming)…");
      } else {
        setStatus("Recording (batch)…");
      }

      const recorder = new MediaRecorder(stream);
      const chunks   = [];
      recorder.ondataavailable = (ev) => { if (ev.data.size > 0) chunks.push(ev.data); };
      recorder.onstop = () => {
        stopWebSocketStreaming();
        const blob = new Blob(chunks, { type: recorder.mimeType || "audio/webm" });
        // Use batch fallback if WebSocket streaming wasn't used
        if (!audioProcessorRef.current) {
          blobToWavAndSend(blob, setStatus, appendText, showError);
        }
        streamRef.current?.getTracks().forEach((t) => t.stop());
        streamRef.current  = null;
        recorderRef.current = null;
        setIsRecording(false);
      };
      recorder.start();
      recorderRef.current = recorder;
      setIsRecording(true);
    } catch (err) {
      console.error("Mic error:", err);
      setError("Microphone access denied. Please allow access and try again.");
    }
  };

  const onPTTEnd = (e) => {
    e.preventDefault();
    if (recorderRef.current?.state === "recording") {
      recorderRef.current.stop();
    }
  };

  // ── Keep Listening helpers ─────────────────────────────────────────────────
  // Start a fresh MediaRecorder on the existing stream.
  const startNewRecorder = (stream) => {
    if (!activeRef.current || !stream.active) return;
    const recorder = new MediaRecorder(stream);
    const chunks   = [];
    recorder.ondataavailable = (ev) => { if (ev.data.size > 0) chunks.push(ev.data); };
    recorder.onstop = () => {
      const blob = new Blob(chunks, { type: recorder.mimeType || "audio/webm" });
      blobToWavAndSend(blob, setStatus, appendText, showError); // transcribe in bg
      if (activeRef.current) startNewRecorder(stream);          // immediately re-arm
    };
    recorder.start();
    recorderRef.current = recorder;
  };

  const toggleAlways = async () => {
    if (isRecording) {
      // ── Stop ──
      activeRef.current = false;
      clearInterval(silenceTimerRef.current);
      silenceTimerRef.current = null;
      if (recorderRef.current?.state === "recording") recorderRef.current.stop();
      recorderRef.current = null;
      audioCtxRef.current?.close();
      audioCtxRef.current = null;
      stopWebSocketStreaming();
      streamRef.current?.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
      setIsRecording(false);
      setStatus("Stopped.");
    } else {
      // ── Start ──
      setError("");
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        streamRef.current = stream;
        activeRef.current = true;

        // Try WebSocket streaming if connected
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          startWebSocketStreaming(stream);
          setStatus("Listening (real-time)…");
        } else {
          setStatus("Listening (batch)…");
        }

        // AnalyserNode for silence detection (no ScriptProcessorNode)
        const ctx      = new AudioContext();
        audioCtxRef.current = ctx;
        const source   = ctx.createMediaStreamSource(stream);
        const analyser = ctx.createAnalyser();
        analyser.fftSize = 2048;
        source.connect(analyser);
        silenceStartRef.current = null;

        startNewRecorder(stream);

        const timeDomain = new Uint8Array(analyser.frequencyBinCount);
        silenceTimerRef.current = setInterval(() => {
          if (!activeRef.current) return;
          analyser.getByteTimeDomainData(timeDomain);
          let sum = 0;
          for (let i = 0; i < timeDomain.length; i++) {
            const norm = (timeDomain[i] - 128) / 128;
            sum += norm * norm;
          }
          const rms = Math.sqrt(sum / timeDomain.length);

          if (rms < SILENCE_THRESHOLD) {
            if (!silenceStartRef.current) silenceStartRef.current = Date.now();
            else if (Date.now() - silenceStartRef.current >= SILENCE_DURATION_MS) {
              silenceStartRef.current = null;
              // Stop recorder → onstop will decode + re-arm automatically
              if (recorderRef.current?.state === "recording") {
                recorderRef.current.stop();
              }
            }
          } else {
            silenceStartRef.current = null;
          }
        }, 100);

        setIsRecording(true);
      } catch (err) {
        console.error("Mic error:", err);
        setError("Microphone access denied. Please allow access and try again.");
      }
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <div className="container">
      <h1>Real-Time Listener</h1>

      {/* WebSocket status indicator */}
      <div className="ws-status" style={{
        padding: "8px 12px",
        marginBottom: "12px",
        borderRadius: "4px",
        fontSize: "12px",
        backgroundColor: wsStatus === "connected" ? "#d4edda" : wsStatus === "error" ? "#f8d7da" : "#e2e3e5",
        color: wsStatus === "connected" ? "#155724" : wsStatus === "error" ? "#721c24" : "#383d41"
      }}>
        WebSocket: <strong>{wsStatus}</strong> {wsStatus === "connected" && "✓"}
      </div>

      {/* Mode toggle pill */}
      <div className="mode-toggle">
        <button
          className={`toggle-option${mode === "ptt" ? " selected" : ""}`}
          onClick={() => { if (!isRecording) setMode("ptt"); }}
          disabled={isRecording}
        >
          🖱️ Push to Talk
        </button>
        <button
          className={`toggle-option${mode === "always" ? " selected" : ""}`}
          onClick={() => { if (!isRecording) setMode("always"); }}
          disabled={isRecording}
        >
          🔁 Keep Listening
        </button>
      </div>

      {/* Action button */}
      {mode === "ptt" ? (
        <button
          className={`upload-button ptt-btn${isRecording ? " ptt-active" : ""}`}
          onMouseDown={onPTTStart}
          onMouseUp={onPTTEnd}
          onMouseLeave={isRecording ? onPTTEnd : undefined}
          onTouchStart={onPTTStart}
          onTouchEnd={onPTTEnd}
        >
          {isRecording ? "🔴 Recording…" : "🎙️ Hold to Talk"}
        </button>
      ) : (
        <button
          className={`upload-button${isRecording ? " stop-btn" : ""}`}
          onClick={toggleAlways}
        >
          {isRecording ? "⏹️ Stop Listening" : "🎙️ Start Listening"}
        </button>
      )}

      {/* Pulsing indicator (Keep Listening only) */}
      {isRecording && mode === "always" && (
        <div className="recording-indicator">
          <span className="pulse-dot" />
          <span>Listening… (auto-submits after 3 s silence)</span>
        </div>
      )}

      {status && <p className="status-text">{status}</p>}
      {error  && <p className="error-text">{error}</p>}

      <div className="transcription-result">
        <h2>Transcription</h2>
        <p>
          {transcription || (
            <span className="placeholder-text">Your speech will appear here…</span>
          )}
        </p>
      </div>

      {transcription && (
        <button
          className="upload-button clear-btn"
          onClick={() => { setTranscription(""); setStatus(""); }}
        >
          Clear
        </button>
      )}
    </div>
  );
};

export default RealTimeListener;
