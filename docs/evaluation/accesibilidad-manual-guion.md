# H1a · Guion de la revisión manual con teclado y NVDA

Parte manual de H1a (RNF-1). La hace **el autor**. Este guion no contiene ningún resultado: la columna
`texto_observado_nvda` de las hojas solo se rellena con lo que NVDA lea de verdad. Lo que no se pueda reproducir se
marca «no revisado», nunca «cumple».

- Hoja de las comprobaciones obligatorias: `accesibilidad-manual-obligatorias.csv`.
- Hoja de las recomendadas: `accesibilidad-manual-recomendadas.csv`.
- Parte automática y avisos de axe: `accesibilidad-axe-2026-11-05.md`. Lista general: `accesibilidad-manual.md`.

## 1. Antes de empezar (10 min)

### 1.1 Versión

Trabajar sobre `dev` en `06f39be`, sin cambios en `frontend/` (`git status --short frontend` no debe mostrar nada; los
cambios en `docs/` no afectan a la aplicación). Las correcciones de DEF-05 y DEF-06
entraron en el commit `9c948cf`, que forma parte de `06f39be`. En PowerShell, desde la raíz del repositorio:

```powershell
git rev-parse --short HEAD
git status --short frontend
Get-FileHash frontend\src\app\features\voter\steps\ballot-step.component.ts -Algorithm SHA256
Get-FileHash frontend\src\app\features\voter\receipt-check.page.ts -Algorithm SHA256
Get-FileHash frontend\tests\e2e\voter-flow.accessibility.spec.ts -Algorithm SHA256
```

| Fichero | Valor esperado (PowerShell muestra el SHA-256 en mayúsculas) |
| --- | --- |
| `git rev-parse --short HEAD` | `06f39be` |
| `ballot-step.component.ts` | `353213FB93E633515A271366AAE8B90A5BB1427FC9071DBBDA759CED92FC4F02` |
| `receipt-check.page.ts` | `0448F477BBA3E5E8D610560CB717A83B5EA22F64229628D7CD75A9E8937211E9` |
| `voter-flow.accessibility.spec.ts` | `E4D440E2C4DFFDE27435E4B79AA6F09F7743F4AFA9BE2C3BE7ECFE4EF0397F59` |

Son los ficheros tal como Git los deja en Windows (saltos de línea CRLF). Su contenido es el mismo que se midió el
05/11 (`402C9EF12240…` y `747D65CBEC86…` eran las huellas de esos ficheros con saltos LF).

Si alguno no coincide, **no se sigue**.

### 1.2 Datos sintéticos (no `USAB`)

1. Arrancar el stack como siempre y cargar la demo de la UAL (`.local\2-cargar-demo.cmd`, que ejecuta
   `scripts/demo/seed-ual.mjs`). No usar `seed-usab.mjs` ni el escenario `USAB`.
2. Abrir `.local/demo-ual.md`. Tiene los enlaces personales y la dirección de Mailpit, donde llegan los códigos.
   El proceso «en curso» abre unos 90 s después de crearse.
3. Elegir las personas votantes:

| Papel en el guion | Proceso | Votante | Para qué |
| --- | --- | --- | --- |
| Votante A | «Elección en curso: Representantes en Junta de Centro» | `presente.votante3@ual.test` | V1, B1, V2, B2, B3, B4 y B5 (emite un voto) |
| Votante B | El mismo | `presente.votante4@ual.test` | B1-05 y C4 (queda bloqueado 15 min) |
| Votante V8 | «Elección pasada: Delegación de curso 2026» | `pasada.votante5@ual.test` | V8 (solo contraste) |
| Recomendadas | Ver cada fila | Votantes 1 y 5 de «en curso» y cualquiera de «futura» | V3, V6 y V4 |

Si un votante que debería estar libre muestra «Ya has votado», es que se usó antes: se pasa al siguiente y se anota.
La consulta del código en Mailpit puede hacerse con el ratón: Mailpit no forma parte de lo que se evalúa.

### 1.3 Herramientas

- **NVDA** (anotar la versión: menú NVDA → Ayuda → Acerca de). Activar el **Visor de voz** (menú NVDA, Insert+N →
  Herramientas → Visor de voz): muestra por escrito todo lo que dice NVDA. De ahí se copia el texto literal.
