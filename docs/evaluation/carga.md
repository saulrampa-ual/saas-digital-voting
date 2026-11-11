# Preparación y registro de pruebas de carga

Estado: **H1c ejecutada el 11/11/2026: RECHAZADA** (véase «Resultados H1c»). `tests/load/voting.js` mide solo la
emisión de votos con 500 VU y un token único por iteración. Los escenarios de acceso por código y de escrutinio no se
han medido.

## Preparación sintética

1. Levantar el entorno local de evaluación conforme a `docs/architecture/local-infrastructure.md`, usando únicamente
   cuentas, organizaciones y un censo de prueba. No usar direcciones ni identidades reales.
2. Crear y abrir un proceso con al menos 500 electores sintéticos y una opción válida. Acordar una instancia aislada;
   no enviar la carga a producción ni a entornos compartidos.
3. Completar la fase de autenticación antes de medir: emitir al menos 500 tokens de voto distintos para ese proceso
   (pasos 2 y 3 automatizados por `scripts/load/prepare-voting-tokens.mjs`, ver «Preparación con el script»).
   Guardarlos en un JSON local **fuera del repositorio**, como un array de strings. El archivo contiene credenciales
   de prueba y se elimina al finalizar. No incluir tokens, enlaces, códigos ni recibos en logs o informes.
4. Confirmar que todos los tokens corresponden al mismo proceso abierto, que son distintos y que las opciones están
   en la papeleta. La comprobación de unicidad también se ejecuta al iniciar el script.

## Ejecución

Desde PowerShell, con k6 instalado por el autor, configurar las variables localmente (no guardarlas en el repo):

```powershell
$env:BASE_URL = "http://localhost:8000"
$env:PROCESS_ID = "<id-del-proceso-sintetico>"
$env:OPTION_ID = "<id-de-opcion-valida>"
$env:TOKENS_FILE = "<ruta-local-fuera-del-repo>\tokens.json"
$env:VUS = "500"
$env:ITERATIONS = "1"
k6 run tests/load/voting.js
```

Repetir tres veces con procesos y tokens sintéticos nuevos, equivalentes y con configuración idéntica; dejar que
k6 devuelva su resumen final. El criterio fijado es p95 de emisión < 2 s; conservar el p50 (mediana), p90, p95, p99,
total de peticiones, errores HTTP y comprobaciones fallidas. Un fallo del umbral se informa sin cambiar el criterio.
Cada token se usa una vez para evitar medir el rechazo de doble voto.

## Datos que debe registrar el autor

| Campo | Valor |
| --- | --- |
| Fecha, sistema operativo, CPU y RAM | 11/11/2026, Windows; AMD Ryzen 7 5825U with Radeon Graphics, 16 GB RAM (15,4 GiB visibles para Windows; Docker Desktop dispone de 7,4 GiB) |
| Versiones de k6, Java, PostgreSQL y Docker | k6 v2.3.0; Temurin 25.0.5; PostgreSQL 18.6 (Compose); Docker 29.4.0 (build 9d7ad9f) |
| Límites de CPU/RAM y configuración del stack | Sin límites. Infraestructura en Compose (Keycloak, Mailpit, `pg-main`, `pg-ballot`); los cinco servicios y el *gateway* como procesos Java en Windows (`.local/arrancar.ps1`), en marcha desde el 03/11 |
| Número de VU, iteraciones y duración | 500 VU × 1, sin rampa; la iteración más larga duró 5,3 s, 2,6 s y 5,5 s |
| Ejecuciones 1, 2 y 3: p50/p90/p95/p99 y errores | Véase «Resultados H1c» |
| CPU/RAM pico por contenedor (`docker stats`) | Ejecución 1: `pg-ballot` 56,47 %, `pg-main` 6,29 %; ejecución 3: `pg-ballot` 53,33 %, `pg-main` 41,23 %; ejecución 2 sin muestra dentro de los 2,6 s de k6. Los servicios Java no están en contenedores: sin medición |
| Cadena íntegra tras la carga | Sí en las tres (`check-load-run.mjs`) |
| Votos en urna = personas marcadas como votantes | Sí: 222, 240 y 281 (= respuestas 201) |
| Escenarios independientes de acceso y escrutinio | No medidos |

La medición cubre el tiempo HTTP de la petición de emisión a través del gateway; no equivale a una prueba de
capacidad de producción ni mide la experiencia completa del votante.

## Preparación con el script (05/11/2026)

`scripts/load/prepare-voting-tokens.mjs` hace los pasos 2 y 3 para una ejecución: organización `LOAD`, censo y proceso
«Carga H1c · ejecución N» con 500 entradas sintéticas (`load-N-0001@verivoto.test`…), espera a que se abra y obtiene,
como la PWA, el enlace, el código y el token de voto de cada entrada. Guarda los tokens en
`.local/load/tokens-run-N.json` (fuera del repositorio) y escribe las variables de k6. **Sin probar todavía contra el
stack real.**

```powershell
$s = Read-Host 'DEMO_PASSWORD' -AsSecureString
$env:DEMO_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
$env:LOAD_RUN = "1"
node scripts/load/prepare-voting-tokens.mjs
# copiar las líneas $env:… que imprime y lanzar k6 enseguida (los tokens duran 15 minutos)
```

