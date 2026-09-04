## C4 Nivel 1 — Contexto

### Objetivo

Este documento describe el **diagrama de contexto (C4 Nivel 1)** de BIOPET.
Su alcance es el más externo del modelo C4: muestra el sistema como una
única caja, quiénes lo usan (actores humanos) y con qué sistemas externos
se comunica, sin entrar en contenedores internos (eso lo cubre el
C4 Nivel 2, `docs/diagrams/c4-contenedores/`) ni en componentes del backend
(C4 Nivel 3, `docs/diagrams/c4-componentes-backend/`).

### Fuente versionada

Los tres niveles del modelo C4 (contexto, contenedores y componentes del
backend) se modelan primero en `docs/diagrams/workspace.dsl` (Structurizr
DSL, en inglés). Los `.dot`/`.puml` de esta carpeta son derivaciones
manuales alineadas a esa fuente única — ver la cabecera de
`workspace.dsl` para el comando de exportación reproducible
(`structurizr-cli export`) y el detalle de cómo se generaron los PNG
actuales (Graphviz vía `@hpcc-js/wasm` + `@resvg/resvg-js`, sin binario
nativo de `dot` disponible en el entorno de autoría).

### Alcance real (v1.0.0)

- **Actores humanos (trazo sólido):** los cuatro roles reales del sistema
  (`ROLE_ADMIN`, `ROLE_VETERINARIO`, `ROLE_AUXILIAR`, `ROLE_DUENO`),
  verificados contra `entity/Rol.java` y las anotaciones `@PreAuthorize` de
  los controladores. Todos usan BIOPET vía HTTPS.
- **Sistema externo (trazo punteado):** *API Ninjas (Animals API)*, la
  única integración externa real en v1.0.0 (`ExternalApiController` →
  `ExternalApiService` → `ExternalApiClient`, con caché de respuesta en
  Redis/Valkey). Reemplaza a los tres sistemas "planificados" (Dispositivos
  IoT, Servicio de IA Cognitiva, Servicio de Correos) de la versión anterior
  del diagrama (heredados de la visión original de la Entrega 1A,
  `PFC_Entrega1A_BMT.pdf`, Figura 1): ninguno de esos tres llegó a
  implementarse, así que se retiran del diagrama de contexto para no
  sobre-declarar alcance; quedan como observación histórica en el SRS
  (REQ-F-016, REQ-F-017) si el equipo decide retomarlos a futuro.

### Diagrama

![C4 Nivel 1 - Contexto](c4-contexto.png)

Fuente editable: [`c4-contexto.dot`](c4-contexto.dot) (Graphviz, formato
consistente con el resto de diagramas del repositorio) y
[`c4-contexto.puml`](c4-contexto.puml) (PlantUML, mismo contenido), ambas
derivadas de [`../workspace.dsl`](../workspace.dsl).

### Regenerar el PNG

```bash
# Opción reproducible documentada en workspace.dsl (Structurizr CLI + PlantUML)
# Opción usada en este repositorio (sin Graphviz nativo instalado):
node render.mjs   # ver docs/diagrams/workspace.dsl, cabecera final
# o, si hay Graphviz instalado localmente:
dot -Tpng docs/diagrams/c4-contexto/c4-contexto.dot -o docs/diagrams/c4-contexto/c4-contexto.png
```

### Trazabilidad

- Actores → `entity/Rol.java`, controladores (`@PreAuthorize`).
- API Ninjas (Animals API) → `integration/ExternalApiClient.java`,
  `integration/ExternalApiService.java`, `controller/ExternalApiController.java`.
