#!/usr/bin/env bash
#
# scripts/run-zap-authenticated-local.sh
#
# Ejecuta un OWASP ZAP Automation Framework plan AUTENTICADO, PASIVO (sin
# active scan), contra el stack LOCAL de este repositorio con HTTPS/TLS
# real (docker-compose.tls.yml, backend en https://backend:8443 dentro de
# la red Docker Compose del propio proyecto). NO usa BIOPET-V2, NO usa
# Render.
#
# Requisitos previos (no automatizados aqui, ver docs/mediciones/sec/A02-cryptography-tls.md):
#   1. Keystore de desarrollo generado:
#        powershell -ExecutionPolicy Bypass -File scripts/generate-dev-keystore.ps1   (Windows)
#        scripts/generate-dev-keystore.sh                                             (Linux/macOS)
#   2. Stack local con TLS levantado:
#        docker compose -f docker-compose.yml -f docker-compose.tls.yml up -d postgres redis backend
#
# Credenciales: EXCLUSIVAMENTE via variables de entorno, nunca
# hardcodeadas ni escritas a ningun archivo versionado:
#   ZAP_USERNAME  correo de la cuenta local a usar (ver db/seed.sql / DataInitializer)
#   ZAP_PASSWORD  contrasena de esa misma cuenta local
#
# Este script:
#   1. Verifica que ambas variables de entorno esten definidas (si no,
#      se detiene con mensaje claro, sin valores por defecto para la
#      contrasena).
#   2. Verifica que el backend responda "healthy" por HTTPS local
#      (misma comprobacion que docs/mediciones/sec/A02-cryptography-tls.md).
#   3. Genera una copia TEMPORAL (fuera del repositorio, en un directorio
#      creado con mktemp) de docs/mediciones/sec/zap/zap-authenticated-local.yaml,
#      sustituyendo los tokens __ZAP_USERNAME__/__ZAP_PASSWORD__ por el
#      valor real de las variables de entorno. El archivo versionado en
#      el repositorio NUNCA se modifica ni contiene un valor real.
#   4. Ejecuta ZAP (ghcr.io/zaproxy/zaproxy:stable) en modo Automation
#      Framework contra ese archivo temporal, en la misma red Docker
#      Compose que el backend (resuelve "backend" por nombre de servicio).
#   5. Borra el archivo temporal (con la credencial) en cuanto ZAP termina,
#      pase lo que pase (trap EXIT).
#   6. Verifica en la salida de ZAP si CUALQUIERA de las peticiones
#      autenticadas esperadas (login + las 7 rutas GET protegidas) NO
#      devolvio el codigo 200 esperado. Si encuentra alguna diferencia,
#      el script se DETIENE con exit != 0 y NO presenta la evidencia
#      generada como valida (la deja archivada igual, pero con un aviso
#      explicito en RUN-METADATA-AUTH-LOCAL.txt).
#
# Uso:
#   ZAP_USERNAME='admin@biopet.ec' ZAP_PASSWORD='...' scripts/run-zap-authenticated-local.sh
#
# Variables de entorno opcionales:
#   ZAP_IMAGE   Imagen de ZAP a usar (por defecto ghcr.io/zaproxy/zaproxy:stable)
#   ZAP_TARGET  URL base del backend dentro de la red Docker (por defecto
#               https://backend:8443; SOLO tiene sentido apuntar al stack
#               local -- no se acepta aqui una URL de Render ni de
#               BIOPET-V2, que quedan fuera de alcance de este script)

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ZAP_DIR="$REPO_ROOT/docs/mediciones/sec/zap"
PLAN_TEMPLATE="$ZAP_DIR/zap-authenticated-local.yaml"

if [ -z "${ZAP_USERNAME:-}" ] || [ -z "${ZAP_PASSWORD:-}" ]; then
    echo "Error: define ZAP_USERNAME y ZAP_PASSWORD como variables de entorno antes de ejecutar este script." >&2
    echo "Ejemplo: ZAP_USERNAME='admin@biopet.ec' ZAP_PASSWORD='...' $0" >&2
    echo "No hay valor por defecto para la contrasena: este script nunca la hardcodea." >&2
    exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "Error: docker no esta en el PATH." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Error: el daemon de Docker no responde (¿Docker Desktop esta iniciado?)." >&2
    exit 1
fi

if ! docker inspect biopet-backend --format '{{.State.Health.Status}}' 2>/dev/null | grep -q '^healthy$'; then
    echo "Error: 'biopet-backend' no esta 'healthy'. Levanta primero el stack local con TLS:" >&2
    echo "  docker compose -f docker-compose.yml -f docker-compose.tls.yml up -d postgres redis backend" >&2
    exit 1
fi

ZAP_IMAGE="${ZAP_IMAGE:-ghcr.io/zaproxy/zaproxy:stable}"
ZAP_TARGET="${ZAP_TARGET:-https://backend:8443}"
case "$ZAP_TARGET" in
    https://backend:*) ;;
    https://localhost:*) ;;
    *)
        echo "Error: ZAP_TARGET debe apuntar al backend LOCAL (https://backend:* dentro de la red Docker," >&2
        echo "o https://localhost:* si se ejecuta fuera de ella). No se acepta Render ni BIOPET-V2 en este script." >&2
        exit 2
        ;;
esac

NETWORK_NAME="$(docker inspect biopet-backend --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' 2>/dev/null | head -1)"
if [ -z "$NETWORK_NAME" ]; then
    echo "Error: no se pudo determinar la red Docker del contenedor biopet-backend." >&2
    exit 1
fi

# Verificacion previa (curl, sin ZAP) de que /actuator/health responde por
# HTTPS antes de invertir tiempo en levantar el contenedor de ZAP.
if command -v curl.exe >/dev/null 2>&1; then
    CURL_BIN="curl.exe"
