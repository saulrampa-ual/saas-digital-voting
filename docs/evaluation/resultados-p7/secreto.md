# Secreto del voto e integridad

La integración de `BallotBoxPostgresIntegrationTest` recorre todas las columnas de las 14 tablas de `ballot_box_db` (excepto `flyway_schema_history`). Falla si aparecen columnas de identidad especificadas en la prueba o marcas temporales fuera de la lista permitida: calendario del proceso, apertura/cierre de eventos, escrutinio, revisión de cadena, generación del acta y outbox administrativa. Comprueba además que `VOTE_RECEIVED.occurred_at` sea nulo. Participación de voter-access conserva `voted_on` como `DATE` en la prueba existente.

La batería de `ChainVerifierTest` comprueba hash, enlace, secuencia, cabeza, papeleta canónica, votos/eventos/recibos, evento `CLOSED` y huella final. Esta rama añade casos independientes para papeleta alterada y desajuste de recibos.

| Checklist voting-secrecy-guard | Resultado |
| --- | --- |
| 1. Votos sin identidad, censo, enlace, OTP ni token reversible | Cumple por inspección del esquema PostgreSQL en Testcontainers |
| 2. Participación sin opción, recibo ni id de voto | Cumple por esquema vigente; participación por día en prueba existente |
| 3. Sin identificador compartido entre las dos BD | PENDIENTE: no se ejecutó flujo completo en el stack |
| 4. Token solo para elegibilidad y de un uso | Parcial: unidad existente de VotingToken; faltan pruebas de flujo integral |
| 5. Sin hora exacta en voto/recibo y participación solo por día | Cumple en las pruebas de esquema existentes y ampliadas |
| 6. Orden de inserción no expuesto | PENDIENTE de revisión de endpoints/exportaciones completas |
| 7. Cadena sin identidad ni hora de emisión | Parcial: eventos de voto sin hora; revisión completa de logs pendiente |
| 8. Logs de ballot-box sin IP/cabeceras/token/identidad | PENDIENTE; servicios no estaban arrancados |
| 9. Logs de voter-access sin opción/recibo | PENDIENTE; servicios no estaban arrancados |
| 10. Respuestas/notificaciones/exportaciones sin identidad y voto | PENDIENTE de flujo integral |
| 11. Llamadas urna → voter-access solo con token | PENDIENTE de inspección de tráfico real |

Límites: esta revisión no cubre coacción, cliente comprometido ni observador de tráfico en tiempo real. El MVP no acredita verificabilidad criptográfica completa.
