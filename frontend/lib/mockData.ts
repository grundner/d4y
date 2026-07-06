import type {
  Application,
  UndeclaredContainer,
  ActivityEntry,
  NodeInfo,
  RegistryInfo,
  BackupStoreInfo,
  DnsProviderInfo,
} from "./types";

// Mock-Daten, die dem Design entsprechen. Der Status-Screen ist konzeptionell an
// GET /api/status gebunden; alles Übrige läuft dem Backend bewusst voraus.

export const APPS: Application[] = [
  {
    name: "nginx",
    image: "nginx:1.27-alpine",
    state: "IN_SYNC",
    running: true,
    routes: ["www.example.com"],
    backup: false,
    containerId: "df24a1b9c3e7",
    server: "node-fra-1",
    hold: null,
    volumes: [],
    tempParams: [],
  },
  {
    name: "api-gateway",
    image: "ghcr.io/acme/api-gateway:2.4.1",
    actualImage: "ghcr.io/acme/api-gateway:2.3.0",
    state: "OUTDATED",
    running: true,
    routes: ["api.example.com"],
    backup: true,
    containerId: "a17f42db90c1",
    server: "node-fra-1",
    hold: { type: "temp-param", secs: 11 * 60 + 42 },
    volumes: [
      {
        name: "api-gateway-data",
        type: "Named",
        persist: "persistent",
        backup: true,
        store: "s3://d4y-backups-eu",
        restore: "2026-07-05 03:00",
      },
      {
        name: "/etc/api/config.yaml",
        type: "Bind",
        persist: "ephemer",
        backup: false,
        store: "—",
        restore: "—",
      },
    ],
    tempParams: [
      { key: "LOG_LEVEL", value: "debug", by: "alice@grundner.io", since: "14:32" },
    ],
  },
  {
    name: "postgres",
    image: "postgres:16.2",
    state: "IN_SYNC",
    running: true,
    routes: [],
    backup: true,
    containerId: "8f1c6b2a4d5e",
    server: "node-fra-2",
    hold: null,
    volumes: [
      {
        name: "pgdata",
        type: "Named",
        persist: "persistent",
        backup: true,
        store: "s3://d4y-backups-eu",
        restore: "2026-07-05 03:00",
      },
    ],
    tempParams: [],
  },
  {
    name: "redis",
    image: "redis:7.2-alpine",
    state: "STOPPED",
    running: false,
    routes: [],
    backup: false,
    containerId: "2b9e77a10f3c",
    server: "node-fra-2",
    hold: { type: "stop", secs: 24 * 60 },
    volumes: [],
    tempParams: [],
  },
  {
    name: "worker",
    image: "ghcr.io/acme/worker:1.9.0",
    state: "MISSING",
    running: false,
    routes: [],
    backup: true,
    containerId: null,
    server: "—",
    hold: null,
    volumes: [],
    tempParams: [],
  },
  {
    name: "grafana",
    image: "grafana/grafana:11.0.0",
    state: "IN_SYNC",
    running: true,
    routes: ["metrics.example.com"],
    backup: false,
    containerId: "5c3a8e1b7f22",
    server: "node-fra-1",
    hold: null,
    volumes: [
      {
        name: "grafana-storage",
        type: "Named",
        persist: "persistent",
        backup: false,
        store: "—",
        restore: "—",
      },
    ],
    tempParams: [],
  },
  {
    name: "minio",
    image: "minio/minio:RELEASE.2026-05-01",
    state: "IN_SYNC",
    running: true,
    routes: ["s3.example.com"],
    backup: true,
    containerId: "e0d4c9a63b18",
    server: "node-fra-2",
    hold: null,
    volumes: [
      {
        name: "minio-data",
        type: "Named",
        persist: "persistent",
        backup: true,
        store: "tarsnap:offsite",
        restore: "2026-06-30 02:00",
      },
    ],
    tempParams: [],
  },
];

export const UNDECLARED: UndeclaredContainer[] = [
  { appName: "—", image: "traefik:v3.0", containerId: "a91be3c7f204" },
  { appName: "—", image: "busybox:latest", containerId: "0c77d5e9a3b6" },
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
