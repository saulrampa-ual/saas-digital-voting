# RNF-4 · Protocolo y guion para repetir la sesión con administradores (modalidad online)

Preparado el 08/11/2026 y adaptado el 11/11/2026 a sesiones **online a distancia**: las personas participantes no
pueden venir en persona y se conectan desde su casa, por un túnel HTTPS, al stack local del autor en Windows.
**No se ha ejecutado.** Corrige las dos causas de la decisión anterior (ver `resultados-rnf4-administradores.md`): la
muestra no era la población del criterio (solo 2 de 5 sin formación previa) y los tiempos se estimaron después en
lugar de cronometrarse.

Ficheros de la sesión:

| Fichero | Uso |
| --- | --- |
| `rnf4-participantes-online.md` | Texto para enviar a cada participante (parte A antes, parte B al empezar la llamada) |
| `hoja-registro-rnf4.csv` | Una fila por sesión |
| `scripts/demo/seed-rnf4.mjs` | Escenario sintético, cuentas por participante, activar/desactivar el acceso remoto |
| `infra/compose.rnf4-remote.yaml` | Override de Keycloak para funcionar detrás del túnel |
| `.local/sesion-rnf4.md` | Credenciales generadas (git-ignorado; se borra al terminar) |

## Criterio (no cambia)

Al menos **4 de 5** administradores de organización **sin formación previa** en VeriVoto crean, configuran y programan
un proceso de opción única en **10 minutos o menos** (600 s) y **sin ayuda**. «Sin ayuda» (D-55): sin intervención de
la persona moderadora ni de otras personas; consultar la documentación de la herramienta sí está permitido. Con n = 5
no hay inferencia estadística: el resultado es evidencia descriptiva.

| Resultado con 5 sesiones válidas | Decisión RNF-4 |
| --- | --- |
| 4 o 5 con éxito (completa, sin ayuda y ≤ 600 s) | Aceptado |
| 3 o menos con éxito | Rechazado |
| Menos de 5 sesiones válidas | Sigue sin demostrar |

La modalidad online no cambia el criterio. Cambia cómo se observa (videollamada con pantalla compartida) y añade un
factor de entorno (red del participante y túnel) que se mide y se declara (ver «Latencia del túnel» y «Límites»).

## Participantes

- Cinco personas adultas que **nunca hayan usado VeriVoto** (ni la consola ni una demostración guiada) y hasta dos de
  reserva. Pueden tener experiencia general con aplicaciones web.
- Comprobación al empezar la llamada, con una sola pregunta: «¿Has usado antes VeriVoto o has visto una demostración
  de cómo se crea un proceso?». Si la respuesta es sí, **no participa** y se pasa a una reserva.
- Consentimiento (incluida la grabación) aceptado **por escrito** antes de la llamada, respondiendo al mensaje de la
  parte A de `rnf4-participantes-online.md`, y confirmado de viva voz al empezar la grabación. Las respuestas se
  guardan fuera del repositorio.
- Identificadores anónimos P01–P05, R1 y R2. Cada código tiene su propia cuenta ADMIN
  (`admin.rnf4.p01@verivoto.test` … `admin.rnf4.r2@verivoto.test`). No anotar nombres, correos ni otro dato que
  identifique a la persona en la hoja.
- Equipo del participante: ordenador de escritorio o portátil (no móvil ni tableta), navegador actualizado (Chrome,
  Edge o Firefox), ventana de al menos 1280 px de ancho, zona horaria peninsular española (las fechas de la ficha se
  introducen en la hora local del navegador).

## Acceso remoto

### Qué necesita el navegador del participante

| Servicio local | Lo usa el navegador | Cómo llega |
| --- | --- | --- |
| Front Angular (4200) | Sí | Túnel → 4200 |
| Gateway / API (8000) | Sí (`/api/...`) | Ya pasa por el proxy del servidor de desarrollo (`proxy.conf.json`) |
| Keycloak (8080), realm `verivoto` | Sí (login, tokens, recursos del tema) | Nuevo: el mismo proxy reenvía `/realms/verivoto` y `/resources` |
| Keycloak `/admin` y realm `master` | **No** | No se reenvían: inaccesibles desde fuera |
| Mailpit (8025), PostgreSQL (5432/5433), servicios Java | **No** | No se exponen |

