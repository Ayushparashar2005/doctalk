# DocTalk Backend

DocTalk is a FastAPI backend for document chat that uses Groq for generation and local SQLite for persistence.

## Features

- Document processing for PDF and TXT files
- Local SQLite storage for documents, chunks, and chat history
- Groq-backed chat responses
- Sentence-transformer embeddings for local retrieval

## Tech Stack

- FastAPI
- SQLite
- Groq-compatible OpenAI client
- Sentence Transformers
- PyPDF2, pypdf, NLTK, spaCy

## Quick Start

1. Copy `.env.example` to `.env`.
2. Add your Groq API key.
3. Install dependencies with `pip install -r requirements.txt`.
4. Run `uvicorn main:app --reload --host 0.0.0.0 --port 8000`.

## Environment

```bash
ENVIRONMENT=development
DEBUG=True
GROQ_API_KEY=gsk_your-groq-api-key-here
GROQ_BASE_URL=https://api.groq.com/openai/v1
DEFAULT_GROQ_MODEL=llama3-8b-8192
SQLITE_DB_PATH=data/doctalk.sqlite3
```

## API

- `POST /api/v1/documents/process`
- `POST /api/v1/chat/query`
- `POST /api/v1/documents/{document_id}/search`
- `GET /api/v1/documents/{document_id}/summary`
- `GET /api/v1/documents/{document_id}/status`
- `DELETE /api/v1/documents/{document_id}/delete`
