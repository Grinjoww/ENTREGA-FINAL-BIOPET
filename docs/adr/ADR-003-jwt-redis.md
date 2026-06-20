# ADR-003: Revocación de JWT mediante Redis

## Estado
Aceptado.

## Contexto
BIOPET usa autenticación stateless con JWT. En este enfoque el backend no conserva sesión del usuario; por eso, si un usuario cierra sesión, el token emitido seguiría siendo válido hasta su expiración si no existe un mecanismo de revocación.

## Decisión
Se implementa una blacklist de JWT en Redis. Cada token emitido contiene un claim `jti` único. Durante el logout, el backend almacena el `jti` en Redis con un TTL igual al tiempo restante de vida del token. En cada solicitud protegida, el `JwtAuthenticationFilter` valida firma, expiración y consulta Redis para confirmar que el `jti` no esté revocado.

## Consecuencias positivas
- Mantiene la autenticación stateless.
- Permite invalidar tokens luego del logout.
- Redis ofrece baja latencia para consultar tokens revocados.

## Consecuencias negativas
- El backend depende de Redis para validar revocación.
- Si Redis no está disponible, debe tratarse como error de infraestructura y no como sesión válida.
