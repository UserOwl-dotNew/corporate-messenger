#!/bin/bash

echo "=== Testing Full Integration ==="

# 1. Регистрация пользователя
echo "1. Register user..."
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser"
  }')
echo "Register response: $REGISTER_RESPONSE"

# 2. Логин
echo -e "\n2. Login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')
echo "Login response: $LOGIN_RESPONSE"

# Извлекаем токен
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.token')
if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "Failed to get token. Using test token..."
    TOKEN="test-token-123"
fi
echo "Token: $TOKEN"

# 3. Создание чата
echo -e "\n3. Create chat..."
CHAT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/chats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "Test Chat",
    "type": "GROUP",
    "participantIds": ["11111111-1111-1111-1111-111111111111"]
  }')
echo "Chat response: $CHAT_RESPONSE"

CHAT_ID=$(echo $CHAT_RESPONSE | jq -r '.id')
echo "Chat ID: $CHAT_ID"

# 4. Получение списка чатов
echo -e "\n4. Get chats..."
curl -s -X GET http://localhost:8080/api/v1/chats/me \
  -H "Authorization: Bearer $TOKEN" | jq '.'

# 5. Проверка блокчейн адаптера
echo -e "\n5. Check blockchain adapter..."
curl -s http://localhost:8083/api/v1/blockchain/stats | jq '.'

# 6. Проверка поиска
echo -e "\n6. Check search engine..."
curl -s http://localhost:8084/health | jq '.'

# 7. Проверка всех сервисов через gateway
echo -e "\n7. Check all services..."
echo "Auth: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/health)"
echo "Chat: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/v1/chats/me -H 'Authorization: Bearer $TOKEN')"
echo "Blockchain: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/v1/blockchain/health)"
echo "Search: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/v1/search/health)"

echo -e "\n=== Test completed ==="