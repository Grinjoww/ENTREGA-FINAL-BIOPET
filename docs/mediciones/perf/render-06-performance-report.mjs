// Genera docs/informe/figuras/fred/06-performance-report.png a partir de los
// datos FINALES de rendimiento entregados por Fred en docs/mediciones/perf/REPORT.md
// (10 corridas reales de k6, v1.0.0). No inventa valores: parsea la tabla
// Markdown directamente y renderiza la misma informacion que aparece en la
// Tabla "Resultado real de las diez corridas de k6" de
// docs/informe/secciones-final/07-pruebas-calidad.tex.
//
// Autoria: Zaida Taipe (hallazgo #12 del plan de recalificacion — figuras
// de rendimiento/Lighthouse faltantes, "Zaida con datos finales").
// No modifica ni sustituye scripts/perf-analysis.py (zona de Fred).
//
// Uso:
//   node docs/mediciones/perf/render-06-performance-report.mjs
// Requiere (instalados como devDependencies locales, no se versionan node_modules):
//   npm install @hpcc-js/wasm @resvg/resvg-js
//
// Salida: docs/informe/figuras/fred/06-performance-report.png

import { readFileSync, writeFileSync } from "fs";
import { fileURLToPath } from "url";
import path from "path";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "..", "..", "..");
const reportPath = path.join(repoRoot, "docs/mediciones/perf/REPORT.md");
const outPng = path.join(repoRoot, "docs/informe/figuras/fred/06-performance-report.png");
const outSvg = outPng.replace(/\.png$/, ".svg");

const md = readFileSync(reportPath, "utf8");

// --- Parse the first Markdown table (per-run results) ----------------------
const lines = md.split(/\r?\n/);
const headerIdx = lines.findIndex((l) => l.startsWith("| Corrida "));
if (headerIdx === -1) throw new Error("No se encontro la tabla de corridas en REPORT.md");
const rows = [];
for (let i = headerIdx + 2; i < lines.length; i++) {
  const line = lines[i];
  if (!line.startsWith("|")) break;
  const cells = line.split("|").slice(1, -1).map((c) => c.trim());
  const [file, n, mean, median, sd, ci, p50, p90, p95, p99, err, thr] = cells;
  const modo = /caliente/.test(file) ? "warm" : "cold";
  const corridaNum = file.match(/-(\d\d)\.json$/)?.[1] ?? "";
  rows.push({
    label: `${modo}-${corridaNum}`,
    modo,
    n: Number(n),
    mean: Number(mean),
    median: Number(median),
    p95: Number(p95),
    p99: Number(p99),
    err: Number(err),
    throughput: Number(thr),
  });
}
if (rows.length !== 10) throw new Error(`Se esperaban 10 corridas, se parsearon ${rows.length}`);

// Order: 5 warm then 5 cold, matching the report table (caliente-01..05, frio-01..05)
rows.sort((a, b) => (a.modo === b.modo ? a.label.localeCompare(b.label) : a.modo === "warm" ? -1 : 1));

const maxP99 = Math.max(...rows.map((r) => r.p99));
const chartW = 900, chartH = 260, chartLeft = 60, chartRight = 900 - 20, chartTop = 80, chartBottom = 290;
const barGroupW = (chartRight - chartLeft) / rows.length;
const barW = barGroupW * 0.28;

function y(val) {
  return chartBottom - (val / maxP99) * (chartBottom - chartTop);
}

const colWarm = "#2e7d32";
const colCold = "#e65100";
const colP99 = "#94a3b8";

let bars = "";
rows.forEach((r, i) => {
  const cx = chartLeft + i * barGroupW + barGroupW / 2;
  const color = r.modo === "warm" ? colWarm : colCold;
  // p95 bar
  const x95 = cx - barW - 2;
  bars += `<rect x="${x95.toFixed(1)}" y="${y(r.p95).toFixed(1)}" width="${barW.toFixed(1)}" height="${(chartBottom - y(r.p95)).toFixed(1)}" fill="${color}" opacity="0.85"/>`;
  bars += `<text x="${(x95 + barW / 2).toFixed(1)}" y="${(y(r.p95) - 4).toFixed(1)}" text-anchor="middle" font-family="Arial" font-size="9">${r.p95}</text>`;
  // p99 bar
  const x99 = cx + 2;
  bars += `<rect x="${x99.toFixed(1)}" y="${y(r.p99).toFixed(1)}" width="${barW.toFixed(1)}" height="${(chartBottom - y(r.p99)).toFixed(1)}" fill="${colP99}" opacity="0.9"/>`;
  bars += `<text x="${(x99 + barW / 2).toFixed(1)}" y="${(y(r.p99) - 4).toFixed(1)}" text-anchor="middle" font-family="Arial" font-size="9">${r.p99}</text>`;
  bars += `<text x="${cx.toFixed(1)}" y="${chartBottom + 16}" text-anchor="middle" font-family="Arial" font-size="10" font-weight="bold">${r.label}</text>`;
});

