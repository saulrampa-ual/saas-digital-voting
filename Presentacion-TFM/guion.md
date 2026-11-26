# Guion de la defensa

Qué decir en cada diapositiva de `presentacion.pdf`. La exposición dura **15 minutos** (confirmado por el director);
el guion suma unos **13 minutos y medio** para dejar margen a los nervios y a algún paso de diapositiva lento.
Las cifras coinciden con la memoria a 17/11/2026: si cambian, actualízalas aquí y en `presentacion.tex`.

Consejo general: no leas la diapositiva. Di la idea en una frase, señala la figura y pasa a la siguiente.
Las diapositivas de **reserva** (después de «Gracias») no se presentan: sirven para responder preguntas
(H1c en detalle, más capturas, decisiones de arquitectura, arquitectura objetivo, STRIDE y plan).

---

## 1. Portada (25 s)

> Buenos días. Soy Saul David Ramos Pacheco y presento mi Trabajo Fin de Máster, VeriVoto: una plataforma SaaS de
> votación digital segura y auditable para organizaciones, dirigida por Javier Criado Rodríguez.

## 2. Problema y motivación (1 min)

> Las universidades y las asociaciones votan a menudo: representantes, juntas, consultas internas. Hoy se hace con
> papel, con formularios que no garantizan el secreto ni impiden votar dos veces, o con plataformas comerciales
> cerradas y de pago por elección.
> Lo que necesitan es fácil de enunciar y difícil de hacer bien: secreto, un voto por persona, una prueba que el
> votante entienda, un recuento auditable, uso desde el móvil y varias organizaciones en una misma plataforma.

## 3. Estado del arte (1 min)

> Hay tres familias. Las comerciales, como ElectionBuddy o POLYAS, funcionan, pero cobran por elección y explican
> poco su verificación. Las académicas, como Helios o Belenios, son muy sólidas en criptografía, pero un votante no
> técnico no entiende qué comprueba. Y las de participación ciudadana, como Decidim, están pensadas para procesos públicos.
> El hueco que aborda VeriVoto es juntar autenticación institucional, móvil accesible y una verificación comprensible,
> en un servicio multi-organización con API abierta.

## 4. Objetivos y preguntas (50 s)

> Los cuatro objetivos siguen el recorrido de una votación: la plataforma y sus organizaciones; el censo y el ciclo de
> vida; la participación con secreto y recibo; y el escrutinio con trazabilidad, evaluando la calidad con ISO/IEC 25010.
> De ahí salen tres preguntas: cómo hacer comprensible la verificación, qué separación protege el secreto y la
> unicidad, y cómo evaluar la calidad.

## 5. Metodología y alcance (45 s)

> He seguido Design Science Research: construir un artefacto que responda al problema y evaluarlo, con hitos, un
> diagrama de Gantt y un registro de decisiones. Los criterios de cada hipótesis se fijaron antes de medir y no se
> cambiaron después.
> El MVP cubre RF-01 a RF-21; quedan fuera, de acuerdo con el director, sustituir el voto y el cifrado en el cliente.
> Todo se ha evaluado en local, con datos sintéticos.

## 6. Arquitectura implementada (1 min 20 s)

> La aplicación web es una PWA en Angular e Ionic, con el flujo del votante y la consola de gestión. Detrás hay un
> gateway y cuatro servicios en Java con Spring Boot y arquitectura hexagonal: identity, election, voter-access y
> ballot-box. Keycloak gestiona la identidad de gestión y PostgreSQL guarda los datos. Entre servicios, REST y una
> tabla outbox que reintenta hasta entregar cada cambio.
> Son cinco servicios porque es la separación mínima que protege el secreto sin complicar el despliegue.

## 7. Secreto del voto (1 min 30 s)

> Esta es la idea central. Voter-access sabe quién ha participado, pero solo guarda el día. Ballot-box sabe qué se ha
> votado, pero no guarda identidad ni hora. Entre ambos pasa un token de un solo uso: el votante lo recibe al
> identificarse y ballot-box lo consume al registrar el voto.
> Con veinte envíos simultáneos del mismo token solo se registra un voto, y la urna está en una base de datos aparte.
> El límite, dicho con honestidad: no protege frente a quien controle a la vez las dos bases o el dispositivo.

## 8. Recibo y auditoría (1 min)

> Al votar, la persona recibe un recibo y comprueba en una página pública que su voto consta, sin que se vea su opción.
> Para la auditoría, cada evento de la urna se encadena con una huella SHA-256; si la cadena se rompe, el escrutinio se
> bloquea. El resultado se recalcula en el navegador y el acta lleva su huella.
> Esto acredita presencia e integridad, no verificabilidad extremo a extremo.

