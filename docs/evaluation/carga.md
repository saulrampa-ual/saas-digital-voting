# Preparación y registro de pruebas de carga

Estado: **parcial; no ejecutado**. `tests/load/voting.js` mide solo la emisión de votos con 500 VU y un token
único por iteración. El escenario de acceso por código y el de escrutinio se deben preparar y medir aparte antes de
cerrar P7.5. No se han iniciado servicios ni se han generado resultados de rendimiento.

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
| Fecha, sistema operativo, CPU y RAM | PENDIENTE |
| Versiones de k6, Java, PostgreSQL y Docker | PENDIENTE |
| Límites de CPU/RAM y configuración del stack | PENDIENTE |
| Número de VU, iteraciones y duración | 500 VU × 1; ejecución pendiente |
| Ejecuciones 1, 2 y 3: p50/p90/p95/p99 y errores | PENDIENTE |
| CPU/RAM pico por contenedor (`docker stats`) | PENDIENTE |
| Cadena íntegra tras la carga | PENDIENTE |
| Votos en urna = personas marcadas como votantes | PENDIENTE |
| Escenarios independientes de acceso y escrutinio | PENDIENTE |

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
