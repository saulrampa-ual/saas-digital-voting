# Preguntas previsibles del tribunal

Respuestas orientativas basadas en la memoria (cifras a 16/11/2026). Son para preparar, no para recitar: di la idea
con tus palabras y, si no sabes algo, dilo y explica cómo lo averiguarías.

Según el protocolo, pregunta primero el **secretario**, después el **vocal** y por último el **presidente**. La
rúbrica valora el trabajo (5 puntos), la memoria (2,5) y la presentación (2,5), y pide justificar cada nota, así que
suelen preguntar por decisiones, por la evaluación y por lo que falta.

Las diapositivas de reserva (después de «Gracias») sirven de apoyo: decisiones de arquitectura, arquitectura objetivo,
amenazas STRIDE y plan frente a ejecución.

---

## Problema y alcance

**¿Qué aporta VeriVoto frente a ElectionBuddy, POLYAS o Helios?**
No compite en criptografía con Helios ni en madurez con los comerciales. Aporta la combinación que faltaba para
votaciones internas: varias organizaciones en una plataforma, SSO institucional, flujo móvil accesible, API abierta y
una verificación que un votante no técnico entiende (5 de 5 en H1b).

**¿Por qué dejaste fuera sustituir el voto (RF-11)?**
Mitiga la coacción, pero complica el modelo de la urna (estados «sustituido», recibos que cambian) y su verificación.
Se decidió con el director (D-03, confirmado el 30/10/2026) priorizar un flujo de voto único correcto. El modelo
queda descrito en la especificación como trabajo futuro.

**¿Tiene validez legal una votación hecha con VeriVoto?**
El trabajo no lo afirma: es un prototipo para votaciones internas, evaluado en local. Su uso real dependería de los
estatutos o reglamentos de cada organización y de un despliegue con garantías que aquí no se ha hecho.

## Arquitectura y decisiones

**¿Por qué microservicios para un MVP? ¿No era más sencillo un monolito?**
La razón principal es el secreto: identidad (voter-access) y urna (ballot-box) deben estar separadas, con bases de
datos distintas. Con cinco servicios (D-05) esa separación es real y no solo lógica. Además encaja con mi experiencia
previa en microservicios. El coste es más complejidad de despliegue, que se nota en H1c.

**¿Por qué REST con outbox y no Kafka?**
Para un MVP local, Kafka añadía una pieza más que operar. La tabla outbox garantiza que un cambio se entrega aunque el
destino esté caído (reintento y entrega única, probado en integración). Si el volumen creciera, Kafka sería la
evolución natural.

**¿Por qué Keycloak y no una autenticación propia?**
No conviene implementar credenciales, segundo factor y federación a mano. Keycloak da contraseña más código por
correo, SSO por organización y tokens estándar. El votante, en cambio, entra con enlace y código propios de
voter-access, para no obligarle a tener cuenta.

**¿Cómo se aíslan las organizaciones (multi-tenant)?**
Esquema compartido con `organization_id` en cada entidad y filtro en cada consulta. Está probado por HTTP con dos
organizaciones: la otra organización recibe siempre 404, sin revelar que el recurso existe (RNF-15).

**¿Por qué arquitectura hexagonal?**
El dominio no depende de Spring, se prueba sin infraestructura y los adaptadores (Keycloak, PostgreSQL, REST) se
cambian sin tocar las reglas. ArchUnit comprueba esas reglas en cada servicio.

## Secreto, seguridad y auditoría

**¿Qué impide saber quién votó qué?**
Voter-access guarda quién participa, solo con el día; ballot-box guarda el voto sin identidad ni hora; entre ambos
solo pasa un token de un solo uso que no contiene identidad. No hay ningún identificador común entre las dos bases.

**¿Y si alguien cruza las horas de los registros (logs) de ambos servicios?**
Por eso la urna no guarda hora por voto y la participación se guarda por día. Aun así, un operador que observe el
tráfico de los dos servicios en tiempo real podría correlacionar: es un límite declarado en la memoria.

**¿Puede un administrador manipular la urna?**
Los eventos de la urna están encadenados con SHA-256 en una tabla en la que solo se puede añadir. Si se altera un
evento, la verificación falla y el escrutinio se bloquea. Borrar la base entera no se detectaría desde dentro: haría
falta publicar la huella final en un lugar externo, que es trabajo futuro.

**¿El recibo permite vender el voto?**
No: el recibo solo demuestra que un voto consta en la urna, no qué opción contiene. Por eso no sirve para probarle a
un tercero cómo se votó. La resistencia a la coacción en general sí queda fuera de alcance.

**¿Por qué dices que no es verificabilidad extremo a extremo?**
Porque el votante comprueba presencia, no que su voto se registró tal como lo emitió ni que se contó bien con pruebas
criptográficas. Eso exigiría cifrado en el cliente y pruebas como las de Helios (RF-23, fuera del MVP).

