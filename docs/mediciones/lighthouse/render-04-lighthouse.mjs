// Genera docs/informe/figuras/zaida/04-lighthouse.png a partir de los 12 JSON
// REALES de la corrida Lighthouse contra el despliegue publico de Render
// (docs/mediciones/lighthouse/lhci-<STAMP>-render-*.json). No inventa
// valores: lee los campos categories.*.score de cada JSON y promedia por
// perfil/ruta, igual que compute-render-averages.mjs (paso 25 del plan de
// recalificacion), y ademas verifica requestedUrl.
//
// Autoria: Zaida Taipe (hallazgo #12 del plan de recalificacion — figura
// de Lighthouse, desbloqueada tras completar Z3 con la corrida real contra
// Render).
//
// Uso:
//   node docs/mediciones/lighthouse/render-04-lighthouse.mjs <STAMP>
// Ejemplo:
//   node docs/mediciones/lighthouse/render-04-lighthouse.mjs 20260903-2102
// Requiere (no versionado): npm install @resvg/resvg-js
//
// Salida: docs/informe/figuras/zaida/04-lighthouse.png

import { readdirSync, readFileSync, writeFileSync } from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "..", "..", "..");
const lhDir = __dirname;
const outPng = path.join(repoRoot, "docs/informe/figuras/zaida/04-lighthouse.png");
const outSvg = outPng.replace(/\.png$/, ".svg");

const stamp = process.argv[2];
if (!stamp) {
  console.error("Uso: node render-04-lighthouse.mjs <STAMP>  (p.ej. 20260903-2102)");
  process.exit(1);
}

const files = readdirSync(lhDir).filter(
  (f) => f.startsWith(`lhci-${stamp}-render-`) && f.endsWith(".json")
);
if (files.length < 6) {
  throw new Error(`Se esperaban al menos 6 JSON para el stamp ${stamp}, se encontraron ${files.length}`);
}

const groups = {}; // "mobile|/login" -> {perf:[],a11y:[],bp:[],seo:[]}
let fetchTimes = [];
let lighthouseVersion = null;

for (const f of files) {
  const r = JSON.parse(readFileSync(path.join(lhDir, f), "utf8"));
  const url = new URL(r.requestedUrl);
  if (url.hostname !== "biopet-frontend.onrender.com") {
    throw new Error(`${f}: requestedUrl inesperado (${r.requestedUrl})`);
  }
  const profile = r.configSettings.formFactor;
  const key = `${profile}|${url.pathname}`;
  groups[key] ??= { perf: [], a11y: [], bp: [], seo: [] };
  groups[key].perf.push(r.categories.performance.score * 100);
  groups[key].a11y.push(r.categories.accessibility.score * 100);
  groups[key].bp.push(r.categories["best-practices"].score * 100);
  groups[key].seo.push(r.categories.seo.score * 100);
  fetchTimes.push(r.fetchTime);
  lighthouseVersion = r.lighthouseVersion;
}

const avg = (arr) => arr.reduce((a, b) => a + b, 0) / arr.length;
const order = ["mobile|/login", "mobile|/mascotas", "desktop|/login", "desktop|/mascotas"];
const rows = order
  .filter((k) => groups[k])
  .map((k) => {
    const [profile, route] = k.split("|");
    const g = groups[k];
    return {
      label: `${profile} ${route}`,
      n: g.perf.length,
      performance: avg(g.perf),
      accessibility: avg(g.a11y),
      bestPractices: avg(g.bp),
      seo: avg(g.seo),
    };
  });

fetchTimes.sort();
const firstFetch = fetchTimes[0];
const lastFetch = fetchTimes[fetchTimes.length - 1];

// --- SVG rendering -----------------------------------------------------
const chartLeft = 70, chartRight = 900, chartTop = 120, chartBottom = 320;
const metrics = [
  ["performance", "Performance", "#1d4ed8"],
  ["accessibility", "Accessibility", "#7c3aed"],
  ["bestPractices", "Best Practices", "#0f766e"],
  ["seo", "SEO", "#b45309"],
];
const groupW = (chartRight - chartLeft) / rows.length;
const barW = (groupW - 20) / metrics.length;

function y(val) {
  // Scale 0-100, but zoom into 80-100 since every score sits there and a
  // full 0-100 axis would flatten all the bars into indistinguishable lines.
  const min = 75;
  return chartBottom - ((val - min) / (100 - min)) * (chartBottom - chartTop);
}

let bars = "";
let xLabels = "";
rows.forEach((r, gi) => {
  const groupX = chartLeft + gi * groupW + 10;
  metrics.forEach(([key, , color], mi) => {
    const val = r[key];
    const x = groupX + mi * barW;
    const barY = y(val);
    bars += `<rect x="${x.toFixed(1)}" y="${barY.toFixed(1)}" width="${(barW - 4).toFixed(1)}" height="${(chartBottom - barY).toFixed(1)}" fill="${color}" opacity="0.88"/>`;
    bars += `<text x="${(x + (barW - 4) / 2).toFixed(1)}" y="${(barY - 4).toFixed(1)}" text-anchor="middle" font-family="Arial" font-size="9.5">${val.toFixed(1)}</text>`;
  });
  xLabels += `<text x="${(groupX + (groupW - 20) / 2).toFixed(1)}" y="${chartBottom + 20}" text-anchor="middle" font-family="Arial" font-size="11" font-weight="bold">${r.label}</text>`;
});

