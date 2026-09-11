# CHANGELOG-REQ.md

Registro de cambios a los requisitos del SRS de BIOPET desde la Entrega 1A,
siguiendo la convención Keep a Changelog adaptada a requisitos (bloque A.3.4
de la Guía de la Tercera Entrega). No reemplaza a `docs/requisitos/cambios/CAMBIOS-SRS.md`
(que narra el contexto general de la migración de pila ASP.NET → Spring Boot);
este archivo es el registro formal, fila por requisito, exigido por A.3.4.

> **Los encabezados `[vX.Y.Z]` de este archivo son versiones DOCUMENTALES del
> SRS, no etiquetas de Git.** Una entrada fechada bajo `[v1.0.0]` describe una
> revisión del documento hecha mientras la versión documental seguía siendo
> v1.0.0; **no** significa que el tag Git `v1.0.0` se haya creado, movido ni
> reemitido en esa fecha. El tag Git `v1.0.0` es inmutable y apunta desde su
> creación al commit `0d5cd525ce648cca7219da204e16fa622e671a87`
> (2026-08-18); puede comprobarse con `git rev-parse v1.0.0^{}`. Cada entrada
> indica, cuando aplica, el commit real sobre el que se hizo la revisión.

## [v1.0.0 — revisión documental] - 2026-09-11 (corrección integral del SRS tras la retroalimentación del docente-director)

> Revisión del documento sobre el commit `8130ee00b0083808fc60567018589e38302b375d`.
> **No se creó, movió ni reemitió ninguna etiqueta de Git en esta fecha.** El tag
> histórico `v1.0.0` sigue apuntando a `0d5cd525ce648cca7219da204e16fa622e671a87`.

Revisión documental completa del SRS previa a la firma, hecha sobre el commit
`8130ee00b0083808fc60567018589e38302b375d` y verificada requisito por
requisito contra el código real del repositorio. **No se modificó código de
backend ni de frontend**, no se movió el tag histórico `v1.0.0` y no se
renumeró ningún requisito existente. Los estados que empeoran lo hacen porque
la revisión encontró que la evidencia anterior no sostenía el estado
declarado, no porque el sistema haya retrocedido.

### Added

- **Plantilla única de requisito** (sección 1.3.2) aplicada a los 47 bloques
  de las secciones 3.1 y 3.2: Tipo, Prioridad, Enunciado, Rationale,
  Criterios de aceptación, Método de verificación, Trazabilidad, Estado y
  Observaciones.
- **Criterios de aceptación verificables en todos los requisitos.** Antes
  sólo `REQ-F-017` y `REQ-F-022` los tenían; ahora los declaran los 47
  bloques, en forma *Dado / cuando / entonces*, con el artefacto que respalda
  cada criterio.
- **Taxonomía de estados** (sección 1.3.1) con exactamente tres valores:
  `verificado`, `implementado` y `pendiente`.
- **REQ-F-026 — Recuperación de contraseña** (`pendiente`). No existe
  implementación: la búsqueda de `recuperar|reset|forgot|olvid` sobre
  `Backend/src` y `frontend/src` no devuelve coincidencias. Sin historia ni
  caso de uso todavía.
- **REQ-NF-014 — Comportamiento ante indisponibilidad de Redis**
  (`pendiente`). Especifica la política exigida (*fail-closed* para la
  verificación de revocación, degradación de la caché a consulta directa) y
  documenta el comportamiento real actual: `RedisConnectionFailureException`
  escapa de `JwtAuthenticationFilter` y produce un `500` sin `ProblemDetail`.
- **REQ-NF-015 — Aislamiento de datos por propietario** (`implementado`),
  requisito transversal que formaliza en un solo lugar la regla que antes
  estaba dispersa entre `REQ-F-009`, `REQ-F-010`, `REQ-F-015` y `REQ-F-024`.
- **REQ-NF-016 — Política de contraseñas** (`implementado`): documenta la
  política realmente implementada (`@Size(min = 8, max = 80)` + BCrypt coste
  12 + ningún DTO de respuesta con contraseña), sin atribuirle reglas de
  complejidad o caducidad que el sistema no aplica. No alcanza `verificado`
  porque el límite superior de 80 caracteres no tiene prueba automatizada.