**¿Cómo se evita votar dos veces?**
El token es de un solo uso y lo consume ballot-box a través de voter-access; además hay una restricción única en la
urna. Con veinte envíos simultáneos del mismo token se registra un solo voto (prueba de integración).

**¿Qué amenazas analizaste?**
STRIDE sobre las interfaces y almacenes (capítulo 12). Ejemplos ya cubiertos: audiencia del JWT exigida (un token de
otro cliente no sirve), límite de peticiones por IP en el gateway, rutas internas solo para clientes de servicio
concretos y secretos fuera del repositorio (gitleaks en CI).

## Evaluación

**¿Cinco participantes son suficientes?**
Para detectar problemas de usabilidad, cinco es la cifra habitual en pruebas cualitativas. Pero no permite inferencia
estadística, y así lo presento: evidencia descriptiva limitada a ese prototipo y esa población.

**Los participantes eran conocidos tuyos, ¿no sesga el resultado?**
Es una amenaza a la validez y está declarada. Para reducirla: criterio fijado antes, sin ayuda durante la tarea,
sin formación previa en VeriVoto, sesiones grabadas y hoja de registro con tiempos.

**¿Por qué falló H1c y por qué no rebajaste el criterio?**
Porque un criterio que se ajusta después de medir deja de ser una hipótesis. Las causas probables son 500 conexiones
a la vez sin rampa, que el gateway rechazó en parte, y que los votos de un mismo proceso se guardan de uno en uno para
encadenarlos. Aun así no se perdió ni duplicó ningún voto.

**¿Cómo lo arreglarías?**
Primero medir dónde se va el tiempo (no está perfilado). Después: consumir el token antes del bloqueo para acortar la
sección crítica, ajustar las colas de conexión del gateway y los servicios, y repetir las tres ejecuciones en un
entorno dimensionado, con el mismo criterio.

**¿Por qué H1a queda «sin decisión» y no aceptada?**
El criterio exige la parte automática y una revisión manual con teclado y lector de pantalla. La automática da 0
incumplimientos críticos y serios en 212 comprobaciones, pero la manual no se pudo hacer (NVDA no funcionó en el
equipo). Aceptarla sin esa parte sería cambiar el criterio.

**¿Qué significa evaluar con ISO/IEC 25010?**
Cada requisito no funcional se asigna a una característica y subcaracterística del modelo (por ejemplo, autenticidad
dentro de seguridad) con una métrica y un umbral. La evaluación indica qué características tienen evidencia y cuáles
no; no es una certificación.

**La cobertura es baja (52 % y 42 %). ¿No es un problema?**
Es una limitación declarada. El código nuevo desde noviembre supera el 85 % y las reglas críticas (secreto, unicidad,
aislamiento, cadena) tienen pruebas específicas, incluidas de integración. La cobertura no demuestra corrección por sí
sola; lo prioritario fue cubrir lo que protege el voto.

## Proceso y aprendizaje

**¿Qué relación tiene con las asignaturas del máster?**
Arquitectura y diseño de software (hexagonal, microservicios), seguridad (modelo de amenazas, autenticación),
calidad (ISO/IEC 25010, pruebas, análisis estático) y gestión de proyectos (planificación, decisiones, riesgos). La
memoria detalla el encaje en la sección de encaje con el máster.

**¿Qué cambiarías si empezaras de nuevo?**
Haría la prueba de carga mucho antes, con una versión mínima del flujo de voto, para diseñar el registro de la urna
pensando en el rendimiento desde el principio. Y montaría el entorno de accesibilidad manual al inicio.

**¿Has usado herramientas de inteligencia artificial?**
Responde con la verdad y con precisión: qué usaste, para qué (código, pruebas, documentación) y cómo lo controlaste
(reglas en el repositorio, revisión de cada cambio, ejecución real de pruebas y mediciones, decisiones tomadas por ti).
**Antes de la defensa**, comprueba si la UAL o el máster exigen declarar ese uso en la memoria: ahora mismo la memoria
no incluye ninguna declaración.

**¿Cuánto costaría ponerlo en producción?**
No está estimado en la memoria. Lo honesto es decir qué haría falta: despliegue gestionado de los cinco servicios y
Keycloak, dos instancias PostgreSQL, correo transaccional, CSP y HSTS, gestión de secretos y la prueba de carga
repetida en ese entorno.

**¿Está desplegado? ¿Podemos probarlo?**
No: se evalúa en local con datos sintéticos (decisión D-53). El tutor tiene acceso al repositorio con una guía para
arrancarlo, y se puede hacer una demostración en vivo con la demo UAL.
