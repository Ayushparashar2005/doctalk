"""
Pydantic models for API requests and responses
"""

from pydantic import BaseModel, Field, validator
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

class DocumentStatus(str, Enum):
    UPLOADING = "uploading"
    PROCESSING = "processing"
    PROCESSED = "processed"
    FAILED = "failed"
    DELETED = "deleted"

class DocumentType(str, Enum):
    PDF = "pdf"
    TXT = "txt"
    DOCX = "docx"

# Request Models
class DocumentProcessRequest(BaseModel):
    document_id: str = Field(..., description="Document ID")
    user_id: str = Field(..., description="User ID")
    file_name: str = Field(..., description="File name")
    file_type: DocumentType = Field(..., description="File type")
    download_url: str = Field(..., description="Download URL")
    
    @validator('file_type')
    def validate_file_type(cls, v):
        if v not in DocumentType:
            raise ValueError(f"File type must be one of: {[dt.value for dt in DocumentType]}")
        return v

class ChatRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=4000, description="User query")
    document_id: str = Field(..., description="Document ID")
    user_id: str = Field(..., description="User ID")
    session_id: Optional[str] = Field(None, description="Chat session ID")
    model: Optional[str] = Field("llama-3.1-8b-instant", description="Groq model to use")
    max_context: Optional[int] = Field(5, ge=1, le=10, description="Max context chunks")

class DocumentSearchRequest(BaseModel):
    query: str = Field(..., min_length=1, description="Search query")
    document_id: str = Field(..., description="Document ID")
    top_k: Optional[int] = Field(5, ge=1, le=20, description="Number of results")

# Response Models
class BaseResponse(BaseModel):
    success: bool = Field(..., description="Request success status")
    message: Optional[str] = Field(None, description="Response message")
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class DocumentProcessResponse(BaseResponse):
    document_id: str = Field(..., description="Document ID")
    status: DocumentStatus = Field(..., description="Processing status")
    page_count: Optional[int] = Field(None, description="Number of pages")
    word_count: Optional[int] = Field(None, description="Word count")
    chunk_count: Optional[int] = Field(None, description="Number of chunks")
    processing_time: Optional[float] = Field(None, description="Processing time in seconds")

class ChatResponse(BaseResponse):
    answer: str = Field(..., description="AI response")
    context: List[str] = Field(default_factory=list, description="Retrieved context")
    sources: List[str] = Field(default_factory=list, description="Source documents")
    response_time: float = Field(..., description="Response time in milliseconds")
    tokens_used: int = Field(..., description="Tokens used")
    model_used: str = Field(..., description="Model used")
    confidence: Optional[float] = Field(None, description="Response confidence")

class DocumentSearchResponse(BaseResponse):
    results: List[str] = Field(..., description="Search results")
    scores: List[float] = Field(..., description="Similarity scores")
    query: str = Field(..., description="Original query")

class DocumentSummaryResponse(BaseResponse):
    summary: str = Field(..., description="Document summary")
    key_points: List[str] = Field(default_factory=list, description="Key points")
    word_count: int = Field(..., description="Total word count")
    reading_time: int = Field(..., description="Estimated reading time in minutes")

class DocumentStatusResponse(BaseResponse):
    document_id: str = Field(..., description="Document ID")
    status: DocumentStatus = Field(..., description="Current status")
    progress: Optional[float] = Field(None, description="Processing progress (0-1)")
    error_message: Optional[str] = Field(None, description="Error message if failed")

# Database Models
class DocumentMetadata(BaseModel):
    document_id: str
    user_id: str
    file_name: str
    file_type: DocumentType
    file_size: int
    download_url: str
    status: DocumentStatus
    uploaded_at: datetime
    processed_at: Optional[datetime] = None
    page_count: Optional[int] = None
    word_count: Optional[int] = None
    chunk_count: Optional[int] = None
    error_message: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)

class ChatSession(BaseModel):
    session_id: str
    document_id: str
    user_id: str
    title: str
    created_at: datetime
    last_message_at: datetime
    message_count: int = 0
    is_active: bool = True

class ChatMessage(BaseModel):
    message_id: str
    session_id: str
    document_id: str
    user_id: str
    content: str
    message_type: str  # "user" or "ai"
    timestamp: datetime
    metadata: Dict[str, Any] = Field(default_factory=dict)

# Error Models
class ErrorResponse(BaseResponse):
    error_code: str = Field(..., description="Error code")
    details: Optional[Dict[str, Any]] = Field(None, description="Error details")

class ValidationError(BaseModel):
    field: str = Field(..., description="Field with validation error")
    message: str = Field(..., description="Validation error message")
    value: Any = Field(..., description="Invalid value")
