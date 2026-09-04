## C4 Nivel 3 — Componentes del Backend

### Objetivo

Este documento describe el **diagrama de componentes (C4 Nivel 3)** del
contenedor **Backend Spring Boot** de BIOPET, actualizado a **v1.0.0**. Su
alcance es exclusivamente interno a ese contenedor: muestra en qué
componentes reales se organiza el backend (controladores, servicios,
seguridad, persistencia, manejo de errores e infraestructura del servidor)
y cómo se relacionan entre sí, con los contenedores externos ya
documentados en el C4 Nivel 2 (`docs/diagrams/c4-contenedores/`) — Frontend,
PostgreSQL y Redis/Valkey — y con el único sistema externo real, *API
Ninjas (Animals API)* (ver C4 Nivel 1, `docs/diagrams/c4-contexto/`).

La fuente única y versionada del modelo (los tres niveles) es
[`../workspace.dsl`](../workspace.dsl) (Structurizr DSL, en inglés); este
`.dot`/`.puml` son derivaciones manuales alineadas a esa fuente.

Respecto a la Tercera Entrega, v1.0.0 añade los controladores/servicios/
repositorios de la Unidad IV — citas (`CitaController`/`CitaService`/
`CitaRepository`), consultas (`ConsultaController`/`ConsultaService`/
`ConsultaRepository`) y vacunas (`VacunaController`/`VacunaService`/
`VacunaRepository`) — más la integración externa (`ExternalApiController`/
`ExternalApiService`/`ExternalApiClient`) y el acceso formal a
procedimientos almacenados (`ProcedimientoBiopetRepository`).

No es un diagrama de clases: no muestra atributos, métodos, constructores,
anotaciones Java ni el detalle línea por línea del código. Los DTOs y
entidades (`AuthResponse`, `LoginRequest`, `Usuario`, `Mascota`, etc.) se
mencionan aquí solo como modelos de datos que viajan entre componentes, sin
representarse como cajas propias, para no saturar el diagrama.

### Diagrama

El repositorio documenta sus diagramas con **Graphviz DOT** (fuente
`.dot`, renderizado a `.png`) y, en paralelo, un archivo **PlantUML**
(`.puml`) equivalente y de sintaxis independiente — la misma convención de
`docs/diagrams/c4-contenedores/`, `docs/diagrams/diagrama-clases/` y
`docs/diagrams/secuencia-jwt/`. Este diagrama sigue exactamente esa misma
convención, en su propia carpeta:

- Fuente Graphviz: [`c4-componentes-backend.dot`](c4-componentes-backend.dot)
- Fuente PlantUML: [`c4-componentes-backend.puml`](c4-componentes-backend.puml)
- Imagen renderizada: [`c4-componentes-backend.png`](c4-componentes-backend.png)
  (5594×2190 px, fondo blanco, generada a partir de la fuente Graphviz real,
  no dibujada a mano), con el comando:
  ```bash
  dot -Tpng c4-componentes-backend.dot -o c4-componentes-backend.png
  ```
  usando Graphviz 15.1.0. Se validó primero la sintaxis con
  `dot -Tcanon c4-componentes-backend.dot -o /dev/null` (sin errores) antes
  de generar la imagen final.

Fuente Graphviz completa (no se duplica aquí para evitar que ambas copias
diverjan): [`c4-componentes-backend.dot`](c4-componentes-backend.dot),
generada desde [`../workspace.dsl`](../workspace.dsl).

### Componentes

