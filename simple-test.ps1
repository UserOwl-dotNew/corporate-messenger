# Прямой запрос к Auth Service (минуя Gateway)
$body = '{"email":"test@example.com","password":"password123"}'
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/auth/login" -Method Post -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "✅ Direct login successful!" -ForegroundColor Green
    Write-Host "Token: $($response.token)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Direct login failed: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $reader.DiscardBufferedData()
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error: $errorBody" -ForegroundColor Red
    }
}