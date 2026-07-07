"use client";

import * as React from "react";
import { Box, Button, Stack, TextField, Typography } from "@mui/material";
import TerminalIcon from "@mui/icons-material/Terminal";
import { execCmd, type ExecResult } from "@/lib/actions";

/** Interaktive exec-Sitzung (POST /api/apps/{name}/exec). */
export default function ExecPanel({ name }: { name: string }) {
  const [cmd, setCmd] = React.useState("echo hi");
  const [result, setResult] = React.useState<ExecResult | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [running, setRunning] = React.useState(false);

  const run = async () => {
    setRunning(true);
    setError(null);
    try {
      setResult(await execCmd(name, ["sh", "-c", cmd]));
    } catch (e: any) {
      setError(e?.message || "exec fehlgeschlagen");
      setResult(null);
    } finally {
      setRunning(false);
    }
  };

  return (
    <Box sx={{ borderRadius: 2, overflow: "hidden", border: "1px solid rgba(0,0,0,0.18)" }}>
      <Stack direction="row" alignItems="center" spacing={1.25} sx={{ bgcolor: "#2d2d2d", color: "#ddd", px: 1.75, py: 1, fontSize: 13 }}>
        <TerminalIcon sx={{ color: "#4caf50", fontSize: 18 }} />
        <span>exec / Shell · Debugging-Sitzung</span>
        <Box sx={{ flex: 1 }} />
        <Box sx={{ fontFamily: "monospace", color: "#aaa", fontSize: 12 }}>{name}.d4y.internal</Box>
      </Stack>
      <Stack direction="row" spacing={1} sx={{ bgcolor: "#252525", p: 1.25 }} alignItems="center">
        <Box sx={{ color: "#4caf50", fontFamily: "monospace", fontSize: 13 }}>root@{name}:/app#</Box>
        <TextField
          value={cmd}
          onChange={(e) => setCmd(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !running) run();
          }}
          size="small"
          fullWidth
          variant="standard"
          slotProps={{ input: { disableUnderline: true, sx: { color: "#fff", fontFamily: "monospace", fontSize: 13 } } }}
        />
        <Button variant="contained" size="small" onClick={run} disabled={running}>
          Ausführen
        </Button>
      </Stack>
      <Box sx={{ bgcolor: "#1e1e1e", color: "#d4d4d4", fontFamily: "monospace", fontSize: 12.5, lineHeight: 1.7, p: 2, minHeight: 160, whiteSpace: "pre-wrap" }}>
        {error ? (
          <Box sx={{ color: "#f48771" }}>{error}</Box>
        ) : result ? (
          <>
            <Box sx={{ color: "#9e9e9e" }}>{result.output || "(keine Ausgabe)"}</Box>
            <Box sx={{ color: result.exitCode === 0 ? "#4caf50" : "#f48771", mt: 1 }}>exit {result.exitCode}</Box>
          </>
        ) : (
          <Box sx={{ color: "#666" }}>Kommando eingeben und „Ausführen"…</Box>
        )}
      </Box>
      <Typography sx={{ bgcolor: "#2d2d2d", color: "#888", px: 1.75, py: 0.9, fontSize: 11.5 }}>
        Flüchtige Debug-Sitzung · wird auditiert · kann sanktionierte, temporäre Drift erzeugen.
      </Typography>
    </Box>
  );
}
