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
ambos límites vienen desactivados por defecto. Al configurar el flujo se deben
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

## Preparación antes de arrancar

En PowerShell, definir una contraseña temporal para el administrador local antes
de ejecutar Compose:

```powershell
$env:KC_BOOTSTRAP_ADMIN_PASSWORD = Read-Host "Contraseña temporal de Keycloak"
```

El nombre del administrador local por defecto es `local-admin`; puede cambiarse
con `KC_BOOTSTRAP_ADMIN_USERNAME`. No guardar la contraseña en el repositorio.

Cuando se decida arrancar el entorno, desde la raíz del repositorio:

```powershell
docker compose -f infra/keycloak/compose.yaml up --build -d
```

No se ha ejecutado este comando. Para el primer arranque, Keycloak creará la
base de datos de desarrollo en el volumen local `keycloak-data`.

## Configuración pendiente en la consola

La instalación de la extensión no activa por sí sola el flujo de autenticación.
El siguiente paso será configurar un realm de desarrollo, el servidor SMTP de
Mailpit y un flujo de navegador que solicite contraseña y luego Email OTP. Allí
se fijarán el código de seis dígitos, su caducidad y límites de emisión; también
se configurará el cliente OIDC y la emisión de roles que consume `identity-service`.

No reutilizar el usuario de prueba ni el almacenamiento integrado de desarrollo
en un despliegue real. El proveedor de OTP es software comunitario y requiere
revisión y pruebas de seguridad específicas antes de cualquier uso productivo.
