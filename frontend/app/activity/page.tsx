"use client";

import * as React from "react";
import { Alert, Box, Chip, Paper, Stack, Typography } from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { useD4y } from "@/lib/store";
import { useActivity } from "@/lib/api";
import { formatTimestamp } from "@/lib/format";

// Dunkle, transluzente Akzent-BGs passend zur ADR-0015-Palette (Blau/Lila/Amber/Neutral).
const TYPE_COLORS: Record<string, [string, string]> = {
  inspect: ["rgba(139,147,161,0.15)", "#c7cdd6"],
  restart: ["rgba(77,184,255,0.16)", "#6ac4ff"],
  stop: ["rgba(224,169,74,0.16)", "#e0a94a"],
  "temp-param": ["rgba(224,169,74,0.16)", "#e0a94a"],
  "hold-set": ["rgba(178,141,255,0.16)", "#c3a9ff"],
  "hold-expired": ["rgba(139,147,161,0.12)", "#8b93a1"],
};

const FILTERS = ["Alle Apps", "Aktionstyp", "Zeitraum: 24 h"];

export default function ActivityPage() {
  const { refreshSignal } = useD4y();
  const { data, error, loading } = useActivity(refreshSignal, 100);

  const columns: GridColDef[] = [
    { field: "time", headerName: "Zeitpunkt", width: 180, renderCell: (p) => <Box sx={{ fontFamily: "monospace", fontSize: 12, color: "text.secondary" }}>{p.row.time}</Box> },
    { field: "actor", headerName: "Akteur", flex: 1, minWidth: 150 },
    { field: "app", headerName: "Ziel-App", flex: 1, minWidth: 120 },
    {
      field: "type",
      headerName: "Aktion",
      width: 150,
      renderCell: (p) => {
        const c = TYPE_COLORS[p.row.type] ?? ["rgba(139,147,161,0.15)", "#c7cdd6"];
        return <Chip size="small" label={p.row.type} sx={{ bgcolor: c[0], color: c[1], fontWeight: 500, fontFamily: "monospace" }} />;
      },
    },
    {
      field: "result",
      headerName: "Ergebnis",
      width: 120,
      renderCell: (p) => {
        const ok = p.row.result === "OK";
        return (
          <Chip
            size="small"
            label={p.row.result}
            sx={ok ? { bgcolor: "rgba(95,208,168,0.16)", color: "#5fd0a8", fontWeight: 500 } : { bgcolor: "rgba(244,119,107,0.16)", color: "#f4776b", fontWeight: 500 }}
          />
        );
      },
    },
    {
      field: "drift",
      headerName: "Drift",
      width: 130,
      renderCell: (p) =>
        p.row.drift ? (
          <Chip size="small" label="sanktioniert" sx={{ bgcolor: "rgba(178,141,255,0.16)", color: "#c3a9ff", fontWeight: 500 }} />
        ) : (
          <Typography component="span" color="text.disabled">
            —
          </Typography>
        ),
    },
    { field: "hold", headerName: "Hold", flex: 1, minWidth: 140, renderCell: (p) => <Box sx={{ fontSize: 12.5, color: "text.secondary" }}>{p.row.hold}</Box> },
  ];

  const rows = (data ?? []).map((a, i) => ({
    id: i,
    time: formatTimestamp(a.time),
    actor: a.actor,
    app: a.app,
    type: a.action,
    result: a.result,
    drift: a.drift,
    hold: a.hold ?? "—",
  }));

  return (
    <>
      <Typography variant="h4" sx={{ fontWeight: 400 }}>
        Aktivität / Audit
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        Vollständiges Audit-Log aller operativen Aktionen · sanktionierte, temporäre Drift ist markiert
      </Typography>

      {error && !data && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Stack direction="row" spacing={1.25} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        {FILTERS.map((f) => (
          <Chip key={f} variant="outlined" label={f} onClick={() => {}} deleteIcon={<span>▾</span>} onDelete={() => {}} />
        ))}
      </Stack>

      <Paper variant="outlined">
        <DataGrid
          autoHeight
          loading={loading && !data}
          rows={rows}
          columns={columns}
          disableRowSelectionOnClick
          initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
          pageSizeOptions={[25, 50]}
          sx={{ border: 0 }}
        />
      </Paper>
    </>
  );
}
