package io.d4y.domain.model;

/**
 * Art eines {@link Hold}. Bestimmt, welche operative Aktion den Hold gesetzt hat.
 */
public enum HoldType {
    /** Manuell gestoppt. */
    STOP,
    /** Temporäre Parameter-Overrides aktiv. */
    TEMP_PARAM,
    /** Direkt gesetzter Hold ohne spezifische Aktion. */
    MANUAL
}