Solución: **un único dominio HTTPS** (el del túnel) apuntando solo al puerto 4200. El servidor de desarrollo de
Angular hace de proxy inverso para la API y para el realm `verivoto` de Keycloak. Así no hay CORS, ni cookies de
terceros, ni varios túneles que mantener, y solo se publica lo imprescindible.

### Herramienta: Cloudflare Tunnel

| | Cloudflare Tunnel | ngrok (gratuito) | localtunnel |
| --- | --- | --- | --- |
| Cuenta | *Quick tunnel*: no. Túnel con nombre: sí y dominio propio en Cloudflare | Sí (authtoken) | No |
| URL estable | *Quick tunnel*: cambia en cada arranque. Con nombre: fija | Fija (un *dev domain* asignado) | Subdominio pedido, no garantizado |
| Página intermedia para el visitante | No | **Sí**: aviso de ngrok en el navegador | **Sí**: pide una «contraseña» (IP pública del autor) |
| Límites relevantes | *Quick tunnel*: 200 peticiones simultáneas, sin garantía de disponibilidad | 1 GB/mes, 20 000 peticiones/mes | Servicio comunitario, caídas frecuentes |

Se elige **Cloudflare Tunnel**: no añade ninguna pantalla ajena antes de la consola (que contaminaría la primera
impresión y añadiría un paso que explicar), no tiene límite de tiempo y la conexión es saliente (no se abren puertos
en el router). ngrok tendría URL fija, pero su página de aviso y su cupo mensual lo descartan; localtunnel pide una
contraseña al visitante y es poco fiable.

- **Con dominio en Cloudflare** (recomendado si existe): túnel con nombre, URL fija durante todas las sesiones.
- **Sin dominio**: *quick tunnel* (`*.trycloudflare.com`). La URL cambia cada vez que se arranca `cloudflared`, así que
  se arranca **una vez** y se mantiene abierto durante el bloque de sesiones. Si se cae, ver «Si cambia la URL».

### Cambios en Keycloak y en el front

1. **Front** (ya aplicados en el repositorio, sin efecto en `localhost`):
   - `frontend/proxy.conf.json`: reenvía `/realms/verivoto` y `/resources` a `127.0.0.1:8080` y marca esas peticiones
     con `X-Forwarded-Proto: https`.
   - `frontend/src/app/core/config/app-config.ts`: en `localhost` usa `http://localhost:8080` como siempre; con
     cualquier otro host usa el mismo origen de la página.
2. **Keycloak, configuración del servidor**: `infra/compose.rnf4-remote.yaml` añade `KC_PROXY_HEADERS=xforwarded`
   para que Keycloak trate las peticiones del túnel como HTTPS (cookies `Secure`). No se toca `infra/compose.yaml`.
3. **Keycloak, realm** (`node scripts/demo/seed-rnf4.mjs remote-on <URL>`, por la API de administración local):
   - *Frontend URL* del realm `verivoto` = `<URL>`: el emisor (`iss`) de los tokens y las URL del login pasan a ser las
     del túnel. El realm `master` y la consola `/admin` siguen solo en `http://localhost:8080`.
   - Cliente `verivoto-admin`: añade `<URL>/*` a *redirect URIs* y *post logout redirect URIs* y `<URL>` a
     *web origins*. Las entradas de `localhost:4200` se conservan.
   - `remote-off` lo deshace todo.
4. **Servicios Java**: como el emisor cambia, los cuatro servicios deben aceptar `<URL>/realms/verivoto` como
   emisor. Las claves se siguen leyendo en local (sin depender del túnel al arrancar). Variables de entorno, en la misma
   consola de PowerShell desde la que se lanzan los servicios:

   ```powershell
   $PUBLIC = "https://<host-del-tunel>"
   $env:IDENTITY_OIDC_ISSUER_URI     = "$PUBLIC/realms/verivoto"
   $env:ELECTION_OIDC_ISSUER_URI     = "$PUBLIC/realms/verivoto"
   $env:VOTER_ACCESS_OIDC_ISSUER_URI = "$PUBLIC/realms/verivoto"
   $env:BALLOT_BOX_OIDC_ISSUER_URI   = "$PUBLIC/realms/verivoto"
   $env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI = "http://localhost:8080/realms/verivoto/protocol/openid-connect/certs"
   ```

   Pendiente de comprobar en el ensayo: que `.local/arrancar.ps1` hereda estas variables y que las llamadas entre
   servicios con *client credentials* siguen funcionando con el nuevo emisor.