## 9. El prototipo (1 min)

> Así lo ve el votante en el móvil: la papeleta, el recibo con su código y la comprobación de que el voto está en la
> urna, que no dice qué se votó. A la derecha, el resultado en la consola tras el cierre: el recuento es automático y
> se acompaña del acta y de las huellas. El asistente de creación y el resto del recorrido están en la reserva.

## 10. Calidad y seguridad (1 min)

> Hay 437 pruebas automáticas sin fallos, 24 de ellas contra PostgreSQL real, y 212 comprobaciones de accesibilidad
> con axe sin incumplimientos críticos ni serios. El código nuevo supera el 85 % de cobertura en SonarQube, y la
> integración continua ejecuta Semgrep, Trivy y gitleaks.
> La cobertura de líneas es del 88 % en el frontend; la del backend, 60 %, es una limitación que declaro.
> Además cronometré 16 acciones que llaman al servidor: la primera señal en pantalla llega en menos de 40 milisegundos
> y todas terminan en menos de un segundo, que es lo que pide RNF-8.

## 11. Hipótesis: resultados (1 min 30 s)

> Tres evaluaciones se aceptan. Cinco votantes no técnicos votaron y obtuvieron su recibo en menos de tres minutos,
> con una mediana de 51 segundos, y los cinco entendieron qué significaba. Cinco administradores sin formación previa
> crearon y programaron una votación en cuatro minutos como máximo, frente a los diez del criterio.
> La accesibilidad del flujo del votante también se acepta: axe no da incumplimientos críticos ni serios y, en la
> revisión con el Narrador de Windows, no hubo bloqueos; el contraste con foco de los botones verdes estaba por debajo
> del mínimo, lo corregí y lo volví a medir, y las dos incidencias menores que salieron las corregí y las comprobé de
> nuevo con el lector. Y el rendimiento se rechaza en las tres versiones que medí: con 500 usuarios a la vez no se
> llega a los 2 segundos, aunque no se perdió ni duplicó ningún voto. Ampliar las colas y guardar los votos por grupos
> no cambió el ritmo, de unos 50 votos por segundo, así que el límite está en la cadena de servicios.
> Con cinco participantes, la evidencia es descriptiva, no estadística.

## 12. Conclusiones (1 min)

> Un recibo de presencia permite explicar la verificación sin revelar el voto, y los cinco participantes lo
> entendieron. Separar participación y urna con un token de un solo uso controla la unicidad y reduce la vinculación.
> E ISO/IEC 25010 organiza la evaluación, aunque la evidencia queda incompleta en rendimiento.
> Los cuatro objetivos y los cinco objetivos específicos del anteproyecto tienen evidencia; la aceptación experimental
> es parcial y así lo declaro.

## 13. Limitaciones y trabajo futuro (45 s)

> Las limitaciones principales: solo en local, el rendimiento y una accesibilidad revisada con un solo lector de
> pantalla. Como trabajo futuro: optimizar y repetir la prueba de carga, repetir la revisión con NVDA y con personas
> usuarias de lectores de pantalla, permitir cambiar el voto y
> avanzar hacia la verificabilidad extremo a extremo y un despliegue real.

## 14. Gracias (10 s)

> Muchas gracias. Quedo a su disposición para las preguntas.

---

## Tiempos

| Bloque | Diapositivas | Tiempo |
| --- | --- | --- |
| Apertura y problema | 1–3 | 2:25 |
| Objetivos y metodología | 4–5 | 1:35 |
| Arquitectura, secreto y auditoría | 6–8 | 3:50 |
| Prototipo y calidad | 9–10 | 2:00 |
| Evaluación | 11 | 1:30 |
| Cierre | 12–14 | 1:55 |
| **Total** | | **≈ 13:15** |

**Si vas justo de tiempo:** en la 3 di solo la frase del hueco, en la 5 solo «Design Science Research con criterios
fijados antes de medir» y en la 9 solo la frase de la izquierda. Ahorras cerca de un minuto.

**Ensayo:** cronométrate al menos dos veces con el PDF definitivo. Si pasas de 14 minutos, recorta primero la 3 y la 9.

**Si piden demostración en vivo:** ten arrancado el stack con la demo UAL (`node scripts/demo/seed-ual.mjs`), una
pestaña en la consola y otra en un enlace de votante. Si algo falla, vuelve a las capturas: «el entorno local a veces
tarda; aquí tienen la misma pantalla».