- **REQ-NF-017 — Respaldo y recuperación de la base de datos**
  (`implementado`): formaliza el procedimiento de `docs/despliegue/BACKUP.md`.
  El volcado `docs/despliegue/ejemplo-backup-20260817.sql` se verificó de
  forma independiente (6 tablas, 6 secuencias, 4 triggers y 7 rutinas de este
  proyecto; `usuarios`: 1 fila). No alcanza `verificado` porque la ejecución
  de la restauración consta sólo como narración, sin log crudo archivado.
- **REQ-NF-018 — Accesibilidad de la interfaz** (`verificado`): umbral
  Lighthouse Accessibility ≥ 90, medido 91 por lectura directa del campo
  `categories.accessibility.score` de los 12 archivos del 2026-08-18. **No**
  declara conformidad WCAG en ningún nivel. El criterio de archivado se apoya
  en `lhci-20260818-0538.meta.txt`, no en los `SHA256SUMS*.txt`, que
  corresponden a la corrida del 2026-08-01.
- **Sección 2.7 — Interfaces externas** (usuario, software, hardware y
  comunicaciones), construida desde el código y la configuración reales.
- **Sección 2.8 — Matriz de permisos por rol y operación**, derivada
  exclusivamente de las anotaciones `@PreAuthorize` de
  `com.biopet.controller.*` y de las reglas de datos de
  `com.biopet.service.*` de **este** repositorio.
- **Sección 2.9 — Estados principales del dominio y transiciones**
  (`EstadoCita`, ciclo de vida `activo`, sesión y token JWT).
- **Sección 3.4 — Tabla de control de completitud**: 44 requisitos × 8
  campos obligatorios, 352 celdas, 0 `FALTA`, más el resumen por estado
  (26 `verificado`, 9 `implementado`, 9 `pendiente`) y la cobertura real de
  los requisitos `Must` (20 de 25 `verificado`).
- **Sección 1.6 — Control documental e historial de revisiones**, con fecha
  de emisión explícita y una fila por revisión respaldada por un commit real.
- **Secciones 7.1 a 7.4**, con las limitaciones abiertas numeradas y las dos
  acciones que requieren cambio de código aisladas en 7.4.
- **`docs/requisitos/tools/`**: `paso2-preambulo.py`, `paso3-cortes-texttt.py`,
  `paso4-para-firma.py` y `auditoria-srs.py`, para que la cadena de
  generación del PDF y la auditoría del SRS sean reproducibles.

### Changed

- **REQ-F-013**, **REQ-F-015** y **REQ-NF-012** pasan a declarar
  explícitamente sus **dos obligaciones** (*Obligación A* y *Obligación B*)
  dentro de su propio bloque, con criterios numerados por obligación
  (`A1…`, `B1…`) y **un solo** valor de Estado que describe el requisito
  completo. **No se crean identificadores nuevos**: cada uno conserva su
  identificador único, su historia y su caso de uso, de modo que el SRS y
  `docs/trazabilidad/matriz.csv` declaran exactamente los mismos 44
  identificadores. Se evaluó y descartó la alternativa de crear
  subrequisitos con sufijo de letra porque habría roto esa correspondencia
  y fragmentado la trazabilidad histórica hacia los `RF-NN` de la Entrega 1A.
- **REQ-F-013 a REQ-F-020** pasan de ser filas de una tabla resumen a bloques
  individuales completos, sin perder identificador, prioridad, origen `RF-NN`,
  historia, caso de uso ni estado.