### Puesta en marcha (orden exacto)

Atajo: `.local\rnf4-remoto.ps1` (local, git-ignorado) hace los pasos 3 a 5 en un solo comando y los deshace con
`-Off`. Para los servicios 8081–8084 y el front, fija o quita las variables del emisor, relanza `arrancar.ps1`,
recrea Keycloak con o sin el override y ejecuta `remote-on` / `remote-off`:

```powershell
powershell -ExecutionPolicy Bypass -File .local\rnf4-remoto.ps1 -Url https://<host-del-tunel>
powershell -ExecutionPolicy Bypass -File .local\rnf4-remoto.ps1 -Off
```

Ojo: `arrancar.ps1` recrea Keycloak **sin** el override; si se relanza a mano durante las sesiones, hay que volver a
ejecutar `rnf4-remoto.ps1 -Url ...`.

Paso a paso, con el stack local arrancado y probado en `localhost`:

```powershell
# 0. Una sola vez: instalar cloudflared
winget install --id Cloudflare.cloudflared

# 1. Escenario y cuentas, ANTES de activar el modo remoto (sus logins van por localhost)
$env:DEMO_PASSWORD = "<12+ caracteres>"
node scripts/demo/seed-rnf4.mjs

# 2. Túnel (dejar esta consola abierta todo el bloque de sesiones). Sin dominio:
cloudflared tunnel --url http://localhost:4200 --http-host-header localhost:4200
#    copiar la URL https://<palabras>.trycloudflare.com que imprime
#    Con dominio en Cloudflare: ver «Túnel con nombre» más abajo

# 3. Keycloak detrás del proxy (recrea solo el contenedor; los datos siguen en pg-main)
docker compose -p verivoto-p7-phase2 -f infra/compose.yaml -f infra/compose.rnf4-remote.yaml --env-file infra/.env up -d keycloak

# 4. Realm y cliente
node scripts/demo/seed-rnf4.mjs remote-on https://<host-del-tunel>

# 5. Reiniciar los cuatro servicios con las variables del apartado anterior; front con: cd frontend; npm start
```

Túnel con nombre (solo si hay un dominio en Cloudflare). Una vez:

```powershell
cloudflared tunnel login
cloudflared tunnel create verivoto-rnf4
cloudflared tunnel route dns verivoto-rnf4 rnf4.<tu-dominio>
```

Y un fichero **fuera del repositorio y fuera de `~\.cloudflared`** (si existe `~\.cloudflared\config.yml`, los
*quick tunnels* dejan de funcionar), por ejemplo `C:\Users\<usuario>\verivoto-tunel\rnf4.yml`:

```yaml
tunnel: verivoto-rnf4
credentials-file: C:\Users\<usuario>\.cloudflared\<id-del-tunel>.json
ingress:
  - hostname: rnf4.<tu-dominio>
    service: http://localhost:4200
    originRequest:
      httpHostHeader: localhost:4200
  - service: http_status:404
```

En cada bloque: `cloudflared tunnel --config C:\Users\<usuario>\verivoto-tunel\rnf4.yml run verivoto-rnf4`.

`--http-host-header localhost:4200` (o `httpHostHeader`) hace que el servidor de desarrollo de Angular reciba `Host: localhost:4200` y no
rechace el host del túnel. Si aun así responde «host not allowed», arrancar el front con
`npx ng serve --allowed-hosts <host-del-tunel>`.

Comprobaciones antes de cada bloque (desde un móvil con datos, fuera de la red de casa):

- `https://<host>/admin` abre la consola y el login de Keycloak con el tema de VeriVoto, en el mismo dominio.
- `https://<host>/admin/master/console/` **no** abre la consola de Keycloak (la ruta la sirve Angular).
- `https://<host>/realms/master/` **no** responde con el realm `master`.
- Ensayo completo con R2 o una cuenta propia: login, crear, programar y ver «Programado».

### Si cambia la URL

Solo con *quick tunnel*, si `cloudflared` se reinicia: (1) `remote-on` con la URL nueva (sustituye a la anterior),
(2) reiniciar los cuatro servicios con `$PUBLIC` nuevo, (3) reenviar el enlace al participante. La sesión en curso se
marca con incidencia técnica (ver «Incidencias»).

