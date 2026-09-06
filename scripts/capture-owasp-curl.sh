#!/usr/bin/env bash
#
# Reproduce las peticiones curl/openssl que respaldan
# docs/mediciones/sec/owasp-curl/A0{1,2,3,5,7}-curl.txt contra el
# despliegue academico publico de BIOPET (Render), no contra localhost.
#
# NO requiere ninguna credencial ni variable de entorno: los cinco
# controles con evidencia curl real solo hacen peticiones SIN autenticar
# (A01: recurso protegido sin sesion; A02: negociacion TLS; A03/A07:
# credenciales deliberadamente invalidas para verificar que el sistema
# las rechaza). No crea cuentas, no modifica datos, no imprime secretos.
#
# A09 (Security Logging and Monitoring Failures): se envia una peticion
# real y benigna (login fallido con credenciales inventadas) con un
# X-Request-ID propio como identificador de correlacion, para dejar
# constancia del lado CLIENTE del evento que deberia auditarse. Esto NO
# demuestra que el servidor escriba el log (el despliegue no expone sus
# logs de contenedor por HTTP): esa parte sigue documentada como
# limitacion en docs/mediciones/sec/owasp-curl/A09-curl.txt, con la
# evidencia real de servidor en docs/mediciones/sec/raw/A09-audit-logs.txt.
#
# Uso:
#   bash scripts/capture-owasp-curl.sh
#
# Variables de entorno opcionales (para reapuntar a otro despliegue, p.
# ej. un entorno de staging propio; por defecto usa el despliegue
# academico publico ya documentado en el repositorio):
#   BACKEND_URL   (por defecto https://biopet-backend-dh5e.onrender.com)
#   FRONTEND_URL  (por defecto https://biopet-frontend.onrender.com)

set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://biopet-backend-dh5e.onrender.com}"
FRONTEND_URL="${FRONTEND_URL:-https://biopet-frontend.onrender.com}"

utc_now() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

echo "== BIOPET OWASP curl evidence =="
echo "Backend:  $BACKEND_URL"
echo "Frontend: $FRONTEND_URL"
echo "Inicio (UTC): $(utc_now)"
echo

echo "--- A01: Broken Access Control (sin sesion) ---"
echo "UTC: $(utc_now)"
curl -sS -D - -o /dev/null "$BACKEND_URL/api/usuarios/me" -w "STATUS:%{http_code}\n"
curl -sS -D - -o /dev/null "$BACKEND_URL/api/mascotas" -w "STATUS:%{http_code}\n"
echo

echo "--- A02: Cryptographic Failures (TLS) ---"
echo "UTC: $(utc_now)"
HOST="$(echo "$BACKEND_URL" | sed -E 's#^https?://##')"
echo | openssl s_client -connect "$HOST:443" -tls1_3 -servername "$HOST" 2>&1 \
  | grep -E "Protocol|Cipher" || true
echo | openssl s_client -connect "$HOST:443" -tls1_3 -servername "$HOST" 2>/dev/null \
  | openssl x509 -noout -subject -issuer -dates || true
echo

echo "--- A03: Injection (payload SQL en campo email) ---"
echo "UTC: $(utc_now)"
curl -sS -D - -o /dev/null -X POST "$BACKEND_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"' OR '1'='1\",\"password\":\"x\"}" \
  -w "STATUS:%{http_code}\n"
echo

echo "--- A05: Security Misconfiguration (cabeceras backend + frontend) ---"
echo "UTC: $(utc_now)"
curl -sS -D - -o /dev/null "$BACKEND_URL/actuator/health" -w "STATUS:%{http_code}\n" \
  | grep -iE "strict-transport-security|content-security-policy|x-frame-options|x-content-type-options|STATUS"
curl -sS -D - -o /dev/null "$FRONTEND_URL/" -w "STATUS:%{http_code}\n" \
  | grep -iE "strict-transport-security|content-security-policy|x-frame-options|x-content-type-options|STATUS"
echo

echo "--- A07: Identification and Authentication Failures (credenciales invalidas) ---"
echo "UTC: $(utc_now)"
TS="$(date +%s)"
curl -sS -D - -o /dev/null -X POST "$BACKEND_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"qa.owasp.a07.curl.${TS}@example.test\",\"password\":\"WrongPass123!\"}" \
  -w "STATUS:%{http_code}\n"
echo
echo "Nota: deliberadamente NO se repiten 6 intentos para no bloquear por"
echo "15 minutos (rate limit) la IP de salida contra el demo publico. Ver"
echo "docs/mediciones/sec/owasp-curl/A07-curl.txt."
echo

echo "--- A09: Security Logging and Monitoring Failures (lado cliente del evento) ---"
echo "UTC: $(utc_now)"
TS9="$(date +%s)"
REQID="qa-owasp-a09-${TS9}"
curl -sS -D - -o /dev/null -X POST "$BACKEND_URL/api/auth/login" \
  -H "X-Request-ID: ${REQID}" -H "Content-Type: application/json" \
  -d "{\"email\":\"qa.owasp.a09.curl.${TS9}@example.test\",\"password\":\"WrongPass123!\"}" \
  -w "STATUS:%{http_code}\n"
echo
echo "Nota: esto demuestra la peticion que dispara LOGIN_FAILURE del lado"
echo "cliente (ver AuthService.java / AuthenticationAuditService.java)."
echo "NO demuestra que el servidor escriba el log: el despliegue no expone"
echo "sus logs de contenedor por HTTP. Ver"
echo "docs/mediciones/sec/owasp-curl/A09-curl.txt y"
echo "docs/mediciones/sec/raw/A09-audit-logs.txt para esa evidencia real."
echo

echo "Fin (UTC): $(utc_now)"
