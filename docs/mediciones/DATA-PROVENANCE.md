# Procedencia de los datos (Data Provenance) — BIOPET

Este documento complementa a `DATA-DICTIONARY.md` (qué significa cada
variable) documentando, para cada conjunto de datos crudo bajo
`docs/mediciones/`, **de dónde viene, cómo se generó, con qué herramienta
y versión, y qué transformación (si alguna) sufrió antes de llegar al
repositorio**. Es el insumo mínimo para que el paquete de evidencia sea
reproducible y trazable (principios FAIR — Findable, Accessible,
Interoperable, Reusable), en línea con lo ya practicado en
`docs/mediciones/lighthouse/README.md` (que documenta anonimización y
`SHA256SUMS-ORIGINAL.txt`).

No se documenta aquí ningún dato nuevo: es un índice de procedencia sobre
datos que ya existen en el repositorio, verificado contra su origen real.

---

# Procedencia de los datos (Data Provenance) — BIOPET (Entrega Final v1.0.0)

Este documento complementa a `DATA-DICTIONARY.md` (qué significa cada
variable) documentando, para cada conjunto de datos crudo bajo
`docs/mediciones/`, **de dónde viene, cómo se generó, con qué herramienta
y versión, y qué transformación (si alguna) sufrió antes de llegar al
repositorio**. Es el insumo mínimo para que el paquete de evidencia sea
reproducible y trazable (principios FAIR — Findable, Accessible,
Interoperable, Reusable), en línea con lo ya practicado en
`docs/mediciones/lighthouse/README.md` (que documenta anonimización y
`SHA256SUMS-ORIGINAL.txt`).

No se documenta aquí ningún dato nuevo: es un índice de procedencia sobre
datos que ya existen en el repositorio, verificado contra su origen real.

---

## 1. Rendimiento — `docs/mediciones/perf/`

| Campo | Detalle |
|---|---|
| Generado por | k6 (`k6 run k6/listado-mascotas.js`), 50 VUs, ~30–35 s por corrida |
| Responsable | Fred Beltrán Montiel |
| Fecha de generación | 2026-09-03 (corridas `k6-20260903T*-local-tls-v1.0.0-*`); corridas históricas `k6-20260817T*` y `k6-run{1,2,3}-{frio,caliente}.json` conservadas para comparación histórica |
| Entrada | Backend real vía `https://localhost:8443` (perfil `tls`, `docker-compose.tls.yml`), no un mock ni un stub |
| Transformación aplicada | Agregación estadística (media, mediana, IC95% con distribución t de Student vía `scipy.stats.t`, percentiles, Wilcoxon pareado + corrección Holm-Bonferroni, Mann-Whitney U sensitivity) realizada por `scripts/perf-analysis.py` sobre los JSON crudos de k6, volcada a `REPORT.md` y `grafico.svg` |
| Archivo crudo sin transformar | Los `.json` individuales (`k6-20260903T*.json`) — **no se editan manualmente**; son la salida directa de k6 |
| Cadena de verificación | `REPORT.md` cita el nombre exacto de cada archivo fuente por fila de su tabla, permitiendo recalcular cualquier estadístico desde el crudo |
| Commit de generación | `3adb230` (test (perf): regenerar evidencia de rendimiento final v1.0.0) |
| Limitación declarada | Corridas ejecutadas contra una sola instancia del backend, sin réplicas (ver README.md, sección "Consideraciones para evolución y despliegue") |

## 2. Caché Redis — `docs/mediciones/redis/`

| Campo | Detalle |
|---|---|
| Generado por | `redis-cli` (`CONFIG GET maxmemory*`, `DBSIZE`, `KEYS mascotas::*`, `TTL <clave>`), ejecutado manualmente contra el contenedor Redis real |
| Responsable | Fred Beltrán Montiel |
| Entrada | Instancia Redis 7 del `docker-compose.yml`, con carga generada por la corrida k6 de referencia |
| Transformación aplicada | Ninguna — son capturas de salida de terminal (`.txt`) tal cual las devolvió `redis-cli` |
| Codificación | UTF-16 LE con terminadores CRLF (verificado con `file`); previsible si la captura se hizo desde PowerShell en Windows — no indica corrupción del dato |
| Limitación declarada | Documenta TTL y existencia de clave bajo carga, pero **no** un hit ratio medido (aciertos/total) — pendiente, ver `docs/requisitos/SRS.md` sección 7 |