- **REQ-NF-007** se reformula: el enunciado anterior ("estar operativo
  durante las semanas de evaluación") no era objetivamente verificable. Ahora
  exige un endpoint de salud observable y una disponibilidad medida como
  porcentaje de sondeos exitosos ≥ 99 % con registro fechado. El compromiso
  académico pasa a la sección 2.5 como **restricción operacional**, sin
  estado de cumplimiento. No se declara ninguna disponibilidad histórica.
- **REQ-F-006** amplía su enunciado para incluir explícitamente el
  aislamiento de datos por propietario, no sólo el control por rol.
- **REQ-F-017** incorpora el criterio 5: restricción explícita de los datos
  que pueden enviarse al servicio de IA externo (campos clínicos mínimos;
  prohibido enviar identidad de dueño, veterinario o mascota, y cualquier
  credencial). No declara conformidad con ninguna norma legal.
- **REQ-F-025** incorpora el criterio 6, que documenta la restricción ya
  implementada: a la API externa sólo sale el nombre de la especie.
- **Sección 2.6** deja de afirmar que no hay servicios externos
  implementados. Declara la integración real con **API Ninjas — Animals API**
  (`ExternalApiClient`/`ExternalApiService`) y enumera las tres integraciones
  que efectivamente **no** existen (correo, IA, IoT).
- **Índice.** Se elimina la lista manual de secciones que duplicaba la tabla
  de contenidos del PDF; queda un único índice.
- **Encabezado del documento.** Se separan *versión documental* (v1.0.0),
  *tag histórico de referencia* (`v1.0.0`,
  `0d5cd525ce648cca7219da204e16fa622e671a87`) y *commit de revisión*
  (`8130ee00b0083808fc60567018589e38302b375d`), con el aviso explícito de que
  esta copia **no** corresponde byte a byte al contenido del tag.
- **`docs/trazabilidad/matriz.csv`** pasa de 38 a 44 filas —una por cada
  identificador del SRS, sin excedentes ni faltantes— y normaliza la columna
  `estado` a los tres valores válidos.
- **`docs/requisitos/README.md`** documenta la cadena completa de generación
  (auditoría, pandoc, pasos 2 y 3 de preámbulo y cortes de línea, tres
  pasadas de `pdflatex`, copia para firma y limpieza).

### Fixed

- **Estados que dejaban de ser declarables.** Se eliminan del campo Estado
  los valores `parcial` (REQ-F-013, REQ-F-015, REQ-F-020), `verificado
  parcialmente` (REQ-NF-012), `pendiente de evidencia archivada`
  (REQ-NF-006), `pendiente de confirmación explícita` (REQ-NF-007) y
  `verificado en configuración TLS de desarrollo` (REQ-NF-002). Los matices
  pasan al campo Observaciones.
- **REQ-F-001.** Se retira la cita al test "`AuthControllerTest` (registro
  exitoso)", que **no existe** en
  `Backend/src/test/java/com/biopet/AuthControllerTest.java`. Se sustituye
  por la evidencia HTTP real que sí existe
  (`docs/mediciones/sec/raw/A01-access-control.txt`, líneas 9-11: dos
  registros con `status_registro=201` y rol forzado a `ROLE_DUENO`). El
  estado sigue siendo `verificado`.
- **REQ-F-006** pasa de `verificado` a `implementado`:
  `ConsultaService.listar()` devuelve `findAllByActivoTrue(pageable)` también
  para `ROLE_DUENO`, y `ConsultaRepository` no declara el método de filtrado
  por propietario que sí tienen `CitaRepository` y `VacunaRepository`. Un
  dueño autenticado puede listar consultas médicas de mascotas ajenas.
- **REQ-F-013 / REQ-F-015 / REQ-F-020** pasan de `parcial` a `implementado`.
  En REQ-F-015 la obligación A está cubierta y probada, pero el calendario
  interactivo que exige RF-06 no existe, por lo que el requisito completo no
  puede declararse `verificado`.
- **REQ-NF-006** se mantiene en `pendiente`: ninguno de sus criterios tiene
  artefacto y el repositorio no contiene configuración específica de
  compatibilidad entre navegadores (no hay `browserslist` propio). Que la
  aplicación compile no constituye implementación de este requisito.
- **REQ-NF-007** pasa de `implementado` (matriz) / `pendiente de confirmación
  explícita` (SRS) a `implementado` con criterios medibles.
- **REQ-NF-012** pasa de `verificado parcialmente` a `implementado`: la
  obligación de TTL configurable está cumplida y la de medición de *hit
  ratio* no tiene ninguna medición en el repositorio.
