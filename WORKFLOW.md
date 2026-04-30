# Spring AI Audio Transcription - Development Workflow

## 🎯 Workflow Overview

This document outlines the complete development, build, test, and deployment workflow for the Spring AI Audio Transcription project.

---

## 📋 Quick Start Workflow

### Phase 1: Environment Setup
```
1. Install Prerequisites
   └─ Java 21 JDK
   └─ Node.js 16+
   └─ Maven 3.6+
   └─ OpenAI API Key

2. Clone & Navigate
   cd audio-transcribe          # Backend
   cd Transcribe-AI             # Frontend

3. Set Environment Variables
   set OPENAI_API_KEY=sk-xxxxx  # Windows
   # OR
   export OPENAI_API_KEY=sk-xxxxx  # Linux/Mac
```

---

## 🔨 Build Workflow

### Backend Build (Spring Boot)
```
Step 1: Build Backend
  cd audio-transcribe
  mvn clean install
  └─ Downloads dependencies
  └─ Compiles Java code
  └─ Creates JAR file: target/audio-transcribe-0.0.1-SNAPSHOT.jar

Step 2: Run Backend
  mvn spring-boot:run
  └─ Starts on http://localhost:8080
  └─ Waits for frontend requests
  └─ Ready to accept audio uploads
```

### Frontend Build (React + Vite)
```
Step 1: Install Dependencies
  cd Transcribe-AI
  npm install
  └─ Downloads React, Vite, Axios, etc.
  └─ Creates node_modules/

Step 2: Development Server
  npm run dev
  └─ Starts on http://localhost:5173
  └─ Hot module reloading enabled
  └─ Console shows port if 5173 is in use

Step 3: Production Build (when ready)
  npm run build
  └─ Bundles and minifies code
  └─ Creates dist/ folder
  └─ Ready for deployment
```

---

## 🧪 Testing Workflow

### Backend Testing
```
Run Unit Tests
  mvn test
  └─ Runs AudioTranscribeApplicationTests.java
  └─ Generates test reports in target/surefire-reports/

Verify Build
  mvn verify
  └─ Runs tests and checks code quality
```

### Frontend Testing
```
Run Linter
  npm run lint
  └─ Checks code style with ESLint
  └─ Reports any issues

Manual Testing
  1. Upload an audio file via UI
  2. Click "Upload and Transcribe"
  3. Verify transcribed text appears
  4. Test voice commands (prefix with "sk")
     Example: "sk open notepad"
```

---

## 🔄 Development Workflow (Local)

### Step 1: Start Backend
```bash
cd audio-transcribe
set OPENAI_API_KEY=your-key-here
mvn spring-boot:run
# Waits on http://localhost:8080
```

### Step 2: Start Frontend (in new terminal)
```bash
cd Transcribe-AI
npm run dev
# Opens http://localhost:5173
```

### Step 3: Development Loop
```
1. Make code changes (Java or React)
2. Backend: Restart Spring Boot (if Java changes)
3. Frontend: Auto-reloads (Vite HMR)
4. Test in browser at http://localhost:5173
5. Upload test audio file
6. Verify transcription works
7. Commit changes when satisfied
```

### Step 4: Code Organization
```
Backend Code Changes
└─ src/main/java/com/audio/transcribe/
   ├─ AudioTranscribeApplication.java (main app)
   ├─ TranscriptionController.java (API endpoints)
   ├─ CommandHandler.java (voice commands)
   └─ WebConfig.java (CORS configuration)

Frontend Code Changes
└─ Transcribe-AI/src/
   ├─ App.jsx (main component)
   ├─ AudioUploader.jsx (file upload component)
   ├─ App.css (styling)
   └─ main.jsx (entry point)
```

---

## 🐳 Docker Workflow (Containerization)

### Build Docker Image
```bash
# From root project directory
docker build -t audio-transcribe:latest .
# Uses Dockerfile in audio-transcribe/
# Creates containerized Java application
```

