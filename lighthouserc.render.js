/**
 * lighthouserc.render.js — Configuración de Lighthouse CI (perfil móvil)
 * contra el despliegue público de BIOPET en Render.
 *
 * Complementa a lighthouserc.js (mismas rutas/umbrales/numberOfRuns, contra
 * localhost:4200). Esta variante existe para el hallazgo de recalificación
 * "Lighthouse final contra Render": la guía final exige requestedUrl
 * público (no localhost), 3 corridas por perfil y más de una ruta.
 *
 * URL pública evaluada: https://biopet-frontend.onrender.com (la misma
 * declarada como \urlfrontend en docs/informe/informe-final-v1.0.0.tex).
 *
 * Cómo se ejecuta (ver scripts/run-lighthouse-render.sh para el flujo
 * completo, incluida la verificación de que el sitio responde antes de
 * auditar):
 *   bash scripts/run-lighthouse-render.sh
 */
module.exports = {
  ci: {
    collect: {
      // Mismas dos rutas reales auditadas contra localhost (ver
      // lighthouserc.js): login (sin sesión) y mascotas (con sesión o
      // redirigiendo a login, igual de válido para las 4 categorías
      // medidas). No se audita '/', porque solo redirige.
      url: [
        'https://biopet-frontend.onrender.com/login',
        'https://biopet-frontend.onrender.com/mascotas',
      ],
      numberOfRuns: 3,
      settings: {
        throttlingMethod: 'simulate',
      },
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.8 }],
        'categories:accessibility': ['error', { minScore: 0.9 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],
        'categories:seo': ['error', { minScore: 0.9 }],
      },
    },
    upload: {
      target: 'filesystem',
      outputDir: './.lighthouseci-render',
    },
  },
};
