"use client";

import * as React from "react";
import Chip from "@mui/material/Chip";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import WarningIcon from "@mui/icons-material/Warning";
import ErrorIcon from "@mui/icons-material/Error";
import SyncIcon from "@mui/icons-material/Sync";
import ScheduleIcon from "@mui/icons-material/Schedule";
import StopIcon from "@mui/icons-material/Stop";
import type { AppState } from "@/lib/types";

type ChipColor = "success" | "warning" | "error" | "info" | "secondary" | "default";

const MAP: Record<AppState, { color: ChipColor; label: string; Icon: React.ElementType }> = {
  IN_SYNC: { color: "success", label: "IN SYNC", Icon: CheckCircleIcon },
  DRIFT: { color: "warning", label: "DRIFT", Icon: WarningIcon },
  OUTDATED: { color: "warning", label: "VERALTET", Icon: WarningIcon },
  MISSING: { color: "error", label: "FEHLT", Icon: ErrorIcon },
  ERROR: { color: "error", label: "FEHLER", Icon: ErrorIcon },
  RECONCILING: { color: "info", label: "RECONCILING", Icon: SyncIcon },
  HOLD: { color: "secondary", label: "GEHALTEN", Icon: ScheduleIcon },
  STOPPED: { color: "default", label: "GESTOPPT", Icon: StopIcon },
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
