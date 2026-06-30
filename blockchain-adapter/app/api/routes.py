from fastapi import APIRouter, HTTPException
from typing import Dict, Any
from datetime import datetime

from app.models import BlockchainVerification
from app.service import service
from app.blockchain_emulator import BlockchainEmulator
from app.database import db

router = APIRouter(prefix="/api/v1/blockchain", tags=["blockchain"])


@router.post("/register")
async def register_hash(hash_value: str) -> Dict[str, Any]:
    """
    Ручная регистрация хэша в блокчейне
    """
    try:
        tx_id = await BlockchainEmulator.register_hash(hash_value)

        if tx_id:
            return {
                "status": "success",
                "hash": hash_value,
                "transaction_id": tx_id,
                "timestamp": datetime.now().isoformat()
            }
        else:
            raise HTTPException(status_code=400, detail="Failed to register hash")

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/verify/{hash_value}")
async def verify_hash(hash_value: str) -> BlockchainVerification:
    """
    Проверка хэша в блокчейне
    """
    try:
        result = await service.verify_hash(hash_value)

        return BlockchainVerification(
            hash=hash_value,
            is_registered=result.get("is_registered", False),
            registered_at=result.get("registered_at"),
            registered_by=result.get("registered_by")
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check() -> Dict[str, Any]:
    """
    Проверка статуса сервиса
    """
    try:
        # Проверка БД
        async with db.pool.acquire() as conn:
            await conn.fetchval("SELECT 1")
            db_status = "connected"
    except Exception as e:
        db_status = f"disconnected: {e}"

    return {
        "service": "blockchain-adapter",
        "status": "healthy" if db_status == "connected" else "degraded",
        "blockchain_type": "emulator",
        "database": db_status,
        "timestamp": datetime.now().isoformat()
    }


@router.get("/stats")
async def get_stats() -> Dict[str, Any]:
    """
    Статистика зарегистрированных хэшей
    """
    try:
        async with db.pool.acquire() as conn:
            count = await conn.fetchval("SELECT COUNT(*) FROM blockchain_emulator")

            recent = await conn.fetch(
                "SELECT hash, registered_at FROM blockchain_emulator "
                "ORDER BY registered_at DESC LIMIT 5"
            )

        return {
            "total_registered": count,
            "recent": [
                {
                    "hash": row["hash"],
                    "registered_at": row["registered_at"].isoformat()
                }
                for row in recent
            ],
            "blockchain_type": "emulator"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))