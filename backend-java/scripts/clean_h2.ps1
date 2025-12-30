# Cleans H2 DB files created during development
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRootInfo = Resolve-Path (Join-Path $scriptDir '..') -ErrorAction SilentlyContinue
$projectRoot = if($projectRootInfo) { $projectRootInfo.Path } else { Join-Path $scriptDir '..' }

$files = @(
  "$projectRoot\data\tienda.mv.db",
  "$projectRoot\data\tienda.trace.db"
)

Write-Host "Cleaning H2 data files (if present):"
foreach($f in $files){
  if(Test-Path $f){
    Write-Host "Removing: $f"
    Remove-Item $f -Force
  } else { Write-Host "Not found: $f" }
}
Write-Host "Done."