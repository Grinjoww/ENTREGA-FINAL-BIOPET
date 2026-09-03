#!/usr/bin/env python3
"""
Genera figura 06-performance-report.png: Vista del reporte agregado de rendimiento.
Gráfico de barras agrupadas comparando caliente vs frío por percentiles (p50, p90, p95, p99)
promediados sobre las 5 corridas, con desviación estándar como barras de error.
"""
import json
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from pathlib import Path

# Datos del REPORT.md (promedios de las 5 corridas por modo)
# Caliente: corridas 01-05
caliente_data = {
    'p50': [8.68, 6.63, 6.01, 4.92, 4.96],
    'p90': [17.16, 8.55, 8.63, 6.39, 6.72],
    'p95': [20.15, 9.70, 10.86, 6.83, 7.81],
    'p99': [30.68, 17.18, 16.13, 10.12, 12.59],
}

# Frío: corridas 01-05
frio_data = {
    'p50': [8.47, 7.60, 8.43, 8.50, 7.88],
    'p90': [16.62, 12.82, 16.50, 16.47, 16.29],
    'p95': [21.10, 17.23, 22.13, 20.16, 20.71],
    'p99': [33.14, 25.79, 33.40, 32.65, 40.87],
}

# Calcular medias y desviaciones
percentiles = ['p50', 'p90', 'p95', 'p99']
labels = ['p50', 'p90', 'p95', 'p99']

caliente_means = [np.mean(caliente_data[p]) for p in percentiles]
caliente_stds = [np.std(caliente_data[p], ddof=1) for p in percentiles]

frio_means = [np.mean(frio_data[p]) for p in percentiles]
frio_stds = [np.std(frio_data[p], ddof=1) for p in percentiles]

print("Caliente means:", caliente_means)
print("Caliente stds:", caliente_stds)
print("Frio means:", frio_means)
print("Frio stds:", frio_stds)

# Crear figura
fig, ax = plt.subplots(figsize=(10, 6))

x = np.arange(len(percentiles))
width = 0.35

bars1 = ax.bar(x - width/2, caliente_means, width, label='Caliente (con caché)', 
               color='#2e7d32', edgecolor='white', yerr=caliente_stds, capsize=5)
bars2 = ax.bar(x + width/2, frio_means, width, label='Frío (sin caché)', 
               color='#e65100', edgecolor='white', yerr=frio_stds, capsize=5)

# Etiquetas y título
ax.set_ylabel('Latencia (ms)', fontsize=12)
ax.set_xlabel('Percentil', fontsize=12)
ax.set_title('BIOPET — Latencia por percentil (promedio 5 corridas, TLS local)', fontsize=14, fontweight='bold')
ax.set_xticks(x)
ax.set_xticklabels(labels, fontsize=11)
ax.legend(fontsize=11, loc='upper left')
ax.grid(axis='y', alpha=0.3)

# Valores encima de las barras
for bars, means in [(bars1, caliente_means), (bars2, frio_means)]:
    for bar, mean in zip(bars, means):
        height = bar.get_height()
        ax.annotate(f'{mean:.1f}',
                    xy=(bar.get_x() + bar.get_width() / 2, height),
                    xytext=(0, 3),
                    textcoords="offset points",
                    ha='center', va='bottom', fontsize=9, fontweight='bold')

# Anotación de mejora
for i, (c_mean, f_mean) in enumerate(zip(caliente_means, frio_means)):
    if f_mean > 0:
        mejora = ((f_mean - c_mean) / f_mean) * 100
        ax.annotate(f'-{mejora:.0f}%',
                    xy=(x[i], max(c_mean, f_mean) + max(caliente_stds[i], frio_stds[i]) + 2),
                    ha='center', va='bottom', fontsize=9, color='#1b5e20', fontweight='bold')

plt.tight_layout()

# Guardar
output_path = Path('docs/informe/figuras/fred/06-performance-report.png')
output_path.parent.mkdir(parents=True, exist_ok=True)
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"Figura guardada en: {output_path}")

# También generar versión SVG para flexibilidad
svg_path = output_path.with_suffix('.svg')
plt.savefig(svg_path, bbox_inches='tight')
print(f"SVG guardado en: {svg_path}")

plt.close()