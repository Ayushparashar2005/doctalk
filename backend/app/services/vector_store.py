"""
Vector store service for managing document chunks in local SQLite.
"""

from __future__ import annotations

import logging
from typing import Any, Dict, List

import numpy as np

from app.core.local_database import local_database
from app.services.embedding_service import EmbeddingService

logger = logging.getLogger(__name__)


class VectorStore:
    """Service for managing chunk embeddings in SQLite."""

    def __init__(self):
        self.similarity_threshold = 0.35
        self._initialized = False

    async def init_connection(self):
        """Initialize the local database connection."""
        if self._initialized:
            return

        await local_database.initialize()
        self._initialized = True
        logger.info("Initialized local SQLite vector store")

    def _ensure_initialized(self):
        if not self._initialized:
            raise RuntimeError("Local vector store is not initialized")

    def _cosine_similarity(self, left: List[float], right: List[float]) -> float:
        left_vector = np.array(left, dtype=float)
        right_vector = np.array(right, dtype=float)

        if left_vector.size == 0 or right_vector.size == 0:
            return 0.0

        denominator = np.linalg.norm(left_vector) * np.linalg.norm(right_vector)
        if denominator == 0:
            return 0.0

        return float(np.dot(left_vector, right_vector) / denominator)

    async def store_document_embeddings(
        self,
        chunk_embeddings: List[Dict[str, Any]],
        document_id: str,
    ) -> bool:
        """Store document chunks and embeddings in SQLite."""
        self._ensure_initialized()

        try:
            await local_database.add_chunks(document_id, chunk_embeddings)
            logger.info("Stored %s embeddings for document %s", len(chunk_embeddings), document_id)
            return True
        except Exception as exc:
            logger.error("Failed to store embeddings for document %s: %s", document_id, exc)
            return False

    async def retrieve_context(
        self,
        query: str,
        document_id: str,
        embedding_service: EmbeddingService,
        top_k: int = 5,
    ) -> List[Dict[str, Any]]:
        """Retrieve relevant context for a query using cosine similarity."""
        self._ensure_initialized()

        try:
            query_embedding = await embedding_service.create_single_embedding(query)
            chunks = await local_database.get_chunks(document_id)

            scored_chunks: List[Dict[str, Any]] = []
            for chunk in chunks:
                score = self._cosine_similarity(query_embedding, chunk.get("embedding", []))
                if score >= self.similarity_threshold:
                    scored_chunks.append(
                        {
                            "text": chunk["text"],
                            "score": score,
                            "chunk_id": chunk["chunk_id"],
                            "chunk_index": chunk["chunk_index"],
                            "word_count": chunk.get("word_count", 0),
                        }
                    )

            scored_chunks.sort(key=lambda item: item["score"], reverse=True)
            return scored_chunks[:top_k]
        except Exception as exc:
            logger.error("Failed to retrieve context: %s", exc)
            return []

    async def search_documents(
        self,
        query: str,
        embedding_service: EmbeddingService,
        top_k: int = 10,
    ) -> List[Dict[str, Any]]:
        """Search across all documents in SQLite."""
        self._ensure_initialized()

        try:
            query_embedding = await embedding_service.create_single_embedding(query)
            documents: List[Dict[str, Any]] = []

            with local_database._connect() as connection:
                rows = connection.execute("SELECT DISTINCT document_id FROM document_chunks").fetchall()

            for row in rows:
                document_id = row["document_id"]
                chunks = await local_database.get_chunks(document_id)
                matched_chunks = []

                for chunk in chunks:
                    score = self._cosine_similarity(query_embedding, chunk.get("embedding", []))
                    if score >= self.similarity_threshold:
                        matched_chunks.append(
                            {
                                "text": chunk["text"],
                                "score": score,
                                "chunk_id": chunk["chunk_id"],
                                "chunk_index": chunk["chunk_index"],
                            }
                        )

                if matched_chunks:
                    documents.append(
                        {
                            "document_id": document_id,
                            "chunks": sorted(matched_chunks, key=lambda item: item["score"], reverse=True),
                        }
                    )

            documents.sort(
                key=lambda item: max(chunk["score"] for chunk in item["chunks"]),
                reverse=True,
            )
            return documents[:top_k]
        except Exception as exc:
            logger.error("Failed to search documents: %s", exc)
            return []

    async def delete_document(self, document_id: str) -> bool:
        """Delete all stored chunks for a document."""
        self._ensure_initialized()

        try:
            await local_database.delete_document(document_id)
            logger.info("Deleted local data for document %s", document_id)
            return True
        except Exception as exc:
            logger.error("Failed to delete document %s: %s", document_id, exc)
            return False

    async def get_document_stats(self, document_id: str) -> Dict[str, Any]:
        """Get statistics for a document."""
        self._ensure_initialized()

        try:
            chunks = await local_database.get_chunks(document_id)
            if not chunks:
                return {"chunk_count": 0, "total_words": 0}

            total_words = sum(chunk.get("word_count", 0) for chunk in chunks)
            return {
                "chunk_count": len(chunks),
                "total_words": total_words,
                "average_words_per_chunk": total_words / len(chunks),
            }
        except Exception as exc:
            logger.error("Failed to get document stats: %s", exc)
            return {"chunk_count": 0, "total_words": 0}

    async def health_check(self) -> bool:
        """Check whether SQLite is reachable."""
        try:
            return await local_database.ping()
        except Exception as exc:
            logger.error("Vector store health check failed: %s", exc)
            return False