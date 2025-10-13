# Script de PowerShell para iniciar la aplicación con Docker Compose
# Uso: .\start-docker.ps1 [-Build] [-Down] [-Logs] [-Help]

param(
    [switch]$Build,
    [switch]$Down,
    [switch]$Logs,
    [switch]$Help
)

# Colores para output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"

# Función para mostrar ayuda
function Show-Help {
    Write-Host "Script de Docker para Chatbot IA Backend" -ForegroundColor $Blue
    Write-Host ""
    Write-Host "Uso: .\start-docker.ps1 [OPCIONES]"
    Write-Host ""
    Write-Host "Opciones:"
    Write-Host "  -Build     Reconstruir las imágenes antes de iniciar"
    Write-Host "  -Down      Detener y eliminar los contenedores"
    Write-Host "  -Logs      Mostrar logs de los servicios"
    Write-Host "  -Help      Mostrar esta ayuda"
    Write-Host ""
    Write-Host "Ejemplos:"
    Write-Host "  .\start-docker.ps1                    # Iniciar servicios"
    Write-Host "  .\start-docker.ps1 -Build           # Reconstruir e iniciar"
    Write-Host "  .\start-docker.ps1 -Down             # Detener servicios"
    Write-Host "  .\start-docker.ps1 -Logs            # Ver logs"
}

# Función para verificar dependencias
function Test-Dependencies {
    Write-Host "Verificando dependencias..." -ForegroundColor $Blue
    
    try {
        $dockerVersion = docker --version
        Write-Host "✓ Docker está disponible: $dockerVersion" -ForegroundColor $Green
    }
    catch {
        Write-Host "Error: Docker no está instalado" -ForegroundColor $Red
        exit 1
    }
    
    try {
        $composeVersion = docker-compose --version
        Write-Host "✓ Docker Compose está disponible: $composeVersion" -ForegroundColor $Green
    }
    catch {
        Write-Host "Error: Docker Compose no está instalado" -ForegroundColor $Red
        exit 1
    }
}

# Función para verificar archivo de entorno
function Test-EnvFile {
    if (-not (Test-Path "env.docker")) {
        Write-Host "Advertencia: No se encontró el archivo env.docker" -ForegroundColor $Yellow
        Write-Host "Creando archivo de ejemplo..." -ForegroundColor $Yellow
        if (Test-Path "env.template") {
            Copy-Item "env.template" "env.docker"
        } else {
            Write-Host "No se pudo crear env.docker" -ForegroundColor $Red
        }
    }
}

# Función para iniciar servicios
function Start-Services {
    Write-Host "Iniciando servicios..." -ForegroundColor $Blue
    
    if ($Build) {
        Write-Host "Reconstruyendo imágenes..." -ForegroundColor $Yellow
        docker-compose -f docker-compose-db.yml build --no-cache
    }
    
    docker-compose -f docker-compose-db.yml up -d
    
    Write-Host "✓ Servicios iniciados correctamente" -ForegroundColor $Green
    Write-Host ""
    Write-Host "Servicios disponibles:" -ForegroundColor $Blue
    Write-Host "  • Aplicación: http://localhost:8180/agent-ai-backend"
    Write-Host "  • Swagger UI: http://localhost:8180/agent-ai-backend/swagger-ui.html"
    Write-Host "  • Health Check: http://localhost:8180/agent-ai-backend/actuator/health"
    Write-Host "  • Base de datos: localhost:25432"
    Write-Host ""
    Write-Host "Para ver logs: .\start-docker.ps1 -Logs" -ForegroundColor $Yellow
    Write-Host "Para detener: .\start-docker.ps1 -Down" -ForegroundColor $Yellow
}

# Función para detener servicios
function Stop-Services {
    Write-Host "Deteniendo servicios..." -ForegroundColor $Blue
    docker-compose -f docker-compose-db.yml down
    Write-Host "✓ Servicios detenidos" -ForegroundColor $Green
}

# Función para mostrar logs
function Show-Logs {
    Write-Host "Mostrando logs de los servicios..." -ForegroundColor $Blue
    docker-compose -f docker-compose-db.yml logs -f
}

# Función principal
function Main {
    if ($Help) {
        Show-Help
        return
    }
    
    if ($Down) {
        Test-Dependencies
        Stop-Services
        return
    }
    
    if ($Logs) {
        Test-Dependencies
        Show-Logs
        return
    }
    
    # Por defecto, iniciar servicios
    Test-Dependencies
    Test-EnvFile
    Start-Services
}

# Ejecutar función principal
Main
