# DocTalk RAG Backend

A complete Retrieval-Augmented Generation (RAG) backend for document-based conversational AI using FastAPI, Pinecone, and Groq.

## Features

- **Document Processing**: PDF and TXT file processing with text extraction
- **Vector Storage**: Pinecone integration for efficient document embeddings
- **RAG System**: Context-aware responses using retrieved document chunks
- **Groq Integration**: Fast LLM responses with multiple model options
- **Real-time Chat**: Firebase integration for chat history
- **Scalable Architecture**: Async processing with Celery
- **Docker Support**: Easy deployment with Docker Compose

## Tech Stack

- **Backend**: FastAPI (Python)
- **Vector Database**: Pinecone
- **LLM**: Groq (Llama 3, Mixtral, Gemma)
- **Embeddings**: OpenAI/Sentence Transformers
- **Document Processing**: PyPDF2, NLTK, spaCy
- **Database**: Firebase (Firestore)
- **Cache**: Redis
- **Task Queue**: Celery
- **Containerization**: Docker

## Quick Start

### Prerequisites

1. **Python 3.11+**
2. **Docker & Docker Compose**
3. **API Keys**:
   - Groq API Key
   - OpenAI API Key (for embeddings)
   - Pinecone API Key
   - Firebase Service Account Key

### Installation

1. **Clone the repository**:
```bash
git clone <repository-url>
cd doctalk/backend
```

2. **Set up environment variables**:
```bash
cp .env.example .env
# Edit .env with your API keys
```

3. **Install dependencies**:
```bash
pip install -r requirements.txt
```

4. **Download language models**:
```bash
python -m spacy download en_core_web_sm
python -c "import nltk; nltk.download('punkt'); nltk.download('stopwords')"
```

### Running with Docker

1. **Build and run**:
```bash
docker-compose up --build
```

2. **Access the API**:
   - API: http://localhost:8000
   - Health Check: http://localhost:8000/health
   - API Docs: http://localhost:8000/docs
   - Flower (Celery Monitor): http://localhost:5555

### Running Locally

1. **Start Redis**:
```bash
redis-server
```

2. **Start the backend**:
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## API Endpoints

### Document Processing

- `POST /api/v1/documents/process` - Process and embed a document
- `GET /api/v1/documents/{document_id}/status` - Get processing status
- `DELETE /api/v1/documents/{document_id}/delete` - Delete document

### Chat & RAG

- `POST /api/v1/chat/query` - Chat with a document
- `POST /api/v1/documents/{document_id}/search` - Search in document
- `GET /api/v1/documents/{document_id}/summary` - Get document summary

### Health & Monitoring

- `GET /health` - Basic health check
- `GET /health/rag` - RAG service health check
- `GET /docs` - Interactive API documentation

## Configuration

### Environment Variables

```bash
# Application
ENVIRONMENT=development
DEBUG=True
SECRET_KEY=your-secret-key

# Groq API
GROQ_API_KEY=gsk_your-groq-api-key
DEFAULT_GROQ_MODEL=llama3-8b-8192

# OpenAI (Embeddings)
OPENAI_API_KEY=your-openai-api-key
EMBEDDING_MODEL=text-embedding-3-small

# Pinecone
PINECONE_API_KEY=your-pinecone-api-key
PINECONE_INDEX_NAME=doctalk-documents

# Firebase
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="your-private-key"

# Redis
REDIS_URL=redis://localhost:6379/0
```

### RAG Configuration

- `CHUNK_SIZE=1000` - Text chunk size
- `CHUNK_OVERLAP=200` - Overlap between chunks
- `MAX_CONTEXT_CHUNKS=5` - Max context chunks for RAG
- `SIMILARITY_THRESHOLD=0.7` - Minimum similarity for retrieval

## Architecture

### Document Processing Pipeline

1. **Upload**: Document uploaded via Firebase Storage
2. **Extract**: Text extraction from PDF/TXT
3. **Chunk**: Split text into overlapping chunks
4. **Embed**: Create vector embeddings
5. **Store**: Store in Pinecone vector database
6. **Index**: Update Firestore with metadata

### RAG Query Pipeline

1. **Query**: User asks question
2. **Embed**: Create query embedding
3. **Retrieve**: Find similar document chunks
4. **Context**: Format retrieved chunks
5. **Generate**: Send to Groq with context
6. **Response**: Return contextual answer

## Usage Examples

### Process a Document

```python
import requests

# Process document
response = requests.post("http://localhost:8000/api/v1/documents/process", 
    json={
        "document_id": "doc123",
        "user_id": "user456",
        "file_name": "document.pdf",
        "file_type": "pdf",
        "download_url": "https://storage.googleapis.com/..."
    },
    headers={"Authorization": "Bearer your-token"}
)
```

### Chat with Document

```python
# Chat query
response = requests.post("http://localhost:8000/api/v1/chat/query",
    json={
        "query": "What are the main topics in this document?",
        "document_id": "doc123",
        "user_id": "user456",
        "model": "llama3-8b-8192"
    },
    headers={"Authorization": "Bearer your-token"}
)

result = response.json()
print(result["answer"])
```

## Development

### Project Structure

```
backend/
|-- app/
|   |-- api/v1/          # API routes
|   |-- core/            # Core configuration
|   |-- models/          # Pydantic models
|   |-- services/        # Business logic
|   |-- utils/           # Utilities
|-- main.py              # Application entry
|-- requirements.txt     # Dependencies
|-- Dockerfile          # Container config
|-- docker-compose.yml   # Multi-container setup
```

### Adding New Features

1. **Models**: Add to `app/models/schemas.py`
2. **Services**: Add to `app/services/`
3. **API**: Add to `app/api/v1/api.py`
4. **Tests**: Add to `tests/`

### Testing

```bash
# Run tests
pytest tests/

# Run with coverage
pytest tests/ --cov=app

# Run specific test
pytest tests/test_rag.py
```

## Deployment

### Production Deployment

1. **Environment Setup**:
```bash
export ENVIRONMENT=production
export DEBUG=False
```

2. **Docker Deployment**:
```bash
docker-compose -f docker-compose.yml up -d
```

3. **Monitoring**:
   - Check logs: `docker-compose logs -f backend`
   - Monitor tasks: http://localhost:5555 (Flower)
   - Health checks: http://localhost:8000/health

### Scaling

- **Horizontal Scaling**: Add more backend instances
- **Vector Store**: Use Pinecone's serverless scaling
- **Cache**: Scale Redis cluster
- **Load Balancer**: Add nginx or cloud load balancer

## Monitoring & Logging

### Logging

Logs are stored in `logs/doctalk.log` with structured JSON format.

### Health Checks

- `/health` - Basic service health
- `/health/rag` - RAG-specific health
- Docker health checks

### Metrics

- Response times
- Document processing stats
- Vector store usage
- API request counts

## Troubleshooting

### Common Issues

1. **Pinecone Connection**:
   - Check API key
   - Verify index exists
   - Check dimension compatibility

2. **Document Processing**:
   - Verify file accessibility
   - Check file format support
   - Monitor memory usage

3. **Groq API**:
   - Check API key validity
   - Monitor rate limits
   - Verify model availability

### Debug Mode

```bash
export DEBUG=True
export LOG_LEVEL=DEBUG
uvicorn main:app --reload
```

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests
4. Submit pull request

## License

MIT License - see LICENSE file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Check the API documentation at `/docs`
- Review the troubleshooting guide
