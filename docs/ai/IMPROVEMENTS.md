# IMPROVEMENTS · VeriVoto

Mejoras propuestas. Ninguna se implementa sin aprobación del autor.
Estados: propuesta / aprobada / descartada / hecha.

| ID | Fecha | Propuesta | Beneficio | Coste / riesgo | Hito | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| IMP-01 | 2026-10-29 | `TenantApiExceptionHandler` convierte cualquier `IllegalStateException` en 409 «ya existe». Usar una excepción de dominio propia (p. ej. `TenantAlreadyExistsException`) | Errores correctos (RNF-8); evita ocultar fallos reales | Bajo; cambia el comportamiento ante otros `IllegalStateException` | P3.1 | Propuesta |
| IMP-02 | 2026-10-29 | Corregir el Gantt del TFM: la iteración 3 dice «Emisión, recibo y re-voto (RF-09–RF-11)» | Coherencia memoria–plan tras D-03 | Ninguno | P8 | Propuesta |
| IMP-03 | 2026-10-29 | Activar solo las pruebas de propiedades críticas (unicidad, cadena de hashes, secreto) al terminar P4 y P5, en vez de esperar a enero | Detectar fallos del núcleo del TFM a tiempo; el Gantt ya prevé pruebas incrementales | Tiempo de desarrollo; requiere `ACTIVAR TESTS` | P4 | Propuesta |
| IMP-04 | 2026-10-29 | El identificador de organización es un `VARCHAR(20)` alfanumérico usado como clave primaria. Decidir si `organization_id` en el resto de servicios es ese identificador o un UUID | Contrato de multi-tenancy estable entre servicios | Migración Flyway si se cambia | P2 | Propuesta |
| IMP-05 | 2026-10-29 | Prototipos: quitar del alcance MVP los elementos de revoto («Cambiar mi voto», recibo sustituido, texto «hasta que se publique la huella final») de V1, V3 y B4 | Coherencia con D-03 | Ninguno en código; solo al implementar P4.5 | P4.5 | Propuesta |
| IMP-06 | 2026-10-29 | Prototipar las pantallas que faltan: notificaciones fallidas (RF-17) y claves de API (RF-21) | Cobertura completa del MVP | Tiempo de diseño | P6 | Propuesta |
| IMP-07 | 2026-10-29 | Eventos asíncronos (Kafka u otro broker) para el cambio de estado de procesos | Desacoplamiento | Infraestructura adicional | P9 | Propuesta |
| IMP-08 | 2026-10-29 | Cuando se active P7, añadir ArchUnit para comprobar las reglas de dependencia hexagonales | Auditoría automática de la arquitectura (P9.2 del plan anterior) | Es una prueba: requiere `ACTIVAR TESTS` | P7 | Propuesta |
