# DocTalk - Complete Run Guide

## 🚀 How to Run DocTalk Application

### Prerequisites
- **Android Studio** (latest version)
- **Java 17+**
- **Docker** (for backend)
- **Python 3.11+** (for backend development)

---

## 📱 Part 1: Run Android App

### Option A: Android Studio (Recommended)

1. **Open Project in Android Studio**:
   ```bash
   # Clone or open the project
   File → Open → Navigate to d:/doctalk/app
   ```

2. **Sync Gradle**:
   - Click "Sync Now" in Android Studio
   - Wait for dependencies to download

3. **Connect Android Device**:
   - Enable USB Debugging on device
   - OR create Android Emulator (API 30+)

4. **Run Application**:
   - Click the green "Run" button (▶️)
   - Select your device/emulator
   - App will install and launch

### Option B: Command Line

1. **Navigate to App Directory**:
   ```bash
   cd d:/doctalk/app
   ```

2. **Build and Install**:
   ```bash
   # Build APK
   ./gradlew assembleDebug
   
   # Install on connected device
   ./gradlew installDebug
   ```

3. **Launch App**:
   ```bash
   adb shell am start -n com.doctalk.app/.presentation.MainActivity
   ```

---

## 🖥️ Part 2: Run RAG Backend

### Option A: Docker (Recommended)

1. **Navigate to Backend Directory**:
   ```bash
   cd d:/doctalk/backend
   ```

2. **Set Environment Variables**:
   ```bash
   # Copy the template
   cp .env.example .env
   
   # Edit .env with your API keys
   notepad .env
   ```

3. **Run with Docker Compose**:
   ```bash
   # Build and start all services
   docker-compose up --build
   
   # Run in background
   docker-compose up -d
   ```

4. **Access Services**:
   - **API**: http://localhost:8000
   - **API Docs**: http://localhost:8000/docs
   - **Health Check**: http://localhost:8000/health
   - **Flower (Monitoring)**: http://localhost:5555

### Option B: Local Development

1. **Install Dependencies**:
   ```bash
   cd d:/doctalk/backend
   pip install -r requirements.txt
   ```

2. **Download Language Models**:
   ```bash
   python -m spacy download en_core_web_sm
   python -c "import nltk; nltk.download('punkt'); nltk.download('stopwords')"
   ```

3. **Start Redis** (if not using Docker):
   ```bash
   redis-server
   # OR
   docker run -d -p 6379:6379 redis:7-alpine
   ```

4. **Run Backend**:
   ```bash
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

---

## 🔗 Part 3: Connect Android App to Backend

### Update Android App Configuration

1. **Update Backend URL** in Android app:
   ```kotlin
   // File: d:/doctalk/app/src/main/java/com/doctalk/app/utils/Constants.kt
   const val BASE_URL = "http://YOUR_COMPUTER_IP:8000/api/"
   ```

2. **Find Your IP Address**:
   ```bash
   # Windows
   ipconfig
   
   # Mac/Linux
   ifconfig
   # OR
   hostname -I
   ```

3. **Rebuild Android App**:
   - Sync Gradle in Android Studio
   - Run the app again

---

## 🧪 Part 4: Test Complete Flow

### 1. **Backend Health Check**
```bash
curl http://localhost:8000/health
```

### 2. **Android App Test**
1. **Launch DocTalk App**
2. **Create Account** (Email/Password)
3. **Upload Document** (PDF or TXT)
4. **Wait for Processing**
5. **Start Chat** with the document
6. **Test Groq Responses**

### 3. **Verify Integration**
- ✅ Document uploads to Firebase Storage
- ✅ Backend processes document and creates embeddings
- ✅ Chat responses come from Groq
- ✅ Real-time updates in Firestore

---

## 🔧 Troubleshooting

### Android App Issues

**Build Errors**:
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

**Firebase Connection Issues**:
- Verify `google-services.json` is in `app/` directory
- Check Firebase project settings
- Ensure package name matches Firebase config

**Groq API Issues**:
- Verify API key in `Constants.kt`
- Check network connectivity
- Monitor API usage limits

### Backend Issues

**Docker Issues**:
```bash
# Check Docker logs
docker-compose logs backend

