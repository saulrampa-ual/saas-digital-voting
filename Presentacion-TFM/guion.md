# Guion de la defensa

Qué decir en cada diapositiva de `presentacion.pdf`. Está pensado para unos **18 minutos** de exposición; el
protocolo del tribunal no fija la duración, así que confírmala con el director y recorta según la tabla del final.
Las cifras coinciden con la memoria a 16/11/2026: si cambian, actualízalas aquí y en `presentacion.tex`.

Consejo general: no leas la diapositiva. Di la idea en una frase, señala la figura y pasa a la siguiente.
Las diapositivas de **reserva** (después de «Gracias») no se presentan: sirven para responder preguntas.

---

## 1. Portada (30 s)

> Buenos días. Soy Saul David Ramos Pacheco y presento mi Trabajo Fin de Máster, VeriVoto: una plataforma SaaS de
> votación digital segura y auditable para organizaciones, dirigida por Javier Criado Rodríguez.

## 2. Contenido (20 s)

> Primero explicaré el problema y lo que ya existe; después, los objetivos, cómo lo he abordado y la arquitectura.
> Mostraré el prototipo, cómo lo he evaluado y terminaré con las conclusiones y lo que queda por hacer.
> Una aclaración desde el principio: todo se ha ejecutado en local con datos sintéticos, no hay despliegue en producción.

## 3. Problema y motivación (1 min 15 s)

> Las universidades y las asociaciones votan a menudo: representantes de estudiantes, juntas, consultas internas.
> Hoy se hace con papel, con formularios genéricos que no garantizan el secreto ni impiden votar dos veces, o con
> plataformas comerciales cerradas y de pago por elección.
> Lo que necesitan es sencillo de enunciar y difícil de hacer bien: secreto, un voto por persona, una prueba que el
> votante entienda, un recuento auditable, uso desde el móvil y que varias organizaciones compartan la plataforma.

## 4. Estado del arte (1 min 15 s)

> Revisé tres familias. Las soluciones comerciales, como ElectionBuddy o POLYAS, funcionan, pero cobran por elección
> y su verificación se explica poco. Las académicas verificables, como Helios o Belenios, son muy sólidas en
> criptografía, pero un votante no técnico no entiende qué está comprobando. Y las de participación ciudadana, como
> Decidim, están pensadas para procesos públicos.
> El hueco que aborda VeriVoto es combinar autenticación institucional, experiencia móvil accesible y una verificación
> comprensible, en un servicio multi-organización con API abierta.

## 5. Objetivos y preguntas (1 min)

> Los cuatro objetivos siguen el recorrido de una votación: la plataforma y sus organizaciones; el censo y el ciclo de
> vida; la participación con secreto y recibo; y el escrutinio con trazabilidad, evaluando la calidad con ISO/IEC 25010.
> De ahí salen tres preguntas: cómo hacer comprensible la verificación, qué separación protege el secreto y la
> unicidad, y cómo evaluar la calidad del producto.

## 6. Metodología y alcance (1 min)

> He seguido Design Science Research: construir un artefacto que responda al problema y evaluarlo. El trabajo se
> organizó en hitos con un diagrama de Gantt, prioridades MoSCoW y un registro de decisiones.
> Un punto que quiero destacar: los criterios de cada hipótesis se fijaron antes de medir y no se cambiaron después.
> El MVP cubre los requisitos RF-01 a RF-21. Quedaron fuera, de acuerdo con el director, sustituir el voto, los
> métodos de votación avanzados y el cifrado en el cliente.

## 7. Arquitectura implementada (1 min 30 s)

> La aplicación web, en Angular e Ionic, es una PWA con dos partes: el flujo del votante y la consola de gestión.
> Detrás hay un gateway y cuatro servicios en Java con Spring Boot y arquitectura hexagonal: identity, election,
> voter-access y ballot-box. Keycloak gestiona la identidad de los usuarios de gestión y PostgreSQL guarda los datos.
> Los servicios se comunican por REST y, cuando hay que propagar un cambio, con una tabla outbox que reintenta hasta
> entregarlo. Elegí cinco servicios porque es la separación mínima que protege el secreto sin complicar el despliegue.

## 8. Secreto del voto (1 min 30 s)

> Esta es la idea central. Voter-access sabe quién ha participado, pero solo guarda el día. Ballot-box sabe qué se ha
> votado, pero no guarda ni la identidad ni la hora. Entre ambos pasa un token de un solo uso: el votante lo recibe al
> identificarse y ballot-box lo consume al registrar el voto.
> Lo he probado con veinte envíos simultáneos del mismo token y solo se registra un voto. La urna además está en una
> base de datos aparte.
> Lo digo con honestidad: esto no protege frente a alguien que controle a la vez las dos bases o el dispositivo del votante.

## 9. Recibo y auditoría (1 min 15 s)