### Run Docker Container
```bash
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-key-here \
  audio-transcribe:latest
# Exposes backend on http://localhost:8080
```

### Docker Compose (Backend + Frontend)
```bash
docker-compose up -d
# Runs both services in containers
# Frontend: http://localhost:3000 (or configured port)
# Backend: http://localhost:8080
```

---

## 🚀 Deployment Workflow

### Option 1: Standalone Deployment
```
1. Build Backend
   cd audio-transcribe
   mvn clean package
   
2. Build Frontend
   cd Transcribe-AI
   npm run build
   
3. Deploy Backend
   java -jar target/audio-transcribe-0.0.1-SNAPSHOT.jar
   
4. Deploy Frontend (serve dist/ folder)
   Use Nginx, Apache, or Node server
   Point to dist/ as root
```

### Option 2: Cloud Deployment (Azure)
```
1. Create Azure App Service
   Resource Group → App Services → Create

2. Deploy Backend
   mvn azure-webapp:deploy

3. Deploy Frontend
   Upload dist/ folder to App Service

4. Configure Environment Variables
   OPENAI_API_KEY in Azure Portal
   PORT configuration
```

### Option 3: Docker Deployment
```
1. Build Image
   docker build -t audio-transcribe:v1.0 .

2. Push to Registry
   docker tag audio-transcribe:v1.0 myregistry.azurecr.io/audio-transcribe:v1.0
   docker push myregistry.azurecr.io/audio-transcribe:v1.0

3. Deploy to AKS (Kubernetes)
   kubectl apply -f deployment.yaml
```

---

## 📊 API Testing Workflow

### Test with cURL (Backend)
```bash
# 1. Create test audio file or use existing
# 2. Test transcription endpoint
curl -X POST \
  -F "file=@test-audio.mp3" \
  http://localhost:8080/api/transcribe

# Expected Response:
# "sk open youtube" or similar transcribed text
```

### Test with Postman
```
1. Create POST request
   URL: http://localhost:8080/api/transcribe
   
2. Set Headers
   Content-Type: multipart/form-data
   
3. Set Body
   file: [select audio file]
   
4. Send and verify response
```

### Test Frontend UI
```
1. Open http://localhost:5173
2. Click file input and select audio file
3. Click "Upload and Transcribe" button
4. Verify transcription appears below
5. Open browser console (F12) for error logs
```

---

## 🔍 Debugging Workflow

### Backend Debugging
```
Enable Debug Mode
  mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"

Connect IDE Debugger
  Set breakpoints in Java code
  IDE connects to localhost:5005
  Step through code execution
```

### Frontend Debugging
```
Browser DevTools (F12)
  1. Console tab → Check for errors
  2. Network tab → Inspect API calls
  3. React tab → Component hierarchy
  4. Sources tab → Set breakpoints

VS Code Debugger
  1. Install "Debugger for Firefox/Chrome" extension
  2. Set breakpoints in .jsx files
  3. F5 to start debugging
  4. Step through code
```

### Logs
```
Backend Logs
  Check console output from mvn spring-boot:run
  CommandHandler logs: "Command 'X' detected"
  Transcription logs: API calls to OpenAI

Frontend Logs
  Browser console.log() output
  Check NetworkTab for HTTP requests/responses
```

---

## ✅ Pre-Deployment Checklist

```
Backend
  [ ] Set OPENAI_API_KEY environment variable
  [ ] Run mvn clean install successfully
  [ ] Run mvn test (all tests pass)
  [ ] Test API endpoint with sample audio
  [ ] Verify CORS headers are correct
  [ ] Check CommandHandler commands work
  [ ] Build JAR file: mvn package

Frontend
  [ ] Run npm install
  [ ] Run npm run lint (no errors)
  [ ] Test UI locally with backend running
  [ ] Test file upload functionality
  [ ] Run npm run build
  [ ] Verify dist/ folder created
  [ ] Test build output (npm run preview)

General
  [ ] API key is secure (not in code)
  [ ] Port conflicts resolved (8080, 5173)
  [ ] CORS URLs match deployment environment
  [ ] Dependencies updated (maven, npm)
  [ ] Documentation updated
  [ ] README reflects current setup
```

