package io.d4y.app;

import io.d4y.domain.reconcile.Reconciler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stellt die framework-freien Domänen-Komponenten als Spring-Beans bereit.
 */
@Configuration
public class DomainConfig {

    @Bean
    public Reconciler reconciler() {
        return new Reconciler();
    }
}
