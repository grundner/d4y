# Changelog

Alle nennenswerten Änderungen an d4y. Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/) — die Version ist der Git-Tag `vX.Y.Z`
([ADR-0022](docs/decisions/0022-release-versioning-image-pipeline.md)).

## [Unreleased]

### Added
- Release-/Image-Pipeline: `bootBuildImage` (Buildpacks) → GHCR, automatisch via GitHub Actions
  (Tag `vX.Y.Z` → `:X.Y.Z`+`:latest`, `main` → `:edge`) — ADR-0022.
- `springBoot { buildInfo() }` → Version & Build-Zeit unter `/actuator/info`.
- Publizierter OpenAPI-Vertrag unter `/v3/api-docs.yaml`, `info.version` aus der Build-Version — ADR-0021.

### Changed
- Versions-Divergenz beseitigt: `OpenApiConfig` bezieht die Version zur Laufzeit aus `BuildProperties`
  statt aus einem hartkodierten Literal.

[Unreleased]: https://github.com/grundner/d4y/commits/main
