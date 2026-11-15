// Uso desde la raíz del repo: node scripts/memoria/cu-a-tabla.mjs Plantilla-TFM-Informatica-UAL/especificacion_detallada.tex RF-01,RF-09 --write
// Converts the use cases of especificacion_detallada.tex (\subsection{RF-xx: …} blocks with \textbf{Campo:} lines,
// an enumerate for the normal flow and an itemize for the exceptions) into the table format requested by the tutor.
//   node cu-a-tabla.mjs <especificacion_detallada.tex> [RF-01,RF-09]   → prints the tables (all RF if no list)
//   node cu-a-tabla.mjs <file> [RF-01,RF-09] --write                  → rewrites the file in place (keep a git diff!)
// Text not recognised is kept under «Otros» so nothing is lost silently; the script reports it.
import { readFileSync, writeFileSync } from 'node:fs';

const [file, ...args] = process.argv.slice(2);
const write = args.includes('--write');
const list = args.find((a) => a !== '--write');
if (!file) { console.error('usage: node cu-a-tabla.mjs <file.tex> [RF-01,RF-09] [--write]'); process.exit(1); }
const raw = readFileSync(file, 'utf8');
const crlf = raw.includes('\r\n');
const src = raw.replace(/\r\n/g, '\n');
const only = list ? new Set(list.split(',')) : null;

