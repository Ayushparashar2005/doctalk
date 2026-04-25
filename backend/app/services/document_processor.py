"""
Document processing service for extracting text and metadata from documents
"""

import asyncio
import aiofiles
import aiohttp
from typing import List, Tuple, Dict, Any, Optional
import logging
from pathlib import Path
import tempfile
import os
from urllib.parse import urlparse, unquote
from zipfile import ZipFile
from xml.etree import ElementTree as ET
from PyPDF2 import PdfReader
from PyPDF2 import PdfWriter
import pypdf
import nltk
from nltk.tokenize import sent_tokenize, word_tokenize
from nltk.corpus import stopwords
from textstat import flesch_reading_ease, flesch_kincaid_grade
import spacy
from datetime import datetime

from app.core.config import settings
from app.models.schemas import DocumentMetadata, DocumentStatus, DocumentType

logger = logging.getLogger(__name__)

class DocumentProcessor:
    """Service for processing uploaded documents"""
    
    def __init__(self):
        self.chunk_size = settings.chunk_size
        self.chunk_overlap = settings.chunk_overlap
        self.max_tokens_per_chunk = settings.max_tokens_per_chunk
        
        # Download NLTK data
        self._download_nltk_data()
        
        # Load spaCy model
        try:
            self.nlp = spacy.load("en_core_web_sm")
        except OSError:
            logger.warning("spaCy model not found. Some features may be limited.")
            self.nlp = None
    
    def _download_nltk_data(self):
        """Download required NLTK data"""
        try:
            nltk.download('punkt', quiet=True)
            nltk.download('stopwords', quiet=True)
            nltk.download('averaged_perceptron_tagger', quiet=True)
        except Exception as e:
            logger.warning(f"Failed to download NLTK data: {e}")
    
    async def process_document(
        self, 
        document_id: str,
        user_id: str,
        file_name: str,
        file_type: DocumentType,
        download_url: str
    ) -> DocumentMetadata:
        """Process a document from download URL or local file URL."""
        
        start_time = datetime.utcnow()
        logger.info(f"Processing document {document_id} for user {user_id}")
        temp_file_path: Optional[str] = None
        
        try:
            # Resolve local file path or download a remote file.
            file_path, temp_file_path = await self._resolve_source_file(download_url, file_name)
            
            # Extract text and metadata
            text, metadata = await self._extract_text(file_path, file_type)
            
            # Create chunks
            chunks = self._create_chunks(text)
            
            # Calculate statistics
            stats = self._calculate_statistics(text, chunks)
            
            # Create document metadata
            document_metadata = DocumentMetadata(
                document_id=document_id,
                user_id=user_id,
                file_name=file_name,
                file_type=file_type,
                file_size=os.path.getsize(file_path),
                download_url=download_url,
                status=DocumentStatus.PROCESSED,
                uploaded_at=start_time,
                processed_at=datetime.utcnow(),
                page_count=metadata.get('page_count'),
                word_count=stats['word_count'],
                chunk_count=len(chunks),
                metadata={
                    'chunks': chunks,
                    'stats': stats,
                    'extraction_metadata': metadata
                }
            )
            
            # Clean up temporary download only. Persisted local uploads are retained.
            if temp_file_path and os.path.exists(temp_file_path):
                os.unlink(temp_file_path)
            
            processing_time = (datetime.utcnow() - start_time).total_seconds()
            logger.info(f"Document {document_id} processed in {processing_time:.2f}s")
            
            return document_metadata
            
        except Exception as e:
            logger.error(f"Failed to process document {document_id}: {e}")
            if temp_file_path and os.path.exists(temp_file_path):
                os.unlink(temp_file_path)
            return DocumentMetadata(
                document_id=document_id,
                user_id=user_id,
                file_name=file_name,
                file_type=file_type,
                file_size=0,
                download_url=download_url,
                status=DocumentStatus.FAILED,
                uploaded_at=start_time,
                error_message=str(e)
            )

    async def process_local_document(
        self,
        document_id: str,
        user_id: str,
        file_name: str,
        file_type: DocumentType,
        file_path: str,
    ) -> DocumentMetadata:
        """Process a document that has already been saved locally."""
        return await self.process_document(
            document_id=document_id,
            user_id=user_id,
            file_name=file_name,
            file_type=file_type,
            download_url=f"file:///{Path(file_path).resolve().as_posix()}",
        )

    async def _resolve_source_file(self, source: str, filename: str) -> Tuple[str, Optional[str]]:
        """Return (file_path, temp_download_path_if_any)."""
        parsed = urlparse(source)
        scheme = parsed.scheme.lower()

        if scheme in ("http", "https"):
            downloaded = await self._download_file(source, filename)
            return downloaded, downloaded

        if scheme == "file":
            raw_path = unquote(parsed.path or "")
            if os.name == "nt" and raw_path.startswith("/") and len(raw_path) > 2 and raw_path[2] == ":":
                raw_path = raw_path[1:]
            file_path = raw_path or unquote(source.replace("file://", "", 1))
        else:
            file_path = source

        file_path = os.path.normpath(file_path)
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"File not found at path: {file_path}")

        return file_path, None
    
    async def _download_file(self, url: str, filename: str) -> str:
        """Download file from URL to temporary location"""
        async with aiohttp.ClientSession() as session:
            async with session.get(url) as response:
                if response.status != 200:
                    raise Exception(f"Failed to download file: {response.status}")
                
                # Create temporary file
                temp_dir = tempfile.gettempdir()
                file_path = os.path.join(temp_dir, f"doctalk_{filename}")
                
                # Write file
                async with aiofiles.open(file_path, 'wb') as f:
                    async for chunk in response.content.iter_chunked(8192):
                        await f.write(chunk)
                
                return file_path
    
    async def _extract_text(self, file_path: str, file_type: DocumentType) -> Tuple[str, Dict[str, Any]]:
        """Extract text and metadata from document"""
        
        if file_type == DocumentType.PDF:
            return await self._extract_pdf_text(file_path)
        elif file_type == DocumentType.TXT:
            return await self._extract_txt_text(file_path)
        elif file_type == DocumentType.DOCX:
            return await self._extract_docx_text(file_path)
        else:
            raise ValueError(f"Unsupported file type: {file_type}")

    async def _extract_docx_text(self, file_path: str) -> Tuple[str, Dict[str, Any]]:
        """Extract text from DOCX using only stdlib XML/ZIP parsing."""
        try:
            with ZipFile(file_path) as docx_zip:
                with docx_zip.open("word/document.xml") as xml_file:
                    xml_content = xml_file.read()

            root = ET.fromstring(xml_content)
            namespace = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}

            paragraphs: List[str] = []
            for paragraph in root.findall(".//w:p", namespace):
                parts = [node.text for node in paragraph.findall(".//w:t", namespace) if node.text]
                text = "".join(parts).strip()
                if text:
                    paragraphs.append(text)

            full_text = "\n\n".join(paragraphs)
            metadata = {
                "paragraph_count": len(paragraphs),
                "page_count": None,
            }
            return full_text, metadata
        except Exception as e:
            logger.error(f"Failed to extract text from DOCX file: {e}")
            raise
    
    async def _extract_pdf_text(self, file_path: str) -> Tuple[str, Dict[str, Any]]:
        """Extract text from PDF file"""
        metadata = {}
        
        try:
            # Try PyPDF2 first
            with open(file_path, 'rb') as file:
                pdf_reader = PdfReader(file)
                
                # Extract metadata
                if pdf_reader.metadata:
                    metadata.update({
                        'title': pdf_reader.metadata.get('/Title', ''),
                        'author': pdf_reader.metadata.get('/Author', ''),
                        'subject': pdf_reader.metadata.get('/Subject', ''),
                        'creator': pdf_reader.metadata.get('/Creator', ''),
                        'producer': pdf_reader.metadata.get('/Producer', ''),
                        'creation_date': str(pdf_reader.metadata.get('/CreationDate', '')),
                        'modification_date': str(pdf_reader.metadata.get('/ModDate', ''))
                    })
                
                metadata['page_count'] = len(pdf_reader.pages)
                
                # Extract text from each page
                text_parts = []
                for page_num, page in enumerate(pdf_reader.pages):
                    try:
                        page_text = page.extract_text()
                        if page_text.strip():
                            text_parts.append(page_text)
                    except Exception as e:
                        logger.warning(f"Failed to extract text from page {page_num}: {e}")
                
                text = '\n\n'.join(text_parts)
                
                # If PyPDF2 didn't work well, try pypdf
                if len(text) < 100:  # Too short, probably failed
                    text = await self._extract_with_pypdf(file_path)
                
        except Exception as e:
            logger.error(f"PyPDF2 failed, trying pypdf: {e}")
            text = await self._extract_with_pypdf(file_path)
        
        return text, metadata
    
    async def _extract_with_pypdf(self, file_path: str) -> str:
        """Extract text using pypdf as fallback"""
        try:
            reader = pypdf.PdfReader(file_path)
            text_parts = []
            
            for page in reader.pages:
                try:
                    page_text = page.extract_text()
                    if page_text.strip():
                        text_parts.append(page_text)
                except Exception as e:
                    logger.warning(f"pypdf failed to extract page: {e}")
            
            return '\n\n'.join(text_parts)
        except Exception as e:
            logger.error(f"pypdf extraction failed: {e}")
            raise
    
    async def _extract_txt_text(self, file_path: str) -> Tuple[str, Dict[str, Any]]:
        """Extract text from TXT file"""
        try:
            async with aiofiles.open(file_path, 'r', encoding='utf-8') as f:
                text = await f.read()
            
            # Try different encodings if utf-8 fails
            if not text.strip():
                encodings = ['latin-1', 'cp1252', 'iso-8859-1']
                for encoding in encodings:
                    try:
                        async with aiofiles.open(file_path, 'r', encoding=encoding) as f:
                            text = await f.read()
                        if text.strip():
                            break
                    except UnicodeDecodeError:
                        continue
            
            metadata = {
                'encoding': 'utf-8',
                'line_count': text.count('\n') + 1,
                'char_count': len(text)
            }
            
            return text, metadata
            
        except Exception as e:
            logger.error(f"Failed to extract text from TXT file: {e}")
            raise
    
    def _create_chunks(self, text: str) -> List[Dict[str, Any]]:
        """Create text chunks for embedding"""
        chunks = []
        
        # Split text into paragraphs first
        paragraphs = text.split('\n\n')
        current_chunk = ""
        chunk_index = 0
        
        for paragraph in paragraphs:
            paragraph = paragraph.strip()
            if not paragraph:
                continue
            
            # Check if adding this paragraph would exceed chunk size
            if len(current_chunk) + len(paragraph) > self.chunk_size and current_chunk:
                # Save current chunk
                chunks.append({
                    'chunk_id': f"{chunk_index}",
                    'text': current_chunk.strip(),
                    'chunk_index': chunk_index,
                    'word_count': len(current_chunk.split())
                })
                chunk_index += 1
                current_chunk = paragraph
            else:
                current_chunk += '\n\n' + paragraph if current_chunk else paragraph
        
        # Add the last chunk
        if current_chunk.strip():
            chunks.append({
                'chunk_id': f"{chunk_index}",
                'text': current_chunk.strip(),
                'chunk_index': chunk_index,
                'word_count': len(current_chunk.split())
            })
        
        # Create overlapping chunks for better context
        if self.chunk_overlap > 0:
            chunks = self._create_overlapping_chunks(chunks)
        
        return chunks
    
    def _create_overlapping_chunks(self, chunks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Create overlapping chunks for better context retrieval"""
        if len(chunks) <= 1:
            return chunks
        
        overlapping_chunks = []
        
        for i, chunk in enumerate(chunks):
            # Start with current chunk
            chunk_text = chunk['text']
            
            # Add overlap from previous chunk
            if i > 0 and self.chunk_overlap > 0:
                prev_chunk = chunks[i - 1]['text']
                words = prev_chunk.split()
                if len(words) > self.chunk_overlap:
                    overlap_text = ' '.join(words[-self.chunk_overlap:])
                    chunk_text = overlap_text + '\n\n' + chunk_text
            
            # Add overlap from next chunk
            if i < len(chunks) - 1 and self.chunk_overlap > 0:
                next_chunk = chunks[i + 1]['text']
                words = next_chunk.split()
                if len(words) > self.chunk_overlap:
                    overlap_text = ' '.join(words[:self.chunk_overlap])
                    chunk_text = chunk_text + '\n\n' + overlap_text
            
            overlapping_chunks.append({
                'chunk_id': chunk['chunk_id'],
                'text': chunk_text,
                'chunk_index': chunk['chunk_index'],
                'word_count': len(chunk_text.split()),
                'has_overlap': True
            })
        
        return overlapping_chunks
    
    def _calculate_statistics(self, text: str, chunks: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Calculate document statistics"""
        words = text.split()
        sentences = sent_tokenize(text)
        
        stats = {
            'word_count': len(words),
            'sentence_count': len(sentences),
            'chunk_count': len(chunks),
            'avg_words_per_sentence': len(words) / len(sentences) if sentences else 0,
            'avg_words_per_chunk': len(words) / len(chunks) if chunks else 0
        }
        
        # Readability scores
        try:
            stats['flesch_reading_ease'] = flesch_reading_ease(text)
            stats['flesch_kincaid_grade'] = flesch_kincaid_grade(text)
        except:
            stats['flesch_reading_ease'] = 0
            stats['flesch_kincaid_grade'] = 0
        
        # Language processing with spaCy
        if self.nlp:
            try:
                doc = self.nlp(text[:100000])  # Limit text length for processing
                stats['unique_words'] = len(set([token.text.lower() for token in doc if not token.is_punct]))
                stats['named_entities'] = len(doc.ents)
                stats['pos_tags'] = len(set([token.pos_ for token in doc]))
            except Exception as e:
                logger.warning(f"spaCy processing failed: {e}")
                stats['unique_words'] = len(set([word.lower() for word in words]))
                stats['named_entities'] = 0
                stats['pos_tags'] = 0
        else:
            stats['unique_words'] = len(set([word.lower() for word in words]))
            stats['named_entities'] = 0
            stats['pos_tags'] = 0
        
        # Estimated reading time (average 200 words per minute)
        stats['reading_time_minutes'] = max(1, len(words) // 200)
        
        return stats
    
    def get_document_summary(self, metadata: DocumentMetadata) -> str:
        """Generate a summary of the document"""
        if metadata.status != DocumentStatus.PROCESSED:
            return "Document not processed yet."
        
        stats = metadata.metadata.get('stats', {})
        chunks = metadata.metadata.get('chunks', [])
        
        # Create summary from first few chunks
        summary_chunks = chunks[:3] if len(chunks) >= 3 else chunks
        summary_text = ' '.join([chunk['text'] for chunk in summary_chunks])
        
        # Limit summary length
        if len(summary_text) > 500:
            summary_text = summary_text[:500] + "..."
        
        return summary_text
    
    async def search_in_document(
        self, 
        document_id: str, 
        query: str, 
        metadata: DocumentMetadata
    ) -> List[str]:
        """Search for text within a document"""
        if metadata.status != DocumentStatus.PROCESSED:
            return []
        
        chunks = metadata.metadata.get('chunks', [])
        query_lower = query.lower()
        
        results = []
        for chunk in chunks:
            if query_lower in chunk['text'].lower():
                # Extract context around the match
                text = chunk['text']
                start_idx = text.lower().find(query_lower)
                if start_idx != -1:
                    # Get context (50 chars before and after)
                    context_start = max(0, start_idx - 50)
                    context_end = min(len(text), start_idx + len(query) + 50)
                    context = text[context_start:context_end]
                    results.append(context)
        
        return results
