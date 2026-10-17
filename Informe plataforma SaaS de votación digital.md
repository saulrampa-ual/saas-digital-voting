## Universidad de Almería

## FACULTAD DE INGENIERÍA

## Plataforma SaaS de votación

digital

## Estado del arte, análisis DAFO y requisitos funcionales

## Realizado por:

Saul David Ramos Pacheco

## Profesor:

Javier Criado Rodríguez


## Contenido


## Introducción

Este informe responde a la petición del tutor sobre la idea elegida para el TFM: una plataforma web SaaS para crear procesos de votación, gestionar participantes, emitir votos de forma segura y mostrar resultados al cierre, dirigida a universidades, asociaciones, organizaciones y comunidades. Se pidió: (1) una revisión lo más extensa posible de las aplicaciones y trabajos similares; (2) un análisis DAFO (debilidades, amenazas, fortalezas, oportunidades); y (3) como cierre, un conjunto

de requisitos funcionales derivados de las oportunidades detectadas.

Metodología. Revisión documental a fecha 30-sep-2026: webs oficiales de los fabricantes, documentación técnica y repositorios, comparativas de software, prensa y literatura académica sobre voto electrónico. No se han probado las plataformas; los datos de funciones y precios

proceden de fuentes públicas.

Limitaciones. Varias comparativas online son de sitios de reseñas de calidad desigual o de fabricantes que compiten entre sí (por ejemplo, la comparativa de vote.direct la publica la propia

empresa, y lo declara). Por eso los datos de la sección 3 deben contrastarse con las webs oficiales o con pruebas reales antes de darse por definitivos en la memoria.

## Marco: qué debe garantizar un sistema de votación electrónica

Antes de comparar productos conviene fijar los criterios con los que se juzgan. La literatura sobre voto electrónico coincide en un conjunto de propiedades:

- Elegibilidad y unicidad: solo votan quienes están en el censo y solo una vez.

- Secreto del voto: nadie puede vincular un voto con quien lo emitió.

- Verificabilidad extrema a extremo: se suele dividir en individual (cada votante comprueba que su voto se contó), universal (cualquiera comprueba que el resultado corresponde a la urna) y de elegibilidad (los votos proceden de votantes legítimos), como explican los autores de Belenios.

- Resistencia a la coacción y a la compra de votos: es la propiedad más difícil de lograr en voto remoto no supervisado.

- Disponibilidad, usabilidad y accesibilidad: el sistema debe aguantar el pico de cierre y ser usable por cualquier votante.

## Estado del arte: aplicaciones existentes

## Criterios de comparación

Modelo (SaaS, código abierto o autoalojado), verificabilidad criptográfica, método de identificación del votante, tipos de votación admitidos, API e integraciones, experiencia móvil, certificaciones, precio y mercado principal.

SaaS comerciales generalistas

- ElectionBuddy (desde 2012). Muy extendida en sindicatos, asociaciones y universidades; admite elecciones de más de 10.000 votantes, notifica por email, SMS y correo postal, y


- ofrece soporte telefónico. Limitaciones señaladas: la identificación llega hasta email y códigos de acceso, no tenía API pública para crear elecciones (agosto 2026) y la votación en directo es un producto aparte (MeetingVote). Precio publicado: gratis hasta 20 votantes, 29 \$ hasta 350, 99 \$ hasta 1.000 y desde 299 \$ por encima.

- Simply Voting (Canadá). Orientada a sindicatos, asociaciones y universidades, con elecciones bilingües inglés/francés e informes de auditoría. Precio solo bajo presupuesto, sin API y verificación basada en códigos de acceso.

- OpaVote. Especializada en voto preferencial (IRV, STV, Condorcet, Borda). Usa enlaces únicos por email: si alguien lo reenvía, otra persona puede votar. Sin API. Gratis hasta 25 votantes y 10 candidatos; después, 10 \$ por cada 125 votantes.

