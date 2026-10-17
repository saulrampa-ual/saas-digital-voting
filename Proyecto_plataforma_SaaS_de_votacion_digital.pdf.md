## Proyecto plataforma SaaS de votación digital

# Especificación de Requisitos

Versión: 1.0 Fecha: 07/10/2026

Queda prohibido cualquier tipo de explotación puesto que es la adaptación con fines académicos de la plantilla de ERQ propuesta por la junta de Andalucía para las asignaturas de la especialidad de Ingeniería del Software del grado de Informática de la Universidad de Almería

Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos

### HOJA DE CONTROL

|Organismo|Universidad de Almería|||
|---|---|---|---|
|Proyecto|Proyecto plataforma SaaS de votación digital|||
|Entregable|Especificación de Requisitos|||
|Autor|Saul David Ramos Pacheco|||
|Versión/Edición|1.0|Fecha Versión|07/10/2026|
|Aprobado por|Javier Criado Rodríguez|Fecha Aprobación Nº Total de Páginas|33|

REGISTRO DE CAMBIOS

|Versión|Causa del Cambio|Responsable del Cambio|Fecha del Cambio|
|---|---|---|---|
|1.0|Versión inicial|Saul David Ramos Pacheco|07/10/2026|

CONTROL DE DISTRIBUCIÓN

##### Nombre y Apellidos

Javier Criado Rodríguez Saul David Ramos Pacheco

Página 2 de 33

||Proyecto plataforma SaaS de votación||
|---|---|---|
||digital|SDRP|
||Especificación de Requisitos||

# ÍNDICE

1 INTRODUCCIÓN ............................................................................................ 4

1.1 Alcance.................................................................................................... 4
1.2 Objetivos................................................................................................. 4
2 INFORMACIÓN DEL DOMINIO DEL PROBLEMA............................................................. 5

2.1 Introducción al Dominio del Problema...................................................................5
2.2 Glosario de Términos...................................................................................... 5
3 CATÁLOGO DE REQUISITOS DEL SISTEMA A DESARROLLAR............................................. 6

3.1 Requisitos Generales del Sistema ....................................................................... 6
3.2 Casos de uso del Sistema................................................................................ 7
3.2.1 Diagramas de Casos de Uso del Sistema........................................................ 7
3.2.2 Especificación de Actores del Sistema............................................................ 8
3.2.3 Especificación de Casos de Uso del Sistema.................................................... 9
3.3 Requisitos Funcionales del Sistema....................................................................23
3.3.1 Requisitos de Información del Sistema.......................................................... 23
3.4 Requisitos No Funcionales del Sistema................................................................26
3.4.1 Requisitos de Usabilidad...........................................................................26
##### Página 3 de 33

SDRP digital Especificación de Requisitos

### 1 INTRODUCCIÓN

Este documento recoge la Especificación de Requisitos del Proyecto plataforma SaaS de votación digital, desarrollado como Trabajo Fin de Máster en la Universidad de Almería. Su finalidad es describir de forma clara y verificable qué debe hacer el sistema y con qué calidad de uso, para que sirva de base común entre el cliente, el tutor y quien lo desarrolle. El documento se organiza así: la sección 2 describe el dominio del problema y el glosario de términos; la sección 3 contiene el catálogo de requisitos, con los objetivos del sistema, los casos de uso y actores, los requisitos funcionales, los requisitos de información y los requisitos no funcionales de usabilidad.

## 1.1 Alcance

La plataforma SaaS de votación digital permitirá a universidades, asociaciones, organizaciones y comunidades crear procesos de votación, gestionar participantes, emitir votos de forma segura y mostrar resultados al cierre. El sistema se ofrecerá como servicio en la nube, con arquitectura multi-tenant.

## 1.2 Objetivos

El objetivo principal es aportar una contribución académica en verificación «cast-as-intended» comprensible, experiencia móvil accesible y SSO institucional opcional. La API pública, la transparencia de precios y la integración avanzada se consideran trabajo futuro.

##### Página 4 de 33

|||digital Especificación de Requisitos|Proyecto plataforma SaaS de votación SDRP|
|---|---|---|---|
|2 2.1 • • • • • • 2.2||INFORMACIÓN DEL DOMINIO DEL PROBLEMA Introducción al Dominio del Problema procedimientos manuales. El proceso habitual es el siguiente: La herramienta envía a cada persona del censo un enlace por correo electrónico. accesible. La plataforma que se especifica pretende cubrir esas carencias. Glosario de Términos|Un sistema de votación electrónica debe garantizar elegibilidad y unicidad, secreto del voto, verificabilidad extrema a extremo, resistencia a la coacción, disponibilidad, usabilidad y accesibilidad. El mercado actual se divide en SaaS generalistas con identidad débil, soluciones abiertas verificables, pero poco pulidas, y opciones certificadas con precios opacos. Los fallos documentados en Estonia, Swiss Post y Voatz se repiten en tres frentes: identidad del votante, implementación criptográfica propia y falta de transparencia. Muchas universidades, asociaciones y comunidades que necesitan votar en línea recurren a herramientas generalistas o a La organización prepara el censo de votantes, normalmente en una hoja de cálculo, y define las opciones. El organizador configura la votación en una herramienta externa o en un formulario genérico. El votante abre el enlace, elige su opción y envía el voto, a menudo sin verificación fuerte de identidad. El votante no recibe ninguna prueba de que su voto se ha registrado tal como lo emitió. Al cierre, el organizador consulta el resultado sin que ningún tercero pueda verificarlo. Este modo de trabajo presenta carencias: identidad del votante débil, falta de verificabilidad y experiencia móvil poco|
||Término||Definición|
||SaaS (Software as a Service)||Modelo de distribución de software mediante el cual los usuarios acceden a una aplicación a través de Internet, sin necesidad de instalarla localmente.|
||Multi-tenant||Arquitectura en la que varias organizaciones comparten una misma infraestructura tecnológica, manteniendo sus datos y configuraciones aislados lógicamente.|
||SSO (Single Sign-On)||Mecanismo de autenticación que permite a los usuarios acceder a diferentes aplicaciones utilizando una única sesión de inicio de sesión.|
||OIDC (OpenID Connect)||Protocolo de autenticación basado en OAuth 2.0 que permite verificar la identidad de los usuarios..|
||OTP (One-Time Password)||Contraseña temporal de un solo uso utilizada como mecanismo adicional de autenticación.|
||PWA (Progressive Web App)||Aplicación web que incorpora funcionalidades similares a las aplicaciones móviles, como instalación en dispositivos y funcionamiento parcial sin conexión.|
||WCAG 2.2 AA||Nivel de conformidad de las pautas internacionales de accesibilidad web que establece criterios para facilitar el acceso a personas con diferentes capacidades. Página 5 de 33|

SDRP digital Especificación de Requisitos

|RGPD|Reglamento General de Protección de Datos de la Unión Europea que regula el tratamiento y la protección de los datos personales.|
|---|---|
|Huella (hash)|Valor resumen de longitud fija que se obtiene a partir de un contenido y permite detectar alteraciones.|
|Recibo de voto|Justificante que el votante obtiene tras emitir su voto y que permite comprobar que consta en la urna sin revelar su contenido.|
|Registro de auditoría|Registro encadenado de los eventos de la urna y de las acciones administrativas, no editable.|

