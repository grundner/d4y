# Release & Versionierung

Wie das d4y-Runtime-Image versioniert, gebaut und publiziert wird. Grundlage:
[ADR-0022](../decisions/0022-release-versioning-image-pipeline.md); ergänzt
[ADR-0002](../decisions/0002-immutable-images-no-build-on-target.md) (unveränderliche Images),
[ADR-0006](../decisions/0006-single-container-image-backend-frontend.md) (ein Image) und
[ADR-0008](../decisions/0008-bootstrap-single-command-install.md) (Bootstrap bezieht das Image).

d4y ist stateless ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) — hier geht es
ausschließlich um die Versionierung des **Runtime-Images**, nicht um Daten oder DB-Schemata.

## Versionierung — eine Quelle der Wahrheit

- **Git-Tag `vX.Y.Z` (SemVer) ist die Version.** Ein Release entsteht durch das Setzen eines Tags.
- Die Gradle-Version wird in CI daraus abgeleitet (`-Pversion=${TAG#v}`). Zwischen Releases gilt die
  Dev-Default `0.1.0-SNAPSHOT` aus `build.gradle.kts`.
- Diese eine Version wird überall propagiert:
  - `springBoot { buildInfo() }` → `/actuator/info` zeigt Version & Build-Zeit.
  - Der OpenAPI-Vertrag ([ADR-0021](../decisions/0021-published-api-contract-openapi.md)) liest
    `info.version` zur Laufzeit aus `BuildProperties`.
  - Der Image-Tag entspricht der Projektversion.

## Image-Build

- **`./gradlew bootBuildImage`** (Cloud Native Buildpacks / Paketo) erzeugt das OCI-Image aus dem
  Fat-Jar — **kein Dockerfile**. Das statische Frontend ist eingebettet (Single-Image, ADR-0006).
- Image-Name: `ghcr.io/grundner/d4y:<version>`. Ein Docker-Daemon wird benötigt.

## Publish — automatisch via GitHub Actions (`.github/workflows/release.yml`)

| Auslöser | Image-Tags | Zweck |
|---|---|---|
| Push auf `main` | `edge`, `sha-<short>` | Continuous, für Live-Tests |
| Tag `vX.Y.Z` | `X.Y.Z`, `latest` | Release |
| Pull Request | — (nur Build/Tests) | Verifikation, kein Push |

Registry: **GHCR** (`ghcr.io/grundner/d4y`), Login in CI via `GITHUB_TOKEN` (`packages: write`).

## Manueller Fallback

```bash
# lokal ein Image bauen und direkt pushen (Credentials vorausgesetzt)
./gradlew bootBuildImage --publishImage -Pversion=<version>
```

## Release-Schritte (Kurzform)

1. `CHANGELOG.md` aktualisieren, mergen nach `main` (→ `:edge` zum Testen).
2. Annotierten Tag setzen: `git tag -a v0.1.0 -m "…" && git push origin v0.1.0`.
3. CI baut & pusht `:0.1.0` + `:latest` nach GHCR.