# Restart services
docker-compose down
docker-compose up --build
```

**Python Dependencies**:
```bash
# Reinstall dependencies
pip install -r requirements.txt --force-reinstall
```

**Pinecone Connection**:
- Verify API key in `.env`
- Check index name matches
- Ensure correct dimension (1536 for OpenAI embeddings)

### Integration Issues

**CORS Errors**:
```bash
# Check if Android device can reach backend
curl -H "Origin: android-app://com.doctalk.app" http://localhost:8000/health
```

**Network Issues**:
- Use your computer's IP, not localhost
- Ensure both devices are on same network
- Check firewall settings

---

## 📊 Monitoring

### Backend Monitoring
- **API Logs**: `docker-compose logs -f backend`
- **Health Status**: http://localhost:8000/health
- **Flower Dashboard**: http://localhost:5555

### Android Monitoring
- **Android Studio Logcat**
- **Firebase Console**: Analytics and Crashlytics
- **Network Inspector**: In Android Studio

---

## 🚀 Production Deployment

### Backend Deployment
1. **Update Environment**:
   ```bash
   export ENVIRONMENT=production
   export DEBUG=False
   ```

2. **Deploy to Cloud**:
   - **AWS**: AWS ECS or Lambda
   - **Google Cloud**: Cloud Run
   - **Azure**: Container Instances
   - **Vercel**: Serverless Functions

### Android App Deployment
1. **Generate Signed APK/AAB**:
   - Build → Generate Signed Bundle/APK
   - Upload to Google Play Console

### Environment Variables for Production
```bash
# Backend .env
ENVIRONMENT=production
DEBUG=False
GROQ_API_KEY=your-production-key
PINECONE_API_KEY=your-production-key
FIREBASE_PRIVATE_KEY="your-production-key"

# Android Constants.kt
const val BASE_URL = "https://your-production-backend.com/api/"
```

---

## 📱 Quick Start Checklist

### Before Running
- [ ] Android Studio installed
- [ ] Docker installed
- [ ] All API keys obtained
- [ ] Firebase project created
- [ ] Pinecone index created

### Running the App
- [ ] Backend started (`docker-compose up`)
- [ ] Backend health check passes
- [ ] Android app built and installed
- [ ] BASE_URL updated in Android app
- [ ] Test user can create account
- [ ] Test document upload works
- [ ] Test chat responses work

### Success Indicators
- ✅ Backend responds to health checks
- ✅ Android app launches without crashes
- ✅ User authentication works
- ✅ Documents upload successfully
- ✅ Chat responses are generated
- ✅ Real-time updates work

---

## 🆘 Support

### Common Issues & Solutions

**"Cannot resolve host"**:
- Use IP address instead of localhost
- Check network connectivity
- Verify backend is running

**"API key invalid"**:
- Verify Groq API key format
- Check Pinecone API key
- Ensure no extra spaces in keys

**"Document processing failed"**:
- Check file format (PDF/TXT only)
- Verify file size (< 10MB)
- Check backend logs for errors

**"Chat not working"**:
- Verify backend URL in Android app
- Check Firebase rules
- Ensure Groq API quota available

### Getting Help
1. **Check Logs**: Both Android and backend
2. **Verify Configuration**: API keys and URLs
3. **Test Components**: Isolate the issue
4. **Consult Documentation**: API docs and README files

---

## 🎯 Next Steps

Once running successfully:
1. **Test All Features**: Upload, chat, search
2. **Monitor Performance**: Response times, error rates
3. **Scale as Needed**: Add more backend instances
4. **Add Features**: Voice input, document sharing
5. **Deploy to Production**: Cloud hosting and app store

🎉 **Your DocTalk application is now ready to run!**
