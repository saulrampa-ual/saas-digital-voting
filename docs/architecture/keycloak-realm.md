# Realm `verivoto` de Keycloak (P3.2)

El realm está versionado en `infra/keycloak/realm/verivoto-realm.json` y Keycloak lo importa al arrancar
(`start-dev --import-realm`). Resuelve RF-04 y RF-05a por configuración (D-02); no hay código de autenticación propio.

## Contenido

| Elemento | Valor | Requisito |
| --- | --- | --- |
| Roles de realm | `SUPERADMIN`, `ADMIN`, `AUDITOR`, `CENSUS_MANAGER`, `SERVICE` | RF-02, D-22 |
| Flujo de navegador | `verivoto browser`: cookie, o redirección a IdP, o contraseña + Email OTP (ambos obligatorios) | RF-04, RF-05a |
| Email OTP | 6 dígitos (`0123456789`), caduca en 600 s, reenvío cada 60 s, máximo 5 códigos en 900 s; confianza por IP y por dispositivo desactivada | RF-04, RNF-14 |
| Bloqueo por fuerza bruta | 5 fallos; bloqueo temporal que crece de 60 s hasta 900 s; no permanente | RNF-14 |
| Política de contraseñas | 12 caracteres mínimo, distinta del usuario y del correo, sin repetir las 3 últimas. Keycloak guarda solo el hash con sal | RNF-14 |
| Sesiones | Access token 5 min; inactividad 30 min; máximo 8 h | RNF-14 |
| Idiomas | `es` (por defecto) y `en` | RNF-5 |
| SMTP | `mailpit:1025` sin TLS (solo desarrollo) | RF-04 |
| Perfil de usuario | `username`, `email`, `firstName`, `lastName` y `organization_id` (solo lo edita un administrador, máx. 20) | D-17 |

## Clientes

| Cliente | Tipo | Uso |
| --- | --- | --- |
| `verivoto-admin` | Público, Authorization Code + PKCE (S256) | Consola de gestión (`http://localhost:4200`) |
| `identity-service` | Confidencial, *service account* con `manage-users`, `view-users`, `query-users` de `realm-management` y rol `SERVICE` | Crear usuarios de gestión y asignar roles por la Admin API (RF-02) |
| `election-service` | Confidencial, *service account* con rol `SERVICE` | Llamar a `/internal/**` de voter-access y ballot-box |
| `ballot-box-service` | Confidencial, *service account* con rol `SERVICE` | Llamar a `/internal/**` de voter-access |

Todos los clientes llevan dos mappers propios que añaden al token:

- `roles`: lista de roles de realm (el claim que ya lee `SecurityConfiguration`).
- `organization_id`: atributo de usuario con el identificador de RI-1 (vacío para el Superadministrador).

## Secretos

El fichero no contiene secretos. Usa marcadores `${VAR}` que Keycloak resuelve con variables de entorno al
importar: `VERIVOTO_SUPERADMIN_EMAIL`, `VERIVOTO_SUPERADMIN_INITIAL_PASSWORD`,
`IDENTITY_SERVICE_CLIENT_SECRET`, `ELECTION_SERVICE_CLIENT_SECRET` y `BALLOT_BOX_SERVICE_CLIENT_SECRET`
(definidas en `infra/.env`).

## Superadministrador inicial

Usuario `superadmin` con rol `SUPERADMIN` y contraseña temporal: al primer acceso pide la contraseña, el
código por correo (llega a Mailpit, `http://localhost:8025`) y una contraseña nueva que cumpla la política.

## Cambiar el realm

Keycloak **no** reimporta un realm que ya existe. Tras modificar el JSON, en desarrollo:

1. Borrar el realm `verivoto` desde la consola de administración (`http://localhost:8080`, usuario `local-admin`).
2. Reiniciar Keycloak: `docker compose -f infra/compose.yaml --env-file infra/.env restart keycloak`.

Se pierden los usuarios creados en ese realm. Alternativa: `docker compose ... down -v` (borra todas las BD locales).

## SSO institucional (RF-05a)

El flujo ya incluye `identity-provider-redirector`. Para probar SSO hace falta un proveedor OIDC externo
(otra instancia o realm de Keycloak, o el IdP de una universidad). Se configura en la consola como
*Identity Provider* de tipo OpenID Connect. No se versiona porque cada organización tendrá el suyo.

## Comprobación manual

1. Consola: `http://localhost:8080/admin/master/console/#/verivoto` muestra el realm, sus roles y clientes.
2. Login del superadministrador: `http://localhost:8080/realms/verivoto/account`.
3. Token de una cuenta de servicio (PowerShell):

```powershell
$body = @{ grant_type = "client_credentials"; client_id = "election-service"; client_secret = "<ELECTION_SERVICE_CLIENT_SECRET>" }
$t = Invoke-RestMethod -Method Post -Uri http://localhost:8080/realms/verivoto/protocol/openid-connect/token -Body $body
$t.access_token.Split('.')[1]
```

Al decodificar el segundo fragmento (Base64URL) debe aparecer `"roles": [..., "SERVICE", ...]`.
