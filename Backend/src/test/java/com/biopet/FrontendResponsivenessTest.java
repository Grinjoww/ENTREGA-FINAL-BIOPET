package com.biopet;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de verificación de responsividad del frontend (REQ-NF-005).
 * Valida que el build del frontend incluye:
 * - Meta tag viewport para responsividad (320-1440px)
 * - CSS media queries para breakpoints móviles/desktop
 * No edita componentes Angular (capa de Zaida); verifica artefactos de build.
 * El frontend se sirve por nginx en Docker; estos tests validan el output del build.
 */
@ActiveProfiles("test")
class FrontendResponsivenessTest {

    private static final Path FRONTEND_DIST = Paths.get("../frontend/dist/biopet-frontend/browser");
    private static final Path INDEX_HTML = FRONTEND_DIST.resolve("index.html");

    @Test
    void frontendBuildExisteIndexHtml() {
        assertTrue(Files.exists(INDEX_HTML), "No existe index.html en build del frontend: " + INDEX_HTML.toAbsolutePath());
    }

    @Test
    void indexHtmlContieneViewportMetaTag() throws IOException {
        String html = Files.readString(INDEX_HTML);
        assertNotNull(html);
        assertTrue(html.contains("<meta name=\"viewport\""), "Falta meta tag viewport en index.html");
        assertTrue(html.contains("width=device-width"), "Viewport no tiene width=device-width");
        assertTrue(html.contains("initial-scale=1"), "Viewport no tiene initial-scale=1");
    }

    @Test
    void indexHtmlContieneDoctypeYHtmlValido() throws IOException {
        String html = Files.readString(INDEX_HTML);
        assertNotNull(html);
        // Acepta DOCTYPE en mayúsculas o minúsculas
        assertTrue(html.trim().toLowerCase().startsWith("<!doctype html>") || html.trim().toLowerCase().startsWith("<html"),
                "HTML no comienza con DOCTYPE o tag html");
        assertTrue(html.toLowerCase().contains("<html"), "Falta tag html");
        assertTrue(html.toLowerCase().contains("<head>"), "Falta tag head");
        assertTrue(html.toLowerCase().contains("<body>"), "Falta tag body");
    }

    @Test
    void indexHtmlReferenciaStylesCss() throws IOException {
        String html = Files.readString(INDEX_HTML);
        assertTrue(html.contains("styles") || html.contains(".css"), "No se referencia CSS en index.html");
    }

    @Test
    void indexHtmlIncluyeAngularBootstrap() throws IOException {
        String html = Files.readString(INDEX_HTML);
        // Verificar que Angular se bootstrappea (app-root o similar)
        assertTrue(html.contains("app-root") || html.contains("ng-app") || html.contains("bootstrap"),
                "No se detecta bootstrap de Angular en index.html");
    }

    @Test
    void buildIncluyeArchivoStylesCss() throws IOException {
        try (Stream<Path> files = Files.list(FRONTEND_DIST)) {
            boolean hasStyles = files.anyMatch(p -> p.getFileName().toString().matches("styles-.*\\.css"));
            assertTrue(hasStyles, "No existe styles-*.css en build del frontend: " + FRONTEND_DIST.toAbsolutePath());
        }
    }

    @Test
    void stylesCssContieneMediaQueriesResponsive() throws IOException {
        try (Stream<Path> files = Files.list(FRONTEND_DIST)) {
            Path stylesCss = files
                    .filter(p -> p.getFileName().toString().matches("styles-.*\\.css"))
                    .findFirst()
                    .orElse(null);
            if (stylesCss != null) {
                String css = Files.readString(stylesCss);
                assertTrue(css.contains("@media"), "CSS no contiene media queries para responsividad: " + stylesCss);
                assertTrue(css.contains("max-width") || css.contains("min-width"), "CSS no contiene breakpoints responsive: " + stylesCss);
            }
        }
    }
}