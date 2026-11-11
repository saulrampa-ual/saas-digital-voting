# RNF-4 · Protocolo y guion para repetir la sesión con administradores

Preparado el 08/11/2026 para cerrar RNF-4 (hoy **no demostrado**, ver `resultados-rnf4-administradores.md`). **No se
ha ejecutado.** Corrige las dos causas de aquella decisión: la muestra no era la población del criterio (solo 2 de 5
sin formación previa) y los tiempos se estimaron después en lugar de cronometrarse. Hoja de registro:
`hoja-registro-rnf4.csv`.

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

## Participantes

- Cinco personas adultas que **nunca hayan usado VeriVoto** (ni la consola ni una demostración guiada) y hasta dos de
  reserva. Pueden tener experiencia general con aplicaciones web; lo que se excluye es la formación en esta herramienta.
- Comprobación antes de empezar, con una sola pregunta: «¿Has usado antes VeriVoto o has visto una demostración de
  cómo se crea un proceso?». Si la respuesta es sí, **no participa** (se agradece y se pasa a una reserva). La columna
  `sin_formacion_previa` de la hoja debe valer `si` en todas las sesiones válidas.
- Consentimiento firmado antes de empezar (el mismo modelo de `consentimiento.md`, adaptado a administradores). Los
  consentimientos se guardan fuera del repositorio.
- Identificadores anónimos P01–P05, R1 y R2, nuevos para esta sesión. No anotar nombres, correos ni otro dato que
  identifique a la persona.

## Preparación (una vez, antes de la primera sesión)

1. Stack local arrancado con datos sintéticos. **No reutilizar el escenario `USAB` de H1b** mientras esté en uso.
2. Una organización de prueba con una cuenta ADMIN y **un censo con al menos una persona sintética** (programar exige
   censo con personas, textos en español e inglés y apertura posterior al momento de programar).
   Lo prepara `scripts/demo/seed-rnf4.mjs` (11/11/2026): organización `RNF4`, cuenta ADMIN `admin.rnf4@verivoto.test`,
   gestor de censo `censo.rnf4@verivoto.test` y censo «Censo de prueba RNF-4» con tres personas sintéticas; no crea
   ningún proceso. Uso: `$env:DEMO_PASSWORD = "<12+ caracteres>"; node scripts/demo/seed-rnf4.mjs`. Se puede repetir
   antes de cada día de sesiones: reutiliza lo que ya existe y lista los procesos que dejaron los participantes.
3. Por cada participante, la persona moderadora inicia sesión antes con la cuenta ADMIN (usuario, contraseña y
   código de correo), de modo que el tiempo no incluya el login de Keycloak. Se deja la consola en «Inicio».
4. Si un participante anterior dejó un proceso, se deja como está: cada participante crea uno nuevo con su propio
   título (ver la ficha).
5. La documentación que se puede consultar está abierta en otra pestaña: `docs/` del proyecto o la guía que el autor
   decida. Se anota cuál fue en la observación general (no por participante).

## Ficha de la tarea (se entrega impresa)

> Crea un proceso de votación de **opción única** con estos datos y déjalo **programado**. Avísame cuando termines.
>
> - Título: «Elección de prueba Pxx» (Pxx es tu código) · en inglés: «Test election Pxx».
> - Apertura: mañana a las 10:00 · Cierre: mañana a las 18:00.
> - Censo: «Censo de prueba RNF-4».
> - Pregunta: «¿Qué día prefieres para la reunión?» · en inglés: «Which day do you prefer for the meeting?».
> - Opciones: «Martes» / «Tuesday» y «Jueves» / «Thursday».
> - Voto en blanco: permitido.

Los datos son neutros y sintéticos. No se pide ninguna preferencia personal.

## Guion

1. Leer literalmente: «Vas a usar la consola de una herramienta de votaciones. Te daré una ficha con lo que tienes que
   hacer. Puedes consultar la documentación que está abierta en la otra pestaña. Yo solo observo y tomo notas: no te
   indicaré cómo usar la aplicación salvo que decidas abandonar».
2. Entregar la ficha. **T0** = la persona termina de leer la ficha y toca por primera vez la pantalla (ratón o
   teclado). Iniciar el cronómetro.
3. Observar sin intervenir. Si la persona pregunta, responder: «Puedes consultar la documentación; yo no puedo
   ayudarte». Si insiste o abandona, se le puede ayudar, pero la sesión **ya no cuenta como sin ayuda**
   (`sin_ayuda = no`, anotar en `ayuda_ofrecida` el momento y el contenido en términos generales).
4. **T1** = el proceso aparece con el estado «Programado» tras confirmar el aviso de programar. Parar el cronómetro.
   Si a los 15 minutos no ha terminado, se detiene la tarea (`completo = no`) y se anota el tiempo como 900.
5. Comprobar en la consola que el proceso creado es de opción única, con el censo, los textos en los dos idiomas y
   las fechas de la ficha. Si algún dato no coincide, `completo = no` y se describe en `observacion` sin datos
   personales.
6. Agradecer y, si la persona lo pide, explicar al final lo que le haya bloqueado.

## Qué cuenta como error, ayuda o incidencia de entorno

- **Error** (`errores`): acción que obliga a volver atrás, un aviso de validación o un dato introducido mal y luego
  corregido. Se cuentan, no invalidan la sesión.
- **Consulta de documentación** (`consulto_documentacion`): permitida; solo se anota `si`/`no`.
- **Ayuda** (`sin_ayuda = no`): cualquier indicación de una persona sobre cómo usar la aplicación.
- **Incidencia de entorno** (`incidencia_entorno = si`, `valida = no`): caída del stack, Keycloak o la red ajena a la
  persona. La sesión se sustituye por una reserva.

## Registro

`hoja-registro-rnf4.csv`, una fila por sesión. Valores `si`/`no`; `t0_t1_segundos` en segundos enteros, medidos con
cronómetro (no estimados después). La fila se rellena **al terminar cada sesión**, no al final del día.

Éxito de una sesión = `valida = si`, `sin_formacion_previa = si`, `completo = si`, `sin_ayuda = si` y
`t0_t1_segundos <= 600`.

## Cómo pasarlo a la memoria

Con la hoja completa:

```powershell
node scripts/evaluation/rnf4-summary.mjs
```

El script comprueba que no falten datos y que todas las sesiones válidas sean sin formación previa, y escribe las
cantidades (x de 5), la mediana y el rango de tiempos y la decisión de la tabla de arriba. Después, la IA actualiza `resultados-rnf4-administradores.md`, la tabla de hipótesis (`evaluacion_resultados.tex`),
`contenido1.tex`, `conclusiones.tex` y `especificacion_detallada.tex`. La sesión del 03/11/2026 se conserva como
antecedente (evidencia indicativa) y no se mezcla con la nueva.

## Límites

n = 5 y un entorno local con datos sintéticos: el resultado se limita al prototipo, la configuración y la población
evaluados. Que la persona moderadora inicie sesión antes deja fuera del tiempo el login de Keycloak (con código por
correo); se declara así en la memoria.
