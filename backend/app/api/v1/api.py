"""
Main API router for DocTalk RAG Backend
"""

from fastapi import APIRouter, Depends, HTTPException, BackgroundTasks
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import List, Optional
import asyncio
import logging

from app.core.security import get_user_id
from app.models.schemas import (
    DocumentProcessRequest, DocumentProcessResponse,
    ChatRequest, ChatResponse,
    DocumentSearchRequest, DocumentSearchResponse,
    DocumentSummaryResponse, DocumentStatusResponse,
    BaseResponse
)
from app.services.document_processor import DocumentProcessor
from app.services.rag_service import RAGService
from app.services.vector_store import VectorStore
from app.core.database import get_firestore_client

logger = logging.getLogger(__name__)

# Create API router
api_router = APIRouter()
security = HTTPBearer()

# Initialize services
document_processor = DocumentProcessor()
rag_service = None
vector_store = None

# Dependency to get services
async def get_rag_service():
    global rag_service, vector_store
    if rag_service is None:
        from app.services.embedding_service import EmbeddingService
        embedding_service = EmbeddingService()
        vector_store = VectorStore()
        await vector_store.init_connection()
        rag_service = RAGService(embedding_service, vector_store)
    return rag_service

async def get_vector_store():
    global vector_store
    if vector_store is None:
        vector_store = VectorStore()
        await vector_store.init_connection()
    return vector_store