- **Navegador**: Chrome o Edge (anotar cuál y su versión en `chrome://version` o `edge://version`), ventana de
  1280 px de ancho o más, al 100 % de zoom.
- **360 px** (solo para medir contraste): DevTools (F12) → barra de dispositivo (Ctrl+Mayús+M) → ancho 360. Cerrar
  DevTools antes de volver a las comprobaciones con NVDA.
- **Colour Contrast Analyser** (TPGi): cuentagotas sobre el texto (primer plano) y sobre el fondo; se anota la
  relación que muestra (p. ej. «5,1:1»).

## 2. Teclas de NVDA que se usan

| Tecla | Qué hace |
| --- | --- |
| Tab / Mayús+Tab | Va al siguiente / anterior control; NVDA lo lee |
| Intro | Activa el botón o el enlace con foco |
| Espacio | Marca la opción con foco |
| Flecha abajo / arriba | En la página (modo exploración): lee la línea siguiente / anterior. Dentro de un grupo de opciones: cambia de opción |
| Flecha derecha / izquierda | Lee carácter a carácter la línea actual |
| Insert+Tab | Repite el elemento que tiene el foco |
| Ctrl | Hace callar a NVDA |
| Ctrl+A, Supr, Fin, Retroceso | Seleccionar todo, borrar, ir al final, borrar el carácter anterior (en un campo) |

En la tecla Insert, «Insert+…» significa mantener Insert y pulsar la otra tecla. Si NVDA está en modo foco y las
flechas no leen la página, Insert+Espacio vuelve al modo exploración.

## 3. Orden de ejecución

1. **Votante A**, a 1280 px y en español: V1-01 → V1-04, B1-01 → B1-03, V2-01, V2-02, B1-04, B2S-01, B2S-02,
   B2-01 → B2-05, B3-01 → B3-04, B4-01 → B4-05, B5P-01, B5P-02, B5N-01, B5F-01 y B5F-02. FOCO-01 se observa
   durante todo el recorrido.
2. **Contraste del votante A**, en cuanto se llega a cada pantalla (antes de seguir): B2-04, B4-04, B5P-03.
3. **Votante B**: «Empezar», esperar 60 s a que aparezca «Reenviar el código», cambiar a EN y medir B1-05 a 360 px.
   Después, C4-01 → C4-03.
4. **Votante V8**: V8C-01 y V8C-02.
5. Recomendadas, si hay tiempo (sección 5).

## 4. Comprobaciones obligatorias

