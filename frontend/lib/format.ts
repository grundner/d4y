export function formatCountdown(secs: number | null): string {
  if (secs == null) return "—";
  const s = Math.max(0, secs);
  const m = Math.floor(s / 60);
  const ss = s % 60;
  return `${m < 10 ? "0" : ""}${m}:${ss < 10 ? "0" : ""}${ss}`;
}

export function holdTypeLabel(type: string): string {
  if (type === "temp-param") return "Temporäre Parameter";
  if (type === "stop") return "Stop (mit Hold)";
  return "Hold";
}
