package io.d4y.app;

/**
 * Ein {@code ${secret:NAME}}-Platzhalter im Sollzustand konnte nicht aufgelöst werden
 * (Secret noch nicht geliefert). Die betroffene Application wird übersprungen (ADR-0024).
 */
public class UnresolvedSecretException extends RuntimeException {

    private final String secretName;

    public UnresolvedSecretException(String secretName) {
        super("Secret '" + secretName + "' nicht verfügbar");
        this.secretName = secretName;
    }

    public String secretName() {
        return secretName;
    }
}
