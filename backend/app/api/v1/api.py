"""
Main API router for DocTalk RAG Backend
"""

from __future__ import annotations

import logging
import re
import shutil
import uuid
from datetime import datetime
from pathlib import Path

from fastapi import APIRouter, BackgroundTasks, File, Form, HTTPException, UploadFile

from app.core.config import settings
from app.core.local_database import local_database
from app.models.schemas import (
    BaseResponse,
    ChatRequest,
    ChatResponse,
    DocumentType,
    DocumentProcessRequest,
    DocumentProcessResponse,
    DocumentSearchRequest,
    DocumentSearchResponse,
    DocumentStatus,
    DocumentStatusResponse,
    DocumentSummaryResponse,
)

logger = logging.getLogger(__name__)

api_router = APIRouter()

document_processor = None
vector_store = None
rag_service = None


async def get_document_processor():
    global document_processor
    if document_processor is None:
        try:
            from app.services.document_processor import DocumentProcessor

            document_processor = DocumentProcessor()
        except Exception as exc:
            raise HTTPException(status_code=503, detail=f"Document processing is unavailable: {exc}") from exc
    return document_processor


def _safe_filename(filename: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]", "_", filename)
    return cleaned or f"upload_{uuid.uuid4().hex}"


def _infer_file_type(filename: str) -> str:
    suffix = Path(filename).suffix.lower()
    if suffix == ".pdf":
        return "pdf"
    if suffix == ".txt":
        return "txt"
    if suffix == ".docx":
        return "docx"
    raise HTTPException(status_code=400, detail="Unsupported file type. Allowed: pdf, txt, docx")


async def get_rag_service():
    global rag_service, vector_store
    if rag_service is None:
        try:
            from app.services.embedding_service import EmbeddingService
            from app.services.rag_service import RAGService
            from app.services.vector_store import VectorStore

            embedding_service_local = EmbeddingService()
            vector_store = VectorStore()
            await vector_store.init_connection()
            rag_service = RAGService(embedding_service_local, vector_store)
        except Exception as exc:
            raise HTTPException(status_code=503, detail=f"RAG services are unavailable: {exc}") from exc
    return rag_service


async def get_vector_store():
    global vector_store
    if vector_store is None:
        try:
            from app.services.vector_store import VectorStore

            vector_store = VectorStore()
            await vector_store.init_connection()
        except Exception as exc:
            raise HTTPException(status_code=503, detail=f"Vector store is unavailable: {exc}") from exc
    return vector_store


@api_router.post("/documents/upload", response_model=DocumentProcessResponse)
async def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    user_id: str = Form("local_user"),
):
    """Upload a file to local storage, then process it in the background for RAG."""
    try:
        original_name = file.filename or f"upload_{uuid.uuid4().hex}"
        file_type_value = _infer_file_type(original_name)
        file_type = DocumentType(file_type_value)

        document_id = str(uuid.uuid4())
        safe_name = _safe_filename(original_name)
        upload_root = Path(settings.upload_dir)
        if not upload_root.is_absolute():
            upload_root = Path(__file__).resolve().parents[3] / upload_root

        target_dir = upload_root / document_id
        target_dir.mkdir(parents=True, exist_ok=True)
        stored_path = target_dir / safe_name

        with stored_path.open("wb") as out_file:
            shutil.copyfileobj(file.file, out_file)

        file_size = stored_path.stat().st_size
        await local_database.upsert_document(
            {
                "document_id": document_id,
                "user_id": user_id,
                "file_name": original_name,
                "file_type": file_type.value,
                "file_size": file_size,
                "download_url": str(stored_path.resolve()),
                "status": DocumentStatus.UPLOADING.value,
                "uploaded_at": datetime.utcnow().isoformat(),
                "metadata": {
                    "original_file_name": original_name,
                    "stored_file_name": safe_name,
                    "storage_path": str(stored_path.resolve()),
                },
            }
        )

        background_tasks.add_task(
            process_uploaded_document_background,
            document_id,
            user_id,
            original_name,
            file_type,
            str(stored_path.resolve()),
        )

        return DocumentProcessResponse(
            success=True,
            message="File uploaded successfully. Processing started.",
            document_id=document_id,
            status=DocumentStatus.PROCESSING,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Document upload failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))
    finally:
        await file.close()