> Al votar, la persona recibe un recibo y puede comprobar en una página pública que su voto consta en la urna, sin
> que se muestre su opción. Para la auditoría, cada evento de la urna se encadena con una huella SHA-256; si la cadena
> se rompe, el escrutinio se bloquea. El resultado se puede recalcular en el navegador y el acta lleva su huella.
> Esto acredita presencia e integridad. No es verificabilidad extremo a extremo, que queda como trabajo futuro.

## 10. El prototipo: consola (1 min)

> A la izquierda, el asistente para crear una votación en cuatro pasos. A la derecha, el panel de participación, que
> solo muestra cifras agregadas y por día, nunca quién ha votado.
> (Si hay tiempo y el tribunal lo pide, aquí se puede hacer la demostración en vivo con la demo UAL.)

## 11. Resultados y trazabilidad (45 s)

> Tras el cierre, el recuento es automático: el resultado, el acta en PDF y las huellas. Y cada acción administrativa
> queda registrada en un registro en el que solo se puede añadir.

## 12. Calidad y seguridad (1 min 15 s)

> Hay 295 pruebas automáticas sin fallos, 16 de ellas contra una base PostgreSQL real, y 212 comprobaciones de
> accesibilidad con axe sin incumplimientos críticos ni serios. El código nuevo supera el 85 % de cobertura en SonarQube.
> La integración continua ejecuta además análisis de seguridad: Semgrep, Trivy y gitleaks.
> Y una limitación que también está en la memoria: la cobertura global de líneas es del 52 % en el backend y del 42 %
> en el frontend.

## 13. Hipótesis: resultados (1 min 30 s)

> De las cuatro evaluaciones, dos se aceptan. Cinco votantes no técnicos votaron y obtuvieron su recibo en menos de
> tres minutos, con una mediana de 51 segundos, y los cinco entendieron qué significaba. Cinco administradores sin
> formación previa crearon y programaron una votación en cuatro minutos como máximo, frente a los diez del criterio.
> La accesibilidad queda sin decisión: la parte automática es favorable, pero no pude hacer la revisión manual con
> lector de pantalla. Y el rendimiento se rechaza, como explico ahora.
> Con cinco participantes, esto es evidencia descriptiva, no estadística.

## 14. H1c: rendimiento (1 min 15 s)

> Con 500 usuarios votando a la vez, el p95 fue de entre 2,4 y 4,9 segundos, por encima de los 2 del criterio, y
> aproximadamente la mitad de las conexiones las rechazó el gateway. Aun así, en las tres ejecuciones la cadena quedó
> íntegra y el recuento coincidía con los votos aceptados: el sistema fue lento, pero no perdió ni duplicó votos.
> La causa más probable es que los votos de un mismo proceso se guardan de uno en uno para encadenarlos. El siguiente
> paso es perfilarlo y repetir la medición en un entorno dimensionado, sin rebajar el criterio.

## 15. Conclusiones (1 min 15 s)

> Respondiendo a las preguntas: un recibo de presencia permite explicar la verificación sin revelar el voto, y los
> cinco participantes lo entendieron. Separar participación y urna con un token de un solo uso controla la unicidad
> y reduce la vinculación. E ISO/IEC 25010 sirve para organizar la evaluación, aunque la evidencia queda incompleta en
> rendimiento y en la accesibilidad manual.
> Los cuatro objetivos están implementados; la aceptación experimental es parcial y así lo declaro.

## 16. Limitaciones y trabajo futuro (1 min)

> Las limitaciones principales son que solo se ha probado en local, el rendimiento y la revisión de accesibilidad
> manual. Como trabajo futuro: optimizar y repetir la prueba de carga, la revisión con lector de pantalla, permitir
> cambiar el voto y avanzar hacia la verificabilidad extremo a extremo y un despliegue real.

## 17. Gracias (10 s)

> Muchas gracias. Quedo a su disposición para las preguntas.

---

## Tiempos

| Bloque | Diapositivas | Tiempo |
| --- | --- | --- |
| Apertura | 1–2 | 0:50 |
| Problema y estado del arte | 3–4 | 2:30 |
| Objetivos y metodología | 5–6 | 2:00 |
| Arquitectura y secreto | 7–9 | 4:15 |
| Prototipo | 10–11 | 1:45 |
| Evaluación | 12–14 | 4:00 |
| Cierre | 15–17 | 2:25 |
| **Total** | | **≈ 17:45** |

**Si tienes 15 minutos:** junta 10 y 11 en una sola frase («la consola permite… y el recuento es automático»),
acorta 4 a la frase del hueco y salta 6 diciendo solo «Design Science Research con criterios fijados antes de medir».

**Si tienes 10 minutos:** presenta 1, 3, 5, 7, 8, 13, 14 y 15.

**Si piden demostración en vivo:** ten arrancado el stack con la demo UAL (`node scripts/demo/seed-ual.mjs`) y una
pestaña abierta en la consola y otra en un enlace de votante. Si algo falla, vuelve a las capturas sin disculparte
demasiado: «el entorno local a veces tarda; aquí tienen la misma pantalla».
