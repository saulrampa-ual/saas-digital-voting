# Revisión heurística (M3) · guion paso a paso para cada persona evaluadora

Versión del 27/11/2026 (sustituye a la del 26/11). Complementa a `protocolo.md`, que dice qué se mide y cómo se
decide. Cada prueba va numerada y en un bloque propio: dice qué enlace o cuenta usar, qué hacer y qué mirar.

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

## 1. Preparación (la hace el autor, unos 30 min, antes de cada sesión)

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
   $env:DEMO_SUFFIX = 'E2'      # 'E1' en la sesión del autor
   node scripts/demo/seed-ual.mjs
   ```
   Debe terminar con `intact=true` y `DONE`.
3. Abrir `.local/demo-ual.md` y comprobar que la fecha de «Generado» es la de hoy y los títulos acaban en «· E2».
4. Copiar el commit (`git rev-parse --short HEAD`) en la celda `commit` de la primera fila de la hoja y arrastrarlo.
   Si hay cambios sin commit en el frontend, escribir `<commit> + cambios sin commit`.
5. En Chrome: una ventana normal para el votante y una **ventana de incógnito por cada cuenta de la consola**. Para ver
   el votante en móvil: DevTools (F12) → icono de dispositivo (Ctrl+Mayús+M) → «Dimensiones: 360 × 640».
6. Dar a la persona evaluadora este guion, `protocolo.md`, su hoja (`hoja-E2.xlsx`) y `.local/demo-ual.md`.
   Durante la sesión el autor no explica la interfaz; solo ayuda con cuestiones técnicas (un servicio caído, un
   enlace que no abre) y hace los pasos de PowerShell de las pruebas 11, 16 y 29.

## 2. Dónde están los enlaces y los códigos

- **Enlaces de los votantes:** en `.local/demo-ual.md`. Cada elección es una sección y cada votante una fila; el enlace
  está en la columna «Enlace personal». Cada prueba de abajo dice la sección y la fila exactas.
- **Códigos de 6 cifras** (del votante y de la consola): en Mailpit, `http://localhost:8025`. Déjalo abierto en una
  pestaña. Usa siempre **el correo más reciente para ese votante o esa cuenta**.
- **Consola:** `http://localhost:4200/admin`, con el usuario de la tabla «Cuentas de gestión» de `demo-ual.md`, la
  contraseña que te dé el autor y el código de Mailpit.

| Votante | Sección de `demo-ual.md` | Fila | Pruebas |
| --- | --- | --- | --- |
| `presente.votante5` | «Elección en curso: Representantes en Junta de Centro · E2» | Votante 5 | 1, 10, 11 y 15 |
| `presente.votante3` | «Elección en curso: Representantes en Junta de Centro · E2» | Votante 3 | 2 a 8 |
| `presente.votante1` | «Elección en curso: Representantes en Junta de Centro · E2» | Votante 1 | 9 |
| `presente.votante4` | «Elección en curso: Representantes en Junta de Centro · E2» | Votante 4 | 12 |
| `futura.votante1` | «Elección futura: Presupuestos participativos 2027 · E2» | Votante 1 | 13 |
| `pasada.votante5` | «Elección pasada: Delegación de curso 2026 · E2» | Votante 5 | 14 |
| `futura.votante2` | «Elección futura: Presupuestos participativos 2027 · E2» | Votante 2 | 16 |

## 3. Cómo rellenar la hoja

Solo la pestaña **«Hallazgos»**, celdas amarillas. Los desplegables ponen el texto exacto; `pantalla` admite también
texto libre. Rellena **todas** las columnas de cada fila (también `rnf`); `tiempo_realimentacion_s` solo si
cronometras. La pestaña «Instrucciones» explica cada columna y cuenta los problemas de gravedad alta.

Ejemplo de **formato** (los textos son ficticios: no los copies, escribe lo tuyo):

| evaluador | fecha | commit | pantalla | ancho_px | heuristica | descripcion | gravedad_0a4 | tiempo_realimentacion_s | rnf | propuesta |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| E2 | 28/11/2026 | 96617f3 | Votante · Papeleta | 360 | 4. Consistencia y estándares | El botón principal es verde aquí y azul en la pantalla anterior | 1 | | RNF-10 | Usar el mismo color |
| E2 | 28/11/2026 | 96617f3 | Votante · Demasiadas peticiones | 360 | | no revisado | | | | No se reiniciaron los servicios con el límite bajo |