// Block = from a \subsection{RF-…} heading to the next \subsection/\section/\chapter or end of file
const HEAD = /\\subsection\{(RF-[0-9]+[a-z]?):\s*([^}]*)\}\n/g;
const blocks = [];
for (let m; (m = HEAD.exec(src));) {
  const start = m.index, bodyStart = m.index + m[0].length;
  const rest = src.slice(bodyStart);
  const next = rest.search(/\\(sub)?section\{|\\chapter\{/);
  blocks.push({ id: m[1], name: m[2].trim(), start, end: next < 0 ? src.length : bodyStart + next, body: next < 0 ? rest : rest.slice(0, next) });
}

const FIELDS = ['Objetivos asociados', 'Requisitos asociados', 'Descripción', 'Precondición', 'Postcondición', 'Comentarios'];
function parse(b) {
  const out = { fields: {}, flow: [], exceptions: [], other: [] };
  let body = b.body;
  body = body.replace(/\\textbf\{Flujo normal:\}\s*\\begin\{enumerate\}([\s\S]*?)\\end\{enumerate\}/, (_, items) => {
    for (const line of items.split('\n').map((l) => l.trim()).filter(Boolean)) {
      const it = line.match(/^\\item\[([^\]]*)\]\s*(.*)$/);
      if (!it) { out.other.push(line); continue; }
      const branch = it[2].match(/^\\textit\{(.*)\}$/);
      out.flow.push(it[1] ? { step: it[1], text: it[2] } : { branch: branch ? branch[1] : it[2] });
    }
    return '';
  });
  body = body.replace(/\\textbf\{Excepciones y alternativas:\}\s*\\begin\{itemize\}([\s\S]*?)\\end\{itemize\}/, (_, items) => {
    for (const line of items.split(/\n(?=\s*\\item)/).map((l) => l.trim()).filter(Boolean)) {
      const it = line.match(/^\\item\s+([0-9.]+[a-z]?)\s*:\s*([\s\S]*)$/);
      if (it) out.exceptions.push({ step: it[1], text: it[2].replace(/\s+/g, ' ') });
      else out.other.push(line);
    }
    return '';
  });
  for (const f of FIELDS) {
    const re = new RegExp(`\\\\textbf\\{${f}:\\}\\s*([\\s\\S]*?)\\\\par`);
    body = body.replace(re, (_, v) => { out.fields[f] = v.trim(); return ''; });
  }
  const left = body.replace(/\\par/g, '').trim();
  if (left) out.other.push(left);
  return out;
}

// Actor of the use case: the «cuando un X (ACT-n)» part of the description
function actors(desc = '') {
  const m = desc.match(/cuando (?:un|una|el|la|se)\s+(.*?)(?:\s+(?:da|asigna|crea|programa|accede|emite|quiere|cambia|consulta|genera|exporta|importa|inicia|solicita|lanza)\b|$)/);
  return m ? m[1].replace(/\s*\(ACT-\d+\)/g, (s) => s) : '';
}

const esc = (s) => s; // texts are already LaTeX
function table(b, p) {
  const r = [];
  const row = (label, text) => r.push(`  \\cuLabel{${label}} & \\multicolumn{2}{p{\\cuWide}|}{${esc(text)}} \\\\ \\hline`);
  r.push(`\\begin{longtable}{|p{\\cuLab}|p{\\cuNum}|p{\\cuText}|}`);
  r.push(`  \\caption{${b.id}: ${b.name}.}\\label{tab:cu-${b.id.toLowerCase()}}\\\\`);
  r.push(`  \\hline \\multicolumn{3}{|>{\\columncolor{cuHead}}p{\\cuAll}|}{\\textbf{${b.id}: ${b.name}}} \\\\ \\hline \\endfirsthead`);
  r.push(`  \\hline \\multicolumn{3}{|>{\\columncolor{cuHead}}p{\\cuAll}|}{\\textbf{${b.id}: ${b.name}} (continuación)} \\\\ \\hline \\endhead`);
  const act = actors(p.fields['Descripción']);
  if (act) row('Actores', act);
  if (p.fields['Objetivos asociados']) row('Objetivos', p.fields['Objetivos asociados']);
  if (p.fields['Requisitos asociados']) row('Requisitos', p.fields['Requisitos asociados']);
  if (p.fields['Descripción']) row('Descripción', p.fields['Descripción']);
  if (p.fields['Precondición']) row('Precondición', p.fields['Precondición']);
  if (p.flow.length) {
    r.push(`  \\cuLabel{Flujo normal} & \\textbf{Paso} & \\textbf{Acción} \\\\ \\cline{2-3}`);
    p.flow.forEach((s, i) => {
      const last = i === p.flow.length - 1;
      r.push(s.branch !== undefined
        ? `  \\cuLabel{} & \\multicolumn{2}{p{\\cuWide}|}{\\textit{${s.branch}}} \\\\ ${last ? '\\hline' : '\\cline{2-3}'}`
        : `  \\cuLabel{} & ${s.step} & ${s.text} \\\\ ${last ? '\\hline' : '\\cline{2-3}'}`);
    });
  }
  if (p.fields['Postcondición']) row('Postcondición', p.fields['Postcondición']);
  if (p.exceptions.length) {
    r.push(`  \\cuLabel{Excepciones} & \\textbf{Paso} & \\textbf{Acción} \\\\ \\cline{2-3}`);
    p.exceptions.forEach((e, i) => r.push(`  \\cuLabel{} & ${e.step} & ${e.text} \\\\ ${i === p.exceptions.length - 1 ? '\\hline' : '\\cline{2-3}'}`));
  }
  if (p.fields['Comentarios']) row('Comentarios', p.fields['Comentarios']);
  if (p.other.length) row('Otros', p.other.join(' '));
  r.push(`\\end{longtable}`);
  return r.join('\n');
}

let result = src, report = [];
for (const b of [...blocks].reverse()) {
  if (only && !only.has(b.id)) continue;
  if (!/\\textbf\{Flujo normal:\}/.test(b.body)) { report.push(`${b.id}: sin «Flujo normal», se deja igual`); continue; }
  const p = parse(b);
  if (p.other.length) report.push(`${b.id}: ${p.other.length} fragmento(s) no reconocidos, en la fila «Otros»`);
  const heading = `\\subsection{${b.id}: ${b.name}}\n`;
  result = result.slice(0, b.start) + heading + table(b, p) + '\n\n' + result.slice(b.end);
  if (!write) console.log(heading + table(b, p) + '\n');
}
if (write) writeFileSync(file, crlf ? result.replace(/\n/g, '\r\n') : result, 'utf8');
console.error(report.length ? report.join('\n') : 'todos los CU convertidos sin restos');
