"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  List,
  ListItemButton,
  Paper,
  Skeleton,
  Stack,
  Typography,
} from "@mui/material";
import ScheduleIcon from "@mui/icons-material/Schedule";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import StatusChip from "@/components/StatusChip";
import { useD4y } from "@/lib/store";
import { useStatus } from "@/lib/api";
import { ACTIVITY } from "@/lib/mockData";
import { formatCountdown, holdTypeLabel } from "@/lib/format";
import type { AppState } from "@/lib/types";

function StatCard({ label, value, color }: { label: string; value: React.ReactNode; color: string }) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
          {label}
        </Typography>
        <Typography sx={{ fontSize: 32, fontWeight: 500, mt: 1, color }}>{value}</Typography>
      </CardContent>
    </Card>
  );
}

const ACTION_SUMMARY: Record<string, string> = {
  restart: "Neustart",
  stop: "Stop",
  "temp-param": "Temp. Parameter",
  inspect: "Inspect",
  "hold-set": "Hold gesetzt",
  "hold-expired": "Hold abgelaufen",
};

export default function DashboardPage() {
  const router = useRouter();
  const { apps: mockApps, remaining, releaseHold, refreshSignal } = useD4y();
  const { data, error, loading, reload } = useStatus(refreshSignal);

  const apps = data?.applications ?? [];
  const undeclared = data?.undeclared ?? [];
  const count = (s: AppState) => apps.filter((a) => a.state === s).length;

  // Holds und Aktivität stammen aus der operativen/Audit-Ebene (noch nicht im /api/status).
  const activeHolds = mockApps.filter((a) => a.hold);
  const feed = ACTIVITY.slice(0, 5).map((a) => {
    const who = a.actor === "system" ? "system" : a.actor.split("@")[0];
    return { clock: a.time.slice(11), summary: `${ACTION_SUMMARY[a.type] ?? a.type} · ${a.app} · ${who}` };
  });

  const attention = apps
    .filter((a) => a.state !== "IN_SYNC")
    .map((a) => {
      let reason = "";
      if (a.state === "OUTDATED") reason = `Ist-Image weicht vom Soll ${a.desiredImage.split(":").pop()} ab`;
      else if (a.state === "MISSING") reason = "Container nicht vorhanden — Reconcile ausstehend";
      else if (a.state === "STOPPED") reason = "Nicht laufend";
      else reason = "Weicht vom Sollzustand ab";
      return { name: a.name, state: a.state, reason };
    });

  const isDrift = data?.overall !== "IN_SYNC";

  return (
    <>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          Dashboard
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          Kontinuierlicher Soll-/Ist-Abgleich · observe → diff → reconcile · selbstheilend
        </Typography>
      </Box>

      {error && !data && (
        <Alert
          severity="error"
          sx={{ mb: 3 }}
          action={
            <Button color="inherit" size="small" onClick={reload}>
              Erneut laden
            </Button>
          }
        >
          {error}
        </Alert>
      )}

      {loading && !data ? (
        <Skeleton variant="rounded" height={92} sx={{ mb: 3 }} />
      ) : data ? (
        <Alert severity={isDrift ? "warning" : "success"} sx={{ mb: 3 }}>
          <AlertTitle sx={{ fontWeight: 600 }}>Gesamtstatus: {isDrift ? "DRIFT" : "IN SYNC"}</AlertTitle>
          {isDrift
            ? `${count("OUTDATED")} App(s) veraltet · ${count("MISSING")} fehlt · ${count("STOPPED")} gestoppt · ${undeclared.length} nicht deklarierte Container. Selbstheilung ist aktiv.`
            : `Alle ${apps.length} Applications entsprechen dem Sollzustand. Selbstheilung ist aktiv.`}
        </Alert>
      ) : null}

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(148px, 1fr))",
          gap: 2,
          mb: 3,
        }}
      >
        <StatCard label="Apps gesamt" value={loading && !data ? <Skeleton width={40} /> : apps.length} color="text.primary" />
        <StatCard label="In Sync" value={loading && !data ? <Skeleton width={40} /> : count("IN_SYNC")} color="success.main" />
        <StatCard label="Veraltet" value={loading && !data ? <Skeleton width={40} /> : count("OUTDATED")} color="warning.main" />
        <StatCard label="Fehlt" value={loading && !data ? <Skeleton width={40} /> : count("MISSING")} color="error.main" />
        <StatCard label="Gestoppt" value={loading && !data ? <Skeleton width={40} /> : count("STOPPED")} color="text.secondary" />
        <StatCard label="Undeclared" value={loading && !data ? <Skeleton width={40} /> : undeclared.length} color="warning.main" />
        <StatCard label="Aktive Holds" value={activeHolds.length} color="secondary.main" />
      </Box>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1.4fr 1fr" }, gap: 2, alignItems: "start" }}>
        <Paper variant="outlined">
          <Typography sx={{ p: 2, fontWeight: 500 }}>Braucht Aufmerksamkeit</Typography>
          <Divider />
          <List disablePadding>
            {attention.map((a) => (
              <ListItemButton key={a.name} onClick={() => router.push(`/applications/${a.name}`)} sx={{ gap: 1.5 }}>
                <StatusChip status={a.state} />
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 500 }}>{a.name}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {a.reason}
                  </Typography>
                </Box>
                <ChevronRightIcon color="disabled" />
              </ListItemButton>
            ))}
            {undeclared.length > 0 && (
              <ListItemButton onClick={() => router.push("/applications?tab=undeclared")} sx={{ gap: 1.5 }}>
                <StatusChip status="DRIFT" />
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 500 }}>{undeclared.length} nicht deklarierte Container</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {undeclared.map((u) => u.image).join(" · ")}
                  </Typography>
                </Box>
                <ChevronRightIcon color="disabled" />
              </ListItemButton>
            )}
            {data && attention.length === 0 && undeclared.length === 0 && (
              <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
                Alles in Sync — keine Auffälligkeiten.
              </Typography>
            )}
          </List>
        </Paper>

        <Stack spacing={2}>
          <Paper variant="outlined">
            <Stack direction="row" alignItems="center" spacing={1} sx={{ p: 2 }}>
              <ScheduleIcon color="secondary" fontSize="small" />
              <Typography sx={{ fontWeight: 500 }}>Aktive Holds</Typography>
            </Stack>
            <Divider />
            {activeHolds.length === 0 ? (
              <Typography variant="body2" color="text.secondary" sx={{ p: 2 }}>
                Keine aktiven Holds.
              </Typography>
            ) : (
              activeHolds.map((a) => (
                <React.Fragment key={a.name}>
                  <Stack direction="row" alignItems="center" spacing={1} sx={{ p: 2 }}>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography sx={{ fontWeight: 500 }}>{a.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {holdTypeLabel(a.hold!.type)} · Rest{" "}
                        <Box component="span" sx={{ fontFamily: "monospace", color: "secondary.main", fontWeight: 500 }}>
                          {formatCountdown(remaining(a.name))}
                        </Box>
                      </Typography>
                    </Box>
                    <Button size="small" color="secondary" variant="outlined" onClick={() => releaseHold(a.name)}>
                      Freigeben
                    </Button>
                  </Stack>
                  <Divider />
                </React.Fragment>
              ))
            )}
          </Paper>

          <Paper variant="outlined">
            <Typography sx={{ p: 2, fontWeight: 500 }}>Letzte Aktivität</Typography>
            <Divider />
            {feed.map((f, i) => (
              <Stack key={i} direction="row" spacing={1.5} sx={{ px: 2, py: 1.2 }} alignItems="center">
                <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
                  {f.clock}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {f.summary}
                </Typography>
              </Stack>
            ))}
          </Paper>
        </Stack>
      </Box>

      <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 2 }}>
        Kennzahlen, Gesamtstatus und „Braucht Aufmerksamkeit&quot; sind live aus{" "}
        <code>GET /api/status</code>. Aktive Holds und Aktivität stammen aus der operativen/Audit-Ebene und folgen mit
        den entsprechenden Backend-Ausbaustufen.
      </Typography>
    </>
  );
}
