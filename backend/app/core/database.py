"""
Database initialization and connection management
"""

import asyncio
import firebase_admin
from firebase_admin import credentials, firestore, storage
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

# Global Firebase instances
firestore_client = None
storage_client = None

async def init_database():
    """Initialize Firebase connections"""
    global firestore_client, storage_client
    
    try:
        # Initialize Firebase Admin SDK
        if not firebase_admin._apps:
            cred = credentials.Certificate({
                "type": "service_account",
                "project_id": settings.firebase_project_id,
                "private_key_id": settings.firebase_private_key_id,
                "private_key": settings.firebase_private_key,
                "client_email": settings.firebase_client_email,
                "client_id": settings.firebase_client_id,
                "auth_uri": settings.firebase_auth_uri,
                "token_uri": settings.firebase_token_uri,
            })
            
            firebase_admin.initialize_app(cred)
        
        # Get Firestore and Storage clients
        firestore_client = firestore.client()
        storage_client = storage.bucket()
        
        logger.info("Firebase connections initialized successfully")
        
        # Test connections
        await test_connections()
        
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")
        raise

async def test_connections():
    """Test database connections"""
    global firestore_client, storage_client
    
    try:
        # Test Firestore
        if firestore_client:
            test_doc = firestore_client.collection('test').document('connection')
            test_doc.set({'test': True})
            test_doc.delete()
            logger.info("Firestore connection test passed")
        
        # Test Storage
        if storage_client:
            blobs = list(storage_client.list_blobs(max_results=1))
            logger.info("Storage connection test passed")
            
    except Exception as e:
        logger.error(f"Database connection test failed: {e}")
        raise

def get_firestore_client():
    """Get Firestore client"""
    global firestore_client
    return firestore_client

def get_storage_client():
    """Get Storage client"""
    global storage_client
    return storage_client

async def close_database():
    """Close database connections"""
    global firestore_client, storage_client
    
    try:
        # Firebase doesn't require explicit closing
        firestore_client = None
        storage_client = None
        logger.info("Database connections closed")
    except Exception as e:
        logger.error(f"Error closing database: {e}")
