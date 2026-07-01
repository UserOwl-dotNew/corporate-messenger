import logging
from datetime import datetime  # <-- ДОБАВИТЬ ЭТУ СТРОКУ
from typing import Optional
from app.database import db

logger = logging.getLogger(__name__)


class BlockchainEmulator:
    """Эмуляция блокчейна для MVP"""

    @staticmethod
    async def register_hash(hash_value: str) -> Optional[str]:
        """
        Регистрация хэша в эмуляторе
        Возвращает transaction_id
        """
        try:
            tx_id = await db.register_hash_emulator(hash_value)
            return tx_id
        except Exception as e:
            logger.error(f"Error registering hash in emulator: {e}")
            return None

    @staticmethod
    async def verify_hash(hash_value: str) -> dict:
        """
        Проверка хэша в эмуляторе
        """
        try:
            return await db.verify_hash_emulator(hash_value)
        except Exception as e:
            logger.error(f"Error verifying hash in emulator: {e}")
            return {"is_registered": False, "error": str(e)}