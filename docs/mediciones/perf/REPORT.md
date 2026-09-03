# Reporte de rendimiento — BIOPET (k6)

Metodo de intervalo de confianza: distribucion t de Student (scipy.stats.t), apropiada para el tamano de muestra de estas corridas.

| Corrida | n | Media (ms) | Mediana (ms) | DE (ms) | IC95% (ms) | p50 | p90 | p95 | p99 | Error (%) | Throughput (req/s) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| k6-20260817T005446-local-tls-v0.9.0-rc-caliente-01.json | 3196 | 10.54 | 8.68 | 6.95 | [10.3, 10.78] | 8.68 | 17.16 | 20.15 | 30.68 | 0.0 | 91.17 |
| k6-20260817T005826-local-tls-v0.9.0-rc-frio-01.json | 3206 | 10.51 | 8.47 | 15.83 | [9.96, 11.05] | 8.47 | 16.62 | 21.1 | 33.14 | 0.0 | 91.49 |
| k6-20260817T005523-local-tls-v0.9.0-rc-caliente-02.json | 3226 | 7.19 | 6.63 | 6.38 | [6.97, 7.41] | 6.63 | 8.55 | 9.7 | 17.18 | 0.0 | 92.09 |
| k6-20260817T005930-local-tls-v0.9.0-rc-frio-02.json | 3211 | 9.37 | 7.6 | 16.15 | [8.82, 9.93] | 7.6 | 12.82 | 17.23 | 25.79 | 0.0 | 91.62 |
| k6-20260817T005559-local-tls-v0.9.0-rc-caliente-03.json | 3226 | 6.6 | 6.01 | 5.27 | [6.42, 6.78] | 6.01 | 8.63 | 10.86 | 16.13 | 0.0 | 92.13 |
| k6-20260817T010033-local-tls-v0.9.0-rc-frio-03.json | 3204 | 10.8 | 8.43 | 21.75 | [10.05, 11.56] | 8.43 | 16.5 | 22.13 | 33.4 | 0.0 | 91.36 |
| k6-20260817T005636-local-tls-v0.9.0-rc-caliente-04.json | 3236 | 5.21 | 4.92 | 4.99 | [5.04, 5.38] | 4.92 | 6.39 | 6.83 | 10.12 | 0.0 | 92.4 |
| k6-20260817T010137-local-tls-v0.9.0-rc-frio-04.json | 3206 | 10.71 | 8.5 | 15.97 | [10.16, 11.26] | 8.5 | 16.47 | 20.16 | 32.65 | 0.0 | 91.45 |
| k6-20260817T005712-local-tls-v0.9.0-rc-caliente-05.json | 3236 | 5.4 | 4.96 | 4.81 | [5.23, 5.57] | 4.96 | 6.72 | 7.81 | 12.59 | 0.0 | 92.41 |
| k6-20260817T010240-local-tls-v0.9.0-rc-frio-05.json | 3199 | 10.61 | 7.88 | 18.18 | [9.98, 11.24] | 7.88 | 16.29 | 20.71 | 40.87 | 0.0 | 91.51 |

## Wilcoxon pareado (caliente vs frio)

Pareo por indice de llegada (truncando al menor tamano de muestra). Tamano de efecto r de Rosenthal (r = Z / sqrt(n)).
Correccion por comparaciones multiples: Holm-Bonferroni (alfa=0.05, m=5).
Nota metodologica: las corridas son independientes; el pareo por indice de llegada no constituye un diseno pareado verdadero. Ver analisis de sensibilidad abajo.

| Par | W | p (original) | p (ajustado Holm) | Significativo (Holm) | r |
|---|---|---|---|---|---|
| local-tls-v0.9.0-rc-01 | 2334567.5 | 2.51e-05 | 2.51e-05 | Si | -0.0745 |
| local-tls-v0.9.0-rc-02 | 919470.5 | 7.74e-219 | 1.55e-218 | Si | -0.5572 |
| local-tls-v0.9.0-rc-03 | 633693.0 | 1.98e-298 | 5.93e-298 | Si | -0.6523 |
| local-tls-v0.9.0-rc-04 | 78460.0 | p < 2.23e-308 | p < 2.23e-308 | Si | -0.8397 |
| local-tls-v0.9.0-rc-05 | 227940.0 | p < 2.23e-308 | p < 2.23e-308 | Si | -0.789 |

## Sensibilidad: Mann-Whitney U (muestras independientes)

Analisis de sensibilidad sin asumir pareo por indice (corridas independientes). Tamano de efecto r = Z / sqrt(N).

| Par | U | p | r |
|---|---|---|---|
| local-tls-v0.9.0-rc-01 | 5362411.0 | 1.22e-03 | 0.0404 |
| local-tls-v0.9.0-rc-02 | 3052051.0 | 4.22e-179 | -0.3557 |
| local-tls-v0.9.0-rc-03 | 1980663.5 | p < 2.23e-308 | -0.5341 |
| local-tls-v0.9.0-rc-04 | 490560.5 | p < 2.23e-308 | -0.7841 |
| local-tls-v0.9.0-rc-05 | 806939.0 | p < 2.23e-308 | -0.7309 |

## Grafico

![Latencia por percentil](grafico.svg)