- **Conteos de pruebas corregidos.** `AuthenticationAuditServiceTest` tiene
  **10** pruebas (no 11) y `ProcedimientosBiopetIntegrationTest` tiene **13**
  (no 12).
- **REQ-F-024.** Se retira la corrida Newman
  (`docs/mediciones/postman/newman-report.json`, 2026-07-31) como evidencia
  de la colección de vacunas: esa corrida es **anterior** a la colección y no
  puede citarse como prueba de su ejecución. El criterio de
  `GET /api/vacunas/mascota/{mascotaId}` se sostiene ahora por inspección de
  `VacunaService.listarPorMascota`, declarado en Observaciones.
- **Trazabilidad de los procedimientos almacenados.** Las seis rutinas de
  `db/procs/` quedan asignadas al requisito al que sirven, sin crear
  requisitos nuevos: `fn_historial_clinico_mascota` → REQ-F-013 (obligación B),
  `fn_reporte_dashboard` → REQ-F-019, `sp_registrar_consulta_validada` →
  REQ-F-013 (obligación A), `sp_actualizar_estado_citas_masivas` → REQ-F-015
  (obligación A),
  `fn_resumen_mascotas_por_especie` → REQ-F-021 y `fn_siguiente_numero_ficha`
  → REQ-NF-013 (declarado sin consumidor en la aplicación).
- **Correspondencia exacta de identificadores SRS ↔ matriz.** Una versión
  intermedia de esta revisión dejó al SRS con 47 bloques (por los sufijos de
  letra) frente a 44 filas en la matriz, de modo que no era cierto que ambos
  documentos declararan el mismo conjunto. Se corrigió fusionando los tres
  requisitos compuestos: ahora ambos documentos declaran **exactamente los
  mismos 44 identificadores**, y `docs/requisitos/tools/auditoria-srs.py`
  imprime y comprueba las cuatro diferencias de conjuntos (SRS − matriz,
  matriz − SRS, SRS − tabla 3.4, tabla 3.4 − SRS), que deben ser vacías, y
  rechaza cualquier identificador con sufijo de letra.
- **REQ-NF-018.** Se retira la cita a `SHA256SUMS.txt` y
  `SHA256SUMS-ORIGINAL.txt` como respaldo del criterio de archivado: esas
  sumas corresponden a la corrida del **2026-08-01**, no a las 12 corridas
  del 2026-08-18 que el requisito usa como evidencia. El criterio se apoya
  ahora en `lhci-20260818-0538.meta.txt`, que sí documenta fecha, commit
  auditado (`e94bb72`), versiones de herramienta, URLs y perfiles. Los 12
  valores de Accessibility se releyeron directamente del campo
  `categories.accessibility.score` de cada JSON: 91 en los doce.
- **REQ-NF-017.** Se verificó de forma independiente el contenido del volcado
  `docs/despliegue/ejemplo-backup-20260817.sql` (6 tablas, 6 secuencias, 4
  triggers, 7 rutinas y los conteos de filas declarados), confirmando que
  toda la evidencia pertenece a **este** repositorio y no a ningún otro
  proyecto.
- **Defecto de formato Markdown que corrompía el PDF.** Una línea de
  continuación que empezaba por `+ ` dentro de una lista hacía que pandoc la
  interpretara como un ítem nuevo, rompiendo un *code span* e invirtiendo la
  tipografía del resto del párrafo (texto normal en monoespaciado y viceversa)
  en REQ-NF-012. Se corrigió el texto y se añadió el control 8d a
  `auditoria-srs.py`, que ahora rechaza tanto esas líneas de continuación como
  cualquier bloque con un número impar de acentos graves.
- **Caracteres Unicode sin mapear.** Se añadieron `−` (U+2212) y `×` (U+00D7)
  al mapeo de `tools/paso2-preambulo.py`; sin ellos pdfTeX abortaba con dos
  errores fatales.
- **Maquetación del PDF.** Los bloques de requisito se imprimían como texto
  corrido porque el `.md` no dejaba línea en blanco entre el título en
  negrita y su lista de campos; ahora cada requisito es un encabezado real y
  aparece en el índice. Se corrigen además los desbordamientos de margen de
  hasta 347 pt causados por rutas e identificadores largos: quedan 3
  `Overfull \hbox` residuales de 4,4 pt, 6,1 pt y 13,0 pt y 0 `Overfull
  \vbox`.

