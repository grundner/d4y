"use client";

import * as React from "react";
import type { AppState } from "./types";

// Schema von GET /api/status (siehe Backend StatusController).
export interface StatusHold {
  type: string;
  remainingSeconds: number;
}

export interface StatusApp {
  name: string;
  desiredImage: string;
  state: AppState;
  running: boolean;
  containerId: string | null;
  hold?: StatusHold | null;
}

export interface StatusUndeclared {
  appName: string;
  image: string;
  containerId: string;
}

export interface StatusResponse {
  overall: string;
  applications: StatusApp[];
  undeclared: StatusUndeclared[];
}

interface UseStatusResult {
  data: StatusResponse | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
}

/**
 * Lädt den Plattformstatus von GET /api/status (über den Next-Proxy).
 * Re-fetcht, wenn sich `reloadKey` ändert (an Auto-/Manual-Refresh gekoppelt).
 */
export function useStatus(reloadKey: number): UseStatusResult {
  const [data, setData] = React.useState<StatusResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);

  const load = React.useCallback(async (signal?: AbortSignal) => {
    try {
      const res = await fetch("/api/status", { signal, cache: "no-store" });
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
      const json = (await res.json()) as StatusResponse;
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
  }, []);

  React.useEffect(() => {
    const ctrl = new AbortController();
    load(ctrl.signal);
    return () => ctrl.abort();
  }, [load, reloadKey]);

  return { data, error, loading, reload: () => load() };
}
