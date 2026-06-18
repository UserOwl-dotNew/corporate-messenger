from fastapi import FastAPI
import logging
import os
from elasticsearch import Elasticsearch
import psycopg2
from kafka import KafkaConsumer
import threading
from contextlib import asynccontextmanager

# Единый формат логов
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def start_kafka_consumer():
    """Фоновый поток для чтения из Kafka"""
    try:
        consumer = KafkaConsumer(
            'search-index',
            bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092'),
            auto_offset_reset='earliest',
            enable_auto_commit=True,
            group_id='search-engine-group'
        )
        logger.info("✅ Kafka consumer started")
        for message in consumer:
            logger.info(f"📨 Received message: {message.value[:100]}...")
    except Exception as e:
        logger.error(f"❌ Kafka consumer error: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("🚀 Starting Search Engine Service")
    logger.info(f"✅ Elasticsearch: {os.getenv('ELASTICSEARCH_HOST', 'localhost')}:{os.getenv('ELASTICSEARCH_PORT', '9200')}")

    # Проверка Elasticsearch
    try:
        es = Elasticsearch(
            [f"http://{os.getenv('ELASTICSEARCH_HOST', 'localhost')}:{os.getenv('ELASTICSEARCH_PORT', '9200')}"]
        )
        if es.ping():
            logger.info("✅ Elasticsearch connection successful")
        else:
            logger.error("❌ Elasticsearch connection failed")
    except Exception as e:
        logger.error(f"❌ Elasticsearch connection error: {e}")

    # Запуск Kafka consumer в фоновом потоке
    consumer_thread = threading.Thread(target=start_kafka_consumer, daemon=True)
    consumer_thread.start()

    yield

    # Shutdown
    logger.info("🛑 Shutting down Search Engine Service")

app = FastAPI(
    title="Search Engine",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "search-engine"}

@app.get("/")
async def root():
    return {
        "service": "Search Engine",
        "status": "running",
        "version": "1.0.0"
    }