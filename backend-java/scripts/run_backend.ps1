# Build and run backend with all runtime dependencies on classpath
# Usage: Open PowerShell in this folder and run: .\run_backend.ps1

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $here\..\

Write-Host "Cleaning H2 files..."
if(Test-Path "scripts\clean_h2.ps1"){ & "scripts\clean_h2.ps1" }

Write-Host "Running mvn clean package and copying dependencies..."
$mvn = "mvn -DskipTests clean package dependency:copy-dependencies -DoutputDirectory=target/dependency"
Write-Host $mvn
Invoke-Expression $mvn

# ensure dependency dir exists
if(-Not (Test-Path "target\dependency")){
  Write-Host "Dependency directory not found. Check Maven output. Aborting run."; Pop-Location; exit 1
}

Write-Host "Starting backend with dependency classpath..."
$cp = "target/classes;target/dependency/*"
Write-Host "java -cp $cp com.example.backend.LauncherMain 8080"
# invoke java directly with arguments to avoid quoting issues in PowerShell
& java -cp $cp com.example.backend.LauncherMain 8080

Pop-Location