| Área | Componente | Responsabilidad | Tecnología |
|---|---|---|---|
| API REST | `AuthController` | Endpoints `/api/auth/registro`, `/login`, `/refresh`, `/logout` | Spring MVC REST |
| API REST | `MascotaController` | Endpoints CRUD de `/api/mascotas` y `/api/mascotas/resumen-especies` | Spring MVC REST |
| API REST | `UsuarioController` | Endpoint `GET /api/usuarios/me` (perfil autenticado) | Spring MVC REST |
| API REST | `CitaController` | CRUD de `/api/citas` y actualización masiva de estado | Spring MVC REST |
| API REST | `ConsultaController` | Registro y consulta de `/api/consultas` | Spring MVC REST |
| API REST | `VacunaController` | CRUD de `/api/vacunas` | Spring MVC REST |
| API REST | `ExternalApiController` | Endpoint `/api/externa/especies/*`, proxy cacheado hacia API Ninjas | Spring MVC REST |
| Servicios | `AuthService` | Orquesta registro, login, refresh, logout: autenticación, JWT, rate limiting, auditoría y revocación | Spring `@Service` |
| Servicios | `MascotaService` | Reglas de negocio de mascotas: autorización por propiedad, caché de listados, resumen por especie | Spring `@Service` + Spring Cache |
| Servicios | `CitaService` | Reglas de negocio de citas: validación de veterinario, actualización masiva de estado | Spring `@Service` |
| Servicios | `ConsultaService` | Valida mascota y veterinario activos antes de registrar una consulta | Spring `@Service` |
| Servicios | `VacunaService` | Reglas de negocio de registros de vacunación | Spring `@Service` |
| Servicios | `ExternalApiService` | Obtiene y cachea (Redis, TTL configurable) datos de especie desde la API externa | Spring `@Service` |
| Servicios | `ExternalApiClient` | Cliente HTTP hacia API Ninjas (Animals API) | Spring `@Service` |
| Seguridad | `SecurityConfig` | Cadena de filtros, CORS, cabeceras HTTP, autorización HTTP, wiring de entry point/access denied handler | Spring Security |
| Seguridad | `JwtAuthenticationFilter` | Resuelve y valida el JWT (cookie o `Authorization`) de cada solicitud protegida | Spring Security `OncePerRequestFilter` |
| Seguridad | `JwtService` | Genera y valida JWT: firma HMAC, claims estándar y propios, tipo access/refresh | JJWT (HS256) |
| Seguridad | `JwtCookieService` | Lee y escribe las cookies `access_token`/`refresh_token` | Jakarta Servlet API |
| Seguridad | `UserDetailsServiceImpl` | Carga el usuario (roles, estado activo) para autenticación y autorización | Spring Security `UserDetailsService` |
| Seguridad | `LoginRateLimiterService` | Limita intentos fallidos de login por IP (5 fallos → 401, 6º → 429) | En memoria (`ConcurrentHashMap`) |
| Seguridad | `TokenBlacklistService` | Revoca y consulta JWT revocados por `jti` | Redis (`StringRedisTemplate`) |
| Seguridad | `AuthenticationAuditService` | Registra eventos `AUTH_AUDIT` estructurados (login, refresh, logout, revocación) | SLF4J / Logback |
| Seguridad | `ProblemAuthenticationEntryPoint` | Construye el `ProblemDetail` 401 para solicitudes no autenticadas | Spring Security `AuthenticationEntryPoint` |
| Seguridad | `ProblemAccessDeniedHandler` | Construye el `ProblemDetail` 403 para solicitudes autenticadas sin permiso | Spring Security `AccessDeniedHandler` |
| Errores | `GlobalExceptionHandler` | Traduce excepciones de negocio/seguridad (validación, rate limit, parámetros inválidos, etc.) a `ProblemDetail` | Spring `@RestControllerAdvice` |
| Errores | `ProblemDetailFactory` | Construye el `ProblemDetail` uniforme (`type`/`title`/`status`/`detail`/`instance`) | Utilitario estático |
| Persistencia | `UsuarioRepository` | Acceso a la tabla `usuarios` | Spring Data JPA |
| Persistencia | `MascotaRepository` | Acceso a la tabla `mascotas` + función nativa `fn_resumen_mascotas_por_especie` (parámetro enlazado `:duenioId`) | Spring Data JPA + `@Query` nativa |
| Persistencia | `CitaRepository` | Acceso a la tabla `citas` | Spring Data JPA |
| Persistencia | `ConsultaRepository` | Acceso a la tabla `consultas` | Spring Data JPA |
| Persistencia | `VacunaRepository` | Acceso a la tabla `vacunas` | Spring Data JPA |
| Persistencia | `ProcedimientoBiopetRepository` | Invocación formal de las 6 rutinas almacenadas de PostgreSQL | Spring Data JPA (`@Procedure`/`@NamedStoredProcedureQuery`) |
| Infraestructura | `TomcatDualConnectorConfig` | Añade el conector HTTP interno (8080) junto al conector HTTPS principal (8443) cuando el perfil `tls` está activo | Tomcat embebido (Spring Boot), `@Profile("tls")` |
| Infraestructura | Flyway | Aplica el esquema (`V1__schema_inicial.sql`) sobre PostgreSQL una sola vez, al iniciar el backend, antes de que este atienda tráfico; no interviene en ninguna solicitud HTTP | Flyway 9.22.3 (`spring.flyway.locations: classpath:db/migration`) |

