# PROGRESS · VeriVoto

Última actualización: 2026-10-29 · Rama de trabajo: `dev` (P3.1 sin commitear) · Último commit: `947ad90`

## Hitos

| Hito | Fechas | Objetivo | Estado |
| --- | --- | --- | --- |
| P0 | 29/10 – 02/11 | Línea base y memoria de la IA | Hecho (`da2e6db`) |
| P1 | 02/11 – 08/11 | Java 25, monorepo Maven, migración de `tenant-service`, esqueletos, infraestructura, CI de compilación | Hecho (CI en verde, `efe8afc`) |
| P2 | 05/11 – 10/11 | Modelo de datos, separación de secreto, contratos OpenAPI de la rebanada 1, ADRs | Hecho (`ce6cb37`) |
| P3 | 09/11 – 17/11 | Organizaciones y roles, configuración de Keycloak, censo, base del frontend y login | En curso (P3.2 importado; P3.1 escrito, sin compilar) |
| P4 | 16/11 – 30/11 | Rebanada vertical del voto (RF-06–10, RF-20) | Pendiente |
| P5 | 30/11 – 14/12 | Auditoría, escrutinio y resultados (RF-12–16), modelo de amenazas | Pendiente |
| P6 | 14/12 – 31/12 | Consola y transversales (RF-05b, RF-17–19, RF-21) | Pendiente |
| P7 | 01/01 – 06/01/2027 | Pruebas y medición (H4) | Bloqueada (`ACTIVAR TESTS`) |
| P8 | 01/12 – 27/01/2027 | Memoria del TFM | Pendiente |
| P9 | Tras la entrega | Post-MVP: RF-11, RF-22, RF-23 | Fuera de alcance |

## Hecho

- Antes de P0 (commits hasta `09f21e1`): estructura de carpetas; `tenant-service` con el caso de uso
  «crear organización» (dominio, puertos, servicio, JPA, Flyway V1, REST, resource server JWT con
  `ROLE_SUPERADMIN`); entorno local de Keycloak 26.8.1 + Email OTP v1.8.1 + Mailpit v1.31.5 con su
  documentación; prototipos en `prototiposPantallas/`. No se ha compilado ni arrancado nada.
- P0.1: `README.md` y `.gitignore` sin el envoltorio de PowerShell; el script del antiguo `.gitkeep`
  movido a `scripts/create-folders.ps1`.
- P0.2: `CLAUDE.md`.
- P0.3: `docs/ai/` (CONTEXT, PROGRESS, DECISIONS, TRACEABILITY, IMPROVEMENTS, PROTOTYPES).
- P0.4: skills en `.claude/skills/`.
- P1.2: `backend/pom.xml` padre (`verivoto-backend`): Spring Boot 4.1.2, `java.version=25`, BOM Spring
  Cloud 2025.1.3, módulo `services/identity-service`. Maven Wrapper 3.3.4 (*only-script*, Maven 3.9.16)
  en `backend/` y `backend/.gitattributes` para los finales de línea de `mvnw` y `mvnw.cmd`.
- P1.3: `tenant-service` → `identity-service`. Paquete `com.saasvoting.tenant` → `com.saasvoting.identity`;
  puertos en `application/port`, adaptadores en `infrastructure/adapter`, configuración en
  `infrastructure/config`. Renombradas `IdentityServiceApplication` e `IdentityServiceConfiguration`.
  Sin cambios de lógica. `domain` y `application` compilados con `javac` de forma aislada; el módulo
  completo no se ha compilado con Maven (sin acceso a Maven Central desde el entorno de la IA).
  Cambios de configuración: `application.properties` → `application.yml`; variables `TENANT_*` →
  `IDENTITY_*`; BD por defecto `tenant_service` → `identity_db` (D-08); puerto 8081 (D-09).
  Compilado por el autor con `mvnw` y Java 25 (commit `5ed5949`).
- P1.4: esqueletos de `api-gateway` (Gateway Server WebFlux, ruta `/api/v1/tenants/**` → identity),
  `election-service`, `voter-access-service` y `ballot-box-service` (main, paquetes hexagonales con
  `package-info.java`, `application.yml`, `/actuator/health`, Flyway sin migraciones, springdoc 3.1.1).
  `identity-service` recibe Actuator, springdoc y acceso público a `/actuator/health` y a la
  documentación OpenAPI. Plantilla documentada en `docs/architecture/hexagonal-template.md`.
  Compilado por el autor: los 5 módulos con `BUILD SUCCESS` (commit `d6f6bc6`).