### Al terminar el bloque de sesiones

```powershell
node scripts/demo/seed-rnf4.mjs cleanup        # deshabilita las 7 cuentas y ejecuta remote-off
# Ctrl+C en la consola de cloudflared (el quick tunnel y su URL desaparecen)
docker compose -p verivoto-p7-phase2 -f infra/compose.yaml --env-file infra/.env up -d keycloak   # Keycloak sin el override
# reiniciar los servicios sin las variables *_OIDC_ISSUER_URI ni ..._JWK_SET_URI
Remove-Item .local\sesion-rnf4.md              # cuando la hoja de registro esté completa
```

### Latencia del túnel

Antes de cada sesión, desde el equipo del autor, 20 peticiones por el túnel y 20 en local al mismo recurso ligero:

```powershell
$PUBLIC = "https://<host-del-tunel>"
$t = 1..20 | % { (Measure-Command { Invoke-WebRequest -UseBasicParsing "$PUBLIC/realms/verivoto/.well-known/openid-configuration" | Out-Null }).TotalMilliseconds }
$l = 1..20 | % { (Measure-Command { Invoke-WebRequest -UseBasicParsing "http://localhost:8080/realms/verivoto/.well-known/openid-configuration" | Out-Null }).TotalMilliseconds }
"tunel mediana ms: {0} · local mediana ms: {1}" -f ($t | Sort-Object)[10], ($l | Sort-Object)[10]
```

La diferencia de medianas es el coste por petición del túnel visto desde aquí; el del participante es parecido o algo
mayor según su conexión. Se anota en `observacion` (`latencia_tunel_ms=<valor>`). Interpretación: la tarea son unas
decenas de peticiones esperadas por la persona; con, por ejemplo, 150 ms extra por petición y 40 peticiones, el
túnel añade del orden de 6 s, el 1 % de los 600 s. Si alguna sesión supera los 600 s por menos de lo que explica la
latencia medida, se señala en `observacion` y en la memoria (no se corrige el tiempo). Si la persona comenta lentitud
o el vídeo muestra esperas visibles (> 2 s) de la interfaz, se anota como incidencia técnica.

## Preparación (una vez, antes de la primera sesión)

1. Stack local con datos sintéticos y el acceso remoto en marcha (apartado anterior). **No reutilizar el escenario
   `USAB` de H1b.**
2. `seed-rnf4.mjs` crea la organización `RNF4`, una cuenta ADMIN por participante con **contraseña propia, no
   temporal** (no aparece el cambio forzado de contraseña) y **de un solo uso** (la cuenta se habilita al empezar su
   llamada y se deshabilita al terminar), y el censo «Censo de prueba RNF-4» con una persona sintética (programar exige
   censo con personas, textos en español e inglés y apertura posterior al momento de programar).
3. Rellenar los huecos `[ENLACE]`, `[FECHA_TAREA]`, `[ENLACE_DOCUMENTACIÓN]`, `[FECHA_BORRADO]` de `rnf4-participantes-online.md`. Las fechas de la ficha son las mismas para todos y
   posteriores a la última sesión.
4. Grabación en el equipo del autor (OBS u otro grabador local), capturando la ventana de la videollamada y el reloj
   del sistema visible. El reloj del equipo del autor es también el del servidor: vídeo y registros de auditoría
   comparten hora.
5. Ensayo completo con la cuenta R2 (o una propia) en remoto, desde otra red. Si R2 se usa en el ensayo, se vuelve a
   ejecutar el seed para regenerar su contraseña y no queda como reserva limpia: se anota.
6. Si un participante anterior dejó un proceso, se deja como está: cada participante crea uno nuevo con su propio
   título (ver la ficha).

## Ficha de la tarea

Se pega en el chat de la videollamada en el paso 4 del guion (parte B de `rnf4-participantes-online.md`):

> Crea un proceso de votación de **opción única** con estos datos y déjalo **programado**. Avísame cuando termines.
>
> - Título: «Elección de prueba Pxx» (Pxx es tu código) · en inglés: «Test election Pxx».
> - Apertura: **[FECHA_TAREA] a las 10:00** · Cierre: **[FECHA_TAREA] a las 18:00** (hora peninsular).
> - Censo: «Censo de prueba RNF-4».
> - Pregunta: «¿Qué día prefieres para la reunión?» · en inglés: «Which day do you prefer for the meeting?».
> - Opciones: «Martes» / «Tuesday» y «Jueves» / «Thursday».
> - Voto en blanco: permitido.

