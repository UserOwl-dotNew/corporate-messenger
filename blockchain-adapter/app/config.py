import os
from typing import Optional


class Settings:
    """Настройки сервиса. Все значения читаются из переменных окружения."""

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
    KAFKA_CONSUMER_GROUP: str = os.getenv("KAFKA_CONSUMER_GROUP", "blockchain-adapter-group")
    KAFKA_TOPIC_MESSAGES: str = os.getenv("KAFKA_TOPIC_MESSAGES", "chat-messages")
    KAFKA_TOPIC_DLQ: str = os.getenv("KAFKA_TOPIC_DLQ", "chat-messages-dlq")

    # PostgreSQL
    POSTGRES_HOST: str = os.getenv("POSTGRES_HOST", "postgres")
    POSTGRES_PORT: int = int(os.getenv("POSTGRES_PORT", "5432"))
    POSTGRES_USER: str = os.getenv("POSTGRES_USER", "admin")
    POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "secure_password_123")
    POSTGRES_DB: str = os.getenv("POSTGRES_DB", "messenger")

    # Redis
    REDIS_HOST: str = os.getenv("REDIS_HOST", "redis")
    REDIS_PORT: int = int(os.getenv("REDIS_PORT", "6379"))
    REDIS_PASSWORD: str = os.getenv("REDIS_PASSWORD", "")

    # Chat Service API
    CHAT_SERVICE_URL: str = os.getenv("CHAT_SERVICE_URL", "http://chat-service:8080")

    # Blockchain
    BLOCKCHAIN_TYPE: str = os.getenv("BLOCKCHAIN_TYPE", "emulator")
    BLOCKCHAIN_EMULATOR_ENABLED: bool = os.getenv("BLOCKCHAIN_EMULATOR_ENABLED", "true").lower() == "true"

    # Ethereum (опционально)
    ETHEREUM_RPC_URL: Optional[str] = os.getenv("ETHEREUM_RPC_URL")
    ETHEREUM_CONTRACT_ADDRESS: Optional[str] = os.getenv("ETHEREUM_CONTRACT_ADDRESS")
    ETHEREUM_PRIVATE_KEY: Optional[str] = os.getenv("ETHEREUM_PRIVATE_KEY")
    ETHEREUM_CHAIN_ID: int = int(os.getenv("ETHEREUM_CHAIN_ID", "11155111"))

    # Hyperledger (опционально)
    HYPERLEDGER_NETWORK_CONFIG: Optional[str] = os.getenv("HYPERLEDGER_NETWORK_CONFIG")
    HYPERLEDGER_CHANNEL: Optional[str] = os.getenv("HYPERLEDGER_CHANNEL")
    HYPERLEDGER_CONTRACT: Optional[str] = os.getenv("HYPERLEDGER_CONTRACT")

    # Processing
    BATCH_SIZE: int = int(os.getenv("BATCH_SIZE", "10"))
    MAX_RETRIES: int = int(os.getenv("MAX_RETRIES", "3"))
    RETRY_DELAY: int = int(os.getenv("RETRY_DELAY", "5"))

    @property
    def database_url(self) -> str:
        """PostgreSQL connection URL для asyncpg."""
        return f"postgresql://{self.POSTGRES_USER}:{self.POSTGRES_PASSWORD}@{self.POSTGRES_HOST}:{self.POSTGRES_PORT}/{self.POSTGRES_DB}"

    @property
    def redis_url(self) -> str:
        """Redis connection URL."""
        if self.REDIS_PASSWORD:
            return f"redis://:{self.REDIS_PASSWORD}@{self.REDIS_HOST}:{self.REDIS_PORT}"
        return f"redis://{self.REDIS_HOST}:{self.REDIS_PORT}"


settings = Settings()

# Для отладки - выводим настройки при запуске
if __name__ == "__main__":
    print(f"KAFKA_BOOTSTRAP_SERVERS: {settings.KAFKA_BOOTSTRAP_SERVERS}")
    print(f"POSTGRES_HOST: {settings.POSTGRES_HOST}")
    print(f"POSTGRES_DB: {settings.POSTGRES_DB}")
    print(f"REDIS_HOST: {settings.REDIS_HOST}")
    print(f"BLOCKCHAIN_TYPE: {settings.BLOCKCHAIN_TYPE}")
    print(f"DATABASE_URL: {settings.database_url}")