##### Página 6 de 33

||||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos|
|---|---|---|---|
|3 3.1||negocio propuestas por el cliente.|CATÁLOGO DE REQUISITOS DEL SISTEMA A DESARROLLAR En esta sección se describe la solución propuesta por el ingeniero de requisitos para satisfacer las necesidades de Requisitos Generales del Sistema A continuación, se describen los requisitos generales del sistema en forma de objetivos.|
|OBJ-1|Versión||Gestión de organizaciones y usuarios 1.0|
||Descripción||El sistema deberá gestionar organizaciones, usuarios, roles y permisos.|
||Comentarios||Incluye superadministrador, administrador de organización, gestor de censo, auditor y votante.|
|OBJ-2|Versión||Gestión de procesos electorales 1.0|
||Descripción||El sistema deberá permitir crear, configurar y gestionar el ciclo de vida de procesos de votación|
||Comentarios||Incluye calendario, censo, papeleta, modalidades y métodos avanzados.|
|OBJ-3|Versión||Participación y voto 1.0|
||Descripción||El sistema deberá permitir a los votantes autenticarse, emitir su voto y obtener un recibo verificable. Página 7 de 33|

||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos|
|---|---|
|Comentarios|Debe garantizar un voto por votante y secreto del voto.|
|OBJ-4|Escrutinio y auditoría|
|Versión|1.0|
|Descripción|El sistema deberá realizar el escrutinio, generar actas y permitir la verificación pública.|
|Comentarios|Incluye panel de participación y registro de auditoría.|
|3.2 nn).|Casos de uso del Sistema Esta sección contiene la especificación de los casos de uso del sistema, incluyendo un diagrama de casos de uso, la especificación de los actores y la especificación de cada uno de los casos de uso. 3.2.1 Diagramas de Casos de Uso del Sistema El diagrama de la Figura 1 resume los casos de uso identificados. Los casos de uso se nombran igual que los RFs (RF-Página 8 de 33|

|digital|Proyecto plataforma SaaS de votación Especificación de Requisitos|SDRP|
|---|---|---|
||Figura 1. Diagrama de Caso de Uso|Página 9 de 33|
|notificaciones.||3.2.2 Especificación de Actores del Sistema Se describen los cinco perfiles de usuario del sistema: Superadministrador, Administrador de organización, Auditor, Votante y Gestor de censo. Adicionalmente, se describen dos actores externos: Proveedor de identidad y Servicio de|
|PU-1||Votante|
|Descripción||Persona que participa en una votación.|
|Comentarios||Ninguno.|
|PU-2||Administrador de organización|
|Descripción||Gestiona usuarios, configuración y procesos de su organización.|
|Comentarios||Ninguno.|
|PU-3||Superadministrador|
|Descripción||Gestiona organizaciones y configuración global del SaaS.|
|Comentarios||Ninguno.|
|PU-4||Auditor|
|Descripción||Consulta registros y verifica información electoral.|
|Comentarios||Ninguno.|
|PU-5||Gestor de censo|
|Descripción||Gestiona participantes, grupos y colegios.|
|Comentarios||Ninguno. A continuación, se describen los actores (ACT) o roles del sistema:|
|ACT-1||Superadministrador|
|Perfil de usuario||PU-3|
||Requisitos asociados|RF-01, RF-02, RF-04|
|Descripción||Gestiona organizaciones y configuración global.|
|Comentarios||Ninguno. Página 10 de 33|
|ACT-2||Administrador|
|Perfil de usuario||PU-2|
||Requisitos asociados|RF-02, RF-04, RF-05, RF-06, RF-07, RF-14, RF-15, RF-16, RF-17, RF-18, RF-19|
|Descripción||Gestiona usuarios, procesos y configuración de su organización.|
|Comentarios||Ninguno.|
|ACT-3||Auditor|
|Perfil de usuario||PU-4|
||Requisitos asociados|RF-04, RF-12, RF-13, RF-19|
|Descripción||Consulta auditoría y verifica resultados.|
|Comentarios||Ninguno.|
|ACT-4||Votante|
|Perfil de usuario||PU-1|
||Requisitos asociados|RF-05, RF-08, RF-09, RF-10, RF-11, RF-12, RF-13, RF-15|
|Descripción||Participa en votaciones.|
|Comentarios||Ninguno. Página 11 de 33|

||Proyecto plataforma SaaS de votación||
|---|---|---|
||digital|SDRP|
||Especificación de Requisitos||

|ACT-5|Gestor de censo|
|---|---|
|Perfil de usuario|PU-5|
|Requisitos asociados|RF-03, RF-04|
|Descripción|Gestiona el censo de votantes de la organización. Para acceder al sistema, deberá autenticarse mediante el mecanismo de autenticación definido en RF-04.|
|Comentarios|Ninguno.|

|ACT-6|Proveedor de identidad|
|---|---|
|Tipo|Sistema externo|
|Requisitos asociados|RF-05|
|Descripción|Autenticación institucional mediante OIDC.|
|Comentarios|Ninguno.|

|ACT-7|Servicio de notificaciones|
|---|---|
|Tipo|Sistema externo|
|Requisitos asociados|RF-08, RF-17|
|Descripción|Envío de correos electrónicos y notificaciones.|
|Comentarios|Ninguno.|

##### Página 12 de 33

SDRP digital Especificación de Requisitos

#### 3.2.3 Especificación de Casos de Uso del Sistema

A continuación, se muestran las especificaciones de los casos de uso del sistema que han sido identificados. Cada caso de uso se identifica como RF-nn y su prioridad (M, S o C, según MoSCoW) se indica en Comentarios. Los requisitos RNF-8 y RNF-10 se aplican a todos los casos de uso.

|RF-01|Gestionar organizaciones||
|---|---|---|
|Objetivos asociados|OBJ-1||
|Requisitos asociados|RI-1||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Superadministrador (ACT-1) da de alta una organización.||
|Precondición|El usuario está autenticado (RF-04) como Superadministrador.||
|Secuencia normal|Paso 1 2 3 4 5|Acción El Superadministrador solicita dar de alta una organización El Sistema solicita el nombre, identificador y administrador inicial de la organización. El Superadministrador introduce los datos solicitados. El Sistema comprueba que el identificador no está en uso. El Sistema crea la organización con su espacio de datos aislado y asigna el rol de Administrador de organización al administrador inicial.|
|Postcondición|La organización existe con su espacio de datos aislado y el administrador inicial tiene asignado el rol de Administrador de organización.||
|Excepciones|Paso 4|Acción Si el identificador ya está en uso, el Sistema informa de la situación y solicita introducir otro identificador.|
|Comentarios|Ninguno||

|RF-02||Gestionar roles y permisos||
|---|---|---|---|
|Objetivos asociados|OBJ-1|||
|Requisitos asociados|RI-2|||
|Descripción||El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) asigna o cambia el rol de un usuario de su organización (el Superadministrador (ACT-1) puede hacerlo en cualquier organización).||
|Precondición||El usuario está autenticado y el usuario destino pertenece a la organización.||
|Secuencia normal|Paso 1 2 3 4|Acción El Administrador solicita gestionar usuarios y roles El Sistema muestra los usuarios de la organización con su rol El Administrador selecciona un usuario o solicita crear uno nuevo El Sistema muestra los roles asignables|Página 13 de 33|

