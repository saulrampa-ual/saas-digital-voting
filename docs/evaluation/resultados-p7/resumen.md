# Resumen para el autor

La rama añade pruebas CSV/importación, una prueba HTTP parcial de autorización y aislamiento, inspección del esquema completo de ballot-box y dos casos de cadena. Las pruebas nuevas de election pasan; se añadieron Playwright 1.64.1 y axe 4.14.0 y cuatro exploraciones automáticas de la pantalla de comprobación de recibo.

Para continuar las comprobaciones manualmente, sigue `manual-simulacion-roles.md`: incluye cinco identidades sintéticas, pasos para crearlas al levantar los servicios, matriz de permisos y recorrido de una votación local. Las cuentas no estaban provisionadas al redactar el manual.

axe detecta incumplimientos críticos/serios en esa pantalla (incidencia DEF-01, origen probable en el selector de idioma compartido); H1a queda PENDIENTE y no puede aceptarse mientras siga abierta. No se cambió producción. H1c queda pendiente: k6 no está instalado y no se hicieron ejecuciones. Las pruebas no cubren todavía concurrencia, outbox, la mayoría de repositorios, el conjunto de roles/rutas internas, flujo completo ni servicios reales.

Revisión del autor (31/10/2026): DEF-02 se reclasifica como aclaración de D-33 (el enlace de la invitación va en la ruta por diseño) e IMP-43 queda como mejora P9. No se hizo commit, push ni merge. Los cambios locales preexistentes de `docs/p7-test-handoff` continúan intactos en su worktree.