Qué RNF elegir:

- **RNF-8** si el problema es un mensaje de error poco claro (no dice qué ha pasado o qué hacer) o una acción lenta sin
  ninguna señal de que algo pasa.
- **RNF-9** si en una pantalla del votante no se ve en qué paso está o el estado del proceso (abierto, hora de cierre).
- **RNF-10** si algo es inconsistente entre pantallas: nombres distintos para la misma acción, botones en sitios
  distintos, patrones de error o de confirmación distintos.
- **Otro** para cualquier otro problema de usabilidad.

### Lo que miras en TODAS las pantallas del votante

| Pregunta | Si falla, RNF |
| --- | --- |
| ¿Se ve en qué paso estoy («Paso n de 4»)? | RNF-9 |
| ¿Se ve el estado del proceso (abierto, hora de cierre)? | RNF-9 |
| Si hay un error, ¿dice **qué ha pasado** y **qué hago ahora**? | RNF-8 |
| ¿Esto se ve o se llama distinto que en otra pantalla? | RNF-10 |

## 4. Orden de la sesión (resumen)

| N.º | Prueba | Quién | Ancho |
| --- | --- | --- | --- |
| 1 | V9 Sesión caducada · parte A (dejarla esperando) | `presente.votante5` | 360 |
| 2–8 | V1 Bienvenida, V2 Código, V3 Código incorrecto, V4 Papeleta, V5 Revisión, V6 Recibo, V7 Comprobación del recibo | `presente.votante3` | 360 |
| 9 | V8 Ya has votado | `presente.votante1` | 360 |
| 10 | V9 Sesión caducada · parte B (a los 16 min) | `presente.votante5` | 360 |
| 11 | V10 Voto no registrado (opcional) | `presente.votante5` + autor | 360 |
| 12 | V11 Bloqueo por intentos | `presente.votante4` | 360 |
| 13 | V12 Votación no abierta | `futura.votante1` | 360 |
| 14 | V13 Resultado | `pasada.votante5` | 360 |
| 15 | V1, V4 y V6 a 1280 px | `presente.votante5` | 1280 |
| 16 | V14 Demasiadas peticiones (opcional) | `futura.votante2` + autor | 360 |
| 17–27 | Consola: C1, C6, C7, C8, C9, C10, C11, C2, C3, C4, C5 | cuentas de gestión | 1280 |
| 28–29 | V15 Votación cerrada (necesita el proceso de la prueba 26) | votante del censo nuevo + autor | 360 |

## 5. Pruebas del votante

Hazlas a **360 px** salvo la prueba 15. Cada votante, en **su propia pestaña** de la ventana normal.

### Prueba 1 · V9 Sesión caducada · parte A (dejarla esperando)

- **Enlace:** sección «Elección en curso · E2», fila **Votante 5**. Pestaña nueva, solo para esta prueba.
- **Pasos:**
  1. Pulsa «Empezar».
  2. Escribe el código de Mailpit (el más reciente para `presente.votante5`).
  3. En la papeleta **no toques nada**. Apunta la hora: ________
  4. Deja esta pestaña quieta y sigue con la prueba 2. Volverás aquí en la prueba 10.

### Pruebas 2 a 8 · recorrido completo con `presente.votante3`

- **Enlace:** sección «Elección en curso · E2», fila **Votante 3**. Pestaña nueva. Las pruebas 2 a 8 se hacen
  **seguidas en esta misma pestaña**.

#### Prueba 2 · V1 Bienvenida

- **Pasos:** abre el enlace y no pulses nada.
- **Qué mirar:** ¿se entiende qué es, hasta cuándo se puede votar y qué va a pasar? ¿Se ve el botón para empezar sin
  desplazar la pantalla? ¿Funcionan los botones ES / EN?

#### Prueba 3 · V2 Código de acceso

