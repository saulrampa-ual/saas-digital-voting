---
name: voting-secrecy-guard
description: Checklist del secreto del voto (RNF-11) de VeriVoto. Usar al terminar cualquier cambio en voter-access-service, ballot-box-service, sus esquemas, logs, contratos o pantallas del votante, y antes de cerrar P4 y P5.
---

# Guardia del secreto del voto

Objetivo: que nadie, ni siquiera con acceso a las bases de datos o a los logs, pueda relacionar a un
votante con su voto. Revisar los cambios y responder cada punto con «cumple», «no cumple (dónde)» o
«no aplica». Reportar el resultado completo en la respuesta.

## Datos

1. La tabla de votos (`ballot_box_db`) no tiene ningún campo de identidad, de censo, de enlace
   personal, de OTP ni de token reversible.
2. La participación (`voter_access_db`) no guarda opción elegida, código de recibo ni identificador
   de voto.
3. No existe ningún identificador compartido entre ambas BD (ni UUID, ni hash determinista de algo
   que exista en la otra).
4. El token de voto solo acredita elegibilidad: es de un solo uso, de corta duración, sin identidad
   ni opción, y ballot-box no puede derivar de él quién es el votante.

## Tiempo y orden

5. La urna no guarda marcas de tiempo exactas que permitan cruzarlas con la hora de participación.
   Usar granularidad baja o ninguna; justificar la elegida.
6. El orden de inserción de votos no es observable (sin secuencias expuestas ni IDs crecientes en
   respuestas o exportaciones).
7. La cadena de hashes no incluye datos de identidad ni marcas de tiempo exactas.

## Comunicación y logs

8. Ningún log de ballot-box contiene IP, cabeceras de usuario, token completo ni identidad.
9. Ningún log de voter-access contiene la opción elegida ni el recibo.
10. Las respuestas HTTP, las notificaciones y las exportaciones nunca combinan identidad y voto.
11. Las llamadas ballot-box → voter-access llevan solo el token, nada más.

## Límites (decirlo con honestidad)

Esta revisión no protege frente a un cliente comprometido, coacción, ni un operador que observe el
tráfico en tiempo real en ambos servicios. El MVP no ofrece verificabilidad criptográfica completa.
