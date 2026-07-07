"use client";

import * as React from "react";
import { Suspense } from "react";
import NextLink from "next/link";
import { useSearchParams } from "next/navigation";
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
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import StatusChip from "@/components/StatusChip";
import { useD4y } from "@/lib/store";
import { useStatus } from "@/lib/api";
import type { AppState } from "@/lib/types";

function ApplicationsInner() {
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
        <MuiLink component={NextLink} href={`/applications/${p.row.name}`} sx={{ fontWeight: 500 }}>
          {p.row.name}
        </MuiLink>
      ),
    },
    {
      field: "desiredImage",
      headerName: "Image",
      flex: 1.6,
      minWidth: 220,
      renderCell: (p) => (
        <Box sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>{p.row.desiredImage}</Box>
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
      field: "running",
      headerName: "Läuft",
      width: 100,
      renderCell: (p) =>
        p.row.running ? (
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ color: "success.main" }}>
            <CheckIcon fontSize="small" />
            <span>Ja</span>
          </Stack>
        ) : (
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ color: "text.disabled" }}>
            <CloseIcon fontSize="small" />
            <span>Nein</span>
          </Stack>
        ),
    },
    {
      field: "containerId",
      headerName: "Container-ID",
      flex: 1,
      minWidth: 150,
      renderCell: (p) => (
        <Box sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>
          {p.row.containerId ? String(p.row.containerId).slice(0, 12) : "—"}
        </Box>
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
          Backend nicht erreichbar ({error}). Läuft das D4Y-Backend auf <code>:8080</code>?
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
                    <TableCell>App-Name</TableCell>
                    <TableCell>Image</TableCell>
                    <TableCell>Container-ID</TableCell>
                    <TableCell align="right">Aktion</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {undeclared.map((u) => (
                    <TableRow key={u.containerId}>
                      <TableCell sx={{ color: "text.secondary" }}>{u.appName || "—"}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{u.image}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>
                        {String(u.containerId).slice(0, 12)}
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          color="error"
                          variant="outlined"
                          startIcon={<DeleteOutlineIcon />}
                          onClick={() =>
                            showSnack(`Nicht deklarierter Container ${u.image} entfernt (operative Aktion).`)
                          }
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
        Route(s), Backup-Policy und Hold sind noch nicht Teil von <code>GET /api/status</code> und folgen mit den
        entsprechenden Backend-Ausbaustufen.
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
