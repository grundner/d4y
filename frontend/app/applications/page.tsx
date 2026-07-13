"use client";

import * as React from "react";
import { Suspense } from "react";
import NextLink from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Alert,
  Box,
  Button,
  Chip,
  Link as MuiLink,
  Paper,
  Skeleton,
  Stack,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  TextField,
  Typography,
} from "@mui/material";
import { DataGrid, type GridColDef } from "@mui/x-data-grid";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import StatusChip from "@/components/StatusChip";
import { useD4y } from "@/lib/store";
import { useStatus } from "@/lib/api";
import type { AppState } from "@/lib/types";

function ApplicationsInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { showSnack, refreshSignal } = useD4y();
  const { data, error, loading, reload } = useStatus(refreshSignal);
  const [tab, setTab] = React.useState(searchParams.get("tab") === "undeclared" ? "undeclared" : "declared");
  const [search, setSearch] = React.useState("");

  const apps = data?.applications ?? [];
  const undeclared = data?.undeclared ?? [];

  const columns: GridColDef[] = [
    {
      field: "name",
      headerName: "Name",
      flex: 1,
      minWidth: 140,
      renderCell: (p) => (
        <MuiLink component={NextLink} href={`/applications/detail?name=${encodeURIComponent(p.row.name)}`} sx={{ fontWeight: 500 }}>
          {p.row.name}
        </MuiLink>
      ),
    },
    {
      field: "services",
      headerName: "Services",
      flex: 1.6,
      minWidth: 220,
      sortable: false,
      valueGetter: (_v, row) => (row.services ?? []).map((s: { name: string }) => s.name).join(" "),
      renderCell: (p) =>
        p.row.services && p.row.services.length > 0 ? (
          <Box sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>
            {p.row.services.map((s: { name: string }) => s.name).join(", ")}
          </Box>
        ) : (
          <Box component="span" sx={{ color: "text.disabled" }}>—</Box>
        ),
    },
    {
      field: "state",
      headerName: "Status",
      width: 140,
      sortable: false,
      renderCell: (p) => <StatusChip status={p.row.state as AppState} />,
    },
    {
      field: "routes",
      headerName: "Routes",
      flex: 1,
      minWidth: 180,
      sortable: false,
      valueGetter: (_v, row) => (row.routes ?? []).map((r: { host: string }) => r.host).join(" "),
      renderCell: (p) =>
        p.row.routes && p.row.routes.length > 0 ? (
          <Box sx={{ fontFamily: "monospace", fontSize: 12.5 }}>
            {p.row.routes
              .map((r: { host: string; tls: boolean }) => `${r.tls ? "https" : "http"}://${r.host}`)
              .join(", ")}
          </Box>
        ) : (
          <Box component="span" sx={{ color: "text.disabled" }}>—</Box>
        ),
    },
  ];

  return (
    <>
      <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          Applications
        </Typography>
        {data && <StatusChip status={(data.overall as AppState) === "IN_SYNC" ? "IN_SYNC" : "DRIFT"} />}
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        Live aus <Box component="span" sx={{ fontFamily: "monospace" }}>GET /api/status</Box> · Sollzustand wird
        ausschließlich über Git deklariert
      </Typography>

      {error && !data && (
        <Alert
          severity="error"
          sx={{ mb: 2 }}
          action={
            <Button color="inherit" size="small" onClick={reload}>
              Erneut laden
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: "divider", mb: 2 }}>
        <Tab value="declared" label={`Deklariert (${apps.length})`} />
        <Tab value="undeclared" label={`Undeclared Container (${undeclared.length})`} />
      </Tabs>

      {loading && !data ? (
        <Paper variant="outlined" sx={{ p: 2 }}>
          {[...Array(6)].map((_, i) => (
            <Skeleton key={i} height={44} />
          ))}
        </Paper>
      ) : tab === "declared" ? (
        <>
          <TextField
            size="small"
            placeholder="Suchen…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            sx={{ mb: 2, width: 320, maxWidth: "60%" }}
          />
          <Paper variant="outlined">
            <DataGrid
              autoHeight
              rows={apps}
              columns={columns}
              getRowId={(r) => r.name}
              disableRowSelectionOnClick
              filterModel={{ items: [], quickFilterValues: search ? search.split(" ") : [] }}
              initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
              pageSizeOptions={[10, 25, 50]}
              localeText={{ noRowsLabel: "Keine Applications deklariert" }}
              sx={{ border: 0 }}
            />
          </Paper>
        </>
      ) : (
        <>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Verwaltete Container ohne Deklaration im Config-Repo gelten als <b>Drift</b>. „Entfernen&quot; ist eine{" "}
            <b>operative Aktion</b> und ändert den Sollzustand nicht.
          </Alert>
          {undeclared.length === 0 ? (
            <Paper variant="outlined" sx={{ p: 5, textAlign: "center", color: "text.secondary" }}>
              <Typography>Keine nicht deklarierten Container.</Typography>
            </Paper>
          ) : (
            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Projekt</TableCell>
                    <TableCell>Services</TableCell>
                    <TableCell align="right">Aktion</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {undeclared.map((u) => (
                    <TableRow
                      key={u.name}
                      hover
                      onClick={() => router.push(`/applications/undeclared?name=${encodeURIComponent(u.name)}`)}
                      sx={{ cursor: "pointer" }}
                    >
                      <TableCell sx={{ color: "text.secondary" }}>{u.name || "—"}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>
                        {u.services.map((s) => `${s.name} (${s.image})`).join(", ")}
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          color="error"
                          variant="outlined"
                          startIcon={<DeleteOutlineIcon />}
                          onClick={(e) => {
                            e.stopPropagation();
                            showSnack(`Nicht deklariertes Projekt ${u.name} entfernt (operative Aktion).`);
                          }}
                        >
                          Entfernen
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
      )}

      <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 2 }}>
        Routes werden read-only angezeigt und über den Reverse Proxy (Traefik) angewandt; Backup-Policy folgt
        mit der entsprechenden Backend-Ausbaustufe.
      </Typography>
    </>
  );
}

export default function ApplicationsPage() {
  return (
    <Suspense fallback={null}>
      <ApplicationsInner />
    </Suspense>
  );
}