SDRP digital Especificación de Requisitos

||5|El Administrador selecciona el rol|
|---|---|---|
||6|El Sistema guarda el rol y aplica sus permisos desde la siguiente acción del usuario|
|Postcondición|El usuario tiene el nuevo rol y sus permisos efectivos.||
|Excepciones|Paso 5 5|Acción Si se intenta dejar la organización sin Administrador, el Sistema lo impide e informa Si el actor no puede asignar ese rol, el Sistema deniega la operación|
|Comentarios|Prioridad M. Se asume que solo un Superadministrador puede asignar el rol||

de Superadministrador.

|RF-03|Gestionar censo||
|---|---|---|
|Objetivos asociados|OBJ-2||
|Requisitos asociados|RI-3, RNF-4||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Administrador da de alta, importa o modifica votantes del censo.||
|Precondición|El Gestor de censo está autenticado.||
|Secuencia normal|Paso 1 2 3 3.1 3.2 3.1 3.2 3.3 3.4 4 5|Acción El Gestor solicita gestionar el censo El Sistema muestra los censos existentes El Gestor elige la acción Si elige alta manual El Sistema solicita nombre, email o teléfono El Gestor introduce los datos Si elige importar CSV El Sistema solicita el fichero CSV El Gestor selecciona el fichero El Sistema valida el fichero y muestra un resumen (altas, duplicados, errores) El Gestor confirma la importación El Sistema detecta duplicados y no los da de alta El Sistema guarda los cambios e informa del resultado|
|Postcondición|El censo está actualizado y sin duplicados.||
|Excepciones|Paso 3.3 4 5|Acción Si el fichero no es válido, el Sistema informa de las líneas erróneas y permite descartarlas o cancelar Si hay duplicados, el Sistema los omite e informa de cuáles Si el censo pertenece a un proceso abierto, el Sistema rechaza altas y bajas e informa|
|Comentarios|Prioridad M. Se asume que el censo queda congelado mientras el proceso está abierto.||

##### Página 14 de 33

SDRP digital Especificación de Requisitos

|RF-04|Autenticar usuario de gestión||
|---|---|---|
|Objetivos asociados|OBJ-1||
|Requisitos asociados|RI-2||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Superadministrador, Administrador, Gestor de censo o Auditor desea iniciar sesión.||
|Precondición|La cuenta existe y está activa.||
|Secuencia normal|Paso 1 2 3 4 5 6 7 8|Acción El Usuario solicita iniciar sesión El Sistema solicita usuario y contraseña El Usuario introduce sus credenciales El Sistema valida las credenciales El Sistema solicita el segundo factor El Usuario introduce el segundo factor El Sistema valida el segundo factor El Sistema inicia la sesión y muestra la pantalla de inicio de su rol|
|Postcondición|Sesión iniciada con el rol y la organización correspondientes.||
|Excepciones|Paso 4 7|Acción Si las credenciales son incorrectas, el Sistema informa del error y permite reintentar. Si el código del segundo factor es incorrecto o ha caducado, el Sistema informa del error y permite reintentar o solicitar un nuevo código.|
|Comentarios|Prioridad M. Existe una vía alternativa de acceso mediante SSO institucional (RF-05). El segundo factor de autenticación se realizará mediante un código enviado al correo electrónico del usuario.||

|RF-05|Iniciar sesión con SSO institucional||
|---|---|---|
|Objetivos asociados|OBJ-1, OBJ-3||
|Requisitos asociados|RI-2, RI-3||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Superadministrador, Administrador, Auditor o Votante inicia sesión con la cuenta de su organización a través del Proveedor de identidad (ACT-6).||
|Precondición|La organización tiene configurado un proveedor de identidad compatible con OIDC.||
|Secuencia normal|Paso 1 2 3 4 5 6|Acción El usuario elige acceder con la cuenta institucional El Sistema redirige al proveedor de identidad de la organización El proveedor de identidad autentica al usuario El proveedor devuelve la identidad al Sistema El Sistema comprueba que la identidad corresponde a un usuario con rol o a un votante del censo El Sistema inicia la sesión o concede el acceso|

##### Página 15 de 33

SDRP digital Especificación de Requisitos

|RF-06|Crear y configurar proceso de votación||
|---|---|---|
|Objetivos asociados|OBJ-2||
|Requisitos asociados|RI-4, RI-5, RNF-4||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) crea y configura un proceso de votación, incluyendo la definición del tipo de voto y de la papeleta.||
|Precondición|El Administrador está autenticado y existe un censo (RF-03).||
|Secuencia normal|Paso 1 2 3 4 5 6 7 8 9 10 11 12 13|Acción El Administrador solicita crear un proceso El Sistema solicita título y descripción El Administrador introduce el título y la descripción. El Sistema solicita el calendario (apertura y cierre) El Administrador introduce fechas y horas El Sistema solicita el censo participante. El Administrador selecciona el censo participante. El Sistema solicita definir la papeleta. El Sistema muestra los tipos de voto disponibles: opción única y sí/no/abstención. El Administrador selecciona el tipo de voto. El Administrador introduce las opciones y decide si habilita el voto en blanco. El Sistema valida la coherencia del proceso y de la papeleta: fechas correctas, censo no vacío y papeleta completa. El Sistema guarda el proceso en estado Borrador.|
|Postcondición|El proceso de votación existe en estado Borrador con su papeleta definida.||
|Excepciones|Paso 12|Acción Si existen datos incompletos o incoherentes en el proceso o en la papeleta, el Sistema informa del error y permite corregir los datos sin guardar.|
|Comentarios|Prioridad M. Incluye la definición del tipo de voto y de la papeleta.||

|Postcondición|Sesión iniciada con la identidad institucional.||
|---|---|---|
|Excepciones|Paso 3 4 5|Acción Si el usuario cancela o no se autentica, el Sistema vuelve a la pantalla de inicio Si el proveedor no responde o la respuesta no es válida, el Sistema informa y ofrece otro método si existe Si la identidad no figura en la organización o el censo, el Sistema deniega el acceso e informa|
|Comentarios|Prioridad Should. Amplía el acceso de RF-08. El protocolo admitido es OIDC.||

##### Página 16 de 33

SDRP digital Especificación de Requisitos

|RF-07|Gestionar ciclo de vida del proceso||
|---|---|---|
|Objetivos asociados|OBJ-2||
|Requisitos asociados|RI-4, RI-9||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) programa y archiva un proceso, y el Sistema lo abre y lo cierra automáticamente en las fechas fijadas.||
|Precondición|El proceso existe y el Administrador está autenticado.||
|Secuencia normal|Paso 1 2 3 4 5 6 7 8|Acción El Administrador solicita programar el proceso El Sistema valida que el proceso está completo. El Sistema cambia el proceso a Programado Al llegar la fecha de apertura, el Sistema cambia el proceso a Abierto y habilita el acceso de los votantes (RF-08) Al llegar la fecha de cierre, el Sistema cambia el proceso a Cerrado y deja de aceptar votos El Sistema publica la huella final de la urna (RF-12), inicia el escrutinio (RF-13) y, al terminar, cambia el proceso a Escrutado El Administrador solicita archivar el proceso El Sistema cambia el proceso a Archivado|
|Postcondición|Estado del proceso actualizado y registrado (RF-19).||
|Excepciones|Paso 2|Acción Si el proceso está incompleto, el Sistema informa y no lo programa.|
|Comentarios|Prioridad M. Lo amplían RF-13 (escrutinio al cerrar) y RF-17 (avisos). No se permite abrir ni cerrar un proceso manualmente de forma anticipada; la apertura y el cierre se realizan automáticamente en las fechas configuradas.||

