// Local development only. Builds the M3 (heuristic review) manual for one evaluator, with the personal links of
// that evaluator's demo already filled in, from .local/demo-ual.md (written by seed-ual.mjs with DEMO_SUFFIX).
// The output stays in .local/m3/ (ignored by git): the links are local demo data and must not be committed.
//
// Usage, from the repository root, after `node scripts/demo/seed-ual.mjs` with DEMO_SUFFIX=E2:
//   node scripts/demo/manual-m3.mjs E2
// Then open .local/m3/manual-E2.html in Chrome.

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const ev = (process.argv[2] || '').trim();
if (!/^E\d$/.test(ev)) throw new Error('Usage: node scripts/demo/manual-m3.mjs E2');

const md = readFileSync(join(ROOT, '.local', 'demo-ual.md'), 'utf8');
if (!md.includes(` · ${ev}\n`)) {
  throw new Error(`.local/demo-ual.md is not the demo of ${ev}: run seed-ual.mjs with DEMO_SUFFIX=${ev} first`);
}
const stamp = (md.match(/Generado: (\d{4}-\d{2}-\d{2}T[\d:.]+Z)/) || [])[1];
const generated = stamp
  ? new Date(stamp).toLocaleString('es-ES', { timeZone: 'Europe/Madrid', dateStyle: 'long', timeStyle: 'short' }) : '?';
const links = {};
for (const m of md.matchAll(/\| `([a-z]+\.votante\d)@ual\.test` \| [^|]+\| (http:\/\/localhost:4200\/votar\/\S+) \|/g)) {
  links[m[1]] = m[2];
}
const needed = ['presente.votante5', 'presente.votante3', 'presente.votante1', 'presente.votante4',
  'futura.votante1', 'futura.votante2', 'pasada.votante5'];
for (const n of needed) if (!links[n]) throw new Error(`No link for ${n} in .local/demo-ual.md`);

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;');
const link = (who) => `<div class="link"><span class="who">${who}@ual.test</span>
  <a href="${links[who]}" target="_blank" rel="noopener">${esc(links[who])}</a>
  <button type="button" onclick="copy(this,'${links[who]}')">Copiar enlace</button></div>`;
const author = (html) => `<div class="author"><strong>⚠️ Avisa al autor.</strong> ${html}</div>`;
const done = (n) => `<label class="done"><input type="checkbox"> Prueba ${n} hecha</label>`;
const test = (n, title, body) => `<section class="test" id="p${n}"><h3><span class="n">${n}</span>${title}</h3>${body}${done(n)}</section>`;
const steps = (...s) => `<p class="lbl">Pasos</p><ol>${s.map((x) => `<li>${x}</li>`).join('')}</ol>`;
const look = (...s) => `<p class="lbl">Qué mirar</p><ul>${s.map((x) => `<li>${x}</li>`).join('')}</ul>`;
const mobile = '<p class="note">📱 En la pestaña nueva: <kbd>F12</kbd> y luego <kbd>Ctrl</kbd>+<kbd>Mayús</kbd>+<kbd>M</kbd> → 360 × 640.</p>';
const P_CURSO = `Representantes en Junta de Centro · ${ev}`;
const P_PASADA = `Delegación de curso 2026 · ${ev}`;
const CENSO = `Prueba ${ev}`;

