# Repetición de B1-01 y B1-03 tras IMP-67 e IMP-68 · plan para el 24/11/2026 (mañana temprano)

En M1 (22/11/2026) B1-01 y B1-03 se revisaron sobre la versión anterior y dieron las dos incidencias (la cuenta atrás
se leía cada segundo y la casilla se anunciaba «Input 1 of 6»). El 23/11 se corrigieron en el código. Falta comprobar
la versión nueva con el Narrador: es una **nueva medición de esas dos filas**, no se cambia lo anotado el 22/11
(igual que se hizo con el contraste de B5P-03). Duración: unos 45 minutos.

## 0. Antes (10 min)

1. Commit de lo del 23/11 (o anotar que se prueba sin commit) y `git rev-parse --short HEAD`.
2. Arrancar el stack con `start-services.ps1` y **reiniciar el frontend** (`.local\7-reiniciar-frontend.cmd`): los
   textos nuevos están en `es.json` y `en.json`, y sin reiniciar se ven los antiguos.
3. Votante: `presente.votante5` de la demo (el 3 votó y el 4 se bloqueó en M1). Si ya está usado, cargar la demo otra
   vez no lo libera: usar un proceso nuevo como en `simulacion-tutor.md`.
4. Chrome a 1280 px, idioma ES, Narrador encendido y Visor de voz abierto. Mismo método que en M1: la IA envía las
   teclas desde el navegador y tú escuchas y confirmas.

## 1. Pruebas automáticas (15 min, en `frontend`)

```powershell
npm run test:coverage      # se esperan 261 pruebas y 0 fallos; anota la cobertura de líneas
npm run test:e2e           # E2E de accesibilidad: se esperan 100 de 100 sin críticos ni serios
```

## 2. Narrador (20 min)

| Fila | Qué se hace | Qué se debe oír | Decisión |
| --- | --- | --- | --- |
| B1-01 (repetición) | Pulsar «Empezar» y esperar 70 s sin tocar nada | El título «Código de acceso, encabezado de nivel 1». **No** debe leerse la cuenta atrás cada segundo. Al acabar la espera, una sola vez: «Ya puedes pedir otro código.» | `cumple` si se oye así; `incidencia` si se repite la cuenta atrás o no se avisa al final |
| B1-03 (repetición) | Tab desde el título hasta la primera casilla; escribir una cifra (el foco pasa sola a la siguiente) y otra más | «Código de acceso, cifra 1 de 6, edición», después «cifra 2 de 6» y «cifra 3 de 6». Ionic oculta al lector las casillas vacías que no tienen el foco, así que no se recorren con flechas. En inglés (botón EN): «Access code, digit 1 of 6» | `cumple` si se oye el nombre nuevo en los dos idiomas; `incidencia` si vuelve «Input 1 of 6» |
| B1-03b (nueva) | Con la interfaz en ES, cambiar a EN **sin recargar** y volver a la primera casilla | El nombre en inglés | Comprueba que el nombre cambia con el idioma |

Se anotan como filas nuevas al final de `accesibilidad-manual-obligatorias.csv`, con los `id` `B1-01R`, `B1-03R` y
`B1-03bR`, la fecha y el commit en `notas`. Las filas del 22/11 no se tocan.

## 3. Después (yo)

- Si las tres cumplen: IMP-67 e IMP-68 pasan a «Hechas y verificadas»; en la memoria (detalle de H1a y sección 14.3)
  se quita «a falta de repetir esas dos comprobaciones»; cifras de 261 pruebas y la cobertura nueva en el resumen, el
  capítulo 13, la tabla 9.2, la presentación e `informe.md`.
- Si alguna da `incidencia`: se registra y lo corrijo antes de tocar la memoria. H1a no cambia (eran incidencias, no
  bloqueos).
