#!/bin/bash

# Script para iniciar la aplicación con Docker Compose
# Uso: ./start-docker.sh [--build] [--down]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar ayuda
show_help() {
    echo -e "${BLUE}Script de Docker para Chatbot IA Backend${NC}"
    echo ""
    echo "Uso: $0 [OPCIONES]"
    echo ""
    echo "Opciones:"
    echo "  --build     Reconstruir las imágenes antes de iniciar"
    echo "  --down      Detener y eliminar los contenedores"
    echo "  --logs      Mostrar logs de los servicios"
    echo "  --help      Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0                    # Iniciar servicios"
    echo "  $0 --build           # Reconstruir e iniciar"
    echo "  $0 --down            # Detener servicios"
    echo "  $0 --logs            # Ver logs"
}

# Función para verificar dependencias
check_dependencies() {
    echo -e "${BLUE}Verificando dependencias...${NC}"
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker no está instalado${NC}"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}Error: Docker Compose no está instalado${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker y Docker Compose están disponibles${NC}"
}

# Función para verificar archivo .env
check_env_file() {
    if [ ! -f "env.docker" ]; then
        echo -e "${YELLOW}Advertencia: No se encontró el archivo env.docker${NC}"
        echo -e "${YELLOW}Creando archivo de ejemplo...${NC}"
        cp env.template env.docker 2>/dev/null || echo -e "${RED}No se pudo crear env.docker${NC}"
    fi
}

# Función para iniciar servicios
start_services() {
    echo -e "${BLUE}Iniciando servicios...${NC}"
    
    if [ "$1" = "--build" ]; then
        echo -e "${YELLOW}Reconstruyendo imágenes...${NC}"
        docker-compose -f docker-compose-db.yml build --no-cache
    fi
    
    docker-compose -f docker-compose-db.yml up -d
    
    echo -e "${GREEN}✓ Servicios iniciados correctamente${NC}"
    echo ""
    echo -e "${BLUE}Servicios disponibles:${NC}"
    echo -e "  • Aplicación: http://localhost:8180/agent-ai-backend"
    echo -e "  • Swagger UI: http://localhost:8180/agent-ai-backend/swagger-ui.html"
    echo -e "  • Health Check: http://localhost:8180/agent-ai-backend/actuator/health"
    echo -e "  • Base de datos: localhost:25432"
    echo ""
    echo -e "${YELLOW}Para ver logs: $0 --logs${NC}"
    echo -e "${YELLOW}Para detener: $0 --down${NC}"
}

# Función para detener servicios
stop_services() {
    echo -e "${BLUE}Deteniendo servicios...${NC}"
    docker-compose -f docker-compose-db.yml down
    echo -e "${GREEN}✓ Servicios detenidos${NC}"
}

# Función para mostrar logs
show_logs() {
    echo -e "${BLUE}Mostrando logs de los servicios...${NC}"
    docker-compose -f docker-compose-db.yml logs -f
}

# Función principal
main() {
    case "${1:-}" in
        --help|-h)
            show_help
            exit 0
            ;;
        --down)
            check_dependencies
            stop_services
            exit 0
            ;;
        --logs)
            check_dependencies
            show_logs
            exit 0
            ;;
        --build|"")
            check_dependencies
            check_env_file
            start_services "$1"
            exit 0
            ;;
        *)
            echo -e "${RED}Opción desconocida: $1${NC}"
            show_help
            exit 1
            ;;
    esac
}

# Ejecutar función principal
main "$@"
