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
