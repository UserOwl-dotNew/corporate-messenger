import asyncio
import logging
from typing import Optional
from uuid import UUID
import httpx

from app.config import settings
from app.database import db
from app.blockchain_emulator import BlockchainEmulator
from app.blockchain_ethereum import ethereum
from app.models import KafkaMessageEvent

logger = logging.getLogger(__name__)


class BlockchainService:
    """Сервис для работы с блокчейном"""

    def __init__(self):
        self.chat_service_url = settings.CHAT_SERVICE_URL

    async def process_message(self, event: KafkaMessageEvent) -> bool:
        """
        Обработка одного сообщения из Kafka
        """
        logger.info(f"Processing message {event.messageId} with hash {event.contentHash}")

        try:
            # 1. Выбираем блокчейн
            if settings.BLOCKCHAIN_TYPE == "ethereum" and settings.ETHEREUM_RPC_URL:
                blockchain = ethereum
            else:
                blockchain = BlockchainEmulator

            # 2. Регистрируем хэш в блокчейне
            tx_id = await blockchain.register_hash(event.contentHash)

            if not tx_id:
                logger.error(f"Failed to register hash for message {event.messageId}")
                return False

            # 3. Обновляем blockchain_tx_id в БД через API
            updated = await self.update_message_in_chat_service(
                str(event.messageId),
                tx_id
            )

            if updated:
                logger.info(f"✅ Message {event.messageId} processed successfully, tx_id: {tx_id}")
                return True
            else:
                logger.error(f"❌ Failed to update message {event.messageId} in chat service")
                return False

        except Exception as e:
            logger.error(f"Error processing message {event.messageId}: {e}")
            return False

    async def update_message_in_chat_service(
            self,
            message_id: str,
            blockchain_tx_id: str
    ) -> bool:
        """
        Обновление blockchain_tx_id через REST API chat-service
        """
        try:
            # Используем прямой запрос к БД для обновления
            # (так как chat-service может не иметь эндпоинта для обновления)
            updated = await db.update_message_blockchain_tx(
                message_id,
                blockchain_tx_id
            )

            if updated:
                return True

            # Альтернатива: через REST API (если добавить эндпоинт в chat-service)
            # async with httpx.AsyncClient() as client:
            #     response = await client.patch(
            #         f"{self.chat_service_url}/api/v1/messages/{message_id}/blockchain",
            #         json={"blockchain_tx_id": blockchain_tx_id},
            #         timeout=30.0
            #     )
            #     return response.status_code == 200

            return False

        except Exception as e:
            logger.error(f"Error updating message {message_id}: {e}")
            return False

    async def verify_hash(self, hash_value: str) -> dict:
        """
        Проверка хэша в блокчейне
        """
        try:
            if settings.BLOCKCHAIN_TYPE == "ethereum" and settings.ETHEREUM_RPC_URL:
                result = await ethereum.verify_hash(hash_value)
            else:
                result = await BlockchainEmulator.verify_hash(hash_value)

            return result
        except Exception as e:
            logger.error(f"Error verifying hash: {e}")
            return {"is_registered": False, "error": str(e)}

    async def health_check(self) -> dict:
        """
        Проверка здоровья сервиса
        """
        status = {
            "service": "blockchain-adapter",
            "blockchain_type": settings.BLOCKCHAIN_TYPE,
            "status": "healthy"
        }

        # Проверка Kafka
        # (будет добавлена в kafka_consumer)

        # Проверка БД
        try:
            async with db.pool.acquire() as conn:
                await conn.fetchval("SELECT 1")
            status["database"] = "connected"
        except Exception as e:
            status["database"] = f"disconnected: {e}"
            status["status"] = "degraded"

        return status


# Глобальный экземпляр
service = BlockchainService()