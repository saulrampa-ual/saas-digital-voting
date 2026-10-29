# Probar las APIs en local

Cada servicio publica Swagger UI en `http://localhost:<puerto>/swagger-ui.html`. El botón **Authorize** inicia
sesión en Keycloak (contraseña + código por correo) con el cliente de desarrollo `verivoto-dev-swagger`
(Authorization Code + PKCE, D-26) y envía el token en cada petición.

## 1. Preparar Keycloak (una sola vez)

Si el realm `verivoto` ya estaba importado antes de existir el cliente de desarrollo:

1. `http://localhost:8080` → usuario `local-admin` → realm `verivoto`.
2. *Realm settings* → *Action* → *Partial import* → fichero `infra/keycloak/dev/verivoto-dev-swagger-client.json`
   → *If a resource exists: Skip* → *Import*.

En una instalación nueva no hace falta: el cliente ya está en `verivoto-realm.json`.

## 2. Arrancar identity-service (PowerShell, desde la raíz del repo)

```powershell
. .\scripts\Import-DotEnv.ps1                     # carga infra/.env en esta sesión
$env:IDENTITY_DB_USERNAME = "identity"
cd backend
.\mvnw.cmd -pl services/identity-service spring-boot:run
```

Usa `IDENTITY_DB_PASSWORD` e `IDENTITY_SERVICE_CLIENT_SECRET` de `infra/.env`. Al arrancar, Flyway aplica V1–V3
en `identity_db`. Health: `http://localhost:8081/actuator/health`.

En IntelliJ, la configuración de ejecución equivalente necesita esas tres variables de entorno.

## 3. Recorrido de prueba (RF-01, RF-02)

1. `http://localhost:8081/swagger-ui.html` → **Authorize** → *client_id* ya relleno → marcar `openid` →
   **Authorize** → login de `superadmin` → código desde Mailpit (`http://localhost:8025`).
2. `POST /api/v1/organizations`:
   ```json
   { "id": "UAL", "name": "Universidad de Almería",
     "initialAdministrator": { "fullName": "Ana Pérez", "email": "ana@ual.test" } }
   ```
   Esperado: **201**. En Mailpit llega a `ana@ual.test` el correo para fijar la contraseña.
3. Repetir el mismo POST → **409** «Ya existe una organización con ese identificador.»
4. `GET /api/v1/organizations` → la lista contiene `UAL`.
5. `GET /api/v1/organizations/UAL/users` → `Ana Pérez` con rol `ADMIN`.
6. `PUT /api/v1/organizations/UAL/users/{id de Ana}/role` con `{ "role": "AUDITOR" }` → **409** «La organización
   debe tener al menos un administrador.»
7. `POST /api/v1/organizations/UAL/users` con `{ "fullName": "Luis Gil", "email": "luis@ual.test", "role": "CENSUS_MANAGER" }`
   → **201**.
8. Aislamiento (RNF-15): fijar la contraseña de Ana desde el correo, **Logout** en Swagger, **Authorize** como
   `ana@ual.test` y pedir `GET /api/v1/organizations` → **403**; `GET /api/v1/organizations/OTRA` → **404**.

En la consola de Keycloak, los usuarios creados aparecen con el atributo `organization_id = UAL` y su rol.
