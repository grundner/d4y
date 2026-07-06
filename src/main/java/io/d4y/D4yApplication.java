package io.d4y;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * D4Y — Git-native Runtime Platform.
 *
 * <p>Backend-Einstiegspunkt. Der Reconciliation-Loop gleicht den Sollzustand
 * (Desired State) kontinuierlich mit dem Ist-Zustand der Container-Engine ab.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class D4yApplication {

    public static void main(String[] args) {
        SpringApplication.run(D4yApplication.class, args);
    }
}