else
    CURL_BIN="curl"
fi
HEALTH_STATUS="$("$CURL_BIN" -sk -o /dev/null -w '%{http_code}' https://localhost:8443/actuator/health 2>/dev/null || echo "000")"
if [ "$HEALTH_STATUS" != "200" ]; then
    echo "Error: https://localhost:8443/actuator/health respondio '$HEALTH_STATUS' (se esperaba 200)." >&2
    echo "Verifica que docker-compose.tls.yml este activo y el keystore generado." >&2
    exit 1
fi
echo "Backend local healthy por HTTPS (actuator/health = 200)."

# El directorio temporal se crea DENTRO del arbol del repo (no en /tmp):
# con MSYS_NO_PATHCONV=1 (necesario para que las rutas DESTINO dentro del
# contenedor, como "/zap/wrk", no se reescriban como rutas de Windows),
# una ruta host tipo "/tmp/..." deja de convertirse a "C:\..." y Docker
# Desktop no la reconoce; las rutas "/c/Users/..." (dentro de este repo)
# si las reconoce, mismo comportamiento ya verificado por
# scripts/run-zap-baseline.sh con "$ZAP_DIR". Se elimina por completo en
# la limpieza (trap), nunca se commitea.
TMP_DIR="$(mktemp -d "$REPO_ROOT/.zap-auth-local-tmp.XXXXXX")"
TMP_PLAN="$TMP_DIR/zap-authenticated-local.plan.yaml"
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

# Sustitucion de tokens SOLO en el archivo temporal (fuera del repo). El
# .yaml versionado nunca se toca ni contiene un valor real.
sed \
    -e "s#__ZAP_USERNAME__#${ZAP_USERNAME//#/\\#}#g" \
    -e "s#__ZAP_PASSWORD__#${ZAP_PASSWORD//#/\\#}#g" \
    "$PLAN_TEMPLATE" > "$TMP_PLAN"

echo "Descargando/verificando imagen de ZAP: $ZAP_IMAGE"
docker pull "$ZAP_IMAGE" >/dev/null

FECHA_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo ""
echo "=== Ejecutando OWASP ZAP Automation Framework (autenticado, local, TLS) ==="
echo "Target : $ZAP_TARGET"
echo "Imagen : $ZAP_IMAGE"
echo "Fecha  : $FECHA_UTC"
echo ""

RUN_LOG="$TMP_DIR/run.log"
# Se monta el DIRECTORIO temporal completo (no el archivo suelto): un
# mount de un unico archivo con MSYS_NO_PATHCONV=1 en Git Bash/Windows
# puede resolverse mal y terminar montando un directorio vacio en su
# lugar (mismo tipo de problema de rutas que documenta
# scripts/run-zap-baseline.sh); montar el directorio es el patron ya
# usado y verificado en ese script.
MSYS_NO_PATHCONV=1 docker run --rm \
    --network "$NETWORK_NAME" \
    -v "$ZAP_DIR:/zap/wrk/:rw" \
    -v "$TMP_DIR:/zap/wrk-plan/:ro" \
    "$ZAP_IMAGE" \
    zap.sh -cmd \
    -autorun /zap/wrk-plan/zap-authenticated-local.plan.yaml \
    2>&1 | tee "$RUN_LOG"
ZAP_EXIT=${PIPESTATUS[0]}

echo ""
echo "zap.sh finalizo con codigo de salida: $ZAP_EXIT"

# Gate real de autenticacion: si CUALQUIER peticion (login o las 7 rutas
# protegidas) no devolvio el codigo esperado, la sesion no estaba
# realmente autenticada -- no se presenta el resultado como exitoso.
AUTH_FALLO=0
if grep -q "Difference in response code values" "$RUN_LOG"; then
    AUTH_FALLO=1
    echo "" >&2
    echo "*** AUTENTICACION NO CONFIRMADA ***" >&2
    echo "Al menos una peticion no devolvio el codigo 200 esperado:" >&2
    grep "Difference in response code values" "$RUN_LOG" >&2
    echo "No se debe presentar esta corrida como evidencia de escaneo autenticado exitoso." >&2
fi

cat > "$ZAP_DIR/RUN-METADATA-AUTH-LOCAL.txt" <<EOF
fecha_utc=$FECHA_UTC
imagen_zap=$ZAP_IMAGE
target=$ZAP_TARGET (HTTPS/TLS local, docker-compose.tls.yml)
red_docker=$NETWORK_NAME
tipo_scan=authenticated passive (sin active scan)
endpoint_login=POST /api/auth/login
endpoint_verificacion=GET /api/usuarios/me
mecanismo_sesion=cookie (access_token, HttpOnly+Secure+SameSite=Strict)
autenticacion_confirmada=$([ "$AUTH_FALLO" -eq 0 ] && echo "SI, todas las peticiones autenticadas devolvieron 200" || echo "NO -- ver run.log, al menos una peticion no devolvio 200")
codigo_salida_zap=$ZAP_EXIT
comando=ZAP_USERNAME=*** ZAP_PASSWORD=*** scripts/run-zap-authenticated-local.sh
EOF

if [ "$AUTH_FALLO" -eq 1 ]; then
    echo ""
    echo "Metadatos de la corrida (marcada como NO autenticada) escritos en $ZAP_DIR/RUN-METADATA-AUTH-LOCAL.txt" >&2
    exit 1
fi

echo ""
echo "Autenticacion confirmada: todas las peticiones esperadas (login + 7 rutas protegidas) devolvieron 200."
echo "Reportes y metadata archivados en: $ZAP_DIR"
exit "$ZAP_EXIT"