|RF-08|Acceder como votante||
|---|---|---|
|Objetivos asociados|OBJ-3||
|Requisitos asociados|RI-3, RI-12, RNF-1, RNF-2, RNF-3, RNF-5, RNF-9||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Votante (ACT-4) accede a la papeleta de un proceso abierto; el código lo envía el Servicio de notificaciones (ACT-7).||
|Precondición|El proceso está Abierto, el votante está en el censo y ha recibido su enlace personal (RF-17).||
|Secuencia normal|Paso 1 2 3 3.1|Acción El Votante abre su enlace personal El Sistema valida el enlace y que el proceso está abierto El Sistema solicita la verificación adicional del proceso Si es un código OTP El Sistema envía el código por email|

##### Página 17 de 33

SDRP digital Especificación de Requisitos

||3.2|El Votante introduce el código|
|---|---|---|
||3.3 3.1 4|El Sistema valida el código Si es SSO El Votante se identifica con RF-05 El Sistema concede acceso a la papeleta|
|Postcondición|El votante está identificado para ese proceso.||
|Excepciones|Paso 2 2 3.3|Acción Si el enlace no es válido o ha caducado, el Sistema informa y no concede acceso Si el proceso no está abierto, el Sistema informa de su estado y calendario Si el código es incorrecto o ha caducado, el Sistema permite reintentar o reenviarlo.|
|Comentarios|Prioridad M. La verificación adicional (OTP o SSO) evita que un enlace||

##### reenviado permita votar a otra persona.

|RF-09|Emitir voto||
|---|---|---|
|Objetivos asociados|OBJ-3||
|Requisitos asociados|RI-5, RI-6, RI-7, RNF-1, RNF-2, RNF-3, RNF-5, RNF-7, RNF-9||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Votante (ACT-4) emite su voto en un proceso abierto.||
|Precondición|El votante ha accedido (RF-08) y no tiene un voto contabilizado, o el proceso permite re-votar (RF-11).||
|Secuencia normal|Paso 1 2 3 4 5 6 7 8 9|Acción El Sistema muestra la papeleta. El Votante selecciona sus opciones El Sistema valida la selección según el tipo de papeleta El Sistema muestra un resumen y pide confirmación El Votante confirma El Sistema comprueba que no hay ya un voto contabilizado del votante El Sistema registra el voto en la urna. El Sistema marca al votante como «ha votado» El Sistema confirma la emisión y ofrece el recibo (RF-10)|
|Postcondición|El voto está en la urna y el votante figura como votante.||
|Excepciones|Paso 3 6 7|Acción Si la selección no es válida, el Sistema informa y no avanza Si el votante ya votó y no se permite re-votar, el Sistema informa y no registra nada Si el proceso se cierra mientras vota o falla el registro, el Sistema informa y no queda ningún voto parcial ni duplicado|
|Comentarios|Prioridad M. El voto se registra en la urna y el votante queda marcado como «ha votado». Como máximo se contabiliza un voto por votante.||

##### Página 18 de 33

SDRP digital Especificación de Requisitos

|RF-10|Obtener y verificar recibo del voto||
|---|---|---|
|Objetivos asociados|OBJ-3, OBJ-4||
|Requisitos asociados|RI-6, RI-8, RNF-1, RNF-2, RNF-3, RNF-6||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Votante (ACT-4) quiere comprobar que su voto consta en la urna.||
|Precondición|El votante ha emitido un voto (RF-09).||
|Secuencia normal|Paso 1 2 3 4 5|Acción El Sistema genera un recibo del voto. El Sistema entrega el recibo al votante El Votante solicita verificar su recibo El Sistema busca el recibo en la urna publicada El Sistema informa de que el voto consta en la urna|
|Postcondición|Ninguna.||
|Excepciones|Paso 4|Acción Si el recibo no se encuentra, el Sistema informa de que el voto no consta|
|Comentarios|Prioridad M. Amplía RF-09. Si el votante vuelve a votar (RF-11), el recibo||

##### anterior se marca como sustituido.

|RF-11|Re-votar||
|---|---|---|
|Objetivos asociados|OBJ-3||
|Requisitos asociados|RI-6, RI-7, RI-8, RNF-1, RNF-2, RNF-7, RNF-9||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Votante (ACT-4) cambia su voto antes del cierre.||
|Precondición|El proceso está Abierto, permite re-votación y el votante ya ha votado y ha accedido (RF-08).||
|Secuencia normal|Paso 1 2 3 4 5 6|Acción El Votante solicita cambiar su voto El Sistema informa de que el nuevo voto sustituirá al anterior El Votante confirma El Votante emite el nuevo voto (RF-09) El Sistema registra el nuevo voto y marca el anterior como sustituido sin borrarlo El Sistema entrega el nuevo recibo|
|Postcondición|Solo cuenta el último voto del votante.||
|Excepciones|Paso 3 5|Acción Si la votante cancela, su voto anterior sigue siendo válido Si el proceso se cierra antes de registrar el nuevo voto, el anterior sigue siendo válido|
|Comentarios|Prioridad C. Amplía RF-15. La re-votación mitiga la coacción: el votante puede cambiar su voto hasta el cierre.||

##### Página 19 de 33

SDRP digital Especificación de Requisitos

|RF-12|Publicar y verificar registro de auditoría||
|---|---|---|
|Objetivos asociados|OBJ-4||
|Requisitos asociados|RI-9||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Auditor (ACT-3) consulta y verifica el registro de la urna, o un Votante (ACT- 4) consulta la huella publicada.||
|Precondición|Existe un proceso con eventos en la urna.||
|Secuencia normal|Paso 1 2 3 4 5 6|Acción El Auditor solicita ver el registro de auditoría de un proceso El Sistema muestra los eventos de la urna (apertura, votos recibidos sin identidad, sustituciones, cierre) con su huella El Auditor solicita verificar la integridad El Sistema comprueba la integridad del registro. El Sistema informa del resultado de la verificación Si el proceso está cerrado El Sistema muestra la huella final publicada, que cualquier Votante puede consultar|
|Postcondición|Ninguna.||
|Excepciones|Paso 2 5|Acción Si el proceso no tiene eventos, el Sistema informa Si la cadena está rota, el Sistema alerta al Auditor y al Administrador y marca una incidencia de integridad|
|Comentarios|Prioridad S.||

