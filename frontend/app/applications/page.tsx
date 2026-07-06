"use client";

import * as React from "react";
import { Suspense } from "react";
import NextLink from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Alert,
  Box,
  Button,
  Link as MuiLink,
  Paper,
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
import ScheduleIcon from "@mui/icons-material/Schedule";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import StatusChip from "@/components/StatusChip";
import { useD4y } from "@/lib/store";
import { UNDECLARED } from "@/lib/mockData";
import { formatCountdown } from "@/lib/format";

function ApplicationsInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { apps, remaining, showSnack } = useD4y();
  const [tab, setTab] = React.useState(searchParams.get("tab") === "undeclared" ? "undeclared" : "declared");
  const [search, setSearch] = React.useState("");

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
      field: "image",
      headerName: "Image",
      flex: 1.6,
      minWidth: 220,
      renderCell: (p) => (
        <Box sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>{p.row.image}</Box>
      ),
    },
    {
      field: "state",
      headerName: "Status",
      width: 140,
      sortable: false,
      renderCell: (p) => <StatusChip status={p.row.state} />,
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
      field: "routes",
      headerName: "Route(s)",
      flex: 1,
      minWidth: 150,
      valueGetter: (_v, row) => (row.routes.length ? row.routes.join(", ") : "—"),
    },
    {
      field: "backup",
      headerName: "Backup",
      width: 100,
      renderCell: (p) =>
        p.row.backup ? (
          <Stack direction="row" spacing={0.7} alignItems="center" sx={{ color: "success.main" }}>
            <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "success.main" }} />
            <span>An</span>
          </Stack>
        ) : (
          <Typography component="span" color="text.disabled">
            Aus
          </Typography>
        ),
    },
    {
      field: "hold",
      headerName: "Hold",
      width: 120,
      sortable: false,
      renderCell: (p) =>
        p.row.hold ? (
          <Stack direction="row" spacing={0.5} alignItems="center" sx={{ color: "secondary.main" }}>
            <ScheduleIcon sx={{ fontSize: 15 }} />
            <Box component="span" sx={{ fontFamily: "monospace", fontWeight: 500 }}>
              {formatCountdown(remaining(p.row.name))}
            </Box>
          </Stack>
        ) : (
          <Typography component="span" color="text.disabled">
            —
          </Typography>
        ),
    },
  ];

  return (
    <>
      <Typography variant="h4" sx={{ fontWeight: 400 }}>
        Applications
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        Gebunden an <Box component="span" sx={{ fontFamily: "monospace" }}>GET /api/status</Box> · Sollzustand wird
        ausschließlich über Git deklariert
      </Typography>

      <Tabs value={tab} onChange={(_e, v) => setTab(v)} sx={{ borderBottom: 1, borderColor: "divider", mb: 2 }}>
        <Tab value="declared" label={`Deklariert (${apps.length})`} />
        <Tab value="undeclared" label={`Undeclared Container (${UNDECLARED.length})`} />
      </Tabs>

      {tab === "declared" && (
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
              sx={{ border: 0 }}
            />
          </Paper>
        </>
      )}

      {tab === "undeclared" && (
        <>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Verwaltete Container ohne Deklaration im Config-Repo gelten als <b>Drift</b>. „Entfernen&quot; ist eine{" "}
            <b>operative Aktion</b> und ändert den Sollzustand nicht.
          </Alert>
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
                {UNDECLARED.map((u) => (
                  <TableRow key={u.containerId}>
                    <TableCell sx={{ color: "text.secondary" }}>{u.appName}</TableCell>
                    <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{u.image}</TableCell>
                    <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>
                      {u.containerId}
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
        </>
      )}
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
