"""
Security utilities for authentication and authorization
"""

import jwt
from datetime import datetime, timedelta
from fastapi import HTTPException, Depends, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

security = HTTPBearer()

def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Verify JWT token from Firebase"""
    try:
        token = credentials.credentials
        
        # In production, you should verify Firebase tokens
        # For now, we'll decode the token without verification for testing
        # TODO: Implement proper Firebase token verification
        
        decoded = jwt.decode(token, options={"verify_signature": False})
        
        # Check if token is expired
        exp = decoded.get('exp')
        if exp and datetime.fromtimestamp(exp) < datetime.utcnow():
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token expired"
            )
        
        return decoded
        
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token expired"
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token"
        )
    except Exception as e:
        logger.error(f"Token verification error: {e}")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token verification failed"
        )

def get_user_id(credentials: HTTPAuthorizationCredentials = Depends(security)):
    """Get user ID from token"""
    decoded = verify_token(credentials)
    user_id = decoded.get('user_id') or decoded.get('uid')
    
    if not user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User ID not found in token"
        )
    
    return user_id

def create_access_token(data: dict, expires_delta: timedelta = None):
    """Create access token (for testing purposes)"""
    to_encode = data.copy()
    
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(hours=24)
    
    to_encode.update({"exp": expire})
    
    # In production, use proper signing
    encoded_jwt = jwt.encode(to_encode, settings.secret_key, algorithm="HS256")
    return encoded_jwt