|RF-13||Escrutar y verificar resultado||
|---|---|---|---|
|Objetivos asociados|OBJ-4|||
|Requisitos asociados||RI-4, RI-6, RI-10, RNF-6||
|Descripción||El sistema deberá comportarse como se describe a continuación cuando un proceso se cierra y el Sistema lanza automáticamente el escrutinio; después, un Auditor (ACT-3) o un Votante (ACT-4) verifica el resultado.||
|Precondición||El proceso está Cerrado.||
|Secuencia normal|Paso 1 2 3 4 5 6 7 8|Acción El Sistema lanza el escrutinio al cerrarse el proceso El Sistema obtiene los votos contabilizables (el último de cada votante). El Sistema cuenta los votos por opción. El Sistema calcula la participación. El Sistema guarda el resultado y publica su huella. El Auditor o el Votante solicita verificar el resultado El Sistema muestra los datos públicos necesarios para recalcularlo El Sistema informa de si el resultado coincide||
|Postcondición||Proceso en estado Escrutado con resultado y huella publicados.||
|Excepciones|Paso|Acción|Página 20 de 33|

||digital|Proyecto plataforma SaaS de votación SDRP Especificación de Requisitos|
|---|---|---|
||5 8|Si falla el recuento, el Sistema no publica nada, avisa al Administrador y permite reintentar. Si no coincide, el Sistema alerta de una incidencia|
|Comentarios||Prioridad M. Amplía RF-07. Queda por decidir qué significa «público»: cualquier persona o solo los participantes del proceso.|
|RF-14||Consultar panel de participación|
|Objetivos asociados|OBJ-4||
|Requisitos asociados||RI-3, RI-7|
|Descripción|proceso.|El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) consulta la participación de un|
|Precondición||El Administrador está autenticado.|
|Secuencia normal|Paso 1 2 3|Acción El Administrador solicita el panel de participación El Sistema muestra el número de votantes del censo, los votos emitidos y el porcentaje de participación. El Sistema actualiza el panel mientras el proceso está abierto.|
|Postcondición||Ninguna.|
|Excepciones|Paso 2 2|Acción Si el proceso aún no está abierto, el Sistema muestra solo el censo Si el proceso está cerrado, muestra la participación final|
|Comentarios||Prioridad M. Ninguna vista muestra el recuento por opción antes del cierre.|
|RF-15||Consultar y exportar resultados|
|Objetivos asociados|OBJ-4||
|Requisitos asociados|RI-10||
|Descripción||El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2), o un Votante (ACT-4) si el proceso lo permite, consulta y exporta los resultados.|
|Precondición||El proceso está Escrutado.|
|Secuencia normal|Paso 1 2 3 4 5|Acción El usuario solicita ver los resultados El Sistema comprueba que el usuario puede verlos El Sistema muestra los resultados con gráficos y participación. El usuario solicita exportarlos El Sistema genera el fichero en formato CSV y lo entrega.|
|Postcondición||Ninguna.|
|Excepciones|Paso|Acción Página 21 de 33|

SDRP digital Especificación de Requisitos 2 Si el proceso no está escrutado, el Sistema informa de que aún no hay resultados 2 Si el usuario no tiene permiso, el Sistema deniega el acceso **Comentarios** Prioridad M. Los resultados pueden ser consultados por el Administrador de organización y por los votantes cuando el proceso lo permita. La exportación de resultados se realiza únicamente en formato CSV.

|RF-16||Generar acta de resultados en PDF|
|---|---|---|
|Objetivos asociados|OBJ-4||
|Requisitos asociados|RI-10||
|Descripción||El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) genera el acta de un proceso.|
|Precondición||El proceso está Escrutado y el Administrador está autenticado.|
|Secuencia normal|Paso 1 2 3 4|Acción El Administrador solicita el acta El Sistema compone el acta (proceso, calendario, censo, participación, resultado y huellas) El Sistema genera el PDF y lo ofrece para descarga El Administrador lo descarga|
|Postcondición||Acta disponible en PDF con huella.|
|Excepciones|Paso 1 3|Acción Si el proceso no está escrutado, el Sistema informa de que el acta no está disponible Si falla la generación, el Sistema informa y permite reintentar|
|Comentarios||Prioridad S. Amplía RF-15. El contenido del acta debe validarse con las exigencias de cada organización.|

|RF-17||Enviar notificaciones||
|---|---|---|---|
|Objetivos asociados|OBJ-3|||
|Requisitos asociados|RI-11, RI-12|||
|Descripción||El sistema deberá comportarse como se describe a continuación cuando se programa, abre o cierra un proceso y el Sistema avisa a los votantes mediante el Servicio de notificaciones (ACT-7). El Administrador de organización (ACT-2) configura las plantillas y los canales.||
|Precondición|canales.|El proceso está programado y el Administrador ha configurado plantillas y||
|Secuencia normal|Paso 1 2 3|Acción El Sistema inicia el envío de invitaciones al programarse el proceso El Sistema genera el enlace personal de cada votante (RF-08) y compone el mensaje en su idioma El Sistema entrega los mensajes al Servicio de notificaciones|Página 22 de 33|

SDRP digital Especificación de Requisitos

|RF-18|Gestionar privacidad de datos (RGPD)||
|---|---|---|
|Objetivos asociados|OBJ-1||
|Requisitos asociados|RI-2, RI-3||
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un Administrador de organización (ACT-2) exporta o borra los datos personales de un interesado a petición de este.||
|Precondición|El Administrador está autenticado.||
|Secuencia normal|Paso 1 2.1 2.2 3.1 3.2|Acción El Administrador solicita la gestión de privacidad Si exporta datos de un interesado El Administrador identifica al interesado. El Sistema compila sus datos personales y los exporta en formato legible por máquina. Si borra datos de un interesado El Administrador identifica al interesado y confirma la solicitud de borrado. El Sistema borra o anonimiza sus datos personales.|
|Postcondición|Los datos del interesado han sido exportados o borrados, según la solicitud.||
|Excepciones|Paso 3.2|Acción Si el interesado ya votó, el Sistema anonimiza sus datos personales y conserva el voto y el registro de auditoría.|
|Comentarios|Prioridad S. El voto no es exportable por interesado porque no está vinculado a su identidad. Si el votante ya ha votado, se anonimizan sus datos personales y se conserva el voto.||

|RF-19|Consultar trazabilidad de acciones|
|---|---|
|Objetivos asociados|OBJ-4|
|Requisitos asociados|RI-13|
|Descripción|El sistema deberá comportarse como se describe a continuación cuando un|

||4|El Servicio de notificaciones informa del resultado de entrega|
|---|---|---|
||5 6 7|El Sistema registra el estado de cada envío En las fechas configuradas, el Sistema envía recordatorios a quien aún no ha votado Al cierre, el Sistema envía el aviso de cierre|
|Postcondición|Votantes avisados y estado de envío registrado.||
|Excepciones|Paso 4 2|Acción Si una entrega falla, el Sistema reintenta y la marca; el Administrador puede consultar los fallos Si un votante no tiene dato de contacto para el canal, el Sistema lo omite y lo lista.|
|Comentarios|Prioridad M. Amplía RF-08.||

Auditor (ACT-3) o un Administrador de organización (ACT-2) consulta las acciones administrativas registradas. Página 23 de 33

SDRP digital Especificación de Requisitos

