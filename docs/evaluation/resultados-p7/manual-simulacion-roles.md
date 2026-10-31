# Manual para revisar roles y simular una votación

Guía de desarrollo local para crear usuarios ficticios, comprobar permisos y recorrer una votación completa. Usa únicamente el realm local `verivoto`, las organizaciones `TESTA`/`TESTB` y los correos reservados `.test`. No reutilices estas cuentas en otro entorno.

## 1. Cuentas que se crearán

`superadmin` ya viene en el realm de desarrollo; no crees una cuenta duplicada. En el primer arranque, completa su acceso usando la contraseña inicial local configurada en `infra/.env` y cambia la contraseña cuando Keycloak lo solicite. No escribas credenciales en este manual. Las otras cuentas se dan de alta desde identity-service y reciben un correo de establecimiento de contraseña que puedes abrir en Mailpit.

| Organización | Nombre | Correo sintético | Rol | Para revisar |
| --- | --- | --- | --- | --- |
| TESTA | Alex Admin A | `alex.admin@verivoto.test` | ADMIN | Administración de la organización y comparación con TESTB |
| TESTA | Andrea Admin A | `andrea.admin@verivoto.test` | ADMIN | Mantener el segundo ADMIN al probar cambios de rol |
| TESTA | Cris Censo A | `cris.censo@verivoto.test` | CENSUS_MANAGER | Alta/importación de censos |
| TESTA | Dani Auditor A | `dani.auditor@verivoto.test` | AUDITOR | Procesos, resultados, auditoría y trazabilidad |
| TESTB | Bea Admin B | `bea.admin@verivoto.test` | ADMIN | Comprobar el aislamiento entre organizaciones |

La integración no es un usuario de inicio de sesión: crea una clave API para TESTA como ADMIN. Keycloak genera el cliente confidencial `INTEGRATION`; conserva el secreto solo en un gestor local y nunca lo pegues en informes, capturas o el repositorio.

## 2. Levantar el entorno local

Sigue [Infraestructura local](../../architecture/local-infrastructure.md) para preparar `infra/.env` y arrancar las bases, Keycloak y Mailpit. No uses `down -v`: elimina las bases locales. Arranca después los seis procesos de aplicación conforme a la configuración del proyecto:

| Componente | URL local |
| --- | --- |
| Keycloak | `http://localhost:8080` |
| Mailpit | `http://localhost:8025` |
| identity-service | `http://localhost:8081` |
| election-service | `http://localhost:8082` |
| voter-access-service | `http://localhost:8083` |
| ballot-box-service | `http://localhost:8084` |
| API gateway | `http://localhost:8000` |
| Frontend | `http://localhost:4200` |

Comprueba `/actuator/health` en los servicios. Ejecuta una instancia por servicio; utiliza una terminal separada para cada proceso. La guía [Probar las APIs en local](../../architecture/api-testing.md) detalla las variables de base de datos, Swagger y OAuth. Para desarrollo, abre Swagger de identity en `http://localhost:8081/swagger-ui.html` y la consola en `http://localhost:4200/admin`.

## 3. Crear las organizaciones y usuarios

1. En Swagger de identity, pulsa **Authorize**. Usa el cliente local `verivoto-dev-swagger`, inicia sesión como `superadmin` y lee el código de acceso en Mailpit. No copies el token a un fichero.
2. Ejecuta `POST /api/v1/organizations` para TESTA:

   ```json
   {
     "id": "TESTA",
     "name": "Organización sintética A",
     "initialAdministrator": {
       "fullName": "Alex Admin A",
       "email": "alex.admin@verivoto.test"
     }
   }
   ```

   Esperado: `201`. La operación crea a Alex como ADMIN y envía el enlace de primer acceso a Mailpit.
3. Repite para TESTB con nombre `Organización sintética B`, administrador `Bea Admin B` y correo `bea.admin@verivoto.test`.
4. Como `superadmin`, en `POST /api/v1/organizations/TESTA/users`, crea los otros tres usuarios de TESTA:

   ```json
   { "fullName": "Andrea Admin A", "email": "andrea.admin@verivoto.test", "role": "ADMIN" }
   { "fullName": "Cris Censo A", "email": "cris.censo@verivoto.test", "role": "CENSUS_MANAGER" }
   { "fullName": "Dani Auditor A", "email": "dani.auditor@verivoto.test", "role": "AUDITOR" }
   ```

   Haz una petición por cada objeto. Si Swagger pide volver a autenticar, utiliza `superadmin`; también se puede hacer cada alta como Alex ADMIN. Esperado: `201` por usuario.
5. Abre en Mailpit cada mensaje de establecimiento de contraseña. Cada persona fija una contraseña distinta que cumpla la política local y completa el código de correo al iniciar sesión. No guardes la contraseña en este repo.
6. Para probar el cambio de rol y la protección del último administrador, conserva los dos ADMIN de TESTA. Solo cambia el rol de Andrea; intenta cambiar el de Alex cuando sea el único ADMIN y comprueba el rechazo `409`.

