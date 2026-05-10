<#
.SYNOPSIS
    Starts the project manually without Docker.
.DESCRIPTION
    Opens three PowerShell windows and starts:
      - backend with Maven
      - frontend with npm
      - AI service with Python
    The backend uses the local Spring Boot application properties template if no application.properties exists.
.PARAMETER InstallDependencies
    Installs required dependencies before starting services.
.EXAMPLE
    .\start_all.ps1
    Starts all services in new PowerShell windows.
.EXAMPLE
    .\start_all.ps1 -InstallDependencies
    Installs dependencies first, then starts the services.
#>

param(
    [switch]$InstallDependencies
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root

$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'frontend'
$aiServiceDir = Join-Path $root 'ai-service'

if (-not (Test-Path $backendDir)) {
    Write-Error 'Backend directory not found.'
    exit 1
}
if (-not (Test-Path $frontendDir)) {
    Write-Error 'Frontend directory not found.'
    exit 1
}
if (-not (Test-Path $aiServiceDir)) {
    Write-Error 'AI service directory not found.'
    exit 1
}

# Create backend application.properties from template if needed.
$backendProps = Join-Path $backendDir 'src\main\resources\application.properties'
$backendPropsTemplate = Join-Path $backendDir 'src\main\resources\application.properties.template'
if (-not (Test-Path $backendProps) -and (Test-Path $backendPropsTemplate)) {
    Copy-Item $backendPropsTemplate $backendProps
    Write-Host 'Copied backend application.properties from template.'
}

$psExe = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell.exe' }

function Start-ServiceWindow($name, $directory, $command) {
    Write-Host "Starting $name..."
    $argList = @('-NoExit', '-Command', "Set-Location '$directory'; $command")
    Start-Process -FilePath $psExe -ArgumentList $argList
}

# Backend command
$backendCommand = 'mvn spring-boot:run'
if ($InstallDependencies) {
    $backendCommand = 'mvn dependency:resolve && mvn spring-boot:run'
}
Start-ServiceWindow 'Backend' $backendDir $backendCommand

# Frontend command
$npmCommand = 'npm start'
if ($InstallDependencies -or -not (Test-Path (Join-Path $frontendDir 'node_modules'))) {
    $npmCommand = 'npm install && npm start'
}
Start-ServiceWindow 'Frontend' $frontendDir $npmCommand

# AI service command
$venvDir = Join-Path $aiServiceDir '.venv'
$pythonCommand = @(
    'if (-not (Test-Path ".\.venv")) { python -m venv .\.venv }',
    '.\.venv\Scripts\Activate.ps1',
    'pip install -r requirements.txt',
    'python app.py'
) -join ' ; '
if (-not $InstallDependencies -and (Test-Path $venvDir)) {
    $pythonCommand = '.\.venv\Scripts\Activate.ps1 ; python app.py'
}
Start-ServiceWindow 'AI Service' $aiServiceDir $pythonCommand

Write-Host ''
Write-Host 'Manual startup launched.'
Write-Host 'Backend: http://localhost:8080'
Write-Host 'Frontend: http://localhost:3000'
Write-Host 'AI Service: http://localhost:5000'
Write-Host ''
Write-Host 'If any window fails to start, check that Java, Node.js, and Python are installed and available in PATH.'
