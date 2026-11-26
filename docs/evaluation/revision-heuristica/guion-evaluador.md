# Revisión heurística (M3) · guion paso a paso para cada persona evaluadora

Versión del 26/11/2026 (sustituye a la del 23/11). Complementa a `protocolo.md`, que dice qué se mide y cómo se
decide. **Sin ejecutar.** Las hojas del 26/11 se descartaron porque varias filas no coincidían con lo que muestra la
aplicación y las dos hojas tenían frases idénticas.

## 0. Reglas (léelas antes de empezar)

1. **Escribe solo lo que ves en la pantalla en ese momento.** Antes de anotar «no hay…» o «no se puede…», busca en la
   pantalla entera (desplázate hasta abajo) y prueba el botón. Si dudas, anótalo como duda en `descripcion`
   («No encuentro cómo…») con gravedad 1, no como un hecho.
2. **Cada persona revisa sola, con sus propias palabras.** E2 no ve la hoja de E1 hasta que las dos estén terminadas.
   No se copian frases ni propuestas de otra hoja, de un ejemplo de este guion ni de una herramienta.
3. **Si una pantalla está bien, no escribas nada.** Solo se anotan problemas. Si una prueba no se puede hacer, una fila
   con `descripcion = no revisado` y el motivo en `propuesta`.
4. **Un problema por fila.** Si el mismo problema aparece en varias pantallas, una fila con `pantalla = Varias pantallas`
   y la lista en `descripcion`.
5. **La gravedad es tu juicio**, con esta guía:

| Gravedad | Cuándo | Ejemplo (ficticio) |
| --- | --- | --- |
| 0 | No es un problema, solo una preferencia | Te gustaría otro color en un botón que se lee bien |
| 1 | Cosmético: molesta pero no frena | Una errata («Te quedan 1 intentos») |
| 2 | Menor: frena un poco o confunde, pero se sale adelante | Hay que desplazar la pantalla para encontrar el botón principal |
| 3 | Mayor: muchas personas se atascarían o se equivocarían | Un error no dice qué hacer y no hay forma de seguir |
| 4 | Catástrofe: impide terminar la tarea o hace perder el voto | Al confirmar, el voto se pierde sin aviso |

Las gravedades 3 y 4 deciden que RNF-10 no se cumple: antes de poner un 3 o un 4, repite la prueba para confirmarlo.

## 1. Preparación (la hace el autor, unos 15 min, antes de cada sesión)

1. Arrancar todo en PowerShell, desde la raíz del repositorio:
   ```powershell
   docker compose -f infra/compose.yaml --env-file infra/.env up -d
   .\scripts\dev\stop-services.ps1
   .\scripts\dev\start-services.ps1
   ```
   Y, en otra ventana, `cd frontend` y `npm start`.
2. Cargar **una demo nueva para cada evaluador** (unos 12 min; se crean las elecciones «… · E1» o «… · E2»):
   ```powershell
   $env:DEMO_PASSWORD = '<contraseña de la demo>'
   $env:DEMO_SUFFIX = 'E1'      # 'E2' en la sesión de la segunda persona
   node scripts/demo/seed-ual.mjs
   ```
   Debe terminar con `intact=true` y `DONE`.
3. Abrir `.local/demo-ual.md`: enlaces de cada votante de **esa** demo. Abrir Mailpit (`http://localhost:8025`): ahí
   llegan los códigos del votante y los de inicio de sesión de la consola.
4. Copiar el commit (`git rev-parse --short HEAD`) en la celda `commit` de la primera fila de la hoja y arrastrarlo.
5. En Chrome: una ventana normal para el votante y una **ventana de incógnito por cada cuenta de la consola**. Para ver
   el votante en móvil: DevTools (F12) → icono de dispositivo (Ctrl+Mayús+M) → «Dimensiones: 360 × 640».