async def process_uploaded_document_background(
    document_id: str,
    user_id: str,
    file_name: str,
    file_type: DocumentType,
    stored_path: str,
):
    """Background worker: convert uploaded file to RAG chunks/embeddings."""
    await local_database.update_document_status(document_id, DocumentStatus.PROCESSING.value)

    request = DocumentProcessRequest(
        document_id=document_id,
        user_id=user_id,
        file_name=file_name,
        file_type=file_type,
        download_url=stored_path,
    )
    await process_document_background(request)


@api_router.post("/documents/process", response_model=DocumentProcessResponse)
async def process_document(
    request: DocumentProcessRequest,
    background_tasks: BackgroundTasks,
):
    """Process a document and create embeddings"""
    try:
        await local_database.upsert_document(
            {
                "document_id": request.document_id,
                "user_id": request.user_id,
                "file_name": request.file_name,
                "file_type": request.file_type.value,
                "file_size": 0,
                "download_url": request.download_url,
                "status": DocumentStatus.PROCESSING.value,
                "uploaded_at": datetime.utcnow().isoformat(),
                "metadata": {},
            }
        )

        background_tasks.add_task(process_document_background, request)

        return DocumentProcessResponse(
            success=True,
            message="Document processing started",
            document_id=request.document_id,
            status=DocumentStatus.PROCESSING,
        )
    except Exception as exc:
        logger.error("Document processing failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


async def process_document_background(request: DocumentProcessRequest):
    """Background task for document processing"""
    try:
        processor = await get_document_processor()
        metadata = await processor.process_document(
            document_id=request.document_id,
            user_id=request.user_id,
            file_name=request.file_name,
            file_type=request.file_type,
            download_url=request.download_url,
        )

        if metadata.status == DocumentStatus.PROCESSED:
            rag = await get_rag_service()
            chunks = metadata.metadata.get("chunks", [])

            if chunks:
                chunk_embeddings = await rag.embedding_service.create_document_embeddings(
                    chunks,
                    request.document_id,
                )

                vector_store_instance = await get_vector_store()
                success = await vector_store_instance.store_document_embeddings(
                    chunk_embeddings,
                    request.document_id,
                )

                if success:
                    await local_database.upsert_document(
                        {
                            "document_id": request.document_id,
                            "user_id": request.user_id,
                            "file_name": request.file_name,
                            "file_type": request.file_type.value,
                            "file_size": metadata.file_size,
                            "download_url": request.download_url,
                            "status": DocumentStatus.PROCESSED.value,
                            "uploaded_at": metadata.uploaded_at.isoformat(),
                            "processed_at": metadata.processed_at.isoformat() if metadata.processed_at else None,
                            "page_count": metadata.page_count,
                            "word_count": metadata.word_count,
                            "chunk_count": metadata.chunk_count,
                            "metadata": metadata.metadata,
                        }
                    )
                else:
                    await local_database.update_document_status(
                        request.document_id,
                        DocumentStatus.FAILED.value,
                        error_message="Failed to store embeddings",
                    )
            else:
                await local_database.update_document_status(
                    request.document_id,
                    DocumentStatus.FAILED.value,
                    error_message="No text content found",
                )
        else:
            await local_database.update_document_status(
                request.document_id,
                DocumentStatus.FAILED.value,
                error_message=metadata.error_message,
            )
    except Exception as exc:
        logger.error("Background document processing failed: %s", exc)
        await local_database.update_document_status(
            request.document_id,
            DocumentStatus.FAILED.value,
            error_message=str(exc),
        )


@api_router.post("/chat/query", response_model=ChatResponse)
async def chat_query(request: ChatRequest):
    """Chat with a document using RAG"""
    try:
        document = await local_database.get_document(request.document_id)
        if not document:
            raise HTTPException(status_code=404, detail="Document not found")

        if document["status"] != DocumentStatus.PROCESSED.value:
            raise HTTPException(status_code=400, detail="Document not processed yet")

        rag = await get_rag_service()
        response = await rag.chat_with_document(request)

        message_timestamp = int(datetime.utcnow().timestamp() * 1000)

        await local_database.add_chat_message(
            {
                "message_id": f"{request.session_id or request.document_id}-{message_timestamp}-user",
                "session_id": request.session_id or request.document_id,
                "document_id": request.document_id,
                "user_id": request.user_id,
                "content": request.query,
                "message_type": "user",
                "timestamp": message_timestamp,
                "metadata": {},
            }
        )
        await local_database.add_chat_message(
            {
                "message_id": f"{request.session_id or request.document_id}-{message_timestamp}-ai",
                "session_id": request.session_id or request.document_id,
                "document_id": request.document_id,
                "user_id": request.user_id,
                "content": response.answer,
                "message_type": "ai",
                "timestamp": message_timestamp + 1,
                "metadata": {
                    "contextUsed": len(response.context) > 0,
                    "responseTime": response.response_time,
                    "tokensUsed": response.tokens_used,
                    "modelUsed": response.model_used,
                    "confidence": response.confidence,
                },
            }
        )

        return response
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Chat query failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


@api_router.post("/documents/{document_id}/search", response_model=DocumentSearchResponse)
async def search_document(document_id: str, request: DocumentSearchRequest):
    """Search within a document"""
    try:
        document = await local_database.get_document(document_id)
        if not document:
            raise HTTPException(status_code=404, detail="Document not found")

        rag = await get_rag_service()
        results = await rag.search_in_document(
            document_id=document_id,
            query=request.query,
            top_k=request.top_k,
        )

        return DocumentSearchResponse(
            success=True,
            results=results,
            scores=[1.0] * len(results),
            query=request.query,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Document search failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


@api_router.get("/documents/{document_id}/summary", response_model=DocumentSummaryResponse)
async def get_document_summary(document_id: str):
    """Get document summary"""
    try:
        document = await local_database.get_document(document_id)
        if not document:
            raise HTTPException(status_code=404, detail="Document not found")

        rag = await get_rag_service()
        summary = await rag.get_document_summary(document_id)
        word_count = document.get("word_count") or 0
        reading_time = max(1, word_count // 200)

        return DocumentSummaryResponse(
            success=True,
            summary=summary,
            word_count=word_count,
            reading_time=reading_time,
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Document summary failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


@api_router.get("/documents/{document_id}/status", response_model=DocumentStatusResponse)
async def get_document_status(document_id: str):
    """Get document processing status"""
    try:
        document = await local_database.get_document(document_id)
        if not document:
            raise HTTPException(status_code=404, detail="Document not found")

        return DocumentStatusResponse(
            success=True,
            document_id=document_id,
            status=DocumentStatus(document["status"]),
            error_message=document.get("error_message"),
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Get document status failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


@api_router.delete("/documents/{document_id}/delete", response_model=BaseResponse)
async def delete_document(document_id: str):
    """Delete a document and its embeddings"""
    try:
        document = await local_database.get_document(document_id)
        if not document:
            raise HTTPException(status_code=404, detail="Document not found")

        vector_store_instance = await get_vector_store()
        await vector_store_instance.delete_document(document_id)

        return BaseResponse(
            success=True,
            message="Document deleted successfully",
        )
    except HTTPException:
        raise
    except Exception as exc:
        logger.error("Document deletion failed: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))


@api_router.get("/health/rag")
async def health_check():
    """Health check for RAG services"""
    try:
        rag = await get_rag_service()
        health = await rag.health_check()
        return health
    except Exception as exc:
        logger.error("Health check failed: %s", exc)
        return {"status": "unhealthy", "error": str(exc)}