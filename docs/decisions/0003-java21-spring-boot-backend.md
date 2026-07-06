# ADR-0003: Backend auf Basis von Java 21 und Spring Boot

Status: Accepted
Datum: 2026-07-06
Angenommen: 2026-07-06
Betrifft: [architecture/overview](../architecture/overview.md)

## Kontext

D4Y benötigt ein Backend, das den Reconciliation-Loop ausführt, Container-Backends steuert und
eine API für das Frontend bereitstellt. Es soll robust, wartbar und im Team gut beherrschbar sein.

## Entscheidung

Das Backend wird auf Basis von **Java 21** und **Spring Boot** umgesetzt.

## Konsequenzen

- **Positiv:** Reifes Ökosystem, gute Bibliotheksunterstützung (u. a. für Docker/HTTP/Git),
  langfristige Wartbarkeit, moderne Java-Sprachfeatures (Records, Pattern Matching, Virtual Threads).
- **Negativ:** JVM-Laufzeit als Grundvoraussetzung; Startzeit/Footprint größer als bei
  nativen Alternativen (später via GraalVM/Native Image adressierbar).

## Alternativen

- Go/Rust — verworfen für die erste Ausbaustufe (Team-Fit, Ökosystem-Fit zu Spring).
- Ältere Java-LTS-Version — verworfen (Java 21 bietet relevante moderne Features).
