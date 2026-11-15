// RNF-4 · aggregated result of the repeated session with organization administrators, from the anonymous register
// sheet docs/evaluation/usabilidad/hoja-registro-rnf4.csv (one row per session; columns as in
// rnf4-protocolo-administradores.md). Criterion: at least 4 of 5 administrators WITHOUT previous training in VeriVoto
// create, configure and schedule a single-choice process in <= 600 s and without help (D-55: help from people; the
// documentation may be consulted). Fewer than 5 valid sessions: still not demonstrated. Reserves R1/R2 count only when
// used (valida = si). Prints the summary in Spanish. It never reads or prints names or emails: the sheet must not
// contain them.
//
// Usage: node scripts/evaluation/rnf4-summary.mjs [path-to-csv]
// Values: si/no (also sí, yes, x); t0_t1_segundos in whole seconds, measured with a stopwatch.

import { readFileSync } from 'node:fs';

const path = process.argv[2] ?? 'docs/evaluation/usabilidad/hoja-registro-rnf4.csv';
const [header, ...lines] = readFileSync(path, 'utf8').replace(/^﻿/, '').split(/\r?\n/).filter((l) => l.trim());
const cols = header.split(',').map((c) => c.trim());
const yes = (v) => /^(s[ií]|yes|y|x|true|1)$/i.test((v ?? '').trim());
const filled = (v) => (v ?? '').trim() !== '';
const LIMIT = 600;

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
  for (const c of ['sin_formacion_previa', 'completo', 'sin_ayuda']) if (!filled(r[c])) problems.push(`${r.id}: falta ${c}`);
  if (filled(r.sin_formacion_previa) && !yes(r.sin_formacion_previa)) {
    problems.push(`${r.id}: tiene formación previa; según el protocolo no participa (valida = no o usar una reserva)`);
  }
}
if (problems.length) {
  console.log('Hoja incompleta o incoherente; corrige estas filas antes de calcular:\n- ' + problems.join('\n- '));
  process.exit(1);
}

const n = valid.length;
const secs = valid.map((r) => Number(r.t0_t1_segundos)).sort((a, b) => a - b);
const median = n ? (n % 2 ? secs[(n - 1) / 2] : (secs[n / 2 - 1] + secs[n / 2]) / 2) : null;
const success = valid.filter((r) => yes(r.completo) && yes(r.sin_ayuda) && Number(r.t0_t1_segundos) <= LIMIT).length;
const completed = valid.filter((r) => yes(r.completo)).length;
const unaided = valid.filter((r) => yes(r.sin_ayuda)).length;
const docs = valid.filter((r) => yes(r.consulto_documentacion)).length;
const reserves = valid.filter((r) => /^R/i.test(r.id)).length;
const environment = invalid.filter((r) => filled(r.incidencia_entorno) && !/^no$/i.test(r.incidencia_entorno.trim())).length;
const errors = valid.reduce((t, r) => t + (Number(r.errores) || 0), 0);
const minutes = (s) => (s === null || s === undefined ? '—' : `${s} s (${(s / 60).toFixed(1).replace('.', ',')} min)`);

let decision;
if (n < 5) decision = `SIN DEMOSTRAR (no concluyente): solo ${n} sesiones válidas de 5`;
else if (n > 5) decision = `REVISAR: ${n} sesiones válidas; el protocolo prevé exactamente 5`;
else if (success >= 4) decision = `RNF-4 ACEPTADO (${success} de 5 sin formación previa completan la tarea sin ayuda en ≤ 10 min)`;
else decision = `RNF-4 RECHAZADO (${success} de 5; el criterio pide al menos 4 de 5)`;

console.log(`Resumen RNF-4 (${path})
- Sesiones válidas: ${n} de 5, todas sin formación previa (reservas usadas: ${reserves}; invalidadas por el entorno: ${environment}).
- Completan crear, configurar y programar: ${completed} de ${n}. Sin ayuda de personas: ${unaided} de ${n}. Consultaron la documentación: ${docs} de ${n}.
- Éxito (completa, sin ayuda y ≤ 600 s): ${success} de ${n}. Criterio: al menos 4 de 5.
- Duración T0→T1 (cronometrada, descriptiva): mediana ${minutes(median)}; mínimo ${minutes(secs[0])}; máximo ${minutes(secs[n - 1])}.
- Errores observados (suma): ${errors}.
- Decisión: ${decision}.
- Con n = 5 no hay inferencia estadística: evidencia descriptiva limitada al prototipo, la configuración y la población evaluados.`);
