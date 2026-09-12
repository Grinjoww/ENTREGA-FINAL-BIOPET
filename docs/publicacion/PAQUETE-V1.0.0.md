# Paquete de release v1.0.0 — BIOPET (F18)

Preparación del paquete de software **sin publicarlo**. Este documento es el
checklist "listo para que el humano publique manualmente" en GHCR y Zenodo.
Ninguna acción aquí se ejecuta en esta rama; todo es preparación verificable.

## 1. Metadatos necesarios

| Campo | Valor real actual | Acción para v1.0.0 |
|---|---|---|
| Título | "BIOPET — Sistema Integral de Gestión Veterinaria" (`CITATION.cff`) | mantener |
| Autores | Fred Adrián Beltrán Montiel, Jaime Josué Mariscal Cabrera, Zaida Melissa Taipe Mora (afiliación: UTEQ) | mantener |
| Versión | `0.9.0-rc` (`CITATION.cff`, `git describe`, k6 filenames) | **cambiar a `1.0.0`** |
| `repository-code` | `https://github.com/JirachinG19Stdio/PFC-VET-ENTR3-v0.9.0-rc` (repo anterior) | **corregir a `https://github.com/Grinjoww/ENTREGA-FINAL-BIOPET`** |
| Licencia | MIT (`LICENSE`, `CITATION.cff`) | mantener |
| DOI | software `10.5281/zenodo.21988746`, dataset `10.5281/zenodo.21988785` (`CITATION.cff`, bloque `identifiers`) | ya asignado — mantener |
| Fecha de release | `date-released: 2026-07-24` (v0.9.0-rc) | actualizar a fecha del tag v1.0.0 |
| Keywords | 8 keywords en `CITATION.cff` | mantener |

> Hallazgo real: `CITATION.cff` todavía apunta al repositorio anterior
> (`JirachinG19Stdio/PFC-VET-ENTR3-v0.9.0-rc`). La URL correcta del proyecto
> actual es `https://github.com/Grinjoww/ENTREGA-FINAL-BIOPET` (verificado con
> `git remote -v`). Esto debe corregirse ANTES de publicar.

## 2. Licencia

- **MIT** ya versionada: `LICENSE` (21 líneas, Copyright (c) 2026 Equipo
  BIOPET, los 3 integrantes nombrados).
- Verificada coherente con `CITATION.cff` (`license: MIT`) y con el widget de
  licencia de GitHub.
- **Acción**: ninguna (ya lista). Al archivar en Zenodo, seleccionar MIT
  (SPDX: `MIT`) para que coincida con el archivo.

## 3. Contenido del paquete (incluir/excluir)

### Incluir (fuente + evidencia, sin secretos)

| Ruta | Por qué |
|---|---|
| `Backend/` | código fuente Java/Spring Boot + Dockerfile + migraciones Flyway |
| `frontend/` | código fuente Angular + Dockerfile + nginx |
| `db/` | schema, seed, roles, `db/procs/*.sql` (6 SP) |
| `docs/` | ADR, SRS, matriz de trazabilidad, mediciones (perf/sec/zap), despliegue, checklists |
| `k6/` | scripts de benchmark + README |
| `scripts/` | scripts reproducibles (perf-analysis, auditorías, versions) |
| `Makefile`, `docker-compose*.yml`, `render.yaml`, `.env.example` | reproducibilidad del entorno |
| `LICENSE`, `CITATION.cff`, `README.md` | metadatos y licencia |

### Excluir (nunca en el paquete)

| Ruta | Razón |
|---|---|
| `.env` | secretos reales (credenciales BD, JWT) — gitignored |
| `Backend/certs/*.p12` | keystore TLS local — gitignored |
| `docs/mediciones/sec/raw/` | evidencia cruda no sanitizada — gitignored |
| `Backend/target/`, `frontend/node_modules/` | artefactos de build |
| `docs/postman/*.local.postman_environment.json` | environments locales con credenciales |

Verificación usada: `git ls-files` + `.gitignore` (los excluidos no están
versionados, por lo que un `git archive` del tag v1.0.0 los excluye
automáticamente).

## 4. Datos necesarios para GHCR (contenedor backend)

