"use client";

import * as React from "react";
import { APPS } from "./mockData";
import type { Application } from "./types";

interface D4yContextValue {
  apps: Application[];
  /** Restsekunden eines aktiven Holds, oder null (auch vor dem Client-Mount). */
  remaining: (appName: string) => number | null;
  releaseHold: (appName: string) => void;
  showSnack: (message: string) => void;
  autoRefresh: boolean;
  toggleAutoRefresh: () => void;
  refreshing: boolean;
  manualRefresh: () => void;
  /** Zähler, der bei jedem Refresh (manuell oder auto) steigt — für Daten-Reloads. */
  refreshSignal: number;
  mounted: boolean;
}

const D4yContext = React.createContext<D4yContextValue | null>(null);

export function D4yProvider({ children }: { children: React.ReactNode }) {
  const [apps, setApps] = React.useState<Application[]>(APPS);
  const [endsAt, setEndsAt] = React.useState<Record<string, number>>({});
  const [now, setNow] = React.useState<number>(0);
  const [mounted, setMounted] = React.useState(false);
  const [snack, setSnack] = React.useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);
  const [refreshSignal, setRefreshSignal] = React.useState(0);

  // Holds erst clientseitig auf absolute Ablaufzeitpunkte abbilden (keine Hydration-Mismatches).
  React.useEffect(() => {
    const base = Date.now();
    const ea: Record<string, number> = {};
    for (const a of APPS) {
      if (a.hold) ea[a.name] = base + a.hold.secs * 1000;
    }
    setEndsAt(ea);
    setNow(Date.now());
    setMounted(true);
    const tick = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(tick);
  }, []);

  // Auto-Refresh-Blip (nur Optik, wie im Design).
  const refreshTimeout = React.useRef<ReturnType<typeof setTimeout>>();
  React.useEffect(() => {
    const iv = setInterval(() => {
      if (autoRefresh) triggerRefresh();
    }, 9000);
    return () => clearInterval(iv);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRefresh]);

  const triggerRefresh = React.useCallback(() => {
    setRefreshing(true);
    setRefreshSignal((n) => n + 1);
    clearTimeout(refreshTimeout.current);
    refreshTimeout.current = setTimeout(() => setRefreshing(false), 950);
  }, []);

  const snackTimeout = React.useRef<ReturnType<typeof setTimeout>>();
  const showSnack = React.useCallback((message: string) => {
    setSnack(message);
    clearTimeout(snackTimeout.current);
    snackTimeout.current = setTimeout(() => setSnack(null), 4000);
  }, []);

  const remaining = React.useCallback(
    (appName: string): number | null => {
      const e = endsAt[appName];
      if (!e || !mounted) return null;
      return Math.max(0, Math.round((e - now) / 1000));
    },
    [endsAt, now, mounted]
  );

  const releaseHold = React.useCallback(
    (appName: string) => {
      setApps((prev) => prev.map((a) => (a.name === appName ? { ...a, hold: null } : a)));
      setEndsAt((prev) => {
        const next = { ...prev };
        delete next[appName];
        return next;
      });
      showSnack(`Hold für ${appName} freigegeben — D4Y reconciled den Sollzustand.`);
    },
    [showSnack]
  );

  const value: D4yContextValue = {
    apps,
    remaining,
    releaseHold,
    showSnack,
    autoRefresh,
    toggleAutoRefresh: () => setAutoRefresh((v) => !v),
    refreshing,
    manualRefresh: triggerRefresh,
    refreshSignal,
    mounted,
  };

  return (
    <D4yContext.Provider value={value}>
      {children}
      <SnackHost message={snack} />
    </D4yContext.Provider>
  );
}

export function useD4y(): D4yContextValue {
  const ctx = React.useContext(D4yContext);
  if (!ctx) throw new Error("useD4y must be used within D4yProvider");
  return ctx;
}

// Lokaler Import erst hier, um Zyklen zu vermeiden.
import { Snackbar } from "@mui/material";
function SnackHost({ message }: { message: string | null }) {
  return (
    <Snackbar
      open={!!message}
      message={message ?? ""}
      anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
      sx={{ maxWidth: 460 }}
    />
  );
}
