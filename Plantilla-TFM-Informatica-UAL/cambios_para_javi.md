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
