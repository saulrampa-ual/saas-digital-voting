# ADR-0002: Comunicación REST síncrona con outbox en election

- Estado: Aceptada
- Fecha: 2026-10-29
- Decisiones relacionadas: D-06, D-21

## Contexto

election debe comunicar a voter-access y ballot-box la instantánea del censo, la papeleta y los cambios de estado (programado, abierto, cerrado).

## Decisión

REST síncrono entre servicios. election guarda los cambios pendientes en `process_sync` y su *scheduler* los reintenta hasta recibir confirmación. Los endpoints receptores (`/internal/v1/...`) son idempotentes. Ballot-box rechaza votos después de `closesAt` aunque no haya llegado el aviso de cierre.

## Consecuencias

Sin broker de mensajes que operar. El outbox evita perder transiciones si un servicio está caído.

## Alternativas descartadas

Kafka u otro broker (IMP-07): desacopla más, pero añade infraestructura que el MVP no necesita.