Restricciones:

- **Después de las sesiones H1b.** Mailpit conserva 500 mensajes y borra los más antiguos; 500 invitaciones y 500
  códigos expulsarían las invitaciones de `USAB`. El script solo borra los mensajes que ha leído, nunca el buzón entero.
- Una ejecución por proceso: `LOAD_RUN` 1, 2 y 3 crean procesos nuevos (un token solo vota una vez).
- Mientras el límite por IP de IMP-21 no esté en `dev`, no afecta. Si se fusiona, ejecutar el *gateway* con
  `GATEWAY_VOTER_ACCESS_RATE_LIMIT_ENABLED=false` y decirlo en los resultados.
- Durante cada ejecución, guardar `docker stats --no-stream` cada pocos segundos y el resumen
  `.local/load/k6-run-N.json`; al terminar, comprobar la cadena y que votos en urna = votantes marcados.

## Variables de k6 y ejecución válida (definición para H1c, 05/11/2026)

### Variables que espera `tests/load/voting.js`

| Variable | Valor | Obligatoria |
| --- | --- | --- |
| `BASE_URL` | URL del *gateway*: `http://localhost:8000` | Sí |
| `PROCESS_ID` | Id del proceso sintético abierto de esa ejecución (lo imprime `prepare-voting-tokens.mjs`) | Sí |
| `OPTION_ID` | Id de una opción válida de su papeleta (lo imprime el script) | Sí |
| `TOKENS_FILE` | Ruta absoluta de `.local\load\tokens-run-N.json` (array JSON de tokens distintos) | Sí |
| `VUS` | `500` | No (por defecto 500) |
| `ITERATIONS` | `1` | No (por defecto 1) |

Forma de la carga (fijada en el script, igual en las tres ejecuciones): ejecutor `per-vu-iterations`, 500 usuarios
virtuales que empiezan **a la vez** (sin rampa de subida, porque RNF-17 habla de 500 votantes simultáneos), un voto cada
uno (500 peticiones `POST /api/v1/ballot-box/processes/{id}/votes`), duración máxima 2 minutos, umbral
`p(95) < 2000 ms` sobre esas peticiones y comprobación de respuesta `201`. Guardar el resumen con
`k6 run --summary-export .local/load/k6-run-N.json tests/load/voting.js` (si la versión instalada de k6 no admite
`--summary-export`, usar `--out json=.local/load/k6-run-N.ndjson`) y anotar la versión de k6.

### Cuándo una ejecución es válida

Antes de lanzar k6:

1. Mismo equipo, misma configuración del stack y mismas variables en las tres ejecuciones; equipo enchufado y sin
   suspensión; sin compilaciones, IDE pesado ni otras pruebas en marcha.
2. Los cinco servicios y el *gateway* responden `UP` en `/actuator/health`, y Keycloak, PostgreSQL y Mailpit están en
   marcha.
3. Proceso de la ejecución `OPEN`, con un `LOAD_RUN` nuevo (nunca se repite un proceso ni un fichero de tokens).
4. `prepare-voting-tokens.mjs` terminado hace **menos de 10 minutos** (los tokens caducan a los 15) y con 500 tokens
   distintos.
5. Si el límite por IP de IMP-21 está en `dev`, *gateway* arrancado con `GATEWAY_VOTER_ACCESS_RATE_LIMIT_ENABLED=false`.
6. `docker stats --no-stream` cada 2–5 s durante la ejecución, guardado en `.local/load/docker-stats-run-N.txt`.

Durante y después:

7. k6 termina sin abortar y ejecuta las 500 iteraciones (ninguna «interrupted»).
8. Al terminar: número de votos en la urna del proceso = número de respuestas `201`, igual al de personas marcadas como
   votantes en la participación, y la cadena de huellas íntegra (auditoría del proceso).

**Se descarta y se repite** con un `LOAD_RUN` nuevo (no cuenta como una de las tres): si falla alguna condición de 1 a
7 por causas del entorno, o si aparecen respuestas `401`, `404`, `409`, `410` o `422`, porque indican una preparación
inválida (token caducado o usado, proceso no abierto) y no el rendimiento.

**No se descarta** (forma parte del resultado): respuestas `5xx`, tiempos de espera agotados y una caída de un servicio
provocada por la propia carga. Se informan como errores de esa ejecución.

### Qué se registra por ejecución

Fecha y hora, versión de k6, equipo (SO, CPU, RAM); p50, p90, p95, p99 y máximo de `http_req_duration` de la emisión;
número de `201` y de errores por código; tasa de `http_req_failed` y de `checks`; duración total; pico de CPU y RAM por
contenedor; resultado de la comprobación del punto 8.

### Decisión de H1c

- **Aceptada:** tres ejecuciones válidas, cada una con p95 < 2 s.
- **Rechazada:** alguna ejecución válida con p95 ≥ 2 s (se informa sin cambiar el criterio).
- **No concluyente:** menos de tres ejecuciones válidas.

