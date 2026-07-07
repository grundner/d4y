package io.d4y.domain.model;

/**
 * Ergebnis einer exec-Aktion in einem Container.
 */
public record ExecResult(String output, int exitCode) {
}
