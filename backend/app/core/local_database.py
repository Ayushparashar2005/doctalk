"""
SQLite persistence for documents, chunks, and chat history.
"""

from __future__ import annotations

import asyncio
import json
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional

from app.core.config import settings


class LocalDatabase:
    def __init__(self, db_path: Optional[str] = None):
        base_path = Path(db_path or settings.sqlite_db_path)
        if not base_path.is_absolute():
            base_path = Path(__file__).resolve().parents[2] / base_path

        self.db_path = base_path
        self.db_path.parent.mkdir(parents=True, exist_ok=True)

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.db_path, check_same_thread=False)
        connection.row_factory = sqlite3.Row
        return connection

    def _initialize_sync(self) -> None:
        with self._connect() as connection:
            connection.executescript(
                """
                PRAGMA journal_mode=WAL;

                CREATE TABLE IF NOT EXISTS documents (
                    document_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    file_type TEXT NOT NULL,
                    file_size INTEGER NOT NULL DEFAULT 0,
                    download_url TEXT NOT NULL,
                    status TEXT NOT NULL,
                    uploaded_at TEXT NOT NULL,
                    processed_at TEXT,
                    page_count INTEGER,
                    word_count INTEGER,
                    chunk_count INTEGER,
                    error_message TEXT,
                    metadata_json TEXT NOT NULL DEFAULT '{}'
                );

                CREATE TABLE IF NOT EXISTS document_chunks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    document_id TEXT NOT NULL,
                    chunk_id TEXT NOT NULL,
                    chunk_index INTEGER NOT NULL,
                    text TEXT NOT NULL,
                    word_count INTEGER NOT NULL DEFAULT 0,
                    token_count INTEGER NOT NULL DEFAULT 0,
                    embedding_json TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY(document_id) REFERENCES documents(document_id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id TEXT NOT NULL UNIQUE,
                    session_id TEXT NOT NULL,
                    document_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    metadata_json TEXT NOT NULL DEFAULT '{}'
                );

                CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
                    ON document_chunks(document_id);

                CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id
                    ON chat_messages(session_id);

                CREATE INDEX IF NOT EXISTS idx_chat_messages_document_id
                    ON chat_messages(document_id);
                """
            )

    async def initialize(self) -> None:
        await asyncio.to_thread(self._initialize_sync)

    def _serialize(self, value: Any) -> str:
        return json.dumps(value, default=str)

    def _deserialize(self, value: Optional[str], default: Any) -> Any:
        if not value:
            return default
        try:
            return json.loads(value)
        except json.JSONDecodeError:
            return default

    def upsert_document_sync(self, document: Dict[str, Any]) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                INSERT INTO documents (
                    document_id, user_id, file_name, file_type, file_size,
                    download_url, status, uploaded_at, processed_at, page_count,
                    word_count, chunk_count, error_message, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(document_id) DO UPDATE SET
                    user_id=excluded.user_id,
                    file_name=excluded.file_name,
                    file_type=excluded.file_type,
                    file_size=excluded.file_size,
                    download_url=excluded.download_url,
                    status=excluded.status,
                    uploaded_at=excluded.uploaded_at,
                    processed_at=excluded.processed_at,
                    page_count=excluded.page_count,
                    word_count=excluded.word_count,
                    chunk_count=excluded.chunk_count,
                    error_message=excluded.error_message,
                    metadata_json=excluded.metadata_json
                """,
                (
                    document["document_id"],
                    document["user_id"],
                    document["file_name"],
                    document["file_type"],
                    document.get("file_size", 0),
                    document["download_url"],
                    document["status"],
                    document["uploaded_at"],
                    document.get("processed_at"),
                    document.get("page_count"),
                    document.get("word_count"),
                    document.get("chunk_count"),
                    document.get("error_message"),
                    self._serialize(document.get("metadata", {})),
                ),
            )

    async def upsert_document(self, document: Dict[str, Any]) -> None:
        await asyncio.to_thread(self.upsert_document_sync, document)

    def update_document_status_sync(
        self,
        document_id: str,
        status: str,
        error_message: Optional[str] = None,
        page_count: Optional[int] = None,
        word_count: Optional[int] = None,
        chunk_count: Optional[int] = None,
        processed_at: Optional[str] = None,
    ) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                UPDATE documents
                SET status = ?,
                    error_message = ?,
                    page_count = COALESCE(?, page_count),
                    word_count = COALESCE(?, word_count),
                    chunk_count = COALESCE(?, chunk_count),
                    processed_at = COALESCE(?, processed_at)
                WHERE document_id = ?
                """,
                (status, error_message, page_count, word_count, chunk_count, processed_at, document_id),
            )

    async def update_document_status(
        self,
        document_id: str,
        status: str,
        error_message: Optional[str] = None,
        page_count: Optional[int] = None,
        word_count: Optional[int] = None,
        chunk_count: Optional[int] = None,
        processed_at: Optional[str] = None,
    ) -> None:
        await asyncio.to_thread(
            self.update_document_status_sync,
            document_id,
            status,
            error_message,
            page_count,
            word_count,
            chunk_count,
            processed_at,
        )

    def get_document_sync(self, document_id: str) -> Optional[Dict[str, Any]]:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT * FROM documents WHERE document_id = ?",
                (document_id,),
            ).fetchone()

            if row is None:
                return None

            metadata = self._deserialize(row["metadata_json"], {})
            return {
                "document_id": row["document_id"],
                "user_id": row["user_id"],
                "file_name": row["file_name"],
                "file_type": row["file_type"],
                "file_size": row["file_size"],
                "download_url": row["download_url"],
                "status": row["status"],
                "uploaded_at": row["uploaded_at"],
                "processed_at": row["processed_at"],
                "page_count": row["page_count"],
                "word_count": row["word_count"],
                "chunk_count": row["chunk_count"],
                "error_message": row["error_message"],
                "metadata": metadata,
            }

    async def get_document(self, document_id: str) -> Optional[Dict[str, Any]]:
        return await asyncio.to_thread(self.get_document_sync, document_id)

    def delete_document_sync(self, document_id: str) -> None:
        with self._connect() as connection:
            connection.execute("DELETE FROM document_chunks WHERE document_id = ?", (document_id,))
            connection.execute("DELETE FROM chat_messages WHERE document_id = ?", (document_id,))
            connection.execute("DELETE FROM documents WHERE document_id = ?", (document_id,))

    async def delete_document(self, document_id: str) -> None:
        await asyncio.to_thread(self.delete_document_sync, document_id)

    def add_chat_message_sync(self, message: Dict[str, Any]) -> None:
        with self._connect() as connection:
            connection.execute(
                """
                INSERT OR REPLACE INTO chat_messages (
                    message_id, session_id, document_id, user_id,
                    content, message_type, timestamp, metadata_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    message["message_id"],
                    message["session_id"],
                    message["document_id"],
                    message["user_id"],
                    message["content"],
                    message["message_type"],
                    message["timestamp"],
                    self._serialize(message.get("metadata", {})),
                ),
            )

    async def add_chat_message(self, message: Dict[str, Any]) -> None:
        await asyncio.to_thread(self.add_chat_message_sync, message)

    def list_chat_messages_sync(self, document_id: str) -> List[Dict[str, Any]]:
        with self._connect() as connection:
            rows = connection.execute(
                "SELECT * FROM chat_messages WHERE document_id = ? ORDER BY timestamp ASC",
                (document_id,),
            ).fetchall()

        messages: List[Dict[str, Any]] = []
        for row in rows:
            messages.append(
                {
                    "message_id": row["message_id"],
                    "session_id": row["session_id"],
                    "document_id": row["document_id"],
                    "user_id": row["user_id"],
                    "content": row["content"],
                    "message_type": row["message_type"],
                    "timestamp": row["timestamp"],
                    "metadata": self._deserialize(row["metadata_json"], {}),
                }
            )
        return messages

    async def list_chat_messages(self, document_id: str) -> List[Dict[str, Any]]:
        return await asyncio.to_thread(self.list_chat_messages_sync, document_id)

    def add_chunks_sync(self, document_id: str, chunks: List[Dict[str, Any]]) -> None:
        with self._connect() as connection:
            connection.execute("DELETE FROM document_chunks WHERE document_id = ?", (document_id,))
            for chunk in chunks:
                connection.execute(
                    """
                    INSERT INTO document_chunks (
                        document_id, chunk_id, chunk_index, text,
                        word_count, token_count, embedding_json, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        document_id,
                        chunk["chunk_id"],
                        chunk["chunk_index"],
                        chunk["text"],
                        chunk.get("word_count", 0),
                        chunk.get("token_count", 0),
                        self._serialize(chunk.get("embedding", [])),
                        chunk.get("created_at") or datetime.utcnow().isoformat(),
                    ),
                )

    async def add_chunks(self, document_id: str, chunks: List[Dict[str, Any]]) -> None:
        await asyncio.to_thread(self.add_chunks_sync, document_id, chunks)

    def get_chunks_sync(self, document_id: str) -> List[Dict[str, Any]]:
        with self._connect() as connection:
            rows = connection.execute(
                "SELECT * FROM document_chunks WHERE document_id = ? ORDER BY chunk_index ASC",
                (document_id,),
            ).fetchall()

        chunks: List[Dict[str, Any]] = []
        for row in rows:
            chunks.append(
                {
                    "chunk_id": row["chunk_id"],
                    "chunk_index": row["chunk_index"],
                    "text": row["text"],
                    "word_count": row["word_count"],
                    "token_count": row["token_count"],
                    "embedding": self._deserialize(row["embedding_json"], []),
                    "created_at": row["created_at"],
                }
            )
        return chunks

    async def get_chunks(self, document_id: str) -> List[Dict[str, Any]]:
        return await asyncio.to_thread(self.get_chunks_sync, document_id)

    def ping_sync(self) -> bool:
        with self._connect() as connection:
            connection.execute("SELECT 1")
        return True

    async def ping(self) -> bool:
        return await asyncio.to_thread(self.ping_sync)


local_database = LocalDatabase()