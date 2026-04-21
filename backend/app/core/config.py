"""
Core configuration settings for DocTalk RAG Backend
"""

from pydantic_settings import BaseSettings
from typing import List, Optional
import os

class Settings(BaseSettings):
    """Application settings"""
    
    # Application
    environment: str = "development"
    debug: bool = True
    secret_key: str = "your-secret-key-change-in-production"
    allowed_origins: List[str] = ["http://localhost:3000", "http://localhost:8080"]
    
    # Groq API
    groq_api_key: str = ""
    groq_base_url: str = "https://api.groq.com/openai/v1"
    default_groq_model: str = "llama3-8b-8192"
    
    # Local SQLite storage
    sqlite_db_path: str = "data/doctalk.sqlite3"
    
    # Document Processing
    max_file_size_mb: int = 10
    chunk_size: int = 1000
    chunk_overlap: int = 200
    max_tokens_per_chunk: int = 1000
    
    # RAG Configuration
    max_context_chunks: int = 5
    similarity_threshold: float = 0.7
    rag_temperature: float = 0.7
    max_response_tokens: int = 1000
    
    # Logging
    log_level: str = "INFO"
    log_file: str = "logs/doctalk.log"
    
    class Config:
        env_file = ".env"
        case_sensitive = False

# Create settings instance
settings = Settings()

# Create logs directory if it doesn't exist
os.makedirs(os.path.dirname(settings.log_file), exist_ok=True)
