import logging
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.database import db
from app.kafka_consumer import consumer
from app.api.routes import router
from app.blockchain_ethereum import ethereum

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Глобальная задача для Kafka consumer
consumer_task = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Управление жизненным циклом приложения"""
    global consumer_task

    # Startup
    logger.info("🚀 Starting Blockchain Adapter Service")
    logger.info(f"✅ Blockchain type: {settings.BLOCKCHAIN_TYPE}")
    logger.info(f"✅ Kafka: {settings.KAFKA_BOOTSTRAP_SERVERS}")
    logger.info(f"✅ PostgreSQL: {settings.POSTGRES_HOST}:{settings.POSTGRES_PORT}")

    # Подключаемся к БД
    await db.connect()

    # Инициализируем Ethereum (если включен)
    if settings.BLOCKCHAIN_TYPE == "ethereum":
        await ethereum.initialize()

    # Запускаем Kafka consumer
    await consumer.start()

    # Запускаем consumer в фоновом режиме
    consumer_task = asyncio.create_task(consumer.consume())

    logger.info("✅ Blockchain Adapter Service started successfully")

    yield

    # Shutdown
    logger.info("🛑 Shutting down Blockchain Adapter Service...")

    # Останавливаем Kafka consumer
    if consumer_task:
        consumer_task.cancel()
        try:
            await consumer_task
        except asyncio.CancelledError:
            pass

    await consumer.stop()
    await db.close()
    logger.info("✅ Blockchain Adapter Service stopped")


# Создаем приложение
app = FastAPI(
    title="Blockchain Adapter",
    description="Блокчейн адаптер для мессенджера",
    version="1.0.0",
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Подключаем роуты
app.include_router(router)


# Health check на корневом пути
@app.get("/")
async def root():
    return {
        "service": "Blockchain Adapter",
        "version": "1.0.0",
        "status": "running",
        "blockchain_type": settings.BLOCKCHAIN_TYPE
    }


@app.get("/health")
async def health():
    return await service.health_check()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=False
    )