- P1.5: `infra/compose.yaml` (proyecto `verivoto`) con `pg-main` y `pg-ballot` (`postgres:18.6`),
  Keycloak 26.8.1 con BD en `keycloak_db` y health en el puerto 9000, Mailpit. Scripts de inicialización
  con un rol por base de datos (probados con PostgreSQL 16 en el entorno de la IA), `infra/.env.example`,
  `.gitattributes` para los `.sh` y `docs/architecture/local-infrastructure.md`. Commit `14a2b33`
  (borra `infra/keycloak/compose.yaml`). El autor corrigió el Dockerfile de Keycloak: `USER root` y
  permisos 644 del jar del OTP, que `ADD` desde URL deja ilegible para el usuario `keycloak`.
  Incidencia: `14a2b33` incluía contraseñas locales en `infra/.env.example`; se restaura la plantilla
  vacía y se corrige el commit antes de publicarlo (no llegó a GitHub).
- P1.6: `.github/workflows/build.yml` con dos jobs: compilación del backend con Temurin 25
  (`actions/checkout@v7`, `actions/setup-java@v6`, caché Maven, `mvnw -DskipTests verify`) y
  validación de `infra/compose.yaml` con `docker compose config` y valores de prueba. Validado en el
  entorno de la IA: YAML del workflow y `docker compose config` (Compose v5.6.1). Primera ejecución en
  GitHub en verde (confirmado por el autor). El commit con contraseñas se rehízo sin ellas (`9ebbf84`).
- P2: `docs/architecture/data-model.md` (modelo por servicio con diagramas ER, separación de secreto,
  flujo del voto con token de un solo uso, riesgos residuales); contratos OpenAPI 3.1 de identity,
  election, voter-access y ballot-box en `src/main/resources/openapi/` (validados con
  `openapi-spec-validator`); ADR-0001 a ADR-0009 en `docs/adr/`; decisiones D-16 a D-23.
- P3.2: realm `verivoto` en `infra/keycloak/realm/verivoto-realm.json` (roles, flujo contraseña + Email OTP,
  bloqueo, política de contraseñas, clientes `verivoto-admin`, `identity-service`, `election-service`,
  `ballot-box-service`, claims `roles` y `organization_id`, perfil de usuario, superadministrador inicial).
  Importación en el compose con secretos por variables (D-24). Validado: JSON, referencias internas y
  `docker compose config`; los nombres de autenticadores, mappers y campos se comprobaron en el código fuente
  de Keycloak 26.8.1 y de Email OTP v1.8.1. Importado por el autor (`947ad90`); el primer login de
  `superadmin` falló por credenciales (4 intentos), probablemente al teclear la contraseña inicial.
- P3.1: `identity-service` con organizaciones (RF-01) y usuarios de gestión (RF-02) según el contrato:
  `Tenant` → `Organization` (ficheros renombrados con `mv`), `Caller` con las reglas de acceso por
  organización (RNF-15), puertos `IdentityProvider`, `OrganizationRepository` y `ManagementUserRepository`,
  adaptador `KeycloakAdminClient` (Admin API con *client credentials*), controladores, `ApiExceptionHandler`
  (IMP-01), migraciones V2 (renombrado) y V3 (`management_users`), ruta del gateway `/api/v1/organizations`.
  Realm: `firstName` y `lastName` dejan de ser obligatorios (requiere reimportar). Verificado en el entorno de
  la IA: `domain` y `application` compilan con `javac`; V1–V3 se aplican sobre PostgreSQL 16. Falta compilar
  con Maven y probar contra Keycloak.

## En curso


## Pendiente

- Validar con el tutor la reducción a 5 servicios (D-05) y la exclusión del revoto (D-03).
- Corregir el Gantt del TFM (`plan_gantt.tex`): la iteración 3 debe quedar en RF-09–RF-10.
- Verificar el último parche de Spring Cloud 2025.1.x, springdoc 3.1.x y Maven (P1.1); de Ionic 9 (P3.4).
- Prototipos: pantallas que faltan para RF-17 y RF-21 (ver `PROTOTYPES.md`).
- Seguridad de `election`, `voter-access` y `ballot-box`: los esqueletos no incluyen Spring Security;
  se añade en el hito de cada servicio (P3.3, P4.3, P4.4).
- CORS y API keys en el gateway (P6); rutas de los demás servicios cuando existan sus contratos (P2).

## Bloqueos / preguntas abiertas

- ¿Se activan las pruebas de propiedades críticas al terminar P4? (IMP-03)
- Librería para el acta PDF (RF-16). (P5)
- Retraso respecto al Gantt: la iteración 1 (RF-01–RF-03) acababa el 31/10.

## Cómo continuar (siguiente paso exacto)

1. Compilar: `.\mvnw.cmd -DskipTests compile` en `backend/`.
2. Reimportar el realm (borrar `verivoto` en la consola y reiniciar Keycloak) y entrar como `superadmin`.
3. Arrancar `identity-service` con `IDENTITY_DB_USERNAME`, `IDENTITY_DB_PASSWORD` e
   `IDENTITY_SERVICE_CLIENT_SECRET` y probar alta de organización y de usuario (ver resumen de la sesión).
4. Commit en `feat/identity-organizations-users` e integración en `dev`.
5. Siguiente: **P3.3** (censo en `election-service`, RF-03).
