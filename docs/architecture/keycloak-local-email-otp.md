# Entorno local de Keycloak y OTP por correo

Este entorno prepara el proveedor OIDC para desarrollo y pruebas manuales de
RF-04. Incluye Keycloak, la extensión comunitaria Email OTP Authenticator y
Mailpit para recibir correos localmente. No se debe usar como configuración de
producción.

## Versiones fijadas

- Keycloak: `26.8.1`.
- Email OTP Authenticator: `v1.8.1`, compilado para Keycloak `26.8.1`.
- Mailpit: `v1.31.5`.

La imagen personalizada descarga el JAR versionado durante su construcción y
verifica su SHA-256. La extensión permite limitar reenvíos y códigos emitidos;
ambos límites vienen desactivados por defecto.
La imagen también fija en tiempo de build `KC_DB=postgres` y `KC_HEALTH_ENABLED=true` (health en el
puerto de gestión 9000, no publicado). Al configurar el flujo se deben
activar y dejar desactivadas las opciones de confianza de IP y dispositivo para
que cada acceso siga solicitando el segundo factor.

## Servicios y puertos locales

| Servicio | Dirección | Uso |
| --- | --- | --- |
| Keycloak | `http://localhost:8080` | Consola, login y OIDC |
| Mailpit web | `http://localhost:8025` | Consultar los correos de prueba |
| Mailpit SMTP | `localhost:1025` | Envío desde Keycloak sin credenciales ni TLS |

Los puertos se enlazan únicamente a `127.0.0.1`. Para el servidor de correo del
realm, usar host `mailpit`, puerto `1025`, sin autenticación y sin TLS.

## Arranque

Keycloak forma parte de la infraestructura local común (`infra/compose.yaml`) y guarda sus datos en
la base `keycloak_db` de `pg-main`. Los pasos de arranque están en `local-infrastructure.md`.
La contraseña del administrador local se define en `infra/.env` (`KC_BOOTSTRAP_ADMIN_PASSWORD`),
que no se versiona.

## Configuración pendiente en la consola

La instalación de la extensión no activa por sí sola el flujo de autenticación.
El siguiente paso será configurar un realm de desarrollo, el servidor SMTP de
Mailpit y un flujo de navegador que solicite contraseña y luego Email OTP. Allí
se fijarán el código de seis dígitos, su caducidad y límites de emisión; también
se configurará el cliente OIDC y la emisión de roles que consume `identity-service`.

No reutilizar el usuario de prueba ni el almacenamiento integrado de desarrollo
en un despliegue real. El proveedor de OTP es software comunitario y requiere
revisión y pruebas de seguridad específicas antes de cualquier uso productivo.