## 3. Seguridad — `docs/mediciones/sec/`

| Campo | Detalle |
|---|---|
| Generado por | `scripts/security-evidence.sh` (o `.ps1` en Windows), que combina: `curl`/`curl.exe` y `openssl s_client` contra el stack Docker real, `mvn clean verify` (suite JUnit real), y `docker compose logs backend` para los logs `AUTH_AUDIT` |
| Responsable | Jaime Mariscal Cabrera |
| Fecha de generación | Desde 2026-07-31 (evidencia HTTP real), consolidada en la Tercera Entrega |
| Entrada | Backend real vía TLS (`https://localhost:8443`), no mocks; peticiones HTTP reales para cada uno de los 6 controles OWASP documentados (A01, A02, A03, A05, A07, A09) |
| Transformación aplicada | Ninguna sobre los archivos individuales de `raw/` (`A01-access-control.txt`, etc. — salida cruda de `curl`/`openssl`); los documentos `.md` (`A01-access-control.md`, etc.) narran e interpretan esa salida cruda, citándola |
| Cadena de verificación | Cada afirmación en los `.md` de esta carpeta enlaza al archivo crudo correspondiente en `raw/` |
| Nota de versionado | `docs/mediciones/sec/raw/` no está excluida por `.gitignore` (solo `.gitkeep`); el contenido es evidencia real destinada a versionarse, no una salida local descartable |

## 4. Cobertura JaCoCo — `docs/mediciones/sec/jacoco-summary.md`

| Campo | Detalle |
|---|---|
| Generado por | `mvn clean verify` (fase `verify`, plugin `jacoco-maven-plugin`, regla `BUNDLE` con umbral LINE ≥70%, BRANCH ≥70%, COMPLEXITY ≥60%, según `Backend/pom.xml`) |
| Responsable | Jaime Mariscal Cabrera |
| Entrada | Suite de pruebas JUnit 5 + MockMvc/Testcontainers reales (`Backend/src/test/java/com/biopet/**`). **Total canónico para la Entrega Final (tag `v1.0.0`): 205 pruebas, 0 fallos, 0 errores, 0 omitidas** — verificado por reproducción independiente de `mvn clean verify` sobre el commit exacto del tag (`0d5cd525ce648cca7219da204e16fa622e671a87`), archivada en [`docs/mediciones/sec/reproduccion-v1.0.0/`](sec/reproduccion-v1.0.0/) y detallada en [`TEST-COUNT-PROVENANCE.md`](TEST-COUNT-PROVENANCE.md). **Nota histórica:** esta fila decía "166 pruebas" (cifra narrativa, sin log crudo localizado que la respalde — ver clasificación en `TEST-COUNT-PROVENANCE.md`); el log crudo archivado el mismo día que se generó `jacoco-summary.md` (commit `bb43baa`, 2026-08-16) reportaba 189, correcto para ese commit pero anterior a dos clases de prueba (16 casos) que ya forman parte del tag `v1.0.0` — ese log de 189 se conserva sin modificar como evidencia histórica de ese punto exacto del proyecto, no como el resultado final. |
| Transformación aplicada | Ninguna manual — `jacoco-summary.md` resume el reporte HTML/XML que genera JaCoCo en `Backend/target/site/jacoco/`, no versionado por ser artefacto regenerable en cada build |
| Reproducibilidad | Se regenera localmente ejecutando `cd Backend && mvn clean verify`. La cobertura vigente de la Entrega Final (91.80% LINE / 79.39% BRANCH) está en `docs/mediciones/sec/jacoco-summary.md`, sección "Cobertura actual (real, Entrega Final)". El baseline histórico (87.45% LINE, 67.98% BRANCH, 71.81% COMPLEXITY) corresponde a la Tercera Entrega y **ya no describe el estado actual**; se conserva en `jacoco-summary.md` solo como referencia histórica |

## 5. Usabilidad SUS — `docs/mediciones/sus/`

