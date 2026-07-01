# simple-test.ps1 - упрощенная версия
Write-Host "=== Simple Test ===" -ForegroundColor Green

# 1. Проверка health
Write-Host "`n1. Checking services..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/health" -ErrorAction Stop
    Write-Host "✅ Auth Service OK" -ForegroundColor Green
} catch {
    Write-Host "❌ Auth Service failed" -ForegroundColor Red
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/health" -ErrorAction Stop
    Write-Host "✅ Chat Service OK" -ForegroundColor Green
} catch {
    Write-Host "❌ Chat Service failed" -ForegroundColor Red
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/v1/blockchain/health" -ErrorAction Stop
    Write-Host "✅ Blockchain Adapter OK" -ForegroundColor Green
} catch {
    Write-Host "❌ Blockchain Adapter failed" -ForegroundColor Red
}

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8084/health" -ErrorAction Stop
    Write-Host "✅ Search Engine OK" -ForegroundColor Green
} catch {
    Write-Host "❌ Search Engine failed" -ForegroundColor Red
}

# 2. Регистрация
Write-Host "`n2. Registering user..." -ForegroundColor Yellow
$body = '{"email":"test@example.com","password":"password123","username":"testuser"}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "✅ Registration successful" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "⚠️ User already exists" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Registration failed" -ForegroundColor Red
    }
}

# 3. Логин
Write-Host "`n3. Logging in..." -ForegroundColor Yellow
$body = '{"email":"test@example.com","password":"password123"}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    $token = $response.token
    Write-Host "✅ Login successful" -ForegroundColor Green
    Write-Host "Token: $token" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Login failed" -ForegroundColor Red
    exit
}

# 4. Создание чата
Write-Host "`n4. Creating chat..." -ForegroundColor Yellow
$headers = @{"Authorization" = "Bearer $token"}
$body = '{"name":"Test Chat","type":"GROUP","participantIds":["11111111-1111-1111-1111-111111111111"]}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/chats" -Method Post -Body $body -Headers $headers -ContentType "application/json" -ErrorAction Stop
    $chatId = $response.id
    Write-Host "✅ Chat created: $chatId" -ForegroundColor Green
} catch {
    Write-Host "❌ Chat creation failed" -ForegroundColor Red
    exit
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green
Write-Host "Chat ID: $chatId" -ForegroundColor Cyan
Write-Host "Token: $token" -ForegroundColor Cyan