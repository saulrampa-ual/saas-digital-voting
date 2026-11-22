# VeriVoto · backend

Monorepo Maven con cinco servicios Spring Boot. Para una guía de inicio desde cero, prepara primero
`infra/.env` y Docker Compose siguiendo la [guía principal](../README.md) y
[Infraestructura local](../docs/architecture/local-infrastructure.md).

## Requisitos

- Java 25.
- Docker Desktop con Docker Compose v2, con PostgreSQL, Keycloak, Mailpit y LibreTranslate iniciados.
- Conexión a Internet la primera vez para que Maven Wrapper descargue Maven y las dependencias.

## Variables de entorno

Desde la raíz del repositorio, crea `infra/.env` a partir de `infra/.env.example` y completa todos los
valores vacíos con contraseñas aleatorias distintas. El archivo contiene contraseñas de PostgreSQL,
Keycloak y las cuentas de servicio; no lo publiques ni lo compartas.

El script `scripts/Import-DotEnv.ps1` carga esas variables en la terminal PowerShell actual. Cada
servicio necesita además su usuario fijo de base de datos: `identity`, `election`, `voter_access` o
`ballot_box`. La configuración local y los valores por defecto están en cada
`services/*/src/main/resources/application.yml`.

## Arrancar los servicios

Inicia primero la infraestructura desde la raíz:

```powershell
docker compose -f infra/compose.yaml --env-file infra/.env up -d --build
```

Abre cinco terminales PowerShell, todas inicialmente en la raíz del repositorio. Ejecuta un bloque en
cada terminal; deja cada proceso en ejecución. El script carga `infra/.env` en la sesión y Flyway crea
o actualiza el esquema al iniciar cada servicio.

**Terminal 1 — identity-service:**

```powershell
. .\scripts\Import-DotEnv.ps1
$env:IDENTITY_DB_USERNAME = "identity"
Set-Location backend
.\mvnw.cmd -pl services/identity-service spring-boot:run
```

**Terminal 2 — election-service:**

```powershell
. .\scripts\Import-DotEnv.ps1
$env:ELECTION_DB_USERNAME = "election"
Set-Location backend
.\mvnw.cmd -pl services/election-service spring-boot:run
```

**Terminal 3 — voter-access-service:**

```powershell
. .\scripts\Import-DotEnv.ps1
$env:VOTER_ACCESS_DB_USERNAME = "voter_access"
Set-Location backend
.\mvnw.cmd -pl services/voter-access-service spring-boot:run
```

**Terminal 4 — ballot-box-service:**

```powershell
. .\scripts\Import-DotEnv.ps1
$env:BALLOT_BOX_DB_USERNAME = "ballot_box"
Set-Location backend
.\mvnw.cmd -pl services/ballot-box-service spring-boot:run
```

**Terminal 5 — api-gateway:**

```powershell
Set-Location backend
.\mvnw.cmd -pl services/api-gateway spring-boot:run
```

El gateway usa los puertos y direcciones locales por defecto. Sus servicios de destino deben estar
activos para que las rutas respondan correctamente.

## Comprobación y uso

| Servicio | Puerto | Comprobación |
| --- | ---: | --- |
| identity-service | 8081 | `http://localhost:8081/actuator/health` |
| election-service | 8082 | `http://localhost:8082/actuator/health` |
| voter-access-service | 8083 | `http://localhost:8083/actuator/health` |
| ballot-box-service | 8084 | `http://localhost:8084/actuator/health` |
| api-gateway | 8000 | `http://localhost:8000` |

Swagger UI está disponible en `http://localhost:<puerto>/swagger-ui.html` en los servicios que
publican API. Para autenticarse en Swagger se usa Keycloak y el cliente local
`verivoto-dev-swagger`. El código de acceso por correo se consulta en Mailpit (`http://localhost:8025`).

La guía [Probar las APIs en local](../docs/architecture/api-testing.md) incluye ejemplos de operaciones,
cuentas sintéticas y comprobaciones de roles. El manual de [roles y simulación](../docs/evaluation/resultados-p7/manual-simulacion-roles.md)
describe qué puede hacer cada tipo de cuenta.
