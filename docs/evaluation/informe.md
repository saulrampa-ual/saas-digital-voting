# Informe de evaluación P7

> Estado: **parcial; resultados pendientes**. Este informe recoge solo evidencias obtenidas en ejecuciones reales.
> No sustituye los informes de accesibilidad, carga ni las sesiones moderadas pendientes.

## Entorno y reproducibilidad

| Elemento | Configuración / evidencia |
| --- | --- |
| Commit evaluado | Rama `chore/p7-p8-tfm-work`, base `5fa854f`; cambios sin commit |
| Sistema operativo y hardware | Windows; hardware exacto PENDIENTE |
| Java / Maven / PostgreSQL / Docker | Temurin 25.0.5; Maven Wrapper del proyecto; PostgreSQL 18.6 en contenedores Testcontainers; Docker Desktop 29.4.0 |
| Configuración de servicios | PENDIENTE |
| Ejecución backend | `backend`: `mvnw --batch-mode --no-transfer-progress verify`; BUILD SUCCESS; 49 pruebas, 0 fallos/errores |
| Datos | Sintéticos; no registrar ni publicar identidades, códigos, tokens o recibos completos |

## Hipótesis

| Hipótesis | Método y criterio | Resultado real | Decisión |
| --- | --- | --- | --- |
| H1a: accesibilidad del flujo | Playwright + axe en las pantallas evaluadas: cero incumplimientos críticos ni serios, más revisión manual con teclado y lector de pantalla sin bloqueos en acceso, voto y recibo. axe no certifica WCAG; un incumplimiento serio puntual se documenta como incidencia y se decide si se corrige antes del cierre (criterio validado por el tutor). | PENDIENTE: flujo E2E y barrido axe no ejecutados. | Pendiente |
| H1b: usabilidad | 5 personas no técnicas: las 5 completan acceso, emisión y recibo en ≤3 min y al menos 4 de 5 interpretan la verificación (criterio validado por el tutor; con n = 5, evidencia descriptiva sin inferencia estadística). Materiales en `docs/evaluation/usabilidad/`. | PENDIENTE: las sesiones las realiza el autor. | Pendiente |
| H1c: rendimiento | 500 emisores simultáneos; p95 de emisión <2 s; tres ejecuciones reproducibles y errores/recursos documentados. | PENDIENTE: carga no ejecutada. | Pendiente |

## Evidencia técnica por servicio

| Servicio | Evidencia | Resultado |
| --- | --- | --- |
| api-gateway | 1 prueba ArchUnit | 1 pasada; cobertura de líneas 0,0 % (0/3) |
| identity-service | Tests unitarios/arquitectura + `IdentityPostgresIntegrationTest` | 12 pasadas; migraciones Flyway y trigger de acciones administrativas solo añadibles comprobados |
| election-service | Tests unitarios/arquitectura + `ElectionPostgresIntegrationTest` | 10 pasadas; Flyway 6 migraciones y aislamiento de censos probado a través de `CensusRepositoryAdapter`/JPA para listar y buscar por organización |
| voter-access-service | Tests unitarios/arquitectura + `VoterAccessPostgresIntegrationTest` | 9 pasadas; migraciones y participación con fecha SQL, sin columnas de hora, comprobadas |
| ballot-box-service | Tests unitarios/arquitectura + `BallotBoxPostgresIntegrationTest` | 17 pasadas; migraciones, esquema sin identificadores/tiempos de identidad y rechazo de evento `VOTE_RECEIVED` con hora comprobados |
| frontend | Vitest: `role.guard.spec.ts`, `voter-api.service.spec.ts`, `voter-sso.service.spec.ts` | 10 pasadas; no se ha medido cobertura |