Los datos son neutros y sintéticos. No se pide ninguna preferencia personal.

## Guion de la persona moderadora (videollamada)

**Antes de la llamada** (5 min): `node scripts/demo/seed-rnf4.mjs enable Pxx`; medir la latencia; abrir Mailpit en
local filtrado por `admin.rnf4.pxx@verivoto.test`; tener la hoja de registro abierta y un cronómetro con vueltas.

1. **Conexión y grabación.** Pedir que comparta la **pantalla completa** (no solo una pestaña), con notificaciones
   silenciadas y otras aplicaciones cerradas. Empezar a grabar. Leer: «La sesión se está grabando. ¿Confirmas que
   aceptas la grabación tal como se explicaba en el mensaje?». Pregunta de formación previa (apartado
   «Participantes»).
2. **Login (fuera del tiempo).** Pegar en el chat el enlace y las credenciales (parte B, primer bloque). La persona
   entra; cuando Keycloak pida el código, la persona moderadora lo lee en Mailpit y lo dicta o lo pega en el chat. Se
   espera a que la consola muestre «Inicio». Anotar navegador y sistema operativo (preguntarlo o verlo en pantalla).
3. **Instrucciones.** Leer literalmente: «Vas a usar la consola de una herramienta de votaciones. Te voy a pegar en el
   chat una ficha con lo que tienes que hacer. Puedes consultar la documentación de la aplicación en el enlace que te
   paso; no puedes buscar en Internet ni pedir ayuda a nadie. Yo solo observo y tomo notas. Si me pides ayuda, paro el
   tiempo y la tarea cuenta como no completada. Avísame cuando la tengas lista».
4. **Ficha.** Pegar la ficha y el enlace a la documentación en el chat. **T0** = primer clic o pulsación en la consola
   tras terminar de leer la ficha (visible en la pantalla compartida). Iniciar el cronómetro y decir en voz alta la
   hora del reloj del sistema («T0, 10:14:05»): queda en la grabación.
5. **Observación.** Cámara y micrófono de la persona moderadora apagados o en silencio, sin comentarios. Anotar con la
   función de vuelta del cronómetro los momentos de error y las consultas de documentación.
   - **Consulta de documentación**: se ve en la pantalla completa (cambio a la pestaña o al fichero del
     `[ENLACE_DOCUMENTACIÓN]`). Permitida; `consulto_documentacion = si`.
   - **Búsqueda externa** (buscador, otra web, otra aplicación de chat o IA): se anota con su minuto en
     `observacion` y la sesión cuenta como `sin_ayuda = no`.
   - **Petición de ayuda**: responder «¿Quieres que te ayude? Si te ayudo, paro el tiempo y la tarea cuenta como no
     completada». Si confirma: parar el cronómetro, anotar `t1` y el tiempo en ese momento, `sin_ayuda = no`,
     `completo = no`, y en `ayuda_ofrecida` el momento y el contenido en términos generales. Tras ayudar, puede
     terminar para observar el resto, pero no cambia el registro.
6. **T1** = el proceso aparece con el estado «Programado» tras confirmar el aviso de programar. Parar el cronómetro y
   decir la hora en voz alta. Si a los 15 minutos (900 s) no ha terminado, se detiene la tarea (`completo = no`,
   `t0_t1_segundos = 900`).
7. **Comprobación.** En la consola local (cuenta propia o la del participante): opción única, censo, textos en los dos
   idiomas, fechas de la ficha y voto en blanco. Si algún dato no coincide, `completo = no` y se describe en
   `observacion` sin datos personales.
8. **Cierre.** Agradecer; si lo pide, explicar lo que le bloqueó. Parar la grabación.
   `node scripts/demo/seed-rnf4.mjs disable Pxx`. Rellenar la fila **ahora**, no al final del día.

### Fuentes de tiempo

| Momento | Fuente principal | Fuente de contraste |
| --- | --- | --- |
| T0 | Cronómetro + hora dicha en la grabación | Vídeo: primer clic después de leer la ficha |
| T1 | Cronómetro + hora dicha en la grabación | Registro de auditoría / fecha de programación del proceso en la app (misma hora del sistema) |

