package io.d4y.domain.model;

/**
 * Label-Konvention, mit der D4Y die von ihm verwalteten Container markiert.
 *
 * <p>Nur so markierte Container gelten als von D4Y verwaltet; alles andere bleibt unangetastet.
 */
public final class D4yLabels {

    /** Markiert einen Container als von D4Y verwaltet ({@code "true"}). */
    public static final String MANAGED = "d4y.managed";

    /** Name der zugehörigen Application. */
    public static final String APP = "d4y.app";

    /** Image-Referenz, mit der der Container erzeugt wurde (für Drift-Vergleich). */
    public static final String IMAGE = "d4y.image";

    /** Kodierte Named-Volume-Deklaration, mit der der Container erzeugt wurde (für Drift-Vergleich). */
    public static final String VOLUMES = "d4y.volumes";

    private D4yLabels() {
    }
}
