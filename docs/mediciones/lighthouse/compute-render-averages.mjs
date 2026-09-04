// Calcula la media aritmetica de las 4 categorias de Lighthouse
// (Performance, Accessibility, Best Practices, SEO) por perfil (mobile/
// desktop) y por ruta (/login, /mascotas), a partir de los 12 JSON crudos
// de la corrida contra el despliegue publico de Render (paso 25 del plan
// de recalificacion: "Calcula media aritmetica por perfil/ruta desde los
// JSON, no manualmente").
//
// Uso: node docs/mediciones/lighthouse/compute-render-averages.mjs <STAMP>
// Ejemplo: node docs/mediciones/lighthouse/compute-render-averages.mjs 20260903-2102

import { readdirSync, readFileSync } from "fs";
import path from "path";
import { fileURLToPath } from "url";

const dir = path.dirname(fileURLToPath(import.meta.url));
const stamp = process.argv[2];
if (!stamp) {
  console.error("Uso: node compute-render-averages.mjs <STAMP>  (p.ej. 20260903-2102)");
  process.exit(1);
}

const files = readdirSync(dir).filter(
  (f) => f.startsWith(`lhci-${stamp}-render-`) && f.endsWith(".json")
);
if (files.length === 0) {
  console.error(`No se encontraron JSON para el stamp ${stamp} en ${dir}`);
  process.exit(1);
}

const groups = {}; // key: "mobile|/login" -> { perf:[], a11y:[], bp:[], seo:[], requestedUrls:[] }

for (const f of files) {
  const r = JSON.parse(readFileSync(path.join(dir, f), "utf8"));
  const url = new URL(r.requestedUrl);
  if (url.hostname !== "biopet-frontend.onrender.com") {
    throw new Error(`${f}: requestedUrl inesperado (${r.requestedUrl}), no es el dominio publico de Render`);
  }
  const profile = r.configSettings.formFactor; // "mobile" | "desktop"
  const key = `${profile}|${url.pathname}`;
  groups[key] ??= { perf: [], a11y: [], bp: [], seo: [], files: [] };
  groups[key].perf.push(r.categories.performance.score * 100);
  groups[key].a11y.push(r.categories.accessibility.score * 100);
  groups[key].bp.push(r.categories["best-practices"].score * 100);
  groups[key].seo.push(r.categories.seo.score * 100);
  groups[key].files.push(f);
}

const avg = (arr) => arr.reduce((a, b) => a + b, 0) / arr.length;

console.log(`\nMedias por perfil/ruta — corrida render, stamp ${stamp} (${files.length} JSON, dominio biopet-frontend.onrender.com)\n`);
console.log("Perfil    Ruta         n  Performance  Accessibility  BestPractices  SEO");
for (const [key, g] of Object.entries(groups).sort()) {
  const [profile, route] = key.split("|");
  console.log(
    `${profile.padEnd(9)} ${route.padEnd(12)} ${String(g.perf.length).padEnd(2)} ` +
    `${avg(g.perf).toFixed(1).padStart(11)}  ${avg(g.a11y).toFixed(1).padStart(13)}  ` +
    `${avg(g.bp).toFixed(1).padStart(13)}  ${avg(g.seo).toFixed(1).padStart(5)}`
  );
}
console.log("");
