# ADR-0006: Registro de auditoría encadenado por hash

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: —

## Contexto

RF-12 y RNF-13 exigen detectar cualquier alteración del registro de la urna.

## Decisión

Cada evento guarda `event_hash = SHA-256(process_id | sequence | event_type | occurred_at | payload_hash | previous_hash)`. La secuencia se serializa por proceso con un bloqueo de fila. La huella final es el hash del evento `CLOSED` y se publica.

## Consecuencias

Cualquier cambio, borrado o reordenación rompe la cadena. No impide que un administrador con acceso total reescriba la cadena completa antes de publicar la huella final.

## Alternativas descartadas

Árbol de Merkle: permitiría pruebas de inclusión más eficientes; innecesario con los volúmenes del MVP.