## [v1.0.0] - 2026-09-07 (sincronización SRS / matriz / código, antes de firma)

Revisión de sincronización del SRS contra `docs/trazabilidad/matriz.csv` y el
código real, previa a la firma del docente-director. No cambia la numeración
de ningún requisito ni agrega funcionalidad; corrige estados, conteos y
referencias que el documento arrastraba de versiones anteriores.

### Fixed
- **REQ-F-007** — la verificación ahora cita el test automatizado real
  (`UsuarioControllerTest.meSigueFuncionando`, `GET /api/usuarios/me`),
  coherente con la matriz; se elimina la afirmación "sin test automatizado
  formal registrado", que había quedado desactualizada. No cambia el
  enunciado ni el estado (`verificado`).
- **REQ-F-024** — corrección de conteo: `VacunaControllerTest` tiene **9**
  pruebas, no 10. No cambia el resto del requisito.
- **REQ-F-013** y **REQ-F-015** — la sección 4.1 documentaba su estado como
  "pendiente"; se pasa a "parcial", en línea con la sección 3.1, la nota real
  de la matriz y el código (CRUD de `ConsultaController`/`CitaController`
  implementado, pendiente el calendario interactivo / la vista de historial
  consolidado).
- **REQ-F-020** — la matriz lo tenía como "pendiente"; pasa a "parcial" con
  el módulo `AuthenticationAuditService` y la nota de que la parte de
  autenticación está cubierta por REQ-NF-009 en producción (falta la
  auditoría genérica de CRUD), en línea con la nota del SRS.
- **REQ-F-025** — la matriz declaraba "sin prueba automatizada dedicada" y
  estado "implementado"; el test `ExternalApiServiceTest` (6 pruebas,
  `Backend/src/test/java/com/biopet/integration/`) sí existe, así que se
  registra como prueba y el estado pasa a "verificado", en línea con el SRS.
- **REQ-NF-001** — evidencia actualizada a la corrida oficial v1.0.0
  (2026-09-03): archivos `k6-20260903T*-local-tls-v1.0.0-*.json` (10
  corridas, 5 frío + 5 caliente). El p95 máximo real es 15.06 ms en caliente
  y 18.56 ms en frío (umbrales 200/500 ms siguen cumpliéndose con holgura).
- **REQ-NF-012** — se retira de la matriz la afirmación "hit ratio cercano al
  100% documentado en docs/mediciones/perf/REPORT.md", que no existe en el
  reporte; la medición de hit ratio sigue pendiente (sección 7 del SRS).
  Estado alineado a "verificado parcialmente".
- **Sección 4 del SRS** — la tabla de correspondencia REQ-F ↔ HU ↔ CU no
  incluía REQ-F-023 (HU-022/CU-22), REQ-F-024 (HU-023/CU-23) ni REQ-F-025
  (HU-024/CU-24); se agregan. La lista de "requisitos sin origen en la
  Entrega 1A" tampoco los mencionaba; se incorporan explícitamente.

## [v0.9.0-rc] - 2026-08-10 (reconciliación GA Unidad IV)

Corrige el fallo del CI de trazabilidad (`CI BIOPET / traceability`, job
`scripts/validate-traceability.sh`, bloque A.3.3 de la Guía), causado por
20 identificadores `REQ-F-023` a `REQ-F-042` agregados a
`docs/trazabilidad/matriz.csv` sin correspondencia en el SRS, fragmentando
funcionalidades completas (Citas, Consultas, Vacunas, Usuarios, API
externa) en un requisito por endpoint HTTP en lugar de un requisito por
módulo.

### Added
- **REQ-F-023** — Gestión administrativa de usuarios (`UsuarioController`
  CRUD, excluye `/api/usuarios/me`, ya cubierto por `REQ-F-007`). Historia
  `HU-022`, caso de uso `CU-22`. Estado: verificado
  (`UsuarioControllerTest`). Autor: Jaime Josué Mariscal Cabrera.