|Precondición|El usuario está autenticado. El Sistema registra automáticamente cada||
|---|---|---|
||acción administrativa.||
|Secuencia normal|Paso 1 2 3 4|Acción El usuario solicita la trazabilidad El Sistema solicita filtros (fechas, usuario, tipo de acción, proceso) El usuario los introduce El Sistema muestra las acciones: quién, qué, sobre qué objeto y cuándo|
|Postcondición|Ninguna.||
|Excepciones|Paso 4|Acción Si no hay resultados, el Sistema informa|
|Comentarios|Prioridad S. El registro lo hace el Sistema en cada caso de uso de||

administración y no se puede editar. El Administrador solo ve las acciones de su organización.

## 3.3 Requisitos Funcionales del Sistema

Los requisitos funcionales del sistema (incluyendo los requisitos de reglas de negocio y los requisitos de conducta) se corresponden con cada uno de los casos de uso identificados en la Sección 3.2.3. La siguiente tabla los resume. Además, el sistema debe almacenar información, que se describe en los requisitos de información de la Sección

3.3.1.

|ID|Requisito funcional|Prioridad|Objetivos|
|---|---|---|---|
|RF-01|Gestionar organizaciones|M|OBJ-1|
|RF-02|Gestionar roles y permisos|M|OBJ-1|
|RF-03|Gestionar censo|M|OBJ-2|
|RF-04|Autenticar usuario de gestión|M|OBJ-1|
|RF-05|Iniciar sesión con SSO institucional|S|OBJ-1, OBJ-3|
|RF-06|Crear y configurar proceso de votación|M|OBJ-2|
|RF-07|Gestionar ciclo de vida del proceso|M|OBJ-2|
|RF-08|Acceder como votante|M|OBJ-3|
|RF-09|Emitir voto|M|OBJ-3|
|RF-10|Obtener y verificar recibo del voto|M|OBJ-3, OBJ-4|
|RF-11|Re-votar|C|OBJ-3|
|RF-12|Publicar y verificar registro de auditoría|S|OBJ-4|
|RF-13|Escrutar y verificar resultado|M|OBJ-4|
|RF-14|Consultar panel de participación|M|OBJ-4|
|RF-15|Consultar y exportar resultados|M|OBJ-4|
|RF-16|Generar acta de resultados en PDF|S|OBJ-4|
|RF-17|Enviar notificaciones|M|OBJ-3|
|RF-18|Gestionar privacidad de datos (RGPD)|S|OBJ-1|
|RF-19|Consultar trazabilidad de acciones|S|OBJ-4|

##### Página 24 de 33

||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos|
|---|---|
|identificados.|3.3.1 Requisitos de Información del Sistema Esta sección contiene los requisitos de almacenamiento de información (requisitos de información) que han sido|
|RI-1|Información sobre organizaciones|
|Objetivos asociados|OBJ-1|
|Requisitos asociados|RF-01|
|Descripción|El sistema deberá almacenar la información correspondiente a las organizaciones (tenants) que utilizan la plataforma.|
|Datos específicos|Identificador de organización: 20 caracteres alfanuméricos (único) Nombre visible: 100 caracteres Estado: Activa o Suspendida Fecha de alta: dd/mm/aaaa hh:mm|
|Comentarios|Los datos de cada organización están aislados lógicamente de los de las demás (arquitectura multi-tenant).|
|RI-2|Información sobre usuarios y roles|
|Objetivos asociados|OBJ-1|
|Requisitos asociados|RF-01, RF-02, RF-04, RF-05|
|Descripción|El sistema deberá almacenar la información correspondiente a los usuarios de gestión de la plataforma y a su rol.|
|Datos específicos|Identificador de usuario: entero positivo Organización: identificador de organización (RI-1) Nombre y apellidos: 60 caracteres Correo electrónico: 100 caracteres (único por organización) Teléfono: 15 dígitos (opcional) Rol: Superadministrador, Administrador o Auditor Contraseña: se almacena solo su huella (hash) Segundo factor activado: Sí o No Estado de la cuenta: Activa, Bloqueada o Inactiva Fecha del último acceso: dd/mm/aaaa hh:mm|
|Comentarios|Los votantes no son usuarios de gestión: se almacenan en el censo (RI-3). Se asume que un usuario pertenece a una única organización, salvo el Superadministrador, que pertenece a la plataforma.|
|RI-3|Información sobre censo|
|Objetivos asociados|OBJ-2|
|Requisitos asociados|RF-03, RF-05, RF-08, RF-14, RF-18|
|Descripción|El sistema deberá almacenar la información correspondiente a los votantes de cada organización.|
|Datos específicos|Identificador de votante: entero positivo Organización: identificador de organización (RI-1) Nombre y apellidos: 60 caracteres Página 25 de 33|
||Correo electrónico: 100 caracteres Teléfono: 15 dígitos (opcional) Identificador institucional: 50 caracteres (para el SSO, opcional)|
|Comentarios|El control de duplicados se hace por correo electrónico o por identificador institucional.|
|RI-4|Información sobre procesos de votación|
|Objetivos asociados|OBJ-2|
|Requisitos asociados|RF-06, RF-07, RF-08, RF-09, RF-12, RF-13, RF-14.|
|Descripción|El sistema deberá almacenar la información correspondiente a los procesos de votación y a su configuración.|
|Datos específicos|Identificador del proceso: entero positivo Organización: identificador de organización (RI-1) Título: 150 caracteres Descripción: 1000 caracteres Fecha de apertura: dd/mm/aaaa hh:mm Fecha de cierre: dd/mm/aaaa hh:mm (posterior a la de apertura) Estado: Borrador, Programado, Abierto, Cerrado, Escrutado o Archivado Re-votación permitida: Sí o No|
|Comentarios|El estado del proceso determina qué operaciones están permitidas (RF-08).|
|RI-5|Información sobre papeletas|
|Objetivos asociados|OBJ-2|
|Requisitos asociados|RF-06, RF-09, RF-10, RF-11|
|Descripción|El sistema deberá almacenar la información correspondiente a la papeleta de cada proceso, incluidas sus traducciones.|
|Datos específicos|Tipo de voto: Opción única o Sí/No/Abstención Pregunta: 300 caracteres Opciones (lista): texto de la opción (200 caracteres) y orden (entero positivo) Voto en blanco habilitado: Sí o No|
|Comentarios|Si falta una traducción, se muestra el texto en el idioma por defecto del proceso.|
|RI-6|Información sobre votos (urna)|
|Objetivos asociados|OBJ-3, OBJ-4|
|Requisitos asociados|RF-09, RF-10, RF-11, RF-13|
|Descripción|El sistema deberá almacenar los votos emitidos en cada proceso, sin ninguna relación con la identidad del votante.|
|Datos específicos|Identificador del voto: 36 caracteres (identificador aleatorio) Proceso: identificador del proceso (RI-4) Contenido del voto: opción elegida Fecha y hora de recepción: dd/mm/aaaa hh:mm:ss Canal: En línea Estado: Contabilizable o Sustituido Página 26 de 33|
|Comentarios|El voto no contiene ningún identificador ni dato de contacto del votante. Cuando un votante vuelve a votar (RF-11), el voto anterior pasa a Sustituido, sin borrarse. Solo se escrutan los votos Contabilizables.|
|RI-7|Información sobre participación|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-09, RF-11, RF-14|
|Descripción|El sistema deberá almacenar qué votantes han participado en cada proceso, sin indicar qué han votado.|
|Datos específicos|Proceso: identificador del proceso (RI-4) Votante: identificador de votante (RI-3) Ha votado: Sí o No Fecha y hora del último voto: dd/mm/aaaa hh:mm Canal del voto: En línea|
|Comentarios|Este registro se guarda separado de la urna (RI-6) y no enlaza con ningún voto concreto. Permite calcular la participación y evitar votos duplicados (RF-08/09).|
|RI-8|Información sobre recibos de verificación|
|Objetivos asociados|OBJ-3, OBJ-4|
|Requisitos asociados|RF-10, RF-11|
|Descripción|El sistema deberá almacenar la información correspondiente a los recibos que permiten al votante verificar su voto.|
|Datos específicos|Código del recibo: 64 caracteres hexadecimales Proceso: identificador del proceso (RI-4) Fecha y hora de emisión: dd/mm/aaaa hh:mm:ss Estado: Vigente o Sustituido|
|Comentarios|El recibo permite comprobar que el voto consta en la urna sin revelar su contenido. El recibo no se asocia a la identidad del votante.|
|RI-9|Información sobre registro de auditoría de la urna|
|Objetivos asociados|OBJ-4|
|Requisitos asociados|RF-07, RF-12|
|Descripción|El sistema deberá almacenar el registro encadenado de los eventos de la urna de cada proceso.|
|Datos específicos|Número de secuencia: entero positivo Proceso: identificador del proceso (RI-4) Tipo de evento: Apertura, Voto recibido, Voto sustituido, Cierre o Publicación de huella Fecha y hora: dd/mm/aaaa hh:mm:ss Huella del evento: 64 caracteres hexadecimales Huella del evento anterior: 64 caracteres hexadecimales Página 27 de 33|
||Huella final publicada: 64 caracteres hexadecimales (solo tras el cierre)|
|Comentarios|Cada evento incluye la huella del anterior, de modo que cualquier alteración rompe la cadena y se detecta al verificarla (RF-12). Los eventos de voto no incluyen datos del votante.|
|RI-10|Información sobre resultados y actas|
|Objetivos asociados|OBJ-4|
|Requisitos asociados|RF-13, RF-15, RF-16|
|Descripción|El sistema deberá almacenar el resultado del escrutinio de cada proceso y el acta generada.|
|Datos específicos|Proceso: identificador del proceso (RI-4) Fecha del escrutinio: dd/mm/aaaa hh:mm Resultado por opción (lista): opción, votos (entero positivo) y porcentaje (número real con 2 cifras decimales) Votos en blanco: entero positivo Participación: número real (con 2 cifras decimales, en porcentaje) Huella del resultado: 64 caracteres hexadecimales Acta: fichero PDF Fecha de generación del acta: dd/mm/aaaa hh:mm|
|Comentarios|El resultado solo existe cuando el proceso está escrutado.|
|RI-11|Información sobre notificaciones|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-17|
|Descripción|El sistema deberá almacenar la información correspondiente a las notificaciones enviadas a los votantes y su estado de entrega.|
|Datos específicos|Identificador de notificación: entero positivo Proceso: identificador del proceso (RI-4) Votante: identificador de votante (RI-3) Tipo: Invitación, Recordatorio, Cierre Canal: Correo electrónico Plantilla: texto de hasta 1000 caracteres Fecha y hora de envío: dd/mm/aaaa hh:mm Estado: Pendiente, Entregada o Fallida Número de reintentos: entero de 1 cifra|
|Comentarios|Las plantillas y los canales los configura el Administrador de organización. Las notificaciones fallidas se pueden consultar (RF-17).|
|RI-12|Información sobre acceso de votantes|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-17|
|Descripción|El sistema deberá almacenar la información necesaria para que cada votante acceda a la papeleta de un proceso abierto.|
|Datos específicos|Proceso: identificador del proceso (RI-4) Página 28 de 33|

