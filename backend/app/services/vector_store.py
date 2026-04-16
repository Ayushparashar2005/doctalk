"""
Vector store service for managing document embeddings in Pinecone
"""

import asyncio
import logging
from typing import List, Dict, Any, Optional, Tuple
import pinecone
from pinecone import Pinecone, ServerlessSpec, PodSpec
import numpy as np
from datetime import datetime
import json

from app.core.config import settings
from app.services.embedding_service import EmbeddingService

logger = logging.getLogger(__name__)

class VectorStore:
    """Service for managing vector embeddings in Pinecone"""
    
    def __init__(self):
        self.client = None
        self.index = None
        self.index_name = settings.pinecone_index_name
        self.dimension = settings.pinecone_dimension
        self.similarity_threshold = settings.similarity_threshold
    
    async def init_connection(self):
        """Initialize Pinecone connection"""
        try:
            # Initialize Pinecone client
            self.client = Pinecone(api_key=settings.pinecone_api_key)
            
            # Check if index exists
            if self.index_name not in self.client.list_indexes().names():
                await self._create_index()
            
            # Connect to index
            self.index = self.client.Index(self.index_name)
            
            # Get index stats
            stats = self.index.describe_index_stats()
            logger.info(f"Connected to Pinecone index: {self.index_name}")
            logger.info(f"Index stats: {stats}")
            
        except Exception as e:
            logger.error(f"Failed to initialize Pinecone: {e}")
            raise
    
    async def _create_index(self):
        """Create a new Pinecone index"""
        try:
            # For serverless index (recommended for production)
            self.client.create_index(
                name=self.index_name,
                dimension=self.dimension,
                metric="cosine",
                spec=ServerlessSpec(
                    cloud="aws",
                    region="us-west-2"
                )
            )
            logger.info(f"Created Pinecone index: {self.index_name}")
            
        except Exception as e:
            logger.error(f"Failed to create Pinecone index: {e}")
            raise
    
    async def store_document_embeddings(
        self, 
        chunk_embeddings: List[Dict[str, Any]],
        document_id: str
    ) -> bool:
        """Store document embeddings in Pinecone"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Prepare vectors for upsert
            vectors = []
            current_time = datetime.utcnow().isoformat()
            
            for chunk_embedding in chunk_embeddings:
                vector = {
                    "id": f"{document_id}_{chunk_embedding['chunk_id']}",
                    "values": chunk_embedding['embedding'],
                    "metadata": {
                        "document_id": document_id,
                        "chunk_id": chunk_embedding['chunk_id'],
                        "chunk_index": chunk_embedding['chunk_index'],
                        "text": chunk_embedding['text'],
                        "word_count": chunk_embedding.get('word_count', 0),
                        "token_count": chunk_embedding.get('token_count', 0),
                        "created_at": current_time
                    }
                }
                vectors.append(vector)
            
            # Upsert in batches (Pinecone limit is 1000 vectors per request)
            batch_size = 1000
            for i in range(0, len(vectors), batch_size):
                batch = vectors[i:i + batch_size]
                self.index.upsert(vectors=batch)
                
                # Add small delay to avoid rate limits
                if i + batch_size < len(vectors):
                    await asyncio.sleep(0.1)
            
            logger.info(f"Stored {len(vectors)} embeddings for document {document_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to store embeddings for document {document_id}: {e}")
            return False
    
    async def retrieve_context(
        self, 
        query: str, 
        document_id: str, 
        embedding_service: EmbeddingService,
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """Retrieve relevant context for a query"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Create embedding for query
            query_embedding = await embedding_service.create_single_embedding(query)
            
            # Search for similar vectors
            results = self.index.query(
                vector=query_embedding,
                filter={"document_id": document_id},
                top_k=top_k,
                include_metadata=True,
                include_values=False
            )
            
            # Process results
            context_chunks = []
            for match in results['matches']:
                if match['score'] >= self.similarity_threshold:
                    context_chunks.append({
                        'text': match['metadata']['text'],
                        'score': match['score'],
                        'chunk_id': match['metadata']['chunk_id'],
                        'chunk_index': match['metadata']['chunk_index'],
                        'word_count': match['metadata'].get('word_count', 0)
                    })
            
            logger.info(f"Retrieved {len(context_chunks)} context chunks for query")
            return context_chunks
            
        except Exception as e:
            logger.error(f"Failed to retrieve context: {e}")
            return []
    
    async def search_documents(
        self, 
        query: str, 
        embedding_service: EmbeddingService,
        top_k: int = 10
    ) -> List[Dict[str, Any]]:
        """Search across all documents"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Create embedding for query
            query_embedding = await embedding_service.create_single_embedding(query)
            
            # Search for similar vectors
            results = self.index.query(
                vector=query_embedding,
                top_k=top_k,
                include_metadata=True,
                include_values=False
            )
            
            # Group results by document
            document_results = {}
            for match in results['matches']:
                if match['score'] >= self.similarity_threshold:
                    doc_id = match['metadata']['document_id']
                    if doc_id not in document_results:
                        document_results[doc_id] = []
                    
                    document_results[doc_id].append({
                        'text': match['metadata']['text'],
                        'score': match['score'],
                        'chunk_id': match['metadata']['chunk_id'],
                        'chunk_index': match['metadata']['chunk_index']
                    })
            
            # Sort documents by best match
            sorted_documents = sorted(
                document_results.items(),
                key=lambda x: max(chunk['score'] for chunk in x[1]),
                reverse=True
            )
            
            return [{'document_id': doc_id, 'chunks': chunks} for doc_id, chunks in sorted_documents]
            
        except Exception as e:
            logger.error(f"Failed to search documents: {e}")
            return []
    
    async def delete_document(self, document_id: str) -> bool:
        """Delete all embeddings for a document"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Get all vectors for this document
            results = self.index.query(
                vector=[0.0] * self.dimension,  # Dummy vector
                filter={"document_id": document_id},
                top_k=10000,  # Get all vectors
                include_metadata=False
            )
            
            if results['matches']:
                # Extract vector IDs
                vector_ids = [match['id'] for match in results['matches']]
                
                # Delete vectors
                self.index.delete(ids=vector_ids)
                logger.info(f"Deleted {len(vector_ids)} vectors for document {document_id}")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to delete document {document_id}: {e}")
            return False
    
    async def get_document_stats(self, document_id: str) -> Dict[str, Any]:
        """Get statistics for a document"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Get all vectors for this document
            results = self.index.query(
                vector=[0.0] * self.dimension,  # Dummy vector
                filter={"document_id": document_id},
                top_k=10000,  # Get all vectors
                include_metadata=True
            )
            
            if not results['matches']:
                return {'chunk_count': 0, 'total_words': 0}
            
            # Calculate statistics
            chunks = results['matches']
            total_words = sum(match['metadata'].get('word_count', 0) for match in chunks)
            
            return {
                'chunk_count': len(chunks),
                'total_words': total_words,
                'avg_words_per_chunk': total_words / len(chunks) if chunks else 0
            }
            
        except Exception as e:
            logger.error(f"Failed to get document stats: {e}")
            return {'chunk_count': 0, 'total_words': 0}
    
    async def update_document_metadata(
        self, 
        document_id: str, 
        metadata: Dict[str, Any]
    ) -> bool:
        """Update metadata for all chunks of a document"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            # Get all vectors for this document
            results = self.index.query(
                vector=[0.0] * self.dimension,  # Dummy vector
                filter={"document_id": document_id},
                top_k=10000,  # Get all vectors
                include_metadata=True
            )
            
            if not results['matches']:
                return True
            
            # Update metadata for each vector
            for match in results['matches']:
                updated_metadata = match['metadata'].copy()
                updated_metadata.update(metadata)
                
                self.index.update(
                    id=match['id'],
                    metadata=updated_metadata
                )
            
            logger.info(f"Updated metadata for {len(results['matches'])} chunks")
            return True
            
        except Exception as e:
            logger.error(f"Failed to update document metadata: {e}")
            return False
    
    async def get_index_stats(self) -> Dict[str, Any]:
        """Get overall index statistics"""
        if not self.index:
            raise Exception("Pinecone index not initialized")
        
        try:
            stats = self.index.describe_index_stats()
            return {
                'total_vector_count': stats.get('totalVectorCount', 0),
                'dimension': stats.get('dimension', self.dimension),
                'index_fullness': stats.get('indexFullness', 0),
                'namespaces': stats.get('namespaces', {})
            }
            
        except Exception as e:
            logger.error(f"Failed to get index stats: {e}")
            return {}
    
    async def health_check(self) -> bool:
        """Check if the vector store is healthy"""
        try:
            if not self.index:
                return False
            
            # Try a simple query
            results = self.index.query(
                vector=[0.0] * self.dimension,
                top_k=1,
                include_metadata=False
            )
            
            return True
            
        except Exception as e:
            logger.error(f"Vector store health check failed: {e}")
            return False