- vote.direct. Entrante reciente con verificación por email y SMS, identificación oficial opcional (3 \$ por votante verificado), voto ponderado, widgets embebibles, papeletas por correo y precio por elección desde 4,99 \$. Declara tener API. Es un actor pequeño con pocas reseñas independientes.

- Google Forms / SurveyMonkey. No son sistemas de votación, pero son el «competidor de facto» en asociaciones pequeñas: no verifican identidad, no garantizan secreto y no dejan pista de auditoría.

## Proveedores europeos, certificación y mercado hispano

- POLYAS (Alemania). Su sistema CORE 2.5 obtuvo la certificación BSI Common Criteria (BSI- DSZ-CC-0862-2016), vigente hasta junio de 2026 según su web, con una nueva certificación bajo el perfil BSI-CC-PP-0121-2024 prevista para el verano de 2026. CORE 3 añade verificabilidad individual y universal. Paquete estándar gratis hasta 50 votantes; los paquetes verificable y certificado parten de 50 €. Ofrece votación en directo para asambleas y combinación con voto por correo o presencial. Los precios completos no son públicos.

- Scytl. Spin-off de la UAB y pionera del voto electrónico en Europa; su plataforma de e-voto la usan las universidades públicas catalanas (ACUP/CSUC), y la UAB la emplea como único sistema de votación en varios procesos, incluidas elecciones sindicales en junio de 2026. Contrapesos: en 2019 investigadores de varios países reportaron fallos críticos de verificabilidad en el sistema suizo que desarrollaba, y la prensa ha informado de su liquidación.

- NemoVote (España). Votación para asociaciones, partidos políticos y sindicatos en formato presencial, híbrido y remoto, con cifrado y resultados en tiempo real.

- Eligo (España). Plataforma de voto por internet y electrónico para universidades, empresas, asociaciones y órdenes religiosas; afirma operar en más de 75 países.

## Código abierto y proyectos académicos

- Helios. Referente académico de voto verificable con criptografía (auditoría abierta); usado en elecciones universitarias reales. Limitaciones según las comparativas: interfaz anticuada, sin optimización móvil, identificación solo por email, sin API y desarrollo poco activo. No existe una oferta SaaS comercial con soporte.

- Belenios (Inria, CNRS, Université de Lorraine; AGPLv3). Garantiza privacidad y verificabilidad incluso frente a un servidor comprometido; permite repartir la clave de descifrado entre varias autoridades (umbral); soporta Condorcet-Schulze y STV; incorpora mixnets. Permite iniciar sesión con Google o con el CAS de Inria. Lo usa el CNRS para más de 30 elecciones al año y Inria en sus comités; se han organizado unos 7.000 procesos. Cumple el nivel 2 de las recomendaciones de la CNIL y hay investigación abierta sobre «cast-as-intended»


- (BeleniosCaI). Limitaciones: es un servicio de investigación más que un SaaS multi- organización con planes; no se ha encontrado una API pública de integración.

- Vocdoni (España). Protocolo abierto con blockchain propia (Vochain), anonimato mediante zk-SNARK y prevención de voto múltiple con nullifiers. Ofrece SDK en TypeScript, API SaaS y componentes React para integradores; en noviembre de 2025 lanzó una aplicación web con plan gratuito y periodos de prueba. Según su comunicación, ha sido usado por cientos de organizaciones y en un referéndum en Bellpuig, y su responsable de producto cifra su capacidad en 40.000 votos por minuto. Limitación potencial: complejidad y recelo social hacia soluciones «blockchain».

- Decidim + Bulletin Board (ElectionGuard). Decidim es la plataforma abierta de participación ciudadana usada por Barcelona, Helsinki, Nueva York o la Generalitat de Catalunya. Incorpora un módulo de voto electrónico basado en el protocolo ElectionGuard de Microsoft con «Bulletin Board», con votaciones online, presenciales o híbridas, y una integración con Vocdoni. Valoración propia: desplegar una plataforma tan amplia solo para votar puede ser desproporcionado para una asociación pequeña.

## Referentes gubernamentales: Estonia y Suiza

