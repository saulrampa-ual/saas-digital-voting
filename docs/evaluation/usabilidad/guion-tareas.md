# Guion de la sesión de usabilidad

Leer los textos entre comillas literalmente y en el mismo orden con todas las personas. No explicar los controles ni
sugerir dónde pulsar. El escenario y las direcciones son sintéticos (véase [`preparacion-sesion.md`](preparacion-sesion.md)).

## 1. Antes de la tarea (sin cronometrar)

1. Entregar el consentimiento, resolver dudas y asignar el identificador (P01–P05, o R1/R2 si se usa una reserva).
2. Leer: «Vas a participar en una votación de prueba. Completa las tareas como lo harías normalmente. Puedo observar y
   tomar notas, pero no te indicaré cómo usar la aplicación salvo que quieras abandonar».
3. Señalar la pestaña de Mailpit y leer: «Esta pestaña es tu correo de prueba. En él tienes una invitación a una
   votación ficticia».
4. Entregar la tarjeta y leer: «En esta votación tienes que elegir la opción de la tarjeta. No es tu opinión: es parte
   de la tarea».

## 2. Tarea (cronometrada)

Leer: «Abre la invitación de tu correo, entra en la votación, vota la opción de la tarjeta y consigue tu recibo.
Después, comprueba tu recibo. Avísame cuando termines».

| Marca | Cuándo se anota |
| --- | --- |
| T0 | Aparece la bienvenida de la votación (botón «Empezar») |
| T1 | Aparece «Voto registrado» con el código de recibo |
| T2 | Aparece el resultado de «Comprobar mi recibo» |

- La duración de H1b es T1 − T0, en segundos. No se interrumpe a los 180 s: se deja continuar para observar.
- Se detiene la tarea si la persona avisa de que ha terminado, si pide abandonar o a los 10 minutos.

## 3. Pregunta de comprensión (después de T2)

Leer: «¿Qué te confirma este recibo y qué no te permite comprobar?». No reformular ni dar pistas. Anotar un resumen sin
datos personales.

**Codificación.** Comprensión correcta solo si la respuesta identifica que puede comprobar que su voto está en la urna
(presencia del voto registrado) y reconoce que no demuestra qué opción se registró ni que coincida con la que quería
emitir. No penalizar vocabulario no técnico si el significado es claro.

## 4. Ayuda

- **Cuenta como ayuda**: cualquier indicación sobre dónde pulsar, qué hacer después, qué significa una pantalla o dónde
  está el código. Si se ofrece, la tarea ya no es «sin ayuda»; anotar el momento y el contenido.
- **No cuenta como ayuda**: repetir una vez, literalmente, la instrucción de la tarea si la persona la pide.
- Si la persona pregunta cómo seguir, responder: «Haz lo que harías normalmente». Solo se ofrece ayuda si pide
  abandonar o lleva más de 2 minutos sin avanzar.

## 5. Eventos a registrar

- **Error**: acción que impide avanzar, obliga a volver atrás o provoca una interpretación incorrecta (por ejemplo,
  código erróneo, botón equivocado o pensar que ya ha votado sin haber confirmado).
- **Incidencia de entorno**: fallo de red, de un servicio, del correo de prueba o del equipo que afecte a la medición.
  Invalida la sesión (véase la preparación, apartado 7).
- **No registrar nunca**: nombre, correo real, código de acceso, token, recibo completo ni la opción mostrada en
  pantalla.
