# Cambios preparados para la revisión de Javier

**Base de comparación:** `referencia-original.pdf` (PDF anterior de 69 páginas),
la especificación Word `Proyecto_plataforma_SaaS_de_votacion_digital.docx` y
las dos imágenes de referencia facilitadas por el autor.

## Cambios incorporados

- **Autenticación (RF-04/RF-05/RF-08):** la memoria describe ahora el inicio de
  sesión de forma genérica. OIDC/SSO se conserva como alternativa de diseño y
  el proveedor de identidad como integración opcional; no se exige una cuenta
  institucional. Se actualizaron la especificación detallada, los actores,
  el glosario, los objetivos y la justificación para mantener esa decisión
  coherente.
- **Figura 5.1:** se reorganizó como arquitectura SaaS multi-tenant orientada a
  microservicios, siguiendo la referencia compartida: clientes, CDN/WAF/API
  gateway, servicios de negocio separados, proveedor de identidad opcional,
  notificaciones, KMS opcional, cola y workers, y persistencia diferenciada.
  Los servicios muestran su correspondencia con los RF, incluido RF-20; RF-21
  se identifica en la API REST/OpenAPI.
- **Figura 5.2 y §5.3:** se ajustaron los participantes y el flujo numerado a
  las imágenes compartidas: enlace y OTP, token de un uso, voto cifrado,
  registro separado de participación, anexado a la urna, evento de auditoría,
  recibo y verificación en tres pasos. La nota aclara la separación entre
  censo y urna y distingue RF-10 de la comprobación de cadena de RF-12. También
  se actualizó la fuente PlantUML del listado A.3.
- **Casos de uso (capítulo 8):** se mantiene un actor genérico para las
  funcionalidades compartidas y se usa herencia de actores para los perfiles
  especializados. Las asociaciones actor-caso de uso son líneas sin flecha;
  RF-10 y RF-11 extienden RF-09 y el escrutinio obligatorio al cierre se incluye
  desde RF-07.
- **Calidad:** se conserva la correspondencia de los RNF con ISO/IEC
  25010:2023, incluida autenticidad para seguridad de acceso. Se redujo el
  ancho de la tabla para corregir el desbordamiento horizontal.
- **Trazabilidad:** se mantiene el desglose de RF-05 en RF-05a/RF-05b y los RF-20
  y RF-21 en el catálogo y la trazabilidad. Las prioridades y el alcance que
  aún requieren validación siguen identificados como provisionales.

## Verificación

`main.tex` se compiló con pdfLaTeX en 69 páginas A4. La pasada final no contiene
errores LaTeX fatales ni referencias o citas indefinidas. Se inspeccionaron
visualmente las figuras 5.1 y 5.2, los diagramas 8.1 y 8.2 y la tabla de
correspondencia ISO. El registro de compilación aún muestra algunos avisos de
maquetación en la portada/contraportada y en elementos de texto estrechos; no
impiden generar el PDF y deben revisarse si se busca una versión final de
impresión.

## Diferencias respecto al material anterior

La especificación Word mantiene RF-05 como SSO institucional. A petición del
tutor, la memoria revisada formula RF-05 como inicio de sesión genérico y deja
la federación OIDC/SSO como opción de diseño. El resto de los cambios de esta
lista se incorporaron a la memoria LaTeX y al PDF de revisión; no se modificó
el Word original.

## Actualización de P8 (borrador del 31/10/2026)

- La memoria incorpora capítulos preliminares de arquitectura implementada,
  implementación, amenazas, evaluación y conclusiones. Resumen y abstract
  informan la batería registrada (49 pruebas backend, 10 frontend) y dejan
  H1a/H1b/H1c pendientes. La cobertura JaCoCo indicada se limita a las cifras
  registradas en el informe P7.
- RF-05b se concreta en el texto de implementación como selección por proceso
  entre código por correo y cuenta institucional (D-48). La figura de
  componentes existente se identifica ahora como arquitectura objetivo; la
  arquitectura real se describe aparte.
- El Gantt distingue fechas previstas y estado conocido al corte. No se
  reconstruyen duraciones diarias no registradas. D-03 (revoto) y D-05
  (consolidación en cinco servicios) se mantienen abiertos para el tutor.
- La batería backend completa (`mvnw verify`, Java 25.0.5) pasó 49 pruebas.
  Cuatro pruebas Testcontainers ejecutaron migraciones y validaciones acotadas
  de esquema en PostgreSQL 18.6 para identity, election, voter-access y
  ballot-box. La prueba de election valida el scope de censos mediante el
  adaptador JPA; autorización HTTP y otros repositorios siguen pendientes.
- La compilación técnica llegó a generar el PDF, pero aún conserva avisos
  previos de maquetación, tablas y portada/contraportada. La inspección visual
  y la revisión editorial completa de este borrador quedan pendientes; no es
  todavía una versión lista para entregar.

## Aplicación de los criterios confirmados por el tutor (31/10/2026)

- **H1a / RNF-1 (1.8, 6.3.1, tabla 13.1):** criterio único: cero incumplimientos críticos ni serios en las
  pantallas evaluadas, más revisión manual con teclado y lector de pantalla sin bloqueos en acceso, voto y
  recibo. La auditoría automática no certifica WCAG; un incumplimiento serio puntual se documenta como
  incidencia; el criterio no se relaja a priori.
- **H1b / RNF-3 / RNF-6:** sin porcentajes: los 5 participantes completan en ≤3 minutos y al menos 4 de 5
  interpretan bien la verificación; H0b reformulada; nota de n = 5 sin inferencia estadística.
- **RNF-4:** al menos 4 de 5 administradores en ≤10 minutos (valor propuesto) y sin ayuda; sin hipótesis
  asociada y declarado PENDIENTE en 1.8, tabla 13.1 y 14.3.
- **Tabla 13.1:** separa criterio del protocolo, estado del criterio y resultado medido (todo PENDIENTE).
- **D-03 y D-05:** confirmadas por el tutor por correo (30/10/2026) en 1.5, 10.1, 10.5, apéndice A,
  6.1.11 y 14.3. Nota de alcance sobre revoto al inicio del capítulo 6; tabla 1.1 y actor Votante anotados;
  RF-11 y su «extend» retirados de la figura 8.2 y del listado A.2.
- **Listado A.3:** alineado con la figura 5.2; desaparecen «voto cifrado» y «Urna cifrada» (el voto no se
  cifra en cliente).
- **14.2:** remite a 14.3 para los protocolos pendientes.