Las tasas de error se informan siempre; RNF-17 no fija un umbral de errores, así que no deciden por sí solas.

## Comprobación tras cada ejecución (11/11/2026)

- `prepare-voting-tokens.mjs` crea también la cuenta sintética `auditor.load@verivoto.test` (AUDITOR) en la
  organización `LOAD` y guarda `.local/load/run-N.json` (solo el id del proceso, sin tokens).
- `tests/load/voting.js` añade submétricas `http_reqs{operation:cast_vote,status:N}` con umbral `count>=0` (nunca falla)
  para que el resumen exportado cuente las respuestas por código. No cambia la carga ni lo que se mide.
- Después de k6, con la misma sesión de PowerShell (`DEMO_PASSWORD`, `LOAD_RUN`):

```powershell
node scripts/load/check-load-run.mjs
```

  Lee `.local/load/k6-run-N.json`, verifica la cadena como AUDITOR (queda registrada como en la consola), cuenta los
  eventos `VOTE_RECEIVED`, lee la participación como ADMIN y comprueba los puntos 7 y 8: iteraciones completas, ninguna
  respuesta 401/404/409/410/422 y 201 = eventos de voto = personas marcadas, con la cadena íntegra. Escribe
  `.local/load/check-run-N.json`. Los puntos 1 a 6 los sigue comprobando el autor. Con `K6_ONLY=1` solo resume el
  fichero de k6, sin el stack.
- Formato de `--summary-export` comprobado con k6 v2.3.0 (11/11/2026) contra un servidor simulado; el script y las
  submétricas todavía **no se han ejecutado contra el stack real**. Primera prueba: la de humo con `LOAD_RUN=9` y
  `LOAD_VOTERS=20`, que no cuenta para H1c.

## Resultados H1c (11/11/2026)

Prueba de humo previa (no cuenta): ejecución 11, proceso 9, 20 VU: 20 × `201`, p95 757 ms, cadena íntegra, 20 de 20
marcados. Las ejecuciones 9 y 10 (procesos 7 y 8) quedaron abiertas sin votos por fallos del preparador, ya corregidos.

| Ejecución (proceso) | 201 | Sin respuesta (`0`) | 500 | p50 | p90 | p95 | p99 | máx. | p95 solo de los 201 | `http_req_failed` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 (10) | 222 | 273 | 5 | 0 ms | 4540 ms | **4852 ms** | 5130 ms | 5201 ms | 5055 ms | 55,6 % |
| 2 (11) | 240 | 260 | 0 | 0 ms | 2227 ms | **2402 ms** | 2523 ms | 2564 ms | 2477 ms | 52,0 % |
| 3 (12) | 281 | 219 | 0 | 1421 ms | 4562 ms | **4849 ms** | 5331 ms | 5438 ms | 5228 ms | 43,8 % |

Fuente: `.local/load/k6-run-N.json` y `check-run-N.json` (fuera del repositorio; sin tokens). Las 500 iteraciones
terminaron en las tres; ninguna respuesta 401/404/409/410/422; cadena íntegra y 201 = eventos de voto = personas
marcadas. El p50 de 0 ms de las ejecuciones 1 y 2 se debe a que más de la mitad de las peticiones fueron conexiones
rechazadas, que terminan casi al instante; por eso se da también el p95 de los votos aceptados.

**Decisión: H1c RECHAZADA (H0c).** Las tres ejecuciones son válidas para la latencia y superan p95 < 2 s. En la
ejecución 2 la captura de `docker stats` (cada ~5 s) no tuvo ninguna muestra dentro de los 2,6 s que duró k6, así que
no cumple estrictamente la condición 6; la decisión no depende de ella, porque las ejecuciones 1 y 3 la cumplen y ya
superan el umbral.

Observaciones (no forman parte del criterio):

- Los `0` son conexiones rechazadas por `localhost:8000` (el *gateway*). Los 5 errores 500 de la ejecución 1 son del
  *gateway* al no poder conectar con ballot-box (`Connection refused` a `127.0.0.1:8084`, log del *gateway*).
  Compatible con colas de aceptación de conexiones desbordadas al llegar 500 conexiones a la vez, sin rampa.
- `VotingService.store` bloquea la fila del proceso (`PESSIMISTIC_WRITE`) para encadenar cada evento: los votos de un
  mismo proceso se guardan de uno en uno, después de una llamada síncrona a voter-access para consumir el token. Es una
  explicación compatible con medianas de 1,8–3,7 s en los votos aceptados, pero no se ha perfilado (IMP-57).
- Versión medida: los servicios llevaban en marcha desde el 03/11 con jars de `.local/run/` compilados el 31/10
  (*gateway*, identity, election) y el 03/11 (ballot-box, voter-access). El código de producción de *gateway*,
  ballot-box y voter-access no cambia entre esas fechas y `dev` (`879cb42`); identity y election, sí (importación
  del censo), pero no intervienen en la emisión del voto.
- `pg-ballot` llegó a 53–56 % de CPU en el pico muestreado; la base de datos de la urna no parece el límite, aunque el
  muestreo cada ~5 s puede perder el pico real.