- **Imagen**: construir `Backend/Dockerfile` (multi-stage Maven 3.9 →
  Temurin 21 JRE Alpine; jar `biopet-backend-0.1.0.jar`, EXPOSE 8080).
- **Nombre real**: `ghcr.io/grinjoww/entregafinal-biopet-backend` (sin
  guion entre "entrega" y "final" — nombre ya fijado en este documento
  antes de automatizar la publicación; no se cambió).
- **Publicación automatizada (ya lista, workflow nuevo, `.github/workflows/ghcr-publish.yml`)**:
  - **Estado actual (verificado):** el tag Git `v1.0.0` fue creado
    originalmente el 2026-08-18 sobre el commit histórico `0d5cd52`, cierre
    de la Entrega Final original. Durante el cierre de esta recalificación,
    ese mismo tag se actualizará para identificar el commit final evaluado,
    sin cambiar su nombre ni crear un tag adicional (ver `README.md`,
    sección "Versiones del proyecto", y
    `docs/informe/secciones-final/09-despliegue-reproducibilidad.tex`,
    sección "Estado de publicación del tag"). **No debe confundirse con
    un GitHub Release**: a la
    fecha de esta nota, ningún GitHub Release ha sido publicado en la
    página *Releases* del repositorio — son dos artefactos distintos de
    GitHub.
  - **Procedimiento previo a la creación del tag `v1.0.0`** (aplicable
    durante la fase pre-release histórica, cuando el tag todavía no
    existía): disparo manual desde GitHub → pestaña *Actions* →
    "Publicar imagen backend en GHCR" → *Run workflow*. Publica solo una
    etiqueta técnica `sha-<7 caracteres del commit>` (nunca `latest` ni un
    número de versión, para no poder confundirse con un release real). Esta
    instrucción se conserva únicamente como registro del flujo utilizado
    antes de crear la etiqueta.
  - **Procedimiento ante la publicación del tag** (ya aplicable, puesto que
    el tag `v1.0.0` existe): el propio `git push --tags` dispara el
    workflow automáticamente y publica además `1.0.0` y `latest` en GHCR
    — esto es independiente de si existe o no un GitHub Release.
  - El workflow usa `GITHUB_TOKEN` (automático, sin secretos que
    configurar) con permisos `contents: read` + `packages: write`, ya
    declarados en el propio archivo del workflow.
  - **Actualización:** el workflow ya se ejecutó al menos una vez de
    forma manual y produjo un digest real, documentado en `README.md`
    (sección "GHCR"): imagen `ghcr.io/grinjoww/entregafinal-biopet-backend`,
    etiqueta `sha-fe2f033`, digest
    `sha256:ef1e857a95a307a115ebe01599a41506eab824808b70a3c8e317dcc55bef5163`.
    Ese digest también está registrado en `CITATION.cff` como
    `identifier` de tipo `other`. **Pendiente sin confirmar en este
    entorno:** que la publicación automática de las etiquetas `1.0.0` y
    `latest`, disparada por el propio tag `v1.0.0`, ya haya terminado
    (README.md lo señala explícitamente como no verificado localmente).
- **Requisitos ya cubiertos por el workflow**: `GITHUB_TOKEN` con scope
  equivalente a `write:packages` (vía el bloque `permissions:` del propio
  YAML, no un PAT manual); repositorio/paquete público para que terceros
  puedan hacer `docker pull` sin autenticarse (a verificar en GitHub →
  Settings → el paquete generado, visibility "Public").

## 5. Datos necesarios para Zenodo

- **Tag**: `v1.0.0` fue creado originalmente el 2026-08-18 sobre el commit
  histórico `0d5cd52`; durante el cierre de esta recalificación se
  actualizará para identificar el commit final evaluado (ver
  `README.md`, sección "Versiones del proyecto"). Nota: esto es distinto
  de un **GitHub Release**, que a la fecha de esta actualización todavía
  no ha sido publicado.
