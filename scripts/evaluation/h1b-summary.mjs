// H1b (RNF-3, RNF-6) · aggregated result of the voter usability sessions from the anonymous register sheet
// docs/evaluation/usabilidad/hoja-registro-h1b.csv (one row per session; columns as in hoja-registro.md).
// Criterion validated by the tutor: the 5 valid participants complete access, casting and receipt in <= 180 s without
// help (5 of 5), and at least 4 of 5 interpret the verification correctly. Fewer than 5 valid sessions: PENDING.
// Reserves R1/R2 count only when used (valida = si). Prints the summary in Spanish, ready for hoja-registro.md and
// informe.md. It never reads or prints names, emails, codes or receipts: the sheet must not contain them.
//
// Usage: node scripts/evaluation/h1b-summary.mjs [path-to-csv]
// Values: si/no (also sí, yes, x); t0_t1_segundos in whole seconds.

import { readFileSync } from 'node:fs';

const path = process.argv[2] ?? 'docs/evaluation/usabilidad/hoja-registro-h1b.csv';
const [header, ...lines] = readFileSync(path, 'utf8').replace(/^﻿/, '').split(/\r?\n/).filter((l) => l.trim());
const cols = header.split(',').map((c) => c.trim());
const yes = (v) => /^(s[ií]|yes|y|x|true|1)$/i.test((v ?? '').trim());
const filled = (v) => (v ?? '').trim() !== '';

// Simple CSV: the observation column may contain commas only if quoted
function parse(line) {
  const out = []; let cur = ''; let quoted = false;
  for (const ch of line) {
    if (ch === '"') quoted = !quoted;
    else if (ch === ',' && !quoted) { out.push(cur); cur = ''; }
    else cur += ch;
  }
  out.push(cur);
  return Object.fromEntries(cols.map((c, i) => [c, out[i] ?? '']));
}

const rows = lines.map(parse).filter((r) => filled(r.valida));
const valid = rows.filter((r) => yes(r.valida));
const invalid = rows.filter((r) => !yes(r.valida));
const problems = [];
for (const r of valid) {
  if (!/^\d+$/.test(r.t0_t1_segundos.trim())) problems.push(`${r.id}: t0_t1_segundos vacío o no entero`);
  for (const c of ['sin_ayuda', 'comprension_correcta']) if (!filled(r[c])) problems.push(`${r.id}: falta ${c}`);
}
if (problems.length) {
  console.log('Hoja incompleta; corrige estas filas antes de calcular:\n- ' + problems.join('\n- '));
  process.exit(1);
}

const n = valid.length;
const secs = valid.map((r) => Number(r.t0_t1_segundos)).sort((a, b) => a - b);
const median = n ? (n % 2 ? secs[(n - 1) / 2] : (secs[n / 2 - 1] + secs[n / 2]) / 2) : null;
const success = valid.filter((r) => yes(r.sin_ayuda) && Number(r.t0_t1_segundos) <= 180).length;
const understood = valid.filter((r) => yes(r.comprension_correcta)).length;
const reserves = valid.filter((r) => /^R/i.test(r.id)).length;
const environment = invalid.filter((r) => filled(r.incidencia_entorno) && !/^no$/i.test(r.incidencia_entorno.trim())).length;
const errors = valid.reduce((t, r) => t + (Number(r.errores) || 0), 0);

let decision;
if (n < 5) decision = `PENDIENTE (no concluyente): solo ${n} sesiones válidas de 5`;
else if (n > 5) decision = `REVISAR: ${n} sesiones válidas; el protocolo prevé exactamente 5`;
else if (success === 5 && understood >= 4) decision = 'H1b ACEPTADA (5 de 5 en ≤ 180 s sin ayuda y al menos 4 de 5 interpretan el recibo)';
else decision = 'H1b RECHAZADA, se mantiene H0b (no se alcanza alguno de los dos criterios)';

console.log(`Resumen H1b (${path})
- Sesiones válidas: ${n} de 5 (reservas usadas: ${reserves}; sesiones invalidadas por el entorno: ${environment}).
- Éxito sin ayuda en ≤ 180 s: ${success} de ${n}. Criterio: 5 de 5.
- Comprensión correcta del recibo: ${understood} de ${n}. Criterio: al menos 4 de 5.
- Duración T0→T1 (descriptiva): mediana ${median ?? '—'} s; mínimo ${secs[0] ?? '—'} s; máximo ${secs[n - 1] ?? '—'} s.
- Errores observados (suma): ${errors}.
- Decisión: ${decision}.
- Con n = 5 no hay inferencia estadística: evidencia descriptiva limitada al prototipo, la configuración y la población evaluados.`);
