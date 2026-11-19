# Revisión heurística (RNF-8, RNF-9 y RNF-10) · mejora M3

Protocolo preparado el 18/11/2026. **Sin ejecutar.** No contiene resultados: la hoja se rellena solo con lo que
observen las personas evaluadoras.

## Qué se comprueba

| RNF | Verificación que pide la especificación | Criterio |
| --- | --- | --- |
| RNF-8 Mensajes de error y realimentación | Revisar los mensajes de las excepciones de los casos de uso y cronometrar las acciones lentas | Toda acción da realimentación visible en 1 segundo o menos (valor propuesto) y todo error dice qué pasó y qué hacer |
| RNF-9 Orientación del votante | Revisar las pantallas del flujo de voto (y H1b, ya hecha) | El votante sabe en qué paso está y el estado del proceso en cada pantalla |
| RNF-10 Consistencia de la interfaz | Revisión heurística de Nielsen por al menos 2 evaluadores | Ningún problema de gravedad alta (3 o 4) |

## Quién

Dos personas que **revisan por separado** y después juntan sus hojas:

1. El autor.
2. Otra persona con conocimientos de interfaces (por ejemplo, un compañero del máster). No debe haber participado
   en el desarrollo. Una IA no cuenta como evaluadora.

## Entorno

- `dev` actualizado, stack arrancado y demo UAL cargada (`node scripts/demo/seed-ual.mjs`). Solo datos sintéticos.
- Navegador de escritorio a 1280 píxeles y, para el votante, también a 360 (DevTools, modo dispositivo).
- Anotar el commit (`git rev-parse --short HEAD`).

## Pantallas (60–90 minutos por persona)

- **Votante:** bienvenida, código (incluido código incorrecto y bloqueo), papeleta, revisión, recibo, voto no
  registrado, ya has votado, sesión caducada, votación no abierta o cerrada, demasiadas peticiones, resultado y
  comprobación del recibo.
- **Consola:** inicio y «Primeros pasos», censos e importación de Excel, asistente de proceso (4 pasos), ciclo de vida,
  participación, resultados y acta, trazabilidad, privacidad y claves de API.

## Las 10 heurísticas de Nielsen

1. Visibilidad del estado del sistema.
2. Relación entre el sistema y el mundo real (lenguaje del usuario).
3. Control y libertad del usuario (deshacer, salir).
4. Consistencia y estándares.
5. Prevención de errores.
6. Reconocer mejor que recordar.
7. Flexibilidad y eficiencia de uso.
8. Diseño estético y minimalista.
9. Ayudar a reconocer, diagnosticar y recuperarse de errores.
10. Ayuda y documentación.

## Gravedad (escala de Nielsen)

| Valor | Significado |
| --- | --- |
| 0 | No es un problema de usabilidad |
| 1 | Cosmético: arreglar si sobra tiempo |
| 2 | Menor: prioridad baja |
| 3 | Mayor: importante arreglarlo (gravedad alta) |
| 4 | Catástrofe: imprescindible arreglarlo (gravedad alta) |

## Cómo se registra

Cada persona rellena su copia de `hoja-revision-heuristica.csv` (una fila por problema). Para RNF-8, en las acciones
que llaman al servidor (pedir código, verificar, votar, importar censo, programar proceso, generar acta) se cronometra
cuánto tarda en aparecer la primera señal de que algo pasa (spinner, botón desactivado, mensaje) y se anota en
`tiempo_realimentacion_s`.

Después se juntan las dos hojas: los problemas repetidos se unen y a cada uno se le asigna la **mayor** de las dos
gravedades.

## Decisión

- RNF-10 **se cumple** si tras juntar las hojas no queda ningún problema de gravedad 3 o 4.
- RNF-8 **se cumple** si además todas las acciones cronometradas dan realimentación en 1 segundo o menos y todos los
  mensajes de error explican qué hacer.
- RNF-9 **se cumple** si en todas las pantallas del votante se ve el paso actual y el estado del proceso.
- Si hay problemas de gravedad alta, se registran como defecto (`docs/evaluation/resultados-p7/defectos.md`); se
  corrigen solo con aprobación del autor y se repite la revisión de esas pantallas.

El resultado se pasa a la tabla 13.2 de la memoria (columna «Estado» de RNF-8, RNF-9 y RNF-10).