|||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos Votante: identificador de votante (RI-3) Enlace personal: 64 caracteres (identificador aleatorio) Fecha de caducidad del enlace: dd/mm/aaaa hh:mm Código de verificación (OTP): 6 dígitos, se almacena solo su huella Fecha de caducidad del código: dd/mm/aaaa hh:mm Intentos fallidos: entero de 1 cifra Bloqueado hasta: dd/mm/aaaa hh:mm|
|---|---|---|
|Comentarios||El enlace personal identifica a un único votante en un único proceso. Tras varios intentos fallidos, el acceso se bloquea temporalmente.|
|RI-13||Información sobre trazabilidad de acciones administrativas|
||Objetivos asociados|OBJ-4|
||Requisitos asociados|RF-19|
|Descripción||El sistema deberá almacenar el registro de las acciones administrativas realizadas en la plataforma.|
|Datos específicos||Identificador de acción: entero positivo Fecha y hora: dd/mm/aaaa hh:mm:ss Usuario: identificador de usuario (RI-2) Organización: identificador de organización (RI-1) Tipo de acción: 50 caracteres Objeto afectado: 100 caracteres Detalle: 500 caracteres|
|Comentarios||El registro lo crea el Sistema automáticamente y no se puede modificar ni borrar. Es distinto del registro de auditoría de la urna (RI-10), que solo recoge eventos de la votación.|
|3.4|3.4.1 Requisitos de|Requisitos No Funcionales del Sistema En esta sección se describen los requisitos no funcionales del sistema, incluyendo aspectos relacionados con la usabilidad, portabilidad, fiabilidad, eficiencia, mantenibilidad y seguridad. Todos los requisitos no funcionales son medibles o se pueden comprobar mediante una prueba concreta, que se indica en Comentarios. Los valores numéricos son propuestos y deben validarse con el cliente. Usabilidad|
|RNF-1||Accesibilidad WCAG 2.2 AA|
||Objetivos asociados|OBJ-3|
||Requisitos asociados|RF-08, RF-09, RF-10, RF-11|
|Descripción||Las pantallas del votante deberán cumplir el nivel AA de las pautas WCAG 2.2: la auditoría automática no debe detectar ningún incumplimiento crítico y el flujo de acceso, voto y recibo debe poder completarse solo con teclado y con un lector de pantalla.|
|Comentarios||Verificación: auditoría automática (por ejemplo, axe o Lighthouse) y revisión manual con teclado y lector de pantalla. Sustituye al antiguo RF-18. Página 29 de 33|

