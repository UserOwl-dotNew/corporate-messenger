from fastapi import FastAPI
import logging
import os
import redis
from kafka import KafkaProducer
import psycopg2
from contextlib import asynccontextmanager

# Настройка логирования в едином формате
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("🚀 Starting Blockchain Adapter Service")
    logger.info(f"✅ Redis: {os.getenv('REDIS_HOST', 'localhost')}:{os.getenv('REDIS_PORT', '6379')}")
    logger.info(f"✅ Kafka: {os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')}")
    logger.info(f"✅ PostgreSQL: {os.getenv('POSTGRES_HOST', 'localhost')}")

    # Проверка подключений
    try:
        # Redis
        r = redis.Redis(
            host=os.getenv('REDIS_HOST', 'localhost'),
            port=int(os.getenv('REDIS_PORT', 6379)),
            password=os.getenv('REDIS_PASSWORD', None),
            decode_responses=True
        )
        r.ping()
        logger.info("✅ Redis connection successful")
    except Exception as e:
        logger.error(f"❌ Redis connection failed: {e}")

    try:
        # PostgreSQL
        conn = psycopg2.connect(
            host=os.getenv('POSTGRES_HOST', 'localhost'),
            port=os.getenv('POSTGRES_PORT', '5432'),
            user=os.getenv('POSTGRES_USER', 'admin'),
            password=os.getenv('POSTGRES_PASSWORD', ''),
            database=os.getenv('POSTGRES_DB', 'messenger')
        )
        conn.close()
        logger.info("✅ PostgreSQL connection successful")
    except Exception as e:
        logger.error(f"❌ PostgreSQL connection failed: {e}")

    try:
        # Kafka
        producer = KafkaProducer(
            bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
        )
        producer.close()
        logger.info("✅ Kafka connection successful")
    except Exception as e:
        logger.error(f"❌ Kafka connection failed: {e}")

    yield

    # Shutdown
    logger.info("🛑 Shutting down Blockchain Adapter Service")

app = FastAPI(
    title="Blockchain Adapter",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "blockchain-adapter"}

@app.get("/")
async def root():
    return {
        "service": "Blockchain Adapter",
        "status": "running",
        "version": "1.0.0"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)