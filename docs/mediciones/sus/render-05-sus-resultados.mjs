// Genera docs/informe/figuras/zaida/05-sus-resultados.png EN INGLES desde
// los datos reales de docs/mediciones/sus/sus-raw.csv (18 participantes),
// calculando media/DE/IC95%/mediana/min/max en vivo (no copia numeros de
// REPORT.md a mano). Reemplaza la version anterior (hallazgo #9 del plan
// de recalificacion), que ademas de estar en espanol mostraba cifras
// obsoletas de n=10 (Tercera Entrega) en vez de las 18 actuales.
//
// Uso: node docs/mediciones/sus/render-05-sus-resultados.mjs
// Requiere (no versionado): npm install @resvg/resvg-js

import { readFileSync, writeFileSync } from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, "..", "..", "..");

const csvPath = path.join(repoRoot, "docs/mediciones/sus/sus-raw.csv");
const outPng = path.join(repoRoot, "docs/informe/figuras/zaida/05-sus-resultados.png");
const outSvg = outPng.replace(/\.png$/, ".svg");

const lines = readFileSync(csvPath, "utf8").trim().split(/\r?\n/);
const header = lines[0].split(",");
const scoreIdx = header.indexOf("sus_score");
if (scoreIdx === -1) throw new Error("Columna sus_score no encontrada en " + csvPath);

const rows = lines.slice(1).map((l) => l.split(","));
const scores = rows.map((r) => Number(r[scoreIdx]));
const n = scores.length;
if (n !== 18) throw new Error(`Se esperaban 18 participantes, se encontraron ${n}`);

const mean = scores.reduce((a, b) => a + b, 0) / n;
const variance = scores.reduce((a, b) => a + (b - mean) ** 2, 0) / (n - 1);
const sd = Math.sqrt(variance);
const tCrit = 2.11; // t de Student, df=17, 95% -- mismo valor que REPORT.md
const marginErr = tCrit * (sd / Math.sqrt(n));
const ciLow = mean - marginErr;
const ciHigh = mean + marginErr;
const sorted = [...scores].sort((a, b) => a - b);
const median = (sorted[8] + sorted[9]) / 2; // n=18 par: promedio de los dos centrales
const min = sorted[0];
const max = sorted[n - 1];

const classify = (v) => (v >= 80.3 ? "Excellent" : v >= 68 ? "Good" : v >= 51 ? "OK" : "Poor");

console.log({ n, mean, sd, ciLow, ciHigh, median, min, max, classification: classify(mean) });

// --- SVG: bar chart of the 18 individual scores + summary stats table ---
const chartLeft = 60, chartRight = 900, chartTop = 90, chartBottom = 320;
function y(val) {
  return chartBottom - (val / 100) * (chartBottom - chartTop);
}
const barW = (chartRight - chartLeft) / n - 6;

let bars = "";
scores.forEach((score, i) => {
  const x = chartLeft + i * ((chartRight - chartLeft) / n) + 3;
  const barY = y(score);
  const color = score >= 68 ? "#0f766e" : "#b45309";
  bars += `<rect x="${x.toFixed(1)}" y="${barY.toFixed(1)}" width="${barW.toFixed(1)}" height="${(chartBottom - barY).toFixed(1)}" fill="${color}" opacity="0.85"/>`;
  bars += `<text x="${(x + barW / 2).toFixed(1)}" y="${(barY - 4).toFixed(1)}" text-anchor="middle" font-family="Arial" font-size="8.5">${score.toFixed(1)}</text>`;
  bars += `<text x="${(x + barW / 2).toFixed(1)}" y="${chartBottom + 14}" text-anchor="middle" font-family="Arial" font-size="8.5">P${String(i + 1).padStart(2, "0")}</text>`;
});

let gridLines = "";
for (const v of [0, 25, 50, 68, 75, 100]) {
  const gy = y(v);
  const dashed = v === 68;
  gridLines += `<line x1="${chartLeft}" y1="${gy.toFixed(1)}" x2="${chartRight}" y2="${gy.toFixed(1)}" stroke="${dashed ? "#dc2626" : "#e2e8f0"}" stroke-width="1" ${dashed ? 'stroke-dasharray="4,3"' : ""}/>`;
  gridLines += `<text x="${chartLeft - 8}" y="${(gy + 3).toFixed(1)}" text-anchor="end" font-family="Arial" font-size="10" fill="${dashed ? "#dc2626" : "#64748b"}">${v}</text>`;
}

const tableTop = 360;
const rowH = 20;
const stats = [
  ["Mean (SUS Score)", `${mean.toFixed(2)} / 100`],
  ["Standard deviation (sample, n-1)", sd.toFixed(2)],
  ["95% confidence interval", `[${ciLow.toFixed(2)}, ${ciHigh.toFixed(2)}] (margin +/- ${marginErr.toFixed(2)})`],
  ["Median (p50)", median.toFixed(2)],
  ["Min / Max", `${min.toFixed(2)} / ${max.toFixed(2)}`],
  ["Qualitative classification (Bangor, Kortum &amp; Miller)", classify(mean)],
];
let tableRows = "";
stats.forEach(([label, val], i) => {
  const ry = tableTop + rowH * (i + 1);
  if (i % 2 === 0) tableRows += `<rect x="30" y="${ry - 14}" width="600" height="${rowH}" fill="#f8fafc"/>`;
  tableRows += `<text x="40" y="${ry}" font-family="Arial" font-size="11" fill="#0f172a">${label}</text>`;
  tableRows += `<text x="430" y="${ry}" font-family="Arial" font-size="11" font-weight="bold" fill="#0f172a">${val}</text>`;
});

const totalH = tableTop + rowH * (stats.length + 1) + 10;
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="920" height="${totalH}" viewBox="0 0 920 ${totalH}">
<rect x="0" y="0" width="920" height="${totalH}" fill="white"/>
<text x="460" y="26" text-anchor="middle" font-family="Arial" font-size="18" font-weight="bold" fill="#0f172a">BIOPET - SUS Results (n=18)</text>
<text x="460" y="44" text-anchor="middle" font-family="Arial" font-size="11" fill="#475569">Individual scores by participant - source: docs/mediciones/sus/sus-raw.csv</text>
${gridLines}
${bars}
<text x="${chartRight}" y="${(y(68) - 6).toFixed(1)}" text-anchor="end" font-family="Arial" font-size="9.5" fill="#dc2626">industry average benchmark = 68 (Bangor et al. 2008)</text>
<rect x="${chartLeft}" y="60" width="12" height="12" fill="#0f766e"/>
<text x="${chartLeft + 16}" y="70" font-family="Arial" font-size="10">score &gt;= 68 (average benchmark)</text>
<rect x="${chartLeft + 220}" y="60" width="12" height="12" fill="#b45309"/>
<text x="${chartLeft + 236}" y="70" font-family="Arial" font-size="10">score &lt; 68</text>
<text x="30" y="${tableTop - 10}" font-family="Arial" font-size="12" font-weight="bold" fill="#0f172a">Aggregated results</text>
${tableRows}
</svg>`;

writeFileSync(outSvg, svg, "utf8");
console.log("SVG written:", outSvg);

const { Resvg } = await import("@resvg/resvg-js");
const resvg = new Resvg(svg, { fitTo: { mode: "width", value: 2000 }, background: "white" });
const png = resvg.render().asPng();
writeFileSync(outPng, png);
console.log("PNG written:", outPng, png.length, "bytes");