6. Dar a la persona evaluadora este guion, `protocolo.md` y su hoja (`hoja-E1-autor.xlsx` o `hoja-E2.xlsx`).
   Durante la sesión el autor no explica la interfaz; solo ayuda con cuestiones técnicas (un servicio caído, un
   enlace que no abre).

Papeles de los votantes de la demo «· E1» o «· E2» (los mismos en las dos):

| Votante | Para qué | Estado al cargar la demo |
| --- | --- | --- |
| `presente.votante1` | Ya has votado (V8) | Ya votó |
| `presente.votante3` | Recorrido completo (V1–V7) | Libre: **vota** en V6 |
| `presente.votante4` | Bloqueo por intentos (V11) | Libre: queda bloqueado 15 min |
| `presente.votante5` | Sesión caducada (V9) y voto no registrado (V10) | Libre |
| `futura.votante1` | Votación no abierta (V12) | Proceso aún no abierto |
| `pasada.votante5` | Resultado (V13) | Proceso escrutado |

## 2. Cómo rellenar la hoja

Solo la pestaña **«Hallazgos»**, celdas amarillas. Los desplegables ponen el texto exacto; `pantalla` admite también
texto libre. La pestaña «Instrucciones» explica cada columna y cuenta los problemas de gravedad alta.

Ejemplo de **formato** (los textos de los ejemplos de este guion son ficticios: no los copies, escribe lo tuyo):

| evaluador | fecha | commit | pantalla | ancho_px | heuristica | descripcion | gravedad_0a4 | tiempo_realimentacion_s | rnf | propuesta |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| E1 | 27/11/2026 | 96617f3 | Votante · Papeleta | 360 | 4. Consistencia y estándares | El botón principal es verde aquí y azul en la pantalla anterior | 1 | | RNF-10 | Usar el mismo color |
| E1 | 27/11/2026 | 96617f3 | Votante · Votación cerrada | 1280 | | no revisado | | | | La demo no tiene un proceso cerrado sin escrutar |

Qué RNF elegir:

- **RNF-8** si el problema es un mensaje de error poco claro (no dice qué ha pasado o qué hacer) o una acción lenta sin
  ninguna señal de que algo pasa.
- **RNF-9** si en una pantalla del votante no se ve en qué paso está o el estado del proceso (abierto, hora de cierre).
- **RNF-10** si algo es inconsistente entre pantallas: nombres distintos para la misma acción, botones en sitios
  distintos, patrones de error o de confirmación distintos.
- **Otro** para cualquier otro problema de usabilidad.

## 3. Pruebas del votante

Hazlas **primero a 360 px** (móvil) y, al final, repite V1, V4 y V6 a **1280 px**. En **cada** pantalla del votante
mira también: ¿se ve en qué paso estás («Paso n de 4»)? ¿Se ve el estado del proceso (abierto, hora de cierre)? Si
falta, fila con RNF-9.

**Empieza por V9**: necesita 15 minutos de espera y se hace en paralelo con las demás.

### V9 · Sesión caducada (empezar al principio) — `presente.votante5`

1. Abre su enlace en una pestaña aparte. Pulsa «Empezar».
2. Copia el código de Mailpit (el correo más reciente para `presente.votante5`) y escríbelo.
3. En la papeleta **no toques nada** y apunta la hora. Sigue con V1 en otra pestaña.
4. Pasados **16 minutos**, vuelve, marca una opción, pulsa «Revisar mi voto» y después «Confirmar y votar».
5. Observa: ¿el mensaje dice qué ha pasado y si has votado o no? ¿Dice cómo seguir? Pulsa el botón que ofrezca.

Ejemplo de fila (si vieras un problema): `Votante · Sesión caducada | 360 | 9. Reconocer, diagnosticar y recuperarse de
errores | <lo que veas> | <0–4> | | RNF-8 | <tu propuesta>`.

### V10 · Voto no registrado (opcional, justo después de V9) — `presente.votante5`

1. Vuelve a entrar con el mismo enlace hasta la pantalla «Revisa tu voto». No pulses «Confirmar».
2. Avisa al autor, que para la urna en PowerShell:
   `Stop-Process -Id ((Get-Content .local\logs\pids.txt)[3])`