| Campo | Detalle |
|---|---|
| Generado por | Instrumento SUS (Brooke, 1996), según `docs/mediciones/sus/instrumento-sus.md` |
| Responsable | Zaida Taipe Mora |
| Fecha de recolección | Muestra inicial P01–P10 (Tercera Entrega), ampliada con P11–P18 (Entrega Final) para cumplir n≥15 |
| Entrada | Respuestas Likert 1–5 de cada participante a los 10 ítems estándar del SUS |
| Transformación aplicada | El puntaje por participante (`sus_score`) se **calcula** desde las respuestas Q1–Q10 con la fórmula de Brooke (suma de contribuciones × 2.5) en `scripts/analisis-sus.py` (`calcular_puntaje_sus`), y se valida contra el valor ya almacenado en `sus-raw.csv`; el script se detiene con error si no coincide. El resultado se vuelca a `REPORT.md` |
| Archivo crudo sin transformar | `docs/mediciones/sus/sus-raw.csv` — respuestas individuales anonimizadas (código de participante, no nombre) |
| Anonimización | Participantes identificados solo por código (P01–P18); sin datos que permitan reidentificación individual, más allá de edad/sexo/experiencia declarados de forma agregada |

**Distinción de procedencia (auditoría 2026-08-31):**

- **Evidencia reproducible:** `sus-raw.csv` contiene 18 filas (P01–P18)
  con sus 10 respuestas Q1–Q10; el puntaje `sus_score` de cada fila es
  matemáticamente reproducible desde esas respuestas (verificado por
  `scripts/analisis-sus.py`); `REPORT.md` es reproducible de forma
  determinista a partir de `sus-raw.csv` (n=18, media 74.44, DE 22.35,
  IC95% [63.33, 85.56], mediana 82.50, mínimo 22.50, máximo 97.50).
- **Declaración del equipo:** los 18 registros proceden de participantes
  reales evaluados durante el desarrollo del proyecto. Esta es una
  declaración del equipo, no un hecho verificable de forma independiente
  desde el repositorio.
- **Limitación documental:** actualmente no existe evidencia verificable,
  en poder del equipo, de los formularios individuales de consentimiento
  informado que la documentación original del proyecto afirmaba
  conservar fuera del repositorio. Esta situación se documentó mediante
  una constancia de regularización firmada por los tres integrantes del
  proyecto
  ([`docs/etica/regularizacion-sus/CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf`](../etica/regularizacion-sus/CONSTANCIA-REGULARIZACION-SUS-BIOPET-2026-08-31.pdf)),
  que **no sustituye** esos formularios individuales — ver
  [`docs/etica/regularizacion-sus/README.md`](../etica/regularizacion-sus/README.md)
  y [`docs/etica/ETHICS.md`](../etica/ETHICS.md) (sección iii).

## 6. Lighthouse (calidad web) — `docs/mediciones/lighthouse/`

| Campo | Detalle |
|---|---|
| Generado por | `npx @lhci/cli autorun` (motor Lighthouse 12.1.0), vía `scripts/run-lighthouse.sh`, contra el frontend servido por el contenedor Docker real (`http://localhost:4200`), nunca contra `ng serve` |
| Responsable | Zaida Taipe Mora |
| Fecha de generación | 2026-08-01, 6 corridas oficiales (3 por cada una de las 2 URLs auditadas) |
| Entrada | `/login` y `/mascotas` (esta última redirige a `/login` sin sesión activa, por diseño del `authGuard` — ver `README.md` de esta carpeta) |
| Transformación aplicada | **Única transformación declarada:** sustitución textual del identificador de usuario del sistema operativo en rutas locales embebidas por la herramienta (`C:\Users\<usuario>\...` → `USER_REDACTED`), documentada explícitamente en `README.md`. Puntajes, fechas, URLs y assertions **no fueron alterados** |
| Verificación de integridad | `SHA256SUMS-ORIGINAL.txt` (hash del original antes de anonimizar) y `SHA256SUMS.txt` (hash de los archivos ya en el repositorio), permitiendo confirmar que la única diferencia es la sustitución declarada |
| Archivos crudos | `raw/*.report.html`, `raw/*.report.json`, `raw/manifest.json`, `raw/assertion-results.json` (31 archivos totales, incluyendo 2 corridas adicionales de `/login` no incluidas en el resumen oficial de 3+3, y duplicados `lhr-<timestamp>.*` con el mismo contenido) |
| Cambios posteriores no cubiertos | Ninguno — la re-corrida del 2026-08-18 (`lhci-20260818-0538-*.json`, 12 archivos, perfil móvil y desktop) ya captura el efecto de los dos fixes de SEO (`meta description`, `robots.txt`) aplicados el 2026-08-17. SEO pasó de 82 a 100 en las 12 corridas. |