- **Pasos:** pulsa «Empezar». Toca la primera casilla. Mira el texto bajo el botón «Continuar» y espera a que cambie.
- **Qué mirar:** ¿se entiende dónde llega el código, cuántas cifras tiene y cuánto dura?

#### Prueba 4 · V3 Código incorrecto

- **Pasos:** escribe `000000` **una sola vez** (más veces bloquearía a este votante).
- **Qué mirar:** ¿el mensaje dice qué ha pasado, qué hacer y cuántos intentos quedan?

#### Prueba 5 · V4 Papeleta

- **Pasos:**
  1. Escribe el código correcto de Mailpit (el más reciente).
  2. Pulsa «Revisar mi voto» **sin marcar nada** y mira el error.
  3. Marca una opción y después otra.
- **Qué mirar:** la pregunta y las opciones. ¿Se distingue cuál está marcada? ¿El error dice qué hacer?

#### Prueba 6 · V5 Revisión

- **Pasos:** pulsa «Revisar mi voto». Busca cómo cambiar la elección, úsalo y vuelve a la revisión.
- **Qué mirar:** ¿se ve qué has elegido y que aún no has votado?

#### Prueba 7 · V6 Recibo (aquí se vota de verdad)

- **Pasos:** pulsa «Confirmar y votar». Usa el botón para copiar el código de recibo y pégalo en un bloc de notas.
- **Qué mirar:** ¿queda claro que has votado? ¿Se ve el código de recibo entero?

#### Prueba 8 · V7 Comprobación del recibo

- **Pasos:**
  1. Pulsa «Comprobar mi recibo». Si el código no aparece ya escrito, pégalo y pulsa «Comprobar».
  2. Escribe un código inventado de 64 caracteres y pulsa «Comprobar».
  3. Escribe un código de 10 caracteres y pulsa «Comprobar».
- **Qué mirar:** los mensajes de los tres casos.

### Prueba 9 · V8 Ya has votado

- **Enlace:** sección «Elección en curso · E2», fila **Votante 1**. Pestaña nueva.
- **Pasos:** «Empezar» y el código de Mailpit.
- **Qué mirar:** ¿el mensaje dice que ya votó y qué puede hacer?

### Prueba 10 · V9 Sesión caducada · parte B

- **Cuándo:** cuando hayan pasado **16 minutos** desde la hora de la prueba 1.
- **Enlace:** ninguno nuevo. Vuelve a **la pestaña de la prueba 1** (no abras el enlace otra vez).
- **Pasos:** marca una opción, pulsa «Revisar mi voto» y después «Confirmar y votar». Pulsa el botón que ofrezca.
- **Qué mirar:** ¿el mensaje dice qué ha pasado y si has votado o no? ¿Dice cómo seguir?

### Prueba 11 · V10 Voto no registrado (opcional)

- **Enlace:** el mismo de la prueba 1 (sección «Elección en curso · E2», fila **Votante 5**).
- **Pasos:**
  1. Entra hasta la pantalla «Revisa tu voto». **No pulses «Confirmar».**
  2. Avisa al autor, que para la urna en PowerShell: `Stop-Process -Id ((Get-Content .local\logs\pids.txt)[3])`
  3. Pulsa «Confirmar y votar». Pulsa el botón que ofrezca.
  4. El autor reinicia los servicios (`.\scripts\dev\stop-services.ps1` y `.\scripts\dev\start-services.ps1`).
- **Qué mirar:** ¿el mensaje dice que el voto no se ha registrado y qué hacer?
- Si no se hace: fila `no revisado`.

### Prueba 12 · V11 Bloqueo por intentos

- **Enlace:** sección «Elección en curso · E2», fila **Votante 4**. Pestaña nueva.
- **Pasos:** «Empezar» y escribe `000000` **cinco veces**, mirando el mensaje cada vez. Pulsa el botón que ofrezca la
  pantalla de bloqueo.
- **Qué mirar:** ¿cada mensaje dice cuántos intentos quedan? ¿La pantalla de bloqueo dice por qué y hasta cuándo?

### Prueba 13 · V12 Votación no abierta

- **Enlace:** sección «Elección futura · E2», fila **Votante 1**. Pestaña nueva.
- **Qué mirar:** ¿se entiende que aún no se puede votar y cuándo se podrá?