- i-Voting de Estonia. Primer país en ofrecer voto por internet a escala nacional (2005); en las elecciones parlamentarias de 2023 el 51 % de los votos se emitió online. Se apoya en la identidad digital nacional (e-ID), permite votar varias veces (cuenta el último voto), deja anular el voto online votando en papel y separa la identidad del voto antes del recuento. Limitaciones para nuestro caso: solo funciona porque existe una e-ID a escala de país, y el análisis de Springall et al. (CCS 2014) señaló limitaciones de arquitectura, carencias de procedimiento y poca transparencia. Lección: la identidad fuerte es lo que hace viable el modelo; una asociación o universidad sin e-ID necesita alternativas como el SSO institucional.

- Swiss Post / Scytl (Suiza). Sistema pensado para ofrecer verificabilidad completa en elecciones federales y en proceso de certificación para hasta el 100 % del electorado de los cantones participantes. En 2019 Lewis, Pereira y Teague, y después Haines et al. (IEEE S&P 2020), demostraron fallos en la verificabilidad individual y universal causados por implementaciones erróneas de componentes criptográficos. Lección: un protocolo correcto no basta si la criptografía se implementa mal, lo que respalda reutilizar librerías revisadas en lugar de programar criptografía propia.

## Voto móvil y lecciones de seguridad

Voatz es una app móvil con verificación de identidad y dispositivo, usada en pilotos pequeños de EE. UU. (del orden de 600 votantes en total en varias jurisdicciones). Un análisis del MIT presentado en USENIX Security 2020 halló vulnerabilidades que permitirían alterar, bloquear o exponer el voto, y cuestionó la utilidad real de su blockchain. Una auditoría de Trail of Bits encargada por la propia Voatz confirmó los hallazgos y añadió 48 incidencias técnicas. Lecciones: el móvil amplía la superficie de ataque, la falta de transparencia destruye la confianza y «blockchain» no equivale a «seguro».

## Literatura académica

La literatura sobre voto electrónico converge en tres líneas. (1) Sistemas verificables de código abierto: Helios (Adida, 2008) introdujo la auditoría abierta en voto por web y Belenios (Cortier, Gaudry y Glondu, 2019) añade privacidad y verificabilidad incluso frente a un servidor comprometido. (2) Auditorías de sistemas reales: Springall et al. (2014) sobre Estonia, Halderman y Teague (2015) sobre iVote de Nueva Gales del Sur, Haines, Lewis, Pereira y Teague (2020) sobre Swiss Post/Scytl y Specter, Koppel y Weitzner (2020) sobre Voatz. (3) Normativa y criterios: la


Recomendación CM/Rec(2017)5 del Consejo de Europa. Siguen abiertas la verificación «cast-as- intended», la resistencia a la coacción y la usabilidad de la verificación para votantes no técnicos.

Conclusión para este proyecto: los fallos documentados se repiten en tres frentes: identidad del votante, implementación criptográfica propia y falta de transparencia. Esto justifica reutilizar protocolos y librerías ya revisados, publicar el código y documentar las pruebas de seguridad.

## Tabla comparativa

