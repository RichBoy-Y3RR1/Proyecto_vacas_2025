# load_report_seed.ps1
# Helper script to import `report_seed.sql` into MySQL or H2. Edit connection params as needed.

param(
    [string]$dbType = 'mysql', # or 'h2'
    [string]$host = 'localhost',
    [int]$port = 3306,
    [string]$user = 'root',
    [string]$password = '',
    [string]$database = 'tienda'
)

$sqlFile = Join-Path $PSScriptRoot 'report_seed.sql'
if (-not (Test-Path $sqlFile)) { Write-Error "Missing $sqlFile"; exit 1 }

if ($dbType -eq 'mysql'){
    if ($password -ne ''){ $creds = "-p$password" } else { $creds = '' }
    $cmd = "mysql -h $host -P $port -u $user $creds $database < `"$sqlFile`""
    Write-Output "Running: $cmd"
    iex $cmd
} elseif ($dbType -eq 'h2'){
    Write-Output "H2 import: run using the H2 console or the H2 shell tool. Example (jdbc):"
    Write-Output "Run using the H2 shell: java -cp h2.jar org.h2.tools.RunScript -url \"jdbc:h2:~/tienda\" -user sa -script \"$sqlFile\""
} else { Write-Error "Unsupported dbType: $dbType"; exit 2 }
