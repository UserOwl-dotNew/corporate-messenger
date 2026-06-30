# test-all.ps1
Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "  ТЕСТИРОВАНИЕ CORPORATE MESSENGER" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Проверка сервисов
Write-Host "`n[1] Проверка сервисов..." -ForegroundColor Yellow
$services = @{
    "API Gateway" = "http://localhost:8080/actuator/health"
    "Auth Service" = "http://localhost:8081/health"
    "Chat Service" = "http://localhost:8082/health"
    "Blockchain Adapter" = "http://localhost:8083/api/v1/blockchain/health"
    "Search Engine" = "http://localhost:8084/health"
}

$allOk = $true
foreach ($name in $services.Keys) {
    try {
        $response = Invoke-RestMethod -Uri $services[$name] -Method Get -TimeoutSec 3 -ErrorAction Stop
        Write-Host "  ✅ $name - OK" -ForegroundColor Green
    } catch {
        Write-Host "  ❌ $name - Failed" -ForegroundColor Red
        $allOk = $false
    }
}

if (-not $allOk) {
    Write-Host "`n⚠️ Некоторые сервисы не работают. Проверьте логи." -ForegroundColor Yellow
    exit
}

# 2. Регистрация
Write-Host "`n[2] Регистрация пользователя..." -ForegroundColor Yellow
$body = '{"email":"test@example.com","password":"password123","username":"testuser"}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "  ✅ Регистрация успешна" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  ⚠️ Пользователь уже существует" -ForegroundColor Yellow
    } else {
        Write-Host "  ❌ Ошибка регистрации" -ForegroundColor Red
        exit
    }
}

# 3. Логин
Write-Host "`n[3] Логин..." -ForegroundColor Yellow
$body = '{"email":"test@example.com","password":"password123"}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    $token = $response.token
    $userId = $response.userId
    Write-Host "  ✅ Логин успешен" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 30))..." -ForegroundColor Gray
} catch {
    Write-Host "  ❌ Ошибка логина: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# 4. Создание чата
Write-Host "`n[4] Создание чата..." -ForegroundColor Yellow
$headers = @{"Authorization" = "Bearer $token"}
$body = '{"name":"Test Chat","type":"GROUP","participantIds":["' + $userId + '"]}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/chats" -Method Post -Body $body -Headers $headers -ContentType "application/json" -ErrorAction Stop
    $chatId = $response.id
    Write-Host "  ✅ Чат создан: $chatId" -ForegroundColor Green
} catch {
    Write-Host "  ❌ Ошибка создания чата: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Проверка блокчейн адаптера
Write-Host "`n[5] Проверка блокчейн адаптера..." -ForegroundColor Yellow
try {
    $stats = Invoke-RestMethod -Uri "http://localhost:8083/api/v1/blockchain/stats" -Method Get -ErrorAction Stop
    Write-Host "  ✅ Блокчейн адаптер работает" -ForegroundColor Green
    Write-Host "  Зарегистрировано хэшей: $($stats.total_registered)" -ForegroundColor Gray
} catch {
    Write-Host "  ❌ Ошибка: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "  ТЕСТ ЗАВЕРШЕН!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "`n📝 Данные для WebSocket:" -ForegroundColor Yellow
Write-Host "  Chat ID: $chatId" -ForegroundColor Cyan
Write-Host "  JWT Token: $token" -ForegroundColor Cyan
Write-Host "`nОткройте websocket-test.html и используйте эти данные" -ForegroundColor Gray