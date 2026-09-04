#!/usr/bin/env bash
#
# scripts/run-lighthouse-render.sh
#
# Ejecuta la auditoría Lighthouse del hallazgo de recalificación "Lighthouse
# final contra Render" (P4/R1): mismas rutas y umbrales que
# scripts/run-lighthouse.sh, pero contra el despliegue PÚBLICO de Render
# (https://biopet-frontend.onrender.com), no contra localhost. Archiva los
# resultados crudos en docs/mediciones/lighthouse/ tal cual los produce la
# herramienta, con el mismo criterio que run-lighthouse.sh (no se edita el
# JSON a mano).
#
# Precondición: el frontend debe estar desplegado y estable en Render (sin
# despliegue en curso) — verificado abajo con un GET real antes de auditar.
#
# Uso:
#   bash scripts/run-lighthouse-render.sh
#
# Corre los DOS perfiles (móvil: lighthouserc.render.js: desktop:
# lighthouserc.render.desktop.js), 3 corridas por perfil, sobre 2 rutas
# (/login, /mascotas) => 12 JSON completos (>= 6 exigidos por el criterio
# de aceptación).
#
# Salida (por cada perfil, sufijo "-render-mobile"/"-render-desktop"):
#   docs/mediciones/lighthouse/lhci-YYYYMMDD-HHMM-render-mobile-login-runN.json
#   docs/mediciones/lighthouse/lhci-YYYYMMDD-HHMM-render-mobile-mascotas-runN.json
#   docs/mediciones/lighthouse/lhci-YYYYMMDD-HHMM-render-desktop-login-runN.json
#   docs/mediciones/lighthouse/lhci-YYYYMMDD-HHMM-render-desktop-mascotas-runN.json
#   docs/mediciones/lighthouse/lhci-YYYYMMDD-HHMM-render.meta.txt
#
# Integración con Makefile (a cargo de Fred, ver hallazgo #8): este script
# no toca el Makefile. Basta un target nuevo que lo invoque, sin duplicar
# nada de lo de abajo, p. ej.:
#   lighthouse-render:
#   	bash scripts/run-lighthouse-render.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$REPO_ROOT/docs/mediciones/lighthouse"
PUBLIC_URL="https://biopet-frontend.onrender.com"
STAMP="$(date -u +%Y%m%d-%H%M)"
ISO_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
COMMIT="$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo "sin-git")"

mkdir -p "$OUT_DIR"

echo "== Verificando que el despliegue publico responda (sin despliegue en curso) =="
for path in /login /mascotas; do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$PUBLIC_URL$path")
  if [[ "$code" != "200" ]]; then
    echo "ERROR: $PUBLIC_URL$path respondio HTTP $code (esperado 200)."
    echo "Puede haber un despliegue en curso en Render; reintenta cuando termine."
    exit 1
  fi
  echo "OK  $PUBLIC_URL$path -> $code"
done

cd "$REPO_ROOT"

# Mismo mecanismo de reintento que run-lighthouse.sh (EPERM transitorio de
# chrome-launcher en Windows al limpiar su carpeta temporal).
run_lhci_autorun() {
  local config="$1"
  local attempt
  for attempt in 1 2 3; do
    echo "-- lhci autorun (config=$config, intento $attempt/3) --"
    if npx --yes @lhci/cli@0.14.x autorun --config="$config"; then
      return 0
    fi
    echo "-- intento $attempt fallo (posible EPERM transitorio de chrome-launcher en Windows); reintentando --"
  done
  echo "ERROR: lhci autorun (config=$config) fallo 3 veces seguidas."
  return 1
}

# Archiva los reportes crudos de un perfil, verificando ademas que
# requestedUrl apunte de verdad al dominio publico de Render (paso 23 del
# plan: nunca archivar un JSON que termine apuntando a localhost).
archive_profile() {
  local profile="$1"
  local raw_dir="$2"
  if [[ ! -d "$raw_dir" ]]; then
    echo "ERROR: no se encontro $raw_dir; ¿lhci autorun (perfil $profile) fallo antes de generar reportes?"
    exit 1
  fi
  local i=0
  for f in "$raw_dir"/*.report.json; do
    [[ -e "$f" ]] || continue
    local url slug
    url=$(node -e "console.log(require(process.argv[1]).requestedUrl)" "$f")
    if [[ "$url" != https://biopet-frontend.onrender.com* ]]; then
      echo "ERROR: requestedUrl inesperado en $f: $url (se esperaba $PUBLIC_URL...)"
      exit 1
    fi
    slug=$(echo "$url" | sed -E 's#https?://[^/]+/##; s#[^a-zA-Z0-9]+#-#g')
    cp "$f" "$OUT_DIR/lhci-${STAMP}-render-${profile}-${slug:-root}-run${i}.json"
    i=$((i+1))
  done
}

echo "== [1/2] Ejecutando lhci autorun contra Render — perfil movil (lighthouserc.render.js) =="
rm -rf "$REPO_ROOT/.lighthouseci-render"
run_lhci_autorun lighthouserc.render.js
echo "== Archivando resultados crudos (render-mobile) en $OUT_DIR =="
archive_profile "mobile" "$REPO_ROOT/.lighthouseci-render"

echo "== [2/2] Ejecutando lhci autorun contra Render — perfil desktop (lighthouserc.render.desktop.js) =="
rm -rf "$REPO_ROOT/.lighthouseci-render-desktop"
run_lhci_autorun lighthouserc.render.desktop.js
echo "== Archivando resultados crudos (render-desktop) en $OUT_DIR =="
archive_profile "desktop" "$REPO_ROOT/.lighthouseci-render-desktop"

LHCI_VERSION="$(npx --yes @lhci/cli@0.14.x --version 2>/dev/null || echo "desconocida")"

cat > "$OUT_DIR/lhci-${STAMP}-render.meta.txt" <<EOF
fecha_iso8601: $ISO_DATE
commit_hash_corto: $COMMIT
node_version: $(node --version)
lighthouse_cli_version: $LHCI_VERSION
url_publica_evaluada: $PUBLIC_URL
rutas_auditadas: $PUBLIC_URL/login, $PUBLIC_URL/mascotas
perfiles: mobile (throttlingMethod=simulate, lighthouserc.render.js), desktop (formFactor=desktop, throttling devtools preset, lighthouserc.render.desktop.js)
corridas_por_url: 3 por perfil (6 total por URL, 12 total)
EOF

echo ""
echo "Listo. Revisa $OUT_DIR y agrega los .json + .meta.txt generados a Git."
