package io.d4y.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ADR-0021/ADR-0022: Metadaten des publizierten API-Vertrags (OpenAPI).
 *
 * <p>Die {@code info.version} stammt zur Laufzeit aus {@link BuildProperties} (aus
 * {@code build-info.properties}, erzeugt von {@code springBoot { buildInfo() }}) — eine einzige
 * Versions-Wahrheit statt eines Literals. Ohne generierte Build-Info (z. B. reiner IDE-Lauf) wird
 * {@code "dev"} verwendet. Endpunkt- und Schema-Beschreibungen werden aus den {@code /api}-Controllern
 * generiert; dieser Bean liefert nur die Kopfdaten.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI d4yOpenApi(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties build = buildProperties.getIfAvailable();
        String version = build != null ? build.getVersion() : "dev";
        return new OpenAPI().info(new Info()
                .title("d4y API")
                .version(version)
                .description("REST-API der d4y Git-native Runtime Platform. "
                        + "Read-only bezüglich des Sollzustands (ADR-0004); operative Aktionen sind "
                        + "transient (ADR-0013). Der Akteur wird über den Header X-Actor übermittelt."));
    }
}
