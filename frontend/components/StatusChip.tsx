"use client";

import * as React from "react";
import Chip from "@mui/material/Chip";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import WarningIcon from "@mui/icons-material/Warning";
import ErrorIcon from "@mui/icons-material/Error";
import StopIcon from "@mui/icons-material/Stop";
import type { AppState } from "@/lib/types";

type ChipColor = "success" | "warning" | "error" | "info" | "secondary" | "default";

const MAP: Record<AppState, { color: ChipColor; label: string; Icon: React.ElementType }> = {
  // Plattform-Gesamtzustand.
  IN_SYNC: { color: "success", label: "IN SYNC", Icon: CheckCircleIcon },
  DRIFT: { color: "warning", label: "DRIFT", Icon: WarningIcon },
  // Laufzeitzustand eines Compose-Projekts (ADR-0029).
  RUNNING: { color: "success", label: "RUNNING", Icon: CheckCircleIcon },
  PARTIAL: { color: "warning", label: "PARTIAL", Icon: WarningIcon },
  STOPPED: { color: "default", label: "GESTOPPT", Icon: StopIcon },
  MISSING: { color: "error", label: "FEHLT", Icon: ErrorIcon },
};

export default function StatusChip({ status }: { status: AppState }) {
  const m = MAP[status] ?? MAP.STOPPED;
  const Icon = m.Icon;
  return (
    <Chip
      size="small"
      color={m.color}
      icon={<Icon />}
      label={m.label}
      sx={{ fontWeight: 500, letterSpacing: "0.03em" }}
    />
  );
}
