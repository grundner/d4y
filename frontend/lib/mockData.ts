import type {
  Application,
  ExtraProject,
  ActivityEntry,
  NodeInfo,
  RegistryInfo,
  BackupStoreInfo,
  DnsProviderInfo,
} from "./types";

// Mock-Daten, die dem Design entsprechen. Der Status-Screen ist konzeptionell an
// GET /api/status gebunden (ADR-0029: Compose-Projekte mit Services); alles Übrige
// läuft dem Backend bewusst voraus.

export const APPS: Application[] = [
  {
    name: "shop",
    state: "RUNNING",
    hold: null,
    services: [
      { name: "web", image: "nginx:1.27-alpine", state: "running" },
      { name: "app", image: "ghcr.io/acme/shop:2.4.1", state: "running" },
    ],
    routes: [{ host: "www.example.com", path: "/", port: 80, tls: true }],
  },
  {
    name: "api-gateway",
    state: "PARTIAL",
    hold: { type: "temp-param", secs: 11 * 60 + 42 },
    services: [
      { name: "gateway", image: "ghcr.io/acme/api-gateway:2.4.1", state: "running" },
      { name: "worker", image: "ghcr.io/acme/api-worker:2.4.1", state: "exited" },
    ],
    routes: [{ host: "api.example.com", path: "/", port: 8080, tls: true }],
  },
  {
    name: "postgres",
    state: "RUNNING",
    hold: null,
    services: [{ name: "db", image: "postgres:16.2", state: "running" }],
    routes: [],
  },
  {
    name: "redis",
    state: "STOPPED",
    hold: { type: "stop", secs: 24 * 60 },
    services: [{ name: "cache", image: "redis:7.2-alpine", state: "exited" }],
    routes: [],
  },
  {
    name: "worker",
    state: "MISSING",
    hold: null,
    services: [{ name: "worker", image: "ghcr.io/acme/worker:1.9.0", state: "created" }],
    routes: [],
  },
  {
    name: "grafana",
    state: "RUNNING",
    hold: null,
    services: [{ name: "grafana", image: "grafana/grafana:11.0.0", state: "running" }],
    routes: [{ host: "metrics.example.com", path: "/", port: 3000, tls: true }],
  },
  {
    name: "minio",
    state: "RUNNING",
    hold: null,
    services: [{ name: "minio", image: "minio/minio:RELEASE.2026-05-01", state: "running" }],
    routes: [{ host: "s3.example.com", path: "/", port: 9000, tls: true }],
  },
];

export const UNDECLARED: ExtraProject[] = [
  { name: "traefik", services: [{ name: "proxy", image: "traefik:v3.0", state: "running" }] },
  { name: "scratch", services: [{ name: "box", image: "busybox:latest", state: "exited" }] },
];

export const ACTIVITY: ActivityEntry[] = [
  { time: "2026-07-06 14:32:10", actor: "alice@grundner.io", app: "api-gateway", type: "temp-param", result: "OK", drift: true, hold: "15 min · Rest 11:42" },
  { time: "2026-07-06 14:28:44", actor: "system", app: "redis", type: "hold-set", result: "OK", drift: true, hold: "30 min · Rest 24:00" },
  { time: "2026-07-06 14:10:02", actor: "bob@grundner.io", app: "nginx", type: "restart", result: "OK", drift: false, hold: "—" },
  { time: "2026-07-06 13:58:19", actor: "alice@grundner.io", app: "worker", type: "inspect", result: "OK", drift: false, hold: "—" },
  { time: "2026-07-06 13:40:00", actor: "system", app: "api-gateway", type: "hold-expired", result: "OK", drift: false, hold: "abgelaufen" },
  { time: "2026-07-06 13:12:55", actor: "bob@grundner.io", app: "redis", type: "stop", result: "OK", drift: true, hold: "30 min" },
  { time: "2026-07-06 12:47:31", actor: "alice@grundner.io", app: "postgres", type: "restart", result: "FEHLER", drift: false, hold: "—" },
];

export const NODES: NodeInfo[] = [
  { id: "node-fra-1", host: "fra1.d4y.internal", containers: 5, status: "IN_SYNC" },
  { id: "node-fra-2", host: "fra2.d4y.internal", containers: 4, status: "IN_SYNC" },
];

