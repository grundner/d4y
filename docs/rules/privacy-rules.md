# Privacy Rules — Datenschutz und Zugangsdaten

Dieses Dokument enthält übergreifende Datenschutz- und Vertraulichkeitsregeln, insbesondere
zum Umgang mit Zugangsdaten (Credentials).

## PR-1 — Datensparsamkeit bei der Inbetriebnahme

Der Bootstrap erfragt ausschließlich die minimal notwendigen Angaben: das
[Config-Repository](../domain/config-repository.md) und die erforderlichen Zugangsdaten.
Keine darüber hinausgehenden personenbezogenen oder sensiblen Daten werden abgefragt.
→ [ADR-0008](../decisions/0008-bootstrap-single-command-install.md)

## PR-2 — Vertraulichkeit von Credentials

Zugangsdaten für Config-Repository und [Registries](../domain/registry.md) sind vertraulich.
Sie werden nicht im Klartext protokolliert und nicht im Config-Repository selbst abgelegt.

## PR-3 — Keine Geheimnisse in der Statusanzeige

Das read-only Frontend ([status-view](../ui/status-view.md)) und die API geben niemals
Zugangsdaten oder Geheimnisse aus.

## PR-4 — Least Privilege

Zugangsdaten werden mit den geringstmöglichen Rechten vergeben, die für Repo-Zugriff und
Image-Pull erforderlich sind.

## PR-5 — Schutz von Backups

[Backups](../domain/backup.md) können sensible Anwendungsdaten enthalten. Der
[Backup-Store](../domain/backup-store.md) und die zugehörigen Zugangsdaten sind entsprechend
vertraulich zu behandeln (Least Privilege, keine Klartext-Protokollierung, keine Ausgabe in der
Statusanzeige). Der Zugriff auf Backups ist auf das für Backup und Restore notwendige Maß zu
beschränken.

## PR-6 — Debug-Zugriff und operative Aktionen

[Operative Aktionen](../domain/operational-action.md) mit Zugriff auf Logs oder exec/Shell können
Geheimnisse und personenbezogene Daten offenlegen. Solcher Zugriff unterliegt Zugriffskontrolle
und Least Privilege, wird **auditiert** und gibt Zugangsdaten/Secrets niemals in der
Statusanzeige aus. → [ADR-0012](../decisions/0012-operational-actions-and-reconciliation-hold.md)

## PR-7 — Schutz der DNS-Provider-Zugangsdaten

Im managed-Modus der DNS-Verwaltung sind die Zugangsdaten des
[DNS-Providers](../domain/dns-provider.md) vertraulich zu behandeln (Least Privilege, keine
Klartext-Protokollierung, keine Ausgabe in der Statusanzeige) und auf das für die
Record-Verwaltung notwendige Maß zu beschränken.

## PR-8 — Gelieferte Image/Container-Secrets

Zugangsdaten, die Container/Images betreffen (Registry-Credentials, App-Env-Secrets), liegen
**weder im Config-Repository noch in d4ys Properties**. Sie werden extern gehalten (z. B.
GitHub-Actions-Secrets) und per **authentifiziertem Push über TLS** geliefert
([ADR-0023](../decisions/0023-push-triggered-reconcile-and-trigger-auth.md),
[ADR-0024](../decisions/0024-delivered-image-secrets-encrypted-store.md)). d4y persistiert sie
ausschließlich **verschlüsselt** (AES-GCM) und gibt Secret-Werte sowie den `X-Registry-Auth`-Header
**niemals** in Logs oder über die API/Statusanzeige aus. Der Verschlüsselungs-Schlüssel und das
Trigger-Token sind host/d4y-Credentials und unterliegen PR-2/PR-4.

> Konkrete Mechanismen zur Ablage und Verteilung von Geheimnissen sind eine
> **Implementierungsentscheidung** und werden per ADR festgelegt.