| id | Pantalla y estado | Qué se comprueba | Teclas, en orden | Qué debe anunciar NVDA (correcto) | Qué anoto si no lo anuncia | Bloqueo en esta pantalla |
| --- | --- | --- | --- | --- | --- | --- |
| V1-01 | V1 bienvenida: Recién abierto el enlace del votante A | Al cargar, el foco va al título y NVDA lo lee | Ninguna: abrir el enlace y esperar 2 s | «Demo UAL · Elección en curso: Representantes en Junta de Centro» y que es un encabezado de nivel 1 | Copia del Visor de voz lo que se oiga; si no dice nada, pulsa Insert+Tab y copia lo que lea | No: si no se lee solo pero se encuentra con Insert+Tab o con las flechas es incidencia |
| V1-02 | V1 bienvenida: Recién abierto el enlace del votante A | Se puede leer el estado del proceso | Flecha arriba varias veces desde el título (modo exploración) hasta oír la barra de estado | «Votación abierta · Cierra el <fecha y hora>» | Copia lo que se oiga | No: es información, no impide votar |
| V1-03 | V1 bienvenida: Recién abierto el enlace del votante A | El selector de idioma tiene nombre y estado y funciona con el teclado | Mayús+Tab desde el título hasta oír «EN»; Intro; escuchar; Mayús+Tab o Tab hasta «ES»; Intro | «EN Inglés, botón, no pulsado» y «ES Español, botón, pulsado» (las palabras del rol y del estado dependen de NVDA); al pulsar EN el texto pasa a inglés y al pulsar ES vuelve a español | Copia lo que se oiga en cada botón | Sí, si no se puede cambiar de idioma con el teclado o no hay forma de saber cuál está activo |
| V1-04 | V1 bienvenida: Recién abierto el enlace del votante A, en español | Se llega a «Empezar» y se activa | Tab desde el título hasta «Empezar»; Intro | «Empezar, botón»; al pulsar Intro pasa a la pantalla del código (B1) | Copia lo que se oiga; anota cuántos Tab hicieron falta | Sí, si «Empezar» no se alcanza con Tab o Intro no hace nada |
| B1-01 | B1 código: Tras pulsar «Empezar» | El foco va al título de la pantalla nueva y se lee | Ninguna: esperar 2 s | «Código de acceso» y que es un encabezado de nivel 1 | Copia lo que se oiga; si no dice nada, Insert+Tab | No: es incidencia si el contenido se encuentra con las flechas |
| B1-02 | B1 código: Tras pulsar «Empezar» | Se puede leer adónde se ha enviado el código | Flecha abajo una vez desde el título | «Hemos enviado un código de 6 cifras a <correo con parte oculta>. Caduca en <N> minutos.» | Copia lo que se oiga | No |
| B1-03 | B1 código: Tras pulsar «Empezar» | La casilla del código tiene un nombre comprensible | Tab desde el título hasta la primera casilla | «Código de acceso» y que es un campo de edición. Aviso: Playwright vio que la casilla puede llevar el nombre interno de Ionic en inglés («Input 1 of 6»); si es eso lo que oyes, cópialo tal cual | Copia lo que se oiga | No, si aun así se puede escribir el código. Si el nombre está en inglés, es incidencia |
| B1-04 | B1 código: Después de V2: escribir el código correcto (Mailpit) | Con el código correcto se pasa a la papeleta | Si quedan cifras en las casillas, Retroceso hasta vaciarlas; escribir las 6 cifras del correo (al escribir la sexta se envía solo) | Pasa a la papeleta y NVDA lee la pregunta (ver B2-01) | Copia lo que se oiga | Sí, si no se puede escribir o corregir el código con el teclado |
| B1-05 | B1 código: A 360 px y en EN, con el botón «Resend the code» visible (aparece a los 60 s del envío) | Contraste del texto del botón «Resend the code» / «Reenviar el código» (aviso de axe) | Colour Contrast Analyser: cuentagotas sobre el texto y sobre el fondo del botón | No aplica (medición). Umbral: 4,5:1 | Anota en «notas» el valor (p. ej. «5,1:1»), los colores y el idioma; si no se pudo medir en EN, dilo | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio (WCAG 1.4.3) |
| V2-01 | V2 código incorrecto: Votante A en B1, antes de escribir el código bueno | El error se anuncia solo, sin buscarlo | En la primera casilla escribir 000000 (se envía solo al teclear la sexta cifra) | «El código no es correcto. Te quedan 4 intentos.» sin pulsar nada más | Copia lo que se oiga; si no se anuncia, busca el mensaje con flecha abajo y anota si lo encuentras | Sí, si el error no se anuncia y no se encuentra navegando; es incidencia si no se anuncia pero se encuentra |
| V2-02 | V2 código incorrecto: Votante A en B1, antes de escribir el código bueno | Se puede volver a escribir el código | Mayús+Tab o Tab hasta la casilla; Retroceso hasta vaciarla; seguir con B1-04 | Se oye la casilla y se puede borrar y escribir | Copia lo que se oiga | Sí, si no se puede volver a escribir sin ratón |
| C4-01 | C4 acceso bloqueado: Votante B: abrir su enlace, «Empezar» y escribir 000000 cinco veces (vaciando la casilla entre intento e intento) | Al bloquearse, NVDA anuncia el bloqueo y hasta cuándo dura | Escribir 000000 y, si quedan cifras, Retroceso hasta vaciar; repetir hasta el quinto intento. Si tras el quinto sigue en la pantalla del código, un intento más | «Acceso bloqueado temporalmente» y «Has superado el número de intentos. Por seguridad, espera hasta las <hora> y vuelve a intentarlo.» | Copia lo que se oiga; si no se pudo bloquear, marca «no revisado» y explica por qué en «notas» | Sí, si no hay forma de saber que el acceso está bloqueado ni hasta cuándo |
| C4-02 | C4 acceso bloqueado: Votante B: abrir su enlace, «Empezar» y escribir 000000 cinco veces (vaciando la casilla entre intento e intento) | El botón de la pantalla se alcanza y tiene nombre | Tab desde el título | «Volver a intentarlo, botón» | Copia lo que se oiga | No (la persona sabe qué pasa y hasta cuándo) |
| C4-03 | C4 acceso bloqueado: Votante B: abrir su enlace, «Empezar» y escribir 000000 cinco veces (vaciando la casilla entre intento e intento), a 1280 px y a 360 px | Contraste del texto del botón «Volver a intentarlo» (aviso de axe) | Colour Contrast Analyser: texto y fondo del botón, a cada ancho | No aplica (medición). Umbral: 4,5:1 | Anota el valor de cada ancho en «notas» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |
| B2S-01 | B2 papeleta sin elegir: Votante A recién llegado a la papeleta, sin marcar nada | Continuar sin elegir: el error se anuncia | Tab desde el título hasta «Revisar mi voto» (sin marcar ninguna opción); Intro | «Todavía no has elegido una opción.» sin pulsar nada más | Copia lo que se oiga; si no se anuncia, búscalo con las flechas y anota si lo encuentras | Sí, si no se anuncia y no se encuentra navegando |
| B2S-02 | B2 papeleta sin elegir: Votante A recién llegado a la papeleta, sin marcar nada, tras el error | Se puede volver a las opciones | Mayús+Tab hasta oír una opción | El nombre de una opción (ver B2-02) | Copia lo que se oiga | Sí, si no se puede volver a las opciones con el teclado |
| B2-01 | B2 papeleta: Votante A en la papeleta | Al llegar, el foco va a la pregunta y se lee | Ninguna: esperar 2 s al llegar (o Insert+Tab) | «¿Qué candidatura apoyas para la Junta de Centro?» y que es un encabezado de nivel 1 | Copia lo que se oiga | No: es incidencia si se encuentra con las flechas |
| B2-02 | B2 papeleta: Votante A en la papeleta | Cada opción tiene nombre, rol, estado y posición | Desde la pregunta, Tab hasta la primera opción | «Lista Participa, botón de opción, no marcado, 1 de 3» o similar. Comprueba también: (a) que no diga «lista» al entrar en las opciones (DEF-05); (b) si nombra el grupo. En el código el grupo no tiene nombre propio; si NVDA no lo nombra, anótalo | Copia lo que se oiga | Sí, si no se puede saber qué opción es cada una; que falte el nombre del grupo es incidencia |
| B2-03 | B2 papeleta: Votante A en la papeleta | Las flechas recorren las opciones y se sabe cuál queda marcada | Flecha abajo, flecha abajo, flecha arriba (dejar marcada «Lista Campus Abierto»); si alguna opción no se marca al moverte, Espacio | Al bajar: «Lista Campus Abierto, … marcado, 2 de 3»; «Voto en blanco, Cuenta como participación, pero no apoya a ninguna opción., … marcado, 3 de 3»; al subir vuelve a «Lista Campus Abierto, marcado» | Copia lo que se oiga en cada pulsación | Sí, si no se puede marcar una opción con el teclado o no se sabe cuál queda marcada |
| B2-04 | B2 papeleta: Votante A en la papeleta, a 1280 px y a 360 px | Contraste del texto de la primera opción («Lista Participa») sobre su tarjeta (aviso de axe) | Colour Contrast Analyser: texto y fondo de la tarjeta, a cada ancho | No aplica (medición). Umbral: 4,5:1 | Anota el valor de cada ancho en «notas» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |
| B2-05 | B2 papeleta: Con «Lista Campus Abierto» marcada | Se continúa a la revisión | Tab hasta «Revisar mi voto»; Intro | «Revisar mi voto, botón»; pasa a la revisión (B3) | Copia lo que se oiga | Sí, si no se puede continuar con el teclado |
| B3-01 | B3 confirmación: Tras «Revisar mi voto» | El foco va al título y se lee | Ninguna: esperar 2 s (o Insert+Tab) | «Revisa tu voto» y que es un encabezado de nivel 1 | Copia lo que se oiga | No: es incidencia si se encuentra con las flechas |
| B3-02 | B3 confirmación: Tras «Revisar mi voto» | Se lee la opción elegida y el aviso antes de confirmar | Flecha abajo varias veces desde el título | «Has elegido», «Lista Campus Abierto», «Aún no has votado. Cuando confirmes, tu voto no se podrá cambiar.» | Copia lo que se oiga | Sí, si no hay forma de saber qué opción se va a confirmar |
| B3-03 | B3 confirmación: Tras «Revisar mi voto» | Se puede volver atrás sin perder la elección | Tab hasta «Cambiar mi selección»; Intro; en la papeleta, Tab hasta la opción marcada y escuchar; Tab hasta «Revisar mi voto»; Intro | «Cambiar mi selección, botón»; en la papeleta «Lista Campus Abierto» sigue marcada; se vuelve a la revisión | Copia lo que se oiga | No, salvo que la elección cambie sin que se anuncie |
| B3-04 | B3 confirmación: Tras «Revisar mi voto» | Se confirma el voto con el teclado y se informa de la espera | Tab hasta «Confirmar y votar»; Intro | «Confirmar y votar, botón»; puede oírse «No cierres esta ventana.»; pasa al recibo (B4) | Copia lo que se oiga | Sí, si no se puede confirmar con el teclado |
| B4-01 | B4 recibo: Tras confirmar | Se anuncia que el voto se ha registrado | Ninguna: esperar 2 s; después, flecha abajo una vez | «Voto registrado» (encabezado de nivel 1) y «Gracias. Tu voto está en la urna.» | Copia lo que se oiga | Sí, si no hay forma de saber que el voto se ha registrado |
| B4-02 | B4 recibo: Tras confirmar | El código de recibo se puede leer, también carácter a carácter. Incluye el aviso de axe aria-prohibited-attr: el párrafo del código lleva aria-labelledby sin un rol que lo permita | Flecha abajo hasta oír «Código de recibo» y el código; en la línea del código, flecha derecha 5 o 6 veces | «Código de recibo» y luego los 64 caracteres del código; con flecha derecha, un carácter por pulsación | Copia lo que se oiga. Anota si NVDA lee «Código de recibo» como nombre del código, solo el código o las dos cosas | Sí, si el código no se puede leer. Que no lea la etiqueta como nombre es incidencia |
| B4-03 | B4 recibo: Tras confirmar | El código se copia con el teclado y se confirma | Tab hasta «Copiar el código»; Intro | «Copiar el código, botón» y después «Copiado» | Copia lo que se oiga; para comprobar la copia, pega en un bloc de notas (Ctrl+V) | No, si el código se puede leer (B4-02) y apuntar |
| B4-04 | B4 recibo: A 360 px | Contraste del texto del botón «Copiar el código» (aviso de axe a 360 px) | Colour Contrast Analyser: texto y fondo del botón | No aplica (medición). Umbral: 4,5:1 | Anota el valor en «notas» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |
| B4-05 | B4 recibo: A 1280 px | Se llega a «Comprobar mi recibo» | Tab hasta «Comprobar mi recibo»; Intro | «Comprobar mi recibo», con rol de botón o de enlace; pasa a la comprobación (B5) con el código ya escrito | Copia lo que se oiga | No: la comprobación también puede abrirse escribiendo la dirección |
| B5P-01 | B5 comprobación: Desde B4, con el código ya escrito | El título y el campo se leen con su contenido | Esperar 2 s; Tab hasta el campo | «Comprobar mi recibo» (encabezado de nivel 1); en el campo, «Código de recibo», que es un campo de edición y el código; puede añadir «64 caracteres (0-9 y a-f)» | Copia lo que se oiga | Sí, si no se sabe qué es el campo |
| B5P-02 | B5 comprobación, presente: Desde B4, con el código ya escrito | El resultado «presente» se anuncia solo | Tab hasta «Comprobar»; Intro | «Comprobar, botón» y después «Tu voto está en la urna.» sin pulsar nada más | Copia lo que se oiga; si no se anuncia, busca el resultado con las flechas y anota si lo encuentras | Sí, si no hay forma de conocer el resultado; es incidencia si no se anuncia pero se encuentra |
| B5P-03 | B5 comprobación: Desde B4, con el código ya escrito, a 1280 px y a 360 px | Contraste del texto del botón «Comprobar» (aviso de axe). V6 usa un botón con el mismo estilo (color secondary, relleno), así que esta medición vale también para V6 | Colour Contrast Analyser: texto y fondo del botón, a cada ancho | No aplica (medición). Umbral: 4,5:1 | Anota el valor de cada ancho en «notas» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |
| B5N-01 | B5 comprobación, no encontrado: Desde B4, con el código ya escrito, después de B5P-02 | El resultado «no encontrado» se anuncia solo | Mayús+Tab hasta el campo; Fin; Retroceso; escribir otro carácter (0 si el último era distinto de 0; 1 si era 0); Tab hasta «Comprobar»; Intro | «No encontramos ese código en esta votación. Revisa que esté completo.» | Copia lo que se oiga | Sí, si no hay forma de conocer el resultado |
| B5F-01 | B5 comprobación, formato erróneo: Desde B4, con el código ya escrito, en español | El error de formato (DEF-06) se lee solo, sin mover el foco a mano | Mayús+Tab hasta el campo; Ctrl+A; Supr; escribir abc; Tab hasta «Comprobar»; Intro | «El código debe tener 64 caracteres entre 0-9 y a-f.» sin pulsar nada más | Copia lo que se oiga; si no se anuncia, busca el mensaje con las flechas y anota si lo encuentras | Sí, si no se anuncia y no se encuentra; es incidencia si no se anuncia pero se encuentra |
| B5F-02 | B5 comprobación, formato erróneo: Desde B4, con el código ya escrito, en inglés | El mismo error en inglés | Mayús+Tab hasta «EN»; Intro; Tab hasta el campo; Ctrl+A; Supr; escribir abc; Tab hasta «Check»; Intro | «The code must have 64 characters from 0-9 and a-f.» | Copia lo que se oiga | Igual que B5F-01 |
| FOCO-01 | Todas las anteriores: Durante el recorrido a 1280 px | El foco se ve en cada control al llegar con Tab | Mirar la pantalla en cada Tab del recorrido | No aplica (se mira, no se escucha) | En «notas», cada control en el que no se viera el foco | Sí, si en algún control no se ve dónde está el foco y eso impide seguir (WCAG 2.4.7) |
| V8C-01 | V8 resultado (solo contraste): Proceso «pasada» (escrutado), votante 5: enlace, «Ver el resultado», código del correo; a 1280 px | Contraste del texto «Los votos publicados solo llevan un identificador aleatorio…» (aviso de axe) | Colour Contrast Analyser: texto y fondo | No aplica (medición). Umbral: 4,5:1 (texto de 13 px) | Anota el valor en «notas»; si no se puede llegar a V8, «no revisado» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |
| V8C-02 | V8 resultado (solo contraste): Proceso «pasada» (escrutado), votante 5: enlace, «Ver el resultado», código del correo; a 1280 px, a 360 px | Contraste de «Huella del resultado» y de su valor (aviso de axe) | Colour Contrast Analyser: el rótulo, su valor y el fondo | No aplica (medición). Umbral: 4,5:1 | Anota los dos valores en «notas» | No es bloqueo; por debajo de 4,5:1 es incumplimiento serio |

