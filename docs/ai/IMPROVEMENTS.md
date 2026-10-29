# IMPROVEMENTS · VeriVoto

Mejoras propuestas. Ninguna se implementa sin aprobación del autor.
Estados: propuesta / aprobada / descartada / hecha.

| ID | Fecha | Propuesta | Beneficio | Coste / riesgo | Hito | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| IMP-01 | 2026-10-29 | `TenantApiExceptionHandler` convierte cualquier `IllegalStateException` en 409 «ya existe». Usar una excepción de dominio propia (p. ej. `TenantAlreadyExistsException`) | Errores correctos (RNF-8); evita ocultar fallos reales | Bajo; cambia el comportamiento ante otros `IllegalStateException` | P3.1 | Hecha (P3.1: excepciones de dominio y `ApiExceptionHandler`) |
| IMP-02 | 2026-10-29 | Corregir el Gantt del TFM: la iteración 3 dice «Emisión, recibo y re-voto (RF-09–RF-11)» | Coherencia memoria–plan tras D-03 | Ninguno | P8 | Propuesta |
| IMP-03 | 2026-10-29 | Activar solo las pruebas de propiedades críticas (unicidad, cadena de hashes, secreto) al terminar P4 y P5, en vez de esperar a enero | Detectar fallos del núcleo del TFM a tiempo; el Gantt ya prevé pruebas incrementales | Tiempo de desarrollo; requiere `ACTIVAR TESTS` | P4 | Propuesta |
| IMP-04 | 2026-10-29 | El identificador de organización es un `VARCHAR(20)` alfanumérico usado como clave primaria. Decidir si `organization_id` en el resto de servicios es ese identificador o un UUID | Contrato de multi-tenancy estable entre servicios | Migración Flyway si se cambia | P2 | Descartada: se mantiene el identificador de RI-1 (D-17) |
| IMP-05 | 2026-10-29 | Prototipos: quitar del alcance MVP los elementos de revoto («Cambiar mi voto», recibo sustituido, texto «hasta que se publique la huella final») de V1, V3 y B4 | Coherencia con D-03 | Ninguno en código; solo al implementar P4.5 | P4.5 | Propuesta |
| IMP-06 | 2026-10-29 | Prototipar las pantallas que faltan: notificaciones fallidas (RF-17) y claves de API (RF-21) | Cobertura completa del MVP | Tiempo de diseño | P6 | Propuesta |
| IMP-07 | 2026-10-29 | Eventos asíncronos (Kafka u otro broker) para el cambio de estado de procesos | Desacoplamiento | Infraestructura adicional | P9 | Propuesta |
| IMP-09 | 2026-10-29 | Escaneo de secretos (p. ej. gitleaks) en la CI y como hook de pre-commit | Habría evitado que entraran contraseñas en `infra/.env.example` | Bajo; herramienta externa adicional | P1.6 | Propuesta |
| IMP-08 | 2026-10-29 | Cuando se active P7, añadir ArchUnit para comprobar las reglas de dependencia hexagonales | Auditoría automática de la arquitectura (P9.2 del plan anterior) | Es una prueba: requiere `ACTIVAR TESTS` | P7 | Propuesta |
| IMP-10 | 2026-10-29 | Devolver el estado real del usuario (`LOCKED` por fuerza bruta, `INACTIVE` si está deshabilitado) leyendo `attack-detection` y `enabled` de Keycloak; hoy siempre `ACTIVE` | Información fiel en la pantalla E3 | Una llamada extra a Keycloak por usuario listado | P6 | Propuesta |
| IMP-11 | 2026-10-29 | Al suspender una organización, deshabilitar sus usuarios en Keycloak y rechazar operaciones de sus miembros | RF-01 completo (hoy solo cambia el estado) | Bajo | P6 | Propuesta |
| IMP-12 | 2026-10-29 | La regla del último administrador no está protegida frente a dos cambios de rol simultáneos; usar bloqueo de fila o una restricción en BD | Elimina una carrera poco probable | Bajo | P6 | Propuesta |
| IMP-13 | 2026-10-29 | `Caller`, `CallerResolver`, `PageResult` y la configuración de seguridad y Swagger se repiten en cada servicio; extraer un módulo Maven común (`verivoto-common`) | Menos código duplicado | Acopla los servicios a una librería compartida | P6 | Propuesta |
| IMP-14 | 2026-10-29 | Tema propio de Keycloak para el login de gestión (D1): paleta, Inter, selector ES/EN y textos de los prototipos (hoy se ve el tema por defecto de Keycloak) | Coherencia visual entre login y consola (RNF-10) | Medio: plantillas FreeMarker que hay que revisar en cada versión de Keycloak | P8 | Propuesta |
| IMP-15 | 2026-10-29 | Aviso 2 minutos antes de que caduque la sesión de gestión (`role="alertdialog"` con «Seguir aquí»), usando `onTokenExpired` / refresco de `keycloak-js` | WCAG 2.2.1 (tiempo ajustable), recomendación 11 de los prototipos | Bajo | P6 | Propuesta |
| IMP-16 | 2026-10-29 | Auditoría automática de accesibilidad con `axe-core` + Playwright en cada pantalla (cuando se activen las pruebas) | RNF-1 comprobado de forma repetible | Requiere `ACTIVAR TESTS` | P8 | Propuesta |
| IMP-17 | 2026-10-29 | Con varias instancias de election, las tareas programadas (ciclo de vida y *outbox*) correrían en paralelo; usar `SELECT … FOR UPDATE SKIP LOCKED` o ShedLock | Escalado horizontal sin dobles envíos | Bajo; hoy hay una sola instancia y los receptores son idempotentes | P6 | Propuesta |
| IMP-18 | 2026-10-29 | Pantalla y endpoint para ver y reintentar los mensajes `FAILED` del *outbox* | Recuperación sin tocar la BD | Bajo | P6 | Propuesta |
| IMP-19 | 2026-10-29 | Al programar, bloquear la fila del censo (`SELECT … FOR UPDATE`) para que un alta simultánea no quede fuera de la instantánea | Elimina una carrera muy poco probable | Bajo | P6 | Propuesta |
