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
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from "@mui/material";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { useStatus } from "@/lib/api";

function UndeclaredDetailInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const id = searchParams.get("id") ?? "";
  const { data, error, loading } = useStatus(0);

  const back = () => router.push("/applications?tab=undeclared");
  const cur = data?.undeclared.find((u) => u.containerId === id) ?? null;

  if (loading && !data) {
    return <Skeleton variant="rounded" height={200} />;
  }
  if (error && !data) {
    return (
      <>
        <Button startIcon={<ArrowBackIcon />} onClick={back} sx={{ mb: 2 }}>
          Zurück zu Undeclared
        </Button>
        <Alert severity="error">{error}</Alert>
      </>
    );
  }
  if (!cur) {
    return (
      <>
        <Button startIcon={<ArrowBackIcon />} onClick={back} sx={{ mb: 2 }}>
          Zurück zu Undeclared
        </Button>
        <Alert severity="info">
          Nicht deklarierter Container nicht mehr vorhanden (evtl. bereits entfernt oder deklariert).
        </Alert>
      </>
    );
  }

  return (
    <>
      <Button startIcon={<ArrowBackIcon />} onClick={back} sx={{ mb: 2 }}>
        Zurück zu Undeclared
      </Button>

      <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          {cur.appName || "(ohne App-Name)"}
        </Typography>
        <Chip
          size="small"
          icon={<WarningAmberIcon />}
          label="Undeclared"
          sx={{ bgcolor: "rgba(224,169,74,0.16)", color: "#e0a94a", fontWeight: 500 }}
        />
        <Chip
          size="small"
          label={cur.running ? "Läuft" : "Gestoppt"}
          sx={
            cur.running
              ? { bgcolor: "rgba(95,208,168,0.16)", color: "#5fd0a8", fontWeight: 500 }
              : { bgcolor: "rgba(139,147,161,0.15)", color: "#c7cdd6", fontWeight: 500 }
          }
        />
      </Stack>

      <Alert severity="warning" sx={{ mb: 2.5 }}>
        Von D4Y verwalteter Container <b>ohne Deklaration</b> im Config-Repository — sanktionsfähige
        Drift. Der Sollzustand wird ausschließlich über Git geändert.
      </Alert>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" }, gap: 2, mb: 2 }}>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Container
            </Typography>
            <Stack spacing={1} sx={{ mt: 1.5 }}>
              <Field label="Image" value={cur.image} mono />
              <Field label="Container-ID" value={cur.containerId} mono />
              <Field label="Läuft" value={cur.running ? "Ja" : "Nein"} />
            </Stack>
          </CardContent>
        </Card>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Hinweis
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5, lineHeight: 1.55 }}>
              Um diesen Container zu legalisieren, deklariere ihn im Config-Repository; andernfalls
              wird er beim nächsten Reconcile als Drift behandelt.
            </Typography>
          </CardContent>
        </Card>
      </Box>

      <Typography variant="overline" color="text.secondary">
        Gemountete Volumes (Ist)
      </Typography>
      {cur.volumes.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 4, textAlign: "center", borderStyle: "dashed", color: "text.secondary", mt: 1 }}>
          <Typography>Keine von D4Y verwalteten Named Volumes gemountet.</Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper} variant="outlined" sx={{ mt: 1 }}>
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
      )}
    </>
  );
}

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <Box sx={{ display: "flex", justifyContent: "space-between", gap: 2 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body2" sx={mono ? { fontFamily: "monospace", fontSize: 12.5, textAlign: "right" } : { textAlign: "right" }}>
        {value}
      </Typography>
    </Box>
  );
}

export default function UndeclaredDetailPage() {
  // useSearchParams erfordert im statischen Export eine Suspense-Grenze.
  return (
    <Suspense fallback={<Skeleton variant="rounded" height={200} />}>
      <UndeclaredDetailInner />
    </Suspense>
  );
}
