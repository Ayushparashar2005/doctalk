"""
RAG (Retrieval-Augmented Generation) service for document-based Q&A
"""

import asyncio
import logging
from typing import List, Dict, Any, Optional
import time
from datetime import datetime

from openai import AsyncOpenAI

from app.core.config import settings
from app.services.embedding_service import EmbeddingService
from app.services.vector_store import VectorStore
from app.models.schemas import ChatRequest, ChatResponse

logger = logging.getLogger(__name__)

DEPRECATED_MODEL_ALIASES = {
    "llama3-8b-8192": settings.default_groq_model,
}

class RAGService:
    """Service for Retrieval-Augmented Generation"""
    
    def __init__(self, embedding_service: EmbeddingService, vector_store: VectorStore):
        self.embedding_service = embedding_service
        self.vector_store = vector_store
        self.max_context_chunks = settings.max_context_chunks
        self.rag_temperature = settings.rag_temperature
        self.max_response_tokens = settings.max_response_tokens
        self.client = AsyncOpenAI(
            api_key=settings.groq_api_key,
            base_url=settings.groq_base_url,
        )

    def _limit_context(self, context: List[str], max_total_chars: int = 12000, max_chunk_chars: int = 1400) -> List[str]:
        """Limit context size to avoid model token-per-minute request rejections."""
        if not context:
            return []

        limited: List[str] = []
        total_chars = 0
        for chunk in context:
            if total_chars >= max_total_chars:
                break

            trimmed_chunk = chunk[:max_chunk_chars]
            remaining = max_total_chars - total_chars
            if remaining <= 0:
                break

            if len(trimmed_chunk) > remaining:
                trimmed_chunk = trimmed_chunk[:remaining]

            if trimmed_chunk.strip():
                limited.append(trimmed_chunk)
                total_chars += len(trimmed_chunk)

        return limited
    
    async def retrieve_context(
        self, 
        query: str, 
        document_id: str, 
        top_k: int = 5
    ) -> List[str]:
        """Retrieve relevant context for a query"""
        try:
            # Get context chunks from vector store
            context_chunks = await self.vector_store.retrieve_context(
                query=query,
                document_id=document_id,
                embedding_service=self.embedding_service,
                top_k=top_k
            )
            
            # Extract text from chunks
            context_texts = [chunk['text'] for chunk in context_chunks]
            
            logger.info(f"Retrieved {len(context_texts)} context chunks")
            return context_texts
            
        except Exception as e:
            logger.error(f"Failed to retrieve context: {e}")
            return []
    
    async def generate_response(
        self, 
        query: str, 
        context: List[str], 
        model: str = None,
        allow_context_retry: bool = True,
    ) -> Dict[str, Any]:
        """Generate response using Groq with context"""
        try:
            model = DEPRECATED_MODEL_ALIASES.get(model or "", model or settings.default_groq_model)
            if model not in DEPRECATED_MODEL_ALIASES.values() and not model:
                model = settings.default_groq_model
            context = self._limit_context(context)
            
            # Create system prompt with context
            system_prompt = self._create_system_prompt(context)
            
            # Create messages for Groq
            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": query}
            ]
            
            # Call Groq API
            start_time = time.time()
            response = await self.client.chat.completions.create(
                model=model,
                messages=messages,
                temperature=self.rag_temperature,
                max_tokens=self.max_response_tokens,
                top_p=1.0,
                frequency_penalty=0.0,
                presence_penalty=0.0,
            )
            
            response_time = (time.time() - start_time) * 1000  # Convert to milliseconds
            
            # Extract response data
            answer = response.choices[0].message.content or ""
            tokens_used = response.usage.total_tokens if response.usage else 0
            
            return {
                'answer': answer,
                'response_time': response_time,
                'tokens_used': tokens_used,
                'model_used': model,
                'context_used': len(context) > 0
            }
            
        except Exception as e:
            if "model_decommissioned" in str(e) and model != settings.default_groq_model:
                logger.warning("Groq model %s is deprecated; retrying with %s", model, settings.default_groq_model)
                return await self.generate_response(query=query, context=context, model=settings.default_groq_model)
            error_text = str(e)
            if allow_context_retry and ("rate_limit_exceeded" in error_text or "Request too large" in error_text):
                reduced_context = self._limit_context(context, max_total_chars=2400, max_chunk_chars=600)
                logger.warning("Groq request too large; retrying with reduced context (%s chunks)", len(reduced_context))
                return await self.generate_response(
                    query=query,
                    context=reduced_context,
                    model=model,
                    allow_context_retry=False,
                )
            logger.error(f"Failed to generate response: {e}")
            raise
    
    def _create_system_prompt(self, context: List[str]) -> str:
        """Create system prompt with context"""
        if not context:
            return """You are a helpful AI assistant. Answer the user's question to the best of your ability.
            If you don't know the answer, say so politely. Be concise and helpful."""
        
        context_text = "\n\n".join([f"[Context {i+1}]: {ctx}" for i, ctx in enumerate(context)])
        
        return f"""You are a helpful AI assistant designed to answer questions based on the provided document context.

Context from the document:
{context_text}

Instructions:
1. Use only the provided context to answer the user's question
2. If the context doesn't contain enough information to answer the question, say so politely
3. Be concise, accurate, and helpful
4. If you find relevant information in the context, reference it in your answer
5. Do not make up information that isn't in the context

Now answer the user's question based on the provided context."""
    
    async def chat_with_document(
        self, 
        request: ChatRequest
    ) -> ChatResponse:
        """Main chat method that combines retrieval and generation"""
        start_time = time.time()
        
        try:
            # Retrieve relevant context
            context = await self.retrieve_context(
                query=request.query,
                document_id=request.document_id,
                top_k=request.max_context
            )
            
            # Generate response
            response_data = await self.generate_response(
                query=request.query,
                context=context,
                model=request.model
            )
            
            # Create response object
            chat_response = ChatResponse(
                success=True,
                answer=response_data['answer'],
                context=context,
                sources=[],  # Will be populated if needed
                response_time=response_data['response_time'],
                tokens_used=response_data['tokens_used'],
                model_used=response_data['model_used'],
                confidence=1.0 if context else 0.5,  # Higher confidence with context
                timestamp=datetime.utcnow()
            )
            
            logger.info(f"Generated response in {response_data['response_time']:.2f}ms")
            return chat_response
            
        except Exception as e:
            logger.error(f"Chat failed: {e}")
            return ChatResponse(
                success=False,
                answer="I'm sorry, I encountered an error while processing your request. Please try again.",
                context=[],
                sources=[],
                response_time=(time.time() - start_time) * 1000,
                tokens_used=0,
                model_used=request.model or settings.default_groq_model,
                confidence=0.0,
                timestamp=datetime.utcnow()
            )
    
    async def search_in_document(
        self, 
        document_id: str, 
        query: str, 
        top_k: int = 10
    ) -> List[str]:
        """Search for specific content in a document"""
        try:
            # Retrieve context chunks
            context_chunks = await self.vector_store.retrieve_context(
                query=query,
                document_id=document_id,
                embedding_service=self.embedding_service,
                top_k=top_k
            )
            
            # Return text snippets
            return [chunk['text'] for chunk in context_chunks]
            
        except Exception as e:
            logger.error(f"Search failed: {e}")
            return []
    
    async def get_document_summary(
        self, 
        document_id: str, 
        max_chunks: int = 5
    ) -> str:
        """Generate a summary of a document"""
        try:
            # Get top chunks from the document
            summary_chunks = await self.vector_store.retrieve_context(
                query="main topics key points summary",
                document_id=document_id,
                embedding_service=self.embedding_service,
                top_k=max_chunks
            )
            
            if not summary_chunks:
                return "No content found to summarize."
            
            # Create summary prompt
            context_text = "\n\n".join([chunk['text'] for chunk in summary_chunks])
            
            summary_prompt = f"""Based on the following document content, provide a concise summary of the main topics and key points:

{context_text}

Summary:"""
            
            # Generate summary
            response_data = await self.generate_response(
                query=summary_prompt,
                context=[],  # No additional context needed
                model=settings.default_groq_model
            )
            
            return response_data['answer']
            
        except Exception as e:
            logger.error(f"Summary generation failed: {e}")
            return "Failed to generate document summary."
    
    async def ask_follow_up_question(
        self, 
        original_query: str, 
        document_id: str, 
        follow_up_query: str
    ) -> ChatResponse:
        """Handle follow-up questions with conversation context"""
        try:
            # Retrieve context for both queries
            original_context = await self.retrieve_context(
                query=original_query,
                document_id=document_id,
                top_k=3
            )
            
            follow_up_context = await self.retrieve_context(
                query=follow_up_query,
                document_id=document_id,
                top_k=3
            )
            
            # Combine contexts
            combined_context = list(set(original_context + follow_up_context))
            
            # Create enhanced prompt
            enhanced_prompt = f"""Previous question: {original_query}

Follow-up question: {follow_up_query}

Please answer the follow-up question considering the context from both questions."""
            
            # Generate response
            response_data = await self.generate_response(
                query=enhanced_prompt,
                context=combined_context
            )
            
            return ChatResponse(
                success=True,
                answer=response_data['answer'],
                context=combined_context,
                sources=[],
                response_time=response_data['response_time'],
                tokens_used=response_data['tokens_used'],
                model_used=response_data['model_used'],
                confidence=1.0,
                timestamp=datetime.utcnow()
            )
            
        except Exception as e:
            logger.error(f"Follow-up question failed: {e}")
            raise
    
    async def evaluate_response_quality(
        self, 
        query: str, 
        answer: str, 
        context: List[str]
    ) -> Dict[str, Any]:
        """Evaluate the quality of a response"""
        try:
            # Create evaluation prompt
            evaluation_prompt = f"""Please evaluate the quality of this answer based on the provided context:

Question: {query}

Answer: {answer}

Context: {chr(10).join(context)}

Rate the answer on a scale of 1-10 for:
1. Relevance to the question
2. Accuracy based on the context
3. Completeness
4. Clarity

Provide a brief explanation for your rating."""
            
            # Generate evaluation
            response_data = await self.generate_response(
                query=evaluation_prompt,
                context=[]
            )
            
            return {
                'evaluation': response_data['answer'],
                'response_time': response_data['response_time'],
                'tokens_used': response_data['tokens_used']
            }
            
        except Exception as e:
            logger.error(f"Response evaluation failed: {e}")
            return {'evaluation': 'Evaluation failed', 'response_time': 0, 'tokens_used': 0}
    
    async def health_check(self) -> Dict[str, Any]:
        """Check the health of the RAG service"""
        try:
            # Test embedding service
            test_embedding = await self.embedding_service.create_single_embedding("test")
            
            # Test vector store
            vector_health = await self.vector_store.health_check()
            
            # Test Groq connection
            test_response = await self.generate_response(
                query="Hello, this is a test.",
                context=[]
            )
            
            return {
                'status': 'healthy',
                'embedding_service': 'ok',
                'vector_store': 'ok' if vector_health else 'error',
                'groq_api': 'ok',
                'test_response_time': test_response['response_time']
            }
            
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return {
                'status': 'unhealthy',
                'error': str(e)
            }
