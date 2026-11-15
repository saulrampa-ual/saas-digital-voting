---
name: project-progress-log
description: Cómo actualizar la memoria del proyecto en docs/ai/ (PROGRESS, DECISIONS, TRACEABILITY, IMPROVEMENTS, CONTEXT) al cerrar una sesión o un hito de VeriVoto. Usar al terminar cada tarea o cuando el autor pida el cierre de sesión.
---

# Registro de progreso

## Pasos

1. Contrastar con el estado real: `git status`, `git branch --show-current`, `git log --oneline -10`.
2. `docs/ai/PROGRESS.md`:
   - Cabecera: fecha, rama y último commit reales.
   - Tabla de hitos: estado (Pendiente / En curso / Hecho / Bloqueada).
   - «Hecho»: solo lo que existe y compila; si no se compiló, decirlo.
   - «Pendiente», «Bloqueos / preguntas abiertas» y «Cómo continuar»: el siguiente paso exacto,
     ejecutable sin más contexto.
3. `docs/ai/DECISIONS.md`: añadir cada decisión nueva del autor como `D-xx` con fecha, motivo y
   alternativas. No reescribir decisiones antiguas: añadir una nueva que las sustituya.
4. `docs/ai/TRACEABILITY.md`: actualizar servicio, endpoint, pantalla y estado de cada RF tocado.
   Estados: Pendiente / Parcial: <qué falta> / Hecho (compila) / Verificado (con pruebas).
5. `docs/ai/IMPROVEMENTS.md`: añadir mejoras detectadas como `IMP-xx` (estado «Propuesta») y
   actualizar las que el autor apruebe o descarte.
6. `docs/ai/CONTEXT.md`: solo si cambia algo imprescindible para entender el proyecto.
7. No inventar datos: lo desconocido se marca como TBD.

## Salida

Resumen de 10 líneas como máximo: qué se hizo, qué falta, bloqueos y el siguiente prompt a usar.
