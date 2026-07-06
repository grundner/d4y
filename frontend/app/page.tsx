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
  Stack,
  Typography,
} from "@mui/material";
import ScheduleIcon from "@mui/icons-material/Schedule";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import StatusChip from "@/components/StatusChip";
import { useD4y } from "@/lib/store";
import { ACTIVITY, CONFIG_REPO, UNDECLARED } from "@/lib/mockData";
import { formatCountdown, holdTypeLabel } from "@/lib/format";
import type { AppState } from "@/lib/types";

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
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
  const { apps, remaining, releaseHold } = useD4y();

  const count = (s: AppState) => apps.filter((a) => a.state === s).length;
  const activeHolds = apps.filter((a) => a.hold);

  const attention = apps
    .filter((a) => a.state !== "IN_SYNC")
    .map((a) => {
      let reason = "";
      if (a.state === "OUTDATED") {
        reason = `Ist-Image ${a.actualImage?.split(":").pop()} weicht vom Soll ${a.image.split(":").pop()} ab`;
      } else if (a.state === "MISSING") {
        reason = "Container nicht vorhanden — Reconcile ausstehend";
      } else if (a.state === "STOPPED") {
        reason = "Manuell gestoppt · Hold aktiv";
      } else {
        reason = "Weicht vom Sollzustand ab";
      }
      return { name: a.name, state: a.state, reason, href: `/applications/${a.name}` };
    });

  const feed = ACTIVITY.slice(0, 5).map((a) => {
    const who = a.actor === "system" ? "system" : a.actor.split("@")[0];
    return { clock: a.time.slice(11), summary: `${ACTION_SUMMARY[a.type] ?? a.type} · ${a.app} · ${who}` };
  });

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

      <Alert severity="warning" sx={{ mb: 3 }}>
        <AlertTitle sx={{ fontWeight: 600 }}>Gesamtstatus: DRIFT</AlertTitle>
        1 App veraltet · 1 App fehlt · 1 App gestoppt · {UNDECLARED.length} nicht deklarierte Container. Die
        letzte Reconciliation vor 42 s wurde erfolgreich abgeschlossen; Selbstheilung ist aktiv.
        <Box component="span" sx={{ display: "block", mt: 1, fontSize: 13 }}>
          Config: <Box component="span" sx={{ fontFamily: "monospace" }}>{CONFIG_REPO.version}</Box>
        </Box>
      </Alert>

      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(148px, 1fr))",
          gap: 2,
          mb: 3,
        }}
      >
        <StatCard label="Apps gesamt" value={apps.length} color="text.primary" />
        <StatCard label="In Sync" value={count("IN_SYNC")} color="success.main" />
        <StatCard label="Veraltet" value={count("OUTDATED")} color="warning.main" />
        <StatCard label="Fehlt" value={count("MISSING")} color="error.main" />
        <StatCard label="Gestoppt" value={count("STOPPED")} color="text.secondary" />
        <StatCard label="Undeclared" value={UNDECLARED.length} color="warning.main" />
        <StatCard label="Aktive Holds" value={activeHolds.length} color="secondary.main" />
      </Box>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1.4fr 1fr" }, gap: 2, alignItems: "start" }}>
        <Paper variant="outlined">
          <Typography sx={{ p: 2, fontWeight: 500 }}>Braucht Aufmerksamkeit</Typography>
          <Divider />
          <List disablePadding>
            {attention.map((a) => (
              <ListItemButton key={a.name} onClick={() => router.push(a.href)} sx={{ gap: 1.5 }}>
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
            <ListItemButton onClick={() => router.push("/applications?tab=undeclared")} sx={{ gap: 1.5 }}>
              <StatusChip status="DRIFT" />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography sx={{ fontWeight: 500 }}>{UNDECLARED.length} nicht deklarierte Container</Typography>
                <Typography variant="body2" color="text.secondary">
                  {UNDECLARED.map((u) => u.image).join(" · ")}
                </Typography>
              </Box>
              <ChevronRightIcon color="disabled" />
            </ListItemButton>
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
    </>
  );
}