## 5. Comprobaciones recomendadas

Solo si se pueden reproducir con datos sintéticos propios; si no, «no revisado».

| id | Pantalla y estado | Qué se comprueba | Teclas, en orden | Qué debe anunciar NVDA (correcto) | Qué anoto si no lo anuncia | Bloqueo en esta pantalla |
| --- | --- | --- | --- | --- | --- | --- |
| V3-01 | V3 ya votó: Votante 1 del proceso «presente» (ya votó con el script): enlace, «Empezar», código | Se anuncia que ya ha votado | Ninguna: esperar 2 s tras el código | «Ya has votado» y «Tu voto se registró el <día>. Solo se puede votar una vez.» (o sin el día) | Copia lo que se oiga | Sí, si no se sabe por qué no puede votar |
| V6-01 | V6 sesión caducada: Votante 5 del proceso «presente»: llegar a la papeleta, esperar más de 15 min sin tocar nada, marcar una opción y pulsar «Revisar mi voto» o «Confirmar y votar» | Se anuncia que la sesión ha caducado y que no ha votado | Las de B2-03 y B2-05 después de la espera | «Tu sesión ha caducado» y «Tu selección no se ha guardado y no has votado. Vuelve a entrar con el mismo enlace.» | Copia lo que se oiga; si no aparece, «no revisado» | Sí, si no se sabe que no ha votado |
| V6-02 | V6 sesión caducada: Votante 5 del proceso «presente»: llegar a la papeleta, esperar más de 15 min sin tocar nada, marcar una opción y pulsar «Revisar mi voto» o «Confirmar y votar» | El botón se alcanza y tiene nombre | Tab desde el título | «Volver a entrar, botón» | Copia lo que se oiga | Sí, si no se puede volver a entrar con el teclado |
| V7-01 | V7 voto no registrado: Solo si puedes parar ballot-box-service justo antes de «Confirmar y votar» (si no, «no revisado») | Se anuncia que el voto NO se ha registrado y se puede reintentar | Intro en «Confirmar y votar»; después, Tab | «Tu voto NO se ha registrado», «Ha fallado la conexión o el servidor. Puedes reintentarlo sin riesgo: nunca se registrarán dos votos.» y «Reintentar, botón» | Copia lo que se oiga | Sí, si se puede pensar que el voto se registró |
| V4-01 | V4 aún no abierto: Cualquier votante del proceso «futura»: abrir su enlace | Se lee el título y el mensaje al cargar | Ninguna: esperar 2 s; flecha abajo | «La votación todavía no está abierta» y «Podrás votar a partir del <fecha>. Vuelve a abrir este enlace entonces.» | Copia lo que se oiga | No |
| V5-01 | V5 cerrado: Solo si tienes un proceso CERRADO y aún sin escrutar (la demo no crea ninguno; si no lo hay, «no revisado») | Se lee el título y el mensaje al cargar | Ninguna: esperar 2 s; flecha abajo | «La votación está cerrada» y «El plazo terminó el <fecha>. …» | Copia lo que se oiga | No |
| C3I-01 | C3 enlace no válido: Cambiar un carácter del token en la dirección de cualquier enlace (Ctrl+L, editar, Intro) | Se lee el título y el mensaje al cargar | Ninguna: esperar 2 s; flecha abajo | «Este enlace no es válido» y «Usa el enlace de tu último correo de invitación. …» | Copia lo que se oiga | No |
| C3E-01 | C3 enlace caducado: Solo si tienes un enlace caducado (30 días después del cierre); si no, «no revisado» | Se lee el título y el mensaje al cargar | Ninguna: esperar 2 s; flecha abajo | «El enlace ha caducado» y «Contacta con tu organización para recibir un enlace nuevo.» | Copia lo que se oiga | No |
| SSO-01 | Acceso con cuenta institucional (RF-05b): Solo si tienes un proceso con cuenta institucional (la demo no lo crea; si no, «no revisado») | El título y el botón se leen y el botón se activa | Ninguna al llegar; Tab hasta el botón; Intro | «Entra con tu cuenta institucional» (encabezado) y «Entrar con tu cuenta institucional, botón» | Copia lo que se oiga | Sí, si no se puede iniciar la entrada con el teclado |
| V8-01 | V8 resultado: Proceso «pasada», votante 5 (igual que V8C-01) | Se lee el título y la información del resultado | Ninguna al llegar; flecha abajo por la página | El título del proceso (encabezado) y los resultados como texto (cada opción con su número de votos) | Copia lo que se oiga | Sí, si el resultado solo se ve en un gráfico sin texto |