||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos|
|---|---|
|RNF-2|Diseño adaptable y navegadores|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-09, RF-10, RF-11|
|Descripción|La interfaz deberá ser usable desde pantallas de 360 px de ancho, sin desplazamiento horizontal, y funcionar en las dos últimas versiones de Chrome, Firefox, Safari y Edge.|
|Comentarios|Verificación: pruebas en móvil, tableta y ordenador con cada navegador. Los valores (360 px, dos versiones) son propuestos. Sustituye al antiguo RF- 18.|
|RNF-3|Facilidad de uso del votante|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-09, RF-10, RF-11|
|Descripción|Un votante sin formación previa deberá completar el acceso, el voto y la obtención del recibo en 3 minutos o menos, y al menos el 90 % de los participantes en la prueba deberá lograrlo sin ayuda.|
|Comentarios|Verificación: test de usabilidad con al menos 5 personas no técnicas. Los valores son propuestos.|
|RNF-4|Facilidad de uso del administrador|
|Objetivos asociados|OBJ-1, OBJ-2|
|Requisitos asociados|RF-03, RF-06, RF-07|
|Descripción|Un administrador de organización sin formación previa deberá crear, configurar y programar un proceso de opción única en 10 minutos o menos, y al menos el 80 % de los participantes en la prueba deberá lograrlo sin ayuda.|
|Comentarios|Verificación: test de usabilidad con al menos 5 personas con perfil de administrador. Los valores son propuestos.|
|RNF-5|Idiomas de la interfaz|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-09|
|Descripción|La interfaz deberá estar disponible en español e inglés, con todos los textos en ficheros de recursos, de modo que añadir un idioma no exija cambiar código. Cambiar de idioma no debe hacer perder los datos introducidos.|
|Comentarios|Verificación: revisión de código y prueba de cambio de idioma durante el flujo de voto.|
|RNF-6|Claridad de la verificación del voto|
|Objetivos asociados|OBJ-3, OBJ-4|
|Requisitos asociados|RF-10, RF-13|
|Descripción|La pantalla de verificación del recibo y del resultado deberá explicarse en lenguaje no técnico, sin términos criptográficos en el mensaje principal, y al menos el 80 % de las personas no técnicas deberá interpretar correctamente si su voto consta en la urna. Página 30 de 33|
|Comentarios|Verificación: test con al menos 5 personas no técnicas. Responde al objetivo de mejorar la usabilidad de la verificación para votantes no técnicos. Los valores son propuestos.|
|RNF-7|Prevención de errores al votar|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-09, RF-11.|
|Descripción|Antes de registrar un voto, el sistema deberá mostrar siempre un resumen de la selección y pedir confirmación explícita, y avisar de cualquier selección no válida antes de avanzar de pantalla.|
|Comentarios|Verificación: prueba funcional con todos los tipos de papeleta (RF-09), incluidas selecciones no válidas.|
|RNF-8|Mensajes de error y realimentación|
|Objetivos asociados|OBJ-1, OBJ-2, OBJ-3, OBJ-4|
|Requisitos asociados|Todos los requisitos funcionales (RF-01 a RF-19)|
|Descripción|Todo error mostrado al usuario deberá indicar con lenguaje claro qué ha ocurrido y cómo corregirlo, sin códigos técnicos. Toda acción que tarde más de 1 segundo deberá mostrar un indicador de progreso y toda acción completada deberá mostrar una confirmación visible.|
|Comentarios|Verificación: revisión de los mensajes de las excepciones de los casos de uso y prueba cronometrada de las acciones lentas. El valor de 1 segundo es propuesto.|
|RNF-9|Orientación del votante durante el voto|
|Objetivos asociados|OBJ-3|
|Requisitos asociados|RF-08, RF-09, RF-11.|
|Descripción|En cada paso del voto (acceso, selección, confirmación y recibo), el votante deberá ver en qué paso está y cuántos quedan, y en todo momento el estado del proceso y su hora de cierre.|
|Comentarios|Verificación: revisión de las pantallas del flujo de voto y test de usabilidad (RNF-3).|
|RNF-10|Consistencia de la interfaz|
|Objetivos asociados|OBJ-1, OBJ-2, OBJ-3, OBJ-4|
|Requisitos asociados|RF-01 a RF-19|
|Descripción|La interfaz deberá usar la misma terminología, la misma ubicación de los elementos de navegación y los mismos componentes en todas las pantallas, sin problemas graves de consistencia en una revisión heurística.|
|Comentarios|Verificación: revisión heurística (heurísticas de Nielsen) por al menos 2 evaluadores, sin problemas de gravedad alta. Los valores son propuestos. Página 31 de 33|

|||Proyecto plataforma SaaS de votación SDRP digital Especificación de Requisitos|
|---|---|---|
|||3.4.2 Requisitos No Funcionales Técnicos|
|RNF-11||Secreto del voto|
||Objetivos asociados|OBJ-3, OBJ-4|
||Requisitos asociados|RF-09, RF-10, RF-11, RF-13|
|Descripción||Ninguna tabla enlaza voto e identidad. La urna (RI-6) y la participación (RI-7) están separadas.|
|Comentarios||Verificación: revisión del modelo de datos y prueba de no correlación.|
|RNF-12||Unicidad|
||Objetivos asociados|OBJ-3|
||Requisitos asociados|RF-09, RF-11|
|Descripción||El sistema deberá garantizar que solo un voto sea contabilizado por votante, incluso cuando se produzcan envíos simultáneos.|
|Comentarios||Verificación: prueba de envíos concurrentes del mismo votante para comprobar que únicamente un voto queda en estado Contabilizable.|
|RNF-13||Integridad y trazabilidad|
||Objetivos asociados|OBJ-4|
||Requisitos asociados|RF-12, RF-19|
|Descripción||El registro usa hash encadenado y cualquier alteración se detecta. Toda acción administrativa queda registrada y no es editable.|
|Comentarios||Verificación: prueba de alteración del registro y auditoría.|
|RNF-14||Seguridad de acceso|
||Objetivos asociados|OBJ-1, OBJ-3|
||Requisitos asociados|RF-04, RF-08|
|Descripción||Contraseñas con hash y sal. Bloqueo tras 5 intentos fallidos. Segundo factor. HTTPS. Sesión con caducidad.|
|Comentarios||Verificación: pruebas de seguridad. Valores propuestos.|
|RNF-15||Aislamiento entre organizaciones|
||Objetivos asociados|OBJ-1 Página 32 de 33|
||Requisitos asociados|RF-01|
|Descripción||Ningún usuario accede a datos de otra organización, comprobado con pruebas.|
|Comentarios||Verificación: pruebas de acceso cruzado entre tenants.|
|RNF-16||Protección de datos (RGPD)|
||Objetivos asociados|OBJ-1|
||Requisitos asociados|RF-18|
|Descripción||El sistema deberá limitar la recogida de datos personales a los estrictamente necesarios para la prestación del servicio y deberá permitir su eliminación cuando corresponda conforme a la normativa aplicable.|
|Comentarios||Verificación: revisión del modelo de datos y prueba del flujo de eliminación de datos personales.|
|RNF-17||Rendimiento|
||Objetivos asociados|OBJ-3|
||Requisitos asociados|RF-09, RF-13|
|Descripción||El sistema deberá soportar al menos 500 votantes simultáneos durante la emisión de votos y deberá responder en menos de 2 segundos en al menos el 95 % de las peticiones realizadas durante dicha carga.|
|Comentarios||Verificación: prueba de carga con al menos 500 usuarios simultáneos y medición del tiempo de respuesta. Los valores establecidos son propuestos y deberán validarse con el cliente.|
|RNF-18||Portabilidad (PWA instalable)|
||Objetivos asociados|OBJ-3|
||Requisitos asociados|RF-08, RF-09|
|Descripción||La aplicación deberá ser instalable como PWA en dispositivos móviles.|
|Comentarios||Prioridad C. Verificación: instalación en Android e iOS. Página 33 de 33|