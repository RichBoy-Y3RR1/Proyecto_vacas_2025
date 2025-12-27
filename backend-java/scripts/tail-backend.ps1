$log1 = 'C:\Users\PC\tomcat11\logs\catalina.out'
$log2 = 'C:\Program Files\Apache Software Foundation\Tomcat 11.0\logs\catalina.out'
if (Test-Path $log1) {
    Write-Output "Tailing $log1"
    Get-Content $log1 -Tail 200 -Wait
} elseif (Test-Path $log2) {
    Write-Output "Tailing $log2"
    Get-Content $log2 -Tail 200 -Wait
} else {
    Write-Output 'No se encontró catalina.out; ejecutando comprobación HTTP periódica…'
    while ($true) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/tienda-backend-1.0.0/index.html' -TimeoutSec 5
            Write-Output ((Get-Date).ToString('s') + ' STATUS: ' + $r.StatusCode)
        } catch {
            Write-Output ((Get-Date).ToString('s') + ' ERROR: ' + $_.Exception.Message)
        }
        Start-Sleep -Seconds 5
    }
}
