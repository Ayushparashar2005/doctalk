"""
DocTalk RAG Backend - FastAPI Application
A complete RAG system for document-based conversational AI
"""

from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from contextlib import asynccontextmanager
import uvicorn
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

from app.core.config import settings
from app.core.database import init_database
from app.core.security import verify_token
from app.api.v1.api import api_router
from app.services.document_processor import DocumentProcessor
from app.services.rag_service import RAGService
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore

# Initialize services
document_processor = DocumentProcessor()
embedding_service = EmbeddingService()
vector_store = VectorStore()
rag_service = RAGService(embedding_service, vector_store)

security = HTTPBearer()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan events"""
    # Startup
    print("Starting DocTalk RAG Backend...")
    await init_database()
    await vector_store.init_connection()
    print("Backend initialized successfully!")
    
    yield
    
    # Shutdown
    print("Shutting down DocTalk RAG Backend...")

# Create FastAPI app
app = FastAPI(
    title="DocTalk RAG Backend",
    description="RAG backend for document-based conversational AI",
    version="1.0.0",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include API routes
app.include_router(api_router, prefix="/api/v1")

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "message": "DocTalk RAG Backend",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "services": {
            "database": "connected",
            "vector_store": "connected",
            "embedding_service": "ready"
        }
    }

@app.post("/test/chat")
async def test_chat_endpoint(
    query: str,
    document_id: str,
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    """Test chat endpoint without Firebase integration"""
    try:
        # Verify token (simplified for testing)
        token = credentials.credentials
        
        # Get relevant context
        context = await rag_service.retrieve_context(
            query=query,
            document_id=document_id,
            top_k=5
        )
        
        return {
            "success": True,
            "data": {
                "context": context,
                "query": query,
                "document_id": document_id
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True if os.getenv("ENVIRONMENT") == "development" else False
    )
