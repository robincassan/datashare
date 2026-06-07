#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────────
# DataShare — Script d'installation et de démarrage
# ──────────────────────────────────────────────────
# Prérequis : Java 21+, Maven 3.9+, Node.js 24+, npm 11+, Docker
# Usage     : bash scripts/setup.sh
# ──────────────────────────────────────────────────

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ── 1. Vérification des prérequis ──────────────────
check_prereq() {
    local cmd=$1
    local hint=$2
    if ! command -v "$cmd" &>/dev/null; then
        error "$cmd n'est pas installé. $hint"
        exit 1
    fi
    info "$cmd trouvé : $($cmd --version 2>&1 | head -1)"
}

check_prereq java "Installe Java 21+ : https://adoptium.net"
check_prereq mvn  "Installe Maven 3.9+ : https://maven.apache.org"
check_prereq node "Installe Node.js 24+ : https://nodejs.org"
check_prereq npm  "Fourni avec Node.js"
check_prereq docker "Installe Docker : https://docker.com"

# ── 2. Base de données PostgreSQL ──────────────────
start_database() {
    if docker ps --format '{{.Names}}' | grep -q '^datashare-db$'; then
        info "Conteneur PostgreSQL déjà en cours d'exécution"
        return 0
    fi

    if docker ps -a --format '{{.Names}}' | grep -q '^datashare-db$'; then
        info "Conteneur PostgreSQL existant trouvé, démarrage..."
        docker start datashare-db
    else
        info "Création du conteneur PostgreSQL..."
        docker compose -f "$ROOT_DIR/docker-compose.yml" up -d postgres
    fi

    info "Attente de la disponibilité de PostgreSQL..."
    for i in $(seq 1 10); do
        if docker exec datashare-db pg_isready -U postgres &>/dev/null; then
            info "PostgreSQL prêt"
            return 0
        fi
        sleep 2
    done

    error "PostgreSQL n'a pas démarré dans le temps imparti"
    exit 1
}

# ── 3. Backend ─────────────────────────────────────
build_and_start_backend() {
    info "Build du backend..."
    cd "$BACKEND_DIR"
    mvn clean install -DskipTests -q

    info "Démarrage du backend (port 8080)..."
    cd "$BACKEND_DIR"
    mvn spring-boot:run -q &
    BACKEND_PID=$!
    echo "$BACKEND_PID" > /tmp/datashare-backend.pid

    info "Attente du démarrage du backend..."
    for i in $(seq 1 15); do
        if curl -s http://localhost:8080/api/auth/login >/dev/null 2>&1; then
            info "Backend prêt sur http://localhost:8080"
            return 0
        fi
        sleep 2
    done

    warn "Le backend n'a pas répondu rapidement, vérifie les logs avec : tail -f $BACKEND_DIR/logs/*.log"
}

# ── 4. Frontend ────────────────────────────────────
build_and_start_frontend() {
    info "Installation des dépendances frontend..."
    cd "$FRONTEND_DIR"
    npm install --silent

    info "Démarrage du frontend (port 4200)..."
    npx ng serve --open &
    FRONTEND_PID=$!
    echo "$FRONTEND_PID" > /tmp/datashare-frontend.pid

    info "Frontend en cours de démarrage sur http://localhost:4200"
}

# ── 5. Résumé ──────────────────────────────────────
show_summary() {
    echo ""
    echo "═══════════════════════════════════════════"
    info "  DataShare est en cours de démarrage"
    echo "  Frontend : http://localhost:4200"
    echo "  API      : http://localhost:8080"
    echo "  Base     : localhost:5432 (postgres/postgres)"
    echo ""
    echo "  Arrêter le backend  : kill \$(cat /tmp/datashare-backend.pid)"
    echo "  Arrêter le frontend : kill \$(cat /tmp/datashare-frontend.pid)"
    echo "  Arrêter la base     : docker stop datashare-db"
    echo "═══════════════════════════════════════════"
    echo ""
}

# ── Exécution ──────────────────────────────────────
main() {
    echo ""
    echo "═══════════════════════════════════════════"
    echo "  DataShare — Installation et démarrage"
    echo "═══════════════════════════════════════════"
    echo ""

    check_prereq
    start_database
    build_and_start_backend
    build_and_start_frontend
    show_summary
}

main
