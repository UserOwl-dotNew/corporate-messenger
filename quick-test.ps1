# Смотрим конфиг Gateway
docker exec messenger-api-gateway cat /app/application.yml 2>/dev/null || echo "File not found"

# Проверяем, что Gateway видит другие сервисы
docker exec messenger-api-gateway sh -c "ping -c 1 auth-service 2>&1 || echo 'ping failed'"
docker exec messenger-api-gateway sh -c "nc -zv auth-service 8080 2>&1 || echo 'Connection failed'"