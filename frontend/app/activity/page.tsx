"use client";

import * as React from "react";
import { Box, Chip, Paper, Stack, Typography } from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import { ACTIVITY } from "@/lib/mockData";

const TYPE_COLORS: Record<string, [string, string]> = {
  inspect: ["#eeeeee", "#424242"],
  restart: ["#e3f2fd", "#0b5cad"],
  stop: ["#fff3e0", "#8a5200"],
  "temp-param": ["#fff3e0", "#8a5200"],
  "hold-set": ["#f3e5f5", "#6a1b9a"],
  "hold-expired": ["#eceff1", "#546e7a"],
};

const FILTERS = ["Alle Apps", "Aktionstyp", "Zeitraum: 24 h"];

export default function ActivityPage() {
  const columns: GridColDef[] = [
    { field: "time", headerName: "Zeitpunkt", width: 180, renderCell: (p) => <Box sx={{ fontFamily: "monospace", fontSize: 12, color: "text.secondary" }}>{p.row.time}</Box> },
    { field: "actor", headerName: "Akteur", flex: 1, minWidth: 150 },
    { field: "app", headerName: "Ziel-App", flex: 1, minWidth: 120 },
    {
      field: "type",
      headerName: "Aktion",
      width: 150,
      renderCell: (p) => {
        const c = TYPE_COLORS[p.row.type] ?? ["#eee", "#333"];
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
            sx={ok ? { bgcolor: "#edf7ed", color: "#1e4620", fontWeight: 500 } : { bgcolor: "#fdeded", color: "#5f2120", fontWeight: 500 }}
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
          <Chip size="small" label="sanktioniert" sx={{ bgcolor: "#f3e5f5", color: "#6a1b9a", fontWeight: 500 }} />
        ) : (
          <Typography component="span" color="text.disabled">
            —
          </Typography>
        ),
    },
    { field: "hold", headerName: "Hold", flex: 1, minWidth: 140, renderCell: (p) => <Box sx={{ fontSize: 12.5, color: "text.secondary" }}>{p.row.hold}</Box> },
  ];

  const rows = ACTIVITY.map((a, i) => ({ id: i, ...a }));

  return (
    <>
      <Typography variant="h4" sx={{ fontWeight: 400 }}>
        Aktivität / Audit
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        Vollständiges Audit-Log aller operativen Aktionen · sanktionierte, temporäre Drift ist markiert
      </Typography>

      <Stack direction="row" spacing={1.25} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
        {FILTERS.map((f) => (
          <Chip key={f} variant="outlined" label={f} onClick={() => {}} deleteIcon={<span>▾</span>} onDelete={() => {}} />
        ))}
      </Stack>

      <Paper variant="outlined">
        <DataGrid
          autoHeight
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