## 7. Diccionario de datos vs. procedencia — relación entre ambos documentos

`DATA-DICTIONARY.md` responde **qué significa cada variable**;
`DATA-PROVENANCE.md` (este documento) responde **de dónde salió cada
archivo y qué le pasó antes de llegar al repositorio**. Ningún archivo
crudo mencionado aquí fue editado manualmente más allá de las
transformaciones explícitamente declaradas arriba (agregación estadística
documentada, o anonimización textual puntual en el caso de Lighthouse);
editar un crudo sin declararlo invalidaría la evidencia, siguiendo el
mismo criterio ya aplicado en `docs/mediciones/lighthouse/README.md`.

---

## 8. Procedencia por tabla/figura del informe (Entrega Final v1.0.0)

La siguiente tabla relaciona cada elemento citado en el informe con su
archivo crudo, script/notebook de generación y commit de verificación.

| Elemento del informe | Archivo crudo | Script / Notebook | Commit (git cat-file -t) |
|---|---|---|---|
| tab:anexo-observaciones | `docs/observaciones/OBSERVACIONES.md` | — | `4c5e51d` |
| fig:performance-report-final | `docs/informe/figuras/fred/06-performance-report.png` | `scripts/gen_performance_report_fig.py` | `8343c30` |
| fig:rendimiento-caliente-final | `docs/informe/figuras/fred/Rendimiento-PruebaCaliente.png` | — (captura consola k6) | histórico |
| fig:rendimiento-frio-final | `docs/informe/figuras/fred/Rendimiento-PruebaFria.png` | — (captura consola k6) | histórico |
| fig:sp-acceso-hibrido-final | `docs/informe/figuras/fred/StoreProceduro-AccesoHibrido.png` | — (captura pgAdmin) | histórico |
| fig:postman-sp-final | `docs/informe/figuras/fred/Postman-STOREPROCEDURE.png` | — (captura Postman) | histórico |
| fig:docker-healthy-final | `docs/informe/figuras/fred/Docker-compose-servicios-healthy.png` | — (captura Docker) | histórico |
| fig:docker-digest-final | `docs/informe/figuras/fred/Docker-compose-diges-sha.png` | — (captura Docker) | histórico |
| tab:catalogo-sp | `docs/basedatos/CATALOGOSP.md` | — | pendiente F9 |
| tab:k6-final | `docs/mediciones/perf/REPORT.md` | `scripts/perf-analysis.py` | `3adb230` / `cfdcc0e` |
| tab:produccion-final | `docs/informe/secciones-final/09-despliegue-reproducibilidad.tex` | — | histórico |
| tab:ci-jobs | `.github/workflows/ci.yml` | — | histórico |
| tab:ghcr-final | `.github/workflows/ghcr-publish.yml` | — | histórico |
| tab:zenodo-final | `CITATION.cff` / `README.md` | — | `20671b6` |
| tab:jacoco-final | `Backend/target/site/jacoco/jacoco.xml` | `mvn clean verify` | `0d5cd52` (tag v1.0.0) |
| tab:lighthouse-final | `docs/mediciones/lighthouse/raw/*.json` | `scripts/run-lighthouse.sh` | histórico / `lhci-20260818` |
| fig:lighthouse-final | `docs/mediciones/lighthouse/raw/*.html` | `scripts/run-lighthouse.sh` | histórico / `lhci-20260818` |
| fig:bd-reproducible-final | `docs/informe/figuras/fred/Bd-reproducible.png` | — (captura pgAdmin) | histórico |
| fig:security-headers-final | `docs/informe/figuras/jaime/security-headers.png` | — (captura curl) | histórico |
| fig:zap-final | `docs/mediciones/sec/zap/raw/*.html` | `scripts/run-zap-baseline.sh` | histórico |

