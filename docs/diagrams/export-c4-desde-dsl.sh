#!/usr/bin/env bash
# Exporta los tres niveles C4 desde docs/diagrams/workspace.dsl (fuente unica)
# y renderiza los PNG evaluados en el informe.
#
# Requiere (una sola vez, fuera del repo):
#   - Java 11+ ............................ java -version
#   - Structurizr CLI v2025.11.09 ......... https://github.com/structurizr/cli/releases
#     (validado con v2025.11.09; descomprimir y poner `structurizr.sh` en el
#     PATH, o STRUCTURIZR_CLI=<ruta>/structurizr.sh; en Windows usar Git Bash
#     o adaptar la llamada a structurizr.bat con los mismos argumentos)
#   - plantuml.jar ........................ https://github.com/plantuml/plantuml/releases
#     (variable PLANTUML_JAR=<ruta>/plantuml.jar)
#   - Graphviz `dot` ...................... https://graphviz.org/download/
#
# Uso (desde la RAIZ del repositorio):
#   STRUCTURIZR_CLI=/ruta/a/structurizr.sh PLANTUML_JAR=/ruta/a/plantuml.jar \
#     bash docs/diagrams/export-c4-desde-dsl.sh
#
# Salidas (versionadas):
#   docs/diagrams/structurizr-export/L{1,2,3}-*.puml .. exporte textual del CLI, SIN editar
#   docs/diagrams/c4-contexto/c4-contexto.png
#   docs/diagrams/c4-contenedores/c4-contenedores.png
#   docs/diagrams/c4-componentes-backend/c4-componentes-backend.png
#   (+ sus copias byte-identicas en docs/informe/figuras/)
set -euo pipefail

STRUCTURIZR_CLI="${STRUCTURIZR_CLI:-structurizr.sh}"
PLANTUML_JAR="${PLANTUML_JAR:-}"

command -v java >/dev/null || { echo "ERROR: falta java (JRE 11+)" >&2; exit 1; }
command -v dot >/dev/null || { echo "ERROR: falta Graphviz dot" >&2; exit 1; }
[ -n "$PLANTUML_JAR" ] && [ -f "$PLANTUML_JAR" ] || { echo "ERROR: define PLANTUML_JAR=<ruta>/plantuml.jar" >&2; exit 1; }

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXP="$ROOT/docs/diagrams/structurizr-export"

"$STRUCTURIZR_CLI" export -workspace "$ROOT/docs/diagrams/workspace.dsl" -format plantuml -output "$EXP/tmp-export"
mv "$EXP/tmp-export/structurizr-L1-SystemContext.puml" "$EXP/L1-SystemContext.puml"
mv "$EXP/tmp-export/structurizr-L2-Containers.puml"    "$EXP/L2-Containers.puml"
mv "$EXP/tmp-export/structurizr-L3-BackendComponents.puml" "$EXP/L3-BackendComponents.puml"
rmdir "$EXP/tmp-export"

render() { # $1=puml $2=png-destino
  # PLANTUML_LIMIT_SIZE evita el recorte del nivel L3 (el mas grande).
  java -DPLANTUML_LIMIT_SIZE=16384 -jar "$PLANTUML_JAR" -Tpng -o "$(dirname "$2")" "$1" >/dev/null
  base="$(basename "$1" .puml).png"
  mv "$(dirname "$1")/$base" "$2"
}

render "$EXP/L1-SystemContext.puml"      "$ROOT/docs/diagrams/c4-contexto/c4-contexto.png"
render "$EXP/L2-Containers.puml"         "$ROOT/docs/diagrams/c4-contenedores/c4-contenedores.png"
render "$EXP/L3-BackendComponents.puml"  "$ROOT/docs/diagrams/c4-componentes-backend/c4-componentes-backend.png"

cp "$ROOT/docs/diagrams/c4-contexto/c4-contexto.png"              "$ROOT/docs/informe/figuras/compartidas/c4-contexto.png"
cp "$ROOT/docs/diagrams/c4-contenedores/c4-contenedores.png"      "$ROOT/docs/informe/figuras/compartidas/c4-contenedores.png"
cp "$ROOT/docs/diagrams/c4-componentes-backend/c4-componentes-backend.png" "$ROOT/docs/informe/figuras/jaime/c4-componentes-backend.png"

echo "OK: PNG evaluados regenerados desde workspace.dsl"