| Plataforma | Modelo | Verificabilidad | Identidad del votante | API | Móvil | Precio |
| --- | --- | --- | --- | --- | --- | --- |
| ElectionBuddy | SaaS | Auditoría/registros; sin E2E criptográfica | Email y códigos de acceso | No pública (ago- 2026) | Web | Gratis ≤20; 29 $ ≤350; 99 $ ≤1.000; desde 299 $ |
| Simply Voting | SaaS | Informes de auditoría | Códigos de acceso No |   | Web | Presupuesto |
| OpaVote | SaaS | Básica | Enlace único por email | No | Web | Gratis ≤25; 10 $/125 votantes |
| vote.direct | SaaS | Registro con huellas hash | Email, SMS, ID oficial opcional | Sí (según su web) | Web + widgets | Desde 4,99 $/elección |
| Helios | Abierto | E2E criptográfica | Email | No | No optimizado | Gratis |
| Belenios | Abierto (AGPLv3) | E2E, umbral, mixnets | Email; login Google o CAS | N/D | Web | Gratis (servidor público) |
| Vocdoni | Abierto + SaaS | E2E, zk-SNARK, Vochain | Censo CSV, CSP, tokens | Sí (SDK/API) | Web | Plan gratuito; de pago N/D |
| Decidim + Bulletin Board | Abierto | ElectionGuard | Cuenta de plataforma | Sí (Decidim) | Web | Autoalojado |
| POLYAS | SaaS | BSI CC (CORE 2.5); CORE 3 verificable | Credenciales y control de acceso | N/D | Web | Gratis ≤50 (estándar); desde 50 € |
| Scytl (e-Voto universitario) | Comercial | Criptográfica; cuestionada en Suiza 2019 | Integración con la universidad | N/D | Web | Contrato |
| NemoVote | SaaS | Cifrado; detalle N/D | N/D | N/D | Web | N/D |
| Voatz | App móvil | Cuestionada (MIT 2020) | Biometría + dispositivo | N/D | App nativa Contrato |   |
| Google Forms | Formularios Ninguna |   | Ninguna o cuenta Google | Sí (Forms) Web |   | Gratis |

## Hallazgos y huecos detectados

- Mercado partido en tres bloques: SaaS fáciles, pero con identidad débil y sin verificabilidad criptográfica; soluciones abiertas verificables, pero de uso técnico y poco pulidas; y opciones certificadas, con precios poco transparentes.


- API público escasa entre los generalistas (ElectionBuddy, OpaVote, Simply Voting, Helios). Solo Vocdoni y vote.direct destacan por ofrecerla. Este es uno de los huecos que menciona el tutor.

- Móvil: casi todas son web; la única app nativa dedicada relevante (Voatz) fue muy criticada. Una PWA accesible parece el camino más prudente.

- Identidad institucional (SSO): Belenios admite CAS, pero no aparece como norma en los SaaS generalistas; es una necesidad clara en universidades. Estonia lo confirma: su modelo depende de una e-ID nacional que una asociación o universidad no tiene.

- Verificabilidad accesible: falta un punto medio entre «confía en el proveedor» y «criptografía para expertos».

- Opacidad de precios en las opciones certificadas (POLYAS, Scytl) frente a tramos públicos en ElectionBuddy u OpaVote.

- Investigación abierta: verificación «cast-as-intended», resistencia a la coacción y experiencia del votante siguen siendo temas activos en la literatura, lo que abre espacio para una contribución académica. Las auditorías académicas de Estonia, Swiss Post y Voatz sitúan los fallos en la identidad, la criptografía implementada a mano y la falta de transparencia.

## Análisis DAFO

DAFO de la plataforma propuesta en el contexto del sector, elaborado a partir de los huecos de la sección anterior. Lo interno (debilidades y fortalezas) se refiere al proyecto y a su equipo; lo externo (amenazas y oportunidades), al mercado, al marco legal y a los usuarios.

| Debilidades (internas) | Fortalezas (internas) |
| --- | --- |
| • Equipo unipersonal y tiempo acotado (TFM) frente a empresas con años de trayectoria (ElectionBuddy desde 2012, Scytl desde 2001). • Sin certificaciones (BSI Common Criteria, ISO 27001) ni historial en elecciones reales: en e-voting la confianza es el principal activo. • Riesgo de errores si se implementa criptografía propia; hay que reutilizar librerías y protocolos revisados. • Sin auditoría independiente al inicio. • Coste y complejidad de operar infraestructura segura y disponible en los picos de cierre de votación. | • Arquitectura moderna: cloud-native, multi-tenant y API-first. • Perfil técnico transversal (cloud, seguridad, web/móvil, integración, visualización) que permite cubrir arquitectura, seguridad y UX sin depender de terceros. • Posibilidad de código abierto y transparencia auditable, en línea con Helios y Belenios. • Libertad para una UX simple y actual (las soluciones académicas abiertas suelen percibirse como anticuadas). • Acceso cercano a usuarios objetivo (universidad, asociaciones) para pilotos reales. |
| Amenazas (externas) | Oportunidades (externas) |
| • Competidores consolidados con planes gratuitos o baratos (ElectionBuddy gratis hasta 20 votantes, OpaVote hasta 25, POLYAS estándar hasta 50, Helios y Belenios gratuitos, Vocdoni con plan gratuito). • Desconfianza hacia el voto por internet y hacia soluciones «blockchain» tras casos como Voatz (MIT, 2020) o el sistema suizo de Scytl (2019). | • Carencia de API pública y webhooks entre los SaaS generalistas (según las fuentes consultadas). • Demanda de SSO institucional (SAML/OIDC/CAS) en universidades, poco cubierta por los generalistas; Estonia funciona gracias a una e-ID nacional que no existe en una organización normal. |


