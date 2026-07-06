export type AppState =
  | "IN_SYNC"
  | "DRIFT"
  | "OUTDATED"
  | "MISSING"
  | "STOPPED"
  | "RECONCILING"
  | "HOLD"
  | "ERROR";

export type HoldType = "temp-param" | "stop" | "hold";

export interface Hold {
  type: HoldType;
  /** Restdauer in Sekunden zum Zeitpunkt des Ladens (wird beim Mount in endsAt umgerechnet). */
  secs: number;
}

export interface Volume {
  name: string;
  type: "Named" | "Bind";
  persist: string;
  backup: boolean;
  store: string;
  restore: string;
}

export interface TempParam {
  key: string;
  value: string;
  by: string;
  since: string;
}

export interface Application {
  name: string;
  image: string;
  actualImage?: string;
  state: AppState;
  running: boolean;
  routes: string[];
  backup: boolean;
  containerId: string | null;
  server: string;
  hold: Hold | null;
  volumes: Volume[];
  tempParams: TempParam[];
}

export interface UndeclaredContainer {
  appName: string;
  image: string;
  containerId: string;
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
