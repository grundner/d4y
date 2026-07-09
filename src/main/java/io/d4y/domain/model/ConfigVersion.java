package io.d4y.domain.model;

/**
 * Aktueller Stand des Config-Repositories (Sollzustands-Quelle).
 *
 * @param commit  gekürzte Commit-ID
 * @param branch  Branch/Ref
 * @param author  Autor des Commits
 * @param message Kurz-Commit-Message
 * @param time    Commit-Zeit (ISO-8601, UTC)
 */
public record ConfigVersion(String commit, String branch, String author, String message, String time) {
}