3. Pulsa «Confirmar y votar». Observa el mensaje y el botón que ofrece.
4. El autor reinicia los servicios (`stop-services.ps1` y `start-services.ps1`). Si no se hace, fila `no revisado`.

### V1 · Bienvenida — `presente.votante3`

1. Abre su enlace. No pulses nada todavía.
2. Observa: ¿se entiende qué es, hasta cuándo se puede votar y qué va a pasar? ¿Se ve el botón para empezar sin
   desplazar la pantalla? ¿Funcionan los botones ES / EN?

### V2 · Código de acceso — `presente.votante3`

1. Pulsa «Empezar». Observa: ¿se entiende dónde llega el código, cuántas cifras tiene y cuánto dura? Toca la primera
   casilla.
2. Mira el texto que hay bajo el botón «Continuar» y espera a que cambie.

### V3 · Código incorrecto — `presente.votante3`

1. Escribe `000000` **una sola vez** (más veces bloquearía al votante 3).
2. Observa el mensaje: ¿dice qué ha pasado y qué hacer? ¿Dice cuántos intentos quedan?

### V4 · Papeleta — `presente.votante3`

1. Escribe el código correcto de Mailpit (el más reciente).
2. Observa la pregunta y las opciones. Marca una opción y después otra: ¿se distingue cuál está marcada?
3. Pulsa «Revisar mi voto» **sin** marcar nada (recarga si ya marcaste) para ver el error, y después marca una.

### V5 · Revisión — `presente.votante3`

1. Pulsa «Revisar mi voto». Observa: ¿se ve qué has elegido y que aún no has votado?
2. Busca cómo cambiar la elección, úsalo y vuelve a la revisión.

### V6 · Recibo — `presente.votante3` (aquí se vota de verdad)

1. Pulsa «Confirmar y votar». Observa: ¿queda claro que has votado? ¿Se ve el código de recibo entero?
2. Usa el botón para copiar el código y pégalo en un bloc de notas (lo necesitas en V7).

### V7 · Comprobación del recibo — `presente.votante3`

1. Pulsa «Comprobar mi recibo». Si el código no aparece ya escrito, pégalo y pulsa «Comprobar».
2. Escribe después un código inventado de 64 caracteres y otro de 10 caracteres. Observa los mensajes.

### V8 · Ya has votado — `presente.votante1`

1. Abre su enlace, «Empezar», código de Mailpit. Observa el mensaje.

### V11 · Bloqueo por intentos — `presente.votante4`

1. Abre su enlace, «Empezar» y escribe `000000` **cinco veces**, mirando el mensaje cada vez.
2. Observa la pantalla de bloqueo: ¿dice por qué y hasta cuándo? Pulsa el botón que ofrezca.

### V12 · Votación no abierta — `futura.votante1`

1. Abre su enlace. Observa: ¿se entiende que aún no se puede votar y cuándo se podrá?

### V13 · Resultado — `pasada.votante5`

1. Abre su enlace y pulsa «Ver el resultado»; escribe el código de Mailpit.
2. Observa: ¿se entiende el resultado, la participación y qué significa «Resultado verificado»?

### V14 · Demasiadas peticiones (opcional)

Solo si el autor ha reiniciado los servicios con el límite bajo (`$env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_CAPACITY='3'`
y `$env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_PER_MINUTE='3'` antes de `start-services.ps1`): recarga cuatro veces el
enlace de `presente.votante4` y observa el mensaje. Si no, fila `no revisado`. Después hay que reiniciar los servicios
sin esas variables.

### V15 · Votación cerrada

La demo no tiene un proceso cerrado y sin escrutar: fila `no revisado`.

## 4. Pruebas de la consola (1280 px)