let gridLines = "";
for (const v of [75, 80, 85, 90, 95, 100]) {
  const gy = y(v);
  gridLines += `<line x1="${chartLeft}" y1="${gy.toFixed(1)}" x2="${chartRight}" y2="${gy.toFixed(1)}" stroke="#e2e8f0" stroke-width="1"/>`;
  gridLines += `<text x="${chartLeft - 8}" y="${(gy + 3).toFixed(1)}" text-anchor="end" font-family="Arial" font-size="10" fill="#64748b">${v}</text>`;
}
// Threshold reference lines (Performance >=80, others >=90) — dashed, since
// they don't all share one value.
const thr90Y = y(90);
gridLines += `<line x1="${chartLeft}" y1="${thr90Y.toFixed(1)}" x2="${chartRight}" y2="${thr90Y.toFixed(1)}" stroke="#dc2626" stroke-width="1" stroke-dasharray="4,3"/>`;
gridLines += `<text x="${chartRight}" y="${(chartTop - 14).toFixed(1)}" text-anchor="end" font-family="Arial" font-size="9.5" fill="#dc2626">- - - threshold 90 (Accessibility/Best Practices/SEO)</text>`;

let legend = "";
metrics.forEach(([, label, color], i) => {
  const lx = chartLeft + i * 180;
  legend += `<rect x="${lx}" y="60" width="12" height="12" fill="${color}"/>`;
  legend += `<text x="${lx + 16}" y="70" font-family="Arial" font-size="10.5">${label}</text>`;
});

// --- Summary table -------------------------------------------------------
const tableTop = 380;
const rowH = 22;
const cols = [
  ["Profile / Route", (r) => r.label],
  ["n", (r) => r.n],
  ["Performance", (r) => r.performance.toFixed(1)],
  ["Accessibility", (r) => r.accessibility.toFixed(1)],
  ["Best Practices", (r) => r.bestPractices.toFixed(1)],
  ["SEO", (r) => r.seo.toFixed(1)],
];
const colX = [40, 220, 300, 420, 550, 700];
let tableHeader = "";
cols.forEach(([label], i) => {
  tableHeader += `<text x="${colX[i]}" y="${tableTop}" font-family="Arial" font-size="11" font-weight="bold" fill="#1e293b">${label}</text>`;
});
let tableRows = "";
rows.forEach((r, ri) => {
  const ry = tableTop + rowH * (ri + 1);
  if (ri % 2 === 0) tableRows += `<rect x="30" y="${ry - 15}" width="840" height="${rowH}" fill="#f8fafc"/>`;
  cols.forEach(([, fn], ci) => {
    tableRows += `<text x="${colX[ci]}" y="${ry}" font-family="Arial" font-size="10.5" fill="#0f172a">${fn(r)}</text>`;
  });
});

const totalH = tableTop + rowH * (rows.length + 2) + 20;
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="920" height="${totalH}" viewBox="0 0 920 ${totalH}">
<rect x="0" y="0" width="920" height="${totalH}" fill="white"/>
<text x="460" y="26" text-anchor="middle" font-family="Arial" font-size="18" font-weight="bold" fill="#0f172a">BIOPET - Lighthouse CI Report (Render production deployment)</text>
<text x="460" y="44" text-anchor="middle" font-family="Arial" font-size="11" fill="#475569">https://biopet-frontend.onrender.com - /login and /mascotas - 3 runs x 2 profiles x 2 routes (12 runs) - ${new Date(firstFetch).toISOString().slice(0,10)}</text>
${legend}
${gridLines}
${bars}
${xLabels}
<text x="30" y="${tableTop - 24}" font-family="Arial" font-size="11" fill="#334155">Score (0-100, axis zoomed to 75-100)</text>
<line x1="30" y1="${tableTop - 16}" x2="870" y2="${tableTop - 16}" stroke="#cbd5e1" stroke-width="1"/>
${tableHeader}
<line x1="30" y1="${tableTop + 6}" x2="870" y2="${tableTop + 6}" stroke="#cbd5e1" stroke-width="1"/>
${tableRows}
<line x1="30" y1="${tableTop + rowH * (rows.length + 1) - 10}" x2="870" y2="${tableTop + rowH * (rows.length + 1) - 10}" stroke="#cbd5e1" stroke-width="1"/>
<text x="30" y="${tableTop + rowH * (rows.length + 2) + 2}" font-family="Arial" font-size="10" fill="#334155">Lighthouse ${lighthouseVersion} - runs from ${firstFetch} to ${lastFetch} (UTC) - source: docs/mediciones/lighthouse/lhci-${stamp}-render-*.json</text>
</svg>`;

writeFileSync(outSvg, svg, "utf8");
console.log("SVG written:", outSvg);

const { Resvg } = await import("@resvg/resvg-js");
const resvg = new Resvg(svg, { fitTo: { mode: "width", value: 2000 }, background: "white" });
const png = resvg.render().asPng();
writeFileSync(outPng, png);
console.log("PNG written:", outPng, png.length, "bytes");
