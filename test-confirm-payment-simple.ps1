# Simple test script for confirm payment API (no authentication needed)
param(
    [int]$OrderId = 22
)

$uri = "http://localhost:8080/test/orders/$OrderId/confirm-payment"
$headers = @{
    "Content-Type" = "application/json"
}
$body = @{
    "notes" = "Test payment confirmation to check transaction rollback fix"
} | ConvertTo-Json

Write-Host "Testing payment confirmation for Order ID: $OrderId" -ForegroundColor Yellow
Write-Host "URL: $uri" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -Headers $headers -Body $body
    Write-Host "✅ Success!" -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 10)
} catch {
    Write-Host "❌ Error occurred:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody" -ForegroundColor Yellow
    }
}