let gridLines = "";
for (let g = 0; g <= 4; g++) {
  const val = (maxP99 / 4) * g;
  const gy = y(val);
  gridLines += `<line x1="${chartLeft}" y1="${gy.toFixed(1)}" x2="${chartRight}" y2="${gy.toFixed(1)}" stroke="#e2e8f0" stroke-width="1"/>`;
  gridLines += `<text x="${chartLeft - 6}" y="${(gy + 3).toFixed(1)}" text-anchor="end" font-family="Arial" font-size="10" fill="#64748b">${val.toFixed(0)}</text>`;
}

// --- Summary table (matches the report table shown in 07-pruebas-calidad.tex) ---
const tableTop = 330;
const rowH = 20;
const cols = [
  ["Run", (r) => r.label],
  ["n", (r) => r.n],
  ["Mean (ms)", (r) => r.mean.toFixed(2)],
  ["Median (ms)", (r) => r.median.toFixed(2)],
  ["p95 (ms)", (r) => r.p95.toFixed(2)],
  ["p99 (ms)", (r) => r.p99.toFixed(2)],
  ["Error (%)", (r) => r.err.toFixed(1)],
  ["Throughput (req/s)", (r) => r.throughput.toFixed(2)],
];
const colX = [40, 130, 190, 270, 360, 440, 520, 610];
const tableWidth = 860;

let tableHeader = "";
cols.forEach(([label], i) => {
  tableHeader += `<text x="${colX[i]}" y="${tableTop}" font-family="Arial" font-size="11" font-weight="bold" fill="#1e293b">${label}</text>`;
});

let tableRows = "";
rows.forEach((r, ri) => {
  const ry = tableTop + rowH * (ri + 1);
  if (ri % 2 === 0) {
    tableRows += `<rect x="30" y="${ry - 14}" width="${tableWidth}" height="${rowH}" fill="#f8fafc"/>`;
  }
  cols.forEach(([, fn], ci) => {
    tableRows += `<text x="${colX[ci]}" y="${ry}" font-family="Arial" font-size="10.5" fill="#0f172a">${fn(r)}</text>`;
  });
});

const totalErrPct = (rows.reduce((s, r) => s + r.err, 0) / rows.length).toFixed(1);
const avgThroughput = (rows.reduce((s, r) => s + r.throughput, 0) / rows.length).toFixed(2);

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="920" height="${tableTop + rowH * (rows.length + 2) + 20}" viewBox="0 0 920 ${tableTop + rowH * (rows.length + 2) + 20}">
<rect x="0" y="0" width="920" height="${tableTop + rowH * (rows.length + 2) + 20}" fill="white"/>
<text x="460" y="26" text-anchor="middle" font-family="Arial" font-size="18" font-weight="bold" fill="#0f172a">BIOPET - Aggregated Performance Report (k6, v1.0.0)</text>
<text x="460" y="44" text-anchor="middle" font-family="Arial" font-size="11" fill="#475569">GET /api/mascotas over HTTPS/TLS 1.3 - 50 VUs - 5 warm + 5 cold runs - source: docs/mediciones/perf/REPORT.md</text>
${gridLines}
${bars}
<text x="${chartLeft}" y="${chartTop - 10}" font-family="Arial" font-size="11" fill="#334155">Latency (ms)</text>
<rect x="${chartRight - 220}" y="${chartTop - 22}" width="12" height="12" fill="${colWarm}"/>
<text x="${chartRight - 204}" y="${chartTop - 12}" font-family="Arial" font-size="10">p95 - warm run</text>
<rect x="${chartRight - 220}" y="${chartTop - 8}" width="12" height="12" fill="${colCold}"/>
<text x="${chartRight - 204}" y="${chartTop + 2}" font-family="Arial" font-size="10">p95 - cold run</text>
<rect x="${chartRight - 90}" y="${chartTop - 22}" width="12" height="12" fill="${colP99}"/>
<text x="${chartRight - 74}" y="${chartTop - 12}" font-family="Arial" font-size="10">p99 (both)</text>
<line x1="30" y1="${tableTop - 16}" x2="890" y2="${tableTop - 16}" stroke="#cbd5e1" stroke-width="1"/>
${tableHeader}
<line x1="30" y1="${tableTop + 6}" x2="890" y2="${tableTop + 6}" stroke="#cbd5e1" stroke-width="1"/>
${tableRows}
<line x1="30" y1="${tableTop + rowH * (rows.length + 1) - 8}" x2="890" y2="${tableTop + rowH * (rows.length + 1) - 8}" stroke="#cbd5e1" stroke-width="1"/>
<text x="30" y="${tableTop + rowH * (rows.length + 2) + 4}" font-family="Arial" font-size="10.5" fill="#334155">Overall: error rate ${totalErrPct}% across all 10 runs - mean throughput ${avgThroughput} req/s</text>
</svg>`;

writeFileSync(outSvg, svg, "utf8");
console.log("SVG written:", outSvg);

const { Resvg } = await import("@resvg/resvg-js");
const resvg = new Resvg(svg, { fitTo: { mode: "width", value: 2000 }, background: "white" });
const png = resvg.render().asPng();
writeFileSync(outPng, png);
console.log("PNG written:", outPng, png.length, "bytes");