- **REQ-F-024** — Gestión de vacunas (`VacunaController`/`VacunaService`,
  registro, consulta global/por mascota/por id, actualización y baja
  lógica). Historia `HU-023`, caso de uso `CU-23`. Estado: verificado
  (`VacunaControllerTest`, colección Postman
  `docs/postman/BIOPET-Vacunas.postman_collection.json`), con la
  salvedad documentada de que `GET /api/vacunas/mascota/{mascotaId}` no
  tiene prueba automatizada dedicada. Autor: Jaime Josué Mariscal Cabrera.
- **REQ-F-025** — Consulta de información externa de especies
  (`ExternalApiController`/`ExternalApiService`, integración con API
  Ninjas y caché Redis *cache-aside*). Historia `HU-024`, caso de uso
  `CU-24`. Estado: implementado (sin prueba automatizada dedicada;
  evidencia empírica manual en
  `docs/u4/evidencias/fred/redis-cache-comparacion.md`). Autor: Jaime
  Josué Mariscal Cabrera.
- Historias `HU-022`, `HU-023`, `HU-024` en `HistoriasUsuario.md` y casos
  de uso `CU-22`, `CU-23`, `CU-24` en `CasosDeUso.md`, correspondientes a
  los tres requisitos anteriores.

### Changed
- **REQ-F-013** (historial clínico / Consultas) — la fila de
  `matriz.csv` ahora consolida las 5 operaciones reales de
  `ConsultaController` (`GET`, `GET /{id}`, `POST`, `PUT`, `DELETE`) en un
  único identificador, en vez de fragmentarlas en `REQ-F-013` y
  `REQ-F-027` a `REQ-F-030`. No cambia el enunciado del requisito en el
  SRS. Se preserva, sin ocultarla, la limitación real ya detectada: el
  listado no filtra por propietario para `ROLE_DUENO`, y el listado y
  `PUT` no tienen prueba automatizada dedicada; por eso el estado se
  mantiene en "implementado", no "verificado". Autor: Jaime Josué
  Mariscal Cabrera.
- **REQ-F-015** (gestión de citas) — la fila de `matriz.csv` ahora
  consolida las 5 operaciones reales de `CitaController` en un único
  identificador, en vez de fragmentarlas en `REQ-F-015` y `REQ-F-023` a
  `REQ-F-026`. No cambia el enunciado del requisito en el SRS. El backend
  está completamente probado (`CitaControllerTest`), pero como el
  enunciado formal de `HU-014`/`REQ-F-015` exige un "calendario
  interactivo" que no existe en el frontend, el estado se mantiene en
  "implementado", no "verificado", para no certificar como cumplido un
  requisito que solo se satisface parcialmente. Autor: Jaime Josué
  Mariscal Cabrera.

### Removed
- **`REQ-F-023` a `REQ-F-042`** (numeración anterior, 20 identificadores)
  — eliminados de `docs/trazabilidad/matriz.csv` por no existir en el SRS.
  Ninguna evidencia técnica se perdió: la trazabilidad de Citas y
  Consultas se conservó consolidada en `REQ-F-015`/`REQ-F-013`; la de
  Usuarios, Vacunas y API externa se conservó formalizándola en los
  nuevos `REQ-F-023`, `REQ-F-024` y `REQ-F-025` de esta misma entrada.
  Autor: Jaime Josué Mariscal Cabrera.

## [v0.9.0-rc] - 2026-07-31 (rama `jaime/cierre-observaciones-1a-1b`)

Cierre de las observaciones de requisitos de la Entrega 1A (OBS-02, OBS-03,
OBS-04), registradas en `docs/observaciones/OBSERVACIONES.md` a partir de la
retroalimentación oficial del SGA.

