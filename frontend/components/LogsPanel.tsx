"use client";

import * as React from "react";
import { Alert, Box, Button, Stack, Typography } from "@mui/material";
import { fetchLogs } from "@/lib/actions";

/** Zeigt echte Container-Logs (GET /api/apps/{name}/logs), optional live gepollt. */
export default function LogsPanel({ name }: { name: string }) {
  const [text, setText] = React.useState("");
  const [paused, setPaused] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  const load = React.useCallback(async () => {
    try {
      const r = await fetchLogs(name, 200);
      setText(r?.output ?? "");
      setError(null);
    } catch (e: any) {
      setError(e?.message || "Logs nicht verfügbar");
    }
  }, [name]);

  React.useEffect(() => {
    load();
  }, [load]);

  React.useEffect(() => {
    if (paused) return;
    const iv = setInterval(load, 2500);
    return () => clearInterval(iv);
  }, [paused, load]);

  React.useEffect(() => {
    const el = scrollRef.current;
    if (el && !paused) el.scrollTop = el.scrollHeight;
  }, [text, paused]);

  return (
    <Box sx={{ borderRadius: 2, overflow: "hidden", border: "1px solid rgba(0,0,0,0.18)" }}>
      <Stack direction="row" alignItems="center" spacing={1.5} sx={{ bgcolor: "#2d2d2d", color: "#ddd", px: 1.75, py: 1, fontSize: 13 }}>
        {paused ? (
          <Stack direction="row" spacing={0.7} alignItems="center" sx={{ color: "#e0a94a" }}>
            <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "#e0a94a" }} />
            <span>Pausiert</span>
          </Stack>
        ) : (
          <Stack direction="row" spacing={0.7} alignItems="center">
            <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: "#4caf50", animation: "d4ypulse 1.4s infinite" }} />
            <span>Live</span>
          </Stack>
        )}
        <Box sx={{ fontFamily: "monospace", color: "#aaa" }}>{name} · stdout+stderr</Box>
        <Box sx={{ flex: 1 }} />
        <Box sx={{ color: "#888", fontSize: 12 }}>Tail 200</Box>
        <Button size="small" variant="outlined" onClick={() => setPaused((p) => !p)} sx={{ minWidth: 82, color: "#ccc", borderColor: "#555" }}>
          {paused ? "Fortsetzen" : "Pause"}
        </Button>
      </Stack>
      {error && (
        <Alert severity="error" sx={{ borderRadius: 0 }}>
          {error}
        </Alert>
      )}
      <Box
        ref={scrollRef}
        sx={{ bgcolor: "#1e1e1e", color: "#d4d4d4", fontFamily: "monospace", fontSize: 12.5, lineHeight: 1.6, p: 2, height: 380, overflow: "auto", whiteSpace: "pre-wrap" }}
      >
        {text || "— keine Ausgabe —"}
      </Box>
      <Typography sx={{ bgcolor: "#2d2d2d", color: "#888", px: 1.75, py: 0.9, fontSize: 11.5 }}>
        Read-only. Hinweis: Secret-Masking ist backend-seitig noch nicht umgesetzt (ADR-0013).
      </Typography>
    </Box>
  );
}