### Relaciones principales

1. **Login.** El Frontend envía `POST /api/auth/login` a `AuthController`,
   que delega en `AuthService`. `AuthService` primero consulta
   `LoginRateLimiterService` (si la IP ya está bloqueada, corta antes de
   autenticar); si no lo está, autentica contra `UsuarioRepository`
   (vía `UserDetailsServiceImpl`, PostgreSQL). Un fallo se registra en
   `LoginRateLimiterService` y en `AuthenticationAuditService`
   (`LOGIN_FAILURE`/`LOGIN_RATE_LIMITED`); un éxito reinicia el contador,
   genera access y refresh token con `JwtService`, registra
   `LOGIN_SUCCESS`, y `AuthController` los entrega como cookies vía
   `JwtCookieService`.

2. **Autenticación de una solicitud protegida.** Toda solicitud a un
   endpoint no público pasa primero por `JwtAuthenticationFilter`, que lee
   la cookie `access_token` (o el header `Authorization`) con
   `JwtCookieService`, valida firma/claims/tipo con `JwtService`, y
   consulta `TokenBlacklistService` (Redis) para descartar tokens
   revocados — registrando `TOKEN_REVOKED` en `AuthenticationAuditService`
   si corresponde. Si es válido, carga el usuario con
   `UserDetailsServiceImpl` y establece el contexto de seguridad.

3. **Consulta o modificación de mascotas.** `MascotaController` delega en
   `MascotaService`, que aplica autorización por rol y por propiedad
   (consultando `UsuarioRepository`) antes de leer/escribir en
   `MascotaRepository` (PostgreSQL). El listado usa caché declarativa de
   Spring (`@Cacheable`/`@CacheEvict`) respaldada por Redis.

4. **Logout y revocación.** `AuthController` obtiene ambas cookies (si
   existen) y llama a `AuthService.logout`, que por cada token válido
   extrae su `jti` y lo revoca en `TokenBlacklistService` (Redis, con TTL
   igual al tiempo de vida restante), registra `LOGOUT_SUCCESS` en
   `AuthenticationAuditService`, y `AuthController` elimina ambas cookies
   en el cliente.

5. **Rate limiting.** `LoginRateLimiterService` mantiene, en memoria y por
   IP, el conteo de fallos dentro de una ventana de 15 minutos; el sexto
   fallo consecutivo bloquea esa IP durante otros 15 minutos y hace que
   `AuthService` propague una excepción que `GlobalExceptionHandler`
   traduce a 429 con cabecera `Retry-After`. Un login exitoso reinicia el
   contador de esa IP.

6. **Manejo de errores mediante ProblemDetail.** `GlobalExceptionHandler`
   intercepta las excepciones de negocio/seguridad (credenciales
   inválidas, recurso no encontrado, validación, parámetro con tipo
   inválido, rate limit excedido) y delega en `ProblemDetailFactory` para
   construir un cuerpo uniforme. Fuera de ese *advice*,
   `ProblemAuthenticationEntryPoint` y `ProblemAccessDeniedHandler` usan
   la misma fábrica para los 401/403 que ocurren a nivel de filtro de
   Spring Security, antes de llegar al controlador.

7. **Registro de auditoría.** `AuthService` y `JwtAuthenticationFilter` son
   los dos puntos de entrada reales hacia `AuthenticationAuditService`,
   que escribe una línea `AUTH_AUDIT` por evento en el log local del
   proceso (sin contraseñas, JWT completos, cookies ni el valor de
   `Authorization`).

### Fronteras y dependencias externas