Cada cuenta en su ventana de incógnito. Entrar: `http://localhost:4200/admin`, usuario, contraseña de la demo y el
código de 6 cifras de Mailpit. En todas, para RNF-10, compara con las pantallas anteriores: ¿misma palabra para la
misma acción, botón principal en el mismo sitio, mismos mensajes de error y de confirmación?

### C1 · Inicio y «Primeros pasos» — `admin@ual.test`

1. Observa la pantalla de inicio y la guía «Primeros pasos». Sigue uno de sus enlaces y vuelve.

### C2 · Censos e importación de Excel — `censo@ual.test`

1. Entra en «Censos», crea un censo nuevo («Prueba E1») y ábrelo.
2. Descarga la plantilla de Excel, borra las filas de ejemplo, escribe tres personas inventadas con correo
   `@ual.test` y súbela. Observa la validación y confirma la importación.
3. Sube otra vez la plantilla con un error a propósito (un correo sin «@»). Observa el mensaje.
4. Opcional (RNF-8): cronometra la importación con DevTools → Performance (marcar «Screenshots») y anota en
   `tiempo_realimentacion_s` los segundos desde el clic hasta la primera señal (indicador, mensaje), no hasta el final.

### C3 · Alta y edición de una persona — `censo@ual.test`

1. En el censo «Prueba E1», añade una persona a mano, edítala y bórrala. Observa las confirmaciones.

### C4 · Asistente de proceso (4 pasos) — `admin@ual.test`

1. «Procesos» → nuevo proceso con el censo «Prueba E1». Recorre los 4 pasos.
2. En el paso de fechas pon un cierre anterior a la apertura y pulsa «Siguiente». Observa el mensaje.
3. Corrige y crea el proceso.

### C5 · Ciclo de vida — `admin@ual.test`

1. En el proceso creado, prográmalo. Observa la confirmación y el estado. Opcional: cronometra «Programar».

### C6 · Participación — `admin@ual.test`

1. «Participación», proceso «Elección en curso · E1». Observa los recuentos y la gráfica.

### C7 · Resultados y acta — `admin@ual.test`

1. «Resultados», proceso «Elección pasada · E1». Exporta el CSV y genera el acta en PDF; abre los dos ficheros.

### C8 · Claves de API — `admin@ual.test`

1. Crea una clave. Observa qué se avisa y cuándo. Revócala.

### C9 · Privacidad — `admin@ual.test`

1. Busca `presente.votante2@ual.test` y exporta sus datos. **No** borres a nadie.

### C10 · Auditoría y verificación de la urna — `auditor@ual.test`

1. «Auditoría», proceso «Elección pasada · E1». Pulsa «Verificar integridad ahora». Observa el resultado y la tabla.

### C11 · Trazabilidad — `auditor@ual.test`

1. «Trazabilidad»: filtra por un día y por una persona. Observa los resultados y la paginación.

## 5. Al terminar

1. Repasa la hoja: cada fila tiene pantalla, heurística, descripción y gravedad; las gravedades 3 y 4 están
   confirmadas repitiendo la prueba.
2. Guarda la hoja y dásela al autor. No la compartas con la otra persona evaluadora.

## 6. Juntar las hojas (autor, cuando estén las dos)

1. Pasarme las dos hojas. Las uno: los problemas que describen lo mismo se fusionan en una fila con la **mayor**
   gravedad, y compruebo en la aplicación los que sean de gravedad 3 o 4 antes de decidir.
2. Aplico la decisión de `protocolo.md`. Si queda alguna de gravedad 3 o 4: defecto en `resultados-p7/defectos.md` y
   corrección solo con tu aprobación; después se repiten esas pruebas.
3. Paso el resultado a la tabla 13.2, `informe.md` y `PLAN.md`.

## 7. Limitación que se declarará

E1 es el autor del sistema: conoce la interfaz y puede pasar por alto problemas. Por eso la decisión se toma con las
dos hojas juntas y no con una sola, y esto se anota en el capítulo 13. Si la segunda persona no puede hacer la
revisión, se declara M3 con un solo evaluador como limitación, sin presentarla como una revisión de dos.