- Marco legal: RGPD, estatutos de cada organización y responsabilidad si una votación se impugna.

- Hueco en experiencia móvil accesible: la única app nativa relevante (Voatz) fue muy criticada, lo que deja espacio a una PWA accesible (WCAG).

- Ataques dirigidos (DDoS, phishing, malware en el dispositivo del votante) que no se pueden eliminar del todo.

- Falta de «verificabilidad amigable»: entre «confía en el proveedor» y «criptografía para expertos» hay un hueco (recibo verificable y registro público comprensible).

- Universidades con plataforma corporativa ya contratada (p. ej. la plataforma compartida de las universidades públicas catalanas): ciclos de compra lentos.

- Opacidad de precios en las opciones certificadas frente a tramos públicos: espacio para precios transparentes y modelo freemium.

- Necesidades específicas del mercado universitario e hispanohablante: voto ponderado/por colegios, modo híbrido, actas exportables, multiidioma y visualización de participación.

## Estrategias que se desprenden

- Fortalezas + Oportunidades: ofrecer una plataforma API-first con SSO institucional, PWA accesible y verificabilidad comprensible, publicada como código abierto.

- Debilidades + Oportunidades: no reinventar la criptografía; apoyarse en protocolos y librerías revisados y centrar el trabajo propio en arquitectura, integración, UX y auditoría.

- Fortalezas + Amenazas: contrarrestar la desconfianza con transparencia: código abierto, registro de auditoría verificable y pruebas de seguridad documentadas.

- Debilidades + Amenazas: acotar el alcance a votaciones de asociaciones y consultas internas, evitando elecciones oficiales vinculantes en la primera versión.

## Requisitos funcionales propuestos

Derivados de los huecos de la sección 3. Prioridad según MoSCoW: M (imprescindible), S (debería), C (podría). Se propone un MVP con los requisitos M y parte de los S.

| ID | Requisito |   | Prior. Origen / hueco |
| --- | --- | --- | --- |
| RF-01 | Alta y configuración de organizaciones (tenants) con aislamiento de datos y marca básica. |   | M Arquitectura SaaS |
| RF-02 | Roles y permisos: superadministrador, administrador de organización, gestor de censo, auditor y votante. |   | M Gestión de usuarios |
|   | RF-03 Gestión de censo: alta manual, importación CSV, grupos/colegios electorales y control de duplicados. |   | M Universidades/asociaciones |
| RF-04 | Autenticación de administradores con doble factor. |   | M Seguridad |
| RF-05 | Inicio de sesión institucional (SAML, OIDC o CAS). | S | Hueco SSO (Belenios usa CAS) |
| RF-06 | Planes, cuotas y facturación por tramos de votantes con precios transparentes. | C | Opacidad de precios |
| RF-07 | Crear y configurar un proceso: título, descripción, calendario, censo y papeleta. |   | M Núcleo del producto |
| RF-08 | Ciclo de vida (borrador, programado, abierto, cerrado, escrutado, archivado) con apertura y cierre automáticos. |   | M Núcleo del producto |