- **Frontend BIOPET** (Angular + Nginx): único cliente HTTP considerado en
  este diagrama; consume la API REST y envía automáticamente las cookies
  de sesión en cada solicitud al mismo origen.
- **PostgreSQL**: contenedor externo, alcanzado únicamente a través de
  `UsuarioRepository`/`MascotaRepository` (Spring Data JPA/Hibernate). El
  backend no abre conexiones JDBC fuera de esos repositorios.
- **Redis**: contenedor externo, con dos usos reales confirmados en el
  código — `TokenBlacklistService` (blacklist de `jti` revocados) y la
  caché declarativa de `MascotaService` (`@Cacheable`/`@CacheEvict`, vía
  la abstracción de caché de Spring, configurada como `spring.cache.type: redis`).
- **Flyway**: no es una clase propia de `com.biopet`, sino una dependencia
  gestionada por `spring-boot-starter-parent` (versión efectiva `9.22.3`,
  verificada con `mvn dependency:tree`) que Spring Boot invoca
  automáticamente al arrancar, aplicando `V1__schema_inicial.sql`
  (`classpath:db/migration`) sobre PostgreSQL antes de que el backend quede
  disponible. No participa en ninguno de los flujos de solicitud HTTP
  descritos arriba; se documenta en este nivel porque la guía de la
  Tercera Entrega exige representarlo explícitamente. Ver también
  `ADR-004-postgresql.md` y `ADR-007-acceso-datos.md`.
- **HTTP 8080**: conector interno, habilitado solo cuando el perfil `tls`
  está activo (`TomcatDualConnectorConfig`); pensado para tráfico dentro
  de la red de contenedores (por ejemplo, el `healthcheck` de Docker), no
  para el tráfico público.
- **HTTPS 8443**: conector TLS 1.3 principal cuando el perfil `tls` está
  activo, con certificado autofirmado académico.
- **Logs locales**: `AuthenticationAuditService` escribe únicamente al log
  del propio proceso/contenedor (stdout); no hay un colector ni un SIEM
  centralizado en el alcance actual.

### Decisiones de seguridad visibles en el diagrama

- Las cookies de sesión (`access_token`, `refresh_token`) se emiten con
  `HttpOnly`, `Secure` y `SameSite=Strict` — nunca se almacena el JWT en
  `localStorage` ni lo administra el frontend directamente.
- Existen dos tokens diferenciados: un *access token* de corta duración y
  un *refresh token* de mayor duración, ambos verificados por
  `JwtService`.
- La autorización combina **rol** (`SecurityConfig`/`@PreAuthorize`) y
  **propiedad del recurso** (`MascotaService`, para `ROLE_DUENO`).
- La revocación de tokens depende de una **blacklist en Redis**
  (`TokenBlacklistService`), consultada en cada solicitud protegida.
- El **rate limiting de login es en memoria**, por instancia del backend,
  no distribuido.
- Cada evento de autenticación relevante queda en una **auditoría
  estructurada** (`AUTH_AUDIT`), sin datos sensibles.
- El backend expone **TLS 1.3** en el puerto 8443 cuando el perfil `tls`
  está activo.
- Las respuestas distinguen explícitamente **401** (sin autenticación
  válida, `ProblemAuthenticationEntryPoint`) de **403** (autenticado pero
  sin permiso, `ProblemAccessDeniedHandler`).
- Todo error de este subsistema usa el formato uniforme **ProblemDetail**
  (`GlobalExceptionHandler` + `ProblemDetailFactory`).

### Limitaciones

- El código confirma un **backend monolítico modular** (un único artefacto
  Spring Boot, organizado en paquetes `controller`/`service`/`security`/
  `repository`/`exception`/`config`), no una arquitectura de
  microservicios.
- El despliegue evaluado corresponde a **una sola instancia** del backend
  (`docker-compose.yml`, servicio `backend` sin réplicas).
- El rate limiting de login **no está distribuido**: cada instancia
  llevaría su propio contador si hubiera más de una.
- Los logs de auditoría son **locales**, sin integración con un SIEM.
- El certificado TLS es **autofirmado y exclusivamente académico/local**.
- Este diagrama representa la **versión v1.0.0** del código, verificada
  contra `main` (Unidad IV: citas, consultas, vacunas, integración externa)
  y actualizada en la rama `fix/zaida-frontend-docs-recalificacion`; debe
  actualizarse si cambian las dependencias reales entre estos componentes.
