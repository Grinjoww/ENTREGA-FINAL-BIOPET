# BIOPET — Entrega 1B

Proyecto Fin de Curso de Aplicaciones Web. Esta entrega implementa el módulo de autenticación JWT stateless y el CRUD principal de **Mascotas** para el sistema veterinario BIOPET.

## Stack

- Java 21
- Spring Boot 3.2.x
- Spring Security 6
- jjwt 0.12.x
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Flyway
- Redis 7
- Angular 17+
- Docker Compose

## Ejecución en 5 pasos

```bash
# 1. Clonar o descomprimir el proyecto
cd BIOPET-Entrega1B

# 2. Copiar variables de entorno
cp .env.example .env

# 3. Levantar todos los servicios
docker compose up --build -d

# 4. Verificar estado healthy
docker compose ps

# 5. Acceder a la aplicación
# Frontend: http://localhost:4200
# Swagger UI: http://localhost:8080/api/swagger-ui.html
# Actuator Health: http://localhost:8080/actuator/health
```

## Pruebas automatizadas

```bash
cd backend
mvn test
```

## Endpoints principales

| Método | URL | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | No | Registrar usuario |
| POST | `/api/auth/login` | No | Iniciar sesión y emitir tokens |
| POST | `/api/auth/logout` | JWT | Revocar JTI en Redis |
| POST | `/api/auth/refresh` | Refresh token | Emitir nuevo accessToken |
| GET | `/api/usuarios/me` | JWT | Perfil autenticado |
| GET | `/api/mascotas?page=0&size=10&sort=id,asc` | JWT | Listado paginado |
| GET | `/api/mascotas/{id}` | JWT | Buscar mascota |
| POST | `/api/mascotas` | JWT | Crear mascota |
| PUT | `/api/mascotas/{id}` | JWT | Actualizar mascota |
| DELETE | `/api/mascotas/{id}` | JWT | Soft delete |

## Credenciales de prueba sugeridas

Primero registra un usuario mediante `/api/auth/registro`:

```json
{
  "nombre": "Jaime Mariscal",
  "email": "jaime@biopet.com",
  "password": "ClaveSegura123*",
  "rol": "ROLE_ADMIN"
}
```

Luego usa `/api/auth/login` con el mismo email y contraseña.

## Estructura

```text
backend/                 API Spring Boot
frontend/                SPA Angular 17 servida con Nginx
database/migrations/     Copia documental de migraciones Flyway
docs/                    Informe, diagramas, ADR, Postman y diccionario de datos
.github/workflows/       CI básico
```

## Git requerido para la entrega

```bash
git checkout -b develop
git checkout -b feature/autenticacion-jwt
# commits: feat: implementar autenticación JWT, test: cubrir login, docs: documentar JWT

git checkout develop
git merge feature/autenticacion-jwt

git checkout -b feature/crud-mascotas
# commits: feat: implementar CRUD mascotas, test: cubrir acceso protegido

git checkout develop
git merge feature/crud-mascotas
git tag v0.1.0-entrega-1b
```
