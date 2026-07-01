import asyncio
import json
import logging
from uuid import UUID
from typing import Optional
from kafka import KafkaConsumer, KafkaProducer
from tenacity import retry, stop_after_attempt, wait_exponential

from app.config import settings
from app.models import KafkaMessageEvent
from app.service import service

logger = logging.getLogger(__name__)


def json_serializer(obj):
    """Сериализация объектов для JSON"""
    if isinstance(obj, UUID):
        return str(obj)
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")

class KafkaMessageConsumer:
    """Kafka Consumer для чтения сообщений (синхронный)"""

    def __init__(self):
        self.consumer: Optional[KafkaConsumer] = None
        self.producer: Optional[KafkaProducer] = None
        self.running = False

    async def start(self):
        """Запуск Kafka Consumer"""
        try:
            # Создаем consumer
            self.consumer = KafkaConsumer(
                settings.KAFKA_TOPIC_MESSAGES,
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                group_id=settings.KAFKA_CONSUMER_GROUP,
                auto_offset_reset='earliest',
                enable_auto_commit=False,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                max_poll_records=settings.BATCH_SIZE
            )

            # Создаем producer для DLQ
            self.producer = KafkaProducer(
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )

            self.running = True
            logger.info(f"✅ Kafka consumer started on topic: {settings.KAFKA_TOPIC_MESSAGES}")
            logger.info(f"✅ Kafka producer started for DLQ: {settings.KAFKA_TOPIC_DLQ}")

        except Exception as e:
            logger.error(f"❌ Failed to start Kafka consumer: {e}")
            raise

    @retry(
        stop=stop_after_attempt(settings.MAX_RETRIES),
        wait=wait_exponential(multiplier=1, min=2, max=10)
    )
    async def process_message_with_retry(self, message: KafkaMessageEvent) -> bool:
        """Обработка сообщения с повторными попытками"""
        return await service.process_message(message)

    async def send_to_dlq(self, message: KafkaMessageEvent, error: str):
        """Отправка сообщения в DLQ"""
        try:
            dlq_message = {
                "original_message": message.dict(),
                "error": error,
                "timestamp": int(asyncio.get_event_loop().time())
            }

            # Используем json.dumps с кастомным сериализатором
            self.producer.send(
                settings.KAFKA_TOPIC_DLQ,
                value=dlq_message
            )
            self.producer.flush()
            logger.info(f"📨 Message {message.messageId} sent to DLQ")
        except Exception as e:
            logger.error(f"❌ Failed to send message to DLQ: {e}")

    async def consume(self):
        """Основной цикл потребления сообщений"""
        logger.info("🔄 Starting Kafka consumer loop...")

        try:
            while self.running:
                # Получаем сообщения пачками
                messages = self.consumer.poll(timeout_ms=1000)

                for topic_partition, records in messages.items():
                    for msg in records:
                        if not self.running:
                            break

                        try:
                            # Парсим сообщение
                            event = KafkaMessageEvent(**msg.value)
                            logger.info(f"📨 Received message: {event.messageId}")

                            # Обрабатываем сообщение с ретраями
                            try:
                                success = await self.process_message_with_retry(event)

                                if success:
                                    # Коммитим офсет
                                    self.consumer.commit()
                                    logger.info(f"✅ Message {event.messageId} processed and committed")
                                else:
                                    # Отправляем в DLQ
                                    await self.send_to_dlq(event, "Processing failed after retries")
                                    self.consumer.commit()
                                    logger.warning(f"⚠️ Message {event.messageId} sent to DLQ and committed")

                            except Exception as e:
                                logger.error(f"❌ Error processing message {event.messageId}: {e}")
                                await self.send_to_dlq(event, str(e))
                                self.consumer.commit()

                        except json.JSONDecodeError as e:
                            logger.error(f"❌ Failed to parse message: {e}")
                            self.consumer.commit()
                        except Exception as e:
                            logger.error(f"❌ Unexpected error in consumer loop: {e}")
                            await asyncio.sleep(1)

                await asyncio.sleep(0.1)

        except asyncio.CancelledError:
            logger.info("Consumer loop cancelled")
        except Exception as e:
            logger.error(f"❌ Consumer loop error: {e}")
        finally:
            await self.stop()

    async def stop(self):
        """Остановка потребителя"""
        self.running = False
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer stopped")
        if self.producer:
            self.producer.close()
            logger.info("Kafka producer stopped")


# Глобальный экземпляр
consumer = KafkaMessageConsumer()