## 6. Cómo se decide

**Bloqueo:** algo que impide a una persona que usa solo teclado y NVDA terminar el acceso, el voto o el recibo sin
ayuda de otra persona ni del ratón, o que la lleva a hacer algo distinto de lo que quería sin saberlo. La columna
«Bloqueo en esta pantalla» lo concreta para cada fila.

**Incidencia:** se puede terminar, pero algo se anuncia mal, de más o en otro idioma, o el orden es mejorable.
Un contraste por debajo del umbral es un **incumplimiento serio** (WCAG 1.4.3): se anota con su valor.

Valores de `decision`: `cumple`, `bloqueo`, `incidencia`, `no revisado`.

| Resultado de las obligatorias | Decisión de H1a |
| --- | --- |
| Alguna fila con `bloqueo` | **Rechazada** en esa versión |
| Ninguna con `bloqueo`, pero alguna `no revisado` | **Pendiente** |
| Todas revisadas, sin `bloqueo` y sin contraste por debajo del umbral | **Aceptada** |
| Todas revisadas, sin `bloqueo`, con algún contraste por debajo del umbral | Se decide si se corrige antes del cierre. Si se corrige, se vuelve a medir esa fila sobre la nueva versión. Si no se corrige, el criterio no se cumple y H1a queda **rechazada** (el criterio no se relaja) |

