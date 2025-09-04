# Test script for confirm payment API
$uri = "http://localhost:8080/admin/api/orders/22/confirm-payment"
$headers = @{
    "Content-Type" = "application/json"
}
$body = @{
    "notes" = "Payment confirmed by admin"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -Headers $headers -Body $body
    Write-Host "Success: " -ForegroundColor Green
    Write-Host ($response | ConvertTo-Json -Depth 10)
} catch {
    Write-Host "Error: " -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody"
    }
}
