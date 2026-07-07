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
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const json = (await res.json()) as StatusResponse;
      setData(json);
      setError(null);
    } catch (e: any) {
      if (e?.name !== "AbortError") setError(e?.message || "Backend nicht erreichbar");
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
