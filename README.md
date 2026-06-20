# hermes-security — IAM (Identidad y Acceso)

Dominio de **identidad, acceso y emisión de tokens** (Identity & Access
Management). Es la **autoridad de identidad** del sistema.

## Propósito
Autenticar usuarios, gestionar tenants/roles/permisos y emitir los JWT que el
resto de la plataforma valida.

## Servicios
| Servicio | Puerto | Responsabilidad |
|----------|--------|-----------------|
| hermes-auth-server | 9000 | OAuth2 Authorization Server (OIDC). Emite JWT, login de sesión, enriquece claims |
| hermes-identity-service | 8181 | Usuarios y credenciales. Endpoints internos de verificación/registro |
| hermes-tenant-service | 8082 | Tenants, membresías, roles y permisos. Endpoints internos de contexto/aprovisionamiento |

## Arquitectura / flujo de login
1. `auth-server` valida credenciales llamando a `identity`
   (`POST /internal/auth/credentials/verify`).
2. Obtiene el contexto de tenant desde `tenant`
   (`GET /internal/users/{id}/tenant-context/default`).
3. Emite un JWT firmado (clave RSA / JWK) con claims Hermes: `user_id`,
   `tenant_id`, `roles`, `permissions`, etc.

Las llamadas internas entre estos servicios se autorizan con la cabecera
compartida `X-Hermes-Internal-Key` (constante en `hermes-shared`).

## Consideraciones técnicas
- **Base de datos `HERMES_IAM`** (MySQL). Flyway por servicio
  (`flyway_schema_history_{auth,identity,tenant}`), `ddl-auto: validate`.
- Consume **`hermes-shared`** (contratos internos) desde **mavenLocal** →
  publicar primero `hermes-platform-shared`.
- Clave de firma JWK: por variable de entorno (prod/dev) o **efímera** en local.
  Para alta disponibilidad debe ser persistente y compartida entre instancias.
- Perfiles `local` / `dev`; en `dev` los secretos no tienen valor por defecto
  (fail-fast: el servicio no arranca si falta una variable).
- Las relaciones con otros contextos son por **UUID vía HTTP**, no por claves
  foráneas cruzadas → el contexto es autónomo.

## Construir / correr
```bash
cd ../hermes-platform-shared && ./gradlew publishToMavenLocal
cd ../hermes-security && ./gradlew test
```

## Stack
Java 25 · Gradle 9.5 · Spring Boot 4.0.6 / Spring Cloud 2025.1.1 · Spring
Authorization Server.