export const REGISTRIES: RegistryInfo[] = [
  { name: "docker.io", note: "Öffentliche Basis-Images (nginx, redis, postgres)" },
  { name: "ghcr.io/acme", note: "Interne Images (api-gateway, worker)" },
];

export const BACKUP_STORES: BackupStoreInfo[] = [
  { name: "s3://d4y-backups-eu", kind: "S3-kompatibel (MinIO)", status: "Erreichbar" },
  { name: "tarsnap:offsite", kind: "Off-site, verschlüsselt", status: "Erreichbar" },
];

export const DNS_PROVIDERS: DnsProviderInfo[] = [
  { name: "Cloudflare", mode: "managed", note: "Autoritative Records, von D4Y verwaltet" },
  { name: "Externer Eintrittspunkt", mode: "extern", note: "Records außerhalb von D4Y gepflegt" },
];

export const CONFIG_REPO = {
  repository: "git@github.com:acme/d4y-config.git",
  branch: "main",
  commit: '3f9a2c1 · „chore: bump api-gateway auf 2.4.1"',
  author: "alice@grundner.io · vor 6 min",
  version: "main @ 3f9a2c1",
  reconcile: "Reconcile vor 42 s · OK",
};

export const LOG_SEED = [
  { ts: "14:33:58", lvl: "INFO", color: "#4fc1ff", msg: "listening on :8080" },
  { ts: "14:33:58", lvl: "INFO", color: "#4fc1ff", msg: "connected to upstream postgres.d4y.internal:5432" },
  { ts: "14:34:02", lvl: "DEBUG", color: "#c586c0", msg: "route table loaded (3 hosts)" },
  { ts: "14:34:10", lvl: "WARN", color: "#dcdcaa", msg: "deprecated header X-Legacy-Auth ignored" },
  { ts: "14:34:11", lvl: "INFO", color: "#4fc1ff", msg: "GET /health 200 1.2ms" },
  { ts: "14:34:15", lvl: "INFO", color: "#4fc1ff", msg: "GET /v1/orders 200 34ms" },
  { ts: "14:34:19", lvl: "ERROR", color: "#f48771", msg: "upstream timeout GET /v1/inventory (retry 1/3)" },
  { ts: "14:34:20", lvl: "INFO", color: "#4fc1ff", msg: "GET /v1/inventory 200 812ms (recovered)" },
  { ts: "14:34:25", lvl: "DEBUG", color: "#c586c0", msg: "LOG_LEVEL=debug (temporärer Override, Hold aktiv)" },
];

export const TERM_LINES = [
  { t: "cmd", text: "cat /etc/os-release | head -1" },
  { t: "out", text: 'PRETTY_NAME="Alpine Linux v3.20"' },
  { t: "cmd", text: "curl -s localhost:8080/health" },
  { t: "out", text: '{"status":"ok","version":"2.3.0"}' },
  { t: "cmd", text: "env | grep LOG" },
  { t: "out", text: "LOG_LEVEL=debug" },
];

export const LOG_TEMPLATES = [
  { lvl: "INFO", color: "#4fc1ff", msg: () => `GET /v1/orders 200 ${(18 + Math.random() * 70) | 0}ms` },
  { lvl: "INFO", color: "#4fc1ff", msg: () => `GET /health 200 1.${(Math.random() * 9) | 0}ms` },
  { lvl: "INFO", color: "#4fc1ff", msg: () => `POST /v1/checkout 201 ${(40 + Math.random() * 120) | 0}ms` },
  { lvl: "DEBUG", color: "#c586c0", msg: () => `cache hit key=session:${Math.random().toString(16).slice(2, 8)}` },
  { lvl: "DEBUG", color: "#c586c0", msg: () => `pool: ${(3 + Math.random() * 8) | 0} active / 20 idle` },
  { lvl: "WARN", color: "#dcdcaa", msg: () => `slow upstream response ${(600 + Math.random() * 400) | 0}ms` },
  { lvl: "INFO", color: "#4fc1ff", msg: () => `GET /v1/pricing 200 ${(30 + Math.random() * 60) | 0}ms` },
  { lvl: "ERROR", color: "#f48771", msg: () => "upstream 503 GET /v1/pricing (retry 1/3)" },
];
