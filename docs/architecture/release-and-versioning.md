# Release & Versionierung

Wie das d4y-Runtime-Artefakt versioniert, gebaut und publiziert wird. Grundlage:
[ADR-0027](../decisions/0027-d4y-host-bundle-systemd.md) (Host-Bundle statt Container-Image), das
[ADR-0022](../decisions/0022-release-versioning-image-pipeline.md) und
[ADR-0006](../decisions/0006-single-container-image-backend-frontend.md) ablöst; die SemVer/Git-Tag-
Versions-Wahrheit aus ADR-0022 bleibt gültig.

d4y ist stateless ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) — hier geht es
ausschließlich um die Versionierung und Auslieferung des **Runtime-Bundles**, nicht um Daten.

## Versionierung — eine Quelle der Wahrheit

- **Git-Tag `vX.Y.Z` (SemVer) ist die Version.** Ein Release entsteht durch das Setzen eines Tags.
- Die Gradle-Version wird in CI daraus abgeleitet (`-Pversion=${TAG#v}`). Zwischen Releases gilt die
  Dev-Default `0.1.0-SNAPSHOT` aus `build.gradle.kts`.
- Diese eine Version wird überall propagiert:
  - `springBoot { buildInfo() }` → `/actuator/info` zeigt Version & Build-Zeit.
  - Der OpenAPI-Vertrag ([ADR-0021](../decisions/0021-published-api-contract-openapi.md)) liest
    `info.version` zur Laufzeit aus `BuildProperties`.
  - Der Bundle-Dateiname enthält die Projektversion.

## Bundle-Build (ADR-0027)

d4y läuft direkt auf dem Host (kein Container-Image mehr). Der Build erzeugt ein selbst-enthaltendes
Bundle mit eingebettetem Minimal-JRE — **kein System-Java** auf dem Ziel nötig:

- **`./gradlew bundleTar`** erzeugt `build/dist/d4y-<version>.tar.gz`. Kette:
  `bootJar` (Fat-Jar inkl. eingebettetem Frontend, ADR-0006-Kern) → `jlinkRuntime` (Minimal-JRE via
  `jlink`) → `jpackageImage` (`jpackage --type app-image` bündelt App + JRE) → `bundleTar` (System-`tar`,
  erhält das Ausführbar-Bit von `bin/d4y`).
- Das jlink-Modul-Set nutzt pragmatisch `java.se` plus die von Netty/TLS/JGit benötigten
  `jdk.*`-Module; bei Bedarf später via `jdeps` trimmen.
- Das Bundle entpackt zu `d4y/` mit `bin/d4y` (Launcher) und eingebettetem Runtime.

## Publish — automatisch via GitHub Actions (`.github/workflows/release.yml`)

| Auslöser | Ergebnis |
|---|---|
| Push auf `main` | nur `verify` (Build/Tests) |
| Pull Request | nur `verify` (Build/Tests) |
| Tag `vX.Y.Z` | Bundle je Architektur bauen und als **GitHub-Release-Assets** `d4y-linux-x86_64.tar.gz` **und** `d4y-linux-aarch64.tar.gz` anhängen |

Der `bundle`-Job (`permissions: contents: write`) läuft als **Matrix über native Runner**
(`ubuntu-latest` = x86_64, `ubuntu-24.04-arm` = aarch64), weil jlink/jpackage nicht cross-kompilieren.
Jeder Job lädt sein arch-spezifisches Tarball per `gh release upload` an das Release `vX.Y.Z`. Der
Installer wählt nach `uname -m` und bezieht es anonym über
`https://github.com/grundner/d4y/releases/latest/download/d4y-linux-<arch>.tar.gz`. **Keine Registry,
kein GHCR, keine Paket-Sichtbarkeit** mehr zu pflegen.

## Release-Schritte (Kurzform)

1. `CHANGELOG.md` aktualisieren, nach `main` mergen.
2. Annotierten Tag setzen: `git tag -a v0.2.0 -m "…" && git push origin v0.2.0`.
3. CI baut das Bundle und hängt `d4y-linux-x86_64.tar.gz` an das GitHub-Release `v0.2.0`.

## Lokaler Build / Fallback

```bash
./gradlew bundleTar -Pversion=<version>   # build/dist/d4y-<version>.tar.gz
```

Hinweis: Das Bundle ist plattformgebunden — auf einem Linux/x86_64-Runner bauen, damit das eingebettete
JRE und die nativen Netty-epoll-Transporte zur Zielplattform passen.
