/**
 * lighthouserc.render.desktop.js — Configuración de Lighthouse CI (perfil
 * desktop) contra el despliegue público de BIOPET en Render.
 *
 * Complementa a lighthouserc.render.js (perfil móvil): mismas URLs públicas,
 * mismos umbrales, mismo numberOfRuns; únicamente cambia el formFactor
 * auditado, con el mismo preset "desktop" que trae Lighthouse por defecto
 * (igual criterio que lighthouserc.desktop.js, ver ese archivo).
 *
 * Cómo se ejecuta (ver scripts/run-lighthouse-render.sh):
 *   bash scripts/run-lighthouse-render.sh
 */
module.exports = {
  ci: {
    collect: {
      url: [
        'https://biopet-frontend.onrender.com/login',
        'https://biopet-frontend.onrender.com/mascotas',
      ],
      numberOfRuns: 3,
      settings: {
        formFactor: 'desktop',
        screenEmulation: {
          mobile: false,
          width: 1350,
          height: 940,
          deviceScaleFactor: 1,
          disabled: false,
        },
        throttling: {
          rttMs: 40,
          throughputKbps: 10240,
          cpuSlowdownMultiplier: 1,
          requestLatencyMs: 0,
          downloadThroughputKbps: 0,
          uploadThroughputKbps: 0,
        },
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
      outputDir: './.lighthouseci-render-desktop',
    },
  },
};
