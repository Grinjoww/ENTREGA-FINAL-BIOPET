workspace "BIOPET" "Veterinary pet-management system (v1.0.0) - C4 model source of truth" {

    model {
        admin       = person "Administrator"   "Manages pets and views the global summary by species."
        vet         = person "Veterinarian"    "Registers and manages pets; handles appointments, clinical consultations and vaccines."
        assistant   = person "Assistant"       "Manages pets and supports clinical staff (ROLE_AUXILIAR)."
        owner       = person "Pet Owner"       "Consults only their own pets, appointments, consultations and vaccines (ROLE_DUENO)."

        animalApi   = softwareSystem "API Ninjas (Animals API)" "External REST API. Provides species taxonomy, habitat and diet data." "External System"

        biopet = softwareSystem "BIOPET" "Centralizes pet registration and role-based management (RBAC + JWT) for a veterinary clinic." {

            frontend = container "Frontend" "Single-page application for authentication and for managing pets, appointments, consultations and vaccines." "Angular 17, Nginx"

            backend = container "Backend API" "Exposes the REST API: authentication, pets, appointments, consultations, vaccines, dashboard reports and the external-species proxy." "Spring Boot 3.2, Java 21" {

                // --- REST controllers ---------------------------------------------------
                authController         = component "AuthController"         "Registration, login, refresh and logout endpoints."          "Spring MVC REST Controller"
                mascotaController      = component "MascotaController"      "CRUD and search endpoints for pets."                          "Spring MVC REST Controller"
                usuarioController      = component "UsuarioController"      "Exposes the authenticated user's profile."                    "Spring MVC REST Controller"
                citaController         = component "CitaController"         "CRUD and bulk status update for appointments."                "Spring MVC REST Controller"
                consultaController     = component "ConsultaController"     "Registers and queries clinical consultations."                "Spring MVC REST Controller"
                vacunaController       = component "VacunaController"       "CRUD for vaccine records."                                    "Spring MVC REST Controller"
                externalApiController  = component "ExternalApiController"  "Exposes species information sourced from the external API."   "Spring MVC REST Controller"

                // --- Application services ------------------------------------------------
                authService        = component "AuthService"        "Registration, login, token refresh, logout and profile lookup."             "Spring Service"
                mascotaService      = component "MascotaService"      "Pet business rules, ownership checks and the read cache."                    "Spring Service"
                citaService         = component "CitaService"         "Appointment business rules, scheduling and bulk status updates."             "Spring Service"
                consultaService     = component "ConsultaService"     "Clinical-consultation validation (active pet and veterinarian)."             "Spring Service"
                vacunaService       = component "VacunaService"       "Vaccine record business rules."                                              "Spring Service"
                externalApiService  = component "ExternalApiService"  "Fetches and caches (Redis, TTL) species data from the external API."         "Spring Service"
                externalApiClient   = component "ExternalApiClient"   "HTTP client for API Ninjas."                                                 "Spring Service"

                // --- Security ------------------------------------------------------------
                securityConfig       = component "SecurityConfig"                  "Spring Security filter chain, providers and CORS policy."            "Spring Configuration"
                jwtAuthFilter        = component "JwtAuthenticationFilter"         "Authenticates each request from the JWT stored in a cookie."          "Spring Security Filter"
                jwtService           = component "JwtService"                      "Issues and validates JWTs (HMAC-SHA256, claims, expiry)."             "Spring Service"
                jwtCookieService     = component "JwtCookieService"                "Reads/writes the HttpOnly+Secure+SameSite=Strict auth cookies."       "Spring Service"
                userDetailsService   = component "UserDetailsServiceImpl"          "Loads user + authorities for Spring Security."                        "Spring Service"
                rateLimiter          = component "LoginRateLimiterService"         "In-memory per-IP login rate limiting."                                "Spring Service"
                blacklist            = component "TokenBlacklistService"           "JWT revocation via a Redis blacklist keyed by jti."                   "Spring Service"
                auditService         = component "AuthenticationAuditService"      "Structured authentication audit logging."                             "Spring Service"
                authEntryPoint       = component "ProblemAuthenticationEntryPoint" "Builds the RFC 7807 response for 401 Unauthorized."                   "Spring Security Component"
                accessDeniedHandler  = component "ProblemAccessDeniedHandler"      "Builds the RFC 7807 response for 403 Forbidden."                      "Spring Security Component"

                // --- Error handling --------------------------------------------------------
                globalExceptionHandler = component "GlobalExceptionHandler" "Maps application exceptions to RFC 7807 ProblemDetail responses." "Spring @ControllerAdvice"
                problemDetailFactory   = component "ProblemDetailFactory"   "Builds uniform RFC 7807 ProblemDetail payloads."                   "Spring Component"

                // --- Persistence (Spring Data JPA) -----------------------------------------
                usuarioRepo      = component "UsuarioRepository"             "User persistence."                                                                 "Spring Data JPA Repository"
                mascotaRepo      = component "MascotaRepository"             "Pet persistence."                                                                  "Spring Data JPA Repository"
                citaRepo         = component "CitaRepository"                "Appointment persistence."                                                          "Spring Data JPA Repository"
                consultaRepo     = component "ConsultaRepository"            "Clinical-consultation persistence."                                                "Spring Data JPA Repository"
                vacunaRepo       = component "VacunaRepository"              "Vaccine-record persistence."                                                       "Spring Data JPA Repository"
                procedimientoRepo = component "ProcedimientoBiopetRepository" "Formal JPA access (@Procedure / @NamedStoredProcedureQuery) to the 6 PostgreSQL stored procedures." "Spring Data JPA Repository"

                // --- Server infrastructure --------------------------------------------------
                tomcatDual = component "TomcatDualConnectorConfig" "Dual HTTP/HTTPS Tomcat connector configuration (profile 'tls')." "Spring Configuration"
            }

            postgres = container "PostgreSQL" "Relational database. Schema and stored procedures managed by Flyway migrations V1-V6." "PostgreSQL 16 (dev) / 18 (prod)" "Database"
            redis    = container "Redis / Valkey" "JWT-blacklist store (by jti) and read-through cache for pets and external species data." "Redis 7 (dev) / Valkey 8 (prod)" "Database"
        }

        // --- System Context relationships ---------------------------------------------
        admin     -> biopet "Uses" "HTTPS"
        vet       -> biopet "Uses" "HTTPS"
        assistant -> biopet "Uses" "HTTPS"
        owner     -> biopet "Uses" "HTTPS"
        biopet    -> animalApi "Requests species taxonomy/habitat/diet data from" "HTTPS/JSON"

        // --- Container relationships -----------------------------------------------------
        admin     -> frontend "Uses" "HTTPS"
        vet       -> frontend "Uses" "HTTPS"
        assistant -> frontend "Uses" "HTTPS"
        owner     -> frontend "Uses" "HTTPS"
        frontend  -> backend "Makes API calls to" "HTTPS/JSON, HttpOnly auth cookies"
        backend   -> postgres "Reads from and writes to" "JDBC/JPA"
        backend   -> redis "Reads from and writes to" "Redis protocol"
        backend   -> animalApi "Requests species data from" "HTTPS/JSON"

        // --- Component relationships (backend) --------------------------------------------
        frontend -> authController "Calls" "HTTPS/JSON + cookies (access_token, refresh_token)"
        frontend -> mascotaController "Calls" "HTTPS/JSON + cookie access_token"
        frontend -> usuarioController "Calls" "HTTPS/JSON + cookie access_token"
        frontend -> citaController "Calls" "HTTPS/JSON + cookie access_token"
        frontend -> consultaController "Calls" "HTTPS/JSON + cookie access_token"
        frontend -> vacunaController "Calls" "HTTPS/JSON + cookie access_token"
        frontend -> externalApiController "Calls" "HTTPS/JSON + cookie access_token"

        authController -> authService "Register / login / refresh / logout"
        authController -> jwtCookieService "Reads/writes auth cookies"
        mascotaController -> mascotaService "List / search / create / update / delete"
        usuarioController -> authService "Get profile (email)"
        citaController -> citaService "CRUD / bulk status update"
        consultaController -> consultaService "Register / query consultations"
        vacunaController -> vacunaService "CRUD vaccine records"
        externalApiController -> externalApiService "Get species info"

        authService -> usuarioRepo "Finds/creates user"
        authService -> jwtService "Generates/validates JWT"
        authService -> rateLimiter "Checks/records login attempts"
        authService -> auditService "Logs LOGIN_*/REFRESH_*/LOGOUT_SUCCESS"
        authService -> blacklist "Revokes / checks isRevoked (refresh, logout)"

        mascotaService -> mascotaRepo "CRUD"
        mascotaService -> usuarioRepo "Validates owner/role"
        mascotaService -> redis "Cache reads/writes (@Cacheable/@CacheEvict)"

        citaService -> citaRepo "CRUD / bulk update"
        citaService -> usuarioRepo "Validates veterinarian role"
        consultaService -> consultaRepo "CRUD"
        consultaService -> usuarioRepo "Validates veterinarian/access"
        vacunaService -> vacunaRepo "CRUD"

        externalApiService -> externalApiClient "Fetches species data"
        externalApiService -> redis "Caches responses (TTL)"
        externalApiClient -> animalApi "GET species data" "HTTPS/JSON"

        jwtAuthFilter -> jwtCookieService "Reads access_token cookie"
        jwtAuthFilter -> jwtService "Validates signature/claims/type"
        jwtAuthFilter -> blacklist "isRevoked(jti)"
        jwtAuthFilter -> auditService "Logs TOKEN_REVOKED"
        jwtAuthFilter -> userDetailsService "loadUserByUsername"

        securityConfig -> jwtAuthFilter "Registers in the filter chain"
        securityConfig -> authEntryPoint "401 entry point"
        securityConfig -> accessDeniedHandler "403 access-denied handler"
        securityConfig -> userDetailsService "DaoAuthenticationProvider"

        authEntryPoint -> problemDetailFactory "Builds 401 response"
        accessDeniedHandler -> problemDetailFactory "Builds 403 response"
        globalExceptionHandler -> problemDetailFactory "Builds 4xx/429 responses"

        blacklist -> redis "StringRedisTemplate"
        usuarioRepo -> postgres "JDBC/JPA"
        mascotaRepo -> postgres "JDBC/JPA"
        citaRepo -> postgres "JDBC/JPA"
        consultaRepo -> postgres "JDBC/JPA"
        vacunaRepo -> postgres "JDBC/JPA"
        procedimientoRepo -> postgres "Invokes stored procedures (@Procedure/@NamedStoredProcedureQuery)"

        frontend -> tomcatDual "HTTPS :8443 (public) / HTTP :8080 (internal)"
    }

    views {
        systemContext biopet "L1-SystemContext" {
            include *
            autoLayout tb
            title "BIOPET - C4 Level 1: System Context (v1.0.0)"
        }

        container biopet "L2-Containers" {
            include *
            autoLayout lr
            title "BIOPET - C4 Level 2: Containers (v1.0.0)"
        }

        component backend "L3-BackendComponents" {
            include *
            autoLayout tb
            title "BIOPET - C4 Level 3: Backend Components (v1.0.0)"
        }

        styles {
            element "Person" {
                shape person
                background #ECFEFF
            }
            element "Software System" {
                background #DBEAFE
            }
            element "External System" {
                background #F1F5F9
                color #64748B
                border dashed
            }
            element "Container" {
                background #F8FAFC
            }
            element "Database" {
                shape cylinder
                background #FDE68A
            }
            element "Component" {
                background #F8FAFC
            }
        }
    }

    // Reproducible export (documented per Z1 acceptance criterion; run from docs/diagrams/):
    //   1. Download structurizr-cli:  https://github.com/structurizr/cli/releases (requires JRE 11+)
    //   2. Export to PlantUML:        structurizr.sh export -workspace workspace.dsl -format plantuml
    //   3. Export to Mermaid:         structurizr.sh export -workspace workspace.dsl -format mermaid
    //   4. Render PlantUML -> PNG:    plantuml structurizr-L1-SystemContext.puml (etc.)
    // The .dot/.puml/.png files committed under c4-contexto/, c4-contenedores/ and
    // c4-componentes-backend/ are hand-aligned derivations of this DSL (rendered with
    // Graphviz, via @hpcc-js/wasm + @resvg/resvg-js since no system Graphviz/Structurizr
    // CLI was available at authoring time); they must stay in sync with this file on every
    // change and are the single reproducible source for the three levels of the C4 model.
}
