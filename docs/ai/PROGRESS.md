# PROGRESS · VeriVoto

Última actualización: 2026-10-29 · Rama de trabajo: `dev` (cambios de P1.4 sin commitear) · Último commit: `d061b56`

## Hitos

| Hito | Fechas | Objetivo | Estado |
| --- | --- | --- | --- |
| P0 | 29/10 – 02/11 | Línea base y memoria de la IA | Hecho (`da2e6db`) |
| P1 | 02/11 – 08/11 | Java 25, monorepo Maven, migración de `tenant-service`, esqueletos, infraestructura, CI de compilación | En curso (P1.2–P1.3 compilados con Java 25; P1.4 hecho, sin compilar) |
| P2 | 05/11 – 10/11 | Modelo de datos, separación de secreto, contratos OpenAPI de la rebanada 1, ADRs | Pendiente |
| P3 | 09/11 – 17/11 | Organizaciones y roles, configuración de Keycloak, censo, base del frontend y login | Pendiente |
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

## En curso

- P1: compilación local de P1.4 por el autor; después P1.5 (compose) y P1.6 (CI).

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
- ¿Qué significa «público» en RF-13? (P2)
- Librería para el acta PDF (RF-16). (P5)
- Retraso respecto al Gantt: la iteración 1 (RF-01–RF-03) acababa el 31/10.

## Cómo continuar (siguiente paso exacto)

1. Desde `backend/`: `.\mvnw.cmd -DskipTests compile`. Deben compilar los 5 módulos. Si falla,
   pasar la salida completa a la IA.
2. Commit de P1.4 en `chore/service-skeletons` e integración en `dev`.
3. Lanzar **P1.5**: `infra/compose.yaml` con `pg-main` y `pg-ballot` (`postgres:18.6`), Keycloak con su
   BD en `pg-main`, Mailpit y `.env.example`.