Backend: 49 pruebas, 0 fallos y 0 errores en la última ejecución completa `verify`. Frontend: 10 pruebas, 0 fallos;
`npm run test:ci` y `npm run build` completaron correctamente. Total conjunto: 59 pruebas. Informes JaCoCo:
`backend/services/<servicio>/target/site/jacoco/index.html`.
Las cifras JaCoCo de la tabla reflejan el informe inicial de cobertura previo a las cuatro pruebas de integración; el gateway no contiene lógica ejercitada en estas pruebas. No se recalculan porcentajes sin una extracción homogénea posterior.
No se asigna umbral de cobertura en esta fase.

## RNF e ISO/IEC 25010:2023

| RNF | Característica / subcaracterística (mapeo de `rnf_iso25010.tex`) | Evidencia prevista | Estado |
| --- | --- | --- | --- |
| RNF-1 | Capacidad de interacción; Inclusivity, User assistance | axe, revisión manual con teclado, foco, lector de pantalla, zoom y contraste | Pendiente |
| RNF-3 | Capacidad de interacción; Learnability, Operability | Sesiones moderadas H1b | Pendiente |
| RNF-4 | Capacidad de interacción; Learnability, Operability | Sesión con 5 administradores: al menos 4 de 5 crean, configuran y programan un proceso de opción única en ≤10 min (valor propuesto) y sin ayuda; sin hipótesis asociada; sesión no realizada | Pendiente |
| RNF-6 | Capacidad de interacción; Self-descriptiveness | Pregunta de comprensión del recibo | Pendiente |
| RNF-11 | Seguridad; Confidentiality | Revisión de esquema, logs, interfaces y separación de bases | Parcial: `VotingServiceTest` comprueba token consumido por separado, evento sin hora y hash del recibo; esquema, logs y persistencia siguen pendientes |
| RNF-12 | Seguridad y fiabilidad; Integrity, Faultlessness | Envíos concurrentes con token único en PostgreSQL | Pendiente |
| RNF-13 | Seguridad; Integrity, Accountability, Non-repudiation | Alteraciones de eventos, verificación de cadena y registros solo añadibles | Parcial: pruebas de dominio en curso |
| RNF-14 | Seguridad; Authenticity, Resistance, Confidentiality | Límites de código, bloqueo, cuenta institucional, permisos y claves API | Pendiente |
| RNF-15 | Seguridad; Confidentiality | Dos organizaciones en pruebas de repositorio y autorización | Parcial: reglas de Caller, acceso a persistencia pendiente |
| RNF-17 | Eficiencia de desempeño; Time behaviour, Capacity | k6: emisión, acceso y escrutinio; tres ejecuciones | Pendiente |
| RNF-18 | Flexibilidad; Installability, Adaptability | Instalación PWA y comprobación en tamaños/navegadores | Pendiente |

La correspondencia resume el mapeo del TFM; no constituye certificación de conformidad con la norma.

## Limitaciones y amenazas a la validez

Las pruebas unitarias cubren reglas aisladas. Las cuatro pruebas de integración ejecutan migraciones y comprobaciones concretas en PostgreSQL 18.6. La prueba de election confirma el alcance por organización de las operaciones de lectura del repositorio de censos, pero no cubre autorización HTTP ni todos los repositorios/entidades. Tampoco demuestran secreto del voto de extremo a extremo ni comportamiento bajo carga. La accesibilidad automatizada no sustituye la evaluación manual.
La usabilidad depende de una muestra pequeña y autoseleccionada. Los resultados de carga dependen del hardware,
contenedores, configuración y preparación de datos; se deben informar sin generalizarlos a producción.

## Pendiente de completar

- Ampliar integración a otros repositorios y autorización HTTP; probar concurrencia y flujos HTTP/outbox.
- E2E con Keycloak y Mailpit reales en 1280 px y 360 px, idiomas ES/EN y axe en flujos acordados.
- Tres mediciones k6 con datos sintéticos, percentiles, errores, CPU/RAM y comprobación posterior de cadena y participación.
- Sesiones moderadas, resultados agregados y cobertura JaCoCo por servicio.
- Actualizar decisiones de H1a/H1b/H1c y trazabilidad solo cuando se disponga de dichas evidencias.
