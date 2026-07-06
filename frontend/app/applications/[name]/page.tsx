"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
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
  Typography,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import RestartAltIcon from "@mui/icons-material/RestartAlt";
import StopIcon from "@mui/icons-material/Stop";
import TuneIcon from "@mui/icons-material/Tune";
import ScheduleIcon from "@mui/icons-material/Schedule";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import TerminalIcon from "@mui/icons-material/Terminal";
import StatusChip from "@/components/StatusChip";
import LogConsole from "@/components/LogConsole";
import { useD4y } from "@/lib/store";
import { TERM_LINES } from "@/lib/mockData";
import { formatCountdown, holdTypeLabel } from "@/lib/format";

const GIT_HINT_VOLUMES = "Volumes, Persistenz und Backup-Policy sind deklarativ. Änderung nur über das Config-Repository (Git).";
const GIT_HINT_ROUTES = "Routes und DNS-Zuordnungen sind deklarativ. Änderung nur über das Config-Repository (Git).";

function EmptyState({ text }: { text: string }) {
  return (
    <Paper variant="outlined" sx={{ p: 5, textAlign: "center", borderStyle: "dashed", color: "text.secondary", mb: 2 }}>
      <Typography>{text}</Typography>
    </Paper>
  );
}

function GitHint({ text }: { text: string }) {
  return (
    <Stack direction="row" spacing={0.75} alignItems="center" sx={{ color: "text.secondary", fontSize: 12.5 }}>
      <LockOutlinedIcon sx={{ fontSize: 15 }} />
      <span>{text}</span>
    </Stack>
  );
}