- La fuente única de verdad es `../workspace.dsl` (Structurizr DSL); tanto
  `c4-componentes-backend.dot` como `c4-componentes-backend.puml` son
  derivaciones manuales de esa fuente y deben mantenerse sincronizadas
  entre sí en cada cambio.

### Trazabilidad

**Controladores:**
- `Backend/src/main/java/com/biopet/controller/AuthController.java`
- `Backend/src/main/java/com/biopet/controller/MascotaController.java`
- `Backend/src/main/java/com/biopet/controller/UsuarioController.java`
- `Backend/src/main/java/com/biopet/controller/CitaController.java`
- `Backend/src/main/java/com/biopet/controller/ConsultaController.java`
- `Backend/src/main/java/com/biopet/controller/VacunaController.java`
- `Backend/src/main/java/com/biopet/controller/ExternalApiController.java`

**Servicios:**
- `Backend/src/main/java/com/biopet/service/AuthService.java`
- `Backend/src/main/java/com/biopet/service/MascotaService.java`
- `Backend/src/main/java/com/biopet/service/UserDetailsServiceImpl.java`
- `Backend/src/main/java/com/biopet/service/CitaService.java`
- `Backend/src/main/java/com/biopet/service/ConsultaService.java`
- `Backend/src/main/java/com/biopet/service/VacunaService.java`
- `Backend/src/main/java/com/biopet/integration/ExternalApiService.java`
- `Backend/src/main/java/com/biopet/integration/ExternalApiClient.java`

**Seguridad:**
- `Backend/src/main/java/com/biopet/security/JwtAuthenticationFilter.java`
- `Backend/src/main/java/com/biopet/security/JwtService.java`
- `Backend/src/main/java/com/biopet/security/JwtCookieService.java`
- `Backend/src/main/java/com/biopet/security/LoginRateLimiterService.java`
- `Backend/src/main/java/com/biopet/security/TokenBlacklistService.java`
- `Backend/src/main/java/com/biopet/security/AuthenticationAuditService.java`
- `Backend/src/main/java/com/biopet/security/ProblemAuthenticationEntryPoint.java`
- `Backend/src/main/java/com/biopet/security/ProblemAccessDeniedHandler.java`

**Errores:**
- `Backend/src/main/java/com/biopet/exception/GlobalExceptionHandler.java`
- `Backend/src/main/java/com/biopet/exception/ProblemDetailFactory.java`

**Persistencia:**
- `Backend/src/main/java/com/biopet/repository/UsuarioRepository.java`
- `Backend/src/main/java/com/biopet/repository/MascotaRepository.java`
- `Backend/src/main/java/com/biopet/repository/CitaRepository.java`
- `Backend/src/main/java/com/biopet/repository/ConsultaRepository.java`
- `Backend/src/main/java/com/biopet/repository/VacunaRepository.java`
- `Backend/src/main/java/com/biopet/repository/ProcedimientoBiopetRepository.java`
- `db/procs/fn_resumen_mascotas_por_especie.sql`
- `Backend/src/main/resources/db/migration/` (Flyway V1..V6)

**Configuración:**
- `Backend/src/main/java/com/biopet/config/SecurityConfig.java`
- `Backend/src/main/java/com/biopet/config/TomcatDualConnectorConfig.java`

**Documentación relacionada:**
- `docs/adr/ADR-006-autenticacion-seguridad.md`
- `docs/adr/ADR-007-acceso-datos.md` (estrategia híbrida JPA / funciones PostgreSQL)
- `docs/basedatos/CATALOGO-SP.md`
- `docs/mediciones/sec/A01-access-control.md`
- `docs/mediciones/sec/A02-cryptography-tls.md`
- `docs/mediciones/sec/A05-security-headers.md`
- `docs/mediciones/sec/A07-authentication.md`
- `docs/mediciones/sec/A09-logging.md`
- `docs/diagrams/c4-contenedores/` (C4 Nivel 2 — contenedores)

No se incluyen capturas de pantalla en esta fase.
