#!/bin/sh
# d4y — 1-Zeiler-Installer (ADR-0027). d4y läuft direkt auf dem Host (kein Container), als
# selbst-enthaltendes Bundle (App + eingebettetes JRE) unter systemd. Kein System-Java nötig.
#
#   curl -fsSL https://grundner.github.io/d4y/install.sh | sh
#
# Konfiguration über Umgebungsvariablen:
#   D4Y_HOST        (Pflicht)   öffentlicher Hostname von d4y (DNS A-Record → dieser Host)
#   D4Y_ACME_EMAIL  (Pflicht)   E-Mail für Let's Encrypt (ACME)
#   D4Y_BUNDLE_URL  (optional)  Default: GitHub-Release-Asset (linux/x86_64)
#
# Docker wird bei Bedarf automatisch installiert (Linux, get.docker.com; benötigt root/sudo) — d4y
# orchestriert Traefik und Apps weiter über den Docker-Socket.
# Sicherheit: Prüfe das Skript vor der Ausführung (curl ... -o install.sh; less install.sh).
set -eu

BUNDLE_URL="${D4Y_BUNDLE_URL:-https://github.com/grundner/d4y/releases/latest/download/d4y-linux-x86_64.tar.gz}"
INSTALL_DIR="/opt/d4y"
DATA_DIR="/var/lib/d4y"
ENV_FILE="/etc/d4y/d4y.env"
UNIT_FILE="/etc/systemd/system/d4y.service"
NETWORK="d4y"

die() { echo "FEHLER: $*" >&2; exit 1; }

[ -n "${D4Y_HOST:-}" ] || die "D4Y_HOST nicht gesetzt (öffentlicher Hostname, z. B. d4y.example.com)."
[ -n "${D4Y_ACME_EMAIL:-}" ] || die "D4Y_ACME_EMAIL nicht gesetzt (E-Mail für Let's Encrypt)."
command -v curl >/dev/null 2>&1 || die "curl nicht gefunden."
[ "$(uname -s)" = "Linux" ] || die "d4y läuft als Host-Bundle nur unter Linux."
[ "$(uname -m)" = "x86_64" ] || die "Nur linux/x86_64-Bundle verfügbar (erkannt: $(uname -m))."
command -v systemctl >/dev/null 2>&1 || die "systemd (systemctl) erforderlich."

# Privileg für System-Installation und Docker bestimmen.
SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  command -v sudo >/dev/null 2>&1 || die "Root-Rechte erforderlich. Als root ausführen oder sudo bereitstellen."
  SUDO="sudo"
fi

# Docker bei Bedarf installieren (Linux, offizielles Convenience-Skript get.docker.com).
if ! command -v docker >/dev/null 2>&1; then
  echo "› Docker nicht gefunden — installiere Docker (get.docker.com) …"
  curl -fsSL https://get.docker.com | $SUDO sh
  $SUDO systemctl enable --now docker >/dev/null 2>&1 || true
  command -v docker >/dev/null 2>&1 || die "Docker-Installation fehlgeschlagen."
fi

gen_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 32
  else
    head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'
  fi
}

# Vorhandene Credentials übernehmen (Re-Install soll Token/Key nicht rotieren — sonst wäre der
# verschlüsselte Secret-Store nicht mehr lesbar).
TOKEN=""; KEY=""
if $SUDO test -f "$ENV_FILE"; then
  EXISTING="$($SUDO cat "$ENV_FILE")"
  TOKEN="$(printf '%s\n' "$EXISTING" | sed -n 's/^D4Y_TRIGGER_TOKEN=//p')"
  KEY="$(printf '%s\n' "$EXISTING" | sed -n 's/^D4Y_SECRETS_ENCRYPTION_KEY=//p')"
fi
[ -n "$TOKEN" ] || TOKEN="$(gen_secret)"
[ -n "$KEY" ] || KEY="$(gen_secret)"

# Persistenzverzeichnisse anlegen.
$SUDO mkdir -p "$DATA_DIR/desired" "$DATA_DIR/traefik-dynamic" /etc/d4y

# Bundle laden und nach /opt/d4y entpacken (bestehenden Service vorher stoppen).
echo "› Lade d4y-Bundle …"
TMP_TGZ="$(mktemp)"
curl -fsSL "$BUNDLE_URL" -o "$TMP_TGZ" || die "Bundle-Download fehlgeschlagen: $BUNDLE_URL"
$SUDO systemctl stop d4y >/dev/null 2>&1 || true
$SUDO rm -rf "$INSTALL_DIR"
$SUDO tar -xzf "$TMP_TGZ" -C /opt          # Tarball enthält top-level 'd4y/' → /opt/d4y
rm -f "$TMP_TGZ"
[ -x "$INSTALL_DIR/bin/d4y" ] || die "Bundle unvollständig: $INSTALL_DIR/bin/d4y fehlt."

# Docker-Netz für Traefik/Apps (idempotent; d4y stellt es sonst beim Start selbst sicher).
$SUDO docker network create "$NETWORK" >/dev/null 2>&1 || true

# Environment-Datei schreiben (0600).
printf '%s\n' \
  "D4Y_HOST=$D4Y_HOST" \
  "D4Y_INGRESS_TLS_ACME_EMAIL=$D4Y_ACME_EMAIL" \
  "D4Y_TRIGGER_TOKEN=$TOKEN" \
  "D4Y_SECRETS_ENCRYPTION_KEY=$KEY" \
  "D4Y_DESIRED_STATE_PATH=$DATA_DIR/desired" \
  "D4Y_SECRETS_FILE=$DATA_DIR/.d4y-secrets" \
  "D4Y_INGRESS_SELF_DYNAMIC_DIR=$DATA_DIR/traefik-dynamic" \
  | $SUDO tee "$ENV_FILE" >/dev/null
$SUDO chmod 600 "$ENV_FILE"

# systemd-Unit installieren und starten.
$SUDO tee "$UNIT_FILE" >/dev/null <<'EOF'
[Unit]
Description=d4y — Git-native Runtime Platform
Documentation=https://github.com/grundner/d4y
Requires=docker.service
After=docker.service network-online.target
Wants=network-online.target

[Service]
Type=simple
EnvironmentFile=/etc/d4y/d4y.env
WorkingDirectory=/var/lib/d4y
ExecStart=/opt/d4y/bin/d4y
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

echo "› Starte d4y (systemd) …"
$SUDO systemctl daemon-reload
$SUDO systemctl enable --now d4y

cat <<EOF

✓ d4y läuft als systemd-Service.

  Status:  systemctl status d4y
  Logs:    journalctl -u d4y -f
  Neustart: systemctl restart d4y

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