@api_router.post("/documents/process", response_model=DocumentProcessResponse)
async def process_document(
    request: DocumentProcessRequest,
    background_tasks: BackgroundTasks,
    user_id: str = Depends(get_user_id)
):
    """Process a document and create embeddings"""
    try:
        # Process document in background
        background_tasks.add_task(
            process_document_background,
            request,
            user_id
        )
        
        return DocumentProcessResponse(
            success=True,
            message="Document processing started",
            document_id=request.document_id,
            status="processing"
        )
        
    except Exception as e:
        logger.error(f"Document processing failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

async def process_document_background(request: DocumentProcessRequest, user_id: str):
    """Background task for document processing"""
    try:
        # Process document
        metadata = await document_processor.process_document(
            document_id=request.document_id,
            user_id=user_id,
            file_name=request.file_name,
            file_type=request.file_type,
            download_url=request.download_url
        )
        
        # Update Firestore with processing status
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(request.document_id)
        
        if metadata.status.value == "processed":
            # Create embeddings
            rag = await get_rag_service()
            chunks = metadata.metadata.get('chunks', [])
            
            if chunks:
                chunk_embeddings = await rag.embedding_service.create_document_embeddings(
                    chunks, request.document_id
                )
                
                # Store in vector store
                vector_store = await get_vector_store()
                success = await vector_store.store_document_embeddings(
                    chunk_embeddings, request.document_id
                )
                
                if success:
                    doc_ref.update({
                        'status': 'processed',
                        'processedAt': metadata.processed_at,
                        'pageCount': metadata.page_count,
                        'wordCount': metadata.word_count,
                        'chunkCount': metadata.chunk_count
                    })
                else:
                    doc_ref.update({
                        'status': 'failed',
                        'errorMessage': 'Failed to store embeddings'
                    })
            else:
                doc_ref.update({
                    'status': 'failed',
                    'errorMessage': 'No text content found'
                })
        else:
            doc_ref.update({
                'status': 'failed',
                'errorMessage': metadata.error_message
            })
            
    except Exception as e:
        logger.error(f"Background document processing failed: {e}")
        # Update Firestore with error
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(request.document_id)
        doc_ref.update({
            'status': 'failed',
            'errorMessage': str(e)
        })

@api_router.post("/chat/query", response_model=ChatResponse)
async def chat_query(
    request: ChatRequest,
    user_id: str = Depends(get_user_id)
):
    """Chat with a document using RAG"""
    try:
        # Verify user has access to document
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(request.document_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Document not found")
        
        if doc.to_dict().get('userId') != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
        
        # Check if document is processed
        if doc.to_dict().get('status') != 'processed':
            raise HTTPException(status_code=400, detail="Document not processed yet")
        
        # Get RAG service and generate response
        rag = await get_rag_service()
        response = await rag.chat_with_document(request)
        
        # Store chat message in Firestore
        await store_chat_message(request, response, user_id)
        
        return response
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Chat query failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

async def store_chat_message(request: ChatRequest, response: ChatResponse, user_id: str):
    """Store chat message in Firestore"""
    try:
        firestore = get_firestore_client()
        
        # Store user message
        user_message = {
            'content': request.query,
            'messageType': 'user',
            'userId': user_id,
            'documentId': request.document_id,
            'timestamp': firestore.SERVER_TIMESTAMP
        }
        
        # Store AI response
        ai_message = {
            'content': response.answer,
            'messageType': 'ai',
            'userId': user_id,
            'documentId': request.document_id,
            'timestamp': firestore.SERVER_TIMESTAMP,
            'metadata': {
                'contextUsed': len(response.context) > 0,
                'responseTime': response.response_time,
                'tokensUsed': response.tokens_used,
                'modelUsed': response.model_used,
                'confidence': response.confidence
            }
        }
        
        # Add to messages collection
        messages_ref = firestore.collection('messages')
        messages_ref.add(user_message)
        messages_ref.add(ai_message)
        
    except Exception as e:
        logger.error(f"Failed to store chat message: {e}")

@api_router.post("/documents/{document_id}/search", response_model=DocumentSearchResponse)
async def search_document(
    document_id: str,
    request: DocumentSearchRequest,
    user_id: str = Depends(get_user_id)
):
    """Search within a document"""
    try:
        # Verify user has access to document
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(document_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Document not found")
        
        if doc.to_dict().get('userId') != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
        
        # Search in document
        rag = await get_rag_service()
        results = await rag.search_in_document(
            document_id=document_id,
            query=request.query,
            top_k=request.top_k
        )
        
        return DocumentSearchResponse(
            success=True,
            results=results,
            scores=[1.0] * len(results),  # Placeholder scores
            query=request.query
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Document search failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.get("/documents/{document_id}/summary", response_model=DocumentSummaryResponse)
async def get_document_summary(
    document_id: str,
    user_id: str = Depends(get_user_id)
):
    """Get document summary"""
    try:
        # Verify user has access to document
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(document_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Document not found")
        
        if doc.to_dict().get('userId') != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
        
        # Generate summary
        rag = await get_rag_service()
        summary = await rag.get_document_summary(document_id)
        
        # Get document stats
        doc_data = doc.to_dict()
        word_count = doc_data.get('wordCount', 0)
        reading_time = max(1, word_count // 200)  # 200 words per minute
        
        return DocumentSummaryResponse(
            success=True,
            summary=summary,
            word_count=word_count,
            reading_time=reading_time
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Document summary failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.get("/documents/{document_id}/status", response_model=DocumentStatusResponse)
async def get_document_status(
    document_id: str,
    user_id: str = Depends(get_user_id)
):
    """Get document processing status"""
    try:
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(document_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Document not found")
        
        doc_data = doc.to_dict()
        
        if doc_data.get('userId') != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
        
        return DocumentStatusResponse(
            success=True,
            document_id=document_id,
            status=doc_data.get('status', 'unknown'),
            error_message=doc_data.get('errorMessage')
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Get document status failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.delete("/documents/{document_id}/delete", response_model=BaseResponse)
async def delete_document(
    document_id: str,
    user_id: str = Depends(get_user_id)
):
    """Delete a document and its embeddings"""
    try:
        # Verify user has access to document
        firestore = get_firestore_client()
        doc_ref = firestore.collection('documents').document(document_id)
        doc = doc_ref.get()
        
        if not doc.exists:
            raise HTTPException(status_code=404, detail="Document not found")
        
        if doc.to_dict().get('userId') != user_id:
            raise HTTPException(status_code=403, detail="Access denied")
        
        # Delete from vector store
        vector_store = await get_vector_store()
        await vector_store.delete_document(document_id)
        
        # Delete from Firestore
        doc_ref.delete()
        
        # Delete related messages
        messages_ref = firestore.collection('messages')
        messages = messages_ref.where('documentId', '==', document_id).get()
        for message in messages:
            message.reference.delete()
        
        return BaseResponse(
            success=True,
            message="Document deleted successfully"
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Document deletion failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.get("/health/rag")
async def health_check():
    """Health check for RAG services"""
    try:
        rag = await get_rag_service()
        health = await rag.health_check()
        return health
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return {"status": "unhealthy", "error": str(e)}