const html = `<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>VeriVoto · Revisión heurística M3 · Manual de ${ev}</title>
<style>
  body { font-family: system-ui, Segoe UI, sans-serif; font-size: 17px; line-height: 1.5; color: #1b2733; margin: 0; background: #f3f5f8; }
  main { max-width: 900px; margin: 0 auto; padding: 24px 20px 80px; }
  h1 { color: #16365c; margin-bottom: 4px; } h2 { color: #16365c; margin-top: 40px; border-bottom: 2px solid #16365c; padding-bottom: 4px; }
  .sub { color: #4b5563; margin-top: 0; }
  .test { background: #fff; border: 1px solid #d5dbe3; border-radius: 10px; padding: 16px 20px; margin: 16px 0; }
  .test h3 { margin: 0 0 10px; display: flex; gap: 10px; align-items: center; }
  .n { background: #16365c; color: #fff; border-radius: 50%; min-width: 32px; height: 32px; display: inline-flex; align-items: center; justify-content: center; font-size: 15px; }
  .lbl { font-weight: 700; margin: 12px 0 4px; color: #16365c; }
  .link { background: #eef3f9; border-radius: 8px; padding: 10px 12px; margin: 8px 0; word-break: break-all; }
  .link .who { display: block; font-weight: 700; font-family: Consolas, monospace; }
  .link a { font-family: Consolas, monospace; font-size: 14px; }
  .link button { display: block; margin-top: 6px; font-size: 15px; padding: 6px 12px; cursor: pointer; }
  .author { background: #fff4d6; border-left: 5px solid #c98a00; padding: 10px 12px; margin: 10px 0; border-radius: 6px; }
  .note { background: #e6f4ee; padding: 8px 12px; border-radius: 6px; }
  .done { display: block; margin-top: 12px; color: #14573f; font-weight: 600; }
  table { border-collapse: collapse; width: 100%; background: #fff; } th, td { border: 1px solid #d5dbe3; padding: 6px 8px; text-align: left; vertical-align: top; }
  th { background: #e9edf2; } kbd { background: #e9edf2; border: 1px solid #c3cad3; border-radius: 4px; padding: 0 5px; font-size: 14px; }
  code { background: #e9edf2; padding: 1px 5px; border-radius: 4px; }
</style></head><body><main>
<h1>Revisión heurística de VeriVoto · Manual de ${ev}</h1>
<p class="sub">Demo cargada el ${esc(generated)}. Unos 90 minutos. Ve en orden: prueba 1, 2, 3…</p>

<h2>Antes de empezar (léelo una vez)</h2>
<ol>
  <li><strong>Revisas solo.</strong> El autor no te explica la interfaz. Solo te ayuda si algo técnico falla (un servicio caído, un enlace que no abre) y hace los pasos marcados con ⚠️.</li>
  <li><strong>Apunta solo problemas.</strong> Si una pantalla está bien, no escribas nada. Un problema = una fila en tu Excel.</li>
  <li><strong>Escribe lo que ves, con tus palabras.</strong> Antes de escribir «no hay…» o «no se puede…», desplázate por toda la pantalla y prueba el botón.</li>
  <li><strong>Gravedad 3 o 4:</strong> repite la prueba antes de ponerla.</li>
  <li><strong>Si una prueba no se puede hacer:</strong> una fila con <code>no revisado</code> en la descripción y el motivo en la propuesta.</li>
</ol>

<h3>Lo que tienes abierto</h3>
<ul>
  <li><strong>Mailpit</strong> (<a href="http://localhost:8025" target="_blank">http://localhost:8025</a>): ahí llegan todos los códigos de 6 cifras. Usa siempre <strong>el correo más reciente</strong> para ese votante o esa cuenta.</li>
  <li><strong>Tu Excel</strong> <code>hoja-${ev}.xlsx</code>: escribe solo en la pestaña «Hallazgos», en las celdas amarillas, y usa los desplegables.</li>
  <li><strong>Chrome:</strong> una ventana normal para el votante y ventanas de incógnito para la consola (las abre el autor).</li>
</ul>

<h3>Cómo rellenar una fila</h3>
<table>
  <tr><th>Columna</th><th>Qué poner</th></tr>
  <tr><td>evaluador / fecha / commit</td><td>${ev}, la fecha de hoy y el commit que te da el autor (igual en todas las filas)</td></tr>
  <tr><td>pantalla / ancho_px</td><td>Dónde lo ves (desplegable) y a qué ancho: 360 (móvil) o 1280 (ordenador)</td></tr>
  <tr><td>heuristica</td><td>La de Nielsen que falla (desplegable)</td></tr>
  <tr><td>descripcion</td><td>Qué pasa, en una o dos frases</td></tr>
  <tr><td>gravedad_0a4</td><td>0 no es problema · 1 cosmético · 2 menor · 3 mayor · 4 catástrofe</td></tr>
  <tr><td>rnf</td><td>RNF-8 si un error no dice qué pasó o qué hacer, o algo lento sin señal · RNF-9 si en el votante no ves el paso o el estado de la votación · RNF-10 si algo es distinto entre pantallas · Otro para lo demás</td></tr>
  <tr><td>propuesta</td><td>Cómo lo arreglarías (opcional)</td></tr>
</table>

<h3>En TODAS las pantallas del votante, mira también</h3>
<table>
  <tr><th>Pregunta</th><th>Si falla</th></tr>
  <tr><td>¿Se ve en qué paso estoy («Paso n de 4»)?</td><td>RNF-9</td></tr>
  <tr><td>¿Se ve el estado de la votación (abierta, hora de cierre)?</td><td>RNF-9</td></tr>
  <tr><td>Si hay un error, ¿dice qué ha pasado y qué hago ahora?</td><td>RNF-8</td></tr>
  <tr><td>¿Esto se ve o se llama distinto que en otra pantalla?</td><td>RNF-10</td></tr>
</table>

<h2>Parte 1 · Votante en móvil (360 px)</h2>
${mobile}

${test(1, 'Sesión caducada · parte A (dejarla esperando)', `<p>Pestaña nueva, <strong>solo para esta prueba</strong>.</p>${link('presente.votante5')}${steps(
  'Abre el enlace y pulsa «Empezar».', 'Escribe el código de Mailpit (el más reciente para <code>presente.votante5</code>).',
  'En la papeleta <strong>no toques nada</strong>. Apunta la hora: ________', 'Deja esta pestaña quieta y sigue con la prueba 2. Volverás en la prueba 10.')}`)}

<h2>Pruebas 2 a 8 · Recorrido completo con un votante</h2>
<p>Las siete en <strong>la misma pestaña nueva</strong>, una detrás de otra:</p>
${link('presente.votante3')}
${mobile}

${test(2, 'Bienvenida', `${steps('Abre el enlace. No pulses nada.')}${look('¿Se entiende qué es, hasta cuándo se puede votar y qué va a pasar?', '¿Se ve el botón para empezar sin desplazar la pantalla?', '¿Funcionan los botones ES / EN?')}`)}
${test(3, 'Código de acceso', `${steps('Pulsa «Empezar».', 'Toca la primera casilla.', 'Mira el texto bajo el botón «Continuar» y espera a que cambie.')}${look('¿Se entiende dónde llega el código, cuántas cifras tiene y cuánto dura?')}`)}
${test(4, 'Código incorrecto', `${steps('Escribe <code>000000</code> <strong>una sola vez</strong> (más veces bloquearía a este votante).')}${look('¿El mensaje dice qué ha pasado, qué hacer y cuántos intentos quedan?')}`)}
${test(5, 'Papeleta', `${steps('Escribe el código correcto de Mailpit (el más reciente).', 'Pulsa «Revisar mi voto» <strong>sin marcar nada</strong> y mira el error.', 'Marca una opción y después otra.')}${look('¿Se distingue cuál está marcada?', '¿El error dice qué hacer?')}`)}
${test(6, 'Revisión', `${steps('Pulsa «Revisar mi voto».', 'Busca cómo cambiar la elección, úsalo y vuelve a la revisión.')}${look('¿Se ve qué has elegido y que aún no has votado?')}`)}
${test(7, 'Recibo (aquí se vota de verdad)', `${steps('Pulsa «Confirmar y votar».', 'Usa el botón para copiar el código de recibo y pégalo en un bloc de notas.')}${look('¿Queda claro que has votado?', '¿Se ve el código de recibo entero?')}`)}
${test(8, 'Comprobación del recibo', `${steps('Pulsa «Comprobar mi recibo». Si el código no aparece escrito, pégalo y pulsa «Comprobar».', 'Escribe un código inventado de 64 caracteres y pulsa «Comprobar».', 'Escribe un código de 10 caracteres y pulsa «Comprobar».')}${look('Los mensajes de los tres casos: ¿se entienden?')}`)}

<h2>Parte 1 (sigue) · Otros votantes</h2>
${test(9, 'Ya has votado', `<p>Pestaña nueva.</p>${link('presente.votante1')}${mobile}${steps('Pulsa «Empezar» y escribe el código de Mailpit.')}${look('¿El mensaje dice que ya votó y qué puede hacer?')}`)}
${test(10, 'Sesión caducada · parte B', `<p><strong>Cuándo:</strong> cuando hayan pasado <strong>16 minutos</strong> desde la hora de la prueba 1.</p><p>Vuelve a <strong>la pestaña de la prueba 1</strong>. No abras el enlace otra vez.</p>${steps('Marca una opción y pulsa «Revisar mi voto».', 'Pulsa «Confirmar y votar».', 'Pulsa el botón que te ofrezca.')}${look('¿El mensaje dice qué ha pasado y si has votado o no?', '¿Dice cómo seguir?')}`)}
${test(11, 'Voto no registrado', `<p>El mismo votante de la prueba 1.</p>${link('presente.votante5')}${steps('Entra hasta la pantalla «Revisa tu voto». <strong>No pulses «Confirmar».</strong>',
  author('Él para un servicio. Espera a que te diga «listo».'), 'Pulsa «Confirmar y votar» y luego el botón que ofrezca.', author('Él vuelve a arrancar los servicios (1–2 min).'))}${look('¿El mensaje dice que el voto no se ha registrado y qué hacer?')}`)}
${test(12, 'Bloqueo por intentos', `<p>Pestaña nueva.</p>${link('presente.votante4')}${mobile}${steps('Pulsa «Empezar».', 'Escribe <code>000000</code> <strong>cinco veces</strong>, mirando el mensaje cada vez.', 'Pulsa el botón que ofrezca la pantalla de bloqueo.')}${look('¿Cada mensaje dice cuántos intentos quedan?', '¿La pantalla de bloqueo dice por qué y hasta cuándo?')}`)}
${test(13, 'Votación no abierta', `<p>Pestaña nueva.</p>${link('futura.votante1')}${mobile}${look('¿Se entiende que aún no se puede votar y cuándo se podrá?')}`)}
${test(14, 'Resultado', `<p>Pestaña nueva.</p>${link('pasada.votante5')}${mobile}${steps('Pulsa «Ver el resultado».', 'Escribe el código de Mailpit.')}${look('¿Se entiende el resultado y la participación?', '¿Se entiende qué significa «Resultado verificado»?')}`)}
${test(15, 'Lo mismo en ordenador (1280 px)', `<p>Hazla <strong>después</strong> de las pruebas 10 y 11. Quita el modo móvil (<kbd>Ctrl</kbd>+<kbd>Mayús</kbd>+<kbd>M</kbd> otra vez). El mismo votante de la prueba 1, que aún no ha votado:</p>${link('presente.votante5')}${steps('Mira la bienvenida.', '«Empezar» → código de Mailpit → mira la papeleta.', 'Marca una opción → «Revisar mi voto» → «Confirmar y votar» → mira el recibo.')}${look('Lo mismo que en las pruebas 2, 5 y 7, ahora en pantalla grande.')}`)}
${test(16, 'Demasiadas peticiones', `${author('Él reinicia los servicios con un límite bajo. Espera a que te diga «listo».')}<p>Pestaña nueva.</p>${link('futura.votante2')}${mobile}${steps('Pulsa <kbd>F5</kbd> (recargar) <strong>cuatro o cinco veces seguidas</strong>, rápido.')}${look('¿El mensaje dice qué ha pasado y cuánto esperar?', '¿Ofrece cómo seguir?')}${author('Él quita el límite y reinicia.')}`)}

<h2>Parte 2 · Consola de gestión (ordenador, 1280 px)</h2>
<p>Entrar: <a href="http://localhost:4200/admin" target="_blank">http://localhost:4200/admin</a> → usuario → la contraseña que te da el autor → código de 6 cifras de Mailpit. <strong>Cada cuenta en su ventana de incógnito.</strong></p>
<p class="note">En cada pantalla compara con las anteriores: ¿la misma palabra para la misma acción? ¿El botón principal en el mismo sitio? ¿Los mismos mensajes de error y de confirmación?</p>

<h3>Cuenta <code>admin@ual.test</code></h3>
${test(17, 'Inicio y «Primeros pasos»', `${steps('Entra con <code>admin@ual.test</code>.', 'Mira la pantalla de inicio: cifras, avisos y la guía «Primeros pasos».', 'Pulsa un enlace de la guía y vuelve al inicio.')}${look('¿Se entiende qué hacer primero?', '¿Vuelves sin perderte?')}`)}
${test(18, 'Participación', `${steps(`Menú → «Participación» → proceso «${P_CURSO}».`)}${look('¿Se entienden los números (votos, censo, %) y la gráfica por días?')}`)}
${test(19, 'Resultados y acta', `${steps(`Menú → «Resultados» → proceso «${P_PASADA}».`, 'Exporta el CSV y abre el fichero.', 'Genera el acta en PDF y ábrela.')}${look('¿Hay una señal mientras se genera?', '¿Los ficheros se entienden?')}`)}
${test(20, 'Claves de API', `${steps('Menú → «Claves de API» → crea una clave con cualquier nombre.', 'Fíjate en lo que avisa sobre la clave.', 'Revócala.')}${look('¿Se entiende qué hace una clave y qué pasa con el secreto?', '¿La confirmación de revocar se entiende?')}`)}
${test(21, 'Privacidad', `${steps('Menú → «Privacidad» → busca <code>presente.votante2@ual.test</code>.', 'Exporta sus datos y abre el fichero.', '<strong>No pulses borrar.</strong>')}${look('¿Se entiende qué datos tiene esa persona y qué puedes hacer?')}`)}

<h3>Cuenta <code>auditor@ual.test</code> (otra ventana de incógnito)</h3>
${test(22, 'Auditoría', `${steps('Entra con <code>auditor@ual.test</code>.', `Menú → «Auditoría» → proceso «${P_PASADA}».`, 'Pulsa «Verificar integridad ahora».')}${look('¿Se entiende el resultado y la tabla?')}`)}
${test(23, 'Trazabilidad', `${steps('Menú → «Trazabilidad».', 'Filtra por el día de hoy.', 'Filtra por una persona (por ejemplo <code>admin@ual.test</code>).', 'Pasa de página si hay más de una.')}${look('¿Se entienden los filtros?', '¿Se ve cuántos resultados hay?')}`)}

<h3>Cuenta <code>censo@ual.test</code> (otra ventana de incógnito)</h3>
${test(24, 'Censos e importación de Excel', `<p>Los dos Excel te los da el autor: <code>censo-prueba-${ev}-con-error.xlsx</code> y <code>censo-prueba-${ev}-correcto.xlsx</code>.</p>${steps('Entra con <code>censo@ual.test</code>.', `«Censos» → crea un censo llamado <strong>${CENSO}</strong> y ábrelo.`, 'Pulsa «Descargar plantilla» y ábrela.', `Sube <code>censo-prueba-${ev}-con-error.xlsx</code> y mira el mensaje.`, `Sube <code>censo-prueba-${ev}-correcto.xlsx</code>, mira la validación y <strong>confirma la importación</strong>.`)}${look('¿El error dice qué fila está mal y qué hacer?', '¿Se entiende qué se va a importar antes de confirmar?')}`)}
${test(25, 'Añadir, editar y borrar una persona', `${steps(`En el censo «${CENSO}», añade a mano una persona inventada.`, 'Edítala.', 'Bórrala.')}<p><strong>No borres a las tres personas importadas</strong>: se usan en la prueba 28.</p>${look('¿Las confirmaciones se entienden?')}`)}

<h3>Otra vez <code>admin@ual.test</code></h3>
${test(26, 'Asistente de proceso (4 pasos)', `${steps(`«Procesos» → nuevo proceso con el censo «${CENSO}». Recorre los 4 pasos.`, 'En el paso de fechas, pon un cierre <strong>anterior</strong> a la apertura y pulsa «Siguiente». Mira el mensaje.', 'Corrige las fechas: <strong>apertura dentro de 10 minutos</strong> y <strong>cierre 5 minutos después de la apertura</strong>. Ejemplo: si son las 17:10, apertura 17:20 y cierre 17:25.', 'Apunta la hora de cierre: ________', 'Crea el proceso.')}${look('¿El error de fechas dice qué hacer?', '¿Sabes en qué paso del asistente estás?')}`)}
${test(27, 'Programar el proceso', `${steps('En el proceso que acabas de crear, pulsa «Programar». Hazlo <strong>antes de la hora de apertura</strong>.')}${look('¿La confirmación y el estado se entienden?')}${author('Justo después de programar, él para un servicio para la prueba 28.')}`)}
${test(28, 'Votación cerrada', `${steps('En Mailpit, abre la invitación a una de las tres personas del censo importado y copia su enlace personal. <strong>No lo abras todavía.</strong>', 'Espera a la hora de cierre de la prueba 26 <strong>más 1 minuto</strong>.', 'Abre ese enlace en una pestaña nueva de la ventana normal, en móvil (360 px).')}${look('¿Se entiende que la votación está cerrada, desde cuándo y qué pasará después?', '¿Se ve el estado de la votación?')}${author('Él vuelve a arrancar los servicios.')}<p>Si no sale la pantalla de votación cerrada, una fila <code>no revisado</code> y avisa al autor.</p>`)}

<h2>Al terminar</h2>
<ol>
  <li>Repasa tu Excel: cada fila tiene pantalla, heurística, descripción, gravedad y RNF.</li>
  <li>Las pruebas que no pudiste hacer tienen su fila <code>no revisado</code>.</li>
  <li>Guarda el Excel y dáselo al autor. <strong>No lo compartas con nadie más.</strong></li>
</ol>
<p class="sub">Gracias por tu ayuda.</p>
</main>
<script>
function copy(btn, text) {
  navigator.clipboard.writeText(text).then(() => { const t = btn.textContent; btn.textContent = 'Copiado ✓'; setTimeout(() => { btn.textContent = t; }, 1500); });
}
</script>
</body></html>
`;

mkdirSync(join(ROOT, '.local', 'm3'), { recursive: true });
const out = join(ROOT, '.local', 'm3', `manual-${ev}.html`);
writeFileSync(out, html);
console.log(`Manual written: .local/m3/manual-${ev}.html (demo generated ${generated})`);