### Prueba 14 · V13 Resultado

- **Enlace:** sección «Elección pasada · E2», fila **Votante 5**. Pestaña nueva.
- **Pasos:** pulsa «Ver el resultado» y escribe el código de Mailpit.
- **Qué mirar:** ¿se entiende el resultado, la participación y qué significa «Resultado verificado»?

### Prueba 15 · V1, V4 y V6 a 1280 px

- **Antes:** quita el modo móvil de DevTools (Ctrl+Mayús+M otra vez). Haz esta prueba **después** de la 10 y la 11.
- **Enlace:** el de la prueba 1 (sección «Elección en curso · E2», fila **Votante 5**), que aún no ha votado.
- **Pasos:** bienvenida (V1) → «Empezar» → código de Mailpit → papeleta (V4) → marca una opción → «Revisar mi voto» →
  «Confirmar y votar» → recibo (V6).
- **Qué mirar:** lo mismo que en las pruebas 2, 5 y 7, ahora en pantalla grande.

### Prueba 16 · V14 Demasiadas peticiones (opcional)

- **Antes (autor, en PowerShell, desde la raíz del repositorio):**
  ```powershell
  .\scripts\dev\stop-services.ps1
  $env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_CAPACITY = '3'
  $env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_PER_MINUTE = '3'
  .\scripts\dev\start-services.ps1
  ```
- **Enlace:** sección «Elección futura · E2», fila **Votante 2**. Pestaña nueva.
- **Pasos:** recarga la página (F5) **cuatro o cinco veces seguidas**, rápido.
- **Qué mirar:** ¿el mensaje dice qué ha pasado y cuánto esperar? ¿Ofrece cómo seguir?
- **Después (autor), obligatorio:** quitar el límite y reiniciar.
  ```powershell
  Remove-Item Env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_CAPACITY, Env:GATEWAY_VOTER_ACCESS_RATE_LIMIT_PER_MINUTE
  .\scripts\dev\stop-services.ps1
  .\scripts\dev\start-services.ps1
  ```
- Si no se hace: fila `no revisado`.

## 6. Pruebas de la consola (1280 px)

Cada cuenta en su ventana de incógnito. En todas, para RNF-10, compara con las pantallas anteriores: ¿misma palabra
para la misma acción, botón principal en el mismo sitio, mismos mensajes de error y de confirmación?

El orden está pensado para que la prueba 28 funcione: las pruebas que usan la urna (17 a 23) van antes de crear el
proceso nuevo.

### Prueba 17 · C1 Inicio y «Primeros pasos» — `admin@ual.test`

- **Pasos:** entra. Mira la pantalla de inicio y la guía «Primeros pasos». Sigue uno de sus enlaces y vuelve.

### Prueba 18 · C6 Participación — `admin@ual.test`

- **Pasos:** «Participación», proceso «Representantes en Junta de Centro · E2».
- **Qué mirar:** ¿se entienden los recuentos y la gráfica?

### Prueba 19 · C7 Resultados y acta — `admin@ual.test`

- **Pasos:** «Resultados», proceso «Delegación de curso 2026 · E2». Exporta el CSV y genera el acta en PDF; abre los dos.

### Prueba 20 · C8 Claves de API — `admin@ual.test`

- **Pasos:** crea una clave. Mira qué se avisa y cuándo. Revócala.

### Prueba 21 · C9 Privacidad — `admin@ual.test`

- **Pasos:** busca `presente.votante2@ual.test` y exporta sus datos. **No** borres a nadie.

### Prueba 22 · C10 Auditoría y verificación de la urna — `auditor@ual.test`

- **Pasos:** «Auditoría», proceso «Delegación de curso 2026 · E2». Pulsa «Verificar integridad ahora».
- **Qué mirar:** el resultado y la tabla.

### Prueba 23 · C11 Trazabilidad — `auditor@ual.test`

- **Pasos:** «Trazabilidad»: filtra por un día y por una persona. Mira los resultados y la paginación.

### Prueba 24 · C2 Censos e importación de Excel — `censo@ual.test`