---

## 🔗 Quick Reference Commands

### Maven Commands (Backend)
```bash
mvn clean              # Clean build artifacts
mvn compile            # Compile source code
mvn test               # Run unit tests
mvn package            # Create JAR file
mvn install            # Install to local repository
mvn spring-boot:run    # Run application
mvn clean install      # Clean + Install (full build)
```

### NPM Commands (Frontend)
```bash
npm install            # Install dependencies
npm run dev            # Start dev server
npm run build          # Build for production
npm run preview        # Preview production build
npm run lint           # Check code style
npm update             # Update packages
```

### Docker Commands
```bash
docker build -t name:tag .     # Build image
docker run -p 8080:8080 name   # Run container
docker logs container-id       # View logs
docker stop container-id       # Stop container
docker-compose up              # Start services
docker-compose down            # Stop services
```

---

## 📈 CI/CD Pipeline (GitHub Actions Example)

```yaml
name: Build and Deploy

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Setup Java
        uses: actions/setup-java@v2
        with:
          java-version: 21
      
      - name: Build Backend
        run: |
          cd audio-transcribe
          mvn clean package
      
      - name: Setup Node
        uses: actions/setup-node@v2
        with:
          node-version: 16
      
      - name: Build Frontend
        run: |
          cd Transcribe-AI
          npm install
          npm run build
      
      - name: Deploy
        run: |
          # Deploy to cloud provider
          echo "Deploying..."
```

---

## 🎓 Development Best Practices

1. **Version Control**
   - Commit frequently with clear messages
   - Create branches for features
   - Use pull requests for code review

2. **Code Quality**
   - Run linter before commits
   - Write meaningful variable names
   - Add comments for complex logic
   - Follow naming conventions

3. **Security**
   - Never commit API keys
   - Use environment variables
   - Keep dependencies updated
   - Validate user inputs

4. **Performance**
   - Optimize API calls
   - Minimize bundle size
   - Cache transcriptions if needed
   - Monitor error rates

5. **Testing**
   - Write unit tests
   - Test edge cases
   - Test with various audio formats
   - Manual user testing

---

## 📞 Troubleshooting

### Backend Won't Start
```
Problem: Port 8080 already in use
Solution: 
  - Change PORT environment variable
  - OR kill process using port 8080
  - lsof -i :8080 && kill -9 <PID>
```

### API Key Error
```
Problem: "OPENAI_API_KEY not set"
Solution:
  set OPENAI_API_KEY=your-actual-key
  Verify with: echo %OPENAI_API_KEY% (Windows)
```

### CORS Error in Console
```
Problem: "Access to XMLHttpRequest blocked by CORS policy"
Solution:
  - Check WebConfig.java has @CrossOrigin
  - Verify frontend URL in CORS configuration
  - Check backend is running on port 8080
```

### Frontend Build Fails
```
Problem: npm run build errors
Solution:
  npm clean-install  # Clean node_modules
  npm install
  npm run build
```

---

## 📝 Workflow Summary

```
Local Development
  └─ Start Backend (mvn spring-boot:run)
  └─ Start Frontend (npm run dev)
  └─ Make changes
  └─ Test in browser
  └─ Commit to git

Testing
  └─ Run backend tests (mvn test)
  └─ Run linter (npm run lint)
  └─ Manual UI testing
  └─ Integration testing

Production Build
  └─ Backend: mvn clean package
  └─ Frontend: npm run build

Deployment
  └─ Docker: Build image, push to registry
  └─ Cloud: Deploy to Azure/AWS/GCP
  └─ Verify endpoints working
  └─ Monitor logs and errors
```

---

This workflow ensures smooth development, testing, and deployment of your audio transcription application!
