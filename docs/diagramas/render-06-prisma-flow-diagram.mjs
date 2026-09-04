// docs/diagramas/render-06-prisma-flow-diagram.mjs
//
// Procedencia de la figura del diagrama de flujo PRISMA del informe:
//   fuente editable : docs/diagramas/prisma-flow-diagram.dot  (versionada)
//   salidas         : docs/diagramas/prisma-flow-diagram.svg
//                     docs/informe/figuras/zaida/06-prisma-flow-diagram.png
//                     (usada por docs/informe/secciones-final/02-trabajos-relacionados.tex)
//
// Antes estaba en la raiz del repositorio como "render-prisma2-tmp.mjs", un
// nombre temporal que no reflejaba que es la procedencia real de una figura
// del informe. Se conserva (no se borra) para que la figura siga siendo
// reproducible desde su fuente .dot versionada.
//
// Las rutas de abajo son relativas a la RAIZ del repositorio, asi que debe
// ejecutarse desde la raiz, no desde este directorio:
//   npm i --no-save @hpcc-js/wasm @resvg/resvg-js
//   node docs/diagramas/render-06-prisma-flow-diagram.mjs
//
// Dependencias de ejecucion puntual (@hpcc-js/wasm, @resvg/resvg-js) no
// forman parte de frontend/package.json: son herramientas de generacion de
// figuras, no dependencias de la aplicacion.

import { Graphviz } from "@hpcc-js/wasm/graphviz";
import { Resvg } from "@resvg/resvg-js";
import { readFileSync, writeFileSync } from "fs";

const graphviz = await Graphviz.load();
const dot = readFileSync("docs/diagramas/prisma-flow-diagram.dot", "utf8");
const svg = graphviz.layout(dot, "svg", "dot");
writeFileSync("docs/diagramas/prisma-flow-diagram.svg", svg, "utf8");
const resvg = new Resvg(svg, { fitTo: { mode: "width", value: 2400 }, background: "white" });
const png = resvg.render().asPng();
writeFileSync("docs/informe/figuras/zaida/06-prisma-flow-diagram.png", png);
console.log("written png", png.length);
