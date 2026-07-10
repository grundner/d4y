package io.d4y.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * ADR-0021: Metadaten des publizierten API-Vertrags (OpenAPI).
 *
 * <p>Titel und Version identifizieren den Vertrag, den externe Clients (z. B. die
 * macOS-Companion-App) konsumieren. Die Endpunkt- und Schema-Beschreibungen selbst werden
 * aus den {@code /api}-Controllern generiert — dieser Block liefert nur die Kopfdaten.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "d4y API",
        version = "0.1.0",
        description = "REST-API der d4y Git-native Runtime Platform. "
                + "Read-only bezüglich des Sollzustands (ADR-0004); operative Aktionen sind "
                + "transient (ADR-0013). Der Akteur wird über den Header X-Actor übermittelt."
))
public class OpenApiConfig {
}
