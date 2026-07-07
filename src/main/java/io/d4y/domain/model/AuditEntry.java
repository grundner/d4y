package io.d4y.domain.model;

import java.time.Instant;

/**
 * Ein Eintrag im Audit-Log operativer Aktionen.
 *
 * @param time     Zeitpunkt der Aktion
 * @param actor    Auslöser (aus Header {@code X-Actor}, Default {@code operator})
 * @param app      betroffene Application
 * @param action   Aktionstyp (z. B. {@code restart}, {@code stop}, {@code temp-param}, {@code hold-set})
 * @param result   Ergebnis ({@code OK} / {@code FEHLER})
 * @param drift    ob eine sanktionierte, temporäre Drift erzeugt wurde
 * @param holdInfo optionale Hold-Beschreibung (oder {@code null})
 */
public record AuditEntry(
        Instant time,
        String actor,
        String app,
        String action,
        String result,
        boolean drift,
        String holdInfo) {
}