El contrato de identity documenta el formato de estas peticiones en `backend/services/identity-service/src/main/resources/openapi/identity-service.yaml`. Para listar usuarios usa `GET /api/v1/organizations/{organizationId}/users`.

## 4. Qué revisar en cada rol

Prueba primero los accesos visibles en el frontend y después confirma los permisos en API/Swagger. Un guard del navegador por sí solo no cuenta como control de autorización.

| Rol | Debe poder | Debe quedar denegado |
| --- | --- | --- |
| SUPERADMIN | Inicio y Organizaciones; crear TESTA/TESTB y gestionar sus usuarios | Censos, procesos y datos de una organización: no tiene `organization_id` |
| ADMIN de TESTA | Procesos, participación, resultados, trazabilidad, privacidad y claves API; gestionar usuarios de TESTA | Crear/editar/importar censo; acceder a TESTB |
| CENSUS_MANAGER de TESTA | Ver y gestionar censos de TESTA | Procesos, participación, resultados, trazabilidad y claves API |
| AUDITOR de TESTA | Consultar procesos, resultados, auditoría y trazabilidad de TESTA | Censos, participación, privacidad, gestión de usuarios y escritura de procesos |
| ADMIN de TESTB | Acceso administrativo limitado a TESTB | Leer o cambiar TESTA; para recursos ajenos la API suele responder `404` |
| INTEGRATION de TESTA | Lecturas API documentadas de TESTA | Escrituras y datos de censo/personales, urna, acta, trazabilidad, usuarios y claves |

### Comprobaciones rápidas

1. Entra como cada usuario en una sesión privada diferente o cierra la sesión antes de cambiar de rol.
2. Confirma que la barra de navegación muestra las opciones de la tabla.
3. Intenta abrir rutas no permitidas directamente, por ejemplo `/admin/censos` como AUDITOR o `/admin/auditoria` como ADMIN; debe llevar a «Sin permiso» o no mostrar la ruta.
4. En Swagger, repite una operación denegada: espera `403` si el rol no tiene permiso, `404` si el recurso pertenece a otra organización y `401` sin token.
5. Como ADMIN de TESTA, crea una clave desde «Claves de API». Obtén un token OAuth `client_credentials` y verifica lecturas y escrituras denegadas. Revoca la clave al terminar. No publiques el secreto ni el token.

Las capacidades exactas están en `frontend/src/app/app.routes.ts`, `frontend/src/app/features/admin/admin-shell.component.ts` y los contratos OpenAPI. Si interfaz y API difieren, anota ruta, rol, petición, código HTTP y resultado; no corrijas producción durante la revisión.

## 5. Simulación sintética de una votación

1. Entra como Cris (`CENSUS_MANAGER`) en la consola. Crea un censo de prueba para TESTA e importa `scripts/samples/voters-sample.csv`. El fichero es sintético: el modo de prueba previa informa de cuatro entradas válidas, una repetida y tres inválidas. Comprueba la vista previa; aplica los datos válidos descartando líneas inválidas.
2. Como Alex (`ADMIN`), crea un proceso de opción única con dos opciones, texto en español e inglés, voto en blanco habilitado y fechas próximas. Selecciona el censo recién creado. Usa un nombre que identifique la simulación, por ejemplo `SIM-P7-AAAA-MM-DD`.
3. Programa el proceso. Confirma que pasa por `SCHEDULED` y `OPEN`; el proceso debe estar abierto antes de invitar a los votantes.
4. En Mailpit revisa los correos sintéticos. Abre el enlace personal de un votante y solicita el código. Introduce el OTP recibido, vota y guarda el recibo ficticio para consultar su verificación.
5. Como AUDITOR, comprueba el proceso, la auditoría del escrutinio y los resultados cuando el proceso se cierre y se escrute. Como ADMIN, compara los resultados y la participación agregada. No intentes vincular la identidad del votante con su opción.
6. Repite las pruebas negativas de acceso con los roles restantes y con ADMIN de TESTB. Registra lo observado en `defectos.md`, sin incluir correos, OTP, enlaces personales, tokens, secretos ni datos de votantes en capturas.

El recorrido detallado de censos, estados, votos, recibos y escrutinio está en [Probar las APIs en local](../../architecture/api-testing.md). Los enlaces personales y OTP son secretos temporales: no los compartas y deja que caduquen al terminar.

## 6. Limpieza y registro

- Revoca la clave `INTEGRATION` de TESTA desde la consola y borra los mensajes de prueba de Mailpit.
- La aplicación no ofrece una operación para borrar la organización de prueba. Puedes dejarla en el entorno local para continuar las pruebas; no intentes limpiar la base a mano.
- No borres los volúmenes para hacer limpieza: `docker compose down -v` elimina todas las bases locales.
- Anota fecha, rol, pantalla o endpoint, esperado, observado y resultado. No registres credenciales ni datos de voto.

## Estado actual

Este manual proporciona las identidades sintéticas y el procedimiento de alta, pero **no se han creado aún las cuentas**: en la ejecución P7 Keycloak estaba activo, pero los servicios VeriVoto no estaban levantados. Las cuentas existirán al completar el apartado 3 en el entorno local.
