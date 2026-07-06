"use client";

import * as React from "react";
import { Box, Button, Stack, Typography } from "@mui/material";
import { LOG_SEED, LOG_TEMPLATES } from "@/lib/mockData";

interface LogLine {
  ts: string;
  lvl: string;
  color: string;
  msg: string;
}

function makeLine(): LogLine {
  const t = LOG_TEMPLATES[Math.floor(Math.random() * LOG_TEMPLATES.length)];
  const d = new Date();
  const p = (n: number) => (n < 10 ? "0" : "") + n;
  return {
    ts: `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`,
    lvl: t.lvl,
    color: t.color,
    msg: t.msg(),
  };
}

export default function LogConsole({ appName }: { appName: string }) {
  const [lines, setLines] = React.useState<LogLine[]>(LOG_SEED);
  const [paused, setPaused] = React.useState(false);
  const scrollRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (paused) return;
    const iv = setInterval(() => {
      setLines((prev) => {
        const next = prev.concat([makeLine()]);
        return next.length > 200 ? next.slice(next.length - 200) : next;
      });
    }, 1400);
    return () => clearInterval(iv);
  }, [paused]);

  React.useEffect(() => {
    const el = scrollRef.current;
    if (el && !paused) el.scrollTop = el.scrollHeight;
  }, [lines, paused]);

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
        <Box sx={{ fontFamily: "monospace", color: "#aaa" }}>{appName} · stdout+stderr</Box>
        <Box sx={{ flex: 1 }} />
        <Box sx={{ color: "#888", fontSize: 12 }}>Tail 200</Box>
        <Button
          size="small"
          onClick={() => setPaused((p) => !p)}
          sx={{ minWidth: 82, color: "#ccc", borderColor: "#555" }}
          variant="outlined"
        >
          {paused ? "Fortsetzen" : "Pause"}
        </Button>
      </Stack>
      <Box
        ref={scrollRef}
        sx={{
          bgcolor: "#1e1e1e",
          color: "#d4d4d4",
          fontFamily: "monospace",
          fontSize: 12.5,
          lineHeight: 1.7,
          p: 2,
          height: 380,
          overflow: "auto",
        }}
      >
        {lines.map((ln, i) => (
          <Box key={i} sx={{ display: "flex", gap: 1.5 }}>
            <Box component="span" sx={{ color: "#6a9955", flex: "none" }}>
              {ln.ts}
            </Box>
            <Box component="span" sx={{ flex: "none", fontWeight: 500, width: 50, color: ln.color }}>
              {ln.lvl}
            </Box>
            <Box component="span">{ln.msg}</Box>
          </Box>
        ))}
      </Box>
      <Typography sx={{ bgcolor: "#2d2d2d", color: "#888", px: 1.75, py: 0.9, fontSize: 11.5 }}>
        Read-only · Geheimnisse werden serverseitig maskiert und niemals in der UI angezeigt.
      </Typography>
    </Box>
  );
}
