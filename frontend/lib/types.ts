/** Gesamtzustand der Plattform (StatusResponse.overall). */
export type OverallState = "IN_SYNC" | "DRIFT";

/** Laufzeitzustand eines Compose-Projekts (ADR-0029). */
export type AppRuntimeState = "RUNNING" | "PARTIAL" | "STOPPED" | "MISSING";

/**
 * Alle vom StatusChip darstellbaren Zustände: der Laufzeitzustand einer App
 * sowie der Plattform-Gesamtzustand.
 */
export type AppState = OverallState | AppRuntimeState;

export type HoldType = "temp-param" | "stop" | "hold";

export interface Hold {
  type: HoldType;
  /** Restdauer in Sekunden zum Zeitpunkt des Ladens (wird beim Mount in endsAt umgerechnet). */
  secs: number;
}

/** Ein Service (Container) innerhalb eines Compose-Projekts. */
export interface ServiceStatus {
  name: string;
  image: string;
  /** Docker-Container-Zustand, z. B. "running", "exited", "created". */
  state: string;
}

/** Eine deklarierte Route (externer Ingress) eines Projekts. */
export interface Route {
  host: string;
  path: string;
  port: number;
  /** true → HTTPS, false → HTTP. */
  tls: boolean;
}

/** Eine Application ist ein Compose-Projekt mit mehreren Services. */
export interface Application {
  name: string;
  state: AppRuntimeState;
  hold: Hold | null;
  services: ServiceStatus[];
  routes: Route[];
}

/** Ein Compose-Projekt ohne Deklaration im Config-Repo. */
export interface ExtraProject {
  name: string;
  services: ServiceStatus[];
}

export interface ActivityEntry {
  time: string;
  actor: string;
  app: string;
  type: string;
  result: string;
  drift: boolean;
  hold: string;
}

export interface NodeInfo {
  id: string;
  host: string;
  containers: number;
  status: AppState;
}

export interface RegistryInfo {
  name: string;
  note: string;
}

export interface BackupStoreInfo {
  name: string;
  kind: string;
  status: string;
}

export interface DnsProviderInfo {
  name: string;
  mode: "managed" | "extern";
  note: string;
}
