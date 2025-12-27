<#
deploy_to_tomcat.ps1

Uso:
  .\deploy_to_tomcat.ps1 -TomcatHome "C:\path\to\tomcat"

Lo que hace:
  - Ejecuta `mvn -DskipTests package` en el directorio del proyecto
  - Verifica que exista el WAR en `target/` (nombre: backend.war o backend.war generado por Maven)
  - Copia el WAR a Tomcat `webapps` y (opcional) reinicia Tomcat vía scripts `bin\shutdown.bat` y `bin\startup.bat`.

Requisitos:
  - Maven en PATH
  - Permisos suficientes para reiniciar Tomcat o escribir en `webapps`
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$TomcatHome
)

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Write-Host "Proyecto: $projectDir"

if (-not (Test-Path $TomcatHome)) {
    Write-Error "TomcatHome no existe: $TomcatHome"
    exit 1
}

# Build
Write-Host "Ejecutando: mvn -DskipTests package"
Push-Location $projectDir
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    Write-Error "Maven no está en PATH. Instala Maven o añade mvn al PATH."
    Pop-Location
    exit 1
}

$mvnArgs = "-DskipTests package"
$m = Start-Process -FilePath mvn -ArgumentList $mvnArgs -NoNewWindow -Wait -PassThru
if ($m.ExitCode -ne 0) {
    Write-Error "Maven falló con código $($m.ExitCode). Revisa la salida."
    Pop-Location
    exit $m.ExitCode
}

Pop-Location

# Localiza el WAR (maven finalName -> backend.war ó any *.war)
$targetDir = Join-Path $projectDir "target"
$war = Get-ChildItem -Path $targetDir -Filter "*.war" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $war) {
    Write-Error "No se encontró archivo .war en $targetDir. Asegúrate de que maven generó el WAR."
    exit 1
}

Write-Host "WAR encontrado: $($war.FullName)"

$webapps = Join-Path $TomcatHome "webapps"
if (-not (Test-Path $webapps)) {
    Write-Error "No existe la carpeta webapps en $TomcatHome"
    exit 1
}

$destWar = Join-Path $webapps $war.Name

# Copiar WAR
Write-Host "Copiando $($war.FullName) -> $destWar"
Copy-Item -Path $war.FullName -Destination $destWar -Force

# Intentar reiniciar Tomcat usando scripts (si existen)
$shutdown = Join-Path $TomcatHome "bin\shutdown.bat"
$startup = Join-Path $TomcatHome "bin\startup.bat"
if (Test-Path $shutdown -and Test-Path $startup) {
    Write-Host "Reiniciando Tomcat vía scripts..."
    & $shutdown
    Start-Sleep -Seconds 2
    & $startup
    Write-Host "Tomcat reiniciado."
} else {
    Write-Warning "No se encontraron scripts de control en 'bin'. Reinicia Tomcat manualmente si es necesario."
}

Write-Host "Despliegue finalizado. Accede a: http://localhost:8080/$([System.Uri]::EscapeDataString(($war.BaseName))) /api/..."