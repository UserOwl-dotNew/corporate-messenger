import asyncpg
import logging
from datetime import datetime
from typing import Optional
from app.config import settings

logger = logging.getLogger(__name__)


class DatabaseManager:
    def __init__(self):
        self.pool: Optional[asyncpg.Pool] = None

    async def connect(self):
        """Создание пула подключений к PostgreSQL"""
        try:
            self.pool = await asyncpg.create_pool(
                host=settings.POSTGRES_HOST,
                port=settings.POSTGRES_PORT,
                user=settings.POSTGRES_USER,
                password=settings.POSTGRES_PASSWORD,
                database=settings.POSTGRES_DB,
                min_size=2,
                max_size=10
            )
            logger.info("✅ PostgreSQL connection pool created")

            # Создаем таблицу для эмуляции блокчейна
            await self.create_emulator_table()

            return self.pool
        except Exception as e:
            logger.error(f"❌ PostgreSQL connection failed: {e}")
            raise

    async def create_emulator_table(self):
        """Создание таблицы для эмуляции блокчейна"""
        async with self.pool.acquire() as conn:
            await conn.execute("""
                CREATE TABLE IF NOT EXISTS blockchain_emulator (
                    hash VARCHAR(64) PRIMARY KEY,
                    registered_at TIMESTAMP DEFAULT NOW(),
                    registered_by VARCHAR(42) DEFAULT 'emulator'
                )
            """)
            logger.info("✅ Blockchain emulator table created/verified")

    async def register_hash_emulator(self, hash_value: str) -> str:
        """Регистрация хэша в эмуляторе блокчейна"""
        async with self.pool.acquire() as conn:
            # Проверяем, существует ли уже хэш
            existing = await conn.fetchval(
                "SELECT registered_at FROM blockchain_emulator WHERE hash = $1",
                hash_value
            )

            if existing:
                logger.info(f"Hash {hash_value} already registered at {existing}")
                tx_id = f"emulator-tx-{hash_value[:8]}"
                return tx_id

            # Регистрируем новый хэш
            await conn.execute(
                "INSERT INTO blockchain_emulator (hash, registered_by) VALUES ($1, 'emulator')",
                hash_value
            )

            tx_id = f"emulator-tx-{hash_value[:8]}-{int(datetime.now().timestamp())}"
            logger.info(f"✅ Hash {hash_value} registered with tx_id: {tx_id}")
            return tx_id

    async def verify_hash_emulator(self, hash_value: str) -> dict:
        """Проверка хэша в эмуляторе"""
        async with self.pool.acquire() as conn:
            row = await conn.fetchrow(
                "SELECT registered_at, registered_by FROM blockchain_emulator WHERE hash = $1",
                hash_value
            )

            if row:
                return {
                    "is_registered": True,
                    "registered_at": row["registered_at"],
                    "registered_by": row["registered_by"]
                }
            return {
                "is_registered": False,
                "registered_at": None,
                "registered_by": None
            }

    async def update_message_blockchain_tx(
            self,
            message_id: str,
            blockchain_tx_id: str
    ) -> bool:
        """Обновление blockchain_tx_id в таблице messages"""
        async with self.pool.acquire() as conn:
            result = await conn.execute(
                "UPDATE messages SET blockchain_tx_id = $1 WHERE id = $2",
                blockchain_tx_id,
                message_id
            )
            updated = result.split()[1]
            if int(updated) > 0:
                logger.info(f"✅ Updated message {message_id} with tx_id: {blockchain_tx_id}")
                return True
            else:
                logger.warning(f"❌ Message {message_id} not found or already updated")
                return False

    async def close(self):
        if self.pool:
            await self.pool.close()
            logger.info("PostgreSQL connection pool closed")


# Глобальный экземпляр
db = DatabaseManager()