`t0_t1_segundos` se calcula con el cronómetro. Después de la sesión se revisa con el vídeo y con la marca de tiempo
de la programación en la app; si difieren más de 5 s, prevalece el vídeo y se anota la diferencia en `observacion`.
Antes del ensayo, comprobar qué eventos registra la auditoría de la consola (creación y programación del proceso, con
hora); si no registra la programación, el contraste de T1 se hace con el estado y las fechas del proceso.

### Compartir pantalla y grabar desde el lado del autor, frente a OBS del participante

Se graba desde el equipo del autor. Es lo más limpio: el participante no instala nada ni maneja ficheros (menos carga
y menos ocasiones de fallo antes de la tarea), la grabación no sale de su equipo con datos de su escritorio, y el
reloj de la grabación es el mismo que el del servidor, lo que permite cruzar vídeo y registros sin corregir desfases.
Lo que se pierde es calidad de imagen y fluidez (la videollamada comprime y puede congelarse); las caídas se registran
como incidencias. OBS en el equipo del participante daría mejor vídeo, pero introduce un paso técnico, un reloj distinto
y el envío de un fichero grande con su escritorio.

## Qué cuenta como error, ayuda o incidencia

- **Error** (`errores`): acción que obliga a volver atrás, un aviso de validación o un dato introducido mal y luego
  corregido. Se cuentan, no invalidan la sesión.
- **Consulta de documentación** (`consulto_documentacion`): permitida; solo se anota `si`/`no`.
- **Ayuda** (`sin_ayuda = no`): cualquier indicación de una persona sobre cómo usar la aplicación, o una búsqueda
  externa. Con ayuda de la persona moderadora la tarea cuenta además como no completada.
- **Incidencia técnica** (`incidencias_tecnicas`, texto breve con minuto y duración): caída de la videollamada, del
  túnel, del stack o de la red del participante; congelación de la pantalla compartida.
  - Si es ajena a la persona y dura **menos de 30 s** sin que tenga que repetir nada, se sigue y se anota; el tiempo
    no se corrige.
  - Si dura más, obliga a recargar, a repetir el login o cambia la URL: se para la tarea,
    `incidencia_entorno = si`, `valida = no`, y la sesión se sustituye por una reserva.

## Registro

`hoja-registro-rnf4.csv`, una fila por sesión. Valores `si`/`no`; `t0_t1_segundos` en segundos enteros medidos con
cronómetro (no estimados después); `t0` y `t1` como hora `HH:MM:SS` del equipo del autor; `fecha` como `DD/MM/AAAA`;
`modalidad = online`. `navegador_so`, por ejemplo `Chrome 142 / Windows 11`. `referencia_grabacion` es el nombre del
fichero de vídeo guardado fuera del repositorio (por ejemplo `rnf4-P01.mkv`), nunca una ruta con datos personales.
`con ayuda` corresponde a `sin_ayuda = no` (no se duplica la columna).

Éxito de una sesión = `valida = si`, `sin_formacion_previa = si`, `completo = si`, `sin_ayuda = si` y
`t0_t1_segundos <= 600`.

## Cómo pasarlo a la memoria

Con la hoja completa:

```powershell
node scripts/evaluation/rnf4-summary.mjs
```

El script comprueba que no falten datos y que todas las sesiones válidas sean sin formación previa, y escribe las
cantidades (x de 5), la mediana y el rango de tiempos y la decisión de la tabla de arriba. Después, la IA actualiza
`resultados-rnf4-administradores.md`, la tabla de hipótesis (`evaluacion_resultados.tex`), `contenido1.tex`,
`conclusiones.tex` y `especificacion_detallada.tex`. La sesión del 03/11/2026 se conserva como antecedente
(evidencia indicativa) y no se mezcla con la nueva.

## Límites

n = 5 y un entorno local con datos sintéticos: el resultado se limita al prototipo, la configuración y la población
evaluados. El login (con código por correo, dictado por la persona moderadora) queda fuera del tiempo; se declara así
en la memoria. La modalidad online añade la latencia del túnel y de la red del participante (medida y anotada) y
observa a través de una pantalla compartida, que puede perder detalle; ninguna de las dos se corrige en los tiempos.