- **Conexión y archivado**: **actualización posterior a la redacción
  original de esta sección** — el repositorio ya fue archivado en Zenodo
  y ambos DOI ya fueron asignados: software
  (`10.5281/zenodo.21988746`) y dataset de evidencias
  (`10.5281/zenodo.21988785`), ambos presentes en `CITATION.cff`. No hay
  en este entorno evidencia directa de si el archivado se disparó desde
  un GitHub Release o desde una carga manual a Zenodo.
- **DOI ya pegado** en `CITATION.cff` (campo `identifiers`), `README.md`
  y `docs/checklists/fair.md` (ítem F1) — verificado por búsqueda directa
  en los tres archivos.

## 6. Checklist final "listo para publicar manualmente"

Marcar TODOS antes de publicar (los 3 primeros son de esta rama y ya están):

- [x] Checklist FAIR completo con ítems pendientes marcados explícitamente
      (`docs/checklists/fair.md`, F17)
- [x] Provenance handoff para Zaida (`docs/mediciones/handoff-fred-provenance.md`, F19)
- [x] Paquete de release preparado (este documento, F18)
- [x] `CITATION.cff` corregido: `version: 1.0.0`, `repository-code` =
      `https://github.com/Grinjoww/ENTREGA-FINAL-BIOPET`, 3 autores reales
      con correo institucional, `orcid` de los tres. `date-released` debe
      leerse siempre directamente del propio archivo (hoy no es
      `2026-08-18`: ese valor quedó obsoleto en cuanto `CITATION.cff` se
      actualizó a la fecha de cierre vigente de la recalificación; no se
      reproduce aquí un valor fijo para no quedar desactualizado de nuevo
      con el próximo commit).
- [x] Workflow de publicación GHCR listo
      (`.github/workflows/ghcr-publish.yml`) y ya ejecutado al menos una
      vez con éxito: existe un digest real (ver sección 4 y `README.md`,
      sección "GHCR"). Pendiente sin confirmar en este entorno: que la
      publicación automática de `1.0.0`/`latest` disparada por el tag ya
      haya terminado.
- [ ] Tag `v1.0.0` actualizado para identificar el commit final evaluado
      de esta recalificación. **Estado real a la fecha de esta revisión:**
      el tag fue creado originalmente el 2026-08-18 sobre el commit
      histórico `0d5cd52` y **todavía no se ha movido** (verificable con
      `git rev-parse v1.0.0^{}` y comparando contra `git rev-parse HEAD`
      de `main`); se moverá una sola vez, al cierre, cuando las tres
      ramas de corrección estén mergeadas. **Distinto de un GitHub
      Release**, que tampoco se ha publicado en la página *Releases* del
      repositorio.
- [ ] CI en verde sobre el tag ya movido al commit final (6 jobs:
      backend-test, frontend-build, traceability, sql-audit,
      security-static, zap-baseline) — no confirmable hasta que el tag
      se mueva
- [ ] Confirmar si el archivado en Zenodo se disparó desde un GitHub
      Release o desde una carga manual (ver sección 5); el DOI en sí ya
      existe
- [x] DOI pegado en `CITATION.cff`, `README.md` y
      `docs/checklists/fair.md` — verificado por búsqueda directa en los
      tres archivos (ver sección 5)
- [x] Imagen backend publicada en GHCR con digest real documentado
      (ver sección 4 y `README.md`, sección "GHCR"). **No verificado en
      este entorno:** ejecución real de `docker pull` contra ese digest
      (sin acceso a Internet saliente desde este entorno de auditoría)
- [ ] URL real de Render registrada en `docs/despliegue/DEPLOYMENT.md`
      (ya está registrada — ver esa sección 5.1/5.3 de ese documento) y
      healthcheck verificado en vivo con `curl -I .../actuator/health`
      — esta segunda parte sigue pendiente, tal como el propio
      `DEPLOYMENT.md` lo declara
- [ ] Notificar a Jaime (ADRs) y Zaida (matriz + provenance + DATA-PROVENANCE.md)

## 7. Fuera de alcance de esta rama (no inventar aquí)

- No se crea el tag ni se hace push de imágenes (lo hace el humano).
- No se crea el DOI ni se publica nada.
- No se toca `DATA-PROVENANCE.md` (lo consolida Zaida, Z12).
- No se inventa la URL de Render (pendiente del deploy real del owner).