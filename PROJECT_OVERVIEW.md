# Spring AI Audio Transcription Project - Overview & Workflow

## 📋 Project Architecture

This is a **full-stack audio transcription application** with a React frontend and Spring Boot backend powered by OpenAI's Whisper API.

```
┌─────────────────────────────────────────────────────────┐
│                   FRONTEND (React + Vite)               │
│  - AudioUploader.jsx: File upload & transcription UI    │
│  - Communicates with backend via axios HTTP             │
│  - Runs on port 5173 (dev) / built as static files      │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ HTTP POST /api/transcribe
                     ▼
┌─────────────────────────────────────────────────────────┐
│              BACKEND (Spring Boot + Spring AI)          │
│  - Java 21 + Spring Boot 3.5.7                          │
│  - Spring AI 1.0.3 (OpenAI integration)                 │
│  - REST API: POST /api/transcribe                       │
│  - Command Handler: Voice command execution             │
│  - Runs on port 8080                                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     │ API Calls
                     ▼
         ┌───────────────────────┐
         │  OpenAI API (Whisper) │
         │  Audio → Text         │
         └───────────────────────┘
```

---

## 🔍 How Things Work

### **Frontend Flow (React + Vite)**

1. **AudioUploader Component** (`src/AudioUploader.jsx`)
   - User selects an audio file via file input
   - File stored in component state
   - User clicks "Upload and Transcribe" button

2. **File Upload**
   - FormData created with the audio file
   - Axios sends `POST` request to `http://localhost:8080/api/transcribe`
   - Backend receives multipart form data

3. **Display Result**
   - Response contains transcribed text
   - Text displayed in the UI under "Transcription Result"

### **Backend Flow (Spring Boot)**

1. **Request Reception** (`TranscriptionController.java`)
   ```
   POST /api/transcribe (with audio file)
     ↓
   Receives MultipartFile
   ```

2. **Transcription Processing**
   - `OpenAiAudioTranscriptionModel` converts file to Resource
   - Sends to OpenAI Whisper API
   - Returns transcribed text

3. **Command Handling** (`CommandHandler.java`)
   - Checks if transcription starts with wake word: **"sk"**
   - If yes, parses and executes commands:
     - `sk open notepad` → Opens Notepad
     - `sk search youtube programming` → Searches YouTube
     - `sk open google` → Opens Google
     - Supports: calculator, VS Code, Chrome, Spotify, WhatsApp, File Explorer
   - Logs execution results

4. **Response**
   - Transcribed text sent back to frontend
   - Frontend displays it in the UI

---

## 🛠️ Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 21
- **AI Library**: Spring AI 1.0.3
- **Build Tool**: Maven
- **API**: OpenAI Whisper (via Spring AI)

### Frontend
- **Framework**: React 19.1.1
- **Build Tool**: Vite 7.1.7
- **HTTP Client**: Axios 1.13.2
- **Language**: JavaScript (ES Module)

### Infrastructure
- Backend: Port 8080
- Frontend: Port 5173 (dev) / static build
- CORS enabled between localhost:5173 ↔ localhost:8080

---

## 📝 Configuration

### Backend (`application.properties`)
```properties
spring.application.name=audio-transcribe
spring.ai.openai.api-key=${OPENAI_API_KEY}  # Environment variable required
server.port=${PORT:8080}                     # Default port 8080
```

### Frontend (Vite Config)
- Dev server: `localhost:5173`
- Build output: `dist/`
- HMR enabled for hot module replacement

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK
- Node.js 16+
- OpenAI API Key (set as environment variable: `OPENAI_API_KEY`)

### Backend Setup
```bash
cd audio-transcribe
mvn clean install        # Download dependencies & build
mvn spring-boot:run     # Start on port 8080
```

### Frontend Setup
```bash
cd Transcribe-AI
npm install             # Install dependencies
npm run dev             # Start dev server on port 5173
```

---

## 📊 Data Flow Example

```
User Action: Upload audio file "hello.mp3"
    ↓
Frontend: File selected → handleFileChange()
    ↓
Frontend: User clicks "Upload and Transcribe"
    ↓
Frontend: FormData created, axios.post() to /api/transcribe
    ↓
Backend: TranscriptionController receives file
    ↓
Backend: OpenAI Whisper transcribes: "sk open youtube"
    ↓
Backend: CommandHandler detects wake word "sk"
    ↓
Backend: Parses command "open youtube"
    ↓
Backend: Executes command (opens URL)
    ↓
Backend: Returns transcribed text to frontend
    ↓
Frontend: Displays: "sk open youtube"
    ↓
User sees transcription + YouTube opens on desktop
```

---

## 🔗 API Endpoints

### POST /api/transcribe
- **Method**: POST with multipart form data
- **Parameter**: `file` (audio file)
- **Response**: Transcribed text (string)
- **Headers**: Requires Content-Type: multipart/form-data
- **CORS**: Allowed from http://localhost:5173

---

## 📦 Project Structure

```
.
├── audio-transcribe/               # Spring Boot Backend
│   ├── pom.xml                    # Maven dependencies & build config
│   ├── src/main/java/.../         # Java source code
│   │   ├── AudioTranscribeApplication.java    # Main entry point
│   │   ├── TranscriptionController.java       # REST API
│   │   └── CommandHandler.java                # Voice command executor
│   ├── src/main/resources/
│   │   └── application.properties  # Configuration
│   └── Dockerfile                 # Docker containerization
│
├── Transcribe-AI/                 # React Frontend
│   ├── package.json               # NPM dependencies
│   ├── vite.config.js            # Vite build config
│   ├── src/
│   │   ├── App.jsx               # Main React component
│   │   ├── AudioUploader.jsx      # File upload component
│   │   ├── App.css               # Styles
│   │   └── main.jsx              # Entry point
│   └── public/                   # Static assets
```

---

## ⚙️ Environment Variables

### Required
- `OPENAI_API_KEY` - Your OpenAI API key for Whisper transcription

### Optional
- `PORT` - Backend port (default: 8080)

---

## 🐛 Key Features

✅ Audio file transcription via OpenAI Whisper  
✅ Voice command execution ("sk" wake word)  
✅ Cross-platform command support (Windows)  
✅ Real-time file upload and processing  
✅ CORS enabled for frontend-backend communication  
✅ Responsive React UI with Vite  

---

## 🔄 Next Steps for Development

1. Add error handling improvements in frontend
2. Implement loading states during transcription
3. Add support for multiple audio formats
4. Create database for command history
5. Implement user authentication
6. Add Docker deployment
7. Deploy to cloud (Azure/AWS)