### Added
- **REQ-F-022** — Notificaciones al usuario por correo electrónico. Cierra
  **OBS-02** (ausencia de RF-07 en la lista consolidada de la Entrega 1A).
  RF-07 correspondía al "Servicio de Correos" ya documentado como sistema
  externo en `docs/diagrams/c4-contexto/C4-L1-contexto.md`, pero nunca
  formalizado como requisito `REQ-F`. Se numera 022 (no 007, ya ocupado por
  "Consulta del perfil propio") para no duplicar identificadores. Estado:
  pendiente (no implementado). Historia `HU-021`, caso de uso `CU-21`.
  Autor: Jaime Josué Mariscal Cabrera.
- **Sección 4.1 del SRS — Trazabilidad histórica RF/RF-WEB → REQ-F**. Cierra
  **OBS-03** (remapeo de RF-WEB a RF-16/RF-17 sin matriz de trazabilidad
  explícita). Consolida en una tabla estructurada (identificador anterior,
  identificador actual, descripción, caso de uso, historia, estado) el
  vínculo que antes solo existía disperso en el campo Rationale de cada
  requisito individual. No modifica `docs/trazabilidad/matriz.csv` (fuera
  del alcance de archivos autorizados para este cierre); queda como acción
  de seguimiento agregar allí una columna equivalente de origen histórico.
  Autor: Jaime Josué Mariscal Cabrera.

### Changed
- **REQ-F-017** — Recomendaciones clínicas informativas. Cierra **OBS-04**
  (ambigüedad leve señalada por el docente en la redacción original de
  RF-10, "recomendaciones informativas"). Se reemplaza el resumen de una
  sola línea por un bloque completo en patrón "El sistema deberá...", con
  entradas, resultado esperado y tres criterios de aceptación verificables.
  No cambia el identificador, la prioridad (`Could`) ni el estado
  (`pendiente`); no se agrega funcionalidad nueva. Autor: Jaime Josué
  Mariscal Cabrera.

### Fixed
- Corrección factual en este mismo changelog: la entrada `[v0.9.0-rc] -
  2026-07-30` de `REQ-F-021` citaba `HU-021`/`CU-021` como su historia y
  caso de uso asociados. La fuente de verdad real
  (`docs/requisitos/historias/HistoriasUsuario.md`,
  `docs/requisitos/casos-de-uso/CasosDeUso.md` y la tabla de correspondencia
  de la sección 4 del SRS) siempre asignó `HU-020`/`CU-20` a `REQ-F-021`; se
  corrige la entrada de abajo para no chocar con `HU-021`/`CU-21`, que a
  partir de esta revisión sí identifican a `REQ-F-022`. Autor: Jaime Josué
  Mariscal Cabrera.

## [v0.9.0-rc] - 2026-07-30

### Added
- **REQ-F-021** — Resumen de mascotas por especie. El endpoint
  `GET /api/mascotas/resumen-especies` ya estaba implementado en el backend
  (función `fn_resumen_mascotas_por_especie`, catalogada en
  `docs/basedatos/CATALOGO-SP.md`) pero no tenía requisito formal en el SRS.
  Se agrega ahora con su historia `HU-020` y caso de uso `CU-20`
  (corregido; ver entrada `Fixed` del 2026-07-31 de más arriba).
  Autor: Zaida Melissa Taipe Mora.

### Changed
- **REQ-NF-007** (diseño responsivo) — cambia de estado "pendiente de
  evidencia empírica" a "implementado". Se agregó una cuadrícula responsive
  (`grid-mascotas` con media query en `styles.css`) y estados de foco visibles
  (`:focus-visible`) en el frontend. Sigue pendiente la corrida formal de
  Lighthouse para pasar a "verificado".
  Autor: Zaida Melissa Taipe Mora.

### Fixed
- Los flujos de error del frontend (login y mascotas) ya interpretan
  `ProblemDetail` para los códigos 400, 401, 403, 404, 409, 422 y 429,
  incluyendo el desglose por campo del 422 (`errors`). Antes de este cambio,
  el 409 caía en un mensaje genérico no diferenciado.
  Autor: Zaida Melissa Taipe Mora.

## [v0.7.0] - 2026-06-14

- Ver `docs/requisitos/cambios/CAMBIOS-SRS.md` para el detalle completo de
  la consolidación de requisitos realizada en esta entrega (migración de
  esquema RF-NN a REQ-F-NNN, separación implementados/pendientes, etc.).
