"use client";

import * as React from "react";
import { Alert, Box, Card, CardContent, Chip, Paper, Stack, Typography } from "@mui/material";
import StorageIcon from "@mui/icons-material/Storage";
import Inventory2Icon from "@mui/icons-material/Inventory2";
import CloudIcon from "@mui/icons-material/Cloud";
import DnsIcon from "@mui/icons-material/Dns";
import StatusChip from "@/components/StatusChip";
import { BACKUP_STORES, DNS_PROVIDERS, NODES, REGISTRIES } from "@/lib/mockData";

const grid = { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: 2 } as const;

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <Typography sx={{ fontWeight: 500, fontSize: 16, mb: 1.5, mt: 3 }}>{children}</Typography>
  );
}

export default function InfrastructurePage() {
  return (
    <>
      <Typography variant="h4" sx={{ fontWeight: 400 }}>
        Infrastruktur / Topologie
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, mb: 2 }}>
        Austauschbare, zustandslose Bausteine — die Persistenz liegt außerhalb der Laufzeit
      </Typography>

      <Alert severity="info" sx={{ mb: 2.5 }}>
        <b>Beispieldaten.</b> Server, Registries, Backup-Stores und DNS-Provider sind noch nicht im
        Backend modelliert — diese Übersicht zeigt geplante Struktur, keine Live-Daten.
      </Alert>

      <Stack direction="row" spacing={1.5} flexWrap="wrap" useFlexGap>
        <Paper variant="outlined" sx={{ px: 2, py: 1.25, display: "inline-flex", alignItems: "center", gap: 1 }}>
          <Inventory2Icon color="primary" fontSize="small" />
          <Typography variant="body2">
            <b>Code</b> ← Registry
          </Typography>
        </Paper>
        <Paper variant="outlined" sx={{ px: 2, py: 1.25, display: "inline-flex", alignItems: "center", gap: 1 }}>
          <CloudIcon color="primary" fontSize="small" />
          <Typography variant="body2">
            <b>Daten</b> ← Backup-Store
          </Typography>
        </Paper>
        <Paper variant="outlined" sx={{ px: 2, py: 1.25, display: "inline-flex", alignItems: "center", gap: 1 }}>
          <DnsIcon color="primary" fontSize="small" />
          <Typography variant="body2">
            <b>Namen</b> ← DNS-Provider
          </Typography>
        </Paper>
      </Stack>

      <SectionTitle>Server / Nodes</SectionTitle>
      <Box sx={grid}>
        {NODES.map((n) => (
          <Card key={n.id} variant="outlined">
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                <Stack direction="row" alignItems="center" spacing={1}>
                  <StorageIcon sx={{ color: "#8b93a1" }} />
                  <Typography sx={{ fontWeight: 500 }}>{n.id}</Typography>
                </Stack>
                <StatusChip status={n.status} />
              </Stack>
              <Typography sx={{ fontFamily: "monospace", fontSize: 12.5, color: "text.secondary", mt: 1.25 }}>
                {n.host}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {n.containers} verwaltete Container
              </Typography>
              <Chip size="small" label="austauschbar · zustandslos" sx={{ mt: 1.5, bgcolor: "rgba(139,147,161,0.15)", color: "#c7cdd6" }} />
            </CardContent>
          </Card>
        ))}
      </Box>

      <SectionTitle>Registries</SectionTitle>
      <Box sx={grid}>
        {REGISTRIES.map((r) => (
          <Card key={r.name} variant="outlined">
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1}>
                <Inventory2Icon sx={{ color: "#8b93a1" }} />
                <Typography sx={{ fontFamily: "monospace", fontSize: 13 }}>{r.name}</Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1.25 }}>
                {r.note}
              </Typography>
              <Chip size="small" label="vertrauenswürdig" sx={{ mt: 1.5, bgcolor: "rgba(95,208,168,0.16)", color: "#5fd0a8" }} />
            </CardContent>
          </Card>
        ))}
      </Box>

      <SectionTitle>Backup-Stores</SectionTitle>
      <Box sx={grid}>
        {BACKUP_STORES.map((b) => (
          <Card key={b.name} variant="outlined">
            <CardContent>
              <Stack direction="row" alignItems="center" spacing={1}>
                <CloudIcon sx={{ color: "#8b93a1" }} />
                <Typography sx={{ fontFamily: "monospace", fontSize: 13 }}>{b.name}</Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1.25 }}>
                {b.kind}
              </Typography>
              <Chip size="small" label={b.status} sx={{ mt: 1.5, bgcolor: "rgba(95,208,168,0.16)", color: "#5fd0a8" }} />
            </CardContent>
          </Card>
        ))}
      </Box>

      <SectionTitle>DNS-Provider</SectionTitle>
      <Box sx={grid}>
        {DNS_PROVIDERS.map((d) => (
          <Card key={d.name} variant="outlined">
            <CardContent>
              <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                <Stack direction="row" alignItems="center" spacing={1}>
                  <DnsIcon sx={{ color: "#8b93a1" }} />
                  <Typography sx={{ fontWeight: 500, fontSize: 14 }}>{d.name}</Typography>
                </Stack>
                <Chip
                  size="small"
                  label={d.mode}
                  sx={
                    d.mode === "managed"
                      ? { bgcolor: "rgba(77,184,255,0.16)", color: "#6ac4ff", fontWeight: 500 }
                      : { bgcolor: "rgba(139,147,161,0.15)", color: "#c7cdd6", fontWeight: 500 }
                  }
                />
              </Stack>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1.25 }}>
                {d.note}
              </Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
    </>
  );
}
