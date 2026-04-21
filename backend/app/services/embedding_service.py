"""
Embedding service for creating vector embeddings from text chunks
"""

import asyncio
import logging
from typing import List, Dict, Any, Optional
import numpy as np
from sentence_transformers import SentenceTransformer
from app.core.config import settings

logger = logging.getLogger(__name__)

class EmbeddingService:
    """Service for creating text embeddings"""
    
    def __init__(self):
        self.model = None
        self._initialize_model()
    
    def _initialize_model(self):
        """Initialize the embedding model"""
        try:
            self.model = SentenceTransformer('all-MiniLM-L6-v2')
            logger.info("Initialized SentenceTransformer embeddings")
        except Exception as e:
            logger.error(f"Failed to initialize embedding model: {e}")
            raise
    
    async def create_embeddings(self, texts: List[str]) -> List[List[float]]:
        """Create embeddings for a list of texts"""
        if not texts:
            return []
        
        try:
            if self.model:
                return await self._create_sentence_transformer_embeddings(texts)
            else:
                raise Exception("No embedding model available")
                
        except Exception as e:
            logger.error(f"Failed to create embeddings: {e}")
            raise
    
    async def _create_sentence_transformer_embeddings(self, texts: List[str]) -> List[List[float]]:
        """Create embeddings using Sentence Transformers"""
        try:
            # Run in thread pool to avoid blocking
            loop = asyncio.get_event_loop()
            embeddings = await loop.run_in_executor(
                None, 
                self.model.encode, 
                texts
            )
            
            # Convert numpy arrays to lists
            return [embedding.tolist() for embedding in embeddings]
            
        except Exception as e:
            logger.error(f"SentenceTransformer embedding failed: {e}")
            raise
    
    async def create_single_embedding(self, text: str) -> List[float]:
        """Create embedding for a single text"""
        embeddings = await self.create_embeddings([text])
        return embeddings[0] if embeddings else []
    
    def calculate_similarity(self, embedding1: List[float], embedding2: List[float]) -> float:
        """Calculate cosine similarity between two embeddings"""
        try:
            # Convert to numpy arrays
            vec1 = np.array(embedding1)
            vec2 = np.array(embedding2)
            
            # Calculate cosine similarity
            dot_product = np.dot(vec1, vec2)
            norm1 = np.linalg.norm(vec1)
            norm2 = np.linalg.norm(vec2)
            
            if norm1 == 0 or norm2 == 0:
                return 0.0
            
            similarity = dot_product / (norm1 * norm2)
            return float(similarity)
            
        except Exception as e:
            logger.error(f"Failed to calculate similarity: {e}")
            return 0.0
    
    def find_most_similar(
        self, 
        query_embedding: List[float], 
        candidate_embeddings: List[List[float]],
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """Find most similar embeddings to query"""
        similarities = []
        
        for i, candidate_embedding in enumerate(candidate_embeddings):
            similarity = self.calculate_similarity(query_embedding, candidate_embedding)
            similarities.append({
                'index': i,
                'similarity': similarity,
                'embedding': candidate_embedding
            })
        
        # Sort by similarity and return top_k
        similarities.sort(key=lambda x: x['similarity'], reverse=True)
        return similarities[:top_k]
    
    def estimate_tokens(self, text: str) -> int:
        """Estimate token count for text"""
        # Fallback estimation (rough: 1 token ~ 4 characters)
        return max(1, len(text) // 4)
    
    def truncate_text(self, text: str, max_tokens: int = 8191) -> str:
        """Truncate text to fit within token limit"""
        # Fallback: truncate by character count
        max_chars = max_tokens * 4  # Rough estimate
        return text[:max_chars] if len(text) > max_chars else text
    
    async def create_document_embeddings(
        self, 
        chunks: List[Dict[str, Any]], 
        document_id: str
    ) -> List[Dict[str, Any]]:
        """Create embeddings for document chunks"""
        if not chunks:
            return []
        
        # Extract text from chunks
        texts = [chunk['text'] for chunk in chunks]
        
        # Create embeddings
        embeddings = await self.create_embeddings(texts)
        
        # Combine chunks with embeddings
        chunk_embeddings = []
        for i, (chunk, embedding) in enumerate(zip(chunks, embeddings)):
            chunk_embedding = {
                'chunk_id': chunk['chunk_id'],
                'document_id': document_id,
                'chunk_index': chunk['chunk_index'],
                'text': chunk['text'],
                'embedding': embedding,
                'word_count': chunk.get('word_count', 0),
                'token_count': self.estimate_tokens(chunk['text']),
                'created_at': None  # Will be set by vector store
            }
            chunk_embeddings.append(chunk_embedding)
        
        return chunk_embeddings
    
    def get_embedding_dimension(self) -> int:
        """Get the dimension of embeddings"""
        if self.model:
            return self.model.get_sentence_embedding_dimension()
        else:
            return 384  # Default for all-MiniLM-L6-v2
    
    async def batch_create_embeddings(
        self, 
        texts: List[str], 
        batch_size: int = 100
    ) -> List[List[float]]:
        """Create embeddings in batches to handle large documents"""
        all_embeddings = []
        
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            batch_embeddings = await self.create_embeddings(batch)
            all_embeddings.extend(batch_embeddings)
            
            # Add small delay to avoid rate limits
            if i + batch_size < len(texts):
                await asyncio.sleep(0.1)
        
        return all_embeddings
