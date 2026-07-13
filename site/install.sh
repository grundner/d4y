#!/bin/sh
# d4y — 1-Zeiler-Installer (ADR-0026). Voll-Push, alle Artefakte auf GitHub.
#
#   curl -fsSL https://grundner.github.io/d4y/install.sh | sh
#
# Konfiguration über Umgebungsvariablen:
#   D4Y_HOST        (Pflicht)   öffentlicher Hostname von d4y (DNS A-Record → dieser Host)
#   D4Y_ACME_EMAIL  (Pflicht)   E-Mail für Let's Encrypt (ACME)
#   D4Y_IMAGE       (optional)  Default: ghcr.io/grundner/d4y:latest
#
# Docker wird bei Bedarf automatisch installiert (Linux, get.docker.com; benötigt root/sudo).
# Sicherheit: Prüfe das Skript vor der Ausführung (curl ... -o install.sh; less install.sh).
set -eu

IMAGE="${D4Y_IMAGE:-ghcr.io/grundner/d4y:latest}"
NETWORK="d4y"
NAME="d4y"
VOLUME="d4y_data"

die() { echo "FEHLER: $*" >&2; exit 1; }

[ -n "${D4Y_HOST:-}" ] || die "D4Y_HOST nicht gesetzt (öffentlicher Hostname, z. B. d4y.example.com)."
[ -n "${D4Y_ACME_EMAIL:-}" ] || die "D4Y_ACME_EMAIL nicht gesetzt (E-Mail für Let's Encrypt)."
command -v curl >/dev/null 2>&1 || die "curl nicht gefunden."

# Privileg für Docker-Installation und Container-Befehle bestimmen.
SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  command -v sudo >/dev/null 2>&1 || die "Root-Rechte erforderlich. Als root ausführen oder sudo bereitstellen."
  SUDO="sudo"
fi

# Docker bei Bedarf installieren (Linux, offizielles Convenience-Skript get.docker.com).
if ! command -v docker >/dev/null 2>&1; then
  [ "$(uname -s)" = "Linux" ] || die "Automatische Docker-Installation nur unter Linux. Bitte Docker manuell installieren."
  echo "› Docker nicht gefunden — installiere Docker (get.docker.com) …"
  curl -fsSL https://get.docker.com | $SUDO sh
  if command -v systemctl >/dev/null 2>&1; then
    $SUDO systemctl enable --now docker >/dev/null 2>&1 || true
  fi
  command -v docker >/dev/null 2>&1 || die "Docker-Installation fehlgeschlagen."
fi

if $SUDO docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  die "Container '$NAME' existiert bereits. Zum Neuaufsetzen: $SUDO docker rm -f $NAME"
fi

gen_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  else
    head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

TOKEN="$(gen_secret)"
KEY="$(gen_secret)"

# Netz + Volume vorab anlegen (idempotent) — d4y muss beim Start am d4y-Netz hängen,
# damit der von d4y gemanagte Traefik seinen Endpoint routet.
$SUDO docker network create "$NETWORK" >/dev/null 2>&1 || true
$SUDO docker volume create "$VOLUME" >/dev/null 2>&1 || true

echo "› Ziehe $IMAGE …"
$SUDO docker pull "$IMAGE" >/dev/null

echo "› Starte d4y …"
# --user 0:0: d4y verwaltet die Docker-Engine über den Socket (bereits root-äquivalent) und
# braucht Schreibzugriff auf Socket und Persistenz-Volume.
$SUDO docker run -d --name "$NAME" --restart unless-stopped \
  --user 0:0 \
  --network "$NETWORK" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$VOLUME:/data" \
  -e D4Y_TRIGGER_TOKEN="$TOKEN" \
  -e D4Y_SECRETS_ENCRYPTION_KEY="$KEY" \
  -e D4Y_INGRESS_TLS_ACME_EMAIL="$D4Y_ACME_EMAIL" \
  -e D4Y_DESIRED_STATE_PATH=/data/desired \
  -e D4Y_SECRETS_FILE=/data/.d4y-secrets \
  --label traefik.enable=true \
  --label "traefik.http.routers.d4y.rule=Host(\`$D4Y_HOST\`)" \
  --label traefik.http.routers.d4y.entrypoints=websecure \
  --label traefik.http.routers.d4y.tls=true \
  --label traefik.http.routers.d4y.tls.certresolver=le \
  --label traefik.http.services.d4y.loadbalancer.server.port=8080 \
  "$IMAGE" >/dev/null

cat <<EOF

✓ d4y läuft.

Trage diese zwei Werte als GitHub-Actions-Secrets in deinem Config-Repo ein:

  D4Y_URL           = https://$D4Y_HOST
  D4Y_TRIGGER_TOKEN = $TOKEN

Dann liefert der Config-Repo-Workflow Sollzustand + Secrets an d4y (Vorlage:
https://grundner.github.io/d4y/config-repo-workflow.yml).

Voraussetzungen für die Erreichbarkeit:
  - DNS: A-Record $D4Y_HOST -> öffentliche IP dieses Hosts
  - Ports 80/443 von außen erreichbar (ACME/HTTP-01 + HTTPS)

WICHTIG: Sichere den Encryption-Key separat — ohne ihn ist der verschlüsselte Secret-Store
bei einem Neuaufsetzen nicht lesbar:
  D4Y_SECRETS_ENCRYPTION_KEY = $KEY
EOF