Todos los commits listados fueron verificados con `git cat-file -t <hash>` (devuelve `commit`). Los marcados como "histórico" corresponden a evidencia generada en fases anteriores del proyecto; los commits exactos se pueden rastrear mediante `git log --oneline -- <archivo>`.

**Nota sobre la línea histórica del repositorio.** Determinadas
referencias de commit de esta tabla (incluido `0d5cd52`, el commit del
tag `v1.0.0`) documentan la línea histórica del historial de Git previa
a una corrección posterior de sus metadatos, y se conservan aquí como
referencias históricas mientras sigan siendo resolubles con
`git cat-file -t <hash>` — no se sustituyen ni se reescriben. El
estado actual verificable del proyecto debe contrastarse contra el
`HEAD` de `main`/`origin/main`, no contra estas referencias históricas.
El tag `v1.0.0` permanece como referencia histórica inmutable y no se
mueve ni se recrea.

---

## 9. Procedencia de la re-corrida Lighthouse (2026-08-18)

| Campo | Detalle |
|---|---|
| Generado por | `scripts/run-lighthouse.sh` + `lighthouserc.desktop.js` (nuevo, perfil desktop agregado junto al móvil existente) |
| Responsable | Equipo (corrida ejecutada tras los fixes de `frontend/src/index.html` y `frontend/public/robots.txt`) |
| Fecha de generación | 2026-08-18, lote `lhci-20260818-0538-*` |
| Entrada | Mismo contenedor Docker real (`http://localhost:4200`), rutas `/login` y `/mascotas`, ahora en dos perfiles (móvil simulado y desktop) × 2 rutas × 3 corridas = 12 archivos JSON |
| Transformación aplicada | Ninguna — JSON crudo de `@lhci/cli` sin editar |
| Resultado | SEO 100/100 en las 12 corridas (antes 82/100); confirma que la causa raíz identificada en la corrida del 2026-08-01 (meta description + robots.txt) era correcta y completa |
| Archivo de configuración nuevo | `lighthouserc.desktop.js` — no existía en la corrida anterior; se agregó junto con la re-ejecución |

## 9. Pendientes de procedencia (no se inventa lo que falta)

- **Número total de pruebas del backend — RESUELTO (2026-08-31):** el
  repositorio contiene cuatro cifras distintas a lo largo del tiempo
  (109, 166, 189, 205), clasificadas por fecha, commit y evidencia en
  [`TEST-COUNT-PROVENANCE.md`](TEST-COUNT-PROVENANCE.md). La cifra 205,
  usada como resultado final en `docs/informe/secciones-final/*.tex`,
  quedó verificada mediante reproducción independiente de
  `mvn clean verify` sobre el commit exacto del tag `v1.0.0`, archivada
  en [`docs/mediciones/sec/reproduccion-v1.0.0/`](sec/reproduccion-v1.0.0/).
  Ya no es una cifra pendiente.
- **Hit ratio de Redis:** no hay todavía una captura de `INFO stats`
  (`keyspace_hits`/`keyspace_misses`) con procedencia documentada; solo
  existe evidencia de TTL y `DBSIZE`. Sigue siendo la única brecha real
  de procedencia de datos de medición en este documento.
- **PRISMA (trabajos relacionados):** CERRADO. Los 10 estudios primarios
  (3 de Zaida, 4 de Jaime, 3 de Fred) tienen procedencia documentada:
  Zaida en `docs/informe/borradores/zaida/estudios-primarios-zaida.md`,
  Jaime en `docs/informe/borradores/jaime/trabajos-relacionados.md` y
  `referencias-candidatas.md`, Fred en
  `docs/investigacion/handoff-fred-trabajos-relacionados.md` y
  `handoff-fred-referencias.bib` (11 DOI verificados uno a uno,
  2026-08-17). Las 24 referencias nuevas de los tres bloques ya están
  integradas en `docs/informe/referencias.bib` (39 entradas totales).