- **Pasos:**
  1. Entra en «Censos», crea un censo nuevo («Prueba E2») y ábrelo.
  2. Descarga la plantilla de Excel, borra las filas de ejemplo, escribe tres personas inventadas con correo
     `@ual.test` y súbela. Mira la validación y confirma la importación.
  3. Sube otra vez la plantilla con un error a propósito (un correo sin «@»). Mira el mensaje.
  4. Opcional (RNF-8): cronometra la importación con DevTools → Performance (marcar «Screenshots») y anota en
     `tiempo_realimentacion_s` los segundos desde el clic hasta la primera señal (indicador, mensaje), no hasta el final.

### Prueba 25 · C3 Alta y edición de una persona — `censo@ual.test`

- **Pasos:** en el censo «Prueba E2», añade una persona a mano, edítala y bórrala. Mira las confirmaciones.
  Las tres personas importadas en la prueba 24 deben quedarse (se usan en la prueba 28).

### Prueba 26 · C4 Asistente de proceso (4 pasos) — `admin@ual.test`

- **Pasos:**
  1. «Procesos» → nuevo proceso con el censo «Prueba E2». Recorre los 4 pasos.
  2. En el paso de fechas pon un cierre anterior a la apertura y pulsa «Siguiente». Mira el mensaje.
  3. Corrige las fechas así: **apertura dentro de 10 minutos y cierre 5 minutos después de la apertura**. Apunta la
     hora de cierre: ________
  4. Crea el proceso.

### Prueba 27 · C5 Ciclo de vida — `admin@ual.test`

- **Pasos:** en el proceso creado, pulsa «Programar» **antes de la hora de apertura**. Opcional: cronometra «Programar».
- **Qué mirar:** la confirmación y el estado.

### Pruebas 28 y 29 · V15 Votación cerrada

- **Antes (autor), justo después de la prueba 27 y antes de la hora de cierre:** parar la urna, para que el proceso se
  quede cerrado y sin escrutar:
  `Stop-Process -Id ((Get-Content .local\logs\pids.txt)[3])`
- **Prueba 28 · enlace:** en Mailpit, la invitación a una de las tres personas del censo «Prueba E2» (llega al programar
  el proceso). Copia el enlace personal del correo. **No lo abras todavía.**
- **Prueba 29 · cuándo:** cuando haya pasado la hora de cierre de la prueba 26 (espera 1 minuto más).
  - **Pasos:** cambia a 360 px, abre ese enlace en una pestaña nueva de la ventana normal.
  - **Qué mirar:** ¿se entiende que la votación está cerrada, desde cuándo y qué pasará después (resultado)?
- **Después (autor):** reiniciar los servicios (`.\scripts\dev\stop-services.ps1` y `.\scripts\dev\start-services.ps1`).
- Si la pantalla no sale o el proceso ya aparece escrutado: fila `no revisado` y avisar al autor.

## 7. Al terminar

1. Repasa la hoja: cada fila tiene pantalla, heurística, descripción, gravedad y RNF; las gravedades 3 y 4 están
   confirmadas repitiendo la prueba.
2. Comprueba que las pruebas que no hiciste tienen su fila `no revisado`.
3. Guarda la hoja y dásela al autor. No la compartas con la otra persona evaluadora.

## 8. Juntar las hojas (autor, cuando estén las dos)

1. Pasarme las dos hojas. Las uno: los problemas que describen lo mismo se fusionan en una fila con la **mayor**
   gravedad, y compruebo en la aplicación los que sean de gravedad 3 o 4 antes de decidir.
2. Aplico la decisión de `protocolo.md`. Si queda alguna de gravedad 3 o 4: defecto en `resultados-p7/defectos.md` y
   corrección solo con tu aprobación; después se repiten esas pruebas.
3. Paso el resultado a la tabla 13.2, `informe.md` y `PLAN.md`.

## 9. Limitación que se declarará

E1 es el autor del sistema: conoce la interfaz y puede pasar por alto problemas. Por eso la decisión se toma con las
dos hojas juntas y no con una sola, y esto se anota en el capítulo 13. Si la segunda persona no puede hacer la
revisión, se declara M3 con un solo evaluador como limitación, sin presentarla como una revisión de dos.