export default function AppDetailPage() {
  const router = useRouter();
  const params = useParams();
  const name = decodeURIComponent(String(params.name));
  const { apps, remaining, releaseHold, showSnack } = useD4y();
  const [tab, setTab] = React.useState("overview");
  const [confirm, setConfirm] = React.useState<{ title: string; message: string; run: () => void } | null>(null);

  const cur = apps.find((a) => a.name === name);
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

  const holdActive = !!cur.hold;
  const rem = formatCountdown(remaining(cur.name));
  const serviceName = `${cur.name}.d4y.internal`;
  const actualImage = cur.actualImage || cur.image;
  const imageMismatch = !!cur.actualImage && cur.actualImage !== cur.image;

  const ask = (title: string, message: string, run: () => void) => setConfirm({ title, message, run });

  return (
    <>
      <Button startIcon={<ArrowBackIcon />} onClick={() => router.push("/applications")} sx={{ mb: 2 }}>
        Zurück zu Applications
      </Button>

      <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          {cur.name}
        </Typography>
        <StatusChip status={cur.state} />
        {holdActive && (
          <Chip color="secondary" size="small" icon={<ScheduleIcon />} label={`HOLD ${rem}`} sx={{ fontWeight: 500 }} />
        )}
      </Stack>
      <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap sx={{ mb: 2, color: "text.secondary", fontSize: 13 }}>
        <span>
          Desired Image:{" "}
          <Box component="span" sx={{ fontFamily: "monospace", color: "text.primary" }}>
            {cur.image}
          </Box>
        </span>
        <span>
          Service-Discovery:{" "}
          <Box component="span" sx={{ fontFamily: "monospace", color: "text.primary" }}>
            {serviceName}
          </Box>
        </span>
      </Stack>

      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 1 }}>
        <Button
          variant="contained"
          startIcon={<RestartAltIcon />}
          onClick={() => ask("Restart bestätigen", `Neustart von ${cur.name} anfordern?`, () => showSnack(`Neustart von ${cur.name} angefordert.`))}
        >
          Restart
        </Button>
        <Button
          variant="outlined"
          color="inherit"
          startIcon={<StopIcon />}
          onClick={() =>
            ask(
              "Stop bestätigen",
              `${cur.name} stoppen? Dies erzeugt sanktionierte, temporäre Drift und setzt einen zeitlich begrenzten Hold.`,
              () => showSnack(`Stop von ${cur.name} — setzt einen zeitlich begrenzten Hold.`)
            )
          }
        >
          Stop (mit Hold)
        </Button>
        <Button
          variant="outlined"
          color="inherit"
          startIcon={<TuneIcon />}
          onClick={() =>
            ask(
              "Temporäre Parameter",
              `Temporären Parameter-Override für ${cur.name} setzen? Erzeugt sanktionierte, temporäre Drift und einen zeitlich begrenzten Hold.`,
              () => showSnack(`Temporäre Parameter für ${cur.name} — setzt einen zeitlich begrenzten Hold.`)
            )
          }
        >
          Temporäre Parameter
        </Button>
        {holdActive ? (
          <Button variant="outlined" color="secondary" onClick={() => releaseHold(cur.name)}>
            Hold freigeben
          </Button>
        ) : (
          <Button
            variant="outlined"
            color="inherit"
            onClick={() =>
              ask("Hold setzen", `Zeitlich begrenzten Hold für ${cur.name} setzen?`, () =>
                showSnack(`Hold für ${cur.name} gesetzt (zeitlich begrenzt).`)
              )
            }
          >
            Hold setzen
          </Button>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2, maxWidth: 760 }}>
        Alle Aktionen erfordern eine Bestätigung. „Stop&quot; und „Temporäre Parameter&quot; erzeugen sanktionierte,
        temporäre Drift und setzen automatisch einen zeitlich begrenzten Hold.
      </Typography>

      {holdActive && (
        <Alert
          icon={<ScheduleIcon />}
          severity="info"
          sx={{ mb: 2, bgcolor: "#f3e5f5", color: "#4a148c" }}
          action={
            <Button color="secondary" size="small" onClick={() => releaseHold(cur.name)}>
              Freigeben
            </Button>
          }
        >
          Gehalten (Hold) · {holdTypeLabel(cur.hold!.type)} aktiv — läuft automatisch ab in{" "}
          <Box component="span" sx={{ fontFamily: "monospace", fontWeight: 600 }}>
            {rem}
          </Box>
          . Der deklarative Sollzustand bleibt unverändert; nach Ablauf reconciled D4Y selbstständig.
        </Alert>
      )}

      <Tabs
        value={tab}
        onChange={(_e, v) => setTab(v)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ borderBottom: 1, borderColor: "divider", mb: 2.5 }}
      >
        <Tab value="overview" label="Übersicht" />
        <Tab value="logs" label="Logs" />
        <Tab value="exec" label="exec / Shell" />
        <Tab value="volumes" label="Volumes" />
        <Tab value="routes" label="Routes" />
        <Tab value="params" label="Parameter / Hold" />
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
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{cur.image}</TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, ...(imageMismatch ? { bgcolor: "#fff4e5", color: "#8a5200" } : {}) }}>
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
                    {cur.containerId || "—"}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell sx={{ color: "text.secondary" }}>Server / Node</TableCell>
                  <TableCell sx={{ color: "text.disabled" }}>—</TableCell>
                  <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>{cur.server}</TableCell>
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
                  <StatusChip status={cur.state} />
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Letzter Reconcile: vor 42 s · OK
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

      {tab === "logs" && <LogConsole appName={cur.name} />}

      {tab === "exec" && (
        <Box sx={{ borderRadius: 2, overflow: "hidden", border: "1px solid rgba(0,0,0,0.18)" }}>
          <Stack direction="row" alignItems="center" spacing={1.25} sx={{ bgcolor: "#2d2d2d", color: "#ddd", px: 1.75, py: 1, fontSize: 13 }}>
            <TerminalIcon sx={{ color: "#4caf50", fontSize: 18 }} />
            <span>exec / Shell · Debugging-Sitzung</span>
            <Box sx={{ flex: 1 }} />
            <Box sx={{ fontFamily: "monospace", color: "#aaa", fontSize: 12 }}>{serviceName}</Box>
          </Stack>
          <Box sx={{ bgcolor: "#1e1e1e", color: "#d4d4d4", fontFamily: "monospace", fontSize: 12.5, lineHeight: 1.75, p: 2, minHeight: 220 }}>
            {TERM_LINES.map((tl, i) =>
              tl.t === "cmd" ? (
                <Box key={i}>
                  <Box component="span" sx={{ color: "#4caf50" }}>
                    root@{cur.name}:/app#
                  </Box>{" "}
                  <Box component="span" sx={{ color: "#fff" }}>
                    {tl.text}
                  </Box>
                </Box>
              ) : (
                <Box key={i} sx={{ color: "#9e9e9e" }}>
                  {tl.text}
                </Box>
              )
            )}
            <Box>
              <Box component="span" sx={{ color: "#4caf50" }}>
                root@{cur.name}:/app#
              </Box>{" "}
              <Box component="span" sx={{ display: "inline-block", width: 8, height: 15, bgcolor: "#d4d4d4", verticalAlign: "middle", animation: "d4yblink 1s infinite" }} />
            </Box>
          </Box>
          <Typography sx={{ bgcolor: "#2d2d2d", color: "#888", px: 1.75, py: 0.9, fontSize: 11.5 }}>
            Flüchtige Debug-Sitzung · kann sanktionierte, temporäre Drift erzeugen · keine Geheimnisse in der Ausgabe.
          </Typography>
        </Box>
      )}

      {tab === "volumes" &&
        (cur.volumes.length ? (
          <>
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Typ</TableCell>
                    <TableCell>Persistenz</TableCell>
                    <TableCell>Backup-Policy</TableCell>
                    <TableCell>Backup-Store</TableCell>
                    <TableCell>Letzter Restore</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cur.volumes.map((v) => (
                    <TableRow key={v.name}>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{v.name}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{v.type}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{v.persist}</TableCell>
                      <TableCell>
                        {v.backup ? (
                          <Stack direction="row" spacing={0.7} alignItems="center" sx={{ color: "success.main" }}>
                            <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "success.main" }} />
                            <span>An</span>
                          </Stack>
                        ) : (
                          <Typography component="span" color="text.disabled">
                            Aus
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary" }}>{v.store}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{v.restore}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <GitHint text={GIT_HINT_VOLUMES} />
          </>
        ) : (
          <>
            <EmptyState text="Keine Volumes deklariert." />
            <GitHint text={GIT_HINT_VOLUMES} />
          </>
        ))}

      {tab === "routes" &&
        (cur.routes.length ? (
          <>
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 2 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Hostname</TableCell>
                    <TableCell>Ziel-App</TableCell>
                    <TableCell>DNS-Modus</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cur.routes.map((host) => (
                    <TableRow key={host}>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{host}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{cur.name}</TableCell>
                      <TableCell>
                        <Chip size="small" label="managed" sx={{ bgcolor: "#e5f6fd", color: "#01579b", fontWeight: 500 }} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            <GitHint text={GIT_HINT_ROUTES} />
          </>
        ) : (
          <EmptyState text="Keine Routes zugeordnet — diese App ist nicht öffentlich erreichbar." />
        ))}

      {tab === "params" && (
        <>
          <Typography sx={{ fontWeight: 500, mb: 1.5 }}>Temporäre Parameter-Overrides (transient)</Typography>
          {cur.tempParams.length ? (
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Key</TableCell>
                    <TableCell>Wert</TableCell>
                    <TableCell>Gesetzt von</TableCell>
                    <TableCell>Seit</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cur.tempParams.map((p) => (
                    <TableRow key={p.key}>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5 }}>{p.key}</TableCell>
                      <TableCell sx={{ fontFamily: "monospace", fontSize: 12.5, color: "#8a5200" }}>{p.value}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{p.by}</TableCell>
                      <TableCell sx={{ color: "text.secondary" }}>{p.since}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <EmptyState text="Keine aktiven Parameter-Overrides." />
          )}
          <Typography sx={{ fontWeight: 500, mb: 1.5 }}>Hold</Typography>
          {holdActive ? (
            <Card variant="outlined" sx={{ borderColor: "rgba(156,39,176,0.35)" }}>
              <CardContent>
                <Stack direction="row" spacing={2.25} alignItems="center">
                  <Box sx={{ width: 70, height: 70, borderRadius: "50%", bgcolor: "#f3e5f5", display: "flex", alignItems: "center", justifyContent: "center", flex: "none" }}>
                    <ScheduleIcon sx={{ color: "#9c27b0", fontSize: 34 }} />
                  </Box>
                  <Box sx={{ flex: 1 }}>
                    <Typography sx={{ fontWeight: 500 }}>{holdTypeLabel(cur.hold!.type)} · aktiv</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      Verbleibend{" "}
                      <Box component="span" sx={{ fontFamily: "monospace", fontWeight: 600, color: "secondary.main", fontSize: 16 }}>
                        {rem}
                      </Box>{" "}
                      · Hold ist immer zeitlich begrenzt und läuft automatisch ab.
                    </Typography>
                  </Box>
                  <Button variant="outlined" color="secondary" onClick={() => releaseHold(cur.name)}>
                    Freigeben
                  </Button>
                </Stack>
              </CardContent>
            </Card>
          ) : (
            <EmptyState text="Kein aktiver Hold." />
          )}
        </>
      )}

      <Dialog open={!!confirm} onClose={() => setConfirm(null)}>
        <DialogTitle>{confirm?.title}</DialogTitle>
        <DialogContent>
          <DialogContentText>{confirm?.message}</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button color="inherit" onClick={() => setConfirm(null)}>
            Abbrechen
          </Button>
          <Button
            variant="contained"
            onClick={() => {
              confirm?.run();
              setConfirm(null);
            }}
          >
            Bestätigen
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
