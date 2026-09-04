# OWASP ZAP — BIOPET Backend (Entrega Final v1.0.0)

Este documento cubre **dos corridas distintas**, claramente separadas:

- **A. Baseline histórico** (sección inmediatamente debajo): sin autenticación, contra el backend HTTP local sin TLS. Se conserva sin alterar como registro histórico.
- **B. [Corrida autenticada local (Fase Jaime 2B)](#b-corrida-autenticada-local-fase-jaime-2b)**: con autenticación real, contra el backend HTTPS/TLS local. Evidencia nueva, no sustituye ni borra la anterior.

## A. Baseline histórico (sin autenticación)

### Resumen ejecutivo

| Campo | Valor |
|---|---|
| Fecha de ejecución (UTC) | `2026-08-18T02:53:21Z` (corregido; el JSON archivado (`zap-baseline-report.json`, campo `@generated`) y `RUN-METADATA.txt` de la corrida realmente archivada coinciden en 18 de agosto, no 17 como afirmaba una versión anterior de esta tabla) |
| Versión de ZAP | `2.17.0` (imagen `ghcr.io/zaproxy/zaproxy:stable`) |
| Target | `http://backend:8080` (contenedor `biopet-backend`, dentro de la red Docker Compose `entrega-final-biopet_default`) |
| Comando exacto | ver [`RUN-METADATA.txt`](RUN-METADATA.txt) (generado automáticamente por el script en cada corrida) |
| Alertas **altas** | **0** |
| Alertas **medias** | **0** |
| Alertas **bajas** | **0** |
| Alertas **informativas** | **1** (`Non-Storable Content`, riesgo `Informational`, confianza `Medium`, 2 instancias) |
| Código de salida de `zap-baseline.py` | `0` |
| Objetivo de la Entrega Final (cero hallazgos de severidad alta) | **Cumplido** |

### Cómo se levantó el sistema (reproducible, sin URL inventada)

`scripts/run-zap-baseline.sh` (nuevo en esta fase) hace lo siguiente, en
este orden, cada vez que se ejecuta:

1. Verifica que el daemon de Docker responda.
2. Si `biopet-backend` no está ya `healthy`, ejecuta
   `docker compose up -d postgres redis backend` (el mismo
   `docker-compose.yml` que ya usa el resto del proyecto) y espera, con
   reintentos reales (no un `sleep` fijo), a que
   `docker inspect biopet-backend --format '{{.State.Health.Status}}'`
   devuelva `healthy` — el mismo healthcheck ya definido en
   `docker-compose.yml` (`wget ... /actuator/health | grep UP`).
3. Detecta el nombre real de la red de Docker Compose creada para este
   proyecto (`entrega-final-biopet_default`) inspeccionando el propio
   contenedor `biopet-backend`, en vez de asumir un nombre fijo.
4. Ejecuta el contenedor oficial de ZAP **en esa misma red**, para que
   pueda resolver `backend` por su nombre de servicio DNS de Docker
   Compose — el mismo nombre que usan `postgres`/`redis`/`frontend` entre
   sí — y apunta el escaneo a `http://backend:8080`, el puerto real que el
   propio `docker-compose.yml` expone para el backend. No se inventó
   ninguna URL: es el mismo backend contra el que corre
   `scripts/security-evidence.sh`.

**Nota técnica documentada (no oculta):** en Git Bash/MSYS (Windows), el
montaje de volumen `-v host:/zap/wrk/:rw` requirió `MSYS_NO_PATHCONV=1`
para evitar que la ruta de destino dentro del contenedor (`/zap/wrk`) fuera
reescrita como una ruta de Windows por el propio shell — sin esa variable,
ZAP fallaba con `"the directory '/zap/wrk' is not mounted"`. El script ya
la incluye; queda documentado aquí por si se reproduce en otro entorno
Windows.

### Comando exacto de esta ejecución

```bash
scripts/run-zap-baseline.sh
```

Que internamente ejecutó:

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
    --network entrega-final-biopet_default \
    -v "<repo>/docs/mediciones/sec/zap:/zap/wrk/:rw" \
    -t ghcr.io/zaproxy/zaproxy:stable \
    zap-baseline.py \
    -t http://backend:8080 \
    -r zap-baseline-report.html \
    -x zap-baseline-report.xml \
    -J zap-baseline-report.json \
    -I
```

(`-I`: no convertir WARN en fallo del código de salida; los reportes HTML/XML/JSON
sí registran igualmente cualquier WARN/FAIL real, nada se oculta por esa opción.)

### Reportes generados (evidencia real, sin editar)

- [`zap-baseline-report.html`](zap-baseline-report.html) — reporte HTML completo, navegable.
- [`zap-baseline-report.xml`](zap-baseline-report.xml) — reporte XML completo.
- [`zap-baseline-report.json`](zap-baseline-report.json) — reporte JSON completo (LHR-equivalente de ZAP).
- [`RUN-METADATA.txt`](RUN-METADATA.txt) — fecha, imagen, target, red Docker y comando exacto de la última corrida (regenerado automáticamente por el script en cada ejecución).
- `zap.yaml` — plan de automatización que `zap-baseline.py` genera internamente (Automation Framework de ZAP 2.x) para ejecutar el baseline; se conserva como parte de la evidencia cruda de la herramienta.

### Interpretación de los hallazgos

**66 reglas pasivas en PASS, 0 FAIL, 1 WARN.** El único hallazgo,
`Non-Storable Content` (`pluginid 10049`), es de **riesgo Informational**
según el propio JSON de ZAP (`"riskcode": "0"`, `"riskdesc": "Informational
(Medium)"`) — ZAP mismo no lo clasifica como una vulnerabilidad de
seguridad explotable, sino como una observación de rendimiento/cacheo.

**Análisis: ¿es real, falso positivo, o limitación del entorno?**

Es un **falso positivo respecto al objetivo de seguridad**, no un hallazgo
que deba corregirse:

- Las dos instancias marcadas son `GET http://backend:8080` (raíz, sin
  ruta) y `GET http://backend:8080/robots.txt` — ambas devuelven **401
  Unauthorized** en este backend (no hay contenido público en la raíz de
  la API; todo está protegido por Spring Security salvo `/api/auth/**`).
  La cabecera detectada como "problema" es `Cache-Control: no-store` en
  esas dos respuestas `401`.
- La recomendación de ZAP es literalmente permitir que esas respuestas se
  puedan cachear, para mejorar el rendimiento. Eso es **lo opuesto** de lo
  que conviene hacer con una respuesta de error de autenticación: no
  cachear una respuesta `401` (que podría depender del estado de sesión
  del cliente que la solicitó) es una práctica de seguridad deliberada,
  no un descuido. Añadir cacheabilidad ahí introduciría el riesgo de que
  un proxy intermedio sirva una respuesta `401` obsoleta a un cliente que
  sí debería estar autenticado.
- No es tampoco una limitación del entorno local: el comportamiento
  (`no-store` en respuestas 401) es el mismo mecanismo ya documentado y
  probado en `docs/mediciones/sec/A05-security-headers.md`, no un artefacto
  de correr contra `localhost`/Docker.

**Decisión:** no se modifica código de producción por este hallazgo. Se
documenta aquí como hallazgo menor, con su análisis, en vez de ocultarlo o
borrarlo del reporte.

### Por qué el escaneo cubrió solo 2 endpoints (limitación real, documentada)

El "spider" no autenticado de ZAP solo pudo descubrir 2 endpoints (`/` y
`/robots.txt`), ambos devolviendo `401`. Esto **no es un error del script
ni del escaneo**: es el comportamiento esperado de una API REST donde,
salvo `/api/auth/login`, `/api/auth/registro` y `/api/auth/refresh`, **todo**
requiere un JWT válido (`SecurityConfig`, ya documentado en
`A01-access-control.md`/`A07-authentication.md`). El log de la propia
ejecución lo confirma explícitamente:

```
Job spider error accessing URL http://backend:8080 status code returned : 401 expected 200
```

Es decir, ZAP intentó y no pudo "entrar" a la API sin autenticación — el
resultado correcto para un backend con control de acceso por defecto
(`deny by default`), no una limitación que oculte hallazgos: no hay
superficie pública adicional que un atacante no autenticado pueda alcanzar
más allá de lo ya cubierto por A01/A03/A05/A07 con `curl` real en
`docs/mediciones/sec/raw/`.

**Alcance no cubierto por esta corrida — implementado posteriormente:** un
ZAP baseline **autenticado** (usando un contexto de ZAP con las cookies de
sesión ya documentadas en `A07-authentication.md`) permitiría escanear los
endpoints protegidos (`/api/mascotas`, `/api/usuarios`, `/api/citas`, etc.)
con las reglas pasivas de ZAP. No se implementó en esta fase por alcance
(requiere manejar credenciales de prueba dentro del propio contenedor de
ZAP, un mecanismo distinto al ya usado por `scripts/security-evidence.sh`).
**Este trabajo futuro ya se realizó** — ver
[sección B, más abajo](#b-corrida-autenticada-local-fase-jaime-2b) — sin
modificar esta corrida histórica ni sus reportes originales.

### Correcciones realizadas

**Ninguna fue necesaria.** El único hallazgo (`Non-Storable Content`,
severidad Informational) fue analizado y determinado como comportamiento
deliberado y correcto (ver sección anterior), no un defecto a corregir. No
hubo hallazgos de severidad alta, media ni baja en esta ejecución — el
objetivo de la Entrega Final (cero hallazgos de severidad alta) se cumplió
sin necesidad de cambios de código.

### Reproducción

```bash
# Requiere Docker Desktop iniciado
scripts/run-zap-baseline.sh

# Para no apagar el stack al terminar (ya usado por otras evidencias):
scripts/run-zap-baseline.sh --keep-stack
```

Variables de entorno opcionales: `ZAP_IMAGE` (por defecto
`ghcr.io/zaproxy/zaproxy:stable`), `ZAP_TARGET` (por defecto
`http://backend:8080`, dentro de la red Docker del propio proyecto).

---

## B. Corrida autenticada local (Fase Jaime 2B)

Escaneo **autenticado, pasivo** (sin *active scan*) contra el stack
**LOCAL** de este repositorio con **HTTPS/TLS real**
(`docker-compose.tls.yml`, backend en `https://backend:8443` dentro de la
red Docker Compose del propio proyecto). **No usa Render ni BIOPET-V2**
(ver `docs/informe/secciones-final/09-despliegue-reproducibilidad.tex`
para la URL de Render real de este repositorio, distinta de las de
BIOPET-V2, un repositorio separado — `README.md` raíz del proyecto).

### Resumen ejecutivo

| Campo | Valor |
|---|---|
| Fecha de ejecución (UTC) | `2026-09-03T21:38:48Z` (`RUN-METADATA-AUTH-LOCAL.txt`) |
| Versión de ZAP | `2.17.0` (imagen `ghcr.io/zaproxy/zaproxy:stable`) |
| Target | `https://backend:8443` (HTTPS/TLS local, contenedor `biopet-backend`, red `entrega-final-biopet_default`) |
| Tipo de escaneo | Automation Framework: autenticación + peticiones GET autenticadas + *passive scan*. **Sin active scan.** |
| Endpoint de login | `POST /api/auth/login` (JSON) |
| Endpoint de verificación | `GET /api/usuarios/me` (poll, `loggedInRegex` sobre `"email"`) |
| Mecanismo de sesión | Cookie (`access_token`, `HttpOnly`+`Secure`+`SameSite=Strict`) — mecanismo nativo de ZAP AF (`sessionManagement.method: cookie`) |
| Total de endpoints alcanzados | **9** (`insight.endpoint.total` del propio JSON de ZAP: las 7 rutas GET protegidas + `POST /api/auth/login`, deduplicado por ZAP a nivel de endpoint). El texto de consola de `outputSummary` («Total of 11 URLs») usa un contador **distinto**, sin deduplicar (incluye la repetición de `POST /api/auth/login` — se invoca dos veces por diseño: una vez para el login explícito y otra vez cuando el ciclo de verificación por *poll* re-confirma la sesión — más alguna otra repetición interna de ZAP); no son la misma métrica y no deben sumarse ni compararse directamente. |
| Distribución de códigos HTTP | `insight.code.2xx = 93`, `insight.code.4xx = 6` (estadísticas agregadas internas de ZAP — **no** un desglose por URL, y su denominador exacto no lo expone ningún reporte generado, ni el HTML ni el XML ni el JSON). Una verificación de reconciliación de solo lectura (exportando el historial HTTP real de una corrida equivalente a HAR, sin generar evidencia nueva ni tocar los reportes ya archivados) mostró **9/9 mensajes en 200** (las 7 rutas GET + 2 llamadas a `POST /api/auth/login`), **0 en 4xx** — es decir, el 4xx que reportan los `insights` **no corresponde a ninguna de las 7 rutas protegidas objetivo ni al login**, ambos confirmados en 200 por partida doble (aserción nativa `responseCode: 200` con 0 diferencias, y esta reconciliación HAR). No fue posible identificar la URL exacta detrás de ese 4xx a partir de los artefactos retenidos (los reportes HTML/XML/JSON de ZAP no incluyen un log de mensajes crudo); es plausiblemente una petición incidental del propio motor de ZAP durante la inicialización del contexto (por ejemplo, una sonda a la URL raíz del contexto, que este backend rechaza con 401 por defecto — mismo comportamiento ya documentado en la corrida histórica de la sección A), pero esto **no está confirmado** y se documenta aquí como límite de trazabilidad, no como un hecho verificado. |
| Alertas **altas** | **0** |
| Alertas **medias** | **0** |
| Alertas **bajas** | **0** |
| Alertas **informativas** | **2 tipos, 3 instancias** — `Authentication Request Identified` (x1) y `Session Management Response Identified` (x2), ambas sobre `POST /api/auth/login`, riesgo `Informational`, `riskcode=0` — son observaciones de ZAP reconociendo el flujo de autenticación, no vulnerabilidades |
| Código de salida de `zap.sh` | `0` |
| **¿Rutas protegidas con 2xx confirmado?** | **SÍ** — las 7 rutas GET protegidas (ver tabla abajo) y el login devolvieron exactamente `200`, verificado con la aserción nativa `responseCode: 200` de ZAP AF en cada petición (0 diferencias registradas) |

### Rutas GET autenticadas alcanzadas (todas con 200 confirmado)

| Ruta | Rol requerido en el código |
|---|---|
| `GET /api/usuarios/me` | Cualquier autenticado |
| `GET /api/mascotas` | ADMIN/VETERINARIO/AUXILIAR/DUENO |
| `GET /api/mascotas/resumen-especies` | ADMIN/VETERINARIO/AUXILIAR/DUENO |
| `GET /api/citas` | ADMIN/VETERINARIO/AUXILIAR/DUENO |
| `GET /api/consultas` | ADMIN/VETERINARIO/AUXILIAR/DUENO |
| `GET /api/vacunas` | ADMIN/VETERINARIO/AUXILIAR/DUENO |
| `GET /api/usuarios` | ADMIN |

`/api/externa/especies` se excluyó deliberadamente del alcance
(`excludePaths` en `zap-authenticated-local.yaml`) por ser un proxy a una
API externa real de terceros. No se ejecutó ningún POST/PUT/PATCH/DELETE.

### Cuenta usada

Cuenta local sembrada por este mismo repositorio
(`db/seed.sql`/`DataInitializer.java`, **no** la cuenta `demo@biopet.com`
mencionada como referencia externa, que pertenece al contexto BIOPET-V2 y
no está documentada aquí). Las credenciales se pasaron exclusivamente
como variables de entorno (`ZAP_USERNAME`/`ZAP_PASSWORD`) al ejecutar
`scripts/run-zap-authenticated-local.sh`; no aparecen en ningún archivo
versionado.

### Configuración y script

- [`zap-authenticated-local.yaml`](zap-authenticated-local.yaml) — plan
  de ZAP Automation Framework versionado. Los campos de credenciales
  contienen los tokens literales `__ZAP_USERNAME__`/`__ZAP_PASSWORD__`,
  **nunca un valor real**; el script los sustituye en una copia temporal
  fuera del repositorio (borrada automáticamente al terminar) a partir de
  las variables de entorno.
- [`scripts/run-zap-authenticated-local.sh`](../../../../scripts/run-zap-authenticated-local.sh)
  — valida el entorno (Docker, backend `healthy` por HTTPS), sustituye
  credenciales solo en memoria/temporal, ejecuta ZAP, y **se detiene con
  código de salida distinto de 0 si cualquier petición autenticada no
  devuelve el código esperado** (no presenta como válida una evidencia
  donde la sesión no se autenticó realmente).

### Reportes generados (evidencia real, sin editar)

- [`zap-authenticated-local-report.html`](zap-authenticated-local-report.html)
- [`zap-authenticated-local-report.xml`](zap-authenticated-local-report.xml)
- [`zap-authenticated-local-report.json`](zap-authenticated-local-report.json)
- [`RUN-METADATA-AUTH-LOCAL.txt`](RUN-METADATA-AUTH-LOCAL.txt) — metadatos seguros (sin contraseña/JWT/cookie) de la corrida.
- [`SHA256SUMS-AUTH-LOCAL.txt`](SHA256SUMS-AUTH-LOCAL.txt) — SHA-256 de los seis artefactos anteriores más el propio script y YAML.

### Verificación explícita de que no hay secretos en la evidencia

Se buscó explícitamente, en los cuatro reportes y en el script, la
contraseña real, cualquier JWT (`eyJ...`) y cualquier `Set-Cookie`
completo: **ninguno de los tres aparece**. El único dato incidental
presente es el **correo** de la cuenta administrativa usada
(`userValue=admin@biopet.ec`), registrado automáticamente por la propia
regla pasiva `Authentication Request Identified` de ZAP dentro de
`zap-authenticated-local-report.json`/`.xml` — no es la contraseña, y ese
mismo correo ya está versionado en texto plano en `db/seed.sql` y
`DataInitializer.java` como cuenta de desarrollo, por lo que no introduce
ninguna filtración nueva.

### Limitaciones de esta corrida

- Es un escaneo **pasivo**, no un *active scan*: no se intentaron ataques
  activos (inyección, fuzzing) contra ninguna ruta. El objetivo de esta
  fase era demostrar cobertura autenticada real, no maximizar hallazgos.
- Cubre solo las 7 rutas GET listadas arriba (más login/verificación), no
  la superficie completa de la API (quedan fuera, deliberadamente, todas
  las rutas de escritura POST/PUT/PATCH/DELETE y `/api/externa/**`).
- Ejecutado contra el entorno **local** con certificado TLS autofirmado
  de desarrollo (`scripts/generate-dev-keystore.ps1`/`.sh`), no contra el
  despliegue de Render.
- Las alertas informativas (`Authentication Request Identified`,
  `Session Management Response Identified`) son ZAP reconociendo
  correctamente el propio flujo de autenticación, no hallazgos de
  seguridad — se documentan aquí por transparencia, no como defectos.
- Los reportes generados (HTML/XML/JSON, plantillas estándar de ZAP) no
  incluyen un log crudo de mensajes por URL: los `insights` agregados
  (`insight.code.2xx`/`insight.code.4xx`) no son rastreables a una
  petición concreta a partir de esos tres archivos. No se pudo confirmar
  con certeza el origen exacto del 6 % de respuestas 4xx que reporta esa
  estadística agregada (ver tabla de resumen ejecutivo); sí se confirmó,
  por partida doble, que ese 4xx no afecta a ninguna de las 7 rutas
  protegidas objetivo ni al login.

### Reproducción

```bash
# Requiere Docker Desktop iniciado y el stack local con TLS levantado:
#   powershell -ExecutionPolicy Bypass -File scripts/generate-dev-keystore.ps1
#   docker compose -f docker-compose.yml -f docker-compose.tls.yml up -d postgres redis backend
ZAP_USERNAME='admin@biopet.ec' ZAP_PASSWORD='<contraseña real>' scripts/run-zap-authenticated-local.sh
```
