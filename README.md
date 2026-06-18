# corporate-messenger

Корпоративный мессенджер с интеграцией блокчейна.

## Архитектура

- **Auth Service** (Java/Spring Boot) — аутентификация и авторизация
- **Chat Service** (Java/Spring Boot) — управление чатами и WebSocket
- **Blockchain Adapter** (Python/FastAPI) — интеграция с блокчейн-сетью
- **Search Engine** (Python/FastAPI) — полнотекстовый поиск

## Инфраструктура

- PostgreSQL — основная БД
- Elasticsearch — поисковый движок
- Redis — кэширование и WebSocket сессии
- Kafka + Zookeeper — асинхронная коммуникация

## Быстрый старт

### 1. Клонирование репозитория
```bash
git clone <repo-url>
cd blockchain-messenger