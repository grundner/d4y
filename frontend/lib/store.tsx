"use client";

import * as React from "react";

interface D4yContextValue {
  showSnack: (message: string) => void;
  autoRefresh: boolean;
  toggleAutoRefresh: () => void;
  refreshing: boolean;
  manualRefresh: () => void;
  /** Zähler, der bei jedem Refresh (manuell oder auto) steigt — für Daten-Reloads. */
  refreshSignal: number;
}

const D4yContext = React.createContext<D4yContextValue | null>(null);

export function D4yProvider({ children }: { children: React.ReactNode }) {
  const [snack, setSnack] = React.useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);
  const [refreshSignal, setRefreshSignal] = React.useState(0);

  const refreshTimeout = React.useRef<ReturnType<typeof setTimeout>>();
  const triggerRefresh = React.useCallback(() => {
    setRefreshing(true);
    setRefreshSignal((n) => n + 1);
    clearTimeout(refreshTimeout.current);
    refreshTimeout.current = setTimeout(() => setRefreshing(false), 950);
  }, []);

  // Auto-Refresh: löst periodisch ein Re-Fetch aller an refreshSignal gebundenen Hooks aus.
  React.useEffect(() => {
    const iv = setInterval(() => {
      if (autoRefresh) triggerRefresh();
    }, 9000);
    return () => clearInterval(iv);
  }, [autoRefresh, triggerRefresh]);

  const snackTimeout = React.useRef<ReturnType<typeof setTimeout>>();
  const showSnack = React.useCallback((message: string) => {
    setSnack(message);
    clearTimeout(snackTimeout.current);
    snackTimeout.current = setTimeout(() => setSnack(null), 4000);
  }, []);

  const value: D4yContextValue = {
    showSnack,
    autoRefresh,
    toggleAutoRefresh: () => setAutoRefresh((v) => !v),
    refreshing,
    manualRefresh: triggerRefresh,
    refreshSignal,
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
