"use client";

import * as React from "react";
import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  MenuItem,
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
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import StopIcon from "@mui/icons-material/Stop";
import TuneIcon from "@mui/icons-material/Tune";
import ScheduleIcon from "@mui/icons-material/Schedule";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import StatusChip from "@/components/StatusChip";
import LogsPanel from "@/components/LogsPanel";
import ExecPanel from "@/components/ExecPanel";
import { useD4y } from "@/lib/store";
import { useStatus } from "@/lib/api";
import { formatCountdown, holdEnumLabel } from "@/lib/format";
import {
  restartApp,
  stopApp,
  setParams,
  setHold,
  releaseHold,
  inspectApp,
  type ContainerDetails,
} from "@/lib/actions";
import type { AppState } from "@/lib/types";

const DURATIONS = [
  { label: "5 Minuten", min: 5 },
  { label: "15 Minuten", min: 15 },
  { label: "30 Minuten", min: 30 },
  { label: "60 Minuten", min: 60 },
];

type DialogKind = "restart" | "stop" | "params" | "hold" | null;

function EmptyState({ text }: { text: string }) {
  return (
    <Paper variant="outlined" sx={{ p: 5, textAlign: "center", borderStyle: "dashed", color: "text.secondary", mb: 2 }}>
      <Typography>{text}</Typography>
    </Paper>
  );
}

function PendingNote({ text }: { text: string }) {
  return (
    <Alert severity="info" icon={<LockOutlinedIcon />}>
      {text}
    </Alert>
  );
}

function AppDetailInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const name = decodeURIComponent(searchParams.get("name") ?? "");
  const { showSnack, refreshSignal, manualRefresh } = useD4y();

  const [localReload, setLocalReload] = React.useState(0);
  const reloadKey = refreshSignal + localReload;
  const { data, error, loading } = useStatus(reloadKey);
  const reload = () => setLocalReload((n) => n + 1);

  const [details, setDetails] = React.useState<ContainerDetails | null>(null);
  const [tab, setTab] = React.useState("overview");

  const [dlg, setDlg] = React.useState<DialogKind>(null);
  const [durationMin, setDurationMin] = React.useState(15);
  const [envKey, setEnvKey] = React.useState("LOG_LEVEL");
  const [envValue, setEnvValue] = React.useState("debug");
  const [busy, setBusy] = React.useState(false);

  const cur = data?.applications.find((a) => a.name === name) ?? null;

  React.useEffect(() => {
    if (!name) return;
    let alive = true;
    inspectApp(name)
      .then((d) => alive && setDetails(d))
      .catch(() => alive && setDetails(null));
    return () => {
      alive = false;
    };
  }, [name, reloadKey]);

  if (loading && !data) {
    return <Skeleton variant="rounded" height={200} />;
  }
  if (error && !data) {
    return (
      <>
        <Button startIcon={<ArrowBackIcon />} onClick={() => router.push("/applications")} sx={{ mb: 2 }}>
          Zurück zu Applications
        </Button>
        <Alert severity="error">{error}</Alert>
      </>
    );
  }
  if (!cur) {
    return (
      <>
        <Button startIcon={<ArrowBackIcon />} onClick={() => router.push("/applications")} sx={{ mb: 2 }}>
          Zurück zu Applications
        </Button>
        <Alert severity="error">Application „{name}&quot; nicht gefunden.</Alert>
      </>
    );
  }

  const hold = cur.hold ?? null;
  const rem = hold ? formatCountdown(hold.remainingSeconds) : "";
  const actualImage = details?.image ?? cur.desiredImage;
  const imageMismatch = !!details && details.image !== cur.desiredImage;

  async function runDialog() {
    setBusy(true);
    try {
      const secs = durationMin * 60;
      if (dlg === "restart") {
        await restartApp(name);
        showSnack(`Neustart von ${name} angefordert.`);
      } else if (dlg === "stop") {
        await stopApp(name, secs);
        showSnack(`Stop von ${name} — Hold ${durationMin} min.`);
      } else if (dlg === "params") {
        await setParams(name, { [envKey]: envValue }, secs);
        showSnack(`Temporäre Parameter für ${name} gesetzt — Hold ${durationMin} min.`);
      } else if (dlg === "hold") {
        await setHold(name, "MANUAL", secs);
        showSnack(`Hold für ${name} gesetzt (${durationMin} min).`);
      }
      setDlg(null);
      reload();
      manualRefresh();
    } catch (e: any) {
      showSnack("Fehler: " + (e?.message || e));
    } finally {
      setBusy(false);
    }
  }

  async function doRelease() {
    try {
      await releaseHold(name);
      showSnack(`Hold für ${name} freigegeben — D4Y reconciled den Sollzustand.`);
      reload();
      manualRefresh();
    } catch (e: any) {
      showSnack("Fehler: " + (e?.message || e));
    }
  }

  return (
    <>
      <Button startIcon={<ArrowBackIcon />} onClick={() => router.push("/applications")} sx={{ mb: 2 }}>
        Zurück zu Applications
      </Button>

      <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          {name}
        </Typography>
        <StatusChip status={cur.state as AppState} />
        {hold && (
          <Chip color="secondary" size="small" icon={<ScheduleIcon />} label={`HOLD ${rem}`} sx={{ fontWeight: 500 }} />
        )}
      </Stack>
      <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap sx={{ mb: 2, color: "text.secondary", fontSize: 13 }}>
        <span>
          Desired Image:{" "}
          <Box component="span" sx={{ fontFamily: "monospace", color: "text.primary" }}>
            {cur.desiredImage}
          </Box>
        </span>
        <span>
          Service-Discovery:{" "}
          <Box component="span" sx={{ fontFamily: "monospace", color: "text.primary" }}>
            {name}.d4y.internal
          </Box>
        </span>
      </Stack>

      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Button variant="contained" startIcon={<RestartAltIcon />} onClick={() => setDlg("restart")}>
          Restart
        </Button>
        <Button variant="outlined" color="inherit" startIcon={<StopIcon />} onClick={() => setDlg("stop")}>
          Stop (mit Hold)
        </Button>
        <Button variant="outlined" color="inherit" startIcon={<TuneIcon />} onClick={() => setDlg("params")}>
          Temporäre Parameter
        </Button>
        {hold ? (
          <Button variant="outlined" color="secondary" onClick={doRelease}>
            Hold freigeben
          </Button>
        ) : (
          <Button variant="outlined" color="inherit" onClick={() => setDlg("hold")}>
            Hold setzen
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 760 }}>
        Alle Aktionen erfordern eine Bestätigung. „Stop&quot; und „Temporäre Parameter&quot; erzeugen sanktionierte,
        temporäre Drift und setzen automatisch einen zeitlich begrenzten Hold.
      </Typography>

      {hold && (
        <Alert
          icon={<ScheduleIcon />}
          severity="info"
          sx={{ mb: 2, bgcolor: "rgba(178,141,255,0.15)", color: "#c3a9ff", "& .MuiAlert-icon": { color: "#b28dff" } }}
          action={
            <Button color="secondary" size="small" onClick={doRelease}>
              Freigeben
            </Button>
          }
        >
          Gehalten (Hold) · {holdEnumLabel(hold.type)} aktiv — läuft automatisch ab in{" "}
          <Box component="span" sx={{ fontFamily: "monospace", fontWeight: 600 }}>
            {rem}
          </Box>
          . Der Sollzustand bleibt unverändert; nach Ablauf reconciled D4Y selbstständig.
        </Alert>
      )}

      <Tabs value={tab} onChange={(_e, v) => setTab(v)} variant="scrollable" scrollButtons="auto" sx={{ borderBottom: 1, borderColor: "divider", mb: 2.5 }}>
        <Tab value="overview" label="Übersicht" />
        <Tab value="logs" label="Logs" />
        <Tab value="exec" label="exec / Shell" />
        <Tab value="volumes" label="Volumes" />
        <Tab value="routes" label="Routes" />
        <Tab value="hold" label="Hold" />
      </Tabs>

      {tab === "overview" && (
        <>
          <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ width: 200 }} />
                  <TableCell>Soll (Git)</TableCell>
                  <TableCell>Ist (Runtime)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <TableRow>
                  <TableCell sx={{ color: "text.secondary" }}>Image</TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{cur.desiredImage}</TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, ...(imageMismatch ? { bgcolor: "rgba(224,169,74,0.16)", color: "#e0a94a" } : {}) }}>
                    {actualImage}
                    {imageMismatch ? " · weicht ab" : ""}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell sx={{ color: "text.secondary" }}>Läuft</TableCell>
                  <TableCell>Ja</TableCell>
                  <TableCell>{cur.running ? "Ja" : "Nein"}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell sx={{ color: "text.secondary" }}>Container-ID</TableCell>
                  <TableCell sx={{ color: "text.disabled" }}>—</TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>
                    {cur.containerId ? cur.containerId.slice(0, 12) : "—"}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell sx={{ color: "text.secondary" }}>Erstellt</TableCell>
                  <TableCell sx={{ color: "text.disabled" }}>—</TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>{details?.createdAt ?? "—"}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </TableContainer>
          <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" }, gap: 2 }}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
                  Sync-State
                </Typography>
                <Box sx={{ mt: 1.5, mb: 1.5 }}>
                  <StatusChip status={cur.state as AppState} />
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Live aus <code>GET /api/status</code>
                </Typography>
              </CardContent>
            </Card>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
                  Selbstheilung
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5, lineHeight: 1.55 }}>
                  Weicht der Ist- vom Sollzustand ab, wird automatisch reconciled — sofern kein Hold aktiv ist. Der
                  Sollzustand wird ausschließlich über Git geändert.
                </Typography>
              </CardContent>
            </Card>
          </Box>
        </>
      )}

      {tab === "logs" && <LogsPanel name={name} />}
      {tab === "exec" && <ExecPanel name={name} />}
      {tab === "volumes" && (
        cur.volumes.length === 0 ? (
          <EmptyState text="Diese App deklariert keine Volumes. Named Volumes werden im Config-Repository (Git) deklariert." />
        ) : (
          <>
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Mount-Pfad (Container)</TableCell>
                    <TableCell>Typ</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cur.volumes.map((v) => (
                    <TableRow key={v.name}>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{v.name}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{v.path}</TableCell>
                      <TableCell>
                        <Chip size="small" label="Named" sx={{ bgcolor: "rgba(77,184,255,0.16)", color: "#6ac4ff", fontWeight: 500 }} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <Typography variant="body2" color="text.secondary">
              Deklarierte Named Volumes (Soll, aus <code>GET /api/status</code>). Deklaration nur über das
              Config-Repository (Git); der Inhalt ist nicht Teil des Sollzustands.
            </Typography>
          </>
        )
      )}
      {tab === "routes" && (
        <PendingNote text="Routes und DNS-Zuordnungen sind noch nicht Teil des API und folgen mit der Backend-Ausbaustufe. Deklaration erfolgt über das Config-Repository (Git)." />
      )}
      {tab === "hold" && (
        hold ? (
          <Card variant="outlined" sx={{ borderColor: "rgba(156,39,176,0.35)" }}>
            <CardContent>
              <Stack direction="row" spacing={2.25} alignItems="center">
                <Box sx={{ width: 70, height: 70, borderRadius: "50%", bgcolor: "rgba(178,141,255,0.16)", display: "flex", alignItems: "center", justifyContent: "center", flex: "none" }}>
                  <ScheduleIcon sx={{ color: "#b28dff", fontSize: 34 }} />
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Typography sx={{ fontWeight: 500 }}>{holdEnumLabel(hold.type)} · aktiv</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    Verbleibend{" "}
                    <Box component="span" sx={{ fontFamily: "monospace", fontWeight: 600, color: "secondary.main", fontSize: 16 }}>
                      {rem}
                    </Box>{" "}
                    · Hold ist immer zeitlich begrenzt und läuft automatisch ab.
                  </Typography>
                </Box>
                <Button variant="outlined" color="secondary" onClick={doRelease}>
                  Freigeben
                </Button>
              </Stack>
            </CardContent>
          </Card>
        ) : (
          <EmptyState text="Kein aktiver Hold." />
        )
      )}

      <Dialog open={dlg !== null} onClose={() => (busy ? null : setDlg(null))} fullWidth maxWidth="xs">
        <DialogTitle>
          {dlg === "restart" && "Restart bestätigen"}
          {dlg === "stop" && "Stop bestätigen"}
          {dlg === "params" && "Temporäre Parameter"}
          {dlg === "hold" && "Hold setzen"}
        </DialogTitle>
        <DialogContent>
          {dlg === "restart" && <DialogContentText>Neustart von {name} anfordern?</DialogContentText>}
          {(dlg === "stop" || dlg === "hold" || dlg === "params") && (
            <Stack spacing={2} sx={{ mt: 1 }}>
              {dlg !== "hold" && (
                <DialogContentText>
                  Erzeugt sanktionierte, temporäre Drift und setzt einen zeitlich begrenzten Hold.
                </DialogContentText>
              )}
              {dlg === "params" && (
                <Stack direction="row" spacing={1}>
                  <TextField label="Env-Key" value={envKey} onChange={(e) => setEnvKey(e.target.value)} size="small" fullWidth />
                  <TextField label="Wert" value={envValue} onChange={(e) => setEnvValue(e.target.value)} size="small" fullWidth />
                </Stack>
              )}
              <TextField
                select
                label="Hold-Dauer"
                value={durationMin}
                onChange={(e) => setDurationMin(Number(e.target.value))}
                size="small"
                fullWidth
              >
                {DURATIONS.map((d) => (
                  <MenuItem key={d.min} value={d.min}>
                    {d.label}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setDlg(null)} disabled={busy}>
            Abbrechen
          </Button>
          <Button variant="contained" onClick={runDialog} disabled={busy || (dlg === "params" && !envKey)}>
            Bestätigen
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default function AppDetailPage() {
  // useSearchParams erfordert im statischen Export eine Suspense-Grenze.
  return (
    <Suspense fallback={<Skeleton variant="rounded" height={200} />}>
      <AppDetailInner />
    </Suspense>
  );
}
