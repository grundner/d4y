"use client";

import * as React from "react";
import type { AppState } from "./types";

// Schema von GET /api/status (siehe Backend StatusController).
export interface StatusHold {
  type: string;
  remainingSeconds: number;
}

export interface StatusVolume {
  name: string;
  path: string;
}

export interface StatusRoute {
  host: string;
  path: string;
  port: number;
}

export interface StatusApp {
  name: string;
  serviceName: string;
  desiredImage: string;
  state: AppState;
  running: boolean;
  containerId: string | null;
  hold?: StatusHold | null;
  volumes: StatusVolume[];
  routes: StatusRoute[];
}

export interface StatusUndeclared {
  appName: string;
  image: string;
  containerId: string;
  running: boolean;
  volumes: StatusVolume[];
}

export interface StatusResponse {
  overall: string;
  applications: StatusApp[];
  undeclared: StatusUndeclared[];
}

// Schema von GET /api/holds (siehe Backend HoldController / HoldView).
export interface HoldItem {
  app: string;
  /** Enum-Name: STOP | TEMP_PARAM | MANUAL. */
  type: string;
  remainingSeconds: number;
  expiresAt: string;
}

// Schema von GET /api/activity (siehe Backend ActivityController / ActivityView).
export interface ActivityItem {
  /** ISO-8601-Zeitpunkt (UTC). */
  time: string;
  actor: string;
  app: string;
  action: string;
  result: string;
  drift: boolean;
  hold: string | null;
}

export interface UseResource<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
}

/**
 * Generischer Loader für einen read-only GET-Endpunkt (über den Next-Proxy).
 * Re-fetcht, wenn sich `reloadKey` ändert (an Auto-/Manual-Refresh gekoppelt), und reicht
 * die Fehler-Message des Backends (JSON-Body { error, message }) ehrlich durch.
 */
function useResource<T>(path: string, reloadKey: number): UseResource<T> {
  const [data, setData] = React.useState<T | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);

  const load = React.useCallback(
    async (signal?: AbortSignal) => {
      try {
        const res = await fetch(path, { signal, cache: "no-store" });
        if (!res.ok) {
          // Das Backend liefert bei Fehlern einen JSON-Body { error, message } (siehe
          // ApiExceptionHandler) — die Message ehrlich durchreichen, statt nur "HTTP 503".
          let message = `HTTP ${res.status}`;
          try {
            const body = await res.json();
            if (body?.message) message = body.message;
          } catch {
            /* kein JSON-Body */
          }
          throw new Error(message);
        }
        const json = (await res.json()) as T;
        setData(json);
        setError(null);
      } catch (e: any) {
        if (e?.name === "AbortError") return;
        // fetch selbst wirft (TypeError) nur, wenn das Backend gar nicht antwortet.
        const message =
          e instanceof TypeError
            ? "Backend nicht erreichbar auf :8080. Läuft das D4Y-Backend?"
            : e?.message || "Unbekannter Fehler";
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    [path]
  );

  React.useEffect(() => {
    const ctrl = new AbortController();
    load(ctrl.signal);
    return () => ctrl.abort();
  }, [load, reloadKey]);

  return { data, error, loading, reload: () => load() };
}

/** Lädt den Plattformzustand von GET /api/status. */
export function useStatus(reloadKey: number): UseResource<StatusResponse> {
  return useResource<StatusResponse>("/api/status", reloadKey);
}

/** Lädt die aktiven Holds von GET /api/holds. */
export function useHolds(reloadKey: number): UseResource<HoldItem[]> {
  return useResource<HoldItem[]>("/api/holds", reloadKey);
}

/** Lädt das Audit-Log von GET /api/activity. */
export function useActivity(reloadKey: number, limit = 100): UseResource<ActivityItem[]> {
  return useResource<ActivityItem[]>(`/api/activity?limit=${limit}`, reloadKey);
}