| ID | Requisito |   | Prior. Origen / hueco |
| --- | --- | --- | --- |
| RF-09 | Tipos de voto: opción única, múltiple, sí/no/abstención y voto en blanco. |   | M Estándar del mercado |
|   | RF-10 Métodos avanzados: preferencial (STV, Condorcet), ponderado y por colegios. | S | OpaVote, Belenios, vote.direct |
| RF-11 | Papeletas y textos multiidioma. | S | Mercado hispano |
|   | RF-12 Quórum y participación mínima configurables. | C | ElectionBuddy, vote.direct |
|   | RF-13 Modo híbrido: registro del voto presencial y online sin duplicados. | C | Decidim, POLYAS, NemoVote |
| RF-14 | Acceso del votante con enlace personal más verificación por OTP (email/SMS) o SSO. |   | M Brecha de identidad |
| RF-15 | Garantía de un voto por votante y secreto del voto. |   | M Propiedades básicas |
| RF-16 | Recibo verificable del voto sin revelar su contenido. | S | Helios, Belenios |
| RF-17 | Re-votación hasta el cierre (cuenta el último voto) como mitigación de la coacción. | C | Literatura de coacción (a validar) |
| RF-18 | Interfaz responsive y accesible (WCAG 2.2 AA). |   | M Usabilidad |
| RF-19 | PWA instalable con notificaciones. | S | Hueco móvil |
| RF-20 | Cifrado del voto en el cliente, urna cifrada y clave de escrutinio repartida entre custodios (umbral). | S | Belenios, Vocdoni (decisión de alcance) |
| RF-21 | Registro de auditoría encadenado por hash y publicación de la huella de la urna. |   | M Integridad |
| RF-22 | Escrutinio automático al cierre y verificación pública del resultado. |   | M Núcleo del producto |
| RF-23 | Acta de resultados exportable en PDF con huella. | S | Legalidad/impugnaciones |
| RF-24 | Panel de participación en tiempo real sin mostrar resultados parciales. |   | M Visualización de datos |
| RF-25 | Resultados con gráficos tras el cierre, exportables a CSV/PDF. |   | M Requisito del enunciado |
| RF-26 | API REST documentada (OpenAPI) con claves por organización y webhooks. | S | Hueco de API |
| RF-27 | Widget embebible para webs de la organización. | C | vote.direct |
|   | RF-28 Notificaciones de invitación, recordatorio y cierre por email/SMS. |   | M Participación |
| RF-29 | Funciones RGPD: minimización, retención y borrado configurables, exportación de datos. |   | M Cumplimiento |
| RF-30 | Trazabilidad de todas las acciones administrativas. |   | M Auditoría |


## Requisitos no funcionales clave

- Seguridad: OWASP ASVS como referencia, TLS y cifrado en reposo, gestión de secretos, limitación de peticiones y protección frente a DDoS, aislamiento entre organizaciones.

- Rendimiento y escalabilidad: tolerar los picos previos al cierre; pruebas de carga documentadas.

- Disponibilidad y recuperación: copias de seguridad y objetivos de recuperación definidos.

- Calidad y operación: infraestructura como código, CI/CD, pruebas automáticas y observabilidad.

- Privacidad y cumplimiento: RGPD desde el diseño y alineación con la Recomendación CM/Rec(2017)5 como lista de comprobación.

## Encaje con el máster y propuesta de TFM

El tema es adecuado para un Trabajo Fin de Máster de tipo ingeniería (estudio previo, diseño, desarrollo y evaluación), porque integra varias asignaturas del máster:

| Asignatura | Aportación al proyecto |
| --- | --- |
| Cloud Computing | Despliegue SaaS multi-tenant, contenedores, infraestructura como código, autoescalado en picos. |
| Desarrollo Web/Móvil | Frontend responsive/PWA y API REST. |
| Seguridad Informática | Autenticación, cifrado, modelado de amenazas, pruebas de intrusión y auditoría. |
| Integración de Tecnologías y Servicios | SSO institucional, API, webhooks y notificaciones. |
| Bases de Datos a Gran Escala | Modelo de datos multi-tenant y registro de auditoría inmutable. |
| Visualización de Datos | Paneles de participación y resultados. |
| Planificación de las TIC | Modelo de negocio, costes, planes de precios y plan de proyecto. |
| Inteligencia Artificial / Sistemas Inteligentes | Extensión opcional: detección de anomalías en patrones de participación. |

