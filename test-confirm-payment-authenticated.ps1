# Test script for confirm payment API with authentication
param(
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin123", 
    [int]$OrderId = 22
)

# Step 1: Login to get JWT token
$loginUri = "http://localhost:8080/auth/login"
$loginHeaders = @{
    "Content-Type" = "application/json"
}
$loginBody = @{
    "username" = $AdminUsername
    "password" = $AdminPassword
} | ConvertTo-Json

Write-Host "Step 1: Logging in as admin..." -ForegroundColor Yellow

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUri -Method POST -Headers $loginHeaders -Body $loginBody
    $token = $loginResponse.token
    Write-Host "✅ Login successful! Token obtained." -ForegroundColor Green
} catch {
    Write-Host "❌ Login failed: " -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $loginError = $reader.ReadToEnd()
        Write-Host "Login Error Response: $loginError"
    }
    exit 1
}

# Step 2: Call confirm payment API with token
Write-Host "Step 2: Confirming payment for Order ID: $OrderId" -ForegroundColor Yellow

$confirmUri = "http://localhost:8080/admin/api/orders/$OrderId/confirm-payment"
$confirmHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}
$confirmBody = @{
    "notes" = "Payment confirmed by admin via API test"
} | ConvertTo-Json

try {
    $confirmResponse = Invoke-RestMethod -Uri $confirmUri -Method POST -Headers $confirmHeaders -Body $confirmBody
    Write-Host "✅ Payment confirmation successful!" -ForegroundColor Green
    Write-Host ($confirmResponse | ConvertTo-Json -Depth 10)
} catch {
    Write-Host "❌ Payment confirmation failed: " -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "Error Response: $errorBody"
    }
}
