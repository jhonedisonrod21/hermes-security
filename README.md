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

## Clave de firma JWT (perfil local)
La clave RSA con que el auth-server firma los JWT es un **secreto** y **no se
versiona** (quien la tenga puede falsificar tokens). En local se gestiona así:

- Se genera **una vez por máquina** en `.local-jwk/` (en `.gitignore`) con la
  tarea `./gradlew :hermes-auth-server:generateLocalJwk` (se ejecuta sola en
  `bootRun` y en `hermes-stack.sh up`).
- `bootRun` y el orquestador la cargan en `HERMES_AUTH_JWK_*` → la firma es
  **estable entre reinicios** (los tokens siguen validando).
- Si `.local-jwk/` no existe, el auth-server genera una clave **efímera** en
  memoria (los tokens dejan de validar al reiniciar).

Para prod/dev la clave llega por variable de entorno desde un gestor de secretos
(Vault), persistente y compartida entre instancias — nunca en git.

## Construir / correr
```bash
cd ../hermes-platform-shared && ./gradlew publishToMavenLocal
cd ../hermes-security && ./gradlew test
```

## Endurecimiento (gate base 1.0)
- **Secretos sin default en base** (fail-fast): `HERMES_INTERNAL_API_KEY`, `HERMES_WEB_CLIENT_SECRET`,
  JWK y cookie `Secure` se exigen por entorno; las comodidades de **local** viven en
  `application-local.yml`. Un arranque sin perfil/secreto no levanta.
- **Sin credenciales en migraciones.** El SYSTEM_ADMIN se crea en local con `LocalAdminSeeder`
  (`@Profile("local")`). En dev/prod se da de alta fuera de banda con password **BCrypt**.
- **Clave interna**: comparación en tiempo constante (`HermesInternalKeys`). La superficie
  `/internal/**` asume **red de confianza**: solo el gateway debe alcanzar los servicios
  (NetworkPolicy/mTLS es requisito de despliegue, no sustituible por la clave compartida).
- **Resiliencia**: los `RestClient` del Auth Server hacia identity/tenant llevan timeouts
  (`hermes.http.connect-timeout`/`read-timeout`).
- **Frescura de token**: `TokenSettings` explícitos (access 15m, refresh 8h, **rotación** de
  refresh). Cambios de rol/membresía se reflejan en la siguiente emisión; no hay revocación
  inmediata (decisión consciente; introspección/lista de revocación si se requiere).
- **JWK persistente y compartida** es obligatoria en dev/prod (HA / validez tras redeploy);
  el perfil `dev` ya falla si falta. La efímera solo aplica a local sin `.local-jwk`.
- **Aislamiento multi-tenant**: primitiva `HermesRequestContext` (en `hermes-shared`) que lee las
  cabeceras confiables `X-Hermes-*` del gateway. Todo servicio de datos **debe** acotar sus
  consultas por `ctx.requireTenant()`; nunca por input del cliente.
- **Observabilidad**: trazas correlacionadas (traceId/spanId) + métricas Prometheus habilitadas;
  requieren un backend (Zipkin/OTLP + Prometheus) en infraestructura.
- **Rate limiting**: por IP en el gateway para login/registro (en memoria, por instancia). A
  escala se requiere un limiter distribuido (Redis).

## Stack
Java 25 · Gradle 9.5 · Spring Boot 4.0.6 / Spring Cloud 2025.1.1 · Spring
Authorization Server.
