# ADR-0022: Release-Versionierung und Image-Pipeline (SemVer/Git-Tag, Buildpacks, GHCR)

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0002](0002-immutable-images-no-build-on-target.md), [ADR-0006](0006-single-container-image-backend-frontend.md), [ADR-0008](0008-bootstrap-single-command-install.md), [architecture/release-and-versioning](../architecture/release-and-versioning.md)

## Kontext

Die bestehenden ADRs **setzen ein publiziertes d4y-Runtime-Image voraus** — ADR-0002 (unveränderliche
Images aus vertrauenswürdiger Registry), ADR-0006 (Backend+Frontend als **ein** Image), ADR-0008
(Bootstrap bezieht & startet dieses Image) — **regeln aber nicht**, wie das d4y-Image versioniert,
gebaut und in eine Registry gepusht wird. Der Ist-Stand bestätigt die Lücke:

- Kein Dockerfile, kein `bootBuildImage`, kein Jib — `./gradlew build` erzeugt nur ein Fat-Jar.
- Version `0.1.0` ist **dreifach hartkodiert und divergent** (`build.gradle.kts` `-SNAPSHOT`,
  `frontend/package.json`, `OpenApiConfig`-Literal). Keine Git-Tags, kein CHANGELOG,
  `/actuator/info` leer.
- Keine Build-/Publish-CI (nur das OpenAPI-Drift-Gate, ADR-0021).

Für den Live-Test wird ein reproduzierbarer Release-Prozess gebraucht. **Kein** persistenter
d4y-Zustand ist im Spiel — d4y bleibt stateless (ADR-0001/0013); es geht ausschließlich um die
Versionierung und Auslieferung des **Runtime-Images**.

## Entscheidung

1. **SemVer, Git-Tag ist die einzige Versions-Wahrheit.** Ein Release ist ein annotierter Tag
   `vX.Y.Z`. Die Gradle-Version wird daraus abgeleitet (CI: `-Pversion=${TAG#v}`). Zwischen Releases
   ist die Dev-Default `0.1.0-SNAPSHOT`.
2. **Eine Version, überall propagiert.** `springBoot { buildInfo() }` erzeugt
   `build-info.properties` → füllt `/actuator/info` und liefert einen `BuildProperties`-Bean. Der
   OpenAPI-Vertrag (ADR-0021) bezieht seine `info.version` **zur Laufzeit** aus `BuildProperties`
   statt aus einem Literal. Image-Tag = Projektversion. Damit endet die Divergenz.
3. **Image-Build via Cloud Native Buildpacks (`bootBuildImage`, Paketo).** Kein Dockerfile zu
   pflegen — konsistent zum „pure-Java, kein Extra-Binary"-Ethos (vgl. JGit-Wahl, ADR-0019). Das
   Frontend wird ins Image eingebettet (kein `-PskipFrontend`); die Single-Image-Zusage (ADR-0006)
   bleibt gewahrt.
4. **Registry: GitHub Container Registry** (`ghcr.io/grundner/d4y`).
5. **Publish automatisch via GitHub Actions:**
   - Push auf `main` → `:edge` und `:sha-<short>` (Continuous, für Live-Tests).
   - Tag `vX.Y.Z` → `:X.Y.Z` und `:latest` (Release).
   - Pull Requests → nur `verify` (Build/Tests), **kein** Push.
6. **Manueller Fallback** bleibt möglich: lokal `./gradlew bootBuildImage --publishImage` mit
   Registry-Credentials.

## Konsequenzen

- **Positiv:** Reproduzierbare, getaggte Images aus einer einzigen Versions-Wahrheit; Version zur
  Laufzeit über `/actuator/info` und im OpenAPI-Vertrag sichtbar. Bootstrap (ADR-0008) hat endlich
  eine definierte Bezugsquelle. `:edge` ermöglicht Live-Tests ohne formales Release.
- **Positiv:** Kein Dockerfile-Wartungsaufwand; Buildpacks liefern reproduzierbare, gehärtete Basis.
- **Negativ:** Buildpack-Images sind größer und weniger transparent als ein handgeschriebenes
  Minimal-Image; Image-Build braucht einen Docker-Daemon (in CI vorhanden).
- **Negativ:** GHCR bindet den Release-Prozess an GitHub; ein Wechsel der Registry erfordert nur eine
  Konfig-Änderung, aber der Default ist gesetzt.

## Alternativen

- **Handgeschriebenes Dockerfile (`eclipse-temurin:21-jre` + Jar, layered)** — verworfen zugunsten
  wartungsfreier Buildpacks; als Option dokumentiert, falls Image-Größe/Transparenz kritisch wird.
- **Jib** — verworfen: zusätzliches Plugin/Modell ohne Mehrwert gegenüber dem bereits vorhandenen
  Spring-Boot-`bootBuildImage`.
- **Versionsableitung per Git-Plugin (axion-release u. ä.)** — verworfen für diesen Schnitt: die
  CI-seitige Ableitung aus dem Tag (`-Pversion`) genügt und bleibt abhängigkeitsarm.
- **Nur manueller Image-Push** — verworfen: nicht reproduzierbar; automatischer CI-Push ist die
  Voraussetzung für verlässliche Live-Tests.
