# Revisión heurística (M3) · guion para cada persona evaluadora

Preparado el 23/11/2026. Complementa a `protocolo.md` (qué se mide y cómo se decide). **Sin ejecutar.**

- **E1** es el autor y rellena `hoja-E1-autor.csv`. **E2** es la segunda persona y rellena `hoja-E2.csv`.
- Cada una revisa **sola y sin ver la hoja de la otra**. Las hojas se juntan al final (sección 5).
- Duración: 60–90 minutos. Si algo no se puede reproducir con la demo, se anota una fila con
  `descripcion = no revisado` y se sigue.
- Solo datos sintéticos de la demo UAL. Nada de capturas con datos reales.

## 1. Preparación (10 min, la hace el autor para las dos sesiones)

1. Arrancar el stack (`start-services.ps1`, **sin** `-NoRateLimit`, para poder ver el mensaje de demasiadas
   peticiones) y cargar la demo: `$env:DEMO_PASSWORD = '<contraseña>'; node scripts/demo/seed-ual.mjs`.
2. Abrir `.local/demo-ual.md` (enlaces de votantes y Mailpit) y `.local/cuentas-locales.md` (cuentas de gestión).
3. Anotar el commit en la columna `commit`: `git rev-parse --short HEAD`.
4. En «en curso» la demo ya emite los votos de los votantes 1 y 2; quedan libres el 3, el 4 y el 5. Volver a
   ejecutar el script no los libera (reutiliza lo que existe). Para la sesión de E2, el autor crea antes en la consola
   un proceso nuevo con un censo de 5 votantes sintéticos (`simulacion-tutor.md`, secciones 1 y 2) y usa esos enlaces
   con los mismos papeles de la tabla de la sección 3.

## 2. Cómo cronometrar la realimentación (RNF-8)

Para las acciones que llaman al servidor, en Chrome: DevTools → **Performance** → marcar **Screenshots** → grabar →
hacer la acción → parar. Se mide desde el clic (fila *Interactions*) hasta el primer fotograma con alguna señal
(spinner, botón desactivado o mensaje) y se anota en segundos en `tiempo_realimentacion_s`. Un cronómetro de mano
no sirve para distinguir 1 segundo.

Acciones a cronometrar: pedir código, verificar código, votar, comprobar recibo, importar censo (Excel), programar
proceso y generar acta.

## 3. Recorrido del votante (1280 px y 360 px)

| Pantalla o estado | Cómo llegar en la demo | Votante sugerido |
| --- | --- | --- |
| Bienvenida | Abrir el enlace del proceso «Elección en curso» | `presente.votante3` |
| Código, código incorrecto | «Empezar», escribir 000000 una vez | `presente.votante3` |
| Papeleta, revisión, recibo | Código correcto de Mailpit, elegir, revisar y votar (cronometrar «Votar») | `presente.votante3` |
| Ya has votado | Abrir el enlace de un votante que ya votó | `presente.votante1` (votó la demo) |
| Demasiadas peticiones | Pedir y reenviar el código repetidamente hasta que salga el aviso; si no sale, `no revisado` | `presente.votante4` |
| Bloqueo por intentos | Después del anterior, escribir 000000 cinco veces | `presente.votante4` (queda bloqueado 15 min) |
| Sesión caducada | Llegar a la papeleta y esperar más de 15 min sin tocar nada (empezarlo pronto, en paralelo) | `presente.votante5` |
| Votación no abierta | Enlace de cualquier votante del proceso «futura» | `futura.votante1` |
| Votación cerrada | Solo si hay un proceso cerrado sin escrutar; si no, `no revisado` | — |
| Voto no registrado | No se reproduce con la demo: `no revisado` | — |
| Resultado y comprobación del recibo | Proceso «pasada»: «Ver el resultado»; después comprobar un código de recibo | `pasada.votante5` |

En cada pantalla del votante, para RNF-9: ¿se ve en qué paso está y el estado del proceso (abierto, hora de cierre)?
Si no, fila con `rnf = RNF-9`.

## 4. Recorrido de la consola (1280 px)

| Rol (ventana privada por rol) | Pantallas |
| --- | --- |
| Administradora `laura.moreno@ual.test` | Inicio y «Primeros pasos»; asistente de proceso (4 pasos, incluidos errores de fechas); ciclo de vida (programar, abrir, cerrar); participación; resultados y acta; claves de API |
| Gestor de censo `sergio.cano@ual.test` | Censos; importación de Excel (plantilla buena y una con un error a propósito); alta y edición de una entrada |
| Auditora `irene.vega@ual.test` | Trazabilidad y verificación de la cadena; privacidad (búsqueda y exportación) |

Para RNF-10, comparar entre pantallas: mismos nombres para la misma acción, botones principales en el mismo sitio,
mismos patrones de error y de confirmación.

## 5. Juntar las hojas (autor, con E2 presente o por correo)

1. Unir los dos CSV. Los problemas que describen lo mismo se fusionan en una fila con la **mayor** gravedad.
2. Aplicar la decisión de `protocolo.md`. Si hay gravedad 3 o 4: defecto en `resultados-p7/defectos.md`, corrección
   solo con aprobación del autor y nueva revisión de esas pantallas.
3. Pasarme las dos hojas: redacto el resultado y actualizo la tabla 13.2, `informe.md` y `PLAN.md`.

## 6. Limitación que se declarará

E1 es el autor del sistema: conoce la interfaz y puede pasar por alto problemas. Por eso la decisión se toma con las
dos hojas juntas y no con una sola, y esto se anota en el capítulo 13.