Título provisional: «Diseño e implementación de una plataforma SaaS de votación digital segura y auditable para organizaciones, con arquitectura cloud multi-tenant y API abierta».

Objetivo general: diseñar, implementar y evaluar un prototipo funcional de plataforma de votación que cubra los huecos detectados (API, SSO, PWA, auditabilidad comprensible).

Alcance realista propuesto: MVP con los requisitos M, evaluación de seguridad (modelo de amenazas y pruebas), pruebas de carga y, si es posible, un piloto con una asociación o grupo universitario. La verificabilidad criptográfica completa (RF-20) queda como decisión de alcance a acordar con el tutor: implementarla, integrar una librería existente o limitarse a registro de auditoría por hash.


## Referencias

ElectionBuddy, OpaVote, Simply Voting, Helios, Google Forms y vote.direct (comparativa y precios, ago-sep 2026; parte interesada): https://vote.direct/blog/best-online-voting-

tools-2026 Belenios, sitio oficial: https://www.belenios.org/

https://radar.inria.fr/report/2018/pesto/uid38.html · Cortier, Gaudry, Glondu (2019), Belenios: A Simple Private and Verifiable Electronic Voting System. POLYAS, precios y certificación: https://www.polyas.com/products/pricing/online-voting · https://www.polyas.com/security/core3 Vocdoni, documentación y SDK: https://docs.vocdoni.io/architecture/general · https://github.com/vocdoni/vocdoni-sdk · https://github.com/vocdoni/vocdoni-

integrator-sdk Decidim y Vocdoni: https://democracy-technologies.org/voting/blockchain-voting- decidim-vocdoni/ · Metadecidim, Community Meeting 2: https://meta.decidim.org/

Scytl y voto electrónico universitario: https://seuelectronica.uab.cat/es/el-voto-

electronico-en-la-uab

https://www.elespanol.com/invertia/disruptores- innovadores/innovadores/tecnologicas/20190314/empresa-espanola-llevado-voto-

electronico-mundo-hackeada/383213595_0.html

https://www.elnacional.cat/oneconomia/es/empresas/scytl-voto-digital-

liquidacion_1186959_102.html Voatz: Specter, Koppel y Weitzner (2020), The Ballot is Busted Before the Blockchain, USENIX Security · MIT News: https://news.mit.edu/2020/voting-voatz-app-hack-issues-

0213 · Computerworld y Vice sobre la auditoría de Trail of Bits. Consejo de Europa, Recomendación CM/Rec(2017)5 sobre estándares de e-voting: https://rm.coe.int/0900001680726f6f

Estonia: Springall et al. (2014), Security Analysis of the Estonian Internet Voting System, ACM CCS 2014: https://estoniaevoting.org/findings/paper/ · Madise y Vinkel (2014), Internet Voting in Estonia, en Regulating eTechnologies in the European Union (Springer) · cifras de participación: https://www.valimised.ee/node/1667 · https://e- estonia.com/how-did-estonia-carry-out-the-worlds-first-mostly-online-national-

elections/ Swiss Post / Scytl: Haines, Lewis, Pereira y Teague (2020), How Not to Prove Your Election Outcome, IEEE S&P 2020 (DOI 10.1109/SP40000.2020.00048) · Lewis, Pereira y Teague (2019), Addendum: https://sarahjamielewis.com/log/2019/swisspost-addendum.html

Helios: Adida (2008), Helios: Web-based Open-Audit Voting, USENIX Security 2008 · iVote (Nueva Gales del Sur): Halderman y Teague (2015), The New South Wales iVote System:

Security Failures and Verification Flaws in a Live Online Election, VoteID 2015.

Directorios

(https://www.capterra.es/directory/31217/voting/software), (https://eligovotacion.es/).

·

informe Inria:

·

El

Español

(2019):

· El Nacional (liquidación):

de

software

español:

Capterra

España Eligo
