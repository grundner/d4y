"use client";

import * as React from "react";
import { Alert, AlertTitle, Box, Button, Card, CardContent, Chip, Stack, Typography } from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import FolderIcon from "@mui/icons-material/Folder";
import InsertDriveFileIcon from "@mui/icons-material/InsertDriveFile";
import { CONFIG_REPO } from "@/lib/mockData";

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography sx={{ mt: 0.25, fontSize: 13, ...(mono ? { fontFamily: "monospace", fontSize: 12.5 } : {}) }}>{value}</Typography>
    </Box>
  );
}

const OBJECTS: { icon: "folder" | "file"; text: string; indent?: boolean }[] = [
  { icon: "folder", text: "apps/" },
  { icon: "file", text: "nginx.yaml · api-gateway.yaml · postgres.yaml · …", indent: true },
  { icon: "folder", text: "routes/" },
  { icon: "folder", text: "volumes/" },
  { icon: "file", text: "registries.yaml" },
  { icon: "file", text: "backup-stores.yaml" },
  { icon: "file", text: "dns.yaml" },
];

export default function ConfigPage() {
  return (
    <>
      <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography variant="h4" sx={{ fontWeight: 400 }}>
          Config-Repository
        </Typography>
        <Chip size="small" icon={<LockOutlinedIcon />} label="READ-ONLY" sx={{ bgcolor: "rgba(139,147,161,0.15)", color: "#c7cdd6", fontWeight: 500 }} />
      </Stack>
      <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75, mb: 2.5 }}>
        Quelle der Wahrheit für den deklarativen Sollzustand
      </Typography>

      <Alert
        severity="info"
        icon={<LockOutlinedIcon />}
        sx={{ mb: 3 }}
        action={
          <Button color="info" size="small" startIcon={<OpenInNewIcon />}>
            Im Repository öffnen
          </Button>
        }
      >
        <AlertTitle sx={{ fontWeight: 600 }}>Änderungen erfolgen ausschließlich über Git</AlertTitle>
        Der deklarative Sollzustand — welche Apps, Images, Routes, Volumes, Backup-Policies — wird nur über das
        Config-Repository geändert. Dieses UI ist bezüglich des Sollzustands vollständig read-only.
      </Alert>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 1.1fr" }, gap: 2, alignItems: "start" }}>
        <Card variant="outlined">
          <CardContent>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Aktueller Stand
            </Typography>
            <Stack spacing={1.5} sx={{ mt: 1.5 }}>
              <Field label="Repository" value={CONFIG_REPO.repository} mono />
              <Field label="Branch" value={CONFIG_REPO.branch} mono />
              <Field label="Commit" value={CONFIG_REPO.commit} mono />
              <Field label="Autor / Zeit" value={CONFIG_REPO.author} />
            </Stack>
          </CardContent>
        </Card>

        <Card variant="outlined">
          <CardContent>
            <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: "0.05em" }}>
              Deklarierte Objekte
            </Typography>
            <Box sx={{ fontFamily: "monospace", fontSize: 13, mt: 1.5 }}>
              {OBJECTS.map((o, i) => (
                <Stack key={i} direction="row" spacing={1} alignItems="center" sx={{ py: 0.5, pl: o.indent ? 2.75 : 0, color: o.indent ? "text.secondary" : "text.primary" }}>
                  {o.icon === "folder" ? (
                    <FolderIcon sx={{ fontSize: 18, color: "#e0a94a" }} />
                  ) : (
                    <InsertDriveFileIcon sx={{ fontSize: 16, color: "#8b93a1" }} />
                  )}
                  <span>{o.text}</span>
                </Stack>
              ))}
            </Box>
          </CardContent>
        </Card>
      </Box>
    </>
  );
}