Las filas `incidencia` no impiden aceptar H1a: se documentan y se registran como mejoras.

## 7. Cómo se devuelve el resultado

Las dos hojas CSV rellenas (en UTF-8, sin cambiar las columnas ni los `id`) y, en el mensaje, esta cabecera:

```text
Fecha y hora de la revisión:
Sistema operativo:
NVDA, versión:
Navegador y versión:
Commit (git rev-parse --short HEAD):
SHA-256 de los tres ficheros: coinciden / no coinciden (cuál)
Votantes usados (A, B, V8) y si alguno ya estaba usado:
Herramienta de contraste y versión:
```

## Anexo · Redacción si la parte manual no se llega a hacer

Solo se usa si la revisión no se realiza antes del cierre. No es un incumplimiento: es una parte del protocolo que no
se ejecutó, igual que en RNF-4. No se aplica a la memoria hasta que el autor lo decida. Aplicado el 08/11/2026; H1a queda como parte automática favorable y parte manual no realizada, sin decisión.

**Tabla 13.1 (`evaluacion_resultados.tex`), columna «Resultado medido» de H1a**, en lugar del texto actual:

```latex
parte automática favorable; parte manual no realizada. Barrido axe del flujo del votante (05/11/2026; 22 pantallas o estados, 1280 y 360 píxeles, español e inglés; 88 auditorías): el primer barrido dio 8 incumplimientos críticos de una misma incidencia en la papeleta (\texttt{aria-required-children}); corregida, el segundo barrido da 0 críticos y 0 serios. También se corrigió una incidencia que axe no detecta (el error de formato del código de recibo no se mostraba); el tercer barrido, con las dos correcciones, da 0 críticos y 0 serios. Repetido el 08/11/2026 sobre la versión vigente (\texttt{06f39be}), con el mismo resultado. La revisión manual con teclado y lector de pantalla que exige el criterio no se realizó antes del cierre: no es un incumplimiento, sino una parte del protocolo sin ejecutar. Evidencia parcial; sin decisión. \\
```

