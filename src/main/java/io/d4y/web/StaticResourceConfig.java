package io.d4y.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Liefert das statisch exportierte Next.js-Frontend (ADR-0006 Single-Image, ADR-0014)
 * direkt aus dem Backend aus. UI und {@code /api/**} teilen sich denselben Port
 * ({@code server.port}) → same-origin, kein Proxy, kein CORS, kein Node zur Laufzeit.
 *
 * <p>Die statischen Dateien liegen im Klassenpfad unter {@code /static/} (der Gradle-Build
 * kopiert {@code frontend/out/} dorthin).
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Eigenes /**-Mapping: Spring Boot registriert sein Default nur, wenn /** noch
        // nicht belegt ist (hasMappingForPattern), daher übernimmt dieser Resolver.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * Bildet URLs auf die Dateien des Next.js-Exports (trailingSlash:false) ab:
     * <ul>
     *   <li>exakte Datei — z. B. {@code /_next/static/…}, {@code /favicon.ico}</li>
     *   <li>Route ohne Endung → {@code <route>.html} — z. B. {@code /activity} → {@code activity.html}</li>
     *   <li>Verzeichnis → {@code <route>/index.html}</li>
     *   <li>sonst SPA-Fallback → {@code index.html}</li>
     * </ul>
     * {@code /api/**} und {@code /actuator/**} werden hier nie bedient — dafür sind die
     * {@code @RestController} bzw. der Actuator zuständig (höhere Mapping-Präzedenz).
     */
    static class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
                return null;
            }
            if (resourcePath.isEmpty()) {
                return readable(location, "index.html");
            }
            Resource exact = readable(location, resourcePath);
            if (exact != null) {
                return exact;
            }
            if (!hasExtension(resourcePath)) {
                Resource html = readable(location, resourcePath + ".html");
                if (html != null) {
                    return html;
                }
                Resource index = readable(location, resourcePath + "/index.html");
                if (index != null) {
                    return index;
                }
            }
            // Unbekannte, client-seitige Route → Einstiegsseite (SPA-Fallback).
            return readable(location, "index.html");
        }

        private Resource readable(Resource location, String relative) throws IOException {
            Resource resource = location.createRelative(relative);
            // checkResource stellt sicher, dass die Ressource unterhalb der Location liegt
            // (Schutz gegen Path-Traversal).
            if (resource.exists() && resource.isReadable() && checkResource(resource, location)) {
                return resource;
            }
            return null;
        }

        private static boolean hasExtension(String path) {
            int slash = path.lastIndexOf('/');
            int dot = path.lastIndexOf('.');
            return dot > slash;
        }
    }
}
