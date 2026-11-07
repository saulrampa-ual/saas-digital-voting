# Infraestructura local

Todo se levanta con un solo Compose: `infra/compose.yaml`. Solo para desarrollo: sin TLS y con los
puertos enlazados a `127.0.0.1`.

## Servicios

| Contenedor | Imagen | Puerto local | Uso |
| --- | --- | --- | --- |
| `pg-main` | `postgres:18.6` | 5432 | `identity_db`, `election_db`, `voter_access_db`, `keycloak_db` |
| `pg-ballot` | `postgres:18.6` | 5433 | `ballot_box_db` (aislada, RNF-11) |
| `keycloak` | `quay.io/keycloak/keycloak:26.8.1` + Email OTP v1.8.1 | 8080 | Identidad de gestión (OIDC) |
| `mailpit` | `axllent/mailpit:v1.31.5` | 1025 (SMTP), 8025 (web) | Correo de pruebas |

Keycloak importa el realm `verivoto` en el primer arranque (`keycloak-realm.md`).

Cada base de datos tiene su propio rol, que solo puede conectarse a ella (`REVOKE ALL ... FROM PUBLIC`).
Los roles se crean en el primer arranque con `infra/postgres/*/01-*.sh`.

| Base de datos | Rol | Instancia |
| --- | --- | --- |
| `identity_db` | `identity` | `pg-main` |
| `election_db` | `election` | `pg-main` |
| `voter_access_db` | `voter_access` | `pg-main` |
| `keycloak_db` | `keycloak` | `pg-main` |
| `ballot_box_db` | `ballot_box` | `pg-ballot` |

## Primer arranque (Windows, PowerShell, Docker Desktop)

```powershell
Copy-Item infra/.env.example infra/.env
# Rellenar infra/.env con contraseñas locales
docker compose -f infra/compose.yaml --env-file infra/.env up -d --build
docker compose -f infra/compose.yaml ps
```

Comprobaciones:
- `pg-main` y `pg-ballot` en estado `healthy`.
- Keycloak en `healthy` y consola en `http://localhost:8080`.
- Mailpit en `http://localhost:8025`.

## Ejecutar un servicio contra la infraestructura

Variables de entorno (por ejemplo, en la configuración de ejecución de IntelliJ):

| Servicio | Variables |
| --- | --- |
| identity-service | `IDENTITY_DB_USERNAME=identity`, `IDENTITY_DB_PASSWORD`, `IDENTITY_OIDC_ISSUER_URI` (por defecto `http://localhost:8080/realms/verivoto`), `IDENTITY_SERVICE_CLIENT_SECRET` (el mismo de `infra/.env`) |
| election-service | `ELECTION_DB_USERNAME=election`, `ELECTION_DB_PASSWORD` |
| voter-access-service | `VOTER_ACCESS_DB_USERNAME=voter_access`, `VOTER_ACCESS_DB_PASSWORD` |
| ballot-box-service | `BALLOT_BOX_DB_USERNAME=ballot_box`, `BALLOT_BOX_DB_PASSWORD` |

## Reiniciar desde cero

Los scripts de inicialización solo se ejecutan con volúmenes vacíos. Para regenerar las bases de datos
(se pierden todos los datos locales):

```powershell
docker compose -f infra/compose.yaml down -v
```

## Notas

- PostgreSQL 18 monta el volumen en `/var/lib/postgresql` (no en `/var/lib/postgresql/data`, como en
  versiones anteriores).
- Los scripts `.sh` deben tener finales de línea LF; lo garantiza `.gitattributes` en la raíz.