**`conclusiones.tex`**: sustituir el paréntesis de H1a por:

```latex
(H1a: parte automática favorable; parte manual no realizada; la parte
automática se midió en el flujo del votante tras corregir las incidencias de
la papeleta y del recibo)
```

y, si en ese párrafo se enumera lo que «queda por ejecutar», mantener ahí la revisión manual con teclado y lector
de pantalla.

**`contenido1.tex`**, lista de hipótesis: etiqueta `\item [parte automática favorable; parte manual no realizada]`,
y la última frase del punto pasa a ser:

```latex
  serios. La revisión manual con teclado y lector de pantalla no se realizó
  antes del cierre: es una parte del protocolo sin ejecutar, no un
  incumplimiento, y H1a queda sin decisión.
```

**Capítulo de validez.** En `evaluacion_resultados.tex`, sección «Validez», añadir al final del párrafo:

```latex
La parte manual de H1a (teclado y lector de pantalla) no se ejecutó: el
barrido automático solo cubre las reglas que axe puede decidir y dejó
avisos sin resolver (contraste sobre fondos que no puede calcular y un
atributo ARIA del recibo), y no comprueba cómo se anuncian los errores ni el
orden del foco. Por eso H1a no se declara aceptada ni rechazada; igual que
RNF-4, queda como evidencia parcial y sin decisión.
```

En `contenido1.tex`, «Amenazas a la validez», párrafo «Validez de constructo», añadir tras «certificación WCAG;»:

```latex
además, la revisión manual de H1a no se realizó, así que la conclusión sobre
accesibilidad se limita a lo que axe puede decidir